# 08 — Branching & workflow

Branch names mirror the small steps in [`07-small-steps.md`](07-small-steps.md) with short, readable
slugs — one branch per step, one PR per branch.

## Remotes

| Remote | Repo | Role |
|---|---|---|
| `origin` | `gluonhq/nbfx` | upstream; PRs target `origin/main` |
| `sven` | `svenreimers/nbfx` | fork; feature branches are pushed here |
| `source` | `gluonhq/netbeans` | the original nbfx source repo (read-only reference; unused) |

## Branches

| Pattern | Use |
|---|---|
| `main` | Integration/release line; tracks `origin/main`. Never commit work directly. |
| `feature/<topic>` | One branch per step; `<topic>` is a short, readable slug (see table). Default working branch. |
| `spike/<topic>` | Throwaway investigations that may not be merged. |
| `fix/<topic>` | Bug fixes not tied to a step. |
| `docs/<topic>` | Documentation-only changes. |

Rules: lowercase, hyphen-separated, descriptive; no numeric codes or personal prefixes. The step name is
recorded in the PR body for traceability.

### Step → branch

| Step (from `07-small-steps.md`) | Branch |
|---|---|
| Progress UI parity | `feature/progress-ui` |
| Progress: drop the launcher's hard-wired progress | `feature/progress-launcher-removal` |
| Status elements: dynamic registration | `fix/status-elements-dynamic-registration` |
| Wizard framework (**incl. the themed dialog/makeover**) | `feature/wizard-framework` |
| FX dialogs & notifications (UI) | `feature/fx-dialogs` |
| Theming & branding (light/dark, CSS via branding) | `feature/theming` |
| Options system: infrastructure + example | `feature/options-foundation` |
| Options: settings/preferences browser (debug aid) | `feature/options-settings-browser` |
| Options: Appearance category | `feature/options-appearance` |
| Options: Editor + Fonts & Colors categories | `feature/options-editor` |
| Options: Java/JavaFX category | `feature/options-java` |
| Options: Miscellaneous/Advanced + Team categories | `feature/options-misc` |
| Options: export/import (incl. legacy NetBeans import) | `feature/options-export` |
| Options: global Quick Search provider | `feature/options-quicksearch` |
| Output window (api.io + ANSI + FX view) | `feature/output-window` |
| Execution service (process model + pumps) | `feature/execution-service` |
| Terminal emulator model (VT/xterm) | `feature/terminal-model` |
| Terminal PTY backend (JLine) | `feature/terminal-pty` |
| Terminal view (JavaFX) | `feature/terminal-view` |
| Run/build output integration (Terminal default; Maven as a process) | `feature/run-output-integration` |
| Terminal/Output options | `feature/terminal-options` |
| Remote terminal (later) | `feature/terminal-remote` |
| Nodes & views API (NodeModel/ChildrenModel/PropertyModel + FX views) | `feature/nodes-api` |
| Property sheet & editors (PropertyModel, editor SPI, outline columns) | `feature/property-sheet-editors` |
| Views on nodes (migration) | `refactor/views-on-nodes` |
| Project modularity refactoring | `refactor/project-modularity` |
| New File templates | `feature/newfile-templates` |
| New File: per-template wizard panels | `feature/newfile-template-panels` |
| New Project templates | `feature/newproject-templates` |
| FxProject model (headless project model) | `feature/fxproject-model` |
| Project problems UI (FX) | `feature/project-problems-ui` |
| Java Platforms | `feature/java-platforms` |
| J2SE customizers | `feature/customizers-j2se` |
| Platform customizers | `feature/customizers-platforms` |
| Build actions | `feature/build-actions` |
| Run lifecycle | `feature/run-lifecycle` |
| Run configurations | `feature/run-configurations` |
| Ant runtime | `feature/ant-runtime` |
| SceneBuilder embedding | `spike/scenebuilder-embedding` |
| JavaFX cluster | `feature/javafx-cluster` |
| JavaFX project | `feature/javafx-project` |
| JavaFX customizers | `feature/javafx-customizers` |
| FXML completion | `feature/fxml-completion` |
| SceneBuilder editor | `feature/scenebuilder-editor` |
| Documentation set (these docs) | `planning/javafx-support` |

