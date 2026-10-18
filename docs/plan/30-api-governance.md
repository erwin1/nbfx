# 30 — API governance, versioning and upstream strategy

This doc responds to the "Jaroslav review": nbfx publishes an API in 38 modules where **everything is
public by accident**, has only 10 modules with `arch.xml`/`apichanges.xml`, 57 public `Fx*` types, three
`friend` patches, mutable statics as models, stringly-typed contracts and grep-based guardrails. An API is
a promise; the plan must treat it as one.

Steps introduced here: `refactor/api-split`, `refactor/api-naming`, `refactor/services-not-statics`,
`refactor/guardrail-structural-checks`; registers maintained continuously; the walking skeleton
(`feature/walking-skeleton`) and budgets in [26-roadmap.md](26-roadmap.md)/[07-small-steps.md](07-small-steps.md).
The front-end SPI/naming architecture is [31-front-end-apis.md](31-front-end-apis.md).

## 1. Public-package inventory, and splitting the kitchen sink

**Measured facts (2026-10-18):** 38 nbfx modules declare a `<public-packages>` element; **25 export at least
one package** and **13 declare it empty**; of the 25, **21 export their whole package subtree**
(`<subpackages>`); only **10 modules** carry `arch.xml` + `apichanges.xml`. Known violations to burn down:
**57 public `Fx*` types** (doc 32 §5).

**Rules.**

