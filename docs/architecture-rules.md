# nbfx architecture rules

Durable, cross-cutting rules for the nbfx code base. They exist so nbfx can keep reusing NetBeans
infrastructure and, eventually, contribute the reusable parts back.

## Model / UI split: UI-free models with a core-only dependency chain

Keep behaviour that is not a user interface ("model") in **its own module, separate from the JavaFX UI
that presents it, and give the two modules different dependency chains**:

- **Model modules** must not import JavaFX, Swing/AWT or the legacy NetBeans UI
  (`javafx.*`, `javax.swing*`, `java.awt*`, `org.openide.awt`, `org.openide.nodes`,
  `org.openide.explorer`, `org.openide.windows`, `org.openide.dialogs`) and must **depend only on
  NetBeans platform/core APIs** (`org.netbeans.api.*`, `org.netbeans.modules.*` core,
  `org.openide.filesystems`, `org.openide.util`, `org.openide.util.lookup`, …).
- **UI modules** may import JavaFX and depend on the model module (plus the nbfx UI modules).

**Why.** A model module with a core-only dependency chain can later be **moved into the NetBeans core**
and the **Swing front end rebuilt on top of it**, so the pure-JavaFX nbfx front end and the Swing
front end share one implementation instead of keeping two copies in sync.

**Where.** Model modules live in **`nbfx-platform`** (which depends only on `nb.cluster.platform`); the
JavaFX UI that consumes them lives in **`nbfx-ide`** / **`nbfx-java`** / **`nbfx-javafx`**, following the
layer model of `plan/26-roadmap.md` and the module allocation of `plan/27-layer-module-map.md`.

**Enforcement is structural, not textual.** The UI-free/toolkit-free part is checked by a bytecode scan
of the built jars (no `javafx.*`/`javax.swing`/`java.awt` in model/SPI jars) and by the module-graph
check, not by a source grep; imports are a style check only. `ant check-purity` runs the structural checks
in the verify phase (see `plan/30-api-governance.md` §6).

**Prefer reuse.** Before writing a model, check whether the NetBeans core already exposes it headlessly
(for example `org.netbeans.api.templates.FileBuilder` for template creation). Reuse it and keep only
the genuinely new, UI-free parts (for example template *discovery*, which NetBeans ships only inside
its Swing chooser).

**Example.** `com.gluonhq.netbeans.nbfx.templates` is the model (template discovery + creation, no
JavaFX); the New File wizard UI that consumes it lives in the project UI module, mirroring NetBeans'
`ide/projectui` (where the original New File action, wizard and chooser live).

## Observability in models

Model modules must not use `javafx.beans.*` / `javafx.collections.*`: those are JavaFX, so they would
break the core-only chain and the `check-models` guardrail. Model state is observable through **typed
`Lookup`-based mechanisms** — a `Lookup.Result` over `InstanceContent`/`AbstractLookup` carrying typed
event objects (the platform idiom) — or, where an adapted engine forces it, JDK
`java.beans.PropertyChangeSupport` behind a **typed adapter**; a small **UI adapter** wraps the model in
JavaFX `ObservableList` / `ObservableValue` for binding. Do not invent a parallel event mechanism when the
platform already has one; do not expose `java.beans`-style untyped events as a public API.

`org.openide.util.Lookup` is fine in a model: it is part of the NetBeans core, and it **replaces the
legacy `Node.Cookie` extension mechanism** (a node's lookup is the extension point; the old
`Node.getCookie(type)` already delegates to it).

**Reference example (planned).** The nodes/views split: `nbfx.nodes` (model — `NodeModel`,
`ChildrenModel`, `ChildFactory<T>`, `PropertyModel`, `PropertySetModel`, `ExplorerContext`; `java.beans`
events, `Lookup` for content, no JavaFX) and `nbfx.nodes.ui` (JavaFX views over the model, FX observable
adapters, property editors). See [`plan/12-nodes-and-views.md`](plan/12-nodes-and-views.md).

## Settings / preferences data

User-configurable settings live in a **core-only model module** (`nbfx.settings`): a typed facade over
`NbPreferences` with change listeners and defaults, no JavaFX. Headless feature code and the JavaFX
option panels read the **same keys**, so an option change affects the feature without a UI dependency.
The JavaFX binding helper (`SettingsBindings`) and the options window are UI concerns and live in the
UI module (`nbfx.options`).

**Options are UI-only (documented exception).** Unlike nodes/views there is no headless part of the
Options system to split out: its SPI returns a JavaFX `Node`, so `nbfx.options` is a UI module by
design. Only the settings *data* is model-side. See [`plan/14-options.md`](plan/14-options.md).

## Output, execution and terminal

The output/execution/terminal stack follows the same split:

- **Model (core-only, JavaFX-free)**: `nbfx.execution` (process descriptors, output pumps, cancel/exit)
  and `nbfx.terminal.model` (VT/xterm screen, scrollback, attributes, input encoder; `java.beans`-style
  events). Both must pass `check-models`.
