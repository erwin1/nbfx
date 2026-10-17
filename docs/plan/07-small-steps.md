# 07 — Small-step execution backlog

The part docs describe *what* to build; this document slices it into **small, independently shippable
steps**, roughly ordered. Each step should merge on its own (build + guardrails green), so progress is
visible and risk stays low.

**Recommended first step: Progress UI parity** — self-contained, reuses the headless `api.progress`
API/controller, needs no JavaFX-project work, and is immediately visible.

Branch per step and the PR flow are in [`08-branching-and-workflow.md`](08-branching-and-workflow.md).
Each step is one branch (`feature/<topic>`) and one PR.

Legend per step: **Scope**, **Steps**, **Acceptance**, **Depends**.

---

## Progress UI parity
→ detail: [`06-progress-ui.md`](06-progress-ui.md) · branch: `feature/progress-ui`
- **Scope**: FX `ProgressUIWorkerProvider` over the headless `Controller`; status-line aggregate +
  all-tasks popup + per-task details/actions; exclude the Swing `progress.ui`.
- **Steps**:
  1. Exclude `progress.ui` from the nbfx clusters; confirm `api.progress`/`api.progress.nb` are present.
  2. New `nbfx.progress` module: depend on `api.progress` + `api.progress.nb`; register a minimal
     `ProgressUIWorkerProvider` (`ProgressUIWorkerWithModel`; `ExtractedProgressUIWorker` → `null`).
  3. FX status element driven by `TaskModel`: aggregate bar (determinate/indeterminate) + active task
     label/detail + task-count tooltip.
  4. All-tasks popup: rows (name, detail, progress bar, elapsed) with **Cancel** (confirm) and **View**
     (default action); system tasks shown distinctly.
  5. "View → Progress" action + auto show/hide + initial delay.
  6. Off-FX-thread coalesced updates (FX analogue of `RunOffEDTImpl`).
  7. Route `nbfx.run` builds and long operations through real `ProgressHandle`s; make `FxProgress` a thin
     wrapper.
  8. Unit tests (task-model wiring) + manual matrix (parallel tasks, cancel, popup, system task).
- **Acceptance**: two+ concurrent tasks render in the status line and popup; details/cancel/view work; no
  Swing `progress.ui` in the cluster set; guardrails green.
- **Depends**: none.
- **Landed** (branch `feature/progress-ui`): (1) `progress.ui` excluded from the generated cluster **and
  pruned from the dist** (a stale jar otherwise keeps winning the `ProgressEnvironment` lookup, so no
  progress shows); (2) `nbfx.progress` module = `FxProgressEnvironment` + `FxProgressController` (extends
  the headless `Controller`) + `FxInternalHandle` + `FxProgressWorker` (`ProgressUIWorkerWithModel`);
  (3) status element (aggregate bar + active label + task count + cancel) and (4) all-tasks popup
  (name/detail/bar + View + Cancel, system tasks marked); (6) coalesced off-FX updates; (5) View →
  Progress command; (7) `FxProgress` reimplemented as a thin `ProgressHandle` facade, so project loads
  and builds become real tasks in the same UI.
- **Remaining**: (8) unit tests + manual matrix.

## Progress: drop the launcher's hard-wired progress
→ branch: `feature/progress-launcher-removal`
- **Scope**: `JavaFXLaunchApp` still owns progress: it tracks the pending project loads (`ProjectLoads`),
  drives the single-slot `FxProgress` facade in `updateLoadingProgress()`, and used to hold a
  `progressElement`. Remove all of that and report progress **from the source** with the progress API:
  the project-open path creates a real `ProgressHandle` ("Opening project …", cancellable) and finishes it
  when the platform open completes; background scanning is already reported by the indexing API's own
  handles.
- **Steps**: move the open-progress handle into the open path (`ProjectRegistryImpl.open` /
  `openInPlatform`, or the opener the launcher calls) with per-load cancel; delete
  `updateLoadingProgress()` and the `loads`-driven status handling from `JavaFXLaunchApp`; remove the
  `FxProgress` service (interface + `FxProgressImpl`) once nothing references it.
- **Acceptance**: `JavaFXLaunchApp` contains no progress code; opening/closing a project and running a
  build each show a real, cancellable task in the status line and popup.
