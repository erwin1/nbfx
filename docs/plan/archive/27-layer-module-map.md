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
# 27 — Layer & module map (what lives where)

The authoritative allocation of every nbfx building block to a cluster, its NetBeans counterpart, its
status, the step/branch that delivers it and the detail doc. Read with [26-roadmap.md](26-roadmap.md)
(layers/milestones), [32](32-api-tiers-and-plugin-contract.md) (API tiers + plugin contract),
[33](33-swing-retirement.md) (retirement) and [`architecture-rules.md`](../architecture-rules.md) (the
dependency rules). **The tier table and the split steps are derived from the source audit in
[`30`](30-api-governance.md) §1.1–1.5** — the tree on `main` differs from the local branches (§1.4).

Legend — **status**: `landed` (in the repo), `in-flight` (branch/PR), `planned` (step + branch agreed),
`missing` (identified, not yet a step). **Layer**: `plat` = `nbfx-platform`, `ide` = `nbfx-ide`,
`java` = `nbfx-java`, `fx` = `nbfx-javafx`, `lib` = external library module.

## 1. `nbfx-platform` — core building blocks

| Module | Purpose | NetBeans counterpart | Status | Step / branch | Doc |
|---|---|---|---|---|---|
| `nbfx.actions` | command/action registry, keymap, menus/toolbars wiring | `openide.actions`, `openide.awt` actions | landed | — | 00 |
| `nbfx.annotations` | **core** layer-generating annotations + processors: actions, views, menus, toolbars, status (+ `FxLayer` for the shell folders) | `openide.filesystems` layer + `nbs.annotations` | landed (to split) | `refactor/annotations-split` | 29 |
| `nbfx.api.options` (T2 annotation-only) | `@OptionsRegistration`/`Container`/`Sub` + processor, `OPTIONS` path | `options.api` registration | planned | `refactor/annotations-split` (slice 1) | 29/32 |
| `nbfx.api.wizard` (T2 annotation-only) | `@WizardRegistration` + processor, `WIZARDS` path | `openide.loaders` wizard registration | planned | `refactor/annotations-split` (slice 2) | 29/32 |
| `nbfx.api.property` (T2 annotation-only) | `@PropertyEditorRegistration` + processor, `PROPERTY_EDITORS` path (plus `PropertyModel`/`PropertyEditor`) | `openide.explorer` property editors | planned | `refactor/annotations-split` (slice 3) | 29/32 |
| `nbfx.api.project` (T2, ide) | `@ProjectTypeRegistration` + processor, `PROJECTS` path, `ProjectModel`/project-type SPI | `projectuiapi` project-type registration | planned | `refactor/annotations-split` (slice 4) | 29/32 |
| `nbfx.api` | view/action/status/editor/progress **APIs** + (misplaced) project API | `openide.util.lookup`, platform APIs | landed; **to split per capability** (`refactor/api-split`, doc 30 §1) | — | 00/30 |
| `nbfx.context` | global selection/context (`FxActionContext`) | `openide.awt` `Actions`/global `Lookup` | landed | — | 00 |
| `nbfx.docking` | docks/areas/tabs/layout model | `core.windows` (dropped from nbfx) | landed | — | 00 |
| `nbfx.menus` / `nbfx.windows` | menu bar + main window shell/persistence | `core.windows`, `core.ui` | landed | — | 00 |
| `nbfx.statusbar` | status elements + registration | `core.ui` status line | landed | `fix/status-elements-dynamic-registration` | 00 |
| `nbfx.output` | output consoles (tabs) | `core.output2` (+ `api.io`/`spi.io`) | landed (basic) | `feature/output-window` (rewrite) | 15 |
| `nbfx.wizard` | wizard framework + themed shell | `openide.loaders.TemplateWizard`, `WizardDescriptor` | landed | `feature/wizard-framework` | 07 |
| `nbfx.options` | options window (categories/containers/subs, search) | `options.api` + `core.ui`/`platform` panels | in-flight (stub) | 14 (§steps A–H) | 14 |
| `nbfx.theme` | theme manager, light/dark, tokens, branding | `o.n.swing.laf.*`, `core.windows` LAF options | planned | `feature/theming` | 10 |
| `nbfx.dialogs` | dialogs service + notifications + toasts + exception reporting | `openide.dialogs`, `uihandler`, `notifications` | planned | `feature/fx-dialogs` | 09 |
| `nbfx.nodes` | **node/view model** (`NodeModel`/`ChildrenModel`/`PropertyModel`/`ExplorerContext`) | `openide.nodes` (Node/Children/Sheet) | planned | `feature/nodes-api` | 12 |
| `nbfx.nodes.ui` | FX views over the node model (tree/tree-table/table/list) + property editors | `openide.explorer` views, `spi.viewmodel` renderers | planned | `feature/nodes-api` | 12 |
| `nbfx.propertysheet` | property sheet + editor SPI (currently in `nbfx-ide`; move here) | `openide.explorer.propertysheet` | landed (to move) | `feature/property-sheet-editors` | 13 |
| `nbfx.execution` | process model (PIPE/PTY), pumps, cancel/exit | `extexecution.base`/`.process` (legacy `extexecution` skipped) | planned | `feature/execution-service` | 15 |
| `nbfx.terminal.model` | VT/xterm emulator model (headless) | `lib.terminalemulator` core (ported) | planned | `feature/terminal-model` | 15 |
| `nbfx.terminal.ui` | FX terminal renderer + tabs | `terminal.nb` (Swing) | planned | `feature/terminal-view` | 15 |
| `nbfx.libs.asyncprofiler.*` | PTY/agent native libs per OS/arch (module enablement) | — (async-profiler is external) | planned | `feature/terminal-pty`, `feature/profiler-async-profiler` | 15/17 |
| `nbfx.quicksearch` | global Quick Search service + providers + FX popup | `spi.quicksearch` + `core.ui` popup | **missing** (only the options step exists) | new step `feature/quicksearch` | 18/14 |
| `nbfx.keyring` | credentials/keyring (VCS, update centers) | `keyring`, `keyring.impl`, `keyring.fallback` | **missing** | new step `feature/credentials` | 24 |
| `nbfx.favorites` | Favorites view | `platform/favorites` | **missing** (low priority) | new step `feature/favorites` | 27 |
| `nbfx.templates` | headless template model + registry/creator | `openide.loaders` templates, `api.templates` | in-flight (branch) | `feature/newfile-templates` | 02/07 |
| `libs.jline` | JLine 3 (PTY, termios, signals) | (NetBeans ships JLine inside `libs.graalsdk`) | planned | `feature/terminal-pty` | 15 |

