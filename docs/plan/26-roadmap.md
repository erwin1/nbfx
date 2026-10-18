# 26 — nbfx roadmap: layers, building blocks and milestones

This is the structural master document: what nbfx is, how the work is **layered**, which **building
blocks** belong to which layer, and the **milestones** in which they land. The topic docs (00–25) remain
the detail for each area; this doc is the map that gives them order and location.

## 1. What nbfx is

nbfx is a **pure-JavaFX IDE**, not a plug-in: a JavaFX **application shell** plus JavaFX replacements for
every user-facing service of the NetBeans platform, IDE and Java clusters. It reuses the NetBeans
**headless engines** as-is (project model, parsing/indexing, `java.source`, build/execution, debugger,
profiler, VCS, templates, autoupdate) and replaces the **presentation layer** with a JavaFX one that has a
strict model/UI split and modern APIs (JavaFX properties/observables, `CompletionStage`/async,
`Lookup`-based services, declarative layer annotations, theme tokens).

Two consequences for planning:

- JavaFX is the **UI toolkit**, not the goal. "JavaFX project support" (FXML, SceneBuilder, JavaFX
  platform) is one *usage* layer on top of a complete IDE — milestone M3 in §6.
- Every task has a **home layer**. A block that only the Java usage needs goes to `nbfx-java`; if two usage
  layers would need it, it belongs to `nbfx-ide` or `nbfx-platform`.

## 2. The layer model

```
  usage layers        nbfx-javafx     JavaFX project, FXML completion, embedded SceneBuilder,
  (language/type)     nbfx-java       Java/JVM: project types (J2SE Ant, Maven, Gradle, apisupport),
                                      Java Platforms, build tools, Java debug backend, Java profiler
                                      backend, Java editor semantics
  ────────────────────────────────────────────────────────────────────────────────────────────────
  IDE building        nbfx-ide        project API/UI (ProjectModel), nodes/views integration, editor
  blocks                              framework + editor intelligence (navigation, refactoring, hints,
                                      codegen, Javadoc), structure/navigator, find usages / in projects,
                                      diff/patch, VCS core + Git, test runner/coverage, debugger UI,
                                      profiler UI, plugins UI, task list/bookmarks, libraries manager
  ────────────────────────────────────────────────────────────────────────────────────────────────
  platform            nbfx-platform   views/docking, actions/menus/toolbars, status bar, notifications,
  building blocks                     dialogs, wizard, progress, options/settings, theming/branding,
                                      node/view model, property sheet/editors, output, execution,
                                      terminal, quick search, keyring/credentials, favorites, templates
  ────────────────────────────────────────────────────────────────────────────────────────────────
  external libraries  libs.*          jline, async-profiler binaries, jgit, jacoco, javafx, jna, …
  ────────────────────────────────────────────────────────────────────────────────────────────────
  reused runtime      nb.cluster.*    headless NetBeans engines (platform/ide/java/extide/apisupport)
                                      — never modified, only depended on / patched where unavoidable
```

**Dependency rule (top → bottom only):** `nbfx-javafx`/`nbfx-java` → `nbfx-ide` → `nbfx-platform` →
`libs.*` → `nb.cluster.*`. No upward dependencies, no sibling cycles; **model modules are JavaFX-free**
(`check-models`), UI modules must not import the prohibited Swing/legacy modules
(`openide.awt/nodes/explorer/windows/dialogs`, `core.windows`) — see `architecture-rules.md`.

## 3. Allocation principles (where does a block go?)

1. **platform** — language- and project-agnostic, needed by any nbfx application: window/docking, actions,
   status, notifications, dialogs, wizard, progress, settings/options, theme, node/view model, property
   sheet, output, execution, terminal, quick search, credentials, templates.
2. **ide** — generic IDE concepts, independent of language: projects (API/UI), editor framework and
   intelligence, structure/navigator, find/usages, diff, VCS, tests, debugger/profiler *frameworks and
   views*, plugins, task list/bookmarks, libraries.
3. **java / javafx** — language/type specifics: Java project types and build/run integration, Java
   Platforms, Java sources/semantics, JPDA/JFR integration, JavaFX project/FXML/SceneBuilder.
4. **Who uses it decides:** if a future language layer (Kotlin, C/C++, …) would need the block unchanged,
   it belongs in `ide` or `platform`.