- **Depends**: Progress UI parity (its `FxProgress` facade made the launcher's last progress call go
  through the API; this step deletes the facade and the call site).
- **Ordering**: before "Status elements: dynamic registration" — clean the launcher first so the status
  bar work starts from a launcher with no progress wiring.
- **Landed** (local branch `feature/progress-launcher-removal`, stacked on `feature/progress-ui`):
  `ProjectLoads` owns a system `ProgressHandle` per load; `JavaFXLaunchApp` has no progress code;
  `BuildRunner` uses `ProgressHandle`; `FxProgress`/`FxProgressImpl` deleted. Build + purity + tests green.

## Status elements: dynamic registration
→ branch: `fix/status-elements-dynamic-registration`
- **Scope**: the status bar still creates `project`, `caret` and `line-separator` in `JavaFXLaunchApp`
  and passes them to `StatusElementRegistry.discover(additional)`; every element should instead be
  discovered from the layer via `@FxStatusRegistration`, so `discover()` alone yields the whole bar and
  `JavaFXLaunchApp` keeps no per-element references.
- **Steps**: make `ProjectStatusElement`/`CaretStatusElement`/`LineSeparatorStatusElement` observe their
  services (`ProjectContext`/`EditorContext`) instead of being driven by the window; convert them to
  `@FxStatusRegistration` with explicit `alignment`/`position`; drop the programmatic `StatusElement`s
  and the `additional` list; verify ordering/alignment is unchanged and no element is instantiated twice.
- **Acceptance**: `StatusElementRegistry.discover()` returns project/caret/line-separator/progress and the
  bar renders identically; `JavaFXLaunchApp` has no status-element fields.
- **Depends**: Progress UI parity (it already moved the `progress` element off the programmatic list).
- **Ordering**: do this **before** the next feature (Wizard framework) — it removes the last hand-wired
  status elements and keeps the status bar purely layer-driven.
- **Landed** (local branch `fix/status-elements-dynamic-registration`, stacked on the above):
  `project`, `caret` and `line-separator` are `@FxStatusRegistration` elements that follow their
  services; `JavaFXLaunchApp` no longer holds or drives them. Build + purity + tests green.

