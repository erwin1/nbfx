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

1. **Downward only (nbfx edges).** `java`/`javafx` → `ide` → `platform` → `api` → `libs`. No upward
   edges, no cycles. Siblings are allowed only in the declared order `java` → `javafx`. **Upstream reuse
   edges** (an nbfx module depending on a NetBeans module) are not layer edges; they are allowlisted
   separately with a justification (Appendix A). Enforced by the module-graph guard.
2. **No split packages.** A package has exactly one owning module.
3. **Models are toolkit-free.** Any module that a headless engine or a plugin backend uses contains no
   `javafx.*`, no `javax.swing`/`java.awt`, no legacy NetBeans UI.
4. **The UI toolkit lives in implementations**, never in an API type name.
5. **State the tree.** A claim in this plan refers to `main`; local branches are named explicitly.
6. **One semantic boundary per language.** The Java semantic layer (`java.*`) is the only place that may
   import `org.netbeans.api.java.source`/`org.netbeans.libs.javacapi`; every other layer consumes the
   toolkit-free seams (`api.editor.EditorDocument`, `api.editor.OpenSources`, `api.elements.*`). See
   [Appendix B](appendix/B-java-source-isolation.md).
7. **The toolkit policy is Appendix A.** Allowed/forbidden types per tier, the exemption register and the
   guard mechanism live in [Appendix A](appendix/A-toolkit-policy.md); §9 states the guard, Appendix A
   states the rule.

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

Layer folder names are a **contract**, not an implementation detail: the writer (annotation processor) and
the reader (the shell) must query the same folder. Today `nbfx.annotations.FxLayer` publishes the folders
(`NbFx/Views`, `NbFx/Menus`, …) as public constants, and that is the right shape. Decision: the folder
constants stay **public and versioned in the owning `api.*` module** (or as annotation attributes with
public defaults). The writer/reader agreement is asserted by a **contract test** that builds a layer from a
sample annotated class and reads it back through the reader API. "Private constant per reader" is rejected:
no test can compare two privates, so a typo becomes a silent runtime no-op.

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
incompatible → 2.0. The guard checks that a dependant's `<specification-version>` exists. The
signature-test harness is the NetBeans `sigtest` support in `nbbuild`; name the target per module.

**Today's reality — the split exists to fix this.** `nbfx.api` and `nbfx.annotations` export their whole
subtree (`<subpackages>`, `nbfx.api/nbproject/project.xml:76-78`, `nbfx.annotations/…:47-49`), so every
class in them — including `ActionLayerReader`, `FxActionRef`, `LayerMenuBuilder` — is public API, and
`nbfx.api/arch.xml` has no `<api>` entries. The split replaces the subtree export with explicit
per-module packages plus real `arch.xml` `<api>` entries, and the guard asserts *exported set ⊆ arch set*.
Governed set today: `nbfx.api`, `nbfx.annotations`, `nbfx.project.ui.api`, `nbfx.propertysheet`,
`nbfx.project.customizer`, `nbfx.options`, `nbfx.output`, `nbfx.statusbar`, `nbfx.windows`, `nbfx.wizard`.
T0 modules carry no specification version.

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
9. **One Java semantic boundary.** `java.source`/`libs.javacapi` are confined to the Java layer behind one
   bridge (`java.model`) and the toolkit-free seams; the editor core, the generic usages engine and the
   platform never import them. The editor's text model is the FX rich-text model, and the semantic layer
   deals in character offsets — never in a Swing `Document`. See
   [Appendix B](appendix/B-java-source-isolation.md).

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
| `api.elements` | M | `SourceElementKind`, `SourceTypeKind`, `SourceModifiers`, `SourceLocation`, `ElementIcons` — the toolkit-free, nb-javac-safe element boundary (Appendix A.4) |
| `api.nodes` | M | `NodeModel`, `ChildrenModel`, `ChildFactory`, `PropertyModel`, `PropertySetModel`, `PropertyEditorRef` (opaque handle), `ExplorerContext`, property-change events |
| `api.property` | T2-FX | `PropertyEditor`, `PropertyEditorRegistration`, `PropertyEnv`; depends on `api.nodes` (the handle lives there — M never depends on a T2-FX module) |
| `api.usages` | M | `UsagesProvider`, `UsageResult`, `UsageKind` — the generic usages engine SPI; the Java provider is `java.usages` |
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
| `platform.editor` | FX code area, buffers, decorations, search/replace, completion popup; **no language** | from `nbfx.editor` (Appendix B.4) |
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

*(Shell annotations — action/view/menu/toolbar/status — live in their `api.*` module, not in a separate
`platform.annotations` module; the annotation processors ship in the same jar. See Decision 4.)*

