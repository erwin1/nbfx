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
# 32 — API tiers, the plugin contract and naming

The second review's core objection: nbfx treated **every** module as a contract (38 modules with public
packages, 21 exporting whole subtrees) while never defining the one contract that has external consumers —
the **plugin API**. This doc fixes the size of the API: tiers, the plugin contract and trust, naming
conventions, and the rename/split ordering.

Governance mechanics (arch/apichanges/signature tests/versioning) are in
[30-api-governance.md](30-api-governance.md); the toolkit-neutral SPI/factory design is
[31-front-end-apis.md](31-front-end-apis.md).

## 1. Why tiers

nbfx is an **application** with an optional plugin ecosystem, not a platform with third-party API
consumers everywhere. NetBeans needed per-package API governance because the whole IDE was an SDK; nbfx
needs it only where *someone else's code* compiles against ours:

- **plugins** (third-party NBMs) — the real external promise;
- **nbfx's own layers** (`nbfx-ide`/`nbfx-java`/`nbfx-javafx`) — an internal promise between clusters;
- **feature implementations** — no promise at all.

Imposing `apichanges.xml` on 38 modules guarantees abandonment by M1. The tiers make the promise
proportional.

## 2. The tiers

| Tier | Who consumes it | Toolkit | Governance | Change policy |
|---|---|---|---|---|
| **T0 — implementation** | only its own module | any | none (`<public-packages/>` empty) | free to change |
| **T1 — shared API** | other nbfx layers | per package, documented | `arch.xml` `<api>`, `apichanges`, `@since`, signature test | additive preferred; spec minor/major; no third-party promise |
| **T2-FX — published JavaFX plugin API** | third-party plugins | **JavaFX is allowed** (`Node`, `ObservableValue`, `KeyCombination`) | full T1 + compatibility contract, docs/tutorial, deprecation, plugin version range, signature diff | strict: additive within a major; deprecate → remove after a major |
| **M — model/backend API** | nbfx layers and plugins | **toolkit-free** (`java.beans`/`Lookup`; no `javafx.*`, no Swing/AWT) | full T1; published where a plugin implements a backend | additive; no toolkit types |
| **L — external library** | nbfx | bundled library | licence/notice + allowlist | pinned; never exposed as nbfx API |

**The API is JavaFX.** The audit shows **28 files** in `nbfx.api` importing `javafx.*`: `Command` returns
`Node`/`KeyCombination`/`ReadOnlyBooleanProperty`, `ViewProvider` returns `Node`/`ObservableValue`,
`EditorSettings` returns `BooleanProperty`, and menu models use `MenuItem`. That is correct for a JavaFX
plugin API — the **naming** rule ("no toolkit in a name") still applies, but the **signature** rule is
per-tier: T2-FX may expose FX types; M must not.

**Node ownership contract (T2-FX).** Any API returning a `Node` must document ownership: a `Node` has one
parent, so `Command.getIcon()`/`ViewProvider.getIcon()` return a **new** node per call, and
`ViewProvider.getView()` hands ownership of the returned node to the shell. A plugin must not reuse the same
node in two surfaces; the shell must not reparent a node it did not create. Menu items are FX controls
(`MenuItem`), not a toolkit-neutral model.

**Module assignment.** The authoritative table is [`30`](30-api-governance.md) §1.1. Summary:

- **T2-FX**: registration annotations (in annotation-only modules) and the component/plugin SPIs:
  `nbfx.api.actions`, `.view`, `.status`, `.editor`, `.options`, `.wizard`, `.property`, `.file`, `.icons`,
  `.elements`, `.progress`, `.project`; `nbfx.nodes` (node model); `nbfx.project.customizer`;
  `nbfx.api.project` (project types).
- **M (toolkit-free)**: `nbfx.settings`, `nbfx.nodes` model types where applicable, the debug/profiler/
  test/VCS **backend** SPIs (T2-*proposed* until implemented + signature-tested): `DebuggerBackend`,
  `ProfileBackend`, `TestRunner`, `VcsProvider`.
- **T1**: `nbfx.templates`, `nbfx.structure`, `nbfx.project.context`, `nbfx.output` service,
  `nbfx.project.ui.api` (build/run model, split from its UI part), `nbfx.run`.
- **T0**: implementations/UIs/engines.
- **T2 annotations live in annotation-only API modules** (`nbfx.api.options`, `.wizard`, `.property`,
  `.project`, …), so a plugin compiles against them **without any UI module** (doc 29). `nbfx.api.*`
  modules themselves are T2-FX; the annotation modules are FX-free.

**The plugin dependency manifest.** A plugin depends on exactly: the annotation modules it uses, the T2-FX
API modules of the extension points it implements, the M modules of its backend, and the platform core
(`org.openide.util`, `org.openide.filesystems`, `org.openide.util.lookup`). It never depends on a T0 module.

**Rule.** A module starts at **T0**. Promotion to T1/T2-FX/M is deliberate. **Tier assignment comes before
every package move or annotation split.** Demotion is a breaking change with a deprecation cycle.