## Wizard framework
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/wizard-framework`
- **Scope**: `FxWizardDescriptor` (properties bag + `Lookup`), `FxWizardIterator` +
  `ArrayFxWizardIterator`, `readSettings`/`storeSettings`, `instantiate()`, messages/step/help, **and the
  themed dialog shell** (step rail, card, message area, buttons, `wizard.css`, icons) shared by every
  wizard.
- **Steps**: descriptor; panel/iterator SPI; themed dialog chrome (Back/Next/Finish/Cancel + messages +
  step rail + page icons + live refresh); migrate the existing Maven/Gradle wizards onto it; keep
  `@FxWizardRegistration`.
- **Acceptance**: the existing wizards run on the new framework; a 3-page test wizard passes settings
  between pages; the themed shell looks right in light and dark.
- **Depends**: none.
- **Landed** (local branch `feature/wizard-framework`, **two commits**):
  1. *shared descriptor, iterator and page hooks* — `FxWizardDescriptor` (the run's property bag +
     `Lookup`, untyped like NetBeans' `WizardDescriptor` rather than `<Data>`-generic),
     `FxWizardIterator` + `ArrayFxWizardIterator`, `FxWizardMessage` (INFO/WARNING/ERROR), and the
     `FxWizardPanel` hooks (read/store settings, message, description, icon, onChange) + tests.
  2. *themed dialog, icons and CSS* — `wizard.css` and `FxWizardDialog` (vertical step rail with
     numbered/checked badges, content card, severity-aware message area, primary/secondary/ghost
     buttons, owner + sizing, page fade, live Next/Finish refresh), `FxWizardIcons` (list/location/
     folder/file) and per-page `FxWizardPanel.getIcon()`. The list rules keep hover purely cosmetic
     (transparent) and the selection a strong bar, so moving the mouse never looks like a selection
     change.
  The existing wizards keep working through the default `FxWizard.getIterator()` adapter (runs
  `finish()`). Build + purity green; wizard tests 6/6.
- **Remaining**: help context; a 3-page settings-passing reference wizard; dynamic (non-list) iterators;
  reduced-motion handling and a runtime pass (`ant run`). The Maven/Gradle wizards need no change — they
  already run on the adapter.

## FX dialogs & notifications (UI)
→ branch: `feature/fx-dialogs`
- **Scope**: the shared FX counterpart of the Swing dialog/notification stack, plus the replacement of the
  Swing exception reporter. Reused by the project-problems UI and, over time, by the app's ad-hoc dialogs.
- **Parts**:
  1. **Dialogs** — `FxDialogDescriptor` (title, header/message + severity, optional content `Node`, options
     with roles, default/cancel, help, “show again” key) + a `Dialogs` service returning the chosen option,
     and a **notification line** for in-progress/result feedback (the `NotificationLineSupport` analogue).
  2. **Notifications (UI)** — replace the shipped Swing `notifications` module: non-modal, stacked toasts
     (slide in/out) with actions, progress and severity; a **notification centre** (history, unread
     markers, clear, mute/do-not-disturb); grouping/coalescing.
  3. **Exception reporting** — keep the `ErrorReporter` API (30 call sites) and reimplement it on the
     dialogs/notifications service; add a global uncaught-exception hook; replace the Swing `uihandler`
     path (details expander + copy/report instead of a raw stack-trace dialog).
  4. **Cluster** — exclude and prune `notifications`, `uihandler`, `uihandler.exceptionreporter` (and
     `lib.uihandler` if unused), as for `core.windows`/`progress.ui`, so no Swing notifications/dialogs load.
- **Chrome ideas (what FX enables beyond Swing)**:
  - **Slide-in toasts** from a corner (bottom-right, above the status bar), stacked and coalesced;
    slide+fade in/out, auto-dismiss with **pause on hover**, “pin”/sticky for errors.
  - **In-window overlay** instead of an OS dialog: rounded, shadowed cards over the app, no separate
    window/taskbar entry; the same shell used by toasts, modal dialogs and the notification centre.
  - **Rich cards**: severity accent, icon, title/body, inline actions (“Resolve”, “Show Details”),
    progress inside the card, collapsible details (stack trace/markdown), copy/open-log affordances.
  - **Notification centre**: a slide-over panel from the status bar with history, unread markers,
    clear/clear-all and mute; keyboard: Esc dismisses the top toast, Enter activates the primary action.
  - **Dialogs**: severity header band, non-jumping notification line, option buttons with roles, optional
    content, resizable + remembered size, “show again”, expandable exception details.
  - **Theme/motion**: semantic colours for light/dark, a reduced-motion switch, subtle transitions
    (reuse the wizard visual language, without coupling to `nbfx.wizard`).
- **Acceptance**: no Swing notification/exception dialogs load; `ErrorReporter` renders FX; toasts, the
  centre and modal dialogs work in light and dark; guardrails green.
- **Depends**: none.
- **Ordering / parallel**: **can start in parallel** with the wizard/New File topics — it only adds a new
  `nbfx.dialogs` UI module (plus the cluster exclusions) and does not touch `nbfx.wizard`, the project-UI
  moves or the template work. The project-problems UI (below) depends on it.
- **Landed**: n/a.

## Theming & branding (light/dark, CSS via branding)
→ detail: [`10-theming-and-branding.md`](10-theming-and-branding.md) · branch: `feature/theming`
- **Scope**: a default light and dark theme (following the OS), editor syntax colour profiles
  (NetBeans/NetBeansDark semantics), and one brandable CSS pipeline: a new `nbfx.theme` module with
  palettes (`theme-light.css`/`theme-dark.css`), `app.css` (structure using semantic tokens),
  `editor-light/dark.css`, a brandable `theme.properties` manifest and a `ThemeManager` service; every
  CSS loading point goes through it, and a brand overrides it via the NetBeans branding mechanism
  (`branding.dir` + `branding.token`).
- **Parts**: `ThemeManager` (System/Light/Dark, live switch, `Platform.getPreferences()` following,
  persistence, `View ▸ Theme` + Options ▸ Appearance); split `styles.css` into palette + structure and
  migrate every module sheet (`wizard.css`, `codeeditor.css`, `completion-popup.css`,
  `breadcrumbs-popup.css`, `search.css`, the future `dialogs.css`); branding wiring in the harness
  (`nbfx.branding.dir`, `nbfx.branding.token`, apply the `Branding` task) + `docs/theming.md`; editor
  palettes with an independent profile selection.
- **Acceptance**: runtime System/Light/Dark switches every surface and persists; no light-only colours
  remain; editor palettes work in both themes; a `branding/` override + token rebuild brands the CSS;
  guardrails green.
- **Depends**: none.
- **Ordering / parallel**: **can start in parallel** with the wizard/FX-dialogs/New File topics (separate
  `nbfx.theme` module). Agree the semantic colour token names with `feature/fx-dialogs`; land before
  claiming an app-wide dark theme and before the dialogs stylesheet is finalised.
- **Landed**: n/a.

## Project modularity refactoring
→ branch: `refactor/project-modularity`
- **Scope**: mirror `ide/projectui`'s layout so the New File/New Project wizards have a clean home
  **before** they are written. NetBeans puts the New File action/wizard/chooser in `ide/projectui`
  (action `...project.ui.actions.NewFile`; wizard `...project.ui.NewFileWizard` + `NewFileIterator`;
  chooser `...project.ui.TemplateChooserPanel`); the generic template wizard is
  `platform/openide.loaders.TemplateWizard` and headless creation is
  `platform/api.templates.FileBuilder` (already reused by `nbfx.templates`). `nbfx.project` is
  documented (MVP §6) as the **FxProject model** module, but today holds only that UI.
- **Steps**: move `NewFileWizard`, `NewProjectWizard`, `NewGradleProjectWizard`, `Wizards` from
  `nbfx.project` to `nbfx.project.ui`; move `WizardCommand`, `ProjectCommandsProvider`,
  `ProjectToolbarRegistrations` to `nbfx.project.actions`; leave `nbfx.project` for the FxProject model
  (§6); update the cluster fragment and the module dependencies; keep the wizard ids and layer
  registrations unchanged.
- **Acceptance**: New File / New Project still open the same wizards; `nbfx.project` carries no UI;
  guardrails green.
- **Depends**: Wizard framework (landed).
- **Ordering**: **before** "New File templates" and "New Project templates", so the new UI is written
  directly in `nbfx.project.ui` — fewer moving parts.
- **Landed** (local branch `refactor/project-modularity`, stacked on `feature/wizard-framework`): the
  seven project-UI classes moved to `nbfx.project.ui` (package `...project.ui`), bundle keys and
  `nbfx.annotations`/`nbfx.wizard` deps moved with them; `nbfx.project` left empty (reserved for the
  FxProject model, §6). Wizard ids/layer registrations unchanged; build + purity green.

## New File templates
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/newfile-templates`
- **Scope**: the FX chooser + target/parameters page on top of the headless **`nbfx.templates` model**
  (below); wire the New File command. Lives in `nbfx.project.ui` (see the refactoring step).