5. **Data vs presentation:** NetBeans data/formats (project properties, `.nps`/`.hprof`/`.jfr`, keymap,
   breakpoints, platform storage) stay compatible; the *presentation* is replaced.
6. **Registration ownership:** a layer-generating annotation (and its processor) lives with the SPI it
   complements — options registrations in `nbfx.options`, wizard in `nbfx.wizard`, property editors with
   the editors, project types in the project API module — and only the shell registrations
   (actions/views/menus/toolbars/status) stay in the core `nbfx.annotations`. Annotation modules are
   **compile-only**. See [29-annotation-modules.md](29-annotation-modules.md).

### 3.1 Moves and renames the layer model implies

| Block today | Move to | Why |
|---|---|---|
| `nbfx.api.project` (`OpenProject`, `ProjectRegistry`) inside `c.gluonhq.netbeans.nbfx.api` (nbfx-platform) | `nbfx-ide` (`nbfx-project-api`) | the project model is an IDE concept (`ide/projectapi`), not platform |
| `nbfx.propertysheet` (nbfx-ide) | `nbfx-platform` (generic views) | the property sheet is a generic view; project *customizers* stay in `nbfx-ide` |
| `nbfx.debug`, `nbfx.run`, `nbfx.findusages`, `nbfx.vcs`, `nbfx.structure` (nbfx-ide) | stay `nbfx-ide` | language-agnostic UI; the JPDA/JFR **backends** move to `nbfx-java` |
| `nbfx.build.ant`, `nbfx.project.ui.*`, `nbfx.structure.java` (nbfx-java) | stay `nbfx-java` | Java project types |
| `nbfx.output` / future `nbfx.execution`, `nbfx.terminal` | `nbfx-platform` | generic services (used by build, debug, profiler, VCS, shell) |
| `nbfx.options` (nbfx-platform) | stays | generic settings UI |
| editor (`nbfx.editor`, `nbfx.editor.actions`) | `nbfx-ide` (framework) + `nbfx-java` (Java semantics) | the editor framework is language-agnostic; Java parser/completion/hints wiring is `nbfx-java` |

## 4. Building blocks per layer (summary; details in doc 27)

### nbfx-platform
Landed: `actions`, `annotations`, `api` (actions/views/status/editor/progress APIs), `context`, `docking`,
`lib.javafx`, `macos`, `menus`, `output`, `statusbar`, `windows`, `wizard`.
Planned/missing: options+settings (14), theming/branding (10), dialogs/notifications (09),
node/view model + views (12), property sheet/editors (13), execution service (15), terminal (15), quick
search (18/14), keyring/credentials (24), favorites, templates (`nbfx.templates`).

### nbfx-ide
Landed: `editor` (framework, completion, breadcrumbs, annotations), `editor.actions`, `file.actions`,
`findinprojects`, `findusages`, `launcher`, `plugins`, `project`(+`project.actions`, `.context`,
`.customizer`, `.ui`, `.ui.api`), `propertysheet`, `run`, `structure`, `ui`, `vcs`, `debug`.
Planned/missing: navigation & search (18), refactoring (19), hints (20), codegen/editor actions (21),
Javadoc (22), test runner/coverage (23), diff/patch + Git UI + local history (24), task list/bookmarks,
libraries manager, module support, project groups, plugins UI parity (25).

### nbfx-java
Landed: `build.ant`, `project.ui.{ant,java,maven,gradle}`, `structure.java`.
Planned/missing: `java.platform` (doc 02), Java editor semantics (Java parser/completion/hints/refactoring
wiring), build runners (Maven tooling, Gradle tooling, Ant — process-based), Java run configurations,
Java debug backend (`nbfx.debug.java`: JPDA session/launch/breakpoints — doc 16), Java profiler backend
(`nbfx.profiler.java`: JFR/async-profiler backends — doc 17), test integration (doc 23).

### nbfx-javafx
Planned: `javafx.project` (JavaFX Ant project, doc 03), `javafx.platform` (JavaFX runtime/JavaFX-enabled
JDK), `fxml.editor` (FXML completion, doc 04), `scenebuilder` (embedded kit, doc 04).