## 3. The plugin contract

A plugin (NBM) is the only external promise. Define precisely what it may rely on:

| Extension point | Plugin declares | Guarantee |
|---|---|---|
| Views | `@ViewRegistration` + `ViewProvider` (`Lookup`) | the registration and the provider contract are T2-stable; the component is obtained through the front-end factory (Doc 31) |
| Actions/menus/toolbars/status | `@ActionRegistration`/`@MenuRegistration`/`@ToolbarRegistration`/`@StatusRegistration` + `Command` | ids are stable; layers of the host shell are not |
| Options | `@OptionsRegistration`/`Container`/`Sub` + `OptionsPanelController` | panel controller contract + category metadata stable; the window is not |
| Settings | `Settings` (typed keys) | key naming + typed helpers stable; the storage backend is not |
| Nodes/property sheet | `NodeModel`/`ChildrenModel`/`PropertyModel`/`PropertyEditor` | model contracts stable; the FX views are not |
| Project types | `@ProjectTypeRegistration` + `ProjectModel`/`ProjectProvider` | registration + model stable (this is how a plugin adds a project type) |
| Wizards/templates | `@WizardRegistration`/`@TemplateRegistration` + `WizardIterator`/`WizardPanel` | registration + page contract stable; the dialog is not |
| Run/build | `RunConfiguration`, `BuildActionProvider` (Lookup) | **T2 now** — model stable; execution implementation not |
| Debug | `DebuggerBackend`/`DebuggerSession` (Lookup) | **T2-proposed** (doc 16) — do not promise until designed/tested |
| Profiling | `ProfileBackend`/`ProfileSource` (Lookup) | **T2-proposed** (doc 17) |
| Testing | `TestRunner`/`TestReportParser` (Lookup) | **T2-proposed** (doc 23) |
| VCS | `VcsProvider`/`VcsHistoryProvider` (Lookup) | **T2-proposed** (doc 24) |

**What a plugin may *not* rely on** (documented in the plugin guide): internal modules/packages (T0), layer
paths and `.instance` file names, `*Impl` classes, FX/Swing classes used by the host, the order in which
services load, the exact view layout, the theme CSS class names (unless exported as a token API).

**Compatibility rule.** Within a T2 major version: additions only; removals/deprecations follow the
policy in doc 30 §2. Every T2 module publishes its **plugin version range** (`OpenIDE-Module-Requires`
spec versions) and a compatibility report in CI (signature diff against the previous release).

## 4. Trust, signing and update centers

The plugin story is not just API; it is trust:

