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

| Tier | Who consumes it | Public packages | Governance | Change policy |
|---|---|---|---|---|
| **T0 — implementation** | only its own module (and friends, avoided) | **none** (`<public-packages/>` empty) | none | free to change |
| **T1 — shared API** | other nbfx layers | explicit list | `arch.xml` `<api type="export">`, `apichanges`, `@since`, signature test | additive preferred; spec minor/major; no third-party promise |
| **T2 — published plugin API** | third-party plugins | explicit list | full (T1 + compatibility contract, docs/tutorial, deprecation policy, plugin-version range, compatibility report in CI) | strict: additive only within a major; deprecate → remove after a major |

**Module assignment.** The authoritative per-module assignment is the inventory table in
[`30-api-governance.md`](30-api-governance.md) §1.1 (every existing module: exported packages → tier →
action). Summary of the target T2 set:

- **T2 now** (plugin-facing, implemented or being moved): the registration annotations and the SPIs a
  plugin needs today — `nbfx.api.actions`, `nbfx.api.view`, `nbfx.api.status`, `nbfx.api.options`,
  `nbfx.api.settings`, `nbfx.api.wizard`, `nbfx.api.property`, `nbfx.api.project`, `nbfx.nodes`
  (model + `PropertyModel`), `nbfx.project.ui.api` (build/run actions), `nbfx.project.customizer`,
  `nbfx.run`, and the public annotations module(s).
- **T2-proposed** (do **not** promise until implemented + signature-tested + documented): the debugger,
  profiler, test-runner and VCS backends (`DebuggerBackend`, `ProfileBackend`, `TestRunner`,
  `VcsProvider`). They are listed as *proposed* in the plugin contract; promotion is a deliberate step
  after the corresponding feature lands (docs 16/17/23/24).
- **T1**: shared between nbfx layers, not promised externally (e.g. `nbfx.templates`, `nbfx.structure`,
  `nbfx.project.context`, `nbfx.settings` implementation).
- **T0**: everything else — implementations, UIs, engines.

**T2 annotations live in annotation-only API modules.** A plugin must be able to write
`@OptionsRegistration`/`@WizardRegistration`/`@PropertyEditorRegistration`/`@ProjectTypeRegistration`
**without depending on the corresponding UI module**. Therefore (doc 29): the T2 annotations + processors
live in `nbfx.api.<capability>` annotation modules (no UI, no implementation); the UI module depends on
them; the plugin depends on them. Placing a T2 annotation inside the UI module is forbidden.

**The plugin dependency manifest.** A plugin depends on exactly: the annotation modules it uses, the API
modules of the extension points it implements, and the platform core (`org.openide.util`,
`org.openide.filesystems`, `org.openide.util.lookup`). This set is documented in the plugin guide with
version ranges; a plugin never depends on a T0 module.

**Rule.** A module starts at **T0**. Promotion to T1/T2 is a deliberate change with the artefacts above —
never a default. Demotion (T2 → T0) is a breaking change and needs a deprecation cycle. **Tier assignment
comes before every package move or annotation split.**

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

### 5.3 Current code vs target

Docs 12/13/14/15/16/29 contain `Fx*` names in two senses: **current code** (landed classes, e.g.
`FxProperty`, `FxOutput`, `FxOptionsPanel`) and **proposed designs**. From this doc on, the *proposed*
names are the capability names; `Fx*` mentions are marked as current/legacy. The rename step below fixes
the code.

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
