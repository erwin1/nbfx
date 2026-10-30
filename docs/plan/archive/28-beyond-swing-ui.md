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
# 28 — Beyond the Swing UI: where legacy drags, where nbfx advances

The topic docs describe *what* to port. This doc records **where the Swing implementation of the reused
NetBeans layers constrains the UI** (so we do not unknowingly copy the constraint) and **how the JavaFX
reimplementation can be better**. Every idea is mapped to the step that should own it (§6).

## 1. Why this matters

nbfx replaces the presentation layer, but a ported SPI can still force a legacy *shape*: components
returned as `javax.swing.JComponent`, actions as `javax.swing.Action`, models rendered by
`org.openide.explorer`/`spi.viewmodel` Swing renderers, threading tied to `Mutex.EVENT`/`RequestProcessor`,
progress to the Swing `progress.ui`, IO to the legacy `org.openide.windows.InputOutput`. Where nbfx
reimplements the whole stack (not just a view), it should define a **FX-native contract** instead of
translating the Swing one, while keeping the **data** compatible.

## 2. Principles for the nbfx UI layer

1. **Model first, view second**: every surface (tree, table, tree-table, form, sheet) is a view over a
   UI-free model (`NodeModel`/`ExplorerContext`, view models, settings) — one model, many views.
2. **FX-native observability at the boundary**: JDK/`java.beans` in the reused engines, adapted to
   `ObservableList`/`ObservableValue` only at the UI edge (never FX types in models).
3. **Async by default**: `CompletionStage`/`Task`, cancellation, debouncing and progress as first-class —
   no `Mutex.EVENT`, no blocking the FX thread, no `NbDocument`-style synchronous analysis.
4. **Declarative registrations**: extend the layer-generating annotations (`@ViewRegistration`,
   `@ActionRegistration`, `@OptionsRegistration`, `@WizardRegistration`, `@ProjectTypeRegistration`,
   `@PropertyEditorRegistration`) to *every* extension point, so a module declares capability once.
5. **Services through `Lookup`** with narrow SPIs; feature-specific backends (build, debug, profile, VCS,
   test) are usage-layer services the IDE layer discovers.
6. **Theme tokens + accessibility**: all colour/space/font through semantic tokens (doc 10); keyboard-first
   navigation, screen-reader roles, high-DPI from the start.
7. **Reusable component kit**: one set of FX components (outline, property sheet, output, terminal,
   diff, charts, search popup) reused across IDE and usage layers instead of per-feature views.

## 3. Where the legacy drag is, and the FX-native advance