### 1.1 Missing platform blocks (identified while layering)

- **Quick Search** is a platform service (any nbfx app needs it), not an options or Java feature: a
  `SearchProvider` registry + FX popup + keyboard entry point. It is only referenced indirectly today
  (options Quick Search, doc 18 §go-to). **Action:** new platform step `feature/quicksearch`, consumed by
  doc 18's Go to/Quick Search and the action palette.
- **Credentials/keyring** is platform (VCS, update centers, remote debug/profiler): plan a `nbfx.keyring`
  service over NetBeans' `keyring` modules with an FX master-password/unlock prompt.
- **Dialogs/notifications** and **theming** are platform, and both are prerequisites for the Java/IDE
  waves (docs 09/10).
- **Execution + terminal** are platform services (build, run, debug, profiler, VCS, JShell): docs 15.
- **Preferences/settings browser** (the debug aid in doc 14) is platform, not options-specific.
- **Favorites** and **templates manager** are small platform gaps.

### 1.2 Annotation ownership (the `nbfx.annotations` split)

`nbfx.annotations` must not stay a single cross-layer module. Registration annotations and their
processors move to the module that owns the SPI they complement, while the core keeps only shell
registrations (actions/views/menus/toolbars/status + the shell `FxLayer` constants): options →
`nbfx.options`, wizard → `nbfx.wizard`, property editors → the property-editor module, project types →
the project API module (ide). Annotation modules are **compile-only** (build prerequisite, no
`run-dependency`) and the generated layer output must stay identical across the move. Full plan:
[`29-annotation-modules.md`](29-annotation-modules.md).

## 2. `nbfx-ide` — IDE building blocks

