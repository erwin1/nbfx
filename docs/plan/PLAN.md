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
  build/execution primitives, debugger, profiler, VCS, templates, autoupdate) — "as-is" means **per the
  matrix in [Appendix D](appendix/D-bridge-and-retirement-matrix.md)**: reuse / bridge / replace / drop, one
  row per upstream module, with owners and milestones;
- **the FX shell renders no Swing**; upstream Swing modules still load until the retirement-matrix rows land
  ([Appendix D.4](appendix/D-bridge-and-retirement-matrix.md)) — `openide.nodes` is enabled and initialized via
  `core.ui` and `DataObject` today ([D.2.1](appendix/D-bridge-and-retirement-matrix.md));
- a **published plugin API** lets third parties build NBMs on nbfx.

**Non-goals.** Web/JS/JavaEE/PHP/C++ languages, the Swing form editor, WebStart/JNLP execution, Java ME,
`native-image`, JavaHelp topics, database/server tooling. These are **product** non-goals: the architecture is
validated against a second language by the soundness spikes of [Appendix I](appendix/I-language-soundness.md)
— a non-goal is not a licence for a hole.

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
   toolkit-free seams (`api.editor.EditorDocument`, `api.editor.OpenSources`, `api.language.ElementRef`). See
   [Appendix B](appendix/B-java-source-isolation.md).
7. **The toolkit policy is Appendix A.** Allowed/forbidden types per tier, the exemption register and the
   guard mechanism live in [Appendix A](appendix/A-toolkit-policy.md); §9 states the guard, Appendix A
   states the rule.
8. **Test edges are not layer edges.** `<test-dependency>` may point upward — the W0 scenario in `nbfx-ide`
   needs the Java project type from `nbfx-java` — but it must never be a run dependency, and the test artefact
   is not shipped. The graph guard distinguishes test edges.

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
  `output`, `execution`, `terminal`, `language`, `nodes`, `property`, `project`, `build`, `editor`, `wizard`,
  `templates`, `search`, `debug`, `profile`, `tests`, `vcs`, `diff`, `tasklist`, `plugins`.

### 3.2 Types

| Kind | Rule | Examples |
|---|---|---|
| SPI/API type | capability noun, no toolkit | `OptionsPanelController`, `WizardIterator`, `ViewProvider`, `PropertyEditor`, `ProjectModel`, `NodeModel`, `Settings`, `Dialogs`, `TerminalSession` |
| Registration annotation | `@<Capability>Registration` | `@ViewRegistration`, `@ActionRegistration`, `@OptionsRegistration`, `@ProjectTypeRegistration` |
| Implementation type | may carry the toolkit **and must be non-public** in API packages | `FxOptionsWindow` in `…platform.options` (private/package-private) |
| Service | registered via `@ServiceProvider(service = X.class)` for singletons; via layer annotations for contributions | `Dialogs`, `Settings`, `ActionRegistry`, `FxOptions` |
| Ids | one vocabulary per domain, `@since`-tagged | `ActionIds`, `TemplateIds`, `EditorCommands` |

**Forbidden in public API:** `Fx*`/`Sw*` names, `*Impl` names, `Object`-typed factories,
`Map`-parameters with string keys. The only permitted shared constant class is `LayerPaths` (§3.3).
The current tree has **60 public `Fx*` types** (56 in exported packages; the other 4 are implementation in
`nbfx.progress`, where C.1 allows the prefix — [`figures.md`](figures.md)); the rename is
[Appendix C](appendix/C-naming-and-rename-table.md) and the step is `refactor/api-naming` (W1).

### 3.3 Layer paths

Layer folder names are a **contract**, not an implementation detail: the writer (annotation processor) and
the reader (the shell) must query the same folder. Today `nbfx.annotations.FxLayer` publishes the folders
(`NbFx/Views`, `NbFx/Menus`, …) as public constants, and that is the right shape. Decision: **one owner**,
the module `nbfx.api.layer` (cluster `nbfx-api`, tier T1) with the class `LayerPaths`; every processor
writes through it and every registry reads through it. The writer/reader agreement is asserted by a
**contract test** that builds a layer from a sample annotated class and reads it back through the reader
API. "Private constant per reader" is rejected: no test can compare two privates, so a typo becomes a
silent runtime no-op. See [Appendix C](appendix/C-naming-and-rename-table.md) §C.3.

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
extension points it implements, the platform core (`org.openide.util`, `org.openide.filesystems`,
`org.openide.util.lookup`), plus `org.openide.loaders` **API only** (`DataObject`/`DataFolder`, never
`DataNode`) and `org.netbeans.api.project` up to `Project` — the full allowlist is
[Appendix E.6](appendix/E-contracts.md); note that using `DataObject` also initializes `openide.nodes`
([D.2.1](appendix/D-bridge-and-retirement-matrix.md)). It may not use: T0 modules, layer paths (`LayerPaths`), `*Impl`
classes, view layouts, theme CSS classes. A `Node`-returning API documents ownership (new node per call; the
shell owns what a view hands over) and the shell sets an `AccessibleRole` where the plugin does not. Menu
contributions are FX `MenuItem`s.

**Trust (proposed until shipped).** NBM JAR signature verification with a system/pinned trust anchor, HTTPS
update centers, pinned SHA-256 for bundled native artifacts, install-time capability display.

## 5. Architecture of the UI layer

1. **One model, many views.** `nbfx.api.nodes` (M) defines the node/children/property model and the
   explorer context; the platform provides the FX views (tree, tree-table, table, list) and the property
   sheet. The navigator, structure view, breakpoints, test results, variables and task list all render
   through it. Explorer-like panels use it; popups/overlays (completion, breadcrumbs) keep their private
   list models. The inventory and the migration order are in
   [Appendix F](appendix/F-node-and-view-migration.md) (25 view files, ≥5 item models today).
