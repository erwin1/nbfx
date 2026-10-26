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
# 06 — Progress UI parity

nbfx's progress UI is a single status slot (`FxProgress.start(message, onCancel)` / `finish`, rendered
by `ProgressStatusElement`; plus `BuildProgress`). That is far from the NetBeans default. Bring it to
parity, modeled on the headless `platform/api.progress` **Controller** so that *all* NetBeans progress
usage surfaces in the nbfx FX UI.

## Original NetBeans architecture (evidence)

- **Headless core** (`platform/api.progress`):
  - public `ProgressHandle`: `start`/`start(workunits)`/`progress`/`progress(msg)`/
    `switchToIndeterminate`/`switchToDeterminate`/`suspend(msg)`/`setDisplayName`/`setInitialDelay`/
    `addDefaultAction`/`finish`/`close`; `createHandle`/`createSystemHandle`; `ProgressRunnable`,
    `BaseProgressUtils`, `aggregate`.
  - SPI `org.netbeans.modules.progress.spi`: `Controller`, `InternalHandle`, `ProgressEvent`,
    `TaskModel`, `ProgressUIWorker`, `ProgressUIWorkerWithModel`, `RunOffEDTProvider`.
- **NB API** (`platform/api.progress.nb`): `ProgressHandleFactory` (incl. Swing
  `createProgressComponent`/`createMainLabelComponent`/`createDetailLabelComponent`), `ProgressUtils`,
  `AggregateProgressFactory`; UI bridge SPI **`ProgressUIWorkerProvider`**, `ExtractedProgressUIWorker`,
  `SwingController`, `UIInternalHandle`.
- **Swing UI** (`platform/progress.ui`): `ProgressUI`/`ProviderImpl` register the worker;
  `StatusLineComponent` (status-line aggregate), `ListComponent` + `PopupPane` (the **all-tasks popup**),
  `NbProgressBar`, `ProgressListAction` ("View → Progress"), `AbstractWindowRunner`/`RunOffEDTImpl`
  (off-EDT updates), `CancelAction`, `ProgressVisualizerProvider`.

## nbfx today (gaps)

- `FxProgress` is one slot (`start(message, onCancel)` / `finish`); `FxProgressImpl`,
  `ProgressStatusElement`, `BuildProgress`. Missing: multiple concurrent tasks, a **tasks list/popup**,
  **per-task details**, **cancel-per-task / pause**, aggregate bar, elapsed time, "View → Progress".
- The Swing `progress.ui` still ships in `nb.cluster.platform` (inert on the FX status bar).

## Reuse assessment (what we reuse vs reimplement)

| Module / type | Reuse? | Notes |
|---|---|---|
| `org.netbeans.api.progress.ProgressHandle` (+ `ProgressRunnable`, `BaseProgressUtils`, `aggregate`) | **Reuse** | Public headless task API; nbfx keeps calling it. |
| `api.progress.nb` `ProgressHandleFactory`, `ProgressUtils`, `AggregateProgressFactory` | **Reuse** | Also has Swing `createProgressComponent`/label helpers — unused by nbfx. |
| SPI `org.netbeans.modules.progress.spi`: `Controller`, `InternalHandle`, `ProgressEvent`, `TaskModel`, `ProgressUIWorker`, `ProgressUIWorkerWithModel`, `ProgressUIWorkerProvider` | **Reuse** | `Controller`/`TaskModel` do the task bookkeeping, event queue, timer quantum and initial delay; the UI is a **pluggable worker**. |
| `api.progress.nb` `ExtractedProgressUIWorker` (`JComponent`/`JLabel`) | **Not used** | Swing component embedding; nbfx returns `null` / does not support it. |
| `platform/progress.ui` (`ProviderImpl`, `StatusLineComponent`, `ListComponent`, `PopupPane`, `NbProgressBar`, `ProgressListAction`) | **Replace (behaviour reference only)** | Swing widgets — cannot be reused. **Exclude** the module; reimplement the worker in FX. |
| `RunOffEDTProvider` (`showProgressDialogAndRun` Swing dialog) | **Replace** | nbfx provides its own off-FX-thread runner. |

