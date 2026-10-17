# nbfx — roadmap & plan (communication & planning base)

nbfx is a **pure-JavaFX IDE**: a JavaFX application shell plus JavaFX replacements for every user-facing
service of the NetBeans platform, IDE and Java clusters. It reuses the NetBeans **headless engines** as-is
(project model, parsing/indexing, `java.source`, build/execution, debugger, profiler, VCS, templates,
autoupdate) and replaces the **presentation layer**, with a strict model/UI split and modern APIs.

The plan is **layered** so work can be structured by building block:

- **`nbfx-platform`** — core building blocks (shell, actions, status, wizard, dialogs, progress, options,
  theming, node/view model, property sheet, output, execution, terminal, quick search, credentials).
- **`nbfx-ide`** — IDE building blocks (projects, editor framework + intelligence, structure, find,
  diff/VCS/history, tests, debugger/profiler UI, plugins, task list, libraries).
- **`nbfx-java`** — Java/JVM usage (project types, Java Platforms, build/run, Java editor semantics, JPDA
  and JFR integration, JUnit/TestNG).
- **`nbfx-javafx`** — JavaFX usage (JavaFX project, FXML completion, embedded SceneBuilder).
- **`libs.*`** — external libraries (JavaFX, JLine, async-profiler, jgit, JaCoCo, JNA).

Read [`26-roadmap.md`](26-roadmap.md) for the layer model, allocation principles and milestones
(M0 platform → M1 IDE → M2 Java → M3 JavaFX → M4 breadth), [`27-layer-module-map.md`](27-layer-module-map.md)
for the module-by-module allocation, and [`28-beyond-swing-ui.md`](28-beyond-swing-ui.md) for where the
Swing design holds the UI back and how the JavaFX implementation advances beyond it.

## Where to start

- **New to the plan** → [`26-roadmap.md`](26-roadmap.md) (layers/milestones), then
  [`27-layer-module-map.md`](27-layer-module-map.md) (what lives where), then
  [`07-small-steps.md`](07-small-steps.md) (the backlog with a layer index).
- **Current state** → [`11-project-state.md`](11-project-state.md) and the status table below.
- **Implementing** → the step in [`07-small-steps.md`](07-small-steps.md), its detail doc and its layer in
  [`27-layer-module-map.md`](27-layer-module-map.md).
- **Architecture rules** → [`../architecture-rules.md`](../architecture-rules.md) (model/UI split,
  dependency direction, per-layer rules).
- **Build & harness** → [`../repo-and-build.md`](../repo-and-build.md).

## Documents

**Structure (read first).** [`26-roadmap.md`](26-roadmap.md) · [`27-layer-module-map.md`](27-layer-module-map.md)
· [`28-beyond-swing-ui.md`](28-beyond-swing-ui.md) · [`07-small-steps.md`](07-small-steps.md) ·
[`08-branching-and-workflow.md`](08-branching-and-workflow.md).

| Doc | Layer / topic | Contents |
|---|---|---|
| [00-overview.md](00-overview.md) | all | Goal, scope, locked decisions |
| [01-research.md](01-research.md) | all | Evidence from the original NetBeans code (project kind, wizards, templates, progress, SceneBuilder, problems UI, notifications, branding) |
| [02-part1-infrastructure.md](02-part1-infrastructure.md) | ide/java | Project infrastructure: project types, `FxProject`, Java Platforms, wizard framework + templates, customizers, actions, run configs, Ant, problems UI |
| [03-part2-javafx.md](03-part2-javafx.md) | javafx | JavaFX project support |
| [04-fxml-and-scenebuilder.md](04-fxml-and-scenebuilder.md) | javafx | FXML-aware completion + embedded SceneBuilder design/source editor |
| [05-milestones-verification-risks.md](05-milestones-verification-risks.md) | all | Verification, risks, deferred/dropped, resolved decisions |
| [06-progress-ui.md](06-progress-ui.md) | **platform** | Progress UI parity (multi-task status line, all-tasks popup) + implementation notes |
| [09-fx-dialogs-and-notifications.md](09-fx-dialogs-and-notifications.md) | **platform** | FX dialog/notification infrastructure, exception reporting, chrome ideas |
| [10-theming-and-branding.md](10-theming-and-branding.md) | **platform** | Light/dark themes, editor colour profiles, CSS via the NetBeans branding mechanism |
| [12-nodes-and-views.md](12-nodes-and-views.md) | **platform** | Node/view model (`NodeModel`/`ChildrenModel`/`ChildFactory`/`ExplorerContext`) + FX views, migration from `org.openide.nodes` |
| [13-property-sheet-and-editors.md](13-property-sheet-and-editors.md) | **platform** | Property model, multi-selection aggregation, editor SPI/built-ins, property sheet, outline/tree-table columns |
| [14-options.md](14-options.md) | **platform** | Options system: SPI/registrations, containers/search/validity, `nbfx.settings`, settings browser, export/import |
| [15-output-and-terminal.md](15-output-and-terminal.md) | **platform** | Output window (`api.io` + ANSI + FX view), execution service, VT/xterm terminal model, JLine PTY, terminal view, run/build integration |
| [16-debugging.md](16-debugging.md) | **ide + java** | Debugger: headless core reuse vs FX UI port, launch/attach, view models, breakpoints, stepping/editor, evaluate, attach/monitors, build integration, options |
| [17-profiling.md](17-profiling.md) | **ide + java + lib** | Profiling: agent-free backends (JFR, bundled async-profiler per OS/arch, JMX sampler), analysis model, streaming + open `.jfr`/`.hprof`/`.nps`, heap walker, options |
| [18-navigation-and-search.md](18-navigation-and-search.md) | **ide + java** | Editor intelligence foundation: Go to File/Type/Symbol/Line, Quick Search, navigation/hyperlinks, Find Usages parity, call/type hierarchies |
| [19-refactoring.md](19-refactoring.md) | **ide + java** | Refactoring: reuse `refactoring.api`/`refactoring.java` logic, rename first, then the Java refactorings, preview/actions UI |
| [20-java-hints.md](20-java-hints.md) | **ide + java** | Java hints: `spi.java.hints` engine + `java.hints`, editor annotations, lightbulb/fixes, Fix/Organize Imports, Surround With, Hints options |
| [21-codegen-and-editor-actions.md](21-codegen-and-editor-actions.md) | **ide + java** | Code generation (`GeneratorUtilities`), generate dialog, editor action parity |
| [22-javadoc.md](22-javadoc.md) | **java** | Javadoc viewing (Show Javadoc, window/index) and completion |
| [23-testing-and-coverage.md](23-testing-and-coverage.md) | **ide + java + lib** | Test runner (JUnit/TestNG, Ant/Maven/Gradle), Test Results view, test run configs, coverage (bundled JaCoCo) |
| [24-vcs-diff-history.md](24-vcs-diff-history.md) | **ide + lib** | VCS core (versioning.core/masterfs), Git over jgit, diff/merge + patch, local history |
| [25-misc-java-features.md](25-misc-java-features.md) | **java + ide** | Libraries manager, module support/graph, task list & bookmarks, i18n, JavaBeans, project groups, plugins UI, JShell, out-of-scope list |
| [11-project-state.md](11-project-state.md) | all | Current branches/commits/PRs, verification, backups, clean-ups |

