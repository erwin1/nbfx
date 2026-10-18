# nbfx — the plan

**One document. One plan.** Everything else under `docs/plan/archive/` is historical research kept for
evidence; it is not the plan and is not maintained.

This plan is written so that a new contributor can (1) understand the architecture, (2) know the naming and
API rules, (3) find the module they need to touch, and (4) pick the next step. When this document and a
module disagree, the document wins until it is changed here.

---

## 1. What nbfx is

A **pure-JavaFX IDE** built on the NetBeans runtime:

- a JavaFX **application shell** owns the window, docking, menus, views and dialogs;
- nbfx provides JavaFX replacements for every **user-facing** service of the NetBeans platform, IDE and
  Java clusters;
- the NetBeans **headless engines** are reused as-is (project model, parsing/indexing, `java.source`,
  build/execution primitives, debugger, profiler, VCS, templates, autoupdate);
- **no Swing renders**; Swing modules are retired from the distribution, not merely unused;
- a **published plugin API** lets third parties build NBMs on nbfx.

**Non-goals.** Web/JS/JavaEE/PHP/C++ languages, the Swing form editor, WebStart/JNLP execution, Java ME,
`native-image`, JavaHelp topics, database/server tooling.

## 2. Layers, clusters and dependency rules

```
clusters (build order, dependencies downward only):

  nbfx-libs      bundled external libraries            (jline, async-profiler, javafx, jgit, jacoco, jna)
      │
  nbfx-api       published + shared APIs (T1/T2-FX/M)  com.gluonhq.netbeans.nbfx.api.*
      │
  nbfx-platform  shell + platform services             com.gluonhq.netbeans.nbfx.platform.*
      │
  nbfx-ide       IDE building blocks                   com.gluonhq.netbeans.nbfx.ide.*
      │
  nbfx-java      Java/JVM usage                        com.gluonhq.netbeans.nbfx.java.*
  nbfx-javafx    JavaFX usage                          com.gluonhq.netbeans.nbfx.javafx.*
```

**Rules.**

1. **Downward only.** `java`/`javafx` → `ide` → `platform` → `api` → `libs`. No upward edges, no cycles,
   no siblings across layers. Enforced by the module-graph guard.
2. **No split packages.** A package has exactly one owning module.
3. **Models are toolkit-free.** Any module that a headless engine or a plugin backend uses contains no
   `javafx.*`, no `javax.swing`/`java.awt`, no legacy NetBeans UI.
4. **The UI toolkit lives in implementations**, never in an API type name.
5. **State the tree.** A claim in this plan refers to `main`; local branches are named explicitly.

## 3. Naming conventions

### 3.1 Modules

```
com.gluonhq.netbeans.nbfx.api.<capability>            published/shared API        (cluster nbfx-api)
com.gluonhq.netbeans.nbfx.platform.<capability>       platform implementation     (cluster nbfx-platform)
com.gluonhq.netbeans.nbfx.ide.<capability>            IDE implementation          (cluster nbfx-ide)
com.gluonhq.netbeans.nbfx.java.<capability>           Java/JVM usage              (cluster nbfx-java)
com.gluonhq.netbeans.nbfx.javafx.<capability>         JavaFX usage                (cluster nbfx-javafx)
com.gluonhq.netbeans.nbfx.libs.<library>              bundled external library    (cluster nbfx-libs)
```

- **Package == module code-name** (NetBeans convention); no extra package prefixes.
- A module that must expose a model to headless code but a UI elsewhere is split:
  `…ide.<capability>` (UI) + `…api.<capability>` (model). A module never mixes a public model with FX UI.
- `<capability>` is a noun: `actions`, `views`, `status`, `dialogs`, `options`, `settings`, `progress`,
  `output`, `execution`, `terminal`, `nodes`, `property`, `project`, `build`, `editor`, `wizard`,
  `templates`, `search`, `debug`, `profile`, `tests`, `vcs`, `diff`, `tasklist`, `plugins`.