- **Steps**: chooser; target page; fallback for custom-iterator templates; wire the New File command.
- **Acceptance**: New File lists Java/XML/Other categories and creates files headlessly.
- **Depends**: Wizard framework, Project modularity refactoring.
- **Landed** (local branch `feature/newfile-templates`, **two commits** + a follow-up app-styles fix,
  stacked on `refactor/project-modularity`):
  1. *headless model + architecture rule + guard* — `nbfx-platform/c.gluonhq.netbeans.nbfx.templates`:
     `TemplateRegistry` (walks the live `Templates` filesystem into categories with the metadata the
     chooser needs: `templateCategory`, `requireProject`, `openFile`, description URL, custom iterator,
     script engine, display names via the localizing bundle) + `TemplateCreator` (delegates to
     `org.netbeans.api.templates.FileBuilder`); UI-free with a core-only chain, `docs/architecture-rules.md`
     and the `check-purity` `check-models` guardrail; registered in `nbfx-platform`.
  2. *template-driven chooser on the wizard framework* — the New File wizard in `nbfx.project.ui`
     (chooser + name/location, live preview, inline validation, description, each template's own icon),
     with the original chooser's metadata rules (project `RecommendedTemplates` + `templateCategory`,
     `requireProject`, snippets, `invisible` and `simple=false` folders, `openFile`) and the New File
     action on the File toolbar/menu (icon, shortcut tooltip, separator after the two New actions).
     Tests: templates 8/8.
  3. *app list hover fix* (follow-up `styles:` commit) — the app stylesheet painted list hover the
     selection colour and made the selected row transparent while hovering, so moving the mouse looked
     like it changed the selection; hover is now a subtle tint and the selection stays (grey unfocused,
     accent focused). App-wide, not New File-specific.