1. **NBM signing/verification** — NBMs are JARs; the plugin manager verifies **JAR (PKCS#7) signatures**
   against a trust anchor (the system trust store **or** a pinned publisher key), shows the
   publisher/unsigned state, and applies a policy (block/warn/allow). Note: upstream autoupdate does
   **not** verify by default today — this is a **proposed** nbfx behaviour until implemented, and it must
   not be advertised as shipped before then.
2. **Update centers over HTTPS only** by default; user-added centers must be HTTPS or explicitly
   confirmed; credentials via `keyring` (doc 30 §9).
3. **Native artifacts** (async-profiler `.so`/`.dylib`) are verified by SHA-256 pinned in the module
   (`binaries-list`) *and* fetched over HTTPS; never from a mutable URL.
4. **Plugin capabilities** — declare what a plugin may do (filesystem, network, process execution) and
   surface it at install time (advisory; the JVM has no sandbox, so it is a trust decision, not
   enforcement).
5. **`nbfx.plugins` UX** — install/update/remove, update centers, proxy, signatures, restart, and an
   "installed by/verified" column (doc 25's plugins-UI step).

## 5. Naming conventions

### 5.1 Modules and clusters

| Kind | Convention | Examples |
|---|---|---|
| Cluster | `nbfx-<layer>` | `nbfx-platform`, `nbfx-ide`, `nbfx-java`, `nbfx-javafx` |
| T0 implementation | `com.gluonhq.netbeans.nbfx.<capability>` | `…nbfx.editor`, `…nbfx.debug` |
| T1/T2 API | `com.gluonhq.netbeans.nbfx.api.<capability>` | `…nbfx.api.actions`, `…nbfx.api.view`, `…nbfx.api.options` |
| Toolkit implementation | `…<capability>.ui.fx` (module *may* carry the toolkit) | `…nbfx.options.ui.fx` |
| External library | `com.gluonhq.netbeans.nbfx.libs.<lib>` | `…nbfx.libs.jline` |

Grandfathering: existing code-names stay until the module is touched by `refactor/api-split`; the register
in doc 30 §1 records the target. No new module without this scheme.

### 5.2 Types, annotations, constants

| Kind | Rule | Wrong → Right |
|---|---|---|
| SPI/API types | capability noun, toolkit-free | `FxOptionsPanel` → `OptionsPanelController`; `FxWizardIterator` → `WizardIterator`; `FxPropertyEditor` → `PropertyEditor`; `FxViewRegistration` → `ViewRegistration`; `FxDialogDescriptor` → `DialogDescriptor`; `FxProject` → `ProjectModel` |
| Annotations | capability noun | `@FxOptionsRegistration` → `@OptionsRegistration`; `@FxWizardRegistration` → `@WizardRegistration`; `@FxViewRegistration` → `@ViewRegistration`; `@FxPropertyEditorRegistration` → `@PropertyEditorRegistration`; `@FxProjectTypeRegistration` → `@ProjectTypeRegistration`; `@FxProjectNodeFactory` → `@ProjectNodeFactory` (and make it a real, documented SPI) |
| Implementation classes | toolkit prefix **only** in `…ui.fx`, never public | `FxOptionsWindow` may exist in `…options.ui.fx`; not in an API package |
| Layer paths / constants | private; no public path strings | `FxLayer.OPTIONS` → `private static final String OPTIONS = …` in the processor/reader |
| Services | capability nouns | `FxOptionsDisplayer` → `OptionsDisplayer`; `FxOutput`/`FxConsole` → `OutputConsole`/`InputOutputProvider` (new code); `FxProgress` → `ProgressHandle` |
| IDs | identifiers in one vocabulary with `@since` | `ActionIds.*`, `TemplateIds.*`; no ad-hoc strings |

### 5.3 The 53 public `Fx*` types are four different problems

The audit (excluding test classes) yields **53 public production types** — not "57 to rename":

| Category | Count | Examples | Action |
|---|---|---|---|
| **Registration annotations** | 15 | `@FxActionRegistration`, `@FxViewRegistration`, `@FxOptionsRegistration`, `@FxProjectTypeRegistration`, `@FxWizardRegistration` | rename to `@ActionRegistration`/… in the **T2 annotation-only modules** (doc 29) |
| **Processors** | 9 | `FxOptionsRegistrationProcessor`, `FxViewRegistrationProcessor`, … | follow their annotation (package-private where possible) |
| **SPI types** | 16 | `FxOptionsPanel`, `FxProperty`/`FxPropertySet`/`FxPropertyEditor`, `FxPropertiesProvider`, `FxWizard`/`FxWizardPanel`, `FxOutput`/`FxConsole`, `FxActionContext`, `FxStatusElement`, `FxProjectCustomizerPanel`, `FxProgress` | rename to capability names (`OptionsPanelController`, `PropertyModel`, `PropertyEditor`, `WizardIterator`, `OutputConsole`, `ActionContext`, `StatusElement`, `ProjectCustomizerPanel`) and move to `nbfx.api.*` |
| **Implementation widgets** | 12 | `FxOptionsDialog`, `FxPropertySheet`, `FxStatusBar`, `FxWizardDialog`, `FxProjectCustomizerDialog`, `FxOutputImpl`, `FxProgressImpl`, `FxActionContextImpl`, `FxApplicationMenu`, `FxGuiRunLevel`, `FxStructurePanel`, `FxWindowSystem` | **move to `…ui.fx`** and/or privatise; they are not API |
| **Constants** | 1 | `FxLayer` | path constants become **private** per reader (doc 30 §1.5) |
| **Tests** (excluded above) | 3+ | `FxAnnotationsProcessorTest`, `FxAreaTest`, `FxOutputTest` | rename with their subject; never part of the API |

Docs 12/13/14/15/16/29 use `Fx*` in the **current-code** sense unless marked *target*; the target names in
this plan are **to create**. The authoritative mapping (current class → target, or "new") is the table
above plus doc 31 §3; a doc must not imply that a target module (`nbfx.api.options`, `nbfx.nodes`,
`PropertyModel`, …) already exists.

## 6. Ordering: rename → split → freeze

1. **Freeze now**: no new public `Fx*` type (SPI, annotation, constant). Existing in-flight work (including
   the progress PR) is marked "rename required before merge/follow-up".
2. **Rename** (`refactor/api-naming`, doc 31 mapping): capability names in the public packages; FX classes
   move to `…ui.fx`; layer output must stay identical; signature baselines regenerated; `apichanges`
   entries per affected T1/T2 module.
3. **Split** (`refactor/annotations-split`, doc 29) *after* the rename — otherwise the moved annotations
   carry `Fx*` names into four modules and are renamed again. Same for the `nbfx.api` capability split:
   rename first, then split.
4. **Tier assignment** (`refactor/api-split`): T0 modules get `<public-packages/>` empty; T1/T2 get the
   governance artefacts; the public-package inventory records the tier.

## 7. Acceptance

- No public `Fx*`/`Sw*` type, annotation or constant in any module (structural naming check).
- T0 modules export nothing; T1/T2 modules have `arch.xml` + `apichanges` + signature tests.
- The plugin contract is documented as a guide (what a plugin may use/not use, version range, trust).
- The rename is ordered before the annotation/API splits; layer output invariant.
- The plugin manager verifies signatures and restricts centers to HTTPS (policy).