### 3.2 Types

| Kind | Rule | Examples |
|---|---|---|
| SPI/API type | capability noun, no toolkit | `OptionsPanelController`, `WizardIterator`, `ViewProvider`, `PropertyEditor`, `ProjectModel`, `NodeModel`, `Settings`, `Dialogs`, `TerminalSession` |
| Registration annotation | `@<Capability>Registration` | `@ViewRegistration`, `@ActionRegistration`, `@OptionsRegistration`, `@ProjectTypeRegistration` |
| Implementation type | may carry the toolkit **and must be non-public** in API packages | `FxOptionsWindow` in `…platform.options` (private/package-private) |
| Service | registered via `@ServiceProvider(service = X.class)` for singletons; via layer annotations for contributions | `Dialogs`, `Settings`, `ActionRegistry`, `FxOptions` |
| Ids | one vocabulary per domain, `@since`-tagged | `ActionIds`, `TemplateIds`, `EditorCommands` |

**Forbidden in public API:** `Fx*`/`Sw*` names, `*Impl` names, public layer-path constants, `Object`-typed
factories, `Map`-parameters with string keys.

### 3.3 Layer paths

Layer folder names are **implementation detail**: each processor and each reader owns a
`private static final String` and a test asserts that the generated layer contains the folders the readers
query. No public constants for layer paths.

## 4. API tiers and governance

| Tier | Who consumes it | Toolkit | Promise |
|---|---|---|---|
| **T0** implementation | its own module | any | none; exports nothing |
| **T1** shared API | other nbfx layers | documented per package | internal compatibility only |
| **T2-FX** published plugin API | third-party NBMs | **JavaFX allowed** (`Node`, `ObservableValue`, `KeyCombination`) | additive compatibility, version range, docs, signature tests, deprecation |
| **M** model/backend API | nbfx layers, plugin backends | **toolkit-free** | as T2, no toolkit types |
| **L** library | nbfx | bundled library | licence/notice; allowlisted in the guard |

**Governance per T1/T2-FX/M module:** explicit `<public-packages>` (one package per capability, never
`<subpackages>`), `arch.xml` `<api>` entries, `apichanges.xml` on every change, `@since` on new elements,
a signature test, and a **starting specification version of 1.0** with the bump rule: additive → 1.1,
incompatible → 2.0. The guard checks that a dependant's `<specification-version>` exists.

**Plugin contract.** A plugin may use: the registration annotations, the T2-FX/M API modules of the
extension points it implements, and the platform core (`org.openide.util`, `org.openide.filesystems`,
`org.openide.util.lookup`). It may not use: T0 modules, layer paths, `*Impl` classes, view layouts, theme
CSS classes. A `Node`-returning API documents ownership (new node per call; the shell owns what a view
hands over). Menu contributions are FX `MenuItem`s.

**Trust (proposed until shipped).** NBM JAR signature verification with a system/pinned trust anchor, HTTPS
update centers, pinned SHA-256 for bundled native artifacts, install-time capability display.

## 5. Architecture of the UI layer

1. **One model, many views.** `nbfx.api.nodes` (M) defines the node/children/property model and the
   explorer context; the platform provides the FX views (tree, tree-table, table, list) and the property
   sheet. The navigator, structure view, breakpoints, test results, variables and task list all render
   through it.
2. **Typed factories, one SPI many toolkits.** Component-producing SPIs (options, wizard, property editor,
   views, dialogs, menu chrome) are generic over the toolkit's component type `C`:
   `OptionsPanelFactory<C>`, `WizardUi<C>`, `PropertyEditorFactory<C>`, `ViewFactory<C>`. The shell binds
   `C = javafx.scene.Node`; a test front end binds a test node; nothing casts. A raw
   `Lookup.lookup(OptionsPanelFactory.class)` is forbidden.
