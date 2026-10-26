<!--

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
# 07 — Small-step execution backlog

The part docs describe *what* to build; this document slices it into **small, independently shippable
steps**, roughly ordered. Each step should merge on its own (build + guardrails green), so progress is
visible and risk stays low.

**Order: walking skeleton → API discipline → layers.** The first step is `feature/walking-skeleton`
(one project end-to-end plus the budget baseline), followed by `refactor/api-split` and
`refactor/api-naming` (the naming freeze), then the layer steps. The next ten steps and their owners are
the board in [`34-upstream-sync-and-budget.md`](34-upstream-sync-and-budget.md) §3. Progress UI parity is
already in flight and lands as part of M0.

Branch per step and the PR flow are in [`08-branching-and-workflow.md`](08-branching-and-workflow.md).
Each step is one branch (`feature/<topic>`) and one PR.

Legend per step: **Scope**, **Steps**, **Acceptance**, **Depends**.

## Layers (how to read this backlog)

The backlog follows the layer model of [`26-roadmap.md`](26-roadmap.md) —
`nbfx-platform` → `nbfx-ide` → `nbfx-java` → `nbfx-javafx` (with `libs.*` leaves). Every step belongs to
one layer; a capability may span layers (a `platform` service + an `ide` view + a `java` backend). The
module-by-module allocation is in [`27-layer-module-map.md`](27-layer-module-map.md); where the JavaFX
implementation should differ from the Swing original is in
[`28-beyond-swing-ui.md`](28-beyond-swing-ui.md).

| Layer | Milestone | Steps in this backlog |
|---|---|---|
| `nbfx-platform` | M0 | Progress UI parity, Progress launcher removal, Status elements, Wizard framework, FX dialogs & notifications, Theming & branding, Options (all `Options:` steps), Output window, Execution service, Terminal model/PTY/view/options, **Quick Search service**, **Credentials/keyring**, **Favorites**, Nodes & views API, Property sheet & editors |
| `nbfx-ide` | M1 | Project modularity, New File templates (+ per-template panels), New Project templates, Project model, Project problems UI, Views on nodes (migration), Java navigation & search, Refactoring, Java hints, Codegen & editor actions, Javadoc, Testing & coverage, VCS/diff/local history, Debugger UI steps, Profiler UI steps, Libraries, Task list/bookmarks, Plugins UI, Project groups |
| `nbfx-java` | M2 | Java Platforms, (J2SE/Platform) customizers, Build actions, Run lifecycle, Run configurations, Ant runtime, Java editor semantics, Java debug backend, Java profiler backends, Java test integration + coverage, Module support, i18n, Beans, JShell |
| `nbfx-javafx` | M3 | JavaFX cluster, JavaFX project, JavaFX customizers, FXML completion, SceneBuilder embedding/editor |
| `libs.*` | M0–M2 | JLine (terminal PTY), async-profiler per OS/arch (profiler), jgit/JaCoCo (reuse) |
| **cross-cutting** | M0/M1 | **Walking skeleton**, public-package inventory + `nbfx.api` split, capability naming, services-not-statics + typed contracts, structural guardrails, front-end component factory (spike + upstream), **plugin API + trust**, **Swing retirement guard**, **upstream sync**, **module moves & service extraction**, **SPI factory migration** |

**Execution board.** This backlog is the library of steps, not the schedule: the next ten steps, the
dependency graph and the owners live in [`34-upstream-sync-and-budget.md`](34-upstream-sync-and-budget.md)
§3, reviewed at every milestone (WIP limit: two verticals in flight).

---

## Walking skeleton (one project type end-to-end)
→ detail: [`30-api-governance.md`](30-api-governance.md) §11 · branch: `feature/walking-skeleton`
- **Scope**: before filling layers, make the application **usable end-to-end** for one project type:
  open a project → edit a file (tab, save) → build → run → output/status → commit. Deliberately boring,
  deliberately complete; it is the integration test for every later step and the baseline for the
  startup/latency budget (doc 30 §7).
- **Acceptance**: a developer can open/build/run/commit a real project in the app; startup and first-use
  numbers recorded in `11-project-state.md`; every later step keeps the skeleton green.
- **Depends**: the current shell, launcher, docking, output (basic), Git status (basic).
- **Ordering / parallel**: first — before the layer milestones ([`26-roadmap.md`](26-roadmap.md) §6).
- **Landed**: n/a.

## Public-package inventory + `nbfx.api` split
→ detail: [`30-api-governance.md`](30-api-governance.md) §1 · branch: `refactor/api-split`
- **Scope**: regenerate the **usage-derived inventory** (doc 30 §1.1: 25 exporting modules, 21 subtree
  exports, 13 empty; `nbfx.api` 25 consumers, `nbfx.annotations` 20, `nbfx.project` 10, …); assign every
  module a tier (T0/T1/T2) and every T0-with-consumers a **named move**; split
  `com.gluonhq.netbeans.nbfx.api` per capability **with the consumer counts and a deprecated shim per
  slice** (doc 30 §1.3; no split packages); create the T2 API/annotation modules and move implementations
  to `*.impl`/UI modules; give each API module its own specification version.
- **Acceptance**: the inventory table (doc 30 §1.1) is complete and accurate; every module is **tiered**
  (T0/T1/T2); no `<subpackages>` without a written justification; dependants bumped to the new API
  modules; build/tests green. The structural guard (next step) consumes this output — the guard does not