| Module | Purpose | NetBeans counterpart | Status | Step / branch | Doc |
|---|---|---|---|---|---|
| `nbfx.project.api` *(new; split from `nbfx.api`)* | project model API (`FxProject`, registry, kinds) | `projectapi` | to extract | `feature/fxproject-model` | 02 |
| `nbfx.project` | **On `main`: New File/Project wizards** (to move to `nbfx.project.ui` on `refactor/project-modularity`); after that: reserved for the project model. The project model today is `nbfx.api.project` (`OpenProject`/`ProjectRegistry`/`ProjectOpener`/`ProjectIconProvider`, 14 consumers) | `projectapi`/project-type modules | in transition | `refactor/project-modularity` + `feature/fxproject-model` | 02/26/30 §1.4 |
| `nbfx.project.ui` / `.ui.api` / `.customizer` / `.actions` / `.context` | New File/Project wizards, project UI, customizer framework, project actions, context | `projectui`, `projectuiapi`, project customizer SPI | landed (moves done) | `refactor/project-modularity` + 02 steps | 02/07 |
| `nbfx.editor` | FX editor framework: code area, completion, breadcrumbs, syntax decoration, annotations, find/replace | `editor`, `editor.lib*`, `editor.completion`, `editor.breadcrumbs` | landed (framework) | + `feature/editor-actions-parity` | 21 |
| `nbfx.editor.actions` | editor commands (cut/copy/paste/undo/find/save) | `editor.actions` | landed | — | 21 |
| `nbfx.file.actions` / `nbfx.launcher` | file/desktop actions; application launcher | `openide.actions`, `core.startup` | landed | — | 00 |
| `nbfx.structure` | structure/navigator framework | `editor.structure` | landed | migrate to nodes (12) | 12 |
| `nbfx.findinprojects` | Find (and Replace) in Projects | `find`, `projectui` search | landed | replace-in-projects step | 18 |
| `nbfx.findusages` | Find Usages (own usages query) | `refactoring.api` usages + `java.editor` | landed (parity work planned) | `feature/find-usages-parity` | 18 |
| `nbfx.propertysheet` | **landed in `nbfx-ide`; to move to `nbfx-platform`** (step missing until `refactor/module-moves`) | `openide.explorer.propertysheet` | landed (to move) | `refactor/module-moves` + 13 | 13/26 |
| `nbfx.run` | run/build orchestration + `ProcessRunner` | `extexecution` usage by project types | landed (basic) | `feature/execution-service` (adopt) | 15 |
| `nbfx.debug` | debugger UI + session (split: `.core` model, `.java` backend) | `spi.debugger.ui`, `debugger.jpda.ui`, `java.navigation` | landed (basic) | 16 steps | 16 |
| `nbfx.vcs` | VCS core + Git UI (currently CLI Git status only) | `versioning.core/util/ui`, `ide/git` (jgit) | landed (basic) | 24 steps | 24 |
| `nbfx.plugins` | plugin manager (install/update/remove) | `autoupdate.ui` | landed (basic) | `feature/plugins-ui` | 25 |
| `nbfx.ui` | application UI glue (shell actions, session, app state) | `core.ui`, `core.windows` shell | landed | — | 00 |
| `nbfx.editor.intelligence` *(planned)* | semantic access + providers (goto/navigation/usages/hierarchy) | `java.source` API + `jumpto` + `java.navigation` | planned | `feature/editor-intelligence-core` | 18 |
| `nbfx.refactoring.core` / `.ui` *(planned)* | refactoring engine + FX UI | `refactoring.api` + `refactoring.java` | planned | 19 steps | 19 |
| `nbfx.java.hints.core` *(planned, no Java semantics inside — SPI only)* | hints engine plumbing + settings | `spi.java.hints` (`HintsInvoker`) | planned | 20 steps | 20 |
| `nbfx.test.core` / `nbfx.test.ui` *(planned)* | test runner + results view + coverage model | `java.testrunner*`, `junit.ui`, `gsf.codecoverage` | planned | 23 steps | 23 |
| `nbfx.diff` / `nbfx.localhistory` *(planned)* | diff/merge component + patch; local history UI | `ide/diff` engine, `localhistory` | planned | 24 steps | 24 |
| `nbfx.tasklist` / `nbfx.bookmarks` *(planned)* | task list + bookmarks views | `tasklist.*`, `editor.bookmarks` | planned | 25 steps | 25 |
| `nbfx.libraries` *(planned)* | libraries manager UI | `project.libraries(.ui)` | planned | `feature/libraries-manager` | 25 |
| `nbfx.project.groups` *(planned)* | project groups | `projectui` groups | planned | `feature/project-groups` | 25 |