Accepted caveat: `api.progress`'s `TaskModel` internally uses `javax.swing.DefaultListModel` — a Swing
*data* model, not UI (the accepted transitive Swing layer). The nbfx module implements only the headless
SPI interfaces and never imports Swing, so the guardrail is unaffected.

**Conclusion:** reuse the **API + `Controller`/SPI** (`api.progress` + `api.progress.nb`) and
reimplement *only* the **UI worker** (`ProgressUIWorkerProvider` + `ProgressUIWorkerWithModel`) in FX —
a small new `nbfx.progress` module (one provider + status element + tasks popup). No fork of the
original Swing UI is needed.

## Target design

- Provide an **FX `ProgressUIWorkerProvider`** (implements `ProgressUIWorkerWithModel` +
  `ExtractedProgressUIWorker`) in a new **`nbfx.progress`** UI module; keep the headless `Controller`
  driving it, so every module using `ProgressHandle`/`ProgressHandleFactory` (parsing, indexing, maven,
  autoupdate, builds, …) shows up in the nbfx UI.
- **Exclude the Swing `progress.ui`** from the nbfx clusters (as `core.windows` was), so only the FX
  worker is registered.
- UI surfaces to reach parity:
  - **status-line aggregate**: determinate/indeterminate bar + the active task's display name/detail;
    tooltip shows the running-task count; click opens the tasks popup.
  - **tasks popup**: lists **all running tasks**, each with display name, detail message, progress bar,
    elapsed time and status; selecting shows **details**; context menu with **Cancel** (with
    confirmation), **View** (default action → output/link), and system-task entries.
  - **details for the running task**: detail message, secondary progress (N of M), elapsed time, default
    action.
  - **"View → Progress"** action to reopen the list; auto-show after the handle's initial delay,
    auto-hide when done.
  - **system/background handles** displayed distinctly; **silent/aggregated** tasks handled.
  - **off-FX-thread, coalesced** updates — the FX analogue of `RunOffEDTImpl`.
- API compatible with `org.netbeans.api.progress.ProgressHandle`/`ProgressHandleFactory`, so reused
  NetBeans code and nbfx-native callers share one model. The current `FxProgress` becomes a thin
  convenience wrapper (or is retired in favour of `ProgressHandle`).
- Wire `nbfx.run` builds (Ant/Maven/Gradle) and long operations to real `ProgressHandle`s
  (workunits/detail/cancel) instead of the single slot.

## Dependencies

`platform/api.progress` (headless) and `api.progress.nb` (worker SPI). Direct deps stay headless; the
Swing `progress.ui` is **excluded**, not depended on.

## Landed (implementation notes)

Implemented on the local branches (exact ids in [`11-project-state.md`](11-project-state.md)):

- `nbfx.progress` (`nbfx-platform`): `FxProgressEnvironment` (`ProgressEnvironment`),
  `FxProgressController` (headless `Controller`), `FxInternalHandle`, `FxProgressWorker`
  (`ProgressUIWorkerWithModel`) with a status-line element (aggregate bar, active label, task count,
  cancel) and an all-tasks popup (View/Cancel, system tasks marked); View → Progress command; coalesced
  off-FX updates; worker tests 3/3.
- The Swing `progress.ui` is excluded from the generated cluster **and pruned from the dist** — a stale
  jar otherwise keeps winning the `ProgressEnvironment` lookup, so nothing renders (the original symptom
  behind “project loading not visualised”).
- `FxProgress` became a thin `ProgressHandle` facade and was then removed: project loads (`ProjectLoads`)
  and builds (`BuildRunner`) create real `ProgressHandle`s; the launcher no longer has progress code and
  its indexer scan watcher is gone (the indexing API reports through `ProgressHandle`).
- `project`, `caret` and `line-separator` status elements were moved to `@FxStatusRegistration`, so the
  bar is fully layer-driven.
- Remaining: the manual matrix and the progress-related polish (pause/suspend, View details) in
  [`07-small-steps.md`](07-small-steps.md).