2. **Model-first panels, concrete FX factories.** Component-producing SPIs are **concrete and FX-typed**
   (`OptionsPanelFactory` returning `javafx.scene.Node`), because there is exactly one front end; the
   portability lives in the toolkit-free model (`PanelModel`, `FieldModel`, `Validator`), which a future
   front end binds. The `OptionsPanelFactory<C>`/`WizardUi<C>`/`ViewFactory<C>` generics are removed: NetBeans
   `Lookup` cannot resolve a parameterised type, so the plan forbade the only mechanism it needed. See
   [Appendix E.7](appendix/E-contracts.md).
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
10. **Every SPI states its thread; every subscription is owned and disposed; every published view is
    accessible.** The thread rule per SPI, the disposal/ownership rules (a `subscribe` returns a resource),
    the leak test and the a11y requirement are normative in
    [Appendix E](appendix/E-contracts.md) §§E.1–E.3.
11. **One language registry.** `api.language` (M) is the only place a language is registered (`LanguageId`,
    `LanguageRegistration`, `Languages`); every provider SPI is scoped by `LanguageId`, and syntax decoration
    is a provider, not a `switch` in the core. See [Appendix I](appendix/I-language-soundness.md).

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
| `api.language` | M | `LanguageId`, `LanguageRegistration`, `Languages`, neutral `ElementRef`/`SourceLocation`/`ElementIcons` — the one language registry and the provider scope ([Appendix I](appendix/I-language-soundness.md)) |
| `api.elements` | T1 → `java.elements` | the **Java** vocabulary (`SourceElementKind`, `SourceTypeKind`, `SourceModifiers`) moves to `java.elements`; only the neutral `ElementRef`/`SourceLocation`/`ElementIcons` stay in `api.language` (no `java.lang.reflect.Modifier` bits in a neutral API) |
| `api.nodes` | M | `NodeModel`, `ChildrenModel`, `ChildFactory`, `PropertyModel`, `PropertySetModel`, `PropertyEditorRef` (opaque handle), `ExplorerContext`, property-change events |
| `api.property` | T2-FX | `PropertyEditor`, `PropertyEditorRegistration`, `PropertyEnv`; depends on `api.nodes` (the handle lives there — M never depends on a T2-FX module) |
| `api.usages` | M | `UsagesTarget` (opaque `ElementRef` + `LanguageId` + location), `UsagesProvider`, `UsageResult`, `UsageKind` — the generic usages SPI; the Java provider is `java.usages` |
| `api.project` | M | `ProjectModel`, `ProjectRegistry`, `ProjectFactory`, `ProjectFile`/`Directory`/`Library`, `@ProjectTypeRegistration`, `ProjectKindProvider` |
| `api.build` | M | `BuildActionProvider`, `BuildExecution`, `BuildOutput`, `BuildProgress`, `BuildCommands`, `RunConfiguration` |
| `api.project.customizer` | T2-FX | `ProjectCustomizerPanel`, `@CustomizerRegistration`, `ProjectCustomizerPanel` rendering |
| `api.editor` | T2-FX | `EditorService`, `EditorDocument`, `EditorContext`, annotations (`EditorAnnotation`, `EditorAnnotationProvider`), completion/breadcrumbs SPIs |
| `api.editor.intelligence` | M | `GotoProvider`, `NavigationProvider`, `StructureProvider`, `UsagesProvider`, `HierarchyProvider`, `HintProvider`, `RefactoringProvider`, `CodeGenerator` — all scoped by `LanguageId` ([Appendix I.3](appendix/I-language-soundness.md)) |
| `api.wizard` | T2-FX | `WizardIterator`, `WizardPanel`, `WizardDescriptor`, `@WizardRegistration` |
| `api.templates` | M | `TemplateRegistry`, `TemplateCreator`, `@TemplateRegistration` |
| `api.search` | T2-FX | `SearchProvider`, `SearchRequest`, `SearchResult`, `SearchScope` |
| `api.debug` | M | `DebuggerBackend`, `DebuggerSession`, `Breakpoint`, `StackFrame`, `Variable` |
| `api.profile` | M | `ProfileBackend`, `ProfileSession`, `StackTree`, `HeapSnapshot` |
| `api.tests` | M | `TestRunner`, `TestSession`, `TestReportParser`, `CoverageProvider` |
| `api.vcs` | M | `VcsProvider`, `VcsStatus`, `VcsHistoryProvider`, `VcsAnnotator` |
| `api.diff` | M | `Diff`, `DiffResult`, `Patch`, `Merge` (headless engine) |
| `api.tasklist` | M | `TaskProvider`, `Task`, `TaskScope` |
| `api.plugins` | T1 | plugin metadata, install/update service surface |
| `api.layer` | T1 | `LayerPaths` — the one owner of the layer folder constants (`NbFx/Views`, …); nbfx-internal, not part of the published plugin API (§3.3, Appendix C.3) |

