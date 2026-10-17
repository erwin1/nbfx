# 09 — FX dialogs, notifications and exception reporting (UI)

Step: **FX dialogs & notifications (UI)** · branch: `feature/fx-dialogs` · status: planned, can start in
parallel with the wizard/New File work.

## Why

nbfx has no shared dialog/notification layer. Every module builds its own `Dialog<ButtonType>` or
`Alert`, `ErrorReporter` wraps a JavaFX `Alert`, and NetBeans' **Swing** notification and exception
modules are still shipped — so some user-visible dialogs are Swing.

Evidence (all paths relative to the `netbeans` submodule or this repo):

| Area | Current state |
|---|---|
| Error reporting | `nbfx.api/…/ErrorReporter` builds a `javafx.scene.control.Alert`; **30** call sites |
| Ad-hoc dialogs | `FxProjectCustomizerDialog`, `PluginsDialog`/`PluginDialogs`, `GitStatusView`, `IgnoreListDialog`, `FindInProjectsDialog`, `IssuesDialog`, `FxOptionsDialog`, `DeleteActions`, `CloseConfirmation` |
| Notifications (Swing) | `ide/notifications` (`org-netbeans-modules-notifications.jar`) is in the ide cluster |
| Exception reporter (Swing) | `platform/uihandler` + `uihandler.exceptionreporter` + `lib.uihandler` (`org-netbeans-modules-uihandler.jar`, `org-netbeans-lib-uihandler.jar`) |
| Uncaught exceptions | no handler in nbfx (grep for `setDefaultUncaughtExceptionHandler` is empty) |
| Shared service | none (`FxDialogs`/`Dialogs` does not exist) |

## Target architecture

A new UI module **`nbfx.dialogs`** (`nbfx-platform`), plus the cluster exclusions below. The SPI is
UI-only because a dialog descriptor carries a JavaFX `Node`; if a UI-free notification *item* model
(severity/title/body/action ids) is worthwhile later, it can be split per
[`../architecture-rules.md`](../architecture-rules.md).

### 1. Dialogs — the FX `DialogDescriptor`/`DialogDisplayer`

```java
// shape, not final API
record FxDialogDescriptor(
    String title, String header, String message, Severity severity,
    Node content, List<FxDialogOption> options, FxDialogOption initial,
    boolean resizable, String helpId, String showAgainKey) {}

record FxDialogOption(String label, Role role, boolean defaultOption) {}
// Role: DEFAULT, CANCEL, OTHER (like DialogDescriptor's roles)

interface Dialogs {                       // @ServiceProvider
    Optional<FxDialogOption> show(FxDialogDescriptor descriptor);   // showAndWait on FX thread
    CompletableFuture<FxDialogOption> showAsync(FxDialogDescriptor descriptor);
    void showMessage(Severity severity, String title, String message);
}
```

- **Notification line** support (the analogue of Swing's `NotificationLineSupport`) inside the dialog:
  `setNotificationLine(String, Severity)` while a resolver/progress runs, without layout jumps.
- **"Show again"** checkbox bound to a preferences key (used by the project-problems alert).
- **Resizable + remembered size**, owner window, default/cancel buttons, Enter/Esc.
- **Content** is any `Node` (problem list, custom form, details panel).

### 2. Notifications (UI)

Replace the Swing `notifications` module with an FX layer:

- **Toasts** — non-modal cards anchored to the bottom-right above the status bar; stack (max ~3) and
  coalesce repeated events into “+N more”; slide+fade in/out; **auto-dismiss with pause on hover**;
  pin/sticky for errors; inline actions.
- **Notification centre** — a slide-over panel from the status bar: history, unread markers, filter by
  severity, clear/clear-all, mute/do-not-disturb; keyboard: Esc dismisses the top toast, Enter runs the
  primary action.
- **Progress** — a toast can embed a progress bar and hand off to the status-bar progress element (see
  [`06-progress-ui.md`](06-progress-ui.md)).

### 3. Exception reporting

- Keep the `ErrorReporter` **API** (`report(title, header, message[, cause])`) and reimplement it on
  `Dialogs`/notifications — the 30 call sites stay unchanged.
- Global **uncaught-exception handler** in the FX application: recoverable errors → toast (optionally
  sticky); fatal → FX dialog with an expandable details sheet (stack trace, copy, open log), logged via
  `ErrorManager`/`java.util.logging`, deduplicated for repeated exceptions.
- Remove the Swing `uihandler` path (exclusions below) so no `JDialog` can appear.

## Chrome ideas (what FX enables beyond Swing)

- **In-window overlay**, not an OS balloon: rounded, shadowed cards over the app (no separate window /
  taskbar entry), the same visual language for toasts, dialogs and the centre.
- **Slide-in motion** from the screen edge, with a **reduced-motion** switch
  (`Platform.getPreferences().isReducedMotion()` and `Scene.getPreferences().setReducedMotion`).
- **Rich cards**: severity accent, icon, title/body, inline actions (“Resolve”, “Undo”, “Show Details”),
  progress inside the card, collapsible details.
- **Details sheet** for exceptions: slide-up stack trace with copy/save/report.
- **Grouping**: “12 similar errors” with an expander.
- **Theme-aware**: semantic colours for light/dark (see [`10-theming-and-branding.md`](10-theming-and-branding.md)).
- **Accessibility**: accessible roles/text, focus handling, Enter/Esc shortcuts.

## Cluster changes (no Swing dialogs left)

Remove and prune (extend the `-prune-excluded` target in `build.xml`, as already done for
`core.windows` and `progress.ui`):

- `notifications` (Swing notification window/balloon)
- `uihandler`, `uihandler.exceptionreporter`, `lib.uihandler`

Verify: no `org-netbeans-modules-notifications*.jar` / `*uihandler*.jar` in the dist, and no
`JDialog`/`DialogDisplayer` path is reachable for these features.

## Migration

1. `ErrorReporter` → `Dialogs` (no call-site changes).
2. Replace the ad-hoc dialogs listed above, one per step/commit (non-blocking for this step but tracked).
3. Any module that needs a notification uses the new API instead of `Alert`/`Dialog`.

## Tests & acceptance

- Unit tests (headless glass): descriptor → option mapping, severity, default/cancel roles, notification
  line, show-again persistence, toast queue/coalescing/pause, exception dedupe.
- Manual matrix: toast stack + centre in light/dark, action buttons, reduced motion, Esc/Enter, a fatal
  exception path, and the absence of Swing dialogs.
- Acceptance: `ErrorReporter` renders FX; notifications/centre work in light and dark; no Swing
  notification/exception dialogs load; `ant build` + `check-purity` green.

## Ordering / dependencies

- **Depends**: none.
- **Parallel**: with `feature/wizard-framework`/New File topics — separate `nbfx.dialogs` module, no file
  overlap. Agree the **semantic colour token names** with `feature/theming` (see 10) so both foundations
  compose; the dialogs step should consume the theme tokens rather than hard-code colours.
- The **project problems UI** ([`07-small-steps.md`](07-small-steps.md), and 02) depends on this step for
  its alert, list and notification line.

## Open questions

- Blocking (`showAndWait`) vs. future-based API: expose both (as sketched) or keep only the async one?
- Notification centre placement/behaviour: slide-over from the status bar vs. a view (tab) vs. both.
- Whether the notification *item* model should be UI-free (split module) so the Swing front end could
  reuse it later.
- Whether to adopt the OS accent colour (`Platform.getPreferences().getAccentColor()`) in the theme.