### libs.* (external libraries)
`libs.javafx` (landed), `libs.jline` (15), `libs.asyncprofiler.*` per OS/arch (17), `libs.jgit` (24, reuse
NetBeans' jgit), `libs.jacoco` (23, reuse the shipped `org.jacoco.core`), `libs.jna` (reuse NetBeans).

## 5. Cross-cutting verticals (how a feature spans the layers)

Each vertical is one user-facing capability whose pieces live in different layers — this is what makes the
work *structurable*:

| Vertical | platform | ide | java / javafx |
|---|---|---|---|
| **Editor** | FX rich-text editor component, keymap, theme tokens | editor framework/actions, annotations, options page, structure view | Java parser/completion/semantic access, hints, refactoring, codegen |
| **Debugging** | views/property sheet, dialogs, output/terminal, execution | debugger framework + views (sessions/threads/variables/breakpoints), editor annotations | JPDA session/launch/attach (JVM), Ant/Maven/Gradle debug |
| **Profiling** | node/view model, charts primitives, options | profiler window + CPU/memory/locks/threads/heap views | JFR/async-profiler backends, run integration |
| **VCS** | keyring/credentials, notifications, progress, diff-view primitives | VCS framework, status/badges, Git UI, history/blame, local history | project-type hooks (gitignore, ignore rules per project) |
| **Testing** | progress, output, execution | test runner framework + results view, coverage model | JUnit/TestNG/Ant/Maven/Gradle integration, JaCoCo |
| **Options** | options window + categories + search + settings browser | feature-specific panels (editor, debugger, VCS, profiler…) | Java platform/JDK, Maven/Gradle/Ant panels |
| **Theming** | theme manager, tokens, palettes, branding | dialog/wizard/view styling, editor profiles | — |
| **Build/run** | execution, output, terminal, progress | run configurations framework, actions | Java project types, Maven/Gradle/Ant, JavaFX launch |

## 6. Milestones (the roadmap)

Each milestone is shippable, has a demo and an exit criterion. Existing docs/steps fold into these
milestones (§7 maps them).

**Order: a walking skeleton first.** Before filling the layers, `feature/walking-skeleton` makes the app
usable end-to-end for one project type (open → edit → build → run → output/status → commit). It is the
integration test every later step must keep green, and the baseline for the startup/laziness budget
(doc 30 §7). The layers are then filled with the skeleton as the vertical thread — not as a waterfall.

**M0 — Platform shell & services.** The nbfx application runs with a complete FX shell: docking/views,
menus/toolbars/actions, status bar, wizard, templates, dialogs/notifications, progress, options/settings,
theming/branding, node/view model, property sheet, output, execution, terminal, quick search, credentials.
*Exit:* no visible Swing; every shell surface is FX; guardrails green; the service set is documented;
**startup/“usable”/first-use numbers recorded** (doc 30 §7); **API discipline landed before the freeze**:
public-package inventory + `nbfx.api` split, capability naming, services-not-statics/typed contracts,
structural guardrails, and the front-end component factory prototype with its upstream proposal
(docs 30/31); **the T0/T1/T2 tiers and the plugin contract are defined** (doc 32); **the M0 Swing
modules are retired or exempted** (doc 33); **the budget baseline and the upstream sync are running**
(doc 34).
Existing landings: progress UI, status elements, wizard framework, docking/views, actions/menus,
launcher. Remaining: options (14), theming (10), dialogs (09), nodes/views (12), property sheet (13),
output/execution/terminal (15), quick search, credentials, favorites, templates.

**M1 — IDE core.** Projects work end-to-end generically: the project model (`ProjectModel`) + project UI, customizers,
nodes/views, editor framework + actions, structure/navigator, find in projects, find usages, task
list/bookmarks, plugins UI, libraries manager, diff/patch, local history, VCS core + Git, test runner +
coverage, options categories.
*Exit:* open a project, edit, search, find usages, diff, commit, run tests, manage libraries — all FX;
**the M1 Swing modules are retired/exempted** (doc 33); the plugin API (T2) is published with its
signature tests and trust rules (doc 32); the budget is measured against the baseline (doc 34).

**M2 — Java tooling.** The full Java development loop: J2SE Ant/Maven/Gradle/apisupport project types and
customizers, Java Platforms + run configurations, Java editor intelligence (navigation, refactoring, hints,
codegen, Javadoc), debugger (JPDA: launch/attach/breakpoints/stepping/variables/watches/evaluate), profiler
(JFR + async-profiler + heap walker).
*Exit:* a Java developer can do their daily work in nbfx; **the M2 Swing modules are
retired/exempted** (doc 33); the budget is re-measured (doc 34).

**M3 — JavaFX usage.** The JavaFX project type and its tooling: JavaFX Ant project, JavaFX-enabled platform
handling, FXML completion, embedded SceneBuilder design/source editor.
*Exit:* original JavaFX-cluster capability (templates, customizers, run/build, FXML, SceneBuilder).

**M4 — Breadth & polish.** Module system + graph, i18n, JavaBeans, JShell, project groups, remote
debug/profiler, export/import, accessibility, multi-window, and the "beyond Swing" improvements of doc 28.

## 7. Mapping the existing docs/steps onto the milestones

| Milestone | Docs | Main clusters |
|---|---|---|
| M0 | 06 (progress), 09 (dialogs), 10 (theme), 12 (nodes/views), 13 (property sheet), 14 (options), 15 (output/execution/terminal), 18 §1 (quick search service); **30/31 (API discipline)**; `feature/walking-skeleton` | nbfx-platform |
| M1 | 02 (project infra), 07 (modularity/templates/new file+project), 11, 12 §migration, 13 §migration, 18–25 (offline parts: structure, find, task list, libraries, plugins, diff/VCS, tests) | nbfx-ide |
| M2 | 02 (Java Platforms, customizers, actions, run, Ant), 07 (Java steps), 16 (debugging), 17 (profiling), 18–22 (Java editor intelligence), 23 (Java tests/coverage) | nbfx-java (+ nbfx-ide) |
| M3 | 03 (JavaFX project), 04 (FXML/SceneBuilder) | nbfx-javafx |
| M4 | 25 (misc), 28 (§advancements), plus domain-specific work | all |

## 8. Sequencing and parallelism

```
M0 platform blocks ──┬─▶ M1 ide blocks ──┬─▶ M2 java blocks ──▶ M3 javafx ──▶ M4 breadth
                     │                    │
  parallel within M0: │  parallel within M1:│  parallel within M2:
  options / theme /   │  project / editor / │  java platforms / project types /
  dialogs / nodes /   │  structure / find / │  editor intelligence / debug / profiler
  property sheet /    │  VCS / tests /      │
  output+terminal     │  diff / libraries   │
```

- **Vertical slices are allowed**: a milestone need not finish before a vertical starts (e.g. the debugger
  vertical can begin once platform execution + output exist, and its IDE views arrive with M1).
- **Same-layer steps are parallel**; cross-layer prerequisites are explicit in every step entry (07).
- **API discipline runs in parallel with M0/M1 and gates the freeze**: public-package split, capability
  naming, typed contracts, structural guardrails and the component-factory prototype (docs 30/31) land
  before M1's API freeze; the walking skeleton is the integration thread throughout.
- **`nbfx-javafx` never blocks M0–M2**; it consumes M2's Java platform/project/build facilities.

## 9. Definition of done (per layer)

- **platform**: JavaFX-only public API; services registered through `Lookup` + generated layers; no Swing
  imports; usable headlessly (model) with FX views on top; themed and brandable; **every public package
  governed** (`arch.xml` `<api>` + `apichanges.xml` + `@since` + signature test); **no mutable statics**,
  **no toolkit names in SPI types**, **no stringly-typed API contracts** (doc 30).
- **ide**: language-agnostic SPIs with FX views; feature-specific bits delegate to usage layers through
  `Lookup` (e.g. `BuildActionProvider`, `DebugBackend`, `ProfileBackend`); model modules `check-models`
  clean.
- **java/javafx**: NetBeans headless engines reused, Swing UI ported; data compatibility preserved
  (project properties, breakpoints, snapshots, keymap); no language assumptions leaked upward.
- **libs.\***: licences recorded (`licenseinfo`/`nbbuild/licenses`), binaries bundled per OS/arch where
  needed (OS/arch module enablement, doc 17 §3.6).
- Every step: tests (headless + UI), guardrails (`check-purity`), doc updated.

## 10. What this restructure changes

- The docs 00–25 stay as **topic detail**; this doc + doc 27 are the **structural view** (what lives where,
  in which order) and doc 28 the **UX advancement agenda**.
- `07-small-steps.md` gains a layer index so every step declares its cluster; `08` groups branches by layer.
- The old "JavaFX support on top of nbfx" framing is retired: JavaFX is the toolkit and a usage layer
  (M3); the roadmap is the whole IDE, platform → ide → java → javafx.