1. A module declares `<public-packages>` **only** for packages that are contracts, and lists packages
   **explicitly** (no `<subpackages>` for a whole tree; the tree form is allowed only with a written
   justification in the module's `arch.xml`).
2. **Tier first.** Every module gets a tier from the table below before its packages are touched
   ([`32`](32-api-tiers-and-plugin-contract.md) §2); T0 exports nothing, T1/T2 carry the governance
   artefacts of §2.
3. `com.gluonhq.netbeans.nbfx.api` (which today exports `api.actions`, `api.editor`, `api.elements`,
   `api.icons`, `api.progress`, `api.project`, `api.status`, `api.view`, …) is split **per capability**
   into `com.gluonhq.netbeans.nbfx.api.<capability>` modules (doc 32 §5.1), plus `nbfx.nodes` (model),
   and implementations move to `*.impl`/UI modules. Each split module gets its own specification version.
4. Anything not intended as a contract becomes package-private or stays in a non-public package; a
   `<friend>` dependency (see §5) is never a substitute for a contract.
5. The table below **is** the tracked inventory (module → exported packages → tier → owner → action); the
   `refactor/api-split` step updates it and the governance guard (§6) fails the build when a T1/T2 public
   package has no `arch.xml` entry or a T0 module exports a package.

### 1.1 Inventory and tier assignment

`sub` = whole package subtree exported (must be listed explicitly or justified); `—` = no export.

| Module | Exported package(s) | Tier (target) | Action |
|---|---|---|---|
| `nbfx.annotations` | `sub …annotations` | **T2** | split the registration annotations into `nbfx.api.*` annotation modules (doc 29/32); core keeps shell annotations |
| `nbfx.api` | `sub …api` | **T2** | split into `nbfx.api.{actions,view,status,editor,progress,project,…}` |
| `nbfx.docking` | `…docking` | T0 | no plugin needs it; privatise (or T1 if the shell SPI moves) |
| `nbfx.menus` | `sub …menus` | T0 | implementation; registration via annotations |
| `nbfx.options` | `sub …options` | **T2** | move the SPI/annotations to `nbfx.api.options`; keep the window in `nbfx.options.ui.fx` |
| `nbfx.output` | `sub …output` | T0 | `api.io` is the public contract; privatise |
| `nbfx.statusbar` | `sub …statusbar` | T0 | `FxStatusBar` is an implementation type; move the status SPI to `nbfx.api.status` |
| `nbfx.windows` | `sub …windows` | T0 | shell implementation |
| `nbfx.wizard` | `sub …wizard` | **T2** | move the SPI/annotation to `nbfx.api.wizard`; keep the dialog in `nbfx.wizard.ui.fx` |
| `nbfx.debug` | `sub …debug` | T0 | engine/UI internals; the backend SPI is T2-*proposed* (doc 32 §3) |
| `nbfx.file.actions` | `…file.actions` | T0 | implementation |
| `nbfx.launcher` | `sub …launcher` | T0 | application entry point |
| `nbfx.plugins` | `sub …plugins` | T0 | the plugin *manager* is a UI; the plugin **contract** is T2 (doc 32) |
| `nbfx.project` | `sub …project` | **T2** | the project model (`ProjectModel`) is a plugin extension point |
| `nbfx.project.context` | `sub …project.context` | T1 | shared between nbfx layers; no external promise |
| `nbfx.project.customizer` | `sub …project.customizer` | **T2** | project-type customizer SPI (plugins add project types) |
| `nbfx.project.ui` | `sub …project.ui` | T0 | New File/Project UI; templates are the plugin surface |
| `nbfx.project.ui.api` | `sub …project.ui.api` | **T2** | build/run action SPI used by project types |
| `nbfx.propertysheet` | `sub …propertysheet` | **T2** | move `PropertyModel`/`PropertyEditor` to `nbfx.api.property`; keep the sheet in the UI module |
| `nbfx.run` | `sub …run` | **T2** | run-configuration SPI used by project types |
| `nbfx.structure` | `sub …structure` | T1 | language-provider SPI between layers |
| `nbfx.ui` | `sub …ui` | T0 | shell/UI glue |
| `nbfx.vcs` | `sub …vcs` | **T2-proposed** | VCS SPI once designed/tested (doc 32 §3) |
| `nbfx.project.ui.java` | `sub …project.ui.java` | T0 | Java usage implementation |
| `nbfx.lib.javafx` | `sub javafx`, `sub com.sun.*`, `sub jfx.*` | **L** | external library (not an API; keep the licence/allowlist) |

Modules with an empty `<public-packages/>` (T0 today): `nbfx.actions`, `nbfx.context`, `nbfx.editor`,
`nbfx.editor.actions`, `nbfx.findinprojects`, `nbfx.findusages`, `nbfx.macos`, `nbfx.project.actions`,
`nbfx.build.ant`, `nbfx.project.ui.ant`, `nbfx.project.ui.gradle`, `nbfx.project.ui.maven`,
`nbfx.structure.java` — keep them empty.

Owner for every nbfx module: **Sven** (single-maintainer project at this point); the owner is recorded so
that the register is never anonymous. Re-assign if the team grows.

## 2. Per-package governance artefacts

**Scope: only T1 (`shared API`) and T2 (`published plugin API`) packages** — see the tier model in
[`32-api-tiers-and-plugin-contract.md`](32-api-tiers-and-plugin-contract.md) §2. T0 (implementation)
modules export **nothing** and need none of this.

For **every** T1/T2 public package and module:

| Artefact | Rule |
|---|---|
| `arch.xml` | one `<api name="…" group="java|layer" type="export|import|devel" category="official|devel">` per public package, with its javadoc URL; `devel` while a milestone is open |
| `apichanges.xml` | one `<change>` entry per API edit: `id`, `api`, `summary`, `version major/minor`, `date`, `author`, `compatibility addition/change/removal` + `semantic`, `description`, `<class>` list |
| `@since` | on every new public type/member (tag = the module specification version it appeared in), never copied around blindly |
| Specification version | `<run-dependency><specification-version>` in dependants must be bumped when the API changes; the module's `OpenIDE-Module-Specification-Version` follows a documented policy (minor for additions, major for incompatible) |
| Signature test | a unit test that records the public signature (types, methods, exceptions) and fails on unintended change; generated/maintained per API module |
| Javadoc | every public type/member documented; package `package.html`; lint warning-free |

**Compatibility policy.**

- API = the public packages of a module with a specification version. Changing behaviour is a change.
- Adding a method to an interface implemented by third parties is **incompatible** unless it has a
  `default` implementation and the semantic default is safe; document it in `apichanges` either way.
- Removal requires a deprecation across at least one milestone, an `apichanges` entry and a spec-major bump.
- SPIs (implemented by others) and APIs (called by others) are tagged separately in `arch.xml`
  (`type="import"` vs `type="export"`); an SPI change needs a migration note.

**API freeze per milestone:** before M0/M1/M2 close, an API review walks the public packages and either
promotes them (with the artefacts above) or privatises them.

## 3. Naming: capability names, toolkit in the implementation (details in doc 31)

**Facts to fix:** 57 public `Fx*` types; the plan already renamed Nodes to `NodeModel` for this reason, then
kept `FxOptionsPanel`, `FxWizardIterator`, `FxViewRegistration`, `FxProjectTypeRegistration`,
`FxPropertyEditorRegistration`, `FxDialogDescriptor`, … and the options/wizard/view/menu/toolbar/status
**annotations** are `Fx*` too.

**Rule.** A toolkit name never appears in an SPI type name, an annotation name, a layer path constant or a
public field. The toolkit belongs to the **implementation module name** (`nbfx.options.ui.fx`) and to
private classes. Renames land in `refactor/api-naming` (doc 31 §3 has the mapping table).

## 4. Typed contracts (ban stringly-typed APIs)

| Offending contract | Replacement |
|---|---|
| JPDA attach: `Object[] services` containing a `Map` with keys `"name"`, `"sourcepath"`, `"jdksources"`, `"baseDir"` (doc 16 §5.4) | typed services in the session `Lookup` (`SourcePathProvider`, `JdkSources`, `BaseDirectory`, `SessionName`) — the nbfx session builder takes typed parameters and converts to the engine's map at the boundary, documented as an adapter |
| `PropertyModel.getEditorId()` string matched by convention (doc 13) | `PropertyEditorRef` (typed key object; equality by identity/name) resolved through the editor registry; the string form is a private serialisation detail |
| `FxLayer` public folder-path constants (doc 29) | `private static final` in each processor/reader pair; a public constant set of implementation-detail strings is not an API (the generated layer paths are an implementation detail) |
| Action/template/command ids | legitimate identifiers, but the vocabulary lives in one place (`ActionIds`/`TemplateIds`) with `@since` and an `apichanges` entry when extended; no ad-hoc strings in features |
| Events | typed `Lookup.Result`/event objects at API boundaries; `java.beans.PropertyChangeSupport` only inside adapted engines, with a documented UI-boundary adapter (`architecture-rules.md`) |

## 5. Services, not statics

**Facts to fix:** `DebugSession.current` (`private static volatile`), `DebugStatus.ACTIVE`,
`EditorBuffer.OPEN` (static map), `CompletionPopup.cachedFooterWidth` (static mutable int).

**Rules.**

1. Mutable state lives in a **service registered in `Lookup`** with an explicit lifecycle
   (`Closeable`/`ServiceProvider`), so tests inject fakes and multiple instances are possible.
2. Static state is allowed only for **immutable constants** and **bounded caches with a named owner and
   eviction** (e.g. icon caches keyed by resource, with a documented bound); a cache is never the only
   reference to a live model (`EditorBuffer.OPEN` must become an editor-registry service).
3. The current violations are migrated in `refactor/services-not-statics` (one commit per service) and the
   rule is enforced by review + a check that flags `static` non-final fields in public-package classes.

## 6. Guardrails: structural, not grep

**Facts to fix:** `check-sources` uses `<contains text="import org.openide.nodes"/>`; `check-dependencies`
greps `project.xml` strings.

**Restated objective.** (a) no Swing/AWT **rendering** in nbfx; (b) no forbidden module (openide.awt/nodes/
explorer/windows/dialogs, core.windows) on the **runtime dependency path** of the nbfx clusters;
(c) models and API/SPI modules are toolkit-free; (d) every public package is governed (§2).

**Enforcement.**

| Check | Mechanism | Replaces |
|---|---|---|
| Runtime dependency path | parse the built cluster `config/Modules/*.xml` and the module graph; assert forbidden modules are absent — or **exempt** with a live entry (module, why, owner, removal milestone; doc [`33`](33-swing-retirement.md) §3) | `check-dependencies` strings |
| Bytecode purity | `jdeps` (or a constant-pool scan) over the built nbfx jars: no `javafx.*` in model jars, no `javax.swing`/`java.awt` in SPI/model jars, no `org.openide.nodes/explorer/windows/awt/dialogs` anywhere | `check-sources` greps |
| Source style | an import scan (AST or well-formed import regex) as a *style* check only, clearly labelled secondary | `check-sources` as the primary gate |
| API governance | every T1/T2 public package must have an `arch.xml` `<api>` entry and an `apichanges.xml` change when the signature test changes | none today |
| Public-API naming | no public type/annotation/constant in an SPI package matches `Fx*`/`Sw*` toolkit prefixes | none today |

**Use the platform's tooling** where it exists (`sigtest` for signatures, `jdeps` integration,
`verify-classpath`, the module-dependency checks in `nbbuild`) instead of writing new Ant tasks — and
contribute fixes upstream if it falls short (doc 30 §10). The check must be **green on day one**: a
day-one-red guardrail is a guardrail that gets disabled; start from the current exemptions and fail only
on new violations or expired exemptions.