exist yet, so it is not an acceptance item here.
- **Depends**: none.
- **Ordering**: early — before the API spreads further (before M1's API freeze).
- **Landed**: n/a.

## Capability naming: remove the toolkit from the SPI
→ detail: [`31-front-end-apis.md`](31-front-end-apis.md) §3 · branch: `refactor/api-naming`
- **Scope**: classify and fix the **53 public production `Fx*` types** (doc 32 §5.3): **16 SPI** → capability
  names (`FxOptionsPanel`→`OptionsPanelController`, `FxProperty`→`PropertyModel`, `FxOutput`→`OutputConsole`,
  `FxWizard`→`WizardIterator`, …); **15 annotations** → `@XxxRegistration` in the T2 annotation modules;
  **9 processors** follow; **12 implementation widgets** → `…ui.fx`/privatised
  (`FxOptionsDialog`, `FxPropertySheet`, `FxStatusBar`, `FxWizardDialog`, …); **`FxLayer`** constants →
  private per reader. **Renames are source-compatible only** (same members/return types) — the factory
  shape change is `spi-factory-migration`, not this step. Mechanical, with **layer output invariance** and
  `apichanges.xml` entries.
- **Acceptance**: no toolkit name in an SPI type/annotation/public constant; build/tests green; generated
  layer identical. **Freeze**: mark every in-flight branch/PR with "rename required", record the
  known-violations baseline (**53 public production `Fx*` types**, 2026-10-18) and add the naming check to
  the guardrail step. Target names are **to create** — no doc may imply they exist.
- **Depends**: Public-package inventory (to know what is public).
- **Ordering**: before M1's API freeze; coordinate with doc 31's factory migration.
- **Landed**: n/a.

## SPI factory migration (options, views, wizards, property editors)
→ detail: [`31-front-end-apis.md`](31-front-end-apis.md) §2/§4 · branch: `spi-factory-migration`
- **Scope**: after `spike/front-end-component-factory` proves the pattern, change the component-producing
  SPIs one at a time to the typed factory (`OptionsPanelFactory<C>`, `ViewFactory<C>`,
  `WizardUi<C>`, `PropertyEditorFactory<C>`), with a deprecated shim for the old method and a Swing/test
  factory proving the abstraction. This is where `ViewProvider.getView(): Node` becomes factory-produced;
  it is **not** part of the rename.
- **Acceptance**: each migrated SPI has a headless/test factory and an FX factory; old clients compile with
  deprecation; no toolkit name in the SPI; `apichanges` + signature tests updated.
- **Depends**: `spike/front-end-component-factory`, `refactor/api-naming`.
- **Ordering**: after the factory spike; before the options/views UI steps.
- **Landed**: n/a.

## Services, not statics + typed contracts
→ detail: [`30-api-governance.md`](30-api-governance.md) §4/§5 · branch: `refactor/services-not-statics`
- **Scope**: replace mutable statics with lifecycle services in `Lookup` (`DebugSession.current`,
  `DebugStatus.ACTIVE`, `EditorBuffer.OPEN`, `CompletionPopup.cachedFooterWidth`); replace stringly-typed
  contracts — JPDA's `Object[] services` + `Map` keys (`"name"`/`"sourcepath"`/`"jdksources"`/`"baseDir"`) by
  typed session services, `PropertyModel.getEditorId()` by `PropertyEditorRef`, public layer-path constants
  by private ones; registries hold cached `Lookup.Result`s instead of calling `Lookups.forPath` per query
  (14 runtime sites).
- **Acceptance**: no mutable statics in public-package classes (check green); typed services in place and
  unit-tested with fakes; registries cached; no behaviour change.
- **Depends**: capability naming (same files).
- **Landed**: n/a.

## Structural guardrails (replace the grep checks)
→ detail: [`30-api-governance.md`](30-api-governance.md) §6 · branch: `refactor/guardrail-structural-checks`
- **Scope**: restate the objective (no Swing/AWT rendering; no forbidden module on the runtime path;
  toolkit-free models/SPIs; governed public packages) and enforce it structurally: module-graph check over
  the built clusters, `jdeps`/bytecode scan of the built jars (no `javafx.*` in model jars, no
  `javax.swing`/`java.awt`/`org.openide.nodes|explorer|windows|awt|dialogs` in SPI/model jars), a
  governance check (public package ⇒ `arch.xml` + `apichanges`), and a naming check (`Fx*`/`Sw*` in SPI
  packages). Run in the verify phase, fail with a report.
- **Acceptance**: the new checks replace `check-sources`/`check-dependencies` as the gate; they are **green
  on day one** with the current exemptions and fail only on new violations or expired exemptions; a
  deliberately broken fixture fails all of them; docs/rules updated.
- **Depends**: API split/naming (so the checks pass on the new shape).
- **Landed**: n/a.

## Front-end component factory (spike + upstream proposal)
→ detail: [`31-front-end-apis.md`](31-front-end-apis.md) · branch: `spike/front-end-component-factory`
- **Scope**: prototype the *one SPI, many toolkits* hook (`OptionsPanelFactory<C>` (typed, generic) or a `PanelModel`, resolved from `Lookup`) on
  **options** first: toolkit-free `OptionsPanelController` + `OptionsPanelFactory<Node>`, an FX factory in
  `nbfx.options.ui.fx`, and a Swing factory kept working in the upstream tree; then propose the hook to
  `apache/netbeans` (additive, no behaviour change). Establishes the pattern for wizard/dialogs/property
  sheet/views/progress/output (doc 31 §4).
- **Acceptance**: the options SPI is served by a factory; a headless test factory works (headless SPI
  tests); the upstream PR is opened with tests; the nbfx-side shim is deprecation-only.
- **Depends**: capability naming.
- **Ordering**: after `refactor/api-naming`, before the M1 API freeze.
- **Landed**: n/a.

## Plugin API + trust (T2 contract)
→ detail: [`32-api-tiers-and-plugin-contract.md`](32-api-tiers-and-plugin-contract.md) · branch: `feature/plugin-api`
- **Scope**: define the **published plugin API** (T2) from the tier model: the registration annotations
  (`@ViewRegistration`/`@ActionRegistration`/`@MenuRegistration`/`@OptionsRegistration`/`@WizardRegistration`/
  `@ProjectTypeRegistration`/`@PropertyEditorRegistration`/`@StatusRegistration`), the extension points a
  plugin may implement (views, options, nodes/property, project types, build/run, debug/profile/test/VCS
  backends), and the `Lookup` services; document what a plugin may **not** rely on. Add the plugin guide,
  signature tests and version ranges, and the trust path (NBM signature verification, HTTPS-only update
  centers, native-artifact checksums, install-time capability display) in `nbfx.plugins`.
- **Acceptance**: a sample plugin (NBM) compiles against the T2 modules **only** (no UI module on its
  classpath), installs with signature verification and works; the plugin dependency manifest is
  documented; T0 modules export nothing; T2-now modules carry signature tests; T2-proposed backends are
  marked as such; the trust policy is documented and enforced in the plugin manager.
- **Depends**: capability naming (T2 names), public-package split (tiers), plugins UI step.
- **Ordering**: after `refactor/api-split`/`api-naming`; before M1's API freeze.
- **Landed**: n/a.

## Swing retirement guard
→ detail: [`33-swing-retirement.md`](33-swing-retirement.md) · branch: `refactor/swing-retirement`
- **Scope**: put the retirement matrix into the build: the list of upstream modules to retire per
  milestone, the **exemption register** (module, why, owner, removal milestone), the cluster pruning
  (`-prune-excluded`) and the structural guard that fails when a current-milestone `retire` module is
  still present or an exemption is expired. Also the Swing-surface audit (jdeps over the cluster).
- **Acceptance**: the guard is green on the current state (exemptions for engine transitives, each with an
  owner and a date); the current milestone's `retire` modules are absent from the built distribution; the
  audit reports only exempt modules; the matrix rows carry the **measured** present/dependants state; the
  matrix and exemptions are updated per milestone.
- **Depends**: the M0 UI replacements; the guardrail redesign.
- **Ordering**: continuously from M0 onward; the guard gates each milestone exit.
- **Landed**: n/a.

## Upstream sync (cadence, patches, CI)
→ detail: [`34-upstream-sync-and-budget.md`](34-upstream-sync-and-budget.md) · branch: `chore/upstream-sync`
- **Scope**: establish the monthly submodule bump + patch rebase procedure, the **patch register**
  (why/upstream PR/owner/retirement milestone), the patch budget, the `pinned` and weekly `next-upstream`
  CI jobs, and the drift metric recorded in `11-project-state.md`; no new patch without a register row and
  an upstream issue.
- **Acceptance**: the procedure is documented and executed once; CI jobs exist; the patch register and
  drift metric are recorded; the first upstream proposal (component factory) is linked.
- **Depends**: none; run from M0 onward.
- **Landed**: n/a.

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

## Options system: infrastructure + example
→ detail: [`14-options.md`](14-options.md) · branch: `feature/options-foundation`
- **Scope**: complete the FX Options system on the existing `nbfx.options` stub — an nbfx-native SPI on
  the NetBeans `OptionsPanelController` contract (`getComponent(masterLookup)`, `update`, `applyChanges`,
  `cancel`, `isValid`, `isChanged`, `getLookup`, `getHelpCtx`, `handleSuccessfulSearch`,
  `PROP_CHANGED`/`PROP_VALID`), containers/sub-panels + `@FxOptionsKeywords`, a registry that reads
  `NbFx/Options` (single panels, containers, sub-panels), the window (category list, tabs, keyword
  search, change/validity with error markers, OK/Apply/Cancel, deep links) and a new `nbfx.settings`
  model module (typed `Settings` over `NbPreferences` + a JavaFX binding helper in `nbfx.options`).
- **Examples** (infrastructure with an example first): `Editor` as a **container** with a `General`
  sub-panel (line numbers, breadcrumbs, caret, wrap) and `Keymaps` as a top-level panel, both migrated to
  the new contract and bound through `Settings`.
- **Acceptance**: the window opens from Tools ▸ Options / `Shortcut+,`; tabs switch; search finds and
  selects; a change enables Apply; an invalid panel blocks OK with an error marker; values persist via
  `nbfx.settings` and are read back; `nbfx.settings` passes `check-models`; purity green.
- **Depends**: FX dialogs & notifications (chrome), Theming & branding (styles) — usable without them
  meanwhile (plain dialog).
- **Ordering / parallel**: **foundation first**; can run in parallel with the wizard/nodes/New File
  topics. The category steps below follow it; the parity work is sliced stepwise (see doc 14 §7).
- **Landed**: n/a.

## Options: settings/preferences browser (debug aid)
→ detail: [`14-options.md`](14-options.md) · branch: `feature/options-settings-browser`
- **Scope**: a preferences browser (`TreeView` over the `NbPreferences` tree: node/key/value/type,
  filter, add/edit/remove, live refresh) in the Advanced category — a development aid to inspect and
  debug the option panels and their keys; also lists the registered categories/panels/keywords.
- **Show/hide**: hidden by default; shown by `-Jnbfx.options.debug=true` or a persisted "Show advanced
  preferences" toggle; the Advanced category appears when it has panels or the browser is enabled.
- **Acceptance**: browse/filter/edit/remove preferences; toggle works; hidden by default; purity green.
- **Depends**: Options system infrastructure + example.
- **Landed**: n/a.

## Options: Appearance category
→ detail: [`14-options.md`](14-options.md) · branch: `feature/options-appearance`
- **Scope**: the `Appearance` container contributed by `nbfx.theme`, with the `Theme` sub-panel
  (System/Light/Dark, live switch, persisted) — `View ▸ Theme` remains a shortcut to the same state.
- **Acceptance**: the panel switches the theme and persists; it reflects `View ▸ Theme`.
- **Depends**: Theming & branding, Options infrastructure.
- **Landed**: n/a.

## Options: Editor + Fonts & Colors categories
→ detail: [`14-options.md`](14-options.md) · branch: `feature/options-editor`
- **Scope**: the `Editor` container tabs that have an effect in the nbfx JavaFX editor, in order:
  General (extend the example), Formatting/Indentation (tab size, expand tabs, indent), Code Templates,
  Folding, On Save, Hints/MarkOccurrences/InlineHints, Code Completion, Macros, AutoSave, Language
  Servers — each only once the editor feature exists; plus **Fonts & Colors** profiles (light/dark) over
  the `-code-*` tokens of `nbfx.theme`, selectable independently.
- **Acceptance**: each ported panel persists and visibly affects the editor; panels for editor features
  that do not exist yet are not listed (no dead panels); see doc 14's parity matrix.
- **Depends**: Options infrastructure, editor features.
- **Landed**: n/a.

## Options: Java/JavaFX category
→ detail: [`14-options.md`](14-options.md) · branch: `feature/options-java`
- **Scope**: the `Java` container — Java Platform (add/remove/rename, sources/javadoc, JavaFX home),
  Maven (home, settings.xml, offline), Gradle (home, JDK), Ant (home, properties), SceneBuilder
  (kit/home). Debugger/JShell/Form Editor/Nashorn stay out (later/dropped).
- **Acceptance**: each panel persists into the same storage the headless module reads (platform storage,
  Maven/Gradle settings).
- **Depends**: Java Platforms, build modules, Options infrastructure.
- **Landed**: n/a.

## Options: Miscellaneous/Advanced + Team categories
→ detail: [`14-options.md`](14-options.md) · branch: `feature/options-misc`
- **Scope**: the Advanced container with the settings browser and contributed tabs (Output, Diff,
  Terminal, GoTo, File Associations, Janitor, Versioning, Spellchecker, …) as those features land, plus
  the `Team` container (Versioning, ToDo, Issue Tracking).
- **Acceptance**: contributed tabs appear from their modules; the Advanced container only shows when it
  has panels (or the debug browser is enabled).
- **Depends**: Options infrastructure, the individual features.
- **Landed**: n/a.

## Options: export/import (incl. legacy NetBeans import)
→ detail: [`14-options.md`](14-options.md) · branch: `feature/options-export`
- **Scope**: mirror the NetBeans mechanism — declarative `NbFx/OptionsExport/<category>/<item>` entries
  with include/exclude patterns over userdir paths; export to a zip (`build.info`/`enabled-items.info`
  manifest); import from an nbfx zip **or userdir** (keyring/passwords excluded, restart prompt). A
  separate point imports an export/userdir **from the original NetBeans**: shared headless modules map
  path-for-path, the rest via a per-category key map with a report of skipped/unknown keys.
- **Acceptance**: export/import round-trips nbfx settings; a NetBeans export imports the mapped settings
  and reports the unmapped ones.
- **Depends**: Options infrastructure.
- **Landed**: n/a.

## Options: global Quick Search provider
→ detail: [`14-options.md`](14-options.md) · branch: `feature/options-quicksearch`
- **Scope**: a Search provider matching options keywords (`o …`) that opens
  `FxOptionsDisplayer.open(path)` and lets the target panel highlight the match.
- **Acceptance**: typing words lists matching options panels; selecting opens the right category/tab.
- **Depends**: Options infrastructure, Quick Search.
- **Landed**: n/a.

## Quick Search service (command palette)
→ detail: [`27-layer-module-map.md`](27-layer-module-map.md) · branch: `feature/quicksearch`
- **Scope**: a **platform** service — `SearchProvider` registry (reuse `platform/spi.quicksearch`),
  provider categories (actions, open files, recent, projects, options), and an FX **command-palette**
  popup (fuzzy matching, keyboard-only, preview, history) with a configurable key binding (doc 14
  keymap). Consumed by Find/Go-to (doc 18), the options search (doc 14) and feature providers
  (files, types, symbols, Git actions, run configurations).
- **Acceptance**: the palette lists providers, opens the selected item, is keyboard-driven and themed;
  headless tests for registry/matching; no JavaFX in the matching model.
- **Depends**: Theming, Dialogs (for the popup chrome), Options (keymap).
- **Ordering / parallel**: can run in parallel with the other M0 platform steps; a prerequisite for
  doc 18's Go-to/Quick Search.
- **Landed**: n/a.

## Credentials / keyring (platform service)
→ detail: [`27-layer-module-map.md`](27-layer-module-map.md) · branch: `feature/credentials`
- **Scope**: a **platform** credentials service over the NetBeans `keyring`/`keyring.impl`/
  `keyring.fallback` modules with an FX master-password/unlock prompt and a "remember credentials" flow;
  used by VCS (doc 24), update centers/plugins (doc 25), remote debug/profiler (docs 16/17) and update
  centers.
- **Acceptance**: prompts unlock once per session, credentials persist/upgrade with the platform scheme,
  a fallback is offered when the OS keyring is unavailable.
- **Depends**: Dialogs/notifications.
- **Landed**: n/a.

## Favorites (platform view)
→ detail: [`27-layer-module-map.md`](27-layer-module-map.md) · branch: `feature/favorites`
- **Scope**: a **platform** view: favourite folders (add favourite, drag & drop to reorder, open files),
  reusing `platform/favorites`' model where possible; integration with the navigator/editor.
- **Acceptance**: add/remove favourites, persist across restarts, open/paste into them.
- **Depends**: Nodes & views API (or the existing tree infrastructure).
- **Landed**: n/a.

## Split the annotation modules (ownership by tier)
→ detail: [`29-annotation-modules.md`](29-annotation-modules.md) · branch: `refactor/annotations-split`
- **Scope**: split `nbfx.annotations` so each registration annotation **and its processor** live in the
  module its **tier** dictates: T2 annotations → **annotation-only API modules**
  (`nbfx.api.options`, `nbfx.api.wizard`, `nbfx.api.property`, `nbfx.api.project` — no UI, so a plugin can
  compile against them); T1/internal annotations stay with their owning module. The core module keeps only
  shell registrations (actions/views/menus/toolbars/status) plus private path constants. Slices: options,
  wizard, property editors, project types, path-constant split, cleanup (compile-only deps for nbfx
  modules; normal compile deps for plugins; per-module processor tests).
- **Acceptance**: build/tests green per slice; **generated layer fragments identical**; a sample plugin
  compiles against the annotation/API modules **without any UI module**; adding a registration type no
  longer touches the shared platform module.
- **Depends**: **tier assignment (`refactor/api-split`) and the naming freeze (`refactor/api-naming`)**;
  slice 4 (project types) after the project API extraction.
- **Ordering / parallel**: after the tier assignment and rename; before the option/wizard/property-sheet/
  project steps to avoid churn.
- **Landed**: n/a.

## Module moves & service extraction (aligning plan with main)
→ detail: [`30-api-governance.md`](30-api-governance.md) §1.4–1.5 · branch: `refactor/module-moves`
- **Scope**: the moves the tier audit requires but no step owned:
  1. **`nbfx.propertysheet` → `nbfx-platform`** (doc 26 §3.1), **after** moving `ProjectPropertiesProvider`
     out of it (it imports `project.ui.api.ProjectKindProvider`/`ProjectKinds`; otherwise the move creates
     an upward edge), with the `PropertyModel`/`PropertyEditor` SPI extracted to `nbfx.api.property`
     (T2-FX) and the sheet UI kept as `…ui.fx`;
  2. **project model**: extract `nbfx.api.project` (`OpenProject`, `ProjectRegistry`, `ProjectOpener`,
     `ProjectIconProvider`; 14 consumers) into `nbfx.api.project` (T2-FX, `nbfx-ide`) **together with its
     `api.editor`/root consumers**, with a deprecated shim; move the `nbfx.project` wizards to
     `nbfx.project.ui` (depends on `refactor/project-modularity`), and move the `project → wizard` edge with
     them;
  3. **output service**: move `FxOutput`/`FxConsole` to `nbfx.api.output` (T1) and switch `run`/`debug` to
     it (or to `api.io`), keeping the console view in `nbfx.output.ui.fx`;
  4. **`FxLayer` constants**: move each path constant into its reader; fix `ActionLayerReader` to own
     `REF_SUFFIX` **and remove the `nbfx.api → nbfx.annotations` module edge** (prerequisite of
     `refactor/annotations-split`);
  5. **status/file**: create `nbfx.api.status` (T2-FX: `StatusElement` + `@StatusRegistration`) and
     `nbfx.api.file` (T2-FX: `DesktopActions`); keep the bars/actions as T1/T0; `docking`/`menus`/`windows`/
     `ui` stay T1 unless the consumed types move into the shell;
  6. **`EditorSettings` migration**: move `nbfx.api.editor.EditorSettings` (a model with `BooleanProperty`)
     to `nbfx.settings` (M, toolkit-free) + a UI adapter; update the editor and launcher consumers;
  7. **`project.ui.api` split**: headless build/run model → `nbfx.api.project.build` (M); UI parts stay T1.
- **Acceptance**: the audit table is updated; every moved module builds and the consumers compile; no
  package is split between modules; the branch-reality table (doc 30 §1.4) is reflected in the step
  dependencies.
- **Depends**: `refactor/project-modularity` (for the wizards/project model) and the tier assignment.
- **Ordering**: before `refactor/annotations-split` (constants) and before the output/debug/property steps.
- **Landed**: n/a.

## Output window (api.io + ANSI + FX view)
→ detail: [`15-output-and-terminal.md`](15-output-and-terminal.md) · branch: `feature/output-window`
- **Scope**: rewrite `nbfx.output` onto the clean `org.netbeans.spi.io.InputOutputProvider`/`api.io` API
  with a JavaFX view (one tab per console, virtualised styled text, not a `TextArea`): `print`/`println`,
  `OutputColor`, hyperlinks (→ editor), folds, positions, important/error lines, Reset/Clear, input
  prompts, line/byte limits, search, tab lifecycle/names. Port `core.output2`'s ANSI **SGR** colour
  handling (`OutWriter.printANSI`: CSI+SGR, xterm palette, bright/faint, carry-over across writes) onto FX
  colours; strip non-SGR control (that is the Terminal's job). Keep `FxOutput`/`FxConsole` as a thin
  facade while migrating callers.
- **Acceptance**: build/test output shows ANSI colours, clickable file/line links and folds; input prompts
  work; limits trim; callers migrated; tests for SGR parsing and the view model.
- **Depends**: Theming & branding (styles).
- **Ordering / parallel**: can start in parallel with the wizard/options/nodes topics (one module); land
  before the build-provider integration so output is worth targeting.
- **Landed**: n/a.

## Execution service (process model + output pumps)
→ detail: [`15-output-and-terminal.md`](15-output-and-terminal.md) · branch: `feature/execution-service`
- **Scope**: a headless `nbfx.execution` model — `ExecutionService`/`ExecutionDescriptor` (argv, working
  dir, `Environment`, charset, redirects, output mode `PIPE|PTY`, cancel, exit listener),
  `ExecutionHandle` (cancel, exit status, stdin writer) and output pumps (out/err split, line/raw modes)
  with a progress hook. Reuse `ide/extexecution.base`/`extexecution.process` where it fits (clean,
  Swing-free) and do **not** depend on `ide/extexecution` (legacy `InputOutput` coupling). Adopt it in
  `nbfx.run` (`ProcessRunner`).
- **Acceptance**: start/cancel/exit, env/cwd/charset, distinct out/err, back-pressure; `nbfx.execution`
  passes `check-models`.
- **Depends**: none.
- **Ordering / parallel**: parallel with the Output-window/terminal-model steps; the PTY backend and the
  run integration depend on it.
- **Landed**: n/a.

## Terminal emulator model (VT/xterm)
→ detail: [`15-output-and-terminal.md`](15-output-and-terminal.md) · branch: `feature/terminal-model`
- **Scope**: a fresh headless VT/xterm model in `nbfx.terminal.model` (screen + scrollback, attribute
  runs, cursor, resize, input encoder, OSC title/cwd, alt-screen, line discipline), **porting the useful
  pieces** of `ide/lib.terminalemulator` (CSI/SGR/DEC-mode patterns from `InterpDtTerm`/`InterpANSI`/
  `InterpProtoANSI`, `Ops`, `Attr`/`AttrSave`, `LineDiscipline` semantics, `StreamTerm`; the upstream
  `TermTest` as fixtures) and writing the rest fresh (buffer/scrollback structures, public API with
  `java.beans`-style events, FX key → bytes encoder, 256/truecolor + theme mapping). Do not port the Swing
  `Term`/`Screen`.
- **Acceptance**: escape-sequence fixture suite green (movement/erase/SGR/DEC modes/scroll regions/tabs/
  OSC/alt-screen/echo), resize + scrollback behaviour, no JavaFX in the module (`check-models`).
- **Depends**: none.
- **Ordering / parallel**: can run in parallel with the Output-window/execution steps.
- **Landed**: n/a.

## Terminal PTY backend (JLine)
→ detail: [`15-output-and-terminal.md`](15-output-and-terminal.md) · branch: `feature/terminal-pty`
- **Scope**: the PTY implementation of the execution seam over **JLine 3** (`jline-terminal` +
  `terminal-jna`/`terminal-ffm`, `org.jline.terminal.impl.exec.ExecPty`/`ExecTerminalProvider`, per-OS
  termios mappings, signal handling): a `PtyProcess` interface (stdin/stdout bytes, resize, signals, exit
  status, `tty` name) that keeps the backend swappable. Add a `libs.jline` external module (like
  `libs.jna`) and align the JLine version with the platform's JShell (BSD-3, already accepted as
  `BSD-jline3`); licence entries.
- **Acceptance**: a shell runs on a PTY with correct echo/raw mode and window size; resize and signals
  work; exit status reported; no Swing; purity green.
- **Depends**: Execution service.
- **Landed**: n/a.

## Terminal view (JavaFX)
→ detail: [`15-output-and-terminal.md`](15-output-and-terminal.md) · branch: `feature/terminal-view`
- **Scope**: the JavaFX terminal renderer over `nbfx.terminal.model` — `Canvas` glyph grid (attributes as
  runs), scrollback, cursor blink, selection + copy/paste, find, font zoom, resize → PTY size, theme
  colours (ANSI 16 + 256); a `ViewProvider` with tabs (New/Close/Rename/Pin), "Open Terminal Here", "Run
  in Terminal", "Open in External Terminal".
- **Acceptance**: `vim`/`top` render and respond; selection/copy; resize reflows/re-wraps per policy;
  light/dark themes; headless view tests.
- **Depends**: Terminal emulator model, Terminal PTY backend, Theming & branding.
- **Landed**: n/a.

## Run/build output integration (Terminal default; Maven as a process)
→ detail: [`15-output-and-terminal.md`](15-output-and-terminal.md) · branch: `feature/run-output-integration`
- **Scope**: run is always a **separate process** (`java` with the computed classpath/module path) with
  **Terminal (PTY) as the default** and an `Output | Terminal` choice per run configuration; **Maven moves
  to a process entirely** (`mvn`/`mvnw` with the options-system home/`settings.xml`/offline and the JDK
  environment; cancel = destroy the process tree; the embedder stays only for the project model); Ant
  output via `BuildListener`/`LogListener`; Gradle CLI with `--console=plain`; tab naming
  `project (build|run|test)`; Output actions (Clear/Clear All/Find/Next-Prev Error/Rerun); error lines →
  editor hyperlinks.
- **Acceptance**: a run with stdin and `System.console()` works in the terminal; `mvn package` runs as a
  process, shows output, and cancels; Ant/Gradle output lands in the Output window; no in-process Maven
  goal execution remains.
- **Depends**: Output window, Execution service, Terminal view, Maven/Gradle/Ant project support, Java
  Platforms.
- **Ordering**: last of the output/terminal steps.
- **Landed**: n/a.

## Terminal/Output options
→ detail: [`15-output-and-terminal.md`](15-output-and-terminal.md) · branch: `feature/terminal-options`
- **Scope**: **Terminal** options panel (font family/size, palette/theme, scrollback, cursor shape/blink,
  shell/argv, environment, line discipline, bell) and **Output** options (line limit, colours, timestamps),
  persisted via `nbfx.settings` and registered through the Options system (doc 14).
- **Acceptance**: the panels persist and visibly affect the terminal/output; theme-aware.
- **Depends**: Options system infrastructure + example, Terminal view, Output window.
- **Landed**: n/a.

## Remote terminal (later)
→ detail: [`15-output-and-terminal.md`](15-output-and-terminal.md) · branch: `feature/terminal-remote`
- **Scope**: remote terminals (SSH sessions) reusing the terminal view and a remote PTY/exec channel
  (`dlight.terminal` analogue).
- **Depends**: Terminal view.
- **Landed**: n/a.

## Debug foundation (JPDA session + launch/attach + FX renderer)
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-foundation`
- **Scope**: split the debugger into a headless model and a JavaFX UI. New `nbfx.debug.core` (JavaFX-free,
  `check-models`): `DebugLaunchRequest`/`LaunchResolver` (port of `DebugLauncher`), `DebuggerSession`
  (states `STARTING/RUNNING/STOPPED/DISCONNECTED`, `JPDADebugger.attach(host, port, services)` with the
  documented `name`/`sourcepath`/`jdksources`/`baseDir` map, listeners, dedicated executor),
  `DebuggerSessions`, `JdwpAddress` (JDWP args + `Listening on …` parsing with a free-port fallback),
  `DebugActions` (`ActionsManager` `ACTION_*` → nbfx `Command`s + enablement). Debuggee launched with
  `nbfx.execution` (Terminal default), engine only attaches. `nbfx.debug` gets an FX
  `DebugOutlineView` renderer over the new view models; migrate `DebugView`.
- **Acceptance**: Debug File starts a JVM in the Terminal, attaches, hits a caret breakpoint, shows
  threads/call stack/variables, steps and stops; `nbfx.debug.core` has no JavaFX.
- **Depends**: Output window + Execution service (doc 15), Theming (styles).
- **Ordering / parallel**: first debug step; can start once the execution seam exists (doc 15).
- **Landed**: n/a.

## Debug views (sessions, threads, call stack, variables, watches, sources)
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-views`
- **Scope**: the nbfx-native view-model layer (`DebugNode`/`DebugTreeModel`/`DebugTableModel`/
  `DebugAsynchronousModel`) and the models (threads with groups and lazy children, call stack, variables —
  lazy this/args/locals/fields/statics/arrays — watches, sources, sessions, results), plus the FX views
  with columns, context menus, lazy expansion and copy; view state persisted.
- **Acceptance**: all views render and update on stop/thread/frame change; watches add/remove/reorder;
  values expand lazily; no FX-thread blocking.
- **Depends**: Debug foundation.
- **Landed**: n/a.

## Debug breakpoints (view, types, customizers, editor glyphs)
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-breakpoints`
- **Scope**: `BreakpointManager` + descriptors for line/method/exception/field/class-load/thread
  breakpoints with the full field set (condition, hit count, suspend policy, filters, print text, group,
  enabled, hidden) and engine persistence; FX customizers per type; the Breakpoints view (grouping, hit
  counts, enable/disable, go-to) and editor annotations (enabled/disabled/conditional/invalid) + gutter
  toggle.
- **Acceptance**: create/disable/delete each type from the editor and the view; conditions and hit counts
  work; breakpoints survive restart; `breakpoints active` toggles.
- **Depends**: Debug views, editor annotation SPI.
- **Landed**: n/a.

## Debug stepping & editor integration
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-stepping-editor`
- **Scope**: all stepping actions (step over/into/out, step operation, run into method, run to cursor,
  pop topmost call, make caller/callee current) with smart stepping; current-line/current-frame highlight;
  source navigation (frame → editor, caret ↔ frame via `DebugContext`/`ContextProvider`); JDK-sources
  resolution for stepping into libraries.
- **Acceptance**: stepping follows source; the highlight moves; pop/run-to-cursor work; the call-stack
  context menu matches NetBeans.
- **Depends**: Debug views, editor.
- **Landed**: n/a.

## Debug evaluate (expression, watches, value editors)
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-evaluate`
- **Scope**: evaluate window (`JPDADebugger.evaluate`, completion), watches from selection, variable
  formatters, value editors (string/number/boolean/char/array/object `toString`), multiline/big strings,
  numeric display filter, value tooltips.
- **Acceptance**: evaluating an expression returns a value; editing a primitive/string writes back;
  formatters apply.
- **Depends**: Debug views.
- **Landed**: n/a.

## Debug attach & monitors
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-attach-monitors`
- **Scope**: attach dialog (host/port) + "Debugging by socket"; monitors view + deadlock detection
  ("Check Deadlocks"); suspend/resume; session restart.
- **Acceptance**: attach to a running JVM; monitors list; deadlock check reports.
- **Depends**: Debug foundation.
- **Landed**: n/a.

## Debug build integration (Debug Project/Test; Ant/Maven/Gradle)
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-build-integration`
- **Scope**: Debug Project/File/Test via run configurations; Ant/Maven/Gradle debug goals through
  `nbfx.execution` (doc 15, Terminal default); JUnit debug; stop (process tree) and relaunch; debuggee
  output in Terminal/Output.
- **Acceptance**: "Debug Project"/"Debug Test" launch and break for Ant, Maven and Gradle projects; stop
  kills the process tree.
- **Depends**: Debug foundation, Run configurations, Maven/Gradle/Ant project support, Java Platforms.
- **Landed**: n/a.

## Debug options (Java Debugger panel)
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-options`
- **Scope**: the Java Debugger options panel (transport/launcher defaults, step filters, formatters) via
  the Options system (doc 14).
- **Acceptance**: the panel persists and affects new sessions.
- **Depends**: Options system infrastructure + example.
- **Landed**: n/a.

## Debug remote (later)
→ detail: [`16-debugging.md`](16-debugging.md) · branch: `feature/debug-remote`
- **Scope**: remote/SSH debug sessions (`dlight`-style).
- **Depends**: Debug attach & monitors.
- **Landed**: n/a.

## Profiler foundation (session, backend SPI, JFR lifecycle, analysis model)
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-foundation`
- **Scope**: `nbfx.profiler.core` (JavaFX-free, `check-models`): `ProfilerSession`/`ProfileRequest`
  (launch/attach), the `ProfilerBackend` SPI, `JfrBackend` (recording start/stop/dump/save/load,
  `-XX:StartFlightRecording…` + `FlightRecorderMXBean`), the analysis-model skeleton (stack tree/CCT,
  flat profile, allocations, locks, telemetry, source mapping), the snapshot store, and reuse of the
  headless `lib.profiler` parts (HPROF heap parser + `.nps` reader). `nbfx.profiler.ui`: the profiler
  window + snapshot list. JDK module wiring (`jdk.jfr`, `jdk.management.jfr`, `jdk.attach`,
  `jdk.management`); basic options entry.
- **Acceptance**: profile a Java app end to end (launch with JFR, stop, open the recording, see the model);
  `nbfx.profiler.core` has no JavaFX; JDK modules documented.
- **Depends**: Execution service (doc 15).
- **Ordering / parallel**: first profiler step; can start once the execution seam exists.
- **Landed**: n/a.

## Profiler: open recordings (`.jfr`, `.hprof`, `.nps`)
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-recordings`
- **Scope**: file loaders + a Profiler data object/loader with "Open in Profiler"/"Open Heap
  Walker"/"Compare" actions, drag & drop and recent files; map JFR (JFR and async-profiler), HPROF and
  legacy `.nps` into the shared analysis model; exports (collapsed stacks, HTML flame graph, flat-profile
  CSV).
- **Acceptance**: opening each format shows the right views; `.nps`/`.hprof` files from NetBeans open;
  exports round-trip.
- **Depends**: Profiler foundation.
- **Landed**: n/a.

## Profiler: streaming JFR (live views)
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-streaming`
- **Scope**: incremental aggregation (bounded memory, ring buffers, spill-to-file),
  `RecordingStream`/`FlightRecorderMXBean.openStream`, live refresh cadence, snapshot-while-recording,
  pause/resume, `jfrsync` timeline alignment with async-profiler.
- **Acceptance**: a running app's profile updates live; snapshot-while-recording yields a consistent
  snapshot; memory stays bounded on long runs.
- **Depends**: Profiler foundation.
- **Landed**: n/a.

## Profiler: async-profiler backend (bundled per OS/arch)
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-async-profiler`
- **Scope**: the per-OS/arch `libs.asyncprofiler.{macos-x64,macos-aarch64,linux-x64,linux-aarch64}`
  modules (vendored `libasyncProfiler.{so,dylib}` + Apache-2.0 license + a lib-path service), gated by
  `org.openide.modules.os.*`/`.arch.*`; `AsyncProfilerBackend` (launch `-agentpath` with JFR output,
  dynamic attach via `jdk.attach`), parsing via `jdk.jfr.consumer`, options, and the JFR fallback where
  no module exists (Windows/unsupported arch).
- **Acceptance**: async-profiler profiles CPU/alloc/lock on macOS and Linux (launch and attach); the
  recording opens in the same views as JFR; missing module → JFR fallback with a clear message.
- **Depends**: Profiler foundation.
- **Landed**: n/a.

## Profiler: CPU views (call tree, flat, flame graph)
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-cpu`
- **Scope**: the CPU view (call tree/hotspots/callers/callees), flat profile table, **flame graph**
  (Canvas), go-to-source from frames, save/load/compare snapshots.
- **Acceptance**: hot methods rank correctly; the flame graph matches the tree; double-click opens the
  source.
- **Depends**: Profiler foundation.
- **Landed**: n/a.

## Profiler: memory views (allocations, live set, GC)
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-memory`
- **Scope**: allocations by class and allocation site, live set, GC/memory telemetry.
- **Acceptance**: allocation hotspots identify the right classes/sites; numbers match a reference profile.
- **Depends**: Profiler foundation.
- **Landed**: n/a.

## Profiler: locks & threads views
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-locks-threads`
- **Scope**: contended monitors/blocked time, thread-state timeline, thread dumps.
- **Acceptance**: lock contention is attributed to the right monitor/owner; thread dumps render.
- **Depends**: Profiler foundation.
- **Landed**: n/a.

## Profiler: attach & live sampler
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-attach-sampler`
- **Scope**: attach to a running JVM (`jdk.attach` list/attach + `FlightRecorderMXBean` start/stop/dump);
  the JMX live sampler view (thread states/hot stacks, no recording file).
- **Acceptance**: attach to a running JVM and record; the sampler shows live thread activity.
- **Depends**: Profiler foundation.
- **Landed**: n/a.

## Profiler: heap walker
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-heap`
- **Scope**: heap dump via `HotSpotDiagnosticMXBean.dumpHeap`, heap model reusing
  `org.netbeans.lib.profiler.heap`, FX heap walker (classes/instances/fields/references, dominators,
  retained size, merge paths); memory-lint rules later.
- **Acceptance**: walk a known object graph (retained size sanity); open a `.hprof` file; large dumps stay
  responsive.
- **Depends**: Profiler foundation, open recordings.
- **Landed**: n/a.

## Profiler: options panel
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-options`
- **Scope**: the Profiler options panel (backend selection JFR/async-profiler, event mode/interval,
  continuous/`loop`, lib path override, JFR `settings`/`maxAge`/`maxSize`, snapshot dir, filters/root
  methods) via the Options system (doc 14).
- **Acceptance**: settings persist and affect new sessions.
- **Depends**: Options system infrastructure + example.
- **Landed**: n/a.

## Profiler: snapshot tracer, OQL, profiling points, diff (later)
→ detail: [`17-profiling.md`](17-profiling.md) · branches: `feature/profiler-snaptracer`,
`feature/profiler-oql`, `feature/profiler-ppoints`, `feature/profiler-compare`
- **Scope**: JFR-based timeline/tracer, OQL over heap dumps, profiling points (snapshot/heapdump/start/
  stop/stopwatch), the snapshot compare view.
- **Depends**: the respective earlier steps.
- **Landed**: n/a.

## Profiler: legacy agent backend (optional, high risk)
→ detail: [`17-profiling.md`](17-profiling.md) · branch: `feature/profiler-legacy-agent`
- **Scope**: raise the NetBeans agent's class-file cap to JDK 25, rebuild the native libs and validate —
  only if the instrumented-call-count parity is ever needed; default off.
- **Depends**: —
- **Landed**: n/a.

## Java navigation & search (editor intelligence foundation)
→ detail: [`18-navigation-and-search.md`](18-navigation-and-search.md) · branches:
`feature/editor-intelligence-core`, `feature/goto-search`, `feature/editor-navigation`,
`feature/find-usages-parity`, `feature/hierarchies`
- **Scope**: the missing daily-driver navigation. **Foundation**: `nbfx.editor.intelligence` (headless) —
  `SemanticAccess` over `JavaSource`/`WorkingCopy` (already available via the existing `java.source.base`
  dependency), `SourceOpener` (file+offset → editor), and the provider SPIs (`GotoProvider` for
  File/Type/Symbol/Line/Package/Member, `NavigationProvider` for declaration/implementation/overridden/
  super, `UsagesProvider`, `HierarchyProvider` for call/type). **UI**: keyboard-first FX **Go to** popup +
  global **Quick Search** (reuse `platform/spi.quicksearch`, port `ide/jumpto` provider logic), Ctrl/Cmd-click
  **hyperlinks**, **Find Usages parity** (extend `nbfx.findusages`: scopes, read/write filters, tree/flat,
  actions, progress), **Call/Type Hierarchy** views (port `java.navigation` models). `Navigate` menu +
  editor context menu + keymap entries.
- **Acceptance**: Ctrl/Cmd-click and Go to Declaration/Implementation work (own code + libraries); Go to
  Type/Symbol finds JDK/project elements; Find Usages matches the original for fixtures; hierarchies expand
  and navigate; `nbfx.editor.intelligence` is JavaFX-free; headless provider tests.
- **Depends**: Options (keymap, doc 14), Nodes & views / property sheet (docs 12/13) for the
  usages/hierarchy views, `nbfx.editor`.
- **Ordering / parallel**: first of the Java-intelligence wave; gates refactoring/hints/codegen.
- **Landed**: n/a.

## Refactoring
→ detail: [`19-refactoring.md`](19-refactoring.md) · branches: `feature/refactoring-core`,
`feature/refactoring-rename`, `feature/refactoring-java`, `feature/refactoring-actions`
- **Scope**: reuse the UI-agnostic core of `ide/refactoring.api` (`Refactoring`, `RefactoringSession`,
  `RefactoringPlugin`, `Problem`, `Progress`) and the Java plugins from `java/refactoring.java` (port the
  Swing `ui/*`), editing through `java.source.base` (`ModificationResult` + one `UndoManager` edit).
  Steps: **core** (manager/scope/execution + preview model), **rename** (plugin, inline rename, options,
  preview, undo, Instant Rename), **Java refactorings** (move/copy/delete/safe delete, introduce
  variable/constant/field/method/parameter, inline, encapsulate fields, change parameters, pull up/push
  down, extract interface/superclass), **actions & UI** (Refactor menu/context menu/shortcuts, generic
  preview/parameters panel, "Refactor Usages" integration).
- **Acceptance**: rename with correct preview/options/undo; the other refactorings match NetBeans on
  fixtures; context enablement; off-FX execution with progress/cancel; `nbfx.refactoring.core` JavaFX-free.
- **Depends**: Java navigation & search (usages/hierarchy + semantic foundation).
- **Ordering**: after Part 18; `rename` first.
- **Landed**: n/a.

## Java hints / inspections
→ detail: [`20-java-hints.md`](20-java-hints.md) · branches: `feature/java-hints-core`,
`feature/java-hints-editor`, `feature/java-hints-fixes`, `feature/java-hints-options`
- **Scope**: reuse `java/spi.java.hints` (SPI + `HintsInvoker`) and the implementations in `java/java.hints`
  (274 files, mostly logic) — `OrganizeImports`, `SurroundWithTryCatch*`, error/style hints; **engine**
  (run on `CompilationInfo`, severities, debounce/caching/cancel), **editor integration** (annotations,
  gutter, hover, error stripe, next/prev hint), **lightbulb & fixes** (Alt+Enter popup, apply fix /
  fix all, atomic undo; Fix/Organize Imports and Surround With surfaced as commands), **configuration**
  (Editor ▸ Hints options page, doc 14) + project/dependency hints (`ant.hints`/`maven.hints`).
- **Acceptance**: the same hints/severities as NetBeans on fixtures; Alt+Enter offers the same fixes;
  applying is atomic and undoable; options apply live; `nbfx.java.hints.core` JavaFX-free.
- **Depends**: Java navigation & search (semantic foundation), editor annotations, Options (doc 14).
- **Landed**: n/a.

## Code generation & editor actions
→ detail: [`21-codegen-and-editor-actions.md`](21-codegen-and-editor-actions.md) · branches:
`feature/codegen-generate`, `feature/fix-imports`, `feature/editor-actions-parity`
- **Scope**: **Generate** over `GeneratorUtilities` (`java.source.base`) with an FX dialog (constructor,
  getters/setters, override/implement, `equals`/`hashCode`/`toString`, delegate, add property); **Fix/
  Organize Imports** and **Surround With** via the hints engine (doc 20); **Insert Javadoc**; and **editor
  action parity** (format/reformat, fold, indent, insert template, toggle comment, mark occurrences,
  bracket matching, error-stripe navigation, macros) with keymap entries — using the settings from the
  Editor options (doc 14).
- **Acceptance**: generate/inserts are undoable and import-correct; fix/organize imports resolve fixtures;
  surround-with wraps selections; the editor actions work from menus/context/shortcuts.
- **Depends**: Java hints (for imports/surround), Options (Editor).
- **Landed**: n/a.

## Javadoc viewing & completion
→ detail: [`22-javadoc.md`](22-javadoc.md) · branches: `feature/javadoc-view`, `feature/javadoc-index`
- **Scope**: `JavadocResolver` (element → doc comment / JDK javadoc via `ClassPath`/`DocTreePathHandle`,
  reuse `java/javadoc` index logic), FX popup/window + F1 + context actions, "Show Javadoc in Browser",
  the dockable Javadoc window with search, and the editor's Javadoc completion contributor; insert-Javadoc
  comment (shared with doc 21).
- **Acceptance**: F1 on own/library elements shows the doc; the Javadoc window follows selection and
  searches; completion offers tags/params; no FX in the resolver.
- **Depends**: Java navigation & search; Java Platforms (sources/javadoc roots, doc 02).
- **Landed**: n/a.

## Testing & code coverage
→ detail: [`23-testing-and-coverage.md`](23-testing-and-coverage.md) · branches: `feature/test-runner`,
`feature/test-results-ui`, `feature/test-run-configs`, `feature/code-coverage`
- **Scope**: reuse `java.testrunner`/`junit`/`testng`/`maven.junit`/`gradle.test` logic (port the Swing
  results UI): the **Test Runner** SPI + discovery (file/package/project) over `nbfx.execution`, report
  parsers (JUnit XML/Surefire/TNG); the dockable FX **Test Results** view (session/test tree, output,
  statistics, go-to-failure, re-run failed); **Test/Run/Debug Test** actions + test run-configurations
  (doc 02, doc 16); **code coverage** via the bundled **JaCoCo** (`org.jacoco.core`, EPL-2.0, already
  shipped with notice) and the `gsf.codecoverage` model, with an FX coverage view and editor line
  highlighting.
- **Acceptance**: Test File/Project works for Ant/Maven/Gradle with results and go-to-failure; Debug Test
  hits breakpoints; coverage numbers match JaCoCo; `nbfx.test.core` JavaFX-free.
- **Depends**: Run configurations (doc 02), Output/Execution (doc 15), Debug (doc 16) for Debug Test.
- **Landed**: n/a.

## VCS, diff & local history
→ detail: [`24-vcs-diff-history.md`](24-vcs-diff-history.md) · branches: `feature/vcs-core`,
`feature/vcs-git`, `feature/diff-and-patch`, `feature/local-history`, later `feature/vcs-hg`/`feature/vcs-svn`
- **Scope**: reuse `ide/versioning.core` + `versioning.masterfs` (status/annotations) and `versioning.util`
  (history/diff-sidebar model); **VCS core** (status cache, tree badges/annotations, menus/actions);
  **Git** over **jgit** (`ide/o.eclipse.jgit*`, EDL) or the CLI: status view, commit (selection + diff
  preview + amend), history/log with diffs, blame, branches/tags/remotes, credentials
  (`platform/keyring`), revert/reset, ignore list, conflicts; **Diff & patch** reusing `ide/diff`'s engine
  + `spi/diff` with an FX side-by-side/merge view, "Diff to…", apply/export patch; **Local history** from
  `ide/localhistory` (per-file timeline + revert).
- **Acceptance**: badges + Git actions; commit/history/diff/blame/branch/remote/ignore work on a local repo;
  patch apply/export round-trip; local-history revert; `nbfx.vcs.core` JavaFX-free; operations cancellable.
- **Depends**: Nodes & views (docs 12/13) for badges/annotations; Notifications (09); Progress (06).
- **Landed**: n/a.

## Misc Java basics (libraries, modules, task list, i18n, beans, groups, plugins, JShell)
→ detail: [`25-misc-java-features.md`](25-misc-java-features.md) · branches: `feature/libraries-manager`,
`feature/module-support`, `feature/tasklist-bookmarks`, `feature/i18n`, `feature/beans`,
`feature/project-groups`, `feature/plugins-ui`, `feature/jshell`
- **Scope**: **Libraries manager** — reuse `ide/project.libraries` (**no forbidden deps**,
  `org.netbeans.api.project.libraries.*`) + port `project.libraries.ui` (manager dialog, Libraries node,
  J2SE customizer integration); **module support** (module-info completion/highlighting + the
  `java.module.graph` dependency view); **task list & bookmarks** (`tasklist.*`, `editor.bookmarks`
  providers/models, FX views, counts, options); **i18n** (`java/i18n`: insert internationalized string +
  wizard); **JavaBeans** (`java/beans`: bean patterns, Add Property); **project groups** (`ide/projectui`);
  **plugins UI parity** (extend `nbfx.plugins`: update centers, proxy, install/update progress);
  **JShell** (`jdk.jshell` + `jshell.support` logic, FX console reusing the terminal/output stack); plus
  the **explicit out-of-scope list** (free-form/Eclipse import, JavaEE/web/JPA/Spring, form editor, Java
  ME, WebStart, native-image, other languages, databases/servers, JavaHelp topics).
- **Acceptance**: per item (libraries add/remove + build resolve; module graph; task list/bookmarks; i18n;
  Add Property; groups; plugin install/update; JShell snippets).
- **Depends**: mostly independent; plugins need `autoupdate.services` (already a dep); JShell needs doc 15.
- **Landed**: n/a.

## Nodes & views API (NodeModel/ChildrenModel/PropertyModel + FX views)
→ detail: [`12-nodes-and-views.md`](12-nodes-and-views.md) · branch: `feature/nodes-api`
- **Scope**: the headless node model behind trees, tables and the Properties window — `NodeModel`,
  `ChildrenModel` + `ChildFactory<T>` (lazy/async children), `PropertyModel`/`PropertySetModel`
  (promoted from the existing `FxProperty`/`FxPropertySet`, with the full contract in
  [`13`](13-property-sheet-and-editors.md)), `ExplorerContext` (root + selection) — plus FX view
  components over it. Replaces the per-view models of the project navigators, the Structure view and the
  property provider, mirroring NetBeans' `org.openide.nodes`/`openide.explorer` split.
- **Parts**: `nbfx.nodes` (model, JavaFX-free: `java.beans` events + `Lookup` for content, no Cookie API);
  `nbfx.nodes.ui` (tree/tree-table/table/list views over `ExplorerContext`, FX observable adapters,
  node actions/context menu). Action/new-type references are plain ids the UI resolves. Property editors,
  the property sheet and the outline columns are the **next step**.
- **Acceptance**: model unit tests green; `nbfx.nodes` passes the `check-models` guardrail (no JavaFX);
  one model drives several views; guardrails green.
- **Depends**: none.
- **Ordering / parallel**: **can start in parallel** with the wizard/theming/dialogs/New File topics — it
  only adds two new modules. It must land **before** the property-sheet step, the `ProjectModel` node
  factories (`@ProjectNodeFactory`) and the navigator/Structure/Properties parity work, so nodes are
  written once.
- **Landed**: n/a.

## Property sheet & editors (PropertyModel, editor SPI, outline columns)
→ detail: [`13-property-sheet-and-editors.md`](13-property-sheet-and-editors.md) · branch: `feature/property-sheet-editors`
- **Scope**: make the property sheet **and** the outline/tree-table columns work off one property model.
  Includes: the `PropertyModel`/`PropertySetModel` contract completion (identity, editor id,
  `format`/`parse`, sort key, allowed values, async `LOADING`/`ERROR` states, error contract),
  multi-selection aggregation (`AggregatePropertyModel`/`AggregatePropertySetModel`), the editor SPI +
  built-ins (text, boolean, number, enum/combo, file/folder, color, custom) with the commit/revert and
  `PropertyEnv` validity protocol, the property sheet (sets as groups, expert, `"<mixed>"`, persistence,
  keyboard), `PropertyColumn` + the outline cell binding/inline editing, and the migration of
  `nbfx.propertysheet`'s `FxProperty*`/`FxPropertyEditor*`/`FxPropertySheet` onto the node model.
- **Acceptance**: editing a cell in the outline updates the property sheet and vice versa; multi-node
  selection aggregates correctly; invalid input keeps the old value; light/dark; `nbfx.nodes` still
  JavaFX-free; tests: model, aggregation, editors, sheet, outline (see doc 13 §8).
- **Depends**: Nodes & views API.
- **Ordering / parallel**: additive (extends `nbfx.nodes`/`nbfx.nodes.ui`, rewrites
  `nbfx.propertysheet`); do it **before** `refactor/views-on-nodes` so the navigators and Structure use
  the final editor/column stack. It does not touch the navigator/Structure sources, so it can run in
  parallel with unrelated feature work.
- **Landed**: n/a.

## Views on nodes (migration)
→ detail: [`12-nodes-and-views.md`](12-nodes-and-views.md) · branch: `refactor/views-on-nodes`
- **Scope**: migrate the existing views onto the node model and delete the parallel models:
  1. the **project navigators** (`ProjectNavigatorImpl`, `FileNavigatorImpl`) — `ProjectKindProvider`
     produces children/child factories, the view becomes generic, and the **Files view columns** use
     `PropertyColumn` + the shared cell editors (from the property-sheet step);
  2. the **Structure view** — the language providers produce nodes;
  3. **node actions / new-types / preferred action**, then operations (rename/delete/copy/paste, DnD).
  The Properties window itself is migrated in the property-sheet step (doc 13); this step removes the
  remaining callers of the old `FxProperty*` model.
- **Acceptance**: navigator, Structure and Properties behave identically to today; the Properties window
  shows node property sets; context menus come from node actions; one model drives several views;
  navigator columns are editable and stay in sync with the sheet.
- **Depends**: Nodes & views API, Property sheet & editors.
- **Ordering**: before the `ProjectModel` node factories and the navigator/Structure/Properties parity work;
  migrate view by view (keep the old view until its replacement is proven).
- **Landed**: n/a.

## Project modularity refactoring
→ branch: `refactor/project-modularity`
- **Scope**: mirror `ide/projectui`'s layout so the New File/New Project wizards have a clean home
  **before** they are written. NetBeans puts the New File action/wizard/chooser in `ide/projectui`
  (action `...project.ui.actions.NewFile`; wizard `...project.ui.NewFileWizard` + `NewFileIterator`;
  chooser `...project.ui.TemplateChooserPanel`); the generic template wizard is
  `platform/openide.loaders.TemplateWizard` and headless creation is
  `platform/api.templates.FileBuilder` (already reused by `nbfx.templates`). `nbfx.project` is
  documented ([`26`](26-roadmap.md) §3.1) as the **project model** module, but today holds only that UI.
- **Steps**: move `NewFileWizard`, `NewProjectWizard`, `NewGradleProjectWizard`, `Wizards` from
  `nbfx.project` to `nbfx.project.ui`; move `WizardCommand`, `ProjectCommandsProvider`,
  `ProjectToolbarRegistrations` to `nbfx.project.actions`; leave `nbfx.project` for the Project model
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
  Project model, §6). Wizard ids/layer registrations unchanged; build + purity green.

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

