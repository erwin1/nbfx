package com.gluonhq.netbeans.nbfx.api.actions;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.input.KeyCombination;

import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 * Base class for {@link Command}s whose enabled state is derived from the global
 * {@link FxActionContext}.
 * <p>
 * This is the nbfx counterpart of the Swing {@code LookupSensitiveAction}: instead of each command
 * reaching into the project, file and editor registries and re-implementing its own listeners, it
 * declares the context types it depends on and {@link #isEnabled(FxActionContext)}, and the base
 * class re-evaluates the command whenever one of those types changes. The command is disabled when
 * no context is registered.
 * <p>
 * The base class is domain-agnostic: it only knows the context types passed to the constructor, so a
 * build command can observe {@code OpenProject} without this API depending on the project API.
 */
public abstract class ContextSensitiveCommand extends AbstractCommand {

    private final List<Lookup.Result<?>> results = new ArrayList<>();
    private final LookupListener listener = this::onContextChanged;

    /**
     * Creates a context-sensitive command with no default accelerator.
     *
     * @param id            the stable command id, never {@code null}
     * @param text          the display text, never {@code null}
     * @param observedTypes the context types whose changes re-evaluate this command
     */
    protected ContextSensitiveCommand(String id, String text, Class<?>... observedTypes) {
        this(id, text, null, observedTypes);
    }

    /**
     * Creates a context-sensitive command.
     *
     * @param id            the stable command id, never {@code null}
     * @param text          the display text, never {@code null}
     * @param accelerator   the preferred keyboard accelerator, or {@code null} if none
     * @param observedTypes the context types whose changes re-evaluate this command
     */
    @SuppressWarnings("this-escape")
    protected ContextSensitiveCommand(String id, String text, KeyCombination accelerator,
            Class<?>... observedTypes) {
        super(id, text, accelerator);
        FxActionContext context = FxActionContext.getDefault();
        if (context != null) {
            for (Class<?> type : observedTypes) {
                Lookup.Result<?> result = context.lookupResult(type);
                result.addLookupListener(listener);
                results.add(result);
            }
            // Defer the first evaluation until the subclass is fully constructed: calling the
            // overridable isEnabled() from the constructor would read uninitialized subclass state.
            scheduleRefresh();
        }
    }

    /**
     * The global action context this command observes, or {@code null} when none is registered.
     *
     * @return the context, or {@code null}
     */
    protected final FxActionContext context() {
        return FxActionContext.getDefault();
    }

    /**
     * Re-evaluates {@link #isEnabled(FxActionContext)} and updates the disabled state. Called
     * automatically when an observed context type changes, and once after construction. A subclass
     * that needs the state to be correct before the next JavaFX pulse (for example in a headless
     * unit test) may call it at the end of its constructor.
     */
    protected final void refreshContext() {
        FxActionContext context = context();
        setDisabled(context == null || !isEnabled(context));
    }

    private void scheduleRefresh() {
        try {
            Platform.runLater(this::refreshContext);
        } catch (IllegalStateException toolkitNotInitialized) {
            // No JavaFX runtime (headless unit test): leave the command disabled until the context
            // changes or refreshContext() is called explicitly.
        }
    }

    private void onContextChanged(LookupEvent event) {
        refreshContext();
    }

    /**
     * Whether this command can run in {@code context}. Called on the JavaFX Application Thread when
     * the observed context changes; must be side-effect free.
     *
     * @param context the global action context, never {@code null}
     * @return {@code true} when the command is enabled
     */
    protected abstract boolean isEnabled(FxActionContext context);

    @Override
    public void dispose() {
        for (Lookup.Result<?> result : results) {
            result.removeLookupListener(listener);
        }
        results.clear();
    }
}