3. **Model-first panels.** A panel that is only fields/validation is a `PanelModel` (ordered `FieldModel`s
   with a `PropertyEditorRef`, actions as command ids, a validator) rendered by the property sheet — no
   component factory at all. Use the factory only for genuinely custom visualisations.
4. **Uniform async.** Analysis, usages, hints, refactoring preview, output, execution, VCS and profiler all
   run as cancellable jobs with progress; the FX thread never blocks. One job/task model drives the status
   line and the all-tasks popup.
5. **One notification surface.** `api.dialogs`: dialogs with roles, notification line, toasts,
   "show again" keys, exception reporting.
6. **One settings layer.** `api.settings` (M) is a typed facade over `NbPreferences`; panels bind through a
   UI adapter; keys are a documented, stable convention (`nbfx.<capability>.<key>`).
7. **One search surface.** `api.search` (quick search/command palette) + providers; find-in-projects,
   find-usages and Go-to are providers over the same index.
8. **One theme.** Semantic token CSS for every FX surface, light/dark, brandable (doc: theming section in
   the archived research).

## 6. Module catalogue

### 6.1 `nbfx-api` — published/shared APIs

| Module | Tier | Contents |
|---|---|---|
| `api.actions` | T2-FX | `Command`, `CommandsProvider`, `ActionRegistry`, `KeyMap`, `ActionContext`, `@ActionRegistration`, `ActionIds` |
| `api.views` | T2-FX | `ViewProvider`, `ViewManager`, `DockLocation`, `@ViewRegistration`, `ViewLocation` |
| `api.status` | T2-FX | `StatusElement`, `@StatusRegistration`, `StatusAlignment` |
| `api.dialogs` | T2-FX | `Dialogs`, `DialogDescriptor`, `DialogOption`, `Notification` (notification line, toasts) |
| `api.options` | T2-FX | `OptionsPanelController`, `@OptionsRegistration`/`Container`/`Sub`, `OptionsDisplayer`, `OptionsKeywords` |
| `api.settings` | M | `Settings`, typed keys, change listeners |
| `api.progress` | M | `ProgressHandle`, task tree, cancellation |
| `api.output` | M | `OutputConsole`, `OutputWriter`, hyperlinks, folds, colours |
| `api.execution` | M | `ExecutionService`, `ExecutionDescriptor`, `ExecutionHandle`, `PtySupport` |
| `api.terminal` | M | `TerminalModel`, `PtyProcess`, `TerminalSession` |
| `api.nodes` | M | `NodeModel`, `ChildrenModel`, `ChildFactory`, `PropertyModel`, `PropertySetModel`, `ExplorerContext`, `PropertyChange` events |
| `api.property` | T2-FX | `PropertyEditor`, `PropertyEditorRegistration`, `PropertyEnv`, `PropertyEditorRef` |
| `api.project` | M | `ProjectModel`, `ProjectRegistry`, `ProjectFactory`, `ProjectFile`/`Directory`/`Library`, `@ProjectTypeRegistration`, `ProjectKindProvider` |
| `api.build` | M | `BuildActionProvider`, `BuildExecution`, `BuildOutput`, `BuildProgress`, `BuildCommands`, `RunConfiguration` |
| `api.project.customizer` | T2-FX | `ProjectCustomizerPanel`, `@CustomizerRegistration`, `ProjectCustomizerPanel` rendering |
| `api.editor` | T2-FX | `EditorService`, `EditorDocument`, `EditorContext`, annotations (`EditorAnnotation`, `EditorAnnotationProvider`), completion/breadcrumbs SPIs |
| `api.editor.intelligence` | M | `GotoProvider`, `NavigationProvider`, `UsagesProvider`, `HierarchyProvider`, `HintProvider`, `RefactoringProvider`, `CodeGenerator` |
| `api.wizard` | T2-FX | `WizardIterator`, `WizardPanel`, `WizardDescriptor`, `@WizardRegistration` |
| `api.templates` | M | `TemplateRegistry`, `TemplateCreator`, `@TemplateRegistration` |
| `api.search` | T2-FX | `SearchProvider`, `SearchRequest`, `SearchResult`, `SearchScope` |
| `api.debug` | M *(T2-proposed)* | `DebuggerBackend`, `DebuggerSession`, `Breakpoint`, `StackFrame`, `Variable` |
| `api.profile` | M *(T2-proposed)* | `ProfileBackend`, `ProfileSession`, `StackTree`, `HeapSnapshot` |
| `api.tests` | M *(T2-proposed)* | `TestRunner`, `TestSession`, `TestReportParser`, `CoverageProvider` |
| `api.vcs` | M *(T2-proposed)* | `VcsProvider`, `VcsStatus`, `VcsHistoryProvider`, `VcsAnnotator` |
| `api.diff` | M | `Diff`, `DiffResult`, `Patch`, `Merge` (headless engine) |
| `api.tasklist` | M | `TaskProvider`, `Task`, `TaskScope` |
| `api.plugins` | T1 | plugin metadata, install/update service surface |
| `api.layer` *(internal)* | T1 | **not planned** — layer paths stay private per reader + test |

