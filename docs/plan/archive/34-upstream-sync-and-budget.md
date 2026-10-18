# 34 — Upstream sync, budgets and the execution board

Two things the plan lacked and the review demanded: a plan for **tracking the upstream** you keep patching,
and **numbers** (budget) plus a **critical path** (board) instead of 120 unordered steps.

## 1. Upstream sync (the #1 long-term risk)

nbfx pins `nbfx.submodule.commit` and carries `patches/*.patch`. Without a cadence, the patch set grows
and the pinned NetBeans ages until nobody dares update it.

**Cadence and mechanics.**

| Item | Rule |
|---|---|
| Submodule bump | **monthly** (or per upstream release) on a `chore/upstream-sync` branch: update the pin, reapply the patch set, run the full build + tests + guardrails; merge only when green. |
| Patch budget | at most **N files / M lines** (set at the first sync; current count recorded); growth blocks the milestone until a patch is retired or an upstream PR lands. |
| Patch register | one row per patch: why, upstream issue/PR link, owner, retirement milestone. No new patch without a register row and an upstream issue. |
| CI | a `next-upstream` job weekly against upstream `master` (build only, allowed to fail informationally) and a `pinned` job on every PR. |
| Drift metric | `git rev-list --count nb.pin..upstream/master` recorded per sync; a rising trend is a milestone risk. |
| Friend patches | covered by the friend register (doc 30 §8): each has an owner + upstream proposal + deadline. |

**Why this is non-negotiable:** every reuse decision (doc 30 §9) and every patch is a bet that the
upstream evolves compatibly. Without a sync cadence, nbfx silently becomes a frozen fork.

## 2. Budget (baseline first, then deltas)

A budget needs a **baseline** and a **reference machine**, not invented absolutes. At the walking skeleton
we record the baseline; every later milestone is compared against it:

| Metric | Baseline / target | Measurement |
|---|---|---|
| Cold start → main window | **recorded at skeleton**; later: **no more than +10%** vs the previous milestone | timed launch on the reference machine (CPU/RAM/OS recorded), 5 runs, median |
| Cold start → usable (project view interactive) | recorded at skeleton; ≤ +10% | scripted launch + first interaction |
| First code completion | recorded at skeleton; ≤ +10% | scripted open + completion request |
| Open project (medium Maven project) | recorded at skeleton; ≤ +10% | scripted |
| Open Output / debugger / profiler view | recorded at skeleton; ≤ +10% | scripted, first show |
| Module count in the nbfx clusters | **measured at skeleton** (retirement lowers it; recorded name `nbfx.modules`) | cluster config count |
| Heap after startup | recorded at skeleton; ≤ +10% | process RSS/heap after the scripted startup |
| Open project heap | recorded at skeleton; ≤ +10% | scripted |
| Regression policy | **> 20% regression blocks the milestone**; 10–20% needs a written justification | compare with the previous record |

The numbers in earlier drafts (`2 s`/`3 s`/`500 ms`) are **provisional** and are replaced by the measured
baseline when the skeleton lands; the reference machine and script are recorded in `11-project-state.md`
alongside the numbers. No budget without a baseline, no baseline without a machine, no measurement without
an owner.

## 3. Execution board and critical path

The backlog (07) is a library, not a schedule. The **board** is the next ten steps, each with an owner, a
milestone and its dependencies; a step enters the board only when its prerequisites are green.

**Milestone timing (planning targets, re-baselined at each milestone).**

| Milestone | Target | Content |
|---|---|---|
| M0 | 2026-12 | platform shell/services + walking skeleton + API discipline/plugin contract |
| M1 | 2027-03 | IDE core (projects/editor/structure/find/VCS/tests/libraries) |
| M2 | 2027-07 | Java tooling (project types/platforms/editor intelligence/debug/profiler) |
| M3 | 2027-09 | JavaFX usage (project/FXML/SceneBuilder) |
| M4 | 2027-12 | breadth (module system, i18n, beans, JShell, remote, accessibility) |

**Next ten steps (the current board).**

