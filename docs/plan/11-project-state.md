# 11 — Project state (snapshot)

Snapshot of the repository state after the history consolidation. Use this to understand what exists
locally, what is on the remote, and how to go back.

## Remotes

| Remote | URL | Purpose |
|---|---|---|
| `origin` | `https://github.com/gluonhq/nbfx` | upstream (PR target) |
| `sven` | `https://github.com/svenreimers/nbfx` | fork (branch source for PRs) |
| `source` | `https://github.com/gluonhq/netbeans` | unused |

`main` tracks `origin/main` at `dc5ba6a9b7` (“Feature: Separate NetBeans FX build from main apache
Repository (#1)”).

The `netbeans` submodule is pinned to `0104eeb3da7094338212ea8a37a32a73354c7574`; its working tree is
intentionally dirty (applied patches + generated `nbbuild/cluster.properties`). `ant clean` restores it.

## Branches (local)

```
main
 ├─ feature/progress-ui                  7c2eb5bebe   PR #2 open (3 commits)
 ├─ feature/progress-launcher-removal    25731c0362   1 commit
 ├─ fix/status-elements-dynamic-registration  d20647375a   1 commit
 └─ feature/wizard-framework             23ce07d257   2 commits
     └─ refactor/project-modularity       a961fb9546   1 commit
         └─ feature/newfile-templates    69ce22420e   2 commits + styles fix
planning/nbfx-roadmap                                   this documentation branch
```

| Branch | Commits |
|---|---|
| `feature/progress-ui` | `bfe9af80db` env/controller/worker + pruning; `9acbf2df89` prune excluded modules; `7c2eb5bebe` coalescing, View action, system tasks, FxProgress→ProgressHandle |
| `feature/progress-launcher-removal` | `25731c0362` ProjectLoads/BuildRunner use ProgressHandle, remove `FxProgress` |
| `fix/status-elements-dynamic-registration` | `d20647375a` project/caret/line-separator via `@FxStatusRegistration` |
| `feature/wizard-framework` | `bc393427a0` descriptor/iterator/page hooks; `23ce07d257` themed dialog/icons/CSS (incl. list hover/selection) |
| `refactor/project-modularity` | `a961fb9546` project UI (New File/New Project wizards) → `nbfx.project.ui` |
| `feature/newfile-templates` | `3c44e6369c` template model + rule + guard; `06c4cf986f` template-driven chooser; `69ce22420e` app list hover/selection styles fix |

Backups kept until the rewrites are reviewed:

- `backup/wizard-stack` — `9c1eb22526` (pre-consolidation tip)
- `backup/list-hover-fix` — `a245358866` (pre-fold tip)

Legacy/local leftovers, not part of this plan: `feature/separate-build` (merged, `aa0aa1c5c3`),
`nbfx/mvp-phase1` (`be0c845cdc`), the `source` remote.

## Verification performed

- `feature/progress-ui`: full `ant build` green; `ant check-purity` 0/0/0/0; progress worker tests 3/3.
- `feature/wizard-framework`: nbfx-platform build; wizard tests 6/6.
- `refactor/project-modularity`: `nbfx-ide` builds.
- `feature/newfile-templates`: full `ant build` green; `ant check-purity` 0/0/0/0 (including
  `model-module UI violations: 0`); template tests 8/8; wizard tests 6/6.
- Final trees: the consolidated branches are **byte-identical** to the previous tips
  (`git diff <new tip> <old tip>` empty), so no functional change was introduced by the rewrite.

## History consolidation (what was rebuilt and why)

The wizard work and the New File work were interleaved across ~20 commits with mixed files. The history
was rebuilt by concern:

```
main
 └─ feature/wizard-framework        framework API; themed dialog + CSS + icons (makeover folded in)
     └─ refactor/project-modularity project UI module move (unchanged content)
         └─ feature/newfile-templates template model; New File chooser; app list-hover styles fix
```

Method (for reproducing or extending it):

1. Back up the current tip (`backup/wizard-stack`).
2. Create the wizard branch from `main` and commit the `nbfx.wizard` files only (2 commits).
3. Cherry-pick the modularity commit onto it (its diff touches only the project-UI files).
4. Create the New File branch from there and commit the template model, then the New File UI, then the
   app styles fix.
5. Fold the wizard half of the styles fix into the wizard branch’s second commit (amend) and rebase the
   downstream branches; drop the duplicated half from the tip commit.
6. Verify `git diff <new tip> <old tip>` is empty, then delete the temporary/obsolete branches.

The only split-by-hand file was the styles hover/selection fix: the `wizard.css` half belongs to the
wizard branch, the app `styles.css` half is the follow-up commit on the New File branch.

## Next actions

1. **New File: per-template wizard panels** (`feature/newfile-template-panels`) — 27 iterators, Java file
   wizard first (see `07-small-steps.md`).
2. **FX dialogs & notifications** (`feature/fx-dialogs`) — can run in parallel (see `09-…`).
3. **Theming & branding** (`feature/theming`) — can run in parallel (see `10-…`).
4. Open PRs for the local branches once `feature/progress-ui` (#2) lands (rebase onto `main` first).
5. Clean-ups: delete `feature/separate-build`, `nbfx/mvp-phase1` and the `source` remote when convenient;
   delete the `backup/*` branches after reviewing the rewritten history.

## PRs

- **#2** `feature/progress-ui` → `main` (open). #3 (a stacked launcher-removal PR) was closed; the branch
  is kept local and will be opened as a plain PR after #2 merges.
