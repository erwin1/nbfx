# 24 — VCS, diff & local history

Steps: **VCS core** (`feature/vcs-core`), **Git** (`feature/vcs-git`), **Diff & patch**
(`feature/diff-and-patch`), **Local history** (`feature/local-history`), later **Mercurial/Subversion**
(`feature/vcs-hg`, `feature/vcs-svn`).

## Why

Version control is a basic IDE feature. nbfx has a small Git client (`nbfx.vcs`: `GitClient`,
`GitStatusView`, a status-bar branch element) but no plan for status badges, commit, history, diff, blame,
branches/remotes, credentials or ignore rules — and no diff/merge or local history at all.

## What nbfx has today

`nbfx.vcs` (6 classes, CLI-based Git: `GitClient`, `GitStatusView(ViewProvider)`,
`GitBranchStatusElement`, `SelectGitCommand`, `GitCommandsProvider`); doc 09 mentions `GitStatusView` and
`IgnoreListDialog`; no diff view, no history, no local history.

## Research (original codebase)

| Module | Files / Swing | Role | Verdict |
|---|---|---|---|
| `ide/versioning.core` | 33 / 16 | VCS model/SPI: `VersioningManager`, `VersioningSystem`, `VCSInterceptor`, `VCSContext`, `DelegatingVCS`, `VCSHistoryProvider`, `VersioningAnnotationProvider` | **reuse** the model/SPI (headless); port UI pieces |
| `ide/versioning` + `ide/versioning.util` | 12 / 4, 140 / 81 | registration, commands, history utilities, `DiffSidebar*`, `HistoryDiffView` | reuse `versioning.util` history/core; **port** the sidebar/diff UI |
| `ide/versioning.masterfs` | 2 / 1 | **filesystem status annotations** (`VersioningAnnotationProvider`, `FilesystemInterceptor`) | **reuse** (small) for badges/annotations on files |
| `ide/versioning.ui` | 28 / 23 | shared UI (history/diff options, `HistoryOptions`, tooltips) | **port** |
| `ide/git` (+ `ide/o.eclipse.jgit*` external, EDL) | 241 / 165 | Git support: `ui/{status,commit,branch,merge,diff,ignore,reset,revert,checkout,conflicts,shelve,repository}`; jgit modules (`o.eclipse.jgit`, `.ssh.apache`, `.lfs`, `.gpg.bc`) | **reuse** headless git logic/jgit; **port** the `ui/*` dialogs to FX |
| `ide/mercurial`, `ide/subversion` | — | Hg/Svn | later (same pattern) |
| `ide/diff` + `ide/languages.diff` | 82 / 58 | diff engine (`modules.diff.builtin.visualizer.UnifiedDiff`), merge (`modules.merge`), `spi/diff`, `api/diff`, options, sidebar | **reuse** the diff/merge engine + `spi/diff`; **port** the view |
| `ide/localhistory` | 18 / 9 | `LocalHistory`, `LocalHistoryStore`, `LocalHistoryVCS(Interceptor)`, `ShowHistoryAction`, `HistoryTopComponent` | **reuse** the store/model; **port** the timeline UI |
| `platform/keyring` | — | credential storage | **reuse** for HTTPS/SSH credentials |
| `ide/bugtracking*` | — | issue tracker integration | later |

Licence note: jgit modules are already in the NetBeans tree (EDL, permissive); reusing them is fine.

## Target architecture

```
nbfx.vcs.core        (headless model, JavaFX-free)
  VcsRegistry        registered VCS (git/hg/svn), file status cache (reuse versioning.core + masterfs)
  VcsStatus          per-file status; annotation provider for Files/Projects trees
  VcsHistory         log/history model per file/repository; annotate (blame) model
  VcsCommands        add/commit/update/revert/merge/branch/tag/remote/ignore/credential helpers
  git/               Git implementation over jgit (o.eclipse.jgit) or the CLI (existing GitClient)
nbfx.vcs.ui          (JavaFX)
  StatusView         changed/added/removed tree, per-project grouping, actions, badges
  CommitDialog       file selection, commit message, diff preview, amend, "commit and push"
  HistoryView        revisions table + commit details + diff, annotations (blame) toggle, compare with
  DiffView           FX side-by-side/merged diff (shared with doc 01/in-editor diff), patch apply/export
  BranchView/RemoteView/IgnoreList   branch switch/create, fetch/push/pull, ignore rules
  LocalHistoryView   per-file timeline + revert, deleted-files list
```

## Steps

1. **VCS core** (`feature/vcs-core`) — reuse `versioning.core` + `versioning.masterfs`; a `VcsStatus`
   model + annotation provider for the navigator views (docs 12); VCS menus/actions; decide the Git
   implementation (existing CLI `GitClient` vs jgit) with a status/jobs/progress model; job
   cancellation and output.
2. **Git** (`feature/vcs-git`) — FX status view, commit dialog (selection + diff preview + amend), history
   view (log, details, diffs, blame), diff/compare with a revision, branches/tags (switch, create, merge,
   rebase?), remotes (fetch/pull/push, credentials via `platform/keyring`), revert/reset, ignore list,
   conflict resolution view; integrate notifications (doc 09) and progress (doc 06).
3. **Diff & patch** (`feature/diff-and-patch`) — reuse `ide/diff`'s engine and `spi/diff`; FX diff/merge
   component (side-by-side, navigation, inline diff); "Diff to…"/"Diff with selected"; apply/export patch
   (unified diff), patch queue; reuse in the commit/merge/history views and the editor's "diff sidebar"
   (`versioning.util` model).
4. **Local history** (`feature/local-history`) — reuse `ide/localhistory` store/VCS; FX file/project
   timeline, diff against a revision, revert, deleted-file list; enable/disable + history settings
   (doc 14 Advanced options).
5. **Later** — Mercurial/Subversion (same `versioning.core` pattern), bugtracking integration.

## Acceptance

- The Files/Projects trees show VCS status badges; a changed file's context menu offers Git actions.
- Commit (with selection + diff preview), history/log with per-revision diff, blame, branch switch/create,
  fetch/pull/push (credentials stored via keyring), revert/reset and ignore rules work against a local repo.
- Diff/patch: applying a unified diff produces the expected edits; export produces a valid patch.
- Local history shows revisions and reverts.
- `nbfx.vcs.core` is JavaFX-free (`check-models`); VCS operations are cancellable off the FX thread.

## Risks / open questions

- **Git backend**: keep the CLI client (simple, no new deps, but parsing/robustness) or move to **jgit**
  (already shipped, EDL; richer API: `Repository`, `RevWalk`, `DiffFormatter`, credentials/SSH). Recommend
  jgit for the engine, keeping the CLI as fallback.
- **`versioning.core`/`util` Swing coupling** — verify which classes are headless; port the sidebar/tooltip
  UI (`DiffSidebar*`).
- **Diff engine reuse**: `ide/diff`'s engine is headless, its view is Swing; the merge (3-way) view is the
  hardest part — phase it.
- **Annotations/badges** must cooperate with the navigator views (doc 12) and the icon/badge model of doc 13.
- **Credentials/SSH**: `keyring` + jgit's ssh session; native ssh agent support; notarisation for the ssh
  library.
- **Large histories**: virtualized tables, paging.