**Rule of creation:** an `api.*` module exists only when a second module (or a plugin) needs it. Start with
T0/T1 inside the owning module and promote deliberately. The table is the **target catalogue**, not a backlog:
`refactor/api-split` creates only the modules whose consumers exist at that point; the rest are created by their
own step. A module with a planned consumer is `M`/T2-FX from the start and published `devel` when its first
consumer lands — the word *proposed* does not appear in a tier column (Decision 21).

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
| `platform.progress` | progress UI (status line, all-tasks popup, task details) | replaces `nbfx.progress` (landed, PR #2 — §18) |
| `platform.theme` | theme manager, light/dark palettes, branding, editor syntax palettes | new |
| `platform.settings` | settings storage UI (preferences browser, export/import) | new |
| `platform.options` | options window (categories, tabs, search, validity) | replaces `nbfx.options` UI |
| `platform.nodes` | FX node views + property sheet on `api.nodes`/`api.property` | new (merges `nbfx.nodes.ui` + `nbfx.propertysheet`); the single construction site of `TreeView`/`TableView` — [Appendix F](appendix/F-node-and-view-migration.md) |
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
| `ide.structure.xml`, `ide.structure.css` | XML/CSS structure providers for the generic structure panel | from `nbfx.structure.xml`/`nbfx.structure.css` (landed, PR #5) |
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
| `java.model` | **T0 bridge** over `org.netbeans.api.java.source`/`libs.javacapi`: classpath cache, in-memory `FileObject`, task runner, position↔offset, nb-javac normalisation into `api.language`/`java.elements` values (Appendix B) |
| `java.elements` | the Java element vocabulary (`SourceElementKind`, `SourceTypeKind`, `SourceModifiers`) moved out of the neutral `api.*` ([Appendix I.3](appendix/I-language-soundness.md)) |
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

**Deferred — created only when the owning step lands, never before:** `libs.jline` (terminal, W3),
`libs.asyncprofiler.<os>-<arch>` (profiler, M2), `libs.jgit` (Git, W4 — reuse NetBeans' jgit modules),
`libs.jacoco` (coverage, M2 — reuse the shipped `org.jacoco.core`). A `libs.*` module exists only when the step
that needs it starts ([Appendix G](appendix/G-licences-and-notices.md) rule 6).

## 7. Module migration map (current → target)

| Current module | Target |
|---|---|
| `nbfx.api` (61 classes in 12 packages + 4 root classes) | `api.actions`; `api.views` (from `api.view` + `api.icons`); `api.status`; `api.editor` (from `api.editor` + `api.breadcrumbs` + `api.completion`); `api.elements` → `api.language` (neutral `ElementRef`/`SourceLocation`/`ElementIcons`) + `java.elements` (`SourceElementKind`/`SourceTypeKind`/`SourceModifiers`); `api.project`; `api.build`; `api.progress`; `api.search`; `api.file` → `api.editor`/`ide.file`. Root classes: `Cancellation` → `api.progress`, `ContentManager` → `api.editor`, `ErrorReporter` → `api.dialogs`, `FxApplicationMenu` → `api.views`/`platform.menus` |
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
| `nbfx.editor`, `nbfx.editor.actions` (88 files, `figures.md`) | `platform.editor` (Java-free core, ~50 files) + `java.editor` (Java, ~36 files) + `ide.editor.{xml,css}` + `api.editor` (seam) |
| `nbfx.structure` | `ide.structure` |
| `nbfx.structure.css`, `nbfx.structure.xml` | `ide.structure.css`, `ide.structure.xml` |
| `nbfx.progress` | `platform.progress` (+ `api.progress` seam) |
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
has 43 modules / 582 source files (`nbfx.editor` 88, `nbfx.api` 63, `nbfx.ui` 40, `nbfx.findinprojects` 39,
`nbfx.structure.java` 32, `nbfx.findusages` 34). A reverse index (target ← sources) is added when the split
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
  convert by name (Appendix A.4, `java.elements.SourceModifiers`).
- **Preferences** keep the NetBeans nodes where a reused headless module owns them; nbfx-specific keys use
  `nbfx.<capability>.<key>`. Module code-name changes move `NbPreferences` roots: covered by a round-trip
  test in `refactor/module-names`, and code-names freeze at 1.0.
- **Formats**: we *read* `.nps`, `.hprof`, `.jfr`; we write only formats we own or that are public.
- **Keymap/breakpoints/platform storage** stay in the platform storage; the UI reads/writes them.
- **Debuggees/VCS/native tools** are launched through `api.execution` (PTY by default for run), so no
  module starts processes itself.

### 8.1 Baseline (the compatibility contract)

The plan stated none of this; a plugin author and the CI both need it.

| Item | Value | Consequence |
|---|---|---|
| **Minimum JDK** | **25** (LTS) | every nbfx module compiles with `javac.release=25` (as `lib.javafx` does today) → class file 69 |
| **JavaFX** | **27** (`lib.javafx/javafx.version`) | bundled jars in `libs.javafx/release/modules/ext/`: `javafx.base`, `javafx.graphics`, `javafx.controls`, `javafx.fxml`, `jfx.incubator.richtext`, `jfx.incubator.input`; **no `javafx.web`** (licence and size, [Appendix G](appendix/G-licences-and-notices.md)) |
| **FX internals** | `platform.editor` uses `com.sun.jfx.incubator.scene.control.richtext.{RichTextAreaSkinHelper,VFlow,CaretInfo}` | reachable today because the FX jars sit on the **classpath (unnamed module)**; if FX ever moves to the module path, the launcher must add `--add-exports javafx.controls/com.sun.jfx.incubator.scene.control.richtext=ALL-UNNAMED` — centralised in one launcher config, never scattered |
| **JPMS posture** | NetBeans module system on the classpath; FX is an autoload module (`is.autoload=true`) started by `launcher.FxGuiRunLevel` (`Application.launch`) | no module-path requirement today; a future `jpackage`/`jlink` step must revisit this ([§11 W10](PLAN.md)) |
| **Profiler** | **agent-free** | the NetBeans profiler agent cannot read class files newer than **55 (JDK 11)** while the target is JDK 25 → profiling = JFR + bundled async-profiler + a JMX sampler. Record this, or someone will "fix" it by re-adding the agent |
| **Test JVM** | Headless glass (§10.3) | FX tests run without a window in CI; no Monocle |

Checked by `check-baseline` (§9): one `javac.release` across nbfx modules; no `javafx.web` in the bundle; the
launcher's `--add-exports` list is empty today and lives in one file.

## 9. Guardrails (structural, in the build)

The rule is [Appendix A](appendix/A-toolkit-policy.md); this is the implementation. **Status is honest**: five
checks exist today (`check-purity.xml` has `check-sources`, `check-dependencies`, `check-jars`,
`check-packaging`, `check-figures`), the rest are `pending` with the step that lands them — a pending check is
not a guardrail until `feature/ci` runs it (§11 W0/W12).

| Target | Status | Mechanism | Fails when |
|---|---|---|---|
| `check-sources` | **exists** | import denylist + per-module allowlist | a forbidden import family outside its declared owner |
| `check-dependencies` | **exists** | walk each `project.xml` graph | a forbidden NetBeans UI module, or an upward layer edge |
| `check-jars` | **exists** (extend to 6 clusters + `jdeps`) | `jdeps` (references) **and** jar listing (bundled classes), on **all** clusters (`nbfx-libs`, `nbfx-api`, `nbfx-platform`, `nbfx-ide`, `nbfx-java`, `nbfx-javafx`) | a reference/bundle of a forbidden package outside an allowed tier or exemption |
| `check-packaging` | **exists** (extend to the retirement matrix) | the retirement matrix (absent modules per milestone) | a retired module reappears in the distribution |
| `check-figures` | **exists** | `ant -f check-purity.xml check-figures` → `java docs/plan/tools/Counts.java --check` | stale figures, a hand-typed count ([`figures.md`](figures.md)) |
| `check-java-source` | pending (W1) | `org.netbeans.api.java.source`/`libs.javacapi` only in the `java.*` semantic modules | imported anywhere else (Appendix B.5) |
| `check-languages` | pending (W1/W11) | core has no `LanguageId`/content-type branch; an unregistered MIME resolves to **plain text**; providers are language-scoped (Appendix I.3/I.4) | an `if (java)` in the core, or an unknown language falling back to Java |
| `check-api` | pending (W2) | exported set ⊆ `arch.xml` set; dependant spec versions exist | a subtree export, a missing `arch.xml` `<api>` entry |
| `check-naming` | pending (W1) | no `Fx*`/`Impl`/`Object`-factory in public API; `LayerPaths` is the only shared constants class | a forbidden public name |
| `check-baseline` | pending (W1) | one `javac.release`; no `javafx.web`; one `--add-exports` file (§8.1) | a drifting JDK/FX/class-file baseline |
| `check-i18n` | pending (W4) | every user-visible string is an `NbBundle` key in `Bundle.properties` | a missing key, a hardcoded string |
| `check-logging` | pending (W1) | no `System.out`/`printStackTrace`; `Logger` named after the module | a stray print |
| `check-lookup` | pending (W3) | the `Lookups.forPath` invocation count is constant across N user actions (Appendix E.5) | a per-action layer lookup |
| leak test | pending (W3) | open/close a view N times; listener count returns to baseline and the view is weakly reachable (Appendix E.2) | a leaked subscription |
| `check-licences` | pending (W10) | every bundled jar/native has a `NOTICE` entry; `javafx.web` absent (`LICENSE`/`THIRDPARTY` present) ([Appendix G](appendix/G-licences-and-notices.md)) | an unnotified bundle |
| `check-budget` | pending (W0) | startup, "usable", first completion, module count, memory | >20 % regression vs the milestone baseline |

The old, grep-only `check-jars` greps the *file list* of three cluster directories for `javax/swing/`, so a
class that imports a JDK `javax.swing` type passes and the two new clusters are unscanned; that is the gap
`jdeps` closes.

## 10. Roadmap

| Milestone | Target | Content | Exit |
|---|---|---|---|
| **M0 — Shell & platform** | 2026-12 | shell/docking/actions/menus/status, dialogs, progress, theme, options, settings UI, nodes+property views, output, execution, terminal, quick search, credentials, favourites, templates, wizard, packaging (run from the built distribution) | the walking-skeleton scenario (open→edit→build→run→output→commit) runs on a **real Ant/J2SE fixture project** (the Ant/Java project types exist; Maven/Gradle arrive in M2); the `projectuiapi.base` bridge in `launcher`/`ui` is **removed** and the M0 rows of the retirement matrix are absent; `refactor/module-names` + `refactor/api-split` landed; all guards green; budget baseline recorded |
| **M1 — IDE core** | 2027-03 | project model, editor (core + Java), structure, search, usages (generic + Java), file actions, VCS+Git, diff, local history, tests **UI**, libraries, task list/bookmarks, plugins UI | open/edit/search/find/commit/run-tests in FX on a J2SE project; every explorer panel renders through `api.nodes` (except popups) — [Appendix F](appendix/F-node-and-view-migration.md); the M1 rows of the retirement matrix absent; guards green |
| **M2 — Java tooling** | 2027-07 | project types (J2SE/Maven/Gradle/Ant), Java Platforms, editor intelligence (goto, usages, hints, refactoring, codegen, Javadoc), debugger, profiler, Java tests engines/coverage | daily Java work on a Maven **and** an Ant project; the M2 rows of the retirement matrix absent; guards green |
| **M3 — JavaFX usage** | 2027-09 | JavaFX project, FXML editing, SceneBuilder | a JavaFX project opens, edits FXML, and runs; `javafx.enabled` round-trips |
| **M4 — Breadth** | 2027-12 | i18n, beans, JShell, remote debug/profiler, export/import, accessibility, multi-window, module graph view, OS bundles/installers (jpackage, Decision 19) | the intended feature scope is present; guards green; a11y pass recorded |
| **M5 — Ecosystem** | 2028 | plugin API hardening, compatibility reports, update centers, docs/tutorials | a third-party plugin builds against the published API and is published |

### 10.1 Critical path

The chain that decides the dates; everything else can slip around it:

```
feature/walking-skeleton → feature/ci → refactor/guard-java-source → refactor/api-naming → refactor/module-names → refactor/api-split
  → refactor/nodes-model (platform.nodes)         → feature/editor (platform.editor)
  → refactor/java-plumbing (java.model)           → refactor/editor-split (java.editor)
  → feature/project-model (api.project)           → feature/projecttypes.java (J2SE)
  → feature/build-run (api.execution)             → feature/debugger (java.debug)
```

The editor branch (`platform.editor` → `java.editor`) and the project branch
(`api.project` → `java.projecttype.java`) run in parallel after `api-split`; they meet at
`feature/build-run`. M2 ("daily Java work") is gated by `feature/debugger`.

Two upstream prototypes run in parallel with the critical path and are **not** allowed to delay it:
`chore/upstream-module-tools` (H.6, portable `nbbuild` checks → `nbfx-tools/`) with `refactor/guardrails`, and
`spike/java-source-headless` (H.1) with `refactor/java-plumbing`; `spike/progress-neutral-spi` (H.2) is the M0
upstream win.

### 10.2 Cut list (in cut order)

If a date is missed, cut in this order; each cut is a plan change, not a silent slip:
1. **M4 breadth** — JShell, beans/i18n tooling, module-graph view, remote debug/profiler, multi-window.
2. **M3 SceneBuilder** — FXML editing stays; the embedded SceneBuilder editor goes.
3. **Profiler** beyond JFR sampling; heap-dump analysis visualisation.
4. **Non-Git VCS** providers.
5. **Maven/Gradle** project types — J2SE + Ant first, Maven/Gradle later (the M2 exit changes to "Ant and
   J2SE", the original JavaFX-cluster scope).
Not cuttable: the walking skeleton, the guards, the API split, the editor core, the J2SE project type.

### 10.3 Test kit (decided here, implemented in W2)

- **FX headless**: **glass headless** (`-Dglass.platform=Headless -Dprism.order=sw`), already used by
  `nbfx.api`, `docking`, `progress` and `windows`; no test opens a real window in CI. Monocle is not bundled
  and not needed.
- **UI**: TestFX (`FxRobot`) only for shell scenarios; the W0 scenario is service-level (no UI events).
  Use `org.testfx:testfx-core:4.0.19-gluon` (the Gluon build) with the Headless platform; official TestFX
  still pulls in Monocle, which nbfx does not use.
- **QA placement**: end-to-end scenarios live in the application assembly (`nbfx-ide`: launcher `test/unit` or
  a test-only module); platform modules keep service tests. `<test-dependency>` may cross layers, is never a
  run dependency and the test artefact is not shipped; the graph guard treats test edges separately. If
  scenarios multiply, move them to a test-only `nbfx-qa` cluster.
- **Harness**: NetBeans `nbjunit` (already used: 26 test classes today) + `nbbuild` `sigtest` targets for the
  governed API modules.
- **Required test types**: unit (model, `M` modules), contract (threading per SPI, layer writer/reader), leak
  (open/close, Appendix E.2), budget (startup/usable/first completion, `check-budget`), smoke
  (walking skeleton, W0). Baseline to beat: **32 test classes, zero signature tests, zero FX tests** — state
  the number in the W2 acceptance.
- **Fast loop**: `nbfx.overlay=symlink` and `ant module -Dmodule=<cluster>/<module>` (PR #8) build one module in
  seconds; CI and the W0 smoke test use the same targets, so the loop and the gate cannot diverge.

### 10.4 Owners and capacity

A plan without owners is a wish. Every step in §11 must have an owner and a size (S/M/L) before M0 starts;
the table is maintained in the issue tracker, not here. Until then, no step is "in progress".

## 11. Workstreams (the work, sliced)

Each workstream is a branch family; each step lands alone. **Order inside a workstream matters; across
workstreams only the stated dependencies do.**

### 11.0 Definition of done (every step, no exceptions)

1. **Scope** — one concern, one sentence; no drive-by refactors.
2. **Acceptance test** — named, added in the same commit, failing before the change.
3. **Guards green** — the §9 checks, plus `check-figures` when a number changed and `check-baseline` when a
   build setting changed.
4. **Docs** — the module's `arch.xml`/`apichanges.xml` entry when a public type changed; this plan updated when
   a rule or a figure changed.
5. **Rollback** — revertable without touching later steps; no half-migrations.
6. **Green trunk** — after every integration commit the application still **starts, opens a file, edits it,
   builds and runs**. A refactor that leaves the app unusable for more than one step is rejected; the module
   being replaced stays in the distribution until its replacement passes the scenario.

### 11.0.1 Acceptance per workstream (the evidence that closes it)

| Workstream | Acceptance |
|---|---|
| W0 walking skeleton | the scenario (open → edit → build → run → output → commit) runs on a J2SE project; budget baseline recorded |
| W1 containment/naming/split | `check-java-source` green with the 4-module allowlist; no `Fx*` in an exported package; all consumers compile after `api-split`; layer contract test green; `check-jars` scans five clusters |
| W2 governance | every governed module has `arch.xml` + `apichanges` + a `sigtest` target; spec-version guard green; exemption register has owners and dates; the language conformance fixture passes |
| W3 platform services | each service documents its thread, disposal and lease tests; index-on-open with progress and cancellation |
| W4 IDE core | the M1 scenario (open/edit/search/find/commit/run-tests) on a J2SE project; every explorer panel renders through `api.nodes` |
| W5 Java tooling | the M2 scenario on an Ant **and** a Maven project, including a debugging session |
| W6 JavaFX usage | FXML edits and runs; `javafx.enabled` round-trips |
| W7 *(withdrawn)* | — |
| W8 upstream/retirement | the milestone rows of the retirement matrix are absent (guard); `chore/upstream-sync` green |
| W9 upstream proposals | ≥1 accepted upstream change; each proposal filed with its prototype, size and fallback |
| W10 packaging | a clean machine installs, launches and passes the guards on the packaged image |
| W11 language soundness | the conformance fixture passes in CI; a second language (Groovy) is highlighted, completed and run without editing core; the PHP/web spikes decided `api.servers`; `check-languages` green |
| W12 CI | the workflow is red on a planted violation and green on the current tree |

**W0 — Walking skeleton & baseline (first on the critical path).** `feature/walking-skeleton`: make the
existing shell modules run **open → edit → build → run → output → commit** on a real Ant/J2SE project and
record the budget baseline. The skeleton is the file set that **exists today** (`figures.md`: launcher, ui,
docking, windows, menus, statusbar, output, editor, project.context, api) — **no new module, no split**.

1. **Fixture** — `nbfx-tests/fixtures/ant-hello`, adapted from
   `netbeans/ide/jellytools.ide/test/qa-functional/data/SampleProject` (J2SE Ant, `build-impl.xml` checked
   in); `hello.Main` prints `NB_FX_SCENARIO_OK`. Acceptance: `ant clean build run` prints the marker.
2. **Scenario** — `ScenarioIT` in `nbfx-ide/.../nbfx.launcher/test/unit` with test-only deps on
   `nbfx.editor`, `nbfx.run`, `nbfx.vcs`, `nbfx.output`, `project.ui.ant`: copy the fixture to a temp git
   repo, wait for the async `OpenProjects` open, edit+save, build, run (assert the marker), commit, assert a
   clean status. Headless: `-Dglass.platform=Headless -Dprism.order=sw`.
3. **Budget** — a `--budget` mode on the launcher records startup→usable, open, first editor, build; median of
   5; commit the baseline; `check-budget` compares.
4. **CI** — Linux, JDK 25: `ant bootstrap`, `ant build`, `check-purity`, `Counts --check`, the scenario and
   the touched-module tests; PR template and required check.
5. **Close** — green from a clean checkout, `docs/w0-scenario.md`, a §18 row.

Acceptance: the scenario runs headless from a clean checkout, the budget numbers are recorded, and everything
else keeps the trunk green (§11.0.6).

**W1 — Containment, naming and API split.** `refactor/guard-java-source` (denylist/allowlist guard over the
current 4-module allowlist; [Appendix B.5](appendix/B-java-source-isolation.md)) → `refactor/api-naming`
([Appendix C](appendix/C-naming-and-rename-table.md): drop `Fx*` from the 56 exported types, move processors
out of exported packages, introduce `api.layer.LayerPaths`; no behaviour change) →
`refactor/java-plumbing` (`java.model` bridge; dedupe the three in-memory-file helpers and the position
conversions) → `refactor/editor-split` (`platform.editor` / `java.editor` / `ide.editor.{xml,css}`; delivers the
`api.editor` decoration SPI + `api.language`, acceptance: XML/CSS are modules and the core contains no
language branch) →
`refactor/usages-split` (`api.usages` + `ide.usages` + `java.usages`) → `refactor/cluster-layout` (create the
`nbfx-libs`, `nbfx-api` and `nbfx-javafx` clusters: `cluster/nbfx-cluster.properties.fragment`, `nbbuild`/CI
wiring, and the `check-purity.xml` `check-jars` includes + `${nbfx.dist.dir}` scans extended to all five
cluster dirs; acceptance: `check-jars` scans five clusters and a dry-run build produces them) →
`refactor/module-names` (rename to
§3.1; settings/layer round-trip test) → `refactor/api-split` (create the `api.*` modules; move packages
**wholesale** — same package names, consumers update only their `project.xml`; topological order:
elements/file/project → editor → actions/views → root last; no shims) →
`refactor/objects-not-statics` (services with lifecycles; typed contracts; no `Lookup.forPath` per call).
The writer/reader layer contract test belongs to `api-split`.

**W2 — API governance.** `feature/api-governance`: `arch.xml`/`apichanges`/signature tests for every
T1/T2-FX/M module, the version baseline (1.0 + bump rule) and the spec-version guard; the audit/guard
implementation (`refactor/guardrails`); the exemption register with owners and dates.
`feature/language-conformance`: the **fake-language fixture** (`LanguageId("text/x-nbfx-conformance")` with
decoration, completion, structure, goto and usages) — the CI proof of the core freeze
([Appendix I.4](appendix/I-language-soundness.md)).

**W3 — Shell and platform services.** Per service, on its own branch: `feature/shell`,
`feature/workspace`, `feature/dialogs`, `feature/progress`, `feature/theme`, `feature/settings-ui`,
`feature/options`, `feature/nodes-views`, `feature/output`, `feature/execution`, `feature/terminal`,
`feature/quicksearch`, `feature/credentials`, `feature/favorites`, `feature/templates`, `feature/wizard`,
`feature/indexing` (reuse `parsing.api`/`parsing.indexing`; index-on-open with progress, cancellation and a
memory budget — [Appendix D.5](appendix/D-bridge-and-retirement-matrix.md)).

**W4 — IDE core.** `feature/project-model`, `feature/project-ui`,
`refactor/nodes-model` → `refactor/nodes-usages` → `refactor/nodes-navigator` →
`refactor/nodes-properties` → `refactor/nodes-misc` (the node/view consolidation;
[Appendix F.3](appendix/F-node-and-view-migration.md)), `feature/editor`,
`feature/navigation-search`, `feature/refactoring`, `feature/hints`, `feature/codegen`,
`feature/javadoc`, `feature/structure`, `feature/diff`, `feature/vcs`, `feature/localhistory`,
`feature/tests`, `feature/libraries`, `feature/tasklist-bookmarks`, `feature/plugins-ui`,
`feature/modulegraph`.

**W5 — Java tooling.** `feature/java-platforms`, `feature/projecttypes` (java/maven/gradle/ant),
`feature/java-editor`, `feature/debugger`, `feature/profiler`, `feature/java-tests-coverage`.

**W6 — JavaFX usage.** `feature/javafx-project`, `feature/fxml`, `feature/scenebuilder`.

**W7 — (withdrawn).** The front-end factory migration is void: §5.2 and
[Appendix E.7](appendix/E-contracts.md) publish **concrete FX factories** (one front end), so
`spike/front-end-component-factory` and `spi-factory-migration` are removed. Portability lives in the
toolkit-free `PanelModel`/`FieldModel`; revisit generics only if a second component toolkit appears.

**W8 — Upstream and retirement.** `chore/upstream-sync` (monthly pin/patch rebase, patch register, patch
budget, CI), `refactor/swing-retirement` (execute the retirement matrix of
[Appendix D.4](appendix/D-bridge-and-retirement-matrix.md); `check-packaging` asserts the absence),
`feature/plugin-api` (contract docs, sample plugin, trust), `feature/plugin-quickstart` (the skeleton plugin,
its NBM build and a view + a command — [Appendix E.6.1](appendix/E-contracts.md)).

**W12 — CI.** `feature/ci`: one workflow running, in order, `ant -f check-purity.xml check-purity`,
`check-figures`, the cluster build and the W0 smoke test, and publishing the freeze baseline. Acceptance: red on
a planted violation, green on the current tree — without it "green trunk" is an assertion (§9).

**W10 — Packaging and distribution.** `feature/packaging`: the app image built from `nbbuild/netbeans` (M0),
then OS bundles/installers (M4) carrying the `libs.*` natives and the licences
([Appendix G](appendix/G-licences-and-notices.md)); branding (icons, splash, about), first-run and the update
centers. **Decide jpackage vs NBI here** (Decision 19). Acceptance: a clean machine installs, launches and
passes `check-licences`/`check-baseline`/`check-figures` on the packaged image.

**W11 — Language soundness.** Per [Appendix I](appendix/I-language-soundness.md): `spike/lang-groovy` (JVM,
no build, no server — the headline exercise of `api.language`), `spike/lang-php` (no build, interpreter +
dev server — decides `api.servers`), `spike/web-javaee` (a thin project-type + deploy slice). Time-boxed, after
`platform.editor` + `api.*` and before the M5 freeze; the deliverable is a **hole list + the smallest core API
fix**, and the core freeze (`check-languages`) is the acceptance. Each spike has a branch, a time box, an owner
and a **kill criterion**; spike code is deleted unless it survives as a real (optional) provider module. No
feature growth — the cut list kills a spike that becomes a language IDE.


**W9 — Upstream proposals.** Per [Appendix H](appendix/H-upstream-proposals.md), each with a tracked upstream
issue, a prototype in nbfx and a recorded fallback: `chore/upstream-module-tools` (the portable `nbbuild`
checks, H.6 — portable ant tasks under `nbfx-tools/`), `spike/progress-neutral-spi` (H.2),
`spike/java-source-headless` (H.1), `spike/actions-neutral-core` (H.3), `spike/nodes-icon-source` (H.4),
`spike/loaders-node-free` (H.4.1),
`spike/runlevel-api` (H.7), `spike/maven-embedder-facade` (H.7), `spike/editor-model-spi` (H.5, last).
Order and the per-proposal credibility test are in [H.8](appendix/H-upstream-proposals.md).

## 12. Mirror and evidence

- **No hand-typed numbers.** Every count the plan states is generated by
  [`tools/Counts.java`](tools/Counts.java) into [`figures.md`](figures.md); `check-figures` fails when the file is
  stale. If a number is worth writing, it is worth deriving.
- The **archived** documents under `docs/plan/archive/` hold the research that justifies each workstream
  (NetBeans class/file references, counts, formats, licences). Consult them for evidence; do not treat
  them as the plan.
- `docs/architecture-rules.md` is a one-screen **index** of every rule and its normative source in this plan;
  it deliberately restates no rule detail. `docs/repo-and-build.md` documents the harness; `docs/MVP.md` is a
  historical draft.

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
    asserted by the guard, per the matrix of
    [Appendix D.4](appendix/D-bridge-and-retirement-matrix.md). For every upstream UI API, the mechanism is
    *reimplement behind the API* when the API is provider-based (and the bridge carries the documented Swing
    exemption), *replace with an nbfx API* otherwise ([Appendix A.3](appendix/A-toolkit-policy.md)).
12. **Layer-path constants have one owner**, the module `api.layer` (T1) with the class `LayerPaths`; the
    writer/reader agreement is a contract test (§3.3). No "private constant per reader".
13. **The Java semantic layer is the only importer of `java.source`/`libs.javacapi`**, behind the `java.model`
    bridge; `javax.swing.text.Document` is banned everywhere
    ([Appendix B](appendix/B-java-source-isolation.md)).
14. **Upstream reuse edges are not layer edges**: an nbfx module depending on a NetBeans module is recorded
    with a justification and a retirement plan; the layer rule constrains nbfx → nbfx edges only.
15. **Project type ids and the `javafx.enabled` property match the NetBeans convention**
    (`org.netbeans.modules.java.j2seproject`); module renames never change them (§8).
16. **Compiler**: JDK `javac` by default, nb-javac only where the project's source level requires it; the
    choice is owned by `java.model` and recorded in the module metadata
    ([Appendix D.6](appendix/D-bridge-and-retirement-matrix.md)).
17. **Upstream agenda** per [Appendix H](appendix/H-upstream-proposals.md): additive-only, new module +
    compatibility wrapper, `ModuleAutoDeps` carries the move, the surface is frozen by `sigtest`, a prototype in
    nbfx is the first customer, and a fallback is recorded. **At least one accepted upstream change per
    milestone.**
18. **Baseline**: JDK 25, JavaFX 27, `javac.release=25`, FX on the classpath (unnamed module) with the
    `--add-exports` list centralised if that ever changes, agent-free profiling, Headless glass for tests (§8.1).
19. **Packaging**: build the app image from `nbbuild/netbeans` at M0, and produce OS bundles/installers with
    **jpackage** at M4 unless a spike shows it cannot meet the requirement (then NBI). Plugins are built as
    **NBMs** with the NetBeans `nbm` Ant harness (Appendix E.6.1).
20. **Language registry**: `api.language` (M) is the one registry; providers are scoped by `LanguageId`;
    syntax decoration is a provider, not a core branch; the neutral `ElementRef` (with a language-defined
    `handle()`) lives in `api.language` and the Java vocabulary moves to `java.elements`; an unregistered MIME
    resolves to **plain text** ([Appendix I](appendix/I-language-soundness.md)). The core must not branch on a
    language (`check-languages`). `api.language` ships `arch.xml category="devel"` and is **not frozen** until
    the conformance fixture and one real second language (Groovy) pass.
21. **Tier discipline**: an `api.*` module with a planned consumer is `M`/T2-FX from the start and published
    `devel` when its first consumer lands; no "proposed" tier, no `libs.*` before its step, no capability
    before a spike decides it (§17).
22. **QA placement**: end-to-end scenarios live in the application assembly (`nbfx-ide`: launcher `test/unit`
    or a test-only module); platform modules keep service tests; `<test-dependency>` may cross layers, never
    run dependencies, never shipped; a test-only `nbfx-qa` cluster when scenarios multiply (§10.3).

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
| [C — Naming and rename table](appendix/C-naming-and-rename-table.md) | the rule per location, all 60 public `Fx*` types and their targets, `api.layer.LayerPaths`, the `refactor/api-naming` step |
| D — [Bridge and retirement matrix](appendix/D-bridge-and-retirement-matrix.md) | every upstream module (Swing API bridges, the 50 editor/Java modules (`figures.md`), the indexing dependency, the compiler choice) with mechanism, owner, milestone and exemptions, plus the retirement matrix per milestone |
| E — [Contracts](appendix/E-contracts.md) | threading per SPI, disposal/ownership + the leak test, accessibility, error/logging, the precise `Lookup` rule, the plugin allowlist, and the removal of the generic-`C` factories |
| F — [Node and view migration](appendix/F-node-and-view-migration.md) | the 25 tree/table/list view files, today's item models, the target `api.nodes` model, and the migration order |
| G — [Licences and notices](appendix/G-licences-and-notices.md) | JavaFX 27 (GPLv2+CE), async-profiler, JLine, jgit, JaCoCo; the NOTICE rules and the `check-licences` guard |
| H — [Upstream proposals](appendix/H-upstream-proposals.md) | the Swing-free API agenda for upstream NetBeans (`java.source`, `api.progress`, `openide.awt`, `openide.nodes`, the editor SPI, the `nbbuild` checks, the friend APIs) and where nbfx prototypes each one first |
| I — [Language soundness](appendix/I-language-soundness.md) | the second-implementation test: the four holes (core `switch`, unscoped providers, the Java-shaped `api.elements`, missing servers/build-less capabilities), the `api.language` design and core freeze, the Groovy/PHP/web spikes, and the measured reuse ratio across the other clusters |

## 16. Risks

Every entry has a **trigger**, so it is a decision rule, not a worry. Owners are filled before M0 (§10.4).

| Risk | Likelihood | Impact | Mitigation | Trigger |
|---|---|---|---|---|
| **R1** upstream refuses the `java.source` split (H.1) | medium | high | keep `java.model`, write the diff-apply path; time-boxed | no upstream decision by M1 |
| **R2** FX internals (`com.sun.jfx.*`) change or vanish in a JavaFX upgrade | medium | high | one adapter (`RichTextView`), pin FX 27, an upgrade spike per FX release | an FX 28 bump |
| **R3** the editor replacement (≈600 upstream files) overruns M1 | high | high | M1 editor scope = edit/highlight/complete/format/find; defer folding, macros, code templates | M1 mid-point slips one milestone |
| **R4** indexing memory/time on large projects | medium | medium | lazy indexing, budget in `check-budget`, degrade (no blocking) | first 10k-file project |
| **R5** pinned NetBeans submodule + friend patches drift | medium | medium | monthly `chore/upstream-sync`, patch budget, rebase CI | a patch fails to apply |
| **R6** JavaFX GPLv2+CE obligations conflict with the distribution | low | high | `check-licences`, NOTICE, exclude `javafx.web`, legal review before M0 ships | a clean-machine installer is produced |
| **R7** no third-party plugin authors appear | medium | high | publish the API as *devel* early, quickstart + sample, compatibility report | M5 with zero external plugins |
| **R8** Maven/Gradle project types need the embedder/friend patches | high | medium | process-based builds; Maven/Gradle are on the cut list (behind J2SE+Ant) | `0003` blocks an M2 step |
| **R9** FX threading/leak defects | high | medium | §E.1–E.3 contracts, leak test, green trunk | a leak test regression |
| **R10** startup/performance regression vs Swing | medium | high | budget baseline at M0, `check-budget` blocks | baseline recorded |
| **R11** settings/keymap data lost on module renames | medium | medium | round-trip test in `refactor/module-names`; code-names freeze at 1.0 | first code-name rename lands |
| **R12** JPDA/debugger integration (attach, sources, hot swap) underestimated | medium | medium | `java.debug` typed session services; M2 gate | debugger step slips |
| **R13** scope creep from the module catalogue | high | medium | Rule of Creation (§6.1), cut list (§10.2), owners before M0 | a new `api.*` module without a consumer #2 |
| **R14** a11y postponed and unfixable later | medium | medium | a11y is an M1 contract of `api.views` (E.3), checked in the shell | M1 exit |

## 17. Plan budget and freeze

The plan is a means, not a deliverable. Measured: ~1,950 lines across `PLAN.md` and its appendices, grown by
34 commits in two days with **no implementation commit** — the risk is now the plan itself.

- **Budget ~1,200 lines.** A review round **deletes at least as much as it adds**.
- **No new appendix without merging or removing one.**
- **No further review round** until a milestone artifact exists: W0 runs, `feature/ci` is green, or the language
  conformance fixture passes.
- **Delete speculation on sight**: a tier marked "proposed", a `libs.*` row for a library that is not bundled,
  an `api.*` module without a consumer #2, a capability (`servers`) before a spike decides it.
- The plan's purpose is to make the next step obvious; when it stops doing that, it is a cost.

## 18. Landed on main

The plan refers to `main`; this is what landed there after the plan was written. It is the only status list;
add a row when a PR lands and update the affected chapter in the same change.

| PR | What | Plan impact |
|---|---|---|
| #2 | FX progress UI: new `nbfx.progress`, `progress.ui` pruned; status element, all-tasks popup, View/Cancel | `platform.progress` replaces `nbfx.progress`; D.4 M0 row; progress is the first **landed** bridge |
| #5 | navigator for XML/FXML/CSS/Ant/Gradle; new `nbfx.structure.css`, `nbfx.structure.xml`, `CssTokens` | new catalogue rows (§6.3) and migration rows (§7); Appendix F rows |
| #8 | symlink overlay and single-module builds (`ant module -Dmodule=…`) | the fast loop for every step (§10.3, W12) |
| #11 | progress-bar styling | theme only |
| #12 | early `Node` initialization to avoid the startup deadlock | **debt**: the real fix is D.2.1 / H.4.1; keep until `openide.nodes` leaves the runtime |