### 6.3 `nbfx-ide` — IDE building blocks

| Module | Purpose | Notes |
|---|---|---|
| `ide.project` | project model impl, registry, kind detection | on `api.project` |
| `ide.project.ui` | New File/New Project wizards, project views, project context | replaces `nbfx.project`/`project.ui` |
| `ide.project.actions` | project commands/menus | replaces `nbfx.project.actions` |
| `ide.project.customizer` | customizer host + built-in categories | on `api.project.customizer` |
| `ide.build` | build/run orchestration on `api.build` + `api.execution` | replaces `nbfx.run` |
| `ide.editor.xml`, `ide.editor.css` | XML/CSS syntax decoration and breadcrumbs (language modules, not core) | from `nbfx.editor` |
| `ide.editor.intelligence` | UI for goto/search, usages, hierarchies, hints, refactoring preview, codegen, Javadoc | new; engines/SPIs are `api.editor.intelligence` (M) |
| `ide.javadoc` | Javadoc viewing/index/completion | new |
| `ide.structure` | structure/navigator view on `api.nodes` | replaces `nbfx.structure` |
| `ide.search` | find in projects + results view | replaces `nbfx.findinprojects` |
| `ide.usages` | generic usages engine + results tree/UI over `api.usages` | generic half of `nbfx.findusages` |
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
| `java.model` | **T0 bridge** over `org.netbeans.api.java.source`/`libs.javacapi`: classpath cache, in-memory `FileObject`, task runner, position↔offset, nb-javac normalisation into `api.elements` (Appendix B) |
| `java.editor` | Java semantics of the editor: lexer decoration, semantic completion, mark-occurrences, highlights, diagnostics, Java breadcrumbs, hints/refactoring/codegen/Javadoc engines |
| `java.structure` | Java structure provider |
| `java.usages` | Java provider for `api.usages` (Java search over `java.model`) |
| `java.navigation` | open-at-caret / `TreeNav` |
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
| `nbfx.api` (61 classes in 12 packages + 4 root classes) | `api.actions`; `api.views` (from `api.view` + `api.icons`); `api.status`; `api.editor` (from `api.editor` + `api.breadcrumbs` + `api.completion`); `api.elements` **(kept as its own M module)**; `api.project`; `api.build`; `api.progress`; `api.search`; `api.file` → `api.editor`/`ide.file`. Root classes: `Cancellation` → `api.progress`, `ContentManager` → `api.editor`, `ErrorReporter` → `api.dialogs`, `FxApplicationMenu` → `api.views`/`platform.menus` |
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
| `nbfx.editor`, `nbfx.editor.actions` (88 files) | `platform.editor` (Java-free core, ~50 files) + `java.editor` (Java, ~36 files) + `ide.editor.{xml,css}` + `api.editor` (seam) |
| `nbfx.structure` | `ide.structure` |
| `nbfx.findinprojects` | `ide.search` |
| `nbfx.findusages` (34 files) | `ide.usages` (generic engine + UI) + `java.usages` (Java search) + `api.usages` |
| `nbfx.file.actions` | `ide.file` (+ `api.views` file context) |
| `nbfx.debug` | `ide.debug` (+ `api.debug` + `java.debug`) |
| `nbfx.vcs` | `ide.vcs` (+ `ide.vcs.git` + `api.vcs`) |
| `nbfx.plugins` | `ide.plugins` (+ `api.plugins`) |
| `nbfx.build.ant` | `java.build.ant` |
| `nbfx.project.ui.java` | `java.projecttype.java` (+ `java.navigation` for `utils/TreeNav`) |
| `nbfx.project.ui.maven` | `java.projecttype.maven` |
| `nbfx.project.ui.gradle` | `java.projecttype.gradle` |
| `nbfx.project.ui.ant` | `java.projecttype.ant` |
| `nbfx.structure.java` | `java.structure` (+ `java.model`) |

The table is **total**: every current module and every root-package class has exactly one target. The tree
has 38 modules / 546 source files (`nbfx.editor` 88, `nbfx.api` 62, `nbfx.ui` 41, `nbfx.findinprojects` 39,
`nbfx.structure.java` 36, `nbfx.findusages` 34). A reverse index (target ← sources) is added when the split
starts; a target with neither a source nor a "new" note is a bug in the table.

The rename is one step (`refactor/module-names`), mechanical in *code*, and it runs **before** the API split
so the API modules get the final names. It is **not** behaviour-free: a module code-name change moves the
`NbPreferences` root, the `Modules/<code-name>` layer namespace, the autoupdate id and the cluster/guard
paths. Pre-1.0 that is allowed (no released version ⇒ code-name churn is permitted); the rule to record is
**code-names freeze at 1.0**, and the step adds a settings/layer round-trip test before and after.