- **UI (JavaFX)**: `nbfx.output` (the Output window, implementing
  `org.netbeans.spi.io.InputOutputProvider`) and `nbfx.terminal.ui` (the Canvas renderer + tabs).
- Reuse only Swing-free headless code: `platform/api.io` + `spi.io`, `ide/extexecution.base`/
  `extexecution.process`, and `core.output2`'s ANSI SGR parser (ported). The Swing terminal
  (`ide/terminal.nb`, `lib.terminalemulator`'s `Term`/`Screen`) is not reusable; the emulator core is
  ported piecewise into `nbfx.terminal.model`. PTY via **JLine 3** (BSD-3). See
  [`plan/15-output-and-terminal.md`](plan/15-output-and-terminal.md).

## Debugging

Same split: `nbfx.debug.core` (headless — session/launch/attach, view models, breakpoints, source paths;
JavaFX-free, `check-models`) and `nbfx.debug` (JavaFX views, actions, breakpoint customizers, editor
annotations). Reuse the headless NetBeans core (`org.netbeans.api.debugger`, `org.netbeans.api.debugger.jpda`,
`org.netbeans.modules.debugger.jpda`, `org.netbeans.modules.debugger.jpda.projects`); the Swing UI modules
(`org.netbeans.spi.debugger.ui`, `org.netbeans.spi.viewmodel`, `org.netbeans.modules.debugger.jpda.ui`) are
unusable (forbidden `openide.awt/nodes/windows/explorer` deps) and are ported. See
[`plan/16-debugging.md`](plan/16-debugging.md).

## Profiling

Same split: `nbfx.profiler.core` (headless — session/backends/recording, analysis model, snapshot store,
heap model; JavaFX-free, `check-models`) and `nbfx.profiler.ui` (JavaFX views). The engine is
**agent-free**: JFR (built into the JDK) as the default, **async-profiler** (Apache-2.0) bundled per OS/arch
as `libs.asyncprofiler.*` modules gated by `org.openide.modules.os.*`/`.arch.*`, and a JMX live sampler;
the legacy NetBeans profiler agent is out (class-file cap = JDK 11). Reuse the headless parts of
`org.netbeans.lib.profiler` (the HPROF heap parser fully; the `.nps`/results algorithms selectively).
One analysis model is fed by live JFR streams, `.jfr` (JFR and async-profiler), `.hprof` and `.nps`
recordings. See [`plan/17-profiling.md`](plan/17-profiling.md).

## Java intelligence & tooling

The Java editor-feature wave (navigation, refactoring, hints, code generation, Javadoc, testing, VCS)
follows the same rule with one important boost: the **semantic API is already a dependency** —
`org.netbeans.modules.java.source.base` provides the whole `org.netbeans.api.java.source` surface
(`JavaSource`, `WorkingCopy`, `SourceUtils`, `ElementUtilities`, `ClassIndex`, `GeneratorUtilities`,
`TreeUtilities`/`TreeMaker`, `ModificationResult`, `ElementHandle`/`TreePathHandle`). So:

- **Reuse the headless logic**: `java.source.base` (edits/index/elements), `java/spi.java.hints`
  (`HintsInvoker`) + `java/java.hints` implementations, `ide/refactoring.api` + `java/refactoring.java`
  plugins, `java/testrunner`/`junit`/`testng` runners, `ide/versioning.core`/`versioning.masterfs` +
  `ide/o.eclipse.jgit*` (EDL), `ide/diff`'s engine + `spi/diff`, `ide/localhistory`, `ide/project.libraries`
  (no forbidden deps), `java/javadoc` index, bundled JaCoCo (`org.jacoco.core`, EPL-2.0, already shipped).
- **Port the Swing UI** to JavaFX; never import the prohibited UI modules (`openide.awt/nodes/explorer/
  windows/dialogs`, `core.windows`) in nbfx sources or list them in nbfx `project.xml` files.
- **Split** model/UI as usual (e.g. `nbfx.refactoring.core` + `nbfx.refactoring.ui`,
  `nbfx.test.core` + `nbfx.test.ui`, `nbfx.vcs.core` + `nbfx.vcs.ui`, `nbfx.editor.intelligence` headless);
  model modules are JavaFX-free (`check-models`).

See [`plan/18-navigation-and-search.md`](plan/18-navigation-and-search.md) through
[`plan/25-misc-java-features.md`](plan/25-misc-java-features.md).

## API is a promise

A **public package is an API** and needs the full governance set: an `arch.xml` `<api>` entry (type
export/import), an `apichanges.xml` change for every API edit, `@since` on new elements, a signature test,
and a specification-version bump on change. A module exports **only** packages that are contracts, listed
explicitly (no whole-tree `<subpackages>` without a written justification). `nbfx.api` is split per
capability rather than being a kitchen sink. See [`plan/30-api-governance.md`](plan/30-api-governance.md).

- **No toolkit in SPI names.** SPI types, annotations, public constants: capability names
  (`OptionsPanelController`, `WizardIterator`, `ViewRegistration`). The toolkit belongs to the
  implementation module (`…ui.fx`) and to private classes. Components are produced through
  **front-end component factories** resolved from `Lookup`, so one SPI serves Swing/JavaFX/web: see
  [`plan/31-front-end-apis.md`](plan/31-front-end-apis.md).
- **No stringly-typed API contracts.** No `Map` with key strings, no string editor ids, no public
  layer-path constants; use typed service types, typed refs and private constants (adapters may translate
  at the boundary to an engine's legacy shape, documented).
- **Services, not statics.** Mutable state lives in a `Lookup`-registered service with a lifecycle
  (`Closeable`), injectable in tests. Statics are allowed only for immutable constants and bounded caches
  with a named owner; a cache is never the only reference to a live model.
- **Registries cache `Lookup.Result`s.** Do not call `Lookups.forPath`/`FileUtil.getConfigFile` per query
  (config-filesystem traversal); keep a cached `Lookup.Result` in the registry and update it on module
  changes.
- **No new `friend` patches** without an entry in the friend register (what, why not API, upstream
  proposal, owner, deadline) and **no "reuse" of `org.netbeans.modules.*` internals** without a reuse
  decision: *upstream API* / *vendored fork with version and upgrade cadence* / *reimplement*. See
  [`plan/30-api-governance.md`](plan/30-api-governance.md) §8/§9.
- **Measure before claiming.** Every milestone records startup, first-use latency, module count and
  memory; a regression beyond the documented budget blocks the milestone.

## Tiered API and the plugin contract

Not every module is an API. **T0**: implementation — exports nothing, no promise. **T1**: shared API
between nbfx layers — `arch.xml`/`apichanges`/`@since`/signature test, additive changes preferred.
**T2**: the **published plugin API** — everything a third-party NBM may compile against, with a
compatibility promise, version ranges, documentation and trust (signing, HTTPS update centers, native
artifact checksums). A module starts at T0; promotion is deliberate. See
[`plan/32-api-tiers-and-plugin-contract.md`](plan/32-api-tiers-and-plugin-contract.md).

Naming follows the tier: capability names in SPI/annotations/constants (`OptionsPanelController`,
`@OptionsRegistration`, `ProjectModel`), the toolkit only in implementation modules (`…ui.fx`) and private
classes; module code-names `…nbfx.<capability>` (T0) / `…nbfx.api.<capability>` (T1/T2).

## Anti-corruption for reused internals; retirement; upstream

- **Internals are not API.** Dependencies on `org.netbeans.modules.*`/`lib.profiler` internal classes are
  isolated behind one adapter package per dependency, listed in the **reuse register** with a decision
  (*upstream API* / *vendored fork with cadence* / *reimplement*) and an owner; a fork without a merge
  cadence is not a plan. Friend patches require a **friend register** entry, an upstream issue and a
  deadline; no new friend patch without them.
- **A front end is done when the Swing modules leave the distribution.** The retirement matrix
  ([`plan/33-swing-retirement.md`](plan/33-swing-retirement.md)) is a build artifact: current-milestone
  `retire` modules must be absent from the built clusters; every `exempt` module has an owner and a
  removal milestone; an expired exemption fails the build.
- **Upstream is tracked.** Monthly submodule bump + patch rebase, patch register, patch budget, a weekly
  `next-upstream` CI job and a drift metric; every milestone opens at least one upstream proposal. See
  [`plan/34-upstream-sync-and-budget.md`](plan/34-upstream-sync-and-budget.md).
- **No invented APIs in docs.** A plan may not specify an extension point that does not exist
  (`@FxProjectNodeFactory` was one); it either designs a real SPI with its artefact set or marks it as a
  proposal to be upstreamed.
- **Data compatibility, not format ownership.** We may *read* upstream private formats (`.nps`, `.hprof`,
  `.jfr`) but only *write* formats we own or that are public; writing someone else's private serialization
  is a promise we cannot keep.

## Registration ownership (layer-generating annotations)

A layer-generating annotation and its processor live **with the SPI they complement**, in the layer that
owns that SPI: options registrations in `nbfx.options`, wizard in `nbfx.wizard`, property editors with the
editor module, project types in the project API module. Only the **shell** registrations
(actions/views/menus/toolbars/status) stay in the core `nbfx.annotations` module (with the shell `FxLayer`
constants). Annotation modules are **compile-only** (build prerequisite, no run dependency) where possible;
the generated layer output must stay identical when they move. This keeps the platform layer free of
ide/java/javafx registration concepts and makes adding a registration type a local change. See
[`plan/29-annotation-modules.md`](plan/29-annotation-modules.md).