## 3. `nbfx-java` — Java/JVM usage layer

| Module | Purpose | NetBeans counterpart | Status | Step / branch | Doc |
|---|---|---|---|---|---|
| `nbfx.project.ui.java` | J2SE project kind + navigator + customizer panels | `java.j2seproject` | landed (basic) | 02 steps | 02 |
| `nbfx.project.ui.maven` | Maven project kind + customizer + build | `maven`, `maven.embedder` | landed (basic) | 02/15 (build as process) | 02/15 |
| `nbfx.project.ui.gradle` | Gradle project kind + build | `extide/gradle` | landed (basic) | 02 | 02 |
| `nbfx.project.ui.ant` | Ant-based J2SE project support | `java.j2seproject`, `ant` modules | landed (basic) | 02 | 02 |
| `nbfx.build.ant` | in-process Ant runner | `o.apache.tools.ant.module` | landed (basic) | 02/15 | 02/15 |
| `nbfx.structure.java` | Java structure/navigator provider | `editor.structure` + `java.source` | landed | 12 (migrate to nodes) | 12 |
| `nbfx.java.platform` *(planned)* | Java Platforms (JDK storage, sources/javadoc, JavaFX capability), FX options | `java.platform`, `java.j2seplatform`, `java.platform.ui` | planned | `feature/java-platforms` | 02 |
| `nbfx.java.editor` *(planned)* | Java semantics wiring: parser services, completion, hints, refactoring, codegen, Javadoc | `java.source`/`java.editor`/`java.hints`/`refactoring.java` | planned | 18–22 steps | 18–22 |
| `nbfx.debug.java` *(planned)* | JPDA backend: session/launch/attach, breakpoints, stepping, evaluate | `debugger.jpda`, `debugger.jpda.projects` | planned | 16 steps | 16 |
| `nbfx.profiler.java` *(planned)* | JFR + async-profiler backends, run integration | `lib.profiler` (heap only), JFR is JDK | planned | 17 steps | 17 |
| `nbfx.test.java` *(planned)* | JUnit/TestNG + Ant/Maven/Gradle test integration, JaCoCo | `junit*`, `testng*`, `maven.junit`, `gradle.test`, `*.coverage` | planned | 23 steps | 23 |
| `nbfx.java.module` *(planned)* | module-info support + module graph view | `java.j2semodule`, `java.module.graph` | planned | `feature/module-support` | 25 |
| `nbfx.i18n`, `nbfx.beans` *(planned)* | i18n wizard; JavaBeans patterns | `java/i18n`, `java/beans` | planned | 25 steps | 25 |
| `nbfx.jshell` *(planned)* | Java Shell console | `jshell.support` + JDK `jdk.jshell` | planned | `feature/jshell` | 25 |

## 4. `nbfx-javafx` — JavaFX usage layer

| Module | Purpose | NetBeans counterpart | Status | Step / branch | Doc |
|---|---|---|---|---|---|
| `nbfx.javafx.project` | JavaFX **usage module** (customizers/run/build; the NetBeans project-type id stays `j2seproject`) | `javafx2.project` | planned | `feature/javafx-project` | 03 |
| `nbfx.javafx.platform` | JavaFX runtime detection/JavaFX-enabled JDK, module path | `javafx2.platform` | planned | `feature/javafx-cluster` | 03 |
| `nbfx.javafx.fxml` | FXML editing/completion | `javafx2.editor` (useful core) | planned | `feature/fxml-completion` | 04 |
| `nbfx.javafx.scenebuilder` | embedded SceneBuilder (kit 26.0.0) | `javafx2.scenebuilder` | planned | `feature/scenebuilder-editor` | 04 |