The planning set lives in **`docs/plan/`** on the `planning/javafx-support` branch; it is the
communication base for the plan (see [`README.md`](README.md)).

**Parallelism:** `feature/fx-dialogs`, `feature/theming` and `feature/nodes-api` can be developed
alongside the wizard/New File topics — they add separate modules (`nbfx.dialogs`, `nbfx.theme`,
`nbfx.nodes`/`nbfx.nodes.ui`) and do not touch `nbfx.wizard` or the project/template files. The
project-problems UI depends on `fx-dialogs`; `fx-dialogs` and `theming` share the semantic colour token
names; the node model must land before the FxProject node factories. `feature/property-sheet-editors`
follows `nodes-api` (it extends `nbfx.nodes`/`nbfx.nodes.ui` and rewrites `nbfx.propertysheet`) and must
land before the views migration. The `refactor/views-on-nodes` migration is a follow-up to both and
rewrites working views, so it should not overlap with concurrent navigator/Structure/Properties work.
`feature/options-foundation` rewrites `nbfx.options` and adds the `nbfx.settings` model module; the
options category/browser/export/Quick-Search branches follow it, and `appearance` depends on `theming`
while `java` depends on the platforms/build steps. The output/terminal chain is
`feature/output-window` + `feature/execution-service` + `feature/terminal-model` in parallel →
`feature/terminal-pty` → `feature/terminal-view` → `feature/run-output-integration` (Terminal default,
Maven as a process), with `feature/terminal-options` after the options foundation; the run integration
also touches the build providers and should not overlap with concurrent build/Maven work.

### Current local stack (history condensed by concern)

```
main
 └─ feature/wizard-framework        (2 commits: framework API; themed dialog + CSS + icons + list-hover fix)
     └─ refactor/project-modularity (1 commit: project UI -> nbfx.project.ui)
         └─ feature/newfile-templates (2 commits: headless template model; New File chooser on the framework,
                                       plus a follow-up `styles:` app list-hover fix)
```

The wizard **makeover** is part of `feature/wizard-framework` (not a separate branch); the earlier
`feature/wizard-makeover` and `feature/newfile-wizard` branches were folded into this stack and deleted.
The app-wide list hover/selection fix is split by concern: the `wizard.css` half lives in the wizard
branch, the app `styles.css` half is the follow-up commit on the New File branch.
`backup/wizard-stack` (pre-consolidation) and `backup/list-hover-fix` (pre-fold) keep the old tips until
the rewrites have been reviewed.

## Flow

1. Branch from an up-to-date `main`: `git switch main && git pull && git switch -c feature/progress-ui`.
2. Work the step's sub-steps; keep `ant build` + `ant check-purity` green.
3. Push to the fork and open a PR into `origin/main`:
   `git push -u sven feature/progress-ui` → PR `svenreimers:feature/progress-ui` → `gluonhq:main`
   (name the step, e.g. "Progress UI parity", in the PR body).
4. One step = one PR (they are sized to be reviewable on their own).
5. After merge: `git switch main && git pull`, then delete the branch locally and on `sven`.

## Commit messages

- Prefix with the step name: `progress-ui: add nbfx.progress module skeleton`,
  `customizers-j2se: FXML customizer base + Sources panel`.
- Reference the plan docs when useful, e.g. `progress-ui: … (07-small-steps.md / 06-progress-ui.md)`.
- Keep commits within a branch scoped to that step.

## Housekeeping

- `feature/separate-build` is merged (PR #1) and can be deleted locally and on `sven`.
- `nbfx/mvp-phase1` is a leftover from the initial import; delete it.
- The `source` remote is unused after the `resync-source` removal; `git remote remove source`.
- Commit this plan set on `docs/javafx-support-plan`, then link it from `docs/MVP.md` /
  `docs/repo-and-build.md`.