## 7. Startup and laziness budget

Add a measured budget **baselined at the walking skeleton** (targets, reference machine, deltas and
regression policy are in [`34-upstream-sync-and-budget.md`](34-upstream-sync-and-budget.md) §2):

- **startup** (cold start to the main window, and to "usable"),
- **module count** in the nbfx clusters,
- **first-use latency** for: opening the project view, opening a file (first completion), opening Output,
  opening the debugger/profiler views,
- **memory** after startup and after a scripted session.

Mechanism: a benchmark step that records the numbers into `docs/plan/11-project-state.md`; a regression
beyond a documented threshold blocks the milestone. Laziness rules: registries hold cached `Lookup.Result`s
(no `Lookups.forPath` per call — 14 runtime sites today), heavy services are `autoload` and initialise on
first use, views are created lazily by the docking layer.

## 8. Friend register (private upstream access — debt with a deadline)

| Patch | Accesses | Why not API | Upstream proposal | Owner | Deadline |
|---|---|---|---|---|---|
| `patches/0001-friend-core-startup.patch` | `core.startup` internals from `nbfx.launcher` | friend is a private agreement | propose the launcher hook as an API (or replace the access with a documented entry point) | Sven | M1 freeze (2027-03) |
| `patches/0002-friend-maven.patch` | `maven` internals from `nbfx.project.ui.maven` | ditto | propose a Maven project API extension | Sven | M2 (2027-07) |
| `patches/0003-friend-maven-embedder.patch` | `maven.embedder` internals | ditto | propose an embedder API (or move to process-based Maven, doc 15) | Sven | M2 (2027-07) |