## 5. External libraries (`libs.*`)

| Library | Use | Licence | Where |
|---|---|---|---|
| `libs.javafx` (landed) | JavaFX runtime + `jfx.incubator` richtext/input | GPLv2+CE | bundled in `nbfx-platform` |
| `libs.jline` (planned) | PTY/termios/signals for the terminal | BSD-3 (accepted: `BSD-jline3`) | `nbfx-platform`, per-OS native extraction |
| `libs.asyncprofiler.*` (planned) | high-fidelity sampling backend | Apache-2.0 | per-OS/arch modules, module enablement (17 §3.6) |
| `libs.jgit` (reuse) | Git engine | EDL | reuse NetBeans `ide/o.eclipse.jgit*` |
| `libs.jacoco` (reuse) | code coverage | EPL-2.0 (already shipped with notice) | reuse `java/gradle.java.coverage/external/org.jacoco.core` |
| `libs.jna` (reuse) | native bindings (fallback PTY) | Apache-2.0/LGPL | reuse `platform/libs.jna` |

## 6. Step → layer master matrix

Every step in [07-small-steps.md](07-small-steps.md) with its layer and where its model/UI halves live.
(This is the "transparency" view: the same capability may have a `plat` service, an `ide` view and a
`java` backend.)

| Capability | plat (service/components) | ide (views/integration) | java/fx (backend/usage) |
|---|---|---|---|
| Progress UI | `nbfx.progress` (landed) | status element (landed) | — |
| Status elements | `nbfx.statusbar` (landed) | registrations (landed) | — |
| Wizard framework | `nbfx.wizard` (landed) | — | per-type wizards (java) |
| Dialogs/notifications | `nbfx.dialogs` | — | error reporting per feature |
| Theming/branding | `nbfx.theme` | view/dialog styling | editor profiles |
| Options | `nbfx.options` + settings | feature panels | Java/Maven/Gradle/Ant panels |
| Nodes/views | `nbfx.nodes`/`.ui` | structure, navigator, usages | Java structure provider |
| Property sheet | `nbfx.propertysheet` (move) | customizers | Java project properties |
| Output/Execution/Terminal | `nbfx.output`, `nbfx.execution`, `nbfx.terminal.*` | run view wiring | build/run/debug/profiler consumers |
| Quick search | `nbfx.quicksearch` | providers (files, actions) | Java type/symbol providers |
| Templates | `nbfx.templates` | New File/Project UI | Java templates |
| Project model | — | `nbfx.project`(+api/ui) | J2SE/Maven/Gradle/Ant kinds |
| Editor framework | — | `nbfx.editor(.actions)` | Java semantics, FXML |
| Editor intelligence | — | `nbfx.editor.intelligence`, refactoring, hints, codegen, javadoc | Java providers (18–22) |
| Find usages | — | `nbfx.findusages` | Java usages query |
| Debugger | views via nodes/property sheet, output/terminal | `nbfx.debug` (UI) | `nbfx.debug.java` (JPDA) |
| Profiler | charts/nodes/options | `nbfx.profiler.ui` | `nbfx.profiler.java` (JFR/async-profiler) |
| VCS | keyring, diff primitives, notifications | `nbfx.vcs`, diff/history/local history | per-project hooks |
| Tests | progress/output/execution | `nbfx.test.core/.ui` | `nbfx.test.java` (JUnit/TestNG/JaCoCo) |
| Libraries | — | `nbfx.libraries` | project-type storage (desc 02) |
| Plugins | autoupdate services (reused) | `nbfx.plugins` UI | — |
| Task list/bookmarks | — | `nbfx.tasklist`, `nbfx.bookmarks` | Java task providers |
| JavaFX usage | — | (consumes platform/ide) | `nbfx.javafx.*` |

## 7. How to use this map

1. Pick a **milestone** (26 §6) and a **vertical** (26 §5).
2. Read the row's docs (last column) for the detail.
3. Find the step in 07 (layer index) and the branch in 08.
4. Keep the layer rules: model modules JavaFX-free, UI modules free of the prohibited Swing modules, no
   upward dependencies; when a block is needed by two usage layers, move it down (`java` → `ide` →
   `platform`).