Repo-wide docs: [`../architecture-rules.md`](../architecture-rules.md) (model/UI split + per-layer rules),
[`../repo-and-build.md`](../repo-and-build.md) (harness, clusters, patches), [`../MVP.md`](../MVP.md)
(pre-existing notes).

## Status at a glance (by layer / milestone)

| Layer | Capability | Branch(es) | State |
|---|---|---|---|
| **platform** | Progress UI parity | `feature/progress-ui` | **PR #2 open** |
| **platform** | Progress: launcher removal | `feature/progress-launcher-removal` | local |
| **platform** | Status elements: dynamic registration | `fix/status-elements-dynamic-registration` | local |
| **platform** | Wizard framework (incl. themed shell) | `feature/wizard-framework` | local, 2 commits |
| **platform** | Dialogs & notifications | `feature/fx-dialogs` | planned |
| **platform** | Theming & branding | `feature/theming` | planned |
| **platform** | Options + settings (+ categories/browser/export/Quick Search) | `feature/options-*` | planned |
| **platform** | Nodes/views + property sheet/editors | `feature/nodes-api`, `feature/property-sheet-editors`, `refactor/views-on-nodes` | planned |
| **platform** | Output window + execution service + terminal | `feature/output-window`, `feature/execution-service`, `feature/terminal-*` | planned |
| **platform** | Quick search (command palette) | `feature/quicksearch` | planned |
| **platform** | Credentials/keyring | `feature/credentials` | planned |
| **ide** | Project modularity refactoring | `refactor/project-modularity` | local, 1 commit |
| **ide** | New File templates (model + chooser + parity) | `feature/newfile-templates` | local, 2 commits + fix |
| **ide** | New File: per-template wizard panels | `feature/newfile-template-panels` | next |
| **ide** | New Project templates / `FxProject` / problems UI | `feature/newproject-templates`, `feature/fxproject-model`, `feature/project-problems-ui` | planned |
| **ide** | Java navigation & search (goto, hyperlinks, usages, hierarchies) | `feature/editor-intelligence-core`, `feature/goto-search`, `feature/editor-navigation`, `feature/find-usages-parity`, `feature/hierarchies` | planned |
| **ide** | Refactoring / hints / codegen / editor actions / Javadoc | `feature/refactoring-*`, `feature/java-hints-*`, `feature/codegen-generate`, `feature/fix-imports`, `feature/editor-actions-parity`, `feature/javadoc-*` | planned |
| **ide** | Testing & coverage | `feature/test-runner`, `feature/test-results-ui`, `feature/test-run-configs`, `feature/code-coverage` | planned |
| **ide** | VCS, diff & local history | `feature/vcs-core`, `feature/vcs-git`, `feature/diff-and-patch`, `feature/local-history` | planned |
| **ide** | Debugger UI / profiler UI (with `nbfx-java` backends) | `feature/debug-*`, `feature/profiler-*` | planned |
| **ide** | Libraries, task list/bookmarks, plugins UI, project groups | `feature/libraries-manager`, `feature/tasklist-bookmarks`, `feature/plugins-ui`, `feature/project-groups` | planned |
| **java** | Java Platforms, customizers, build actions, run, Ant | see `07-small-steps.md` | planned |
| **java** | Java editor semantics / JPDA / JFR / JUnit | see 18–23 | planned |
| **java** | Module support, i18n, Beans, JShell | `feature/module-support`, `feature/i18n`, `feature/beans`, `feature/jshell` | planned |
| **javafx** | JavaFX project / FXML / SceneBuilder | `feature/javafx-project`, `feature/fxml-completion`, `feature/scenebuilder-editor` | planned (M3) |

See [`11-project-state.md`](11-project-state.md) for exact commit ids and verification results; that file
is the snapshot in time, this table is the index.