Rule: **no new friend patch** without an entry here, an upstream proposal and a deadline; a friend patch
without a proposal is a fork we have not admitted.

## 9. Reuse register (internals we depend on — decide, do not drift)

| Dependency | What we use | Is it API? | Risk | Decision | Milestone |
|---|---|---|---|---|---|
| `org.netbeans.modules.debugger.jpda` / `.projects` | engine, models, source paths (doc 16) | no (`modules.*` internals) | breaks on upstream refactors | propose the needed entry points as API; vendor+patch with a monthly rebase if refused (owner Sven) | M2 |
| `org.netbeans.modules.java.hints`, `spi.java.hints` | `HintsInvoker`, hint implementations (doc 20) | partially SPI | internals | propose `HintsInvoker` as SPI; else vendored fork with a monthly rebase (owner Sven) | M2 |
| `org.netbeans.modules.refactoring.java`, `ide/refactoring.api` | refactoring plugins (doc 19) | SPI + internals | internals | reuse the SPI, upstream the plugin entry points we need | M2 |
| `org.netbeans.lib.profiler` (`heap/*`, `results/*`, `.nps`) | HPROF parser, `.nps` reader (doc 17) | no (library internals + private serialization) | format/impl drift | reuse the heap parser; `.nps` import documented as best-effort; upstream an API or keep vendored | M2 |
| `org.netbeans.modules.extexecution.base` / `.process` | process model (doc 15) | no | internals | prefer an nbfx-native model; reuse only if it proves stable | M0 |
| `org.netbeans.modules.java.testrunner`, `junit`, `testng` | runner logic (doc 23) | no | internals | upstream a test-runner SPI or vendored fork | M1 |
| `org.netbeans.modules.versioning.core` / `.util` | VCS model (doc 24) | partially API | mixed | reuse the API packages only | M1 |
| `org.netbeans.api.project.libraries` (`ide/project.libraries`) | library manager (doc 25) | **yes** | low | depend on the API | M1 |
| `platform/spi.quicksearch` | search SPI | **yes** | low | depend on the API | M0 |
| `platform/keyring*` | credentials | service, impl not API | low | use the service; keep the impl private | M0 |
| `org.jacoco.core` (bundled), jgit (`o.eclipse.jgit*`) | coverage/Git engines (docs 23/24) | libraries (EPL-2.0/EDL) | licence/version | reuse as libraries, pinned, with notices | M1 |
| `jdk.jfr`, `jdk.management.jfr`, `jdk.attach`, `jdk.management` | profiling/attach (docs 16/17) | **JDK API** | module-path access | document `--add-modules`, verify on the target JDK | M2 |