**Rule of creation:** an `api.*` module exists only when a second module (or a plugin) needs it. Start with
T0/T1 inside the owning module and promote deliberately.

### 6.2 `nbfx-platform` — shell and platform services

| Module | Purpose | Notes |
|---|---|---|
| `platform.shell` | main window, session restore, app state | replaces current `nbfx.ui` |
| `platform.workspace` | docking model + FX docking view | replaces `nbfx.docking`/`nbfx.windows` |
| `platform.actions` | command registry, keymap, action context impl | replaces `nbfx.actions` + `nbfx.context` |
| `platform.menus` | menu/toolbar/context-menu assembly from the layer | replaces `nbfx.menus` |
| `platform.status` | status bar + element rendering | replaces `nbfx.statusbar` |
| `platform.dialogs` | dialog host, notification line, toasts, exception reporting | new (was planned) |
| `platform.progress` | progress UI (status line, all-tasks popup, task details) | replaces `nbfx.progress` |
| `platform.theme` | theme manager, light/dark palettes, branding, editor syntax palettes | new |
| `platform.settings` | settings storage UI (preferences browser, export/import) | new |
| `platform.options` | options window (categories, tabs, search, validity) | replaces `nbfx.options` UI |
| `platform.nodes` | FX node views + property sheet on `api.nodes`/`api.property` | new (merges `nbfx.nodes.ui` + `nbfx.propertysheet`) |
| `platform.output` | output window view on `api.output` | replaces `nbfx.output` |
| `platform.execution` | process launcher, pumps, PTY (JLine), cancellation | new |
| `platform.terminal` | VT/xterm model + FX renderer + tabs | new |
| `platform.quicksearch` | command palette + providers | new |
| `platform.credentials` | keyring service + unlock prompt | new |
| `platform.favorites` | favourites view | new |
| `platform.templates` | template model (discovery/creation) | replaces `nbfx.templates` (branch) |
| `platform.wizard` | wizard framework + themed shell | replaces `nbfx.wizard` |
| `platform.launcher` | application entry point | replaces `nbfx.launcher` |
| `platform.macos` | macOS integration (menus, file associations) | replaces `nbfx.macos` |
| `platform.annotations` | shell annotations (action/view/menu/toolbar/status) + processors | dissolved into `api.*`; only if a shared processor module is needed |

### 6.3 `nbfx-ide` — IDE building blocks