## Project model (headless `ProjectModel`)
→ detail: [`02-part1-infrastructure.md`](02-part1-infrastructure.md) · branch: `feature/fxproject-model`
- **Scope**: an nbfx-native headless `ProjectModel` implementing the NetBeans contract
  (`org.netbeans.api.project.Project` + `ProjectFactory`, registered via `@ProjectTypeRegistration`)
  with `Sources`/`ClassPathProvider`/`ActionProvider`/`ProjectOpenedHook`/`RecommendedTemplates` — and
  **no** UI SPIs. Replaces the stock `ProjectManager.findProject` + `OpenProjects.open` bridge, so the
  Swing project-type modules and the `projectuiapi.base` dependency can be dropped while
  indexing/classpath/completion keep working.
- **Steps**: `ProjectModel` + factory + kind detection; `Sources`; `ClassPathProvider`; `ActionProvider`
  delegating to the nbfx commands; `ProjectOpenedHook` (indexer roots); per-type providers
  (Maven/Gradle/Ant-Java); `RecommendedTemplates`; wire `ProjectRegistry` to open/close `ProjectModel` and
  remove `OpenProjects`.
- **Acceptance**: opening each type indexes and completes with no `OpenProjects` call; `check-purity`
  passes with the `projectuiapi.base` exemption removed.
