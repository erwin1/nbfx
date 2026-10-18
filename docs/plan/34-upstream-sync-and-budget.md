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

## 2. Budget (numbers, not adjectives)

Recorded in `11-project-state.md` at the walking skeleton, then **every milestone**:

| Metric | Target (initial) | Measurement |
|---|---|---|
| Cold start → main window | **< 2.0 s** | timed launch on the reference machine, 5 runs, median |
| Cold start → usable (project view interactive) | **< 3.0 s** | scripted launch + first interaction |
| First code completion | **< 500 ms** | scripted open + completion request |
| Open project (medium Maven project) | **< 2.0 s** | scripted |
| Open Output / debugger / profiler view | **< 300 ms** | scripted, first show |
| Module count in the nbfx clusters | **< N** (set at skeleton; retirement lowers it) | cluster config count |
| Heap after startup | **< 512 MB** | process RSS/heap after the scripted startup |
| Open project heap | **< 1 GB** | scripted |
| Regression policy | > 20% regression blocks the milestone | compare with the previous record |

No budget without a number, no number without a measurement, no measurement without a recorded owner.

## 3. Execution board and critical path

The backlog (07) is a library, not a schedule. The **board** is the next ten steps, each with an owner,
a milestone and its dependencies; a step enters the board only when its prerequisites are green.

**Critical path (initial).**

```
walking-skeleton
   └─▶ api-split ─▶ api-naming ─▶ annotations-split
          └─▶ services-not-statics ─▶ guardrail-structural-checks
   └─▶ options-foundation ─▶ (options categories)
   └─▶ fx-dialogs ─▶ theming ─▶ (all UI)
   └─▶ nodes-api ─▶ property-sheet-editors ─▶ views-on-nodes
   └─▶ output-window + execution-service + terminal-model ─▶ terminal-view ─▶ run-output-integration
   └─▶ (debug/profiler foundations after execution-service)
```

- **WIP limit:** at most 2 verticals in flight per person; the board is reviewed at each milestone.
- **Definition of ready:** prerequisites landed, the layer/module of doc 27 known, the API tier known
  (doc 32), the guardrails expected to stay green.
- **Definition of done:** tests (unit/UI/API), guardrails, budget measured, docs updated, retirement
  impact recorded (doc 33), `apichanges` updated for T1/T2.

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
