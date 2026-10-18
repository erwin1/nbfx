# nbfx architecture rules

Durable, cross-cutting rules for the nbfx code base. The full plan is
[`plan/PLAN.md`](plan/PLAN.md); this file is the rule form of its §§2–5 and 9. When a rule and a module
disagree, the rule wins (or the rule is changed here first).

## 1. Layers, clusters and dependency direction

```
nbfx-libs → nbfx-api → nbfx-platform → nbfx-ide → nbfx-java / nbfx-javafx
```

- **Downward dependencies only (nbfx edges).** No upward edges, no cycles. Siblings only in the declared
  order `java` → `javafx`. An **upstream reuse edge** (an nbfx module on a NetBeans module) is not a layer
  edge: it is recorded with a justification and a retirement plan ([Appendix A](plan/appendix/A-toolkit-policy.md)).
  The module graph is a DAG and is enforced by the build.
- **No split packages.** Every package has exactly one owning module.
- **Model modules are toolkit-free.** No `javafx.*`, no `javax.swing`/`java.awt` in an M module, except
  the named, owned exemptions of [Appendix A](plan/appendix/A-toolkit-policy.md) (interfaces that an
  upstream SPI forces into a signature, never components); no legacy NetBeans UI in
  any module a headless engine or plugin backend consumes.
- **One semantic boundary per language.** The Java layer is the only place that may import
  `org.netbeans.api.java.source`/`libs.javacapi`; every other layer consumes the toolkit-free seams
  (`EditorDocument`, `OpenSources`, `api.elements`). `javax.swing.text.Document` is banned everywhere
  ([Appendix B](plan/appendix/B-java-source-isolation.md)).
- **The UI toolkit lives in implementations**, never in an API type name.
- **`main` is the truth.** A statement in the plan refers to `main`; local branches are named explicitly.

## 2. Model / UI split

- Behaviour that is not a user interface belongs to an **M** (model/backend) module with a core-only
  dependency chain; the JavaFX UI lives in a separate `ide.*`/`platform.*` module.
- A model module may be **moved into the NetBeans core** and a Swing front end rebuilt on it; that is why it
  must not depend on a toolkit.
- **Prefer reuse**: before writing a model, check whether the NetBeans core exposes it headlessly
  (`api.templates.FileBuilder`, `api.io`, `java.source.base`, `spi.quicksearch`, …). Reuse it and write only
  the genuinely new part.

## 3. Naming

- **Module/package**: `com.gluonhq.netbeans.nbfx.<layer>.<capability>` (`platform`, `ide`, `java`,
  `javafx`), APIs under `…nbfx.api.<capability>`, libraries under `…nbfx.libs.<lib>`. Package == module
  code-name.
- **API/SPI types**: capability nouns (`OptionsPanelController`, `WizardIterator`, `ViewProvider`,
  `ProjectModel`, `NodeModel`, `Settings`). **No `Fx*`/`Sw*` prefixes, no `*Impl`, no `Object`-typed
  factories, no `com.sun.source`/`javax.lang.model` types in a value type** (convert at the boundary —
  nb-javac duplicates those classes; see Appendix A.4).
- **Registration annotations**: `@<Capability>Registration`, living in the owning `api.*` module (there are
  no separate annotation modules).
- **Implementations**: may use the toolkit prefix, but only in non-public classes of the implementation
  module (`FxOptionsWindow` inside `…platform.options`).
- **Ids**: one vocabulary per domain (`ActionIds`, `TemplateIds`, `EditorCommands`), `@since`-tagged.

## 4. API tiers and governance

- **T0** implementation: exports nothing.
- **T1** shared API: nbfx layers only.
- **T2-FX** published plugin API: JavaFX allowed (`Node`, `ObservableValue`, `KeyCombination`).
- **M** model/backend API: toolkit-free.
- **L** bundled library: licence, notice, guard allowlist.

Every T1/T2-FX/M public package has an `arch.xml` `<api>` entry, an `apichanges.xml` entry per change,
`@since` on new elements and a signature test. Modules start at specification version **1.0**; additions
bump the minor, incompatible changes the major; dependants declare a version that exists. A package is
public only if it is a contract; never `<subpackages>` for a whole tree.