- **Depends**: none.
- **Landed**: n/a.
- **Ordering**: **later** — not a prerequisite for the New File/New Project wizard UI (which can start
  with generic filtering); do it before project-type-aware template filtering and before dropping the
  stock project-type modules. Full design + the NetBeans comparison is in
  [`02-part1-infrastructure.md`](02-part1-infrastructure.md) (Project model — `ProjectModel`).

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
  `BrokenReferencesImpl` must be removed (patch `projectui`, or drop it after the ProjectModel step).
- **Ordering**: before removing `projectui` from the clusters. Only one `ProjectProblemsImplementation`
  may be registered, so either patch `projectui` to drop `BrokenReferencesImpl` or drop `projectui` after
  the ProjectModel step. Until then the stock Swing dialogs appear — suppress them with
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
steps; **FX dialogs & notifications (UI)**, **Theming & branding**, **Options system: infrastructure +
example**, **Output window**, **Execution service**, **Terminal emulator model** and **Nodes & views API**
can start in parallel with the wizard/New File topics (separate modules, no file overlap — the
project-problems UI depends on the dialogs step, the options window and the two style foundations share
the semantic colour token names and the dialogs chrome, and the node model must land before the ProjectModel
node factories); the **Property sheet & editors** step follows `nodes-api` and must land before the views
migration; the **Options** category/browser/export/Quick Search steps follow the options foundation; the
**Terminal PTY backend** needs the execution service, the **Terminal view** needs the model + PTY, and the
**run/build output integration** (Terminal default, Maven as a process) comes last; the **Debug** steps
start after the Output/Execution work (foundation → views/breakpoints/stepping/evaluate/attach, with
build integration after Run configurations and the debug options panel after the options foundation); the
**Profiler** steps start after the execution service too (foundation → recordings/streaming/async-profiler
→ CPU/memory/locks → attach/sampler → heap walker → options, with tracer/OQL/profiling points/compare
later); the **Project model** is scheduled later, before project-type-aware template filtering. Then
Part 2. Each step is sized to be reviewable on its own.
