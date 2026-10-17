# nbfx — roadmap & plan (communication & planning base)

This directory is the working plan for **nbfx as a whole** — completing the existing project
infrastructure (wizards/templates, options, output/terminal, nodes/views, dialogs, theming) and then
adding **JavaFX project support** (JavaFX Ant project, Java Platforms, FXML completion, embedded
SceneBuilder). It captures the design, the evidence from the original NetBeans code, the execution
backlog (small steps), the branch/commit layout, and the open work.

It is written to be read by someone who is not in the day-to-day conversation: every non-obvious claim
is backed by a file/class reference in the upstream submodule or in this repository.

## Where to start

- **New to the plan** → [`00-overview.md`](00-overview.md), then [`07-small-steps.md`](07-small-steps.md).
- **Want the current state** → [`11-project-state.md`](11-project-state.md) and the status table below.
- **Implementing** → the step in [`07-small-steps.md`](07-small-steps.md) plus its detail doc.
- **Architecture rules** → [`../architecture-rules.md`](../architecture-rules.md) (model/UI split).
- **Build & harness** → [`../repo-and-build.md`](../repo-and-build.md).

## Documents

| Doc | Contents |
|---|---|
| [00-overview.md](00-overview.md) | Goal, scope, locked decisions, the two parts |
| [01-research.md](01-research.md) | Evidence from the original NetBeans code (project kind, wizards, templates, progress, SceneBuilder, problems UI, notifications, branding) |
| [02-part1-infrastructure.md](02-part1-infrastructure.md) | **Part 1** — complete the existing project infrastructure (project types, FxProject, Java Platforms, wizard framework + templates, customizers, actions, run configs, Ant, problems UI, dialogs/notifications, theming) |
| [03-part2-javafx.md](03-part2-javafx.md) | **Part 2** — JavaFX project support |
| [04-fxml-and-scenebuilder.md](04-fxml-and-scenebuilder.md) | FXML-aware completion + embedded SceneBuilder design/source editor |
| [05-milestones-verification-risks.md](05-milestones-verification-risks.md) | Verification, risks, deferred/dropped, resolved decisions |
| [06-progress-ui.md](06-progress-ui.md) | Progress UI parity (multi-task status line, all-tasks popup) + implementation notes |
| [07-small-steps.md](07-small-steps.md) | **Execution backlog** — the plan sliced into small, ordered, independently shippable steps |
| [08-branching-and-workflow.md](08-branching-and-workflow.md) | Branch naming, PR flow, commit style, current local stack |
| [09-fx-dialogs-and-notifications.md](09-fx-dialogs-and-notifications.md) | FX dialog/notification infrastructure, exception reporting, chrome ideas |
| [10-theming-and-branding.md](10-theming-and-branding.md) | Light/dark themes, editor colour profiles, CSS via the NetBeans branding mechanism |
| [11-project-state.md](11-project-state.md) | Current branches/commits/PRs, verification, backups, clean-ups |
| [12-nodes-and-views.md](12-nodes-and-views.md) | Nodes & views API (`NodeModel`/`ChildrenModel`/`ChildFactory`/`ExplorerContext`), mapping from `org.openide.nodes`, migration |
| [13-property-sheet-and-editors.md](13-property-sheet-and-editors.md) | Property model contract, multi-selection aggregation, editor SPI/built-ins, property sheet, outline/tree-table columns and inline editing |
| [14-options.md](14-options.md) | Porting the NetBeans Options system: SPI/registrations, containers/search/change-validity, `nbfx.settings`, advanced browser, parity matrix and per-category steps, export/import |
| [15-output-and-terminal.md](15-output-and-terminal.md) | Output window (`api.io` + ANSI + FX view), execution service, VT/xterm terminal model, JLine PTY backend, terminal view, run/build integration (Terminal default, Maven as a process) |
| [16-debugging.md](16-debugging.md) | JVM debugging: headless core reuse vs JavaFX UI port, launch/attach, view models, breakpoints, stepping/editor, evaluate, attach/monitors, build integration, options |

Repo-wide docs referenced here: [`../architecture-rules.md`](../architecture-rules.md) (the model/UI
split rule), [`../repo-and-build.md`](../repo-and-build.md) (harness, clusters, patches),
[`../MVP.md`](../MVP.md) (the pre-existing MVP notes).

## Status at a glance

| Step | Branch | State |
|---|---|---|
| Progress UI parity | `feature/progress-ui` | **PR #2 open** |
| Progress: drop the launcher's hard-wired progress | `feature/progress-launcher-removal` | local |
| Status elements: dynamic registration | `fix/status-elements-dynamic-registration` | local |
| Wizard framework (incl. themed shell/makeover) | `feature/wizard-framework` | local, 2 commits |
| Project modularity refactoring | `refactor/project-modularity` | local, 1 commit |
| New File templates (model + chooser + parity) | `feature/newfile-templates` | local, 2 commits + styles fix |
| New File: per-template wizard panels | `feature/newfile-template-panels` | next |
| FX dialogs & notifications (UI) | `feature/fx-dialogs` | planned, parallel |
| Theming & branding (light/dark, CSS) | `feature/theming` | planned, parallel |
| Options system (infra + example) | `feature/options-foundation` | planned, parallel |
| Options categories (Appearance/Editor/Java/Misc) + browser/export/Quick Search | `feature/options-*` | planned |
| Output window + execution service + terminal (VT model, JLine PTY, view) | `feature/output-window`, `feature/execution-service`, `feature/terminal-*` | planned, parallel start |
| Run/build output integration (Terminal default, Maven as a process) | `feature/run-output-integration` | planned |
| Debugger (foundation, views, breakpoints, stepping/editor, evaluate, attach, build integration, options) | `feature/debug-*` | planned |
| Nodes & views API (node model + FX views) | `feature/nodes-api` | planned, parallel |
| Property sheet & editors (editors, sheet, outline columns) | `feature/property-sheet-editors` | planned |
| Views on nodes (migration) | `refactor/views-on-nodes` | planned |
| New Project templates | `feature/newproject-templates` | planned |
| FxProject model | `feature/fxproject-model` | planned (later) |
| Project problems UI (FX) | `feature/project-problems-ui` | planned |
| Java Platforms, customizers, actions, run, Ant | see `07-small-steps.md` | planned |
| Part 2 (JavaFX project, FXML, SceneBuilder) | see `07-small-steps.md` | planned |

See [`11-project-state.md`](11-project-state.md) for exact commit ids and verification results; that
file is the snapshot in time, this table is the index.