**Registration ownership**: an annotation lives in the API module of the capability it registers; only
shell registrations (actions/views/menus/toolbars/status) belong to their respective shell API modules.
Layer paths are a **contract**: the folder constants are public and versioned in the owning `api.*` module
(or annotation attributes with public defaults), and a contract test builds a layer from a sample annotated
class and reads it back through the reader API. "Private constants per reader" is rejected — no test can
compare two privates.

**Plugin contract**: a plugin depends only on the annotation-bearing `api.*` modules, the T2-FX/M modules of
the extension points it implements, and the platform core (`org.openide.util`, `org.openide.filesystems`,
`org.openide.util.lookup`). It never depends on a T0 module. A `Node`-returning API documents ownership
(one parent; a new node per call; the shell owns what a view hands over). Menu contributions are FX
`MenuItem`s. Trust (JAR signature verification, HTTPS centers, native checksums) is part of the contract.

## 5. Observability and settings

- Models use **typed `Lookup`** `Result`/`InstanceContent` (or, where an engine forces it, `java.beans`
  behind a typed adapter). No `javafx.beans`/`javafx.collections` in M modules. The UI adapter converts to
  FX observables at the boundary.
- **Settings** live in `api.settings` (M): a typed facade over `NbPreferences`, keys
  `nbfx.<capability>.<key>`, change listeners. Panels bind through a UI adapter; `EditorSettings`-style
  observable state is migrated to this layer.
- **Registries cache `Lookup.Result`s**; never `Lookups.forPath` per query.

## 6. UI architecture

- **One node model, many views**: `api.nodes` (M) + FX tree/tree-table/table/list and the property sheet.
  The navigator, structure, breakpoints, tests, variables and task list all render through it.
- **Typed factories, one SPI many toolkits**: component-producing SPIs are generic over `C`
  (`OptionsPanelFactory<C>`, `WizardUi<C>`, `PropertyEditorFactory<C>`, `ViewFactory<C>`); the shell binds
  `C = Node`, a test front end binds a test node. No casts, no `Object` factories.
- **Model-first panels**: fields + actions + validator (`PanelModel`) rendered by the property sheet; use a
  factory only for custom visualisations.
- **Uniform async**: cancellable jobs with progress; nothing blocks the FX thread; one job/task model.
- One dialog/notification surface, one quick-search surface, one theme (semantic tokens), one settings layer.

## 7. Data compatibility

Project properties, preferences, keymap/breakpoints/platform storage keep the NetBeans names so headless
modules keep working. We may *read* upstream private formats (`.nps`, `.hprof`, `.jfr`); we write only
formats we own or that are public. Processes are started only through `api.execution` (PTY by default for
run); no module starts a process itself.

## 8. Guardrails (in the build, not in a document)

- module graph: no cycles, no upward layer edge;
- forbidden NetBeans UI modules absent from the runtime path, or exempted (owner + deadline);
- bytecode: M jars contain no `javafx.*`/`javax.swing`/`java.awt`/legacy UI; T2-FX jars contain no Swing;
  L jars allowlisted;
- API governance: `arch.xml` + `apichanges` + signature tests; dependant spec versions exist;
- public-package audit: explicit packages, tiers derived from the audit; T0 with consumers is an error;
- naming: no toolkit/`Impl`/`Object` in public API; layer-path constants only in the owning `api.*` module;
- budget: startup, "usable", first completion, module count, memory; >20 % regression blocks a milestone.

The guardrail must be **green on day one** with the current exemptions; it fails only on new violations or
expired exemptions.

## 9. Process

- A plan step = one branch = one reviewable change with tests and guardrails.
- A naming step is **source-compatible**; SPI shape changes are separate migrations with deprecation.
- No new `friend` patch without an upstream issue and a register entry; no dependency on
  `org.netbeans.modules.*` internals without a decision (*upstream API* / *vendored fork with cadence* /
  *reimplement*).
- Upstream is tracked: monthly submodule/patch rebase, patch budget, a weekly `next-upstream` CI job, and at
  least one upstream proposal per milestone.
- **Retirement is the deliverable**: a milestone is done when its Swing modules are absent from the
  distribution, asserted by the build.
