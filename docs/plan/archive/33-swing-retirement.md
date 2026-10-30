<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
# 33 — Swing retirement plan (the actual deliverable)

A front-end replacement is not done when the JavaFX surfaces exist; it is done when the **Swing modules
leave the distribution**. This doc defines the retirement matrix, the exemptions (for modules the reused
engines still need), the guards, and the per-milestone exit criteria.

Related: the guardrail redesign in [30-api-governance.md](30-api-governance.md) §6, the layer map in
[27-layer-module-map.md](27-layer-module-map.md), the roadmap milestones in [26-roadmap.md](26-roadmap.md).

## 1. Method

For every upstream module that exists only to provide Swing UI (or a Swing UI SPI), record:

- **direct**: nbfx ships it today only because `nb.cluster.*` is included wholesale;
- **transitive**: a reused headless engine needs it (cannot be removed without upstream work);
- **replacement**: the nbfx module/doc that takes over the capability;
- **blocking steps**: what must land first;
- **milestone**: when the module leaves the cluster (or gets an exemption with a deadline).

Three states per module: `retire` (removed from the nbfx clusters), `exempt` (still present, owner +
deadline + upstream proposal), `keep` (it is a headless API we intentionally use — e.g.
`platform/spi.quicksearch`, `platform/api.io`, `platform/keyring`).

