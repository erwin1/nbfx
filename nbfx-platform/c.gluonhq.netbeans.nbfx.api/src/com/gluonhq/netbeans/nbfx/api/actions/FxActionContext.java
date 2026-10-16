package com.gluonhq.netbeans.nbfx.api.actions;

import org.openide.util.Lookup;

/**
 * The global action context: the generic container that describes "what is selected right now" and
 * that every {@link Command} is evaluated against.
 * <p>
 * It is the nbfx view of the NetBeans global context ({@code Utilities.actionsGlobalContext()}): like
 * that context it is a {@link Lookup}, so it is fully generic and has no compile-time knowledge of the
 * domain types it carries. Consumers ask it for the type they care about
 * ({@code context.lookup(SomeType.class)}) and observe it with {@link #lookupResult(Class)}.
 * <p>
 * nbfx contributes a {@code ContextGlobalProvider} that feeds the nbfx selection (the files selected
 * in the active view, the active editor document, the active view) into the NetBeans global context,
 * exactly as the Swing {@code GlobalActionContextImpl} feeds it from the activated TopComponent.
 * <p>
 * Domain objects that are not carried directly (for example the project that owns a selected file)
 * are resolved on demand by per-domain <em>facades</em> (for example {@code ProjectContext} in
 * {@code nbfx.project.context}), so the context stays generic and everything is resolved dynamically.
 */
public abstract class FxActionContext extends Lookup {

    /** Protected constructor for the implementation. */
    protected FxActionContext() {
    }

    /**
     * The global action context, or {@code null} when none is registered (for example in a unit test
     * that does not run the full application).
     *
     * @return the context, or {@code null}
     */
    public static FxActionContext getDefault() {
        return Lookup.getDefault().lookup(FxActionContext.class);
    }
}