Rule: for every `modules.*`/`lib.*` internal, one of *upstream API*, *vendored fork (version + upgrade
cadence)*, or *reimplement* must be recorded before the milestone closes. "Reuse" is not a decision.

## 10. Upstream strategy

**Cadence, patch budget and CI are defined in [`34-upstream-sync-and-budget.md`](34-upstream-sync-and-budget.md)
§1** (monthly submodule bump, patch register with owners and upstream PR links, patch budget, a weekly
`next-upstream` CI job). This section defines *what* to propose:

1. **Prototype then propose.** Every SPI generalisation (component factory, doc 31; hint invoker; refactoring
   plugin entry; profiler heap API) is prototyped in nbfx and offered to `apache/netbeans` as a PR that
   keeps the Swing implementation working (add the factory + a Swing factory, no behaviour change).
2. **Cadence.** At least one accepted upstream proposal per milestone; the patch set shrinks as proposals
   land; each fork patch references its upstream PR.
3. **Hygiene.** Apache-2.0 headers, upstream-style `@since`/`apichanges` in the proposal, `nbbuild` checks
   green, no friend additions.

## 11. Walking skeleton and reversibility

Before the layer milestones (see [26-roadmap.md](26-roadmap.md) §6), a **walking skeleton** proves the
integration: one project type end-to-end — open → edit → build → run → output/status → commit — with the
app usable at every step. Each step is reversible (feature branch, revertable, no data migration without a
documented downgrade) and the startup/laziness budget (§7) is measured.

## 12. Documentation truth

`docs/plan/` is the **canonical** plan; `docs/MVP.md` is marked historical (one canonical truth). The
README, this doc and doc 31 are the entry points; `11-project-state.md` records the budget numbers.

## 13. Steps and acceptance (added to 07/08)

| Step | Branch | Acceptance |
|---|---|---|
| Walking skeleton | `feature/walking-skeleton` | one project type end-to-end in a usable app; budget recorded |
| Public-package inventory + `nbfx.api` split | `refactor/api-split` | every public package listed explicitly and tiered (T0/T1/T2); `nbfx.*-api` modules per capability; governance guard green |
| Capability naming (rename `Fx*` SPI) | `refactor/api-naming` | no toolkit name in SPI/annotation/constant; implementations in `*.ui.fx`; layer output unchanged |
| Services, not statics + typed contracts | `refactor/services-not-statics` | statics removed (services with lifecycles); JPDA/`PropertyEditorRef`/layer constants typed or private |
| Structural guardrails | `refactor/guardrail-structural-checks` | `jdeps`/module-graph/governance/naming checks replace the greps, green on day one with exemptions, run in verify |
| Front-end component factory (spike + upstream) | `spike/front-end-component-factory` | options SPI served by a generic factory (or model-first); FX+headless factories; upstream PR opened (doc 31) |
| Plugin API + trust | `feature/plugin-api` | the T2 plugin contract is defined, documented and covered by signature tests; signing/HTTPS/native checksums in the plugin manager (doc 32) |
| Swing retirement guard | `refactor/swing-retirement` | the retirement matrix + exemption register (doc 33) is in the build; current-milestone `retire` modules are absent, exemptions live |
| Upstream sync | `chore/upstream-sync` | monthly sync procedure + patch register + CI jobs + patch budget established (doc 34) |

## 14. Definition of done for this doc

- Every public package is **tiered** (doc 32): T0 exports nothing; T1/T2 have the artefacts of §2.
- The **plugin contract** (T2) is defined, documented and signature-tested; trust/signing shipped (doc 32 §4).
- The friend register and reuse register are current, with owners and deadlines; the upstream sync cadence
  and patch budget run (doc 34).
- The guardrails of §6 run in verify, are **green on day one** (with exemptions) and fail on violations.
- The budget of §7 is measured with **numbers** and recorded per milestone (doc 34 §2).
- The **Swing retirement** state matches the matrix (doc 33) at every milestone.
- At least one upstream proposal is open per milestone.