- **Remaining**: the **per-template wizard panels** for templates that ship a custom
  `instantiatingIterator` (next step), and a runtime pass (`ant run`).

## New File: per-template wizard panels (full parity)
→ branch: `feature/newfile-template-panels`
- **Scope**: give templates that bring their own wizard iterator their real panels in FX, instead of
  the generic name/location page. A template may declare `instantiatingIterator`; when present the
  original replaces the generic target page with that iterator's panels. The chooser already detects it
  (`TemplateItem.hasCustomIterator()`); this step adds the FX panels. **27 distinct iterators** ship for
  non-Project templates:
  - `java.project.ui` ×2 — `NewJavaFileWizardIterator` (Class/Interface/Enum/Record/AnnotationType/
    Empty/Exception/Main/Singleton/package-info/module-info/Package) and the base-file variant.
  - `junit.ui.wizards` (9: JUnit 3/4/5 test & suite, EmptyJUnitTest), `testng.ui.wizards` (2).
  - `beans.resources.templates.Templates` (5: Bean, BeanInfo, NoIconBeanInfo, Customizer, PropertyEditor).
  - `apisupport.project.ui.wizard.*` (**13**: action, html, window, layer, loader, moduleinstall,
    options, project, quicksearch, updatecenter, winsys, wizard, codegenerator, javahelp, javahint,
    librarydescriptor).
  - `maven.osgi.templates` (Activator), `java.j2semodule` (module-info), `css.prep.ui.wizard`
    (style.less/scss), `selenium2.*` (3), `javascript.karma` (karma.conf.js).
- **Framework**: an FX SPI (e.g. `FxTemplateWizardProvider`, keyed by the `instantiatingIterator` class
  name, registered in Lookup) returning the extra pages + the `instantiate` action; `NewFileWizard`
  uses it when present and falls back to the generic page otherwise. The dialog must support **dynamic
  pages** (rebuild the rail when the page count changes once a template is chosen).
- **Ordering of ports** (most used first): Java file wizard → apisupport action/window → beans →
  junit/testng → the rest.
- **Acceptance**: each ported template shows the original's panels and creates the same files with the
  same parameters (name/package plus the template-specific ones).
- **Depends**: New File templates, Wizard UI makeover.
- **Landed**: model + chooser detect custom iterators and apply the metadata rules (branch
  `feature/newfile-templates`; the wizard shell lives in `feature/wizard-framework`); the panels
  themselves are **not yet ported** — the generic page is used as the fallback.

## New Project templates
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/newproject-templates`
- **Scope**: chooser + templates for the existing types (Java with Ant / Maven / Gradle, NetBeans
  Modules), with FX wizard pages and headless project generation.
- **Steps**: one wizard per template (ported from the original iterators); wire the New Project command;
  skeletons via `ProjectGenerator` / Maven / Gradle writers.
- **Acceptance**: create each existing project type; it builds/runs.
- **Depends**: Wizard framework (New File templates for reuse patterns).

## FxProject model (headless project model)
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/fxproject-model`
- **Scope**: an nbfx-native headless `FxProject` implementing the NetBeans contract
  (`org.netbeans.api.project.Project` + `ProjectFactory`, registered via `@FxProjectTypeRegistration`)
  with `Sources`/`ClassPathProvider`/`ActionProvider`/`ProjectOpenedHook`/`RecommendedTemplates` — and
  **no** UI SPIs. Replaces the stock `ProjectManager.findProject` + `OpenProjects.open` bridge, so the
  Swing project-type modules and the `projectuiapi.base` dependency can be dropped while
  indexing/classpath/completion keep working.
- **Steps**: `FxProject` + factory + kind detection; `Sources`; `ClassPathProvider`; `ActionProvider`
  delegating to the nbfx commands; `ProjectOpenedHook` (indexer roots); per-type providers
  (Maven/Gradle/Ant-Java); `RecommendedTemplates`; wire `ProjectRegistry` to open/close `FxProject` and
  remove `OpenProjects`.