| Area | Reused Swing contract (the drag) | nbfx FX-native advance |
|---|---|---|
| **Nodes / views** | `org.openide.nodes.Node`/`Children`/`Sheet` + `openide.explorer` `BeanTreeView`/`OutlineView`/`TableView` and `spi.viewmodel` models rendered by Swing `OutlineTable` renderers | `NodeModel`/`ChildrenModel`/`ChildFactory` + `ExplorerContext` with **one model many views** (tree/tree-table/table/list), lazy/async children with cancellation, **columns as first-class `PropertyColumn`**, decoration/badge model, virtualized FX views, drag & drop and node operations (doc 12/13) |
| **Property sheet** | `openide.explorer.propertysheet` `PropertyEnv`/`PropertyPanel`/`InplaceEditor`; `java.beans` `PropertyEditor` discovery; single-selection tree | FX editors with a **registry by value type/editor id**, `PropertyEnv` validity protocol, **multi-selection aggregation** (`"<mixed>"`), async `LOADING`/`ERROR` values, `format`/`parse`/sort keys, the same editors in sheet *and* outline cells (doc 13) |
| **Options** | `OptionsPanelController#getComponent` returns `JComponent`; category/tab model in `CategoryModel`; per-panel `JComponent` customizers | `OptionsPanelController#getComponent` returns `Node`; declarative `@OptionsRegistration`/container/sub; **keyword search across categories and keys**, deep links, validity/error markers, **settings browser** as a debug aid (doc 14) |
| **Wizards** | `WizardDescriptor.Panel.getComponent` returns `java.awt.Component`; fixed `Iterator`; synchronous `instantiate` | FX panels, **dynamic page sets**, live Next/Finish validation, async `instantiate` with progress, themed shell, reusable category/chooser components (doc 07) |
| **Dialogs / notifications** | `NotifyDescriptor`/`DialogDisplayer` + `openide.dialogs` thread model; `notifications`/`uihandler` modules | `Dialogs` service with **roles, severity, notification line, toasts, "show again" keys**, FX exception reporting, ownership/async; no legacy `Mutex.EVENT` (doc 09) |
| **Progress** | `api.progress` + Swing `progress.ui`; `StatusDisplayer` status strings | progress **model** (tasks, structured subtasks, system tasks) + FX status line, all-tasks popup, cancel/retry, coalescing (doc 06) |
| **Output / IO** | legacy `org.openide.windows.InputOutput`/`IOProvider`; `core.output2` Swing component | implement the modern **`org.netbeans.spi.io.InputOutputProvider`** with a virtualised FX view; **ANSI SGR colours, hyperlinks, folds, positions, input prompts, limits**, structured task-linked output (doc 15) |
| **Terminal / execution** | none (NetBeans' terminal is Swing + legacy IO; `extexecution` is IO-coupled) | **`nbfx.execution` PIPE/PTY seam** + headless **VT/xterm model** + FX Canvas terminal; PTY via JLine; run defaults to a real terminal (doc 15) |
| **Editor** | Swing `JTextComponent`/`EditorKit`/`NbDocument` analysis, `editor.lib2`; `spi.editor.hints` `JComponent` UI | FX rich-text editor with an **async analysis pipeline** (parse/hints/refactor preview as cancellable tasks), annotations as first-class objects, inline rename/preview, later multi-caret (docs 18–21) |
| **Editor intelligence** | `spi.java.hints`/`spi.editor.hints` Swing popups; `jumpto` Swing dialog; `refactoring.api` `RefactoringUI` returns `JComponent` | headless `nbfx.editor.intelligence` (semantic access, providers) **shared** by goto, hints (lightbulb), refactoring preview and codegen; one **fix/annotation pipeline**; a **command palette** instead of separate Go-to dialogs (docs 18–21) |
| **Debugger** | `spi.debugger.ui`/`spi.viewmodel` `JComponent` views; `BreakpointType.getCustomizer()` returns `JComponent`; `ActionsProvider.getActions()` returns Swing `Action`s; `debugger.jpda.ui` Swing | FX view models with **async lazy variables**, breakpoint customizers as FX, actions via `ActionsManager` constants + nbfx `Command`s (no `javax.swing`), current-line/thread annotations in the FX editor, evaluate window, attach dialog (doc 16) |
| **Profiler** | `lib.profiler.ui` Swing chart components; `.nps` snapshot model; calibrate/instrument agent | **one analysis model, many sources** (live JFR streaming, `.jfr`, async-profiler, `.hprof`, `.nps`), **flame graphs**, timeline/canvas charts, async-profiler for fidelity, heap parser reused headless (doc 17) |
| **VCS / diff** | `versioning.ui` `DiffSidebar`/`HistoryDiffView`; `ide/diff` Swing `DiffController`/`DiffView`; jgit in Swing `ide/git` | FX status/commit/history/blame UX, **FX side-by-side/merge diff**, patch queue, credentials via keyring, cancellable jobs, unified with local history (doc 24) |
| **Tests** | `junit.ui`/`testng.ui`/`java.testrunner.ui` Swing results views | unified FX **Test Results** (all frameworks), re-run failed, go-to-failure, coverage overlay in the editor (doc 23) |
| **Plugins / autoupdate** | `autoupdate.ui` Swing plugin manager/update centers | FX manager with **signing/trust info, offline mode, update-center management, proxy**, progress via the task model (doc 25) |
| **Quick search** | `spi.quicksearch` providers + Swing popup | **global command palette**: actions, files, types, symbols, options, recent items — fuzzy matching, preview, keyboard-only (new platform step; consumed by docs 18/14) |
| **Task list / bookmarks** | `tasklist.ui`, `editor.bookmarks` Swing views | FX views with filters/counts, provider SPI reused; annotations shared with hints (doc 25) |
| **Theming** | `flatlaf`/`core.windows` LAF options; scattered Swing colour keys | **semantic token CSS** for every FX surface + editor palettes + **branding** (doc 10) |
| **Shell / windows** | `core.windows` (`TopComponent`, modes, `ExplorerManager`) | `ViewProvider`/`ViewManager` + Fx docking model; advancement: **multi-window/detached views**, saved layouts, per-project layouts (doc 00/12) |
| **Help** | JavaHelp topics (Swing) + context help | **Javadoc-first help** (F1 = Show Javadoc), in-app search; JavaHelp topics explicitly dropped (doc 25) |
| **Forms / palette** | `java/form`, `form.kit` (Swing visual editor) | dropped for Swing; the design story is **FXML + embedded SceneBuilder** for JavaFX (doc 04) |

## 4. Concrete redesigns to adopt (checked into the owning steps)

1. **One node/view model everywhere** (navigator, structure, usages, breakpoints, test results,
   variables, task list, diff tree): `NodeModel` + `ExplorerContext` + FX views. *Owning steps:* 12/13,
   then every view step consumes it.
2. **One editor analysis pipeline**: parse → hints → refactoring preview → codegen → mark occurrences,
   cancellable and versioned per document; lights up annotations, lightbulb, rename preview, `Find
   Usages` highlighting. *Owning steps:* 18 §1, then 19/20/21.
3. **One property/editor model for sheet, outline columns and customizers** (doc 13), so project
   customizers, JShell/preview forms and the property sheet share editors and validation.
4. **One async job model** for build/run/debug/profiler/VCS/test operations (task tree, cancel, output
   routing to the Output window/terminal). *Owning:* 15 (`nbfx.execution` + progress), consumed by 16/17/23/24.
5. **One search surface**: the command palette (quick search) + find/find-in-projects/find-usages as
   providers over the same index/semantic layer. *Owning:* new `feature/quicksearch` + 18 §2.
6. **Notification/status surface** unified (dialogs, toasts, notification line, "show again", exception
   reporting) so features never invent their own alerts. *Owning:* 09, consumed by 16/17/23/24.
7. **Data compatibility preserved, presentation replaced**: `.nps`/`.hprof`/`.jfr`, breakpoints, keymap,
   project properties, platform storage; the FX layer reads/writes the same data (docs 16/17/24).
8. **Accessibility & high-DPI as a requirement**, not a later task: roles/keyboard for the outline,
   property sheet, editor, diff, terminal; document it in the step acceptance criteria.

## 5. Explicitly dropped (so the drag is not copied)

JavaHelp topics and `JComponent` context help; the Swing form editor/palette (`java/form`); the legacy
`core.windows` window system (replaced by Fx docking); WebStart/JNLP execution (options round-trip only);
`openide.explorer`/`spi.viewmodel`/`spi.debugger.ui`/`spi.editor.hints` Swing UIs; the `progress.ui`,
`notifications`, `uihandler*` Swing modules (excluded/pruned).

## 6. Advancement backlog (idea → owning step)

| Idea | Owner |
|---|---|
| Global command palette (actions/files/types/symbols/options/recent, fuzzy, preview) | `feature/quicksearch` (new) + 18 §2 |
| Unified node/view model consumed by all trees/tables | 12/13, then 16/23/24/25 |
| Property editors shared by sheet, outline and customizers; multi-select aggregation | 13 |
| Visibility into settings (preferences browser) as a shipped debug aid | 14 §B |
| Async analysis pipeline + one fix/annotation pipeline | 18 §1, 20 |
| Output window with SGR colours/links/folds + real PTY terminal | 15 |
| Live structured output tied to the task model (build/debug/profiler/VCS) | 15 (+ 06) |
| One job/task model with cancel/retry used by every long operation | 15 (+ 06) |
| Flame graphs, timeline charts, streaming profiler views | 17 |
| FX diff with inline/history/blame + patch queue | 24 |
| Unified test results + coverage overlay | 23 |
| Multi-window/detached views, per-project layouts | 12 (+ shell) |
| Javadoc-first help, in-app doc search | 22/25 |
| Accessibility & high-DPI pass per view | every UI step |
| Remote terminals/debug/profiler over SSH | 15/16/17 later steps |

## 7. How to read this with the rest of the plan

- [26-roadmap.md](26-roadmap.md) places each block in a layer and a milestone.
- [27-layer-module-map.md](27-layer-module-map.md) says which module owns it.
- This doc says **how the FX implementation should differ from the Swing original**, and points every
  advance at the step that must implement it — so the improvements land as part of the work, not as an
  afterthought.