**Pruning** uses the cluster config plus the harness `-prune-excluded` step (jar/config/update_tracking
removal); the structural guard then asserts the module is absent from the built distribution.
**Provenance:** `-prune-excluded` is introduced by `feature/progress-ui` (PR #2); until that branch is
merged the retirement guard treats pruning as unavailable and only *reports* the gap (it must not be red
on day one).

## 2. Retirement matrix

### M0 — platform shell

| Upstream module | kind | Replacement | Blocking steps | State |
|---|---|---|---|---|
| `core.windows` | direct | `nbfx.docking`/`nbfx.windows` (landed) | shell/docking done | **retire** (M0) |
| `core.ui` (Swing window/menu bits) | direct | `nbfx.ui`/`nbfx.menus`/`nbfx.statusbar` | shell done | **retire** (M0), audit remaining headless pieces first |
| `core.output2` | direct | `nbfx.output` + `api.io`/`spi.io` (doc 15) | output window | **retire** (M0) |
| `core.io.ui` | direct | `nbfx.output` (doc 15) | output window | **retire** (M0) |
| `progress.ui` | direct | `nbfx.progress` (doc 06, landed on the progress branch) | merge progress PR | **retire** (M0) |
| `notifications`, `uihandler`, `uihandler.exceptionreporter`, `lib.uihandler` | direct | `nbfx.dialogs` (doc 09) | dialogs/notifications | **retire** (M0) |
| `openide.dialogs` | direct | `nbfx.dialogs` (doc 09) | dialogs | **retire** (M0) — exemptions for engines if any |
| `openide.awt` | direct/transitive | `nbfx.actions`/`nbfx.menus`/`nbfx.editor.actions` | actions/menus done | **retire** (M0) with exemptions for engines that require it (register + upstream proposal) |
| `o.n.swing.laf.flatlaf`, Swing LAF options | direct | `nbfx.theme` (doc 10) | theming | **retire** (M0) |
| `core.multitabs` (Swing tabs) | direct | `nbfx.docking` tabs | shell done | **retire** (M0) |
| `platform/favorites` | direct | `nbfx.favorites` | favorites step | **retire** (M0) |
| `platform/javahelp` | direct | none (Javadoc-first help, doc 22/25) | help decision | **retire** (M0) |
| `autoupdate.ui` | direct | `nbfx.plugins` (doc 25) | plugins UI parity | **retire** (M1) |

### M1 — IDE core

| Upstream module | kind | Replacement | Blocking steps | State |
|---|---|---|---|---|
| `openide.nodes` | direct | `nbfx.nodes` (doc 12) | nodes/views migration | **retire** (M1) |
| `openide.explorer` | direct | `nbfx.nodes.ui` + property sheet (docs 12/13) | property sheet, views migration | **retire** (M1) |
| `openide.windows` | direct/transitive | `nbfx` views/docking; `api.io` for IO | view manager; engine IO ports | **exempt** for engines (register; upstream `api.io` complete) until M2 |
| `options.api` (Swing UI/panels) | direct | `nbfx.options` (doc 14) | options system + panels | **retire** (M0/M1) — the SPI is ours; no upstream modules should register Swing panels |
| `ide/projectui` (New File/Project UI) | direct | `nbfx.project.ui` (docs 07/02) | templates/wizards | **retire** (M1) with exemptions for engines that need projectui.base |
| `ide/jumpto` (Go to dialog) | direct | `nbfx.quicksearch` + `nbfx.editor.intelligence` (docs 18/32) | goto/search | **retire** (M1) |
| `ide/diff` (Swing view; engine reused) | direct UI | `nbfx.diff` (doc 24) | diff view + patch | **retire** UI (M1), keep the engine classes in a headless module/exemption |
| `ide/localhistory` (UI) | direct UI | `nbfx.localhistory` (doc 24) | local history view | **retire** UI (M1), reuse the store |
| `ide/versioning.ui`, `versioning.util` (UI parts) | direct | `nbfx.vcs` (doc 24) | VCS UI | **retire** UI (M1), keep `versioning.core` |
| `ide/git` (UI) | direct UI | `nbfx.vcs` (doc 24) | Git UI | **retire** UI (M1), reuse jgit |
| `ide/tasklist.ui`, `ide/editor.bookmarks` | direct | `nbfx.tasklist`/`nbfx.bookmarks` (doc 25) | misc views | **retire** (M1) |
| `ide/project.libraries.ui` | direct | `nbfx.libraries` (doc 25) | libraries manager | **retire** UI (M1), keep `project.libraries` API |
| `ide/spi.editor.hints` | direct | `nbfx` editor annotation/fix SPI (docs 20/31) | hints UI | **retire** (M2) |
| `ide/spi.viewmodel` (Swing module) | direct | nbfx view-model layer (docs 12/16) | views migration | **retire** (M2) — unless an engine reuses it; then exemption |
| `ide/gototest` | direct | `nbfx.editor.intelligence` | navigation | **retire** (M1) |

### M2 — Java tooling

| Upstream module | kind | Replacement | Blocking steps | State |
|---|---|---|---|---|
| `java/debugger.jpda.ui` | direct | `nbfx.debug` UI (doc 16) | debugger UI steps | **retire** (M2) |
| `java/spi.debugger.jpda.ui` | direct | nbfx debug SPI | debugger UI | **retire** (M2) |
| `ide/spi.debugger.ui` | direct | nbfx debug SPI/views | debugger UI | **retire** (M2) |
| `java/junit.ui`, `junit.ant.ui`, `testng.ui`, `java.testrunner.ui` | direct | `nbfx.test.ui` (doc 23) | test results | **retire** (M2) |
| `java/java.navigation` (hierarchy UI) | direct | `nbfx.editor.intelligence` (doc 18) | hierarchies | **retire** (M2) |
| `java/java.editor` (Swing UI/actions) | direct | `nbfx.editor` + `nbfx.java.editor` | editor intelligence | **retire** UI (M2); keep headless parts if needed (exemption) |
| `java/java.hints.ui` | direct | `nbfx` hints options (doc 20) | hints options | **retire** (M2) |
| `java/refactoring.java` (UI parts) | direct UI | `nbfx.refactoring.ui` (doc 19) | refactoring UI | **retire** UI (M2); engine reuse per doc 30 §9 |
| `java/java.platform.ui` | direct | `nbfx.java.platform` (doc 02) | Java platforms | **retire** (M2) |
| `profiler/lib.profiler.ui`, `lib.profiler.charts` | direct | `nbfx.profiler.ui` (doc 17) | profiler UI | **retire** (M2) |
| `profiler/*` UI modules (`profiler`, `.heapwalker`, `.snaptracer`, `.oql*`, `.ppoints`, `.options`, `.attach`, `.j2se`, `.nbimpl`, `.projectsupport`, `maven.profiler`, `.freeform`, `.nbmodule`) | direct | `nbfx.profiler.*` (doc 17) | profiler steps | **retire** (M2), keep `lib.profiler` heap parser (exemption) |
| `java/debugger.jpda.visual`, `profiler/debugger.jpda.heapwalk` (UI) | direct | later `feature/debug-visual` (doc 16) | — | **retire** (M2/M4) |

### Explicit exemptions (keep, with owner + deadline)

| Module | Why it stays | Owner | Deadline / upstream proposal (planning target, re-baselined at M0) |
|---|---|---|---|
| `org.openide.text` | reused by editor/debugger engines (annotations) | Sven | M2 (2027-07) — replace with nbfx annotation SPI or an upstream headless API |
| `openide.windows` (API classes) | transitive from engines using old IO | Sven | M2 (2027-07) — move engines to `api.io`; upstream |
| `projectuiapi.base` | project problems/actions bridging | Sven | M1 (2027-03) — replace with `ProjectModel` hooks |
| `lib.profiler` (non-UI) | HPROF parser / `.nps` reader (doc 17) | Sven | M2 (2027-07) — upstream a heap API or keep vendored with monthly rebase |
| `spi.quicksearch` | API we intentionally use | Sven | keep (T1) |
| `keyring`, `masterfs`, `api.io`, `spi.io` | headless services we intentionally use | Sven | keep |

**Measured baseline (2026-10-18).** The matrix states are the *targets*; the first guard run records
the measured truth (present in the built distribution? which dependants?) in this section. Known facts at
the baseline: `progress.ui`, `notifications`/`uihandler*` are excluded/pruned on the progress branch;
`core.windows` is dropped from the nbfx shell; `openide.awt`/`openide.nodes`/`openide.explorer`/
`openide.windows` are present transitively and are the first exemptions. Every `retire` claim is converted
to a measurement before the milestone exit.

## 3. Guards and checks

- The **structural guard** (doc 30 §6) asserts: every `retire` module of the current milestone is absent
  from the built cluster config; every `exempt` module has a live exemption entry (owner + deadline) — an
  expired exemption fails the build.
- The **cluster audit** lists what the nbfx clusters actually pull in (module graph + `config/Modules`),
  so "transitive" is measured, not assumed.
- The same graph check enforces **no cycles** and **no upward layer edge** (`nbfx-platform` must not depend
  on `nbfx-ide`/`nbfx-java`); the propertsheet/project-model moves (doc 30 §1.4) are the first tests.
- The **Swing surface audit** checks the built distribution for remaining `javax.swing`/`java.awt` usage
  reachable from nbfx UI modules (jdeps over the cluster) and reports the owning modules.
- Each milestone's demo script proves the retired capability now works through nbfx.

## 4. Milestone exit criteria (retirement view)

| Milestone | Retired | Remaining exemptions |
|---|---|---|
| M0 | `core.windows`, `core.ui` Swing bits, `core.output2`, `core.io.ui`, `progress.ui`, `notifications`/`uihandler*`, `openide.dialogs`, `o.n.swing.laf.flatlaf`, `core.multitabs`, `javahelp`, `favorites` | `openide.awt`, `openide.windows` (engine transitives) |
| M1 | `openide.nodes`, `openide.explorer`, `options.api` UI, `projectui` UI, `jumpto`, `diff`/`localhistory`/`versioning`/`git` UI, `tasklist.ui`, `editor.bookmarks`, `project.libraries.ui`, `autoupdate.ui`, `gototest` | `openide.windows`, `spi.viewmodel` (engines), `spi.editor.hints` |
| M2 | all debugger/profiler/test/hints/refactoring/platform/navigation UI modules | `lib.profiler` heap, `openide.text`, `projectuiapi.base`, `openide.windows` where engines force it |
| M3 | — (JavaFX usage; no Swing) | same as M2 |
| M4 | remaining misc (`i18n`, `beans`, visual/heapwalk UI) | same |

## 5. Risks

- **Transitive coupling**: several engines (`debugger.jpda`, `java.hints`, `refactoring.java`,
  `lib.profiler`) pull Swing modules at runtime. Removing the *UI* module is possible; removing the
  *transitive* dependency requires upstream decoupling. This is where the exemption register and the
  upstream proposals (doc 30 §10) meet the retirement plan.
- **`options.api`** is both SPI and UI; confirm which classes other reused modules need before removing it
  (an exemption may be needed for `options.api`'s non-UI SPI packages).
- **Pruning mistakes**: an excluded module whose jar remains in the dist keeps loading (the progress-UI
  lesson) — always use `-prune-excluded` and assert absence in the built distribution.
- **Hidden UI in headless modules**: audit `core.*` modules for Swing leftovers; retire only after the
  FX replacement covers the behaviour (per milestone demo).