- **Acceptance**: opening each type indexes and completes with no `OpenProjects` call; `check-purity`
  passes with the `projectuiapi.base` exemption removed.
- **Depends**: none.
- **Landed**: n/a.
- **Ordering**: **later** — not a prerequisite for the New File/New Project wizard UI (which can start
  with generic filtering); do it before project-type-aware template filtering and before dropping the
  stock project-type modules. Full design + the NetBeans comparison is in
  [`02-part1-infrastructure.md`](02-part1-infrastructure.md) (Project model — `FxProject`).

## Project problems UI (FX)
→ branch: `feature/project-problems-ui`
- **Scope**: replace the stock Swing project-problems dialogs with an FX `ProjectProblemsImplementation`.
  Today `ide/projectui`'s `BrokenReferencesImpl` shows two Swing dialogs when a project reports problems
  (unresolved platform, broken references, missing modules): the **“Open Project” alert**
  (`BrokenReferencesAlertPanel`, *Resolve Problems… / Close*) and the **“Resolve Project Problems”**
  customizer (`BrokenReferencesCustomizer`). The SPI (`ProjectProblemsProvider` /
  `ProjectProblemResolver` / `ProjectProblemsImplementation` + `ProjectProblems.showAlert`) is headless;
  only the implementation and its panels are Swing.