## 8. Data and compatibility

- **Project properties** keep the NetBeans names (`javac.classpath`, `run.classpath`, `javadoc.*`, …) so
  Ant/Maven/Gradle projects round-trip; the customizer is the only FX part.
- **Project type ids are user data.** An existing project stores `<type>` in `nbproject/project.xml`
  (J2SE: `org.netbeans.modules.java.j2seproject`). Our modules may be renamed, but the *provided project
  type id* and the `javafx.enabled` property must match the NetBeans convention exactly, or every existing
  project stops opening. This is a hard rule, not a preference.
- **nb-javac duplicates JDK types.** The IDE can run on nb-javac, which bundles its own
  `javax.lang.model` classes; never compare element/modifier identity across the semantic boundary —
  convert by name (Appendix A.4, `api.elements.SourceModifiers`).
- **Preferences** keep the NetBeans nodes where a reused headless module owns them; nbfx-specific keys use
  `nbfx.<capability>.<key>`. Module code-name changes move `NbPreferences` roots: covered by a round-trip
  test in `refactor/module-names`, and code-names freeze at 1.0.
- **Formats**: we *read* `.nps`, `.hprof`, `.jfr`; we write only formats we own or that are public.
- **Keymap/breakpoints/platform storage** stay in the platform storage; the UI reads/writes them.
- **Debuggees/VCS/native tools** are launched through `api.execution` (PTY by default for run), so no
  module starts processes itself.

## 9. Guardrails (structural, in the build)

The rule is [Appendix A](appendix/A-toolkit-policy.md); this is the implementation. Every check is **green
on day one** with the current exemptions and fails only on a new violation or an expired exemption.

| Target (extend `check-purity.xml`) | Mechanism | Fails when |
|---|---|---|
| `check-sources` | import denylist + per-module allowlist | a forbidden import family outside its declared owner |
| `check-sources` (Java boundary) | `org.netbeans.api.java.source`/`libs.javacapi` only in the `java.*` semantic modules | imported anywhere else (Appendix B.5) |
| `check-dependencies` | walk each `project.xml` graph | a forbidden NetBeans UI module, or an upward layer edge |
| `check-jars` | `jdeps` (references) **and** jar listing (bundled classes), on **all** clusters (`nbfx-libs`, `nbfx-api`, `nbfx-platform`, `nbfx-ide`, `nbfx-java`, `nbfx-javafx`) | a reference/bundle of a forbidden package outside an allowed tier or exemption |
| `check-packaging` | the retirement matrix (absent modules per milestone) | a retired module reappears in the distribution |
| `check-api` *(new)* | exported set ⊆ `arch.xml` set; dependant spec versions exist | a subtree export, a missing `arch.xml` `<api>` entry |
| `check-naming` *(new)* | no `Fx*`/`Impl`/`Object`-factory in public API; layer-path constants only in the owning `api.*` module | a forbidden public name |
| `check-budget` *(new)* | startup, "usable", first completion, module count, memory | >20 % regression vs the milestone baseline |

The old, grep-only `check-jars` greps the *file list* of three cluster directories for `javax/swing/`, so a
class that imports a JDK `javax.swing` type passes and the two new clusters are unscanned; that is the gap
`jdeps` closes.

## 10. Roadmap

| Milestone | Target | Content | Exit |
|---|---|---|---|
| **M0 — Shell & platform** | 2026-12 | shell/docking/actions/menus/status, dialogs, progress, theme, options, settings UI, nodes+property views, output, execution, terminal, quick search, credentials, favourites, templates, wizard | the walking-skeleton scenario (open→edit→build→run→output→commit) runs on a **placeholder project** (no real project type until M2); the `projectuiapi.base` bridge in `launcher`/`ui` is **removed** and the M0 rows of the retirement matrix are absent; `refactor/module-names` + `refactor/api-split` landed; all guards green; budget baseline recorded |
| **M1 — IDE core** | 2027-03 | project model, editor (core + Java), structure, search, usages (generic + Java), file actions, VCS+Git, diff, local history, tests **UI**, libraries, task list/bookmarks, plugins UI | open/edit/search/find/commit/run-tests in FX on a J2SE project; the M1 rows of the retirement matrix absent; guards green |
| **M2 — Java tooling** | 2027-07 | project types (J2SE/Maven/Gradle/Ant), Java Platforms, editor intelligence (goto, usages, hints, refactoring, codegen, Javadoc), debugger, profiler, Java tests engines/coverage | daily Java work on a Maven **and** an Ant project; the M2 rows of the retirement matrix absent; guards green |
| **M3 — JavaFX usage** | 2027-09 | JavaFX project, FXML editing, SceneBuilder | a JavaFX project opens, edits FXML, and runs; `javafx.enabled` round-trips |
| **M4 — Breadth** | 2027-12 | i18n, beans, JShell, remote debug/profiler, export/import, accessibility, multi-window, module graph view | the intended feature scope is present; guards green; a11y pass recorded |
| **M5 — Ecosystem** | 2028 | plugin API hardening, compatibility reports, update centers, docs/tutorials | a third-party plugin builds against the published API and is published |