| Module | Purpose | Notes |
|---|---|---|
| `ide.project` | project model impl, registry, kind detection | on `api.project` |
| `ide.project.ui` | New File/New Project wizards, project views, project context | replaces `nbfx.project`/`project.ui` |
| `ide.project.actions` | project commands/menus | replaces `nbfx.project.actions` |
| `ide.project.customizer` | customizer host + built-in categories | on `api.project.customizer` |
| `ide.build` | build/run orchestration on `api.build` + `api.execution` | replaces `nbfx.run` |
| `ide.editor` | FX editor (code area, completion, breadcrumbs, find/replace, actions) | replaces `nbfx.editor` + `nbfx.editor.actions` |
| `ide.editor.intelligence` | goto/search, usages, hierarchies, hints, refactoring, codegen engines + UI | new |
| `ide.javadoc` | Javadoc viewing/index/completion | new |
| `ide.structure` | structure/navigator view on `api.nodes` | replaces `nbfx.structure` |
| `ide.search` | find in projects + results view | replaces `nbfx.findinprojects` |
| `ide.usages` | find usages | replaces `nbfx.findusages` |
| `ide.file` | file actions/desktop API | replaces `nbfx.file.actions` |
| `ide.debug` | debugger views + session UI on `api.debug` | replaces `nbfx.debug` UI |
| `ide.profiler` | profiler views on `api.profile` | new |
| `ide.tests` | test runner UI + coverage view | new |
| `ide.vcs` | VCS framework + status/badges | replaces `nbfx.vcs` |
| `ide.vcs.git` | Git provider (jgit) | new |
| `ide.diff` | diff/merge view + patch UI | new |
| `ide.localhistory` | local history view | new |
| `ide.tasklist` | task list view + providers | new |
| `ide.bookmarks` | bookmarks view | new |
| `ide.libraries` | libraries manager | new |
| `ide.plugins` | plugin manager UI | replaces `nbfx.plugins` |
| `ide.modulegraph` | module dependency graph | new |
| `ide.i18n`, `ide.beans`, `ide.jshell` | i18n/beans/JShell tooling | new |

### 6.4 `nbfx-java` — Java/JVM usage

| Module | Purpose |
|---|---|
| `java.platforms` | Java Platforms (JDK storage, sources/javadoc, JavaFX capability) + options |
| `java.projecttype.java` | J2SE (Ant) project type |
| `java.projecttype.maven` | Maven project type (process-based builds) |
| `java.projecttype.gradle` | Gradle project type |
| `java.projecttype.ant` | Ant project support |
| `java.build.ant` | Ant runner (in-process listeners) |
| `java.editor` | Java semantics: parser services, completion, hints, refactoring, codegen, Javadoc |
| `java.structure` | Java structure provider |
| `java.debug` | JPDA backend (`api.debug`) |
| `java.profiler` | JFR + async-profiler backends (`api.profile`) |
| `java.tests` | JUnit/TestNG + Ant/Maven/Gradle + JaCoCo (`api.tests`) |
| `java.module` | module-info support + module graph data |
| `java.beans`, `java.i18n`, `java.jshell` | language-specific tooling |

### 6.5 `nbfx-javafx` and `nbfx-libs`