| # | Step | Owner | Milestone | Depends on |
|---|---|---|---|---|
| 1 | `feature/walking-skeleton` (one project end-to-end + baseline) | Sven | M0 | current shell/launcher/docking/output (basic), Git status |
| 2 | `refactor/api-split` (inventory + tiers + `nbfx.api.*` split) | Sven | M0 | — (inventory table in 30 §1.1) |
| 3 | `refactor/api-naming` (capability names; freeze; mark in-flight PRs) | Sven | M0 | 2 |
| 4 | `refactor/module-moves` (constants, propertsheet→platform, project model, output service) | Sven | M0 | 2, `refactor/project-modularity` |
| 5 | `refactor/annotations-split` (T2 annotation-only modules) | Sven | M0 | 2, 3, 4 |
| 6 | `refactor/services-not-statics` (+ typed contracts) | Sven | M0 | 3 |
| 7 | `refactor/guardrail-structural-checks` (jdeps/graph/governance/naming) | Sven | M0 | 2, 3 |
| 8 | `spike/front-end-component-factory` (options prototype + upstream PR) | Sven | M0 | 3 |
| 9 | `feature/plugin-api` (T2 contract + trust + sample plugin) | Sven | M1 | 5, 7, 8 |
| 10 | `refactor/swing-retirement` (matrix + exemptions + prune guard) | Sven | M0/M1 | 7 |

*Immediately after the board:* `spi-factory-migration` (options/views/wizards/property editors; depends on the
component-factory spike and `api-naming`) and `feature/options-foundation` (with `fx-dialogs`/`theming`).

**Critical path.**

```
walking-skeleton ──▶ api-split ──▶ api-naming ──▶ module-moves ──▶ annotations-split ──▶ plugin-api
                                    │
                                    ├─▶ services-not-statics
                                    ├─▶ guardrail-structural-checks ──▶ swing-retirement
                                    └─▶ front-end-component-factory ──▶ options-foundation
                                                                        │
                                                       fx-dialogs ─▶ theming ─▶ all UI
nodes-api ─▶ property-sheet-editors ─▶ views-on-nodes
output-window + execution-service + terminal-model ─▶ terminal-view ─▶ run-output-integration
(debug/profiler foundations after execution-service)
```

- **WIP limit:** at most 2 verticals in flight per person; the board is reviewed at each milestone.
- **Definition of ready:** prerequisites landed, the layer/module of doc 27 known, the API tier known
  (doc 32), the guardrails expected to stay green, the retirement impact known (doc 33).
- **Definition of done:** tests (unit/UI/API), guardrails, budget measured, docs updated, retirement
  impact recorded (doc 33), `apichanges` updated for T1/T2, board updated.

## 4. Risk register (top)

| Risk | Trigger | Mitigation | Owner |
|---|---|---|---|
| Upstream drift / patch rot | patch budget exceeded, pin older than 6 months | monthly sync, upstream proposals, patch register | — |
| API governance abandonment | public packages without `arch`/`apichanges` | tier model (32), governance guard | — |
| Swing retirement stalls | milestone without a retired module | retirement matrix + guard (33) | — |
| Debugger/profiler engine internals change | dependency on `modules.*` internals | anti-corruption adapters + reuse register (30 §9) | — |
| Scope creep (platform as a product) | new T2 modules without need | tier rule; plugin contract only | — |
| Performance regression | budget exceeded | measurement per milestone; block | — |
| Plugin trust incident | unsigned/unverified plugin, HTTP center | signing + HTTPS + native checksums (32 §4) | — |

## 5. Adoption

- `07` gains the board/next-10 section (or links here) and the sync/upstream step.
- `08` gains the `chore/upstream-sync` branch.
- `11` records the budget numbers and the drift metric.
- `26` milestone exits reference the budget and the retirement state.
- `30`/`33` link here for cadence and metrics.

## 6. Acceptance

- A recorded baseline for every metric, and a recorded measurement per milestone.
- A monthly upstream sync run with a green build and an updated patch register.
- The board shows the next ten steps with owners and dependencies.
- Budget or retirement regressions block a milestone.