## 11. Workstreams (the work, sliced)

Each workstream is a branch family; each step lands alone with tests and guardrails. **Order inside a
workstream matters; across workstreams only the stated dependencies do.**

**W0 — Walking skeleton & baseline.** `feature/walking-skeleton`: one project end-to-end (open → edit →
build → run → output → commit) + the budget script and baseline. Everything else keeps it green.

**W1 — Containment, names and API split.** `refactor/guard-java-source` (denylist/allowlist guard over the
current 4-module allowlist; [Appendix B.5](appendix/B-java-source-isolation.md)) → `refactor/java-plumbing`
(`java.model` bridge; dedupe the three in-memory-file helpers and the position conversions) →
`refactor/editor-split` (`platform.editor` / `java.editor` / `ide.editor.{xml,css}`) →
`refactor/usages-split` (`api.usages` + `ide.usages` + `java.usages`) → `refactor/module-names` (rename to
§3.1; settings/layer round-trip test) → `refactor/api-split` (create the `api.*` modules; move packages
**wholesale** — same package names, consumers update only their `project.xml`; topological order:
elements/file/project → editor → actions/views → root last; no shims) →
`refactor/objects-not-statics` (services with lifecycles; typed contracts; no `Lookup.forPath` per call).
Layer-path constants stay public and versioned (Decision 12); the writer/reader contract test belongs to
`api-split`.

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
    asserted by the guard. For every upstream UI API, the mechanism is *reimplement behind the API* when the
    API is provider-based, *replace with an nbfx API* otherwise ([Appendix A.3](appendix/A-toolkit-policy.md)).
12. **Layer-path constants are public and versioned in the owning `api.*` module**, and the writer/reader
    agreement is a contract test (§3.3). No "private constant per reader".
13. **The Java semantic layer is the only importer of `java.source`/`libs.javacapi`**, behind the `java.model`
    bridge; `javax.swing.text.Document` is banned everywhere
    ([Appendix B](appendix/B-java-source-isolation.md)).
14. **Upstream reuse edges are not layer edges**: an nbfx module depending on a NetBeans module is recorded
    with a justification and a retirement plan; the layer rule constrains nbfx → nbfx edges only.
15. **Project type ids and the `javafx.enabled` property match the NetBeans convention**
    (`org.netbeans.modules.java.j2seproject`); module renames never change them (§8).

## 14. Open questions

1. **Plugin API stability**: publish T2-FX at M1 or at M5 (after the ecosystem hardening)? Proposed: publish
   at M1 as *devel* (`arch.xml category="devel"`), freeze at M5.
2. **Multi-window/detached views**: M4 or earlier? (Proposed: M4.)
3. **Coverage licence**: JaCoCo is EPL-2.0 and already shipped by NetBeans; confirm the policy for a
   *published* nbfx feature before bundling.
4. **Async jobs model**: fold into `api.progress` or a separate `api.jobs`? (Proposed: `api.progress`.)
5. **FX editor core**: is `jfx.incubator.scene.control.richtext` (the current code area, in `libs.javafx`)
   acceptable long-term, given `platform.editor` also reaches into
   `com.sun.jfx.incubator…RichTextAreaSkinHelper`/`VFlow`/`CaretInfo` (non-public FX internals)? If not, the
   fallback is a `TextFlow`/`Skin` build; decide before M1 (Appendix B.7).

## 15. Appendices

| Appendix | Content |
|---|---|
| [A — Toolkit policy](appendix/A-toolkit-policy.md) | allowed/forbidden types per tier, the `java.awt`/`javax.swing` decision, the bridge-vs-replace table, the nb-javac rule, the exemption register, the guard mechanism |
| [B — Java source isolation](appendix/B-java-source-isolation.md) | the `java.source` audit, the existing seams, the `java.model` bridge, the module split, the policy and the migration steps |
| C — Bridge & retirement matrix *(to write)* | one row per upstream module (`ide/editor*` ×45, `java/java.source*`, `platform/openide.*`, `projectuiapi.base`, …): reuse / bridge / replace / drop, owner, milestone, guard |
| D — API contract book *(to write)* | exported class list + tier per `api.*` module, signatures of the core SPIs, threading/ownership/a11y contracts, the test kit |