- **Steps**: FX alert (title *Open Project*, warning, problems summary, *Resolve Problems…* / *Close*, and
  the “show again” preference); FX problems dialog listing each problem plus its resolver, running it and
  showing the result; register the FX `ProjectProblemsImplementation` (and stop loading/using
  `projectui`'s); honour the suppression preference/property; headless tests for the provider/resolver
  flow + a manual matrix.
- **Acceptance**: opening a project with problems shows FX dialogs only (no Swing); *Resolve* lists the
  problems and runs the same resolvers; guardrails green.
- **Depends**: **FX dialogs & notifications (UI)** — the alert, the problems list and the resolver
  notification line are built on it. The provider/resolver SPIs are headless; the stock
  `BrokenReferencesImpl` must be removed (patch `projectui`, or drop it after the FxProject step).
- **Ordering**: before removing `projectui` from the clusters. Only one `ProjectProblemsImplementation`
  may be registered, so either patch `projectui` to drop `BrokenReferencesImpl` or drop `projectui` after
  the FxProject step. Until then the stock Swing dialogs appear — suppress them with
  `-J-DBrokenReferencesSupport.suppressBrokenRefAlert=true` (or the “show again” preference).
- **Landed**: n/a.

## Java Platforms
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/java-platforms`
- **Scope**: headless `JavaPlatformManager` wiring + FX "Java Platforms" options + JavaFX capability.
- **Steps**: list/add/remove/rename; sources/javadoc; JavaFX runtime resolution (`jfxrt` / modules /
  OpenJFX SDK); platform selector component.
- **Acceptance**: platforms persist and are selectable; JavaFX capability detected.
- **Depends**: none.

## J2SE customizers
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/customizers-j2se`
- **Scope**: FXML customizer base + widget toolkit; complete the J2SE categories (Sources, Libraries,
  Build ▸ {Compiling, Packaging, Documenting}, Run, Application, License).
- **Steps**: `FxmlCustomizerPanel` base + shared controls; category by category; property-key audit;
  round-trip tests.
- **Acceptance**: every J2SE option present, validated, round-trips.
- **Depends**: Java Platforms (Run's platform fields).

## Platform customizers
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/customizers-platforms`
- **Scope**: Maven, Gradle and apisupport (module/suite) categories on the J2SE customizer base.
- **Steps**: one type at a time.
- **Acceptance**: per-type category parity.
- **Depends**: J2SE customizers.

## Build actions
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/build-actions`
- **Scope**: project-lookup `ActionProvider`-style enablement/invocation; full command set; per-type
  action sets; enablement everywhere (menu/toolbar/context menu).
- **Steps**: enablement from the project; full command set; per-type registration; context menu from the
  project, not marker files.
- **Acceptance**: actions appear/enable exactly per type; no unsupported commands on plain folders.
- **Depends**: none (wizard/command infra helps).

## Run lifecycle
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/run-lifecycle`
- **Scope**: enable/disable from the active project/selection; run in progress → disable/replace + Stop;
  bind Debug to `nbfx.debug`.
- **Acceptance**: Run toggles correctly around a running task.
- **Depends**: Build actions.

## Run configurations
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/run-configurations`
- **Scope**: config model (`nbproject[/private]/configs/*.properties`, active config) + Run combo
  (New/Delete/Rename) with config-scoped values; Run/Debug resolve the active config.
- **Acceptance**: multiple configs round-trip and Run uses the active one.
- **Depends**: J2SE customizers, Run lifecycle.

## Ant runtime
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/ant-runtime`
- **Scope**: `ant.home`/`ant.version` + J2SE harness classpath (or reuse the Ant module executor);
  dedupe the two `AntBuildActionProvider`s.
- **Acceptance**: a real J2SE Ant project builds/runs in-process.
- **Depends**: none (pairs with Build actions).

## SceneBuilder embedding
→ detail: [`04-fxml-and-scenebuilder.md`](04-fxml-and-scenebuilder.md) · branch: `spike/scenebuilder-embedding`
- **Scope**: bundle `scenebuilder-kit:26.0.0` (+ `javafx-web`/`javafx-swing`/`jakarta.json`) via the
  external mechanism; render `SceneBuilderPane` in a bare view; load/save an FXML temp file; resolve
  classpath-vs-module-path and launcher flags.
- **Acceptance**: SceneBuilder renders, edits and saves in-IDE.
- **Depends**: none.

## JavaFX cluster
→ detail: [`03-part2-javafx.md`](03-part2-javafx.md) · branch: `feature/javafx-cluster`
- **Scope**: create the `nbfx-javafx` cluster and wire the repo harness (cluster.properties fragment,
  `nbfx.properties`, `build.xml` overlay macros, `check-purity`, launcher/rat patches).
- **Acceptance**: an (empty) `nbfx-javafx` cluster builds in the nbfx config.
- **Depends**: SceneBuilder embedding (for the lib module) optional.

## JavaFX project
→ detail: [`03-part2-javafx.md`](03-part2-javafx.md) · branch: `feature/javafx-project`
- **Scope**: JavaFX wizard pages/generator, templates in the chooser, modernised `jfx-impl.xml`.
- **Acceptance**: create/build/run a JavaFX Ant app.
- **Depends**: New Project templates, Ant runtime, JavaFX cluster.

## JavaFX customizers
→ detail: [`03-part2-javafx.md`](03-part2-javafx.md) · branch: `feature/javafx-customizers`
- **Scope**: JavaFX Application/Run/Packaging/Deployment panels on the J2SE customizer base.
- **Acceptance**: JavaFX categories round-trip.
- **Depends**: J2SE customizers, JavaFX project.

## FXML completion
→ detail: [`04-fxml-and-scenebuilder.md`](04-fxml-and-scenebuilder.md) · branch: `feature/fxml-completion`
- **Scope**: MIME `text/x-fxml+xml`, ported model/parser/beans, completers, diagnostics, codegen.
- **Acceptance**: completion in FXML from the project classpath.
- **Depends**: JavaFX cluster.

## SceneBuilder editor
→ detail: [`04-fxml-and-scenebuilder.md`](04-fxml-and-scenebuilder.md) · branch: `feature/scenebuilder-editor`
- **Scope**: `EditorDocumentProvider` for `.fxml`; Design ⇄ Source; save/sync; dependency scan;
  undo/redo bridge.
- **Acceptance**: visually edit an FXML app with source sync.
- **Depends**: SceneBuilder embedding, FXML completion.

---

**Sequencing:** Progress UI parity first, then drop the launcher's hard-wired progress, then the
status-elements registration fix, then the Wizard framework, then the **project modularity refactoring**
(so the New File/New Project UI lands directly in `nbfx.project.ui`), then the Part 1 steps roughly in
order. Several steps are independent: **Java Platforms** can run in parallel with the wizard/template
steps; **FX dialogs & notifications (UI)** and **Theming & branding** can start in parallel with the
wizard/New File topics (separate modules, no file overlap — the project-problems UI depends on the
dialogs step, and both foundations share the semantic colour token names); the **FxProject model** is
scheduled later, before project-type-aware template filtering. Then Part 2. Each step is sized to be
reviewable on its own.