| Module | Purpose |
|---|---|
| `javafx.project` | JavaFX project usage (J2SE Ant + `javafx.enabled=true`), customizers, run/build |
| `javafx.fxml` | FXML editing/completion |
| `javafx.scenebuilder` | embedded SceneBuilder (kit 26.0.0) |
| `libs.javafx` | bundled JavaFX runtime |
| `libs.jline` | PTY/termios for the terminal |
| `libs.asyncprofiler.<os>-<arch>` | sampling agent binaries (OS/arch modules) |
| `libs.jgit` | Git engine (reuse NetBeans' jgit modules) |
| `libs.jacoco` | coverage (reuse the shipped `org.jacoco.core`) |

## 7. Module migration map (current → target)

| Current module | Target |
|---|---|
| `nbfx.api` | split into `api.actions`, `api.views`, `api.status`, `api.editor`, `api.project`, `api.build`, `api.file`→`api.editor`/`ide.file`, `api.elements`→`api.views`/`api.editor`, `api.icons`→`api.views`, `api.progress`, `api.search`, `api.breadcrumbs`/`api.completion`→`api.editor` |
| `nbfx.annotations` | dissolved: annotations move into their `api.*` module; path constants private per reader + test |
| `nbfx.actions`, `nbfx.context` | `platform.actions` |
| `nbfx.docking`, `nbfx.windows` | `platform.workspace` |
| `nbfx.menus` | `platform.menus` |
| `nbfx.statusbar` | `platform.status` (+ `api.status`) |
| `nbfx.options` | `platform.options` (+ `api.options`) |
| `nbfx.output` | `platform.output` (+ `api.output`) |
| `nbfx.wizard` | `platform.wizard` (+ `api.wizard`) |
| `nbfx.ui` | `platform.shell` |
| `nbfx.launcher` | `platform.launcher` |
| `nbfx.macos` | `platform.macos` |
| `nbfx.lib.javafx` | `libs.javafx` |
| `nbfx.propertysheet` | `platform.nodes` (FX sheet) + `api.property` (editor SPI) + `api.nodes` (model) |
| `nbfx.project` (wizards) | `ide.project.ui` |
| `nbfx.project.ui`, `nbfx.project.actions` | `ide.project.ui`, `ide.project.actions` |
| `nbfx.project.ui.api` | `api.project` (kind + identity), `api.build` (build/run), UI → `ide.project.ui` |
| `nbfx.project.context` | `api.project` (context service) or `ide.project` |
| `nbfx.project.customizer` | `ide.project.customizer` (+ `api.project.customizer`) |
| `nbfx.run` | `ide.build` (+ `api.build`, `api.execution`) |
| `nbfx.editor`, `nbfx.editor.actions` | `ide.editor` (+ `api.editor`) |
| `nbfx.structure` | `ide.structure` |
| `nbfx.findinprojects` | `ide.search` |
| `nbfx.findusages` | `ide.usages` |
| `nbfx.file.actions` | `ide.file` (+ `api.views` file context) |
| `nbfx.debug` | `ide.debug` (+ `api.debug` + `java.debug`) |
| `nbfx.vcs` | `ide.vcs` (+ `ide.vcs.git` + `api.vcs`) |
| `nbfx.plugins` | `ide.plugins` (+ `api.plugins`) |
| `nbfx.build.ant` | `java.build.ant` |
| `nbfx.project.ui.java` | `java.projecttype.java` |
| `nbfx.project.ui.maven` | `java.projecttype.maven` |
| `nbfx.project.ui.gradle` | `java.projecttype.gradle` |
| `nbfx.project.ui.ant` | `java.projecttype.ant` |
| `nbfx.structure.java` | `java.structure` |

The rename is one step (`refactor/module-names`), mechanical, with the cluster fragments and the
`public-packages` updated; no behaviour change. It runs **before** the API split so the API modules get the
final names.

## 8. Data and compatibility

- **Project properties** keep the NetBeans names (`javac.classpath`, `run.classpath`, `javadoc.*`, …) so
  Ant/Maven/Gradle projects round-trip; the customizer is the only FX part.
- **Preferences** keep the NetBeans nodes where a reused headless module owns them; nbfx-specific keys use
  `nbfx.<capability>.<key>`.
- **Formats**: we *read* `.nps`, `.hprof`, `.jfr`; we write only formats we own or that are public.
- **Keymap/breakpoints/platform storage** stay in the platform storage; the UI reads/writes them.
- **Debuggees/VCS/native tools** are launched through `api.execution` (PTY by default for run), so no
  module starts processes itself.

## 9. Guardrails (structural, in the build)

| Check | Rule |
|---|---|
| module graph | no cycles; no upward layer edge; nbfx → nbfx edges only downward |
| dependency allow| forbidden NetBeans UI modules (`openide.awt/nodes/explorer/windows/dialogs`, `core.windows`) absent from the runtime path, or exempted with owner + deadline |
| bytecode — M | M jars contain no `javafx.*`, no `javax.swing`/`java.awt`, no legacy UI |
| bytecode — T2-FX | no `javax.swing`/`java.awt`; JavaFX allowed |
| bytecode — L | allowlisted; their packages forbidden in M jars |
| API governance | every T1/T2-FX/M public package has `arch.xml` + `apichanges` + signature test; dependant spec versions exist |
| public-package audit | every module's packages listed explicitly; T0 with external consumers is an error; tiers derived from the audit, re-run per milestone |
| naming | no toolkit/`Impl`/`Object` in public API; no public layer-path constants |
| budget | startup, "usable", first completion, module count, memory recorded per milestone; >20 % regression blocks |

## 10. Roadmap

| Milestone | Target | Content | Exit |
|---|---|---|---|
| **M0 — Shell & platform** | 2026-12 | shell/docking/actions/menus/status, dialogs, progress, theme, options, settings UI, nodes+property views, output, execution, terminal, quick search, credentials, favourites, templates, wizard | app usable end-to-end for one project type; M0 Swing modules retired; guardrails green; budget baseline recorded; module/API rename + split landed |
| **M1 — IDE core** | 2027-03 | projects, editor, structure, search, usages, file actions, VCS+Git, diff, local history, tests UI, libraries, task list/bookmarks, plugins UI, module graph | open/edit/search/find/commit/test/manage in FX; M1 Swing retired |
| **M2 — Java tooling** | 2027-07 | project types (J2SE/Maven/Gradle/Ant), Java Platforms, editor intelligence (goto, usages, hints, refactoring, codegen, Javadoc), debugger, profiler, Java tests/coverage | daily Java work in nbfx; M2 Swing retired |
| **M3 — JavaFX usage** | 2027-09 | JavaFX project, FXML editing, SceneBuilder | original JavaFX-cluster capability |
| **M4 — Breadth** | 2027-12 | module graph, i18n, beans, JShell, remote debug/profiler, export/import, accessibility, multi-window | feature parity with the intended IDE scope |
| **M5 — Ecosystem** | 2028 | plugin API hardening, compatibility reports, update centers, docs/tutorials | third-party plugin can be built and published |

## 11. Workstreams (the work, sliced)

Each workstream is a branch family; each step lands alone with tests and guardrails. **Order inside a
workstream matters; across workstreams only the stated dependencies do.**

**W0 — Walking skeleton & baseline.** `feature/walking-skeleton`: one project end-to-end (open → edit →
build → run → output → commit) + the budget script and baseline. Everything else keeps it green.

**W1 — Module names and API split.** `refactor/module-names` (rename to the §3.1 scheme, move to the §6
clusters) → `refactor/api-split` (create the `api.*` modules; move packages **wholesale** — same package
names, consumers update only their `project.xml`; topological order: elements/file/project → editor →
actions/views → root last; no shims) → `refactor/constants` (`FxLayer` out; private constants + a
layer-folder test) → `refactor/objects-not-statics` (services with lifecycles; typed contracts; no
`Lookup.forPath` per call).

**W2 — API governance.** `feature/api-governance`: `arch.xml`/`apichanges`/signature tests for every
T1/T2-FX/M module, the version baseline (1.0 + bump rule) and the spec-version guard; the audit/guard
implementation (`refactor/guardrails`); the exemption register with owners and dates.

**W3 — Shell and platform services.** Per service, on its own branch: `feature/shell`,
`feature/workspace`, `feature/dialogs`, `feature/progress`, `feature/theme`, `feature/settings-ui`,
`feature/options`, `feature/nodes-views`, `feature/output`, `feature/execution`, `feature/terminal`,
`feature/quicksearch`, `feature/credentials`, `feature/favorites`, `feature/templates`, `feature/wizard`.

**W4 — IDE core.** `feature/project-model`, `feature/project-ui`, `feature/editor`,
`feature/navigation-search`, `feature/refactoring`, `feature/hints`, `feature/codegen`,
`feature/javadoc`, `feature/structure`, `feature/diff`, `feature/vcs`, `feature/localhistory`,
`feature/tests`, `feature/libraries`, `feature/tasklist-bookmarks`, `feature/plugins-ui`,
`feature/modulegraph`.

**W5 — Java tooling.** `feature/java-platforms`, `feature/projecttypes` (java/maven/gradle/ant),
`feature/java-editor`, `feature/debugger`, `feature/profiler`, `feature/java-tests-coverage`.

**W6 — JavaFX usage.** `feature/javafx-project`, `feature/fxml`, `feature/scenebuilder`.

**W7 — Front-end SPI migration.** `spike/front-end-component-factory` (options prototype + upstream PR) →
`spi-factory-migration` (options → views → wizard → property editors, one at a time, with a test factory).

**W8 — Upstream and retirement.** `chore/upstream-sync` (monthly pin/patch rebase, patch register, patch
budget, CI), `refactor/swing-retirement` (matrix + exemptions; extend the existing `check-packaging`),
`feature/plugin-api` (contract docs, sample plugin, trust).

## 12. Mirror and evidence

- The **archived** documents under `docs/plan/archive/` hold the research that justifies each workstream
  (NetBeans class/file references, counts, formats, licences). Consult them for evidence; do not treat
  them as the plan.
- `docs/architecture-rules.md` holds the durable cross-cutting rules (this plan section 2–5 in rule form).
- `docs/repo-and-build.md` documents the harness; `docs/MVP.md` is a historical draft.

## 13. Decisions (locked unless revised here)

1. **Layers/clusters**: `nbfx-libs` → `nbfx-api` → `nbfx-platform` → `nbfx-ide` → `nbfx-java`/`nbfx-javafx`;
   downward dependencies only.
2. **Module/package naming** per §3; packages equal module code-names; no split packages.
3. **API tiers** per §4; the published plugin API is JavaFX (T2-FX); models/backends are toolkit-free (M).
4. **Registration annotations live in the owning `api.*` module** — no separate annotation modules.
5. **The `nbfx.api` split is a module-ownership migration**: same package names, consumer dependency
   updates only, topological order, **no shims**.
6. **Typed factories** for component-producing SPIs; model-first panels where possible; no `Object`.
7. **Nodes/property model** (`api.nodes`, M) drives every tree/table/sheet view; one property/editor model
   for sheet, outline columns and customizers.
8. **Execution/output/terminal** as platform services on `api.execution`/`api.output`/`api.terminal`; run
   defaults to the terminal; Maven builds run as processes.
9. **Tests and iterations**: every step has tests and guardrails; the module graph and audit are re-run per
   milestone; budgets are baselined and checked.
10. **Upstream first**: SPI generalisations are prototyped in nbfx and proposed to `apache/netbeans`; friend
    patches require an upstream issue (delete `0002`, retire `0003` with the Maven process, keep `0001` with
    a `RunLevel` proposal).
11. **Retirement is the deliverable**: a milestone is done when its Swing modules leave the distribution,
    asserted by the guard.

## 14. Open questions

1. **Plugin API stability**: publish T2-FX at M1 or at M5 (after the ecosystem hardening)? Proposed: publish
   at M1 as *devel* (`arch.xml category="devel"`), freeze at M5.
2. **Multi-window/detached views**: M4 or earlier? (Proposed: M4.)
3. **Coverage licence**: JaCoCo is EPL-2.0 and already shipped by NetBeans; confirm the policy for a
   *published* nbfx feature before bundling.
4. **Async jobs model**: fold into `api.progress` or a separate `api.jobs`? (Proposed: `api.progress`.)
5. **`api.layer`**: none — confirm the private-constants + test decision holds at scale.
