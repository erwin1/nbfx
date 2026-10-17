# 29 — Annotation & processor modules (splitting `nbfx.annotations`)

Steps: **Split the annotation module** (`refactor/annotations-split`, layer `nbfx-platform`, milestone
M0/M1) with per-feature slices; follow-ups in `nbfx-ide`/`nbfx-java` as their SPIs are extracted.

## Why

`nbfx-platform/c.gluonhq.netbeans.nbfx.annotations` currently holds **every** layer-generating annotation
and processor — 18 annotations, 9 processors and `FxLayer` — and every declaring module in every layer
depends on it. Consequences:

- a **platform** module encodes feature concepts of the **ide**/**java** layers (options, wizards,
  property editors, project types), so the platform layer knows about things below it;
- adding a registration type for a feature means changing the shared module (and rebuilding everything
  that depends on it);
- the single `FxLayer` mixes layer paths (actions/views/menus/status *and* options/wizards/property
  editors/projects);
- the dependency is a **compile-time** concern (annotations are `SOURCE` retention, processors run in
  javac) yet is declared as an ordinary module dependency, and nothing distinguishes the runtime path
  constants (used by readers) from the annotations (compile-only).

The split makes annotation ownership follow the **layer model** (doc 26) and module allocation (doc 27):
a registration annotation lives with the SPI it complements.

## Inventory (exact, at the time of the split)

| Annotation(s) | Processor | Owning module (layer) |
|---|---|---|
| `FxActionRegistration`, `FxActionReferences`, `FxActionReference` | `FxActionRegistrationProcessor` | core `nbfx.annotations` — **platform shell** (actions) |
| `FxViewRegistration`, `FxViewLocation` | `FxViewRegistrationProcessor` | core — platform shell (views) |
| `FxMenuRegistration(s)` | `FxMenuRegistrationProcessor` | core — platform shell (menus) |
| `FxToolbarRegistration(s)` | `FxToolbarRegistrationProcessor` | core — platform shell (toolbars) |
| `FxStatusRegistration`, `FxStatusAlignment` | `FxStatusRegistrationProcessor` | core — platform shell (status) |
| `FxOptionsRegistration`, `FxOptionsContainerRegistration`, `FxOptionsSubRegistration` | `FxOptionsRegistrationProcessor` | **`nbfx.options`** (platform) |
| `FxWizardRegistration` | `FxWizardRegistrationProcessor` | **`nbfx.wizard`** (platform) |
| `FxPropertyEditorRegistration` | `FxPropertyEditorRegistrationProcessor` | **property editors** module (`nbfx.propertysheet` today, `nbfx.nodes.ui` after doc 12/13) |
| `FxProjectTypeRegistration` | `FxProjectTypeRegistrationProcessor` | **project API** module (`nbfx.project.api`, ide — doc 26 §3.1), consumed by `nbfx-java`/`nbfx-javafx` project kinds |
| `FxLayer` | — | split (see below) |

Readers that consume these folders (and therefore need the *path constants* at runtime) are
`ViewManager`/`ActionRegistry`/`MenuRegistry`/`StatusBar` (`nbfx-platform`), `OptionsRegistry`
(`nbfx.options`), the wizard registry (`nbfx.wizard`), the property-editor registry
(`nbfx.propertysheet`), the project-kind registry (`nbfx.project.ui.api`/`nbfx.project.api`).

## Target layout

Two patterns; use whichever keeps the layer clean:

1. **Annotations + processor inside the owning module** when that module may depend on
   `org.openide.filesystems` + `openide.util.lookup` (needed by `LayerGeneratingProcessor`). Applies to
   `nbfx.options`, `nbfx.wizard`, `nbfx.propertysheet`, the project API module.
2. **A sibling `*.annotations` module** when the owning module should not carry the processor/API
   dependency (e.g. a pure model module), or when several modules need only the annotation.

Resulting modules (`plat` = `nbfx-platform`, `ide` = `nbfx-ide`):

```
plat/c.gluonhq.netbeans.nbfx.annotations              core: action/view/menu/toolbar/status + FxLayer
plat/c.gluonhq.netbeans.nbfx.options(+ .annotations)  options registrations   (core slice first)
plat/c.gluonhq.netbeans.nbfx.wizard(+ .annotations)   wizard registration
plat/c.gluonhq.netbeans.nbfx.propertysheet(+ …)       property-editor registration
ide/c.gluonhq.netbeans.nbfx.project.api(+ .annotations) project-type registration
```

`FxLayer` after the split:

- **core** keeps the platform-shell folders: `ACTIONS`, `MENUS`, `TOOLBARS`, `CONTEXT_MENUS`, `VIEWS`,
  `STATUS`, `REF_SUFFIX`;
- feature modules own their constants: `OPTIONS` (options), `WIZARDS` (wizard), `PROPERTY_EDITORS`
  (property editors), `PROJECTS` (project API) — read by the corresponding registry in the same module.

## Mechanics

- **Processor location does not affect output**: `LayerGeneratingProcessor` writes into the layer of the
  module being *compiled* (the annotated element's module), so a processor may live in a dependency.
- **Processor discovery**: javac finds processors through `META-INF/services/javax.annotation.processing.
  Processor` (`@ServiceProvider(Processor.class)`) on the **compile classpath**; the declaring module needs
  a dependency on the module hosting the annotation+processor, declared as
  `<build-prerequisite/>` + `<compile-dependency/>` and **without** `<run-dependency/>` where possible
  (compile-only), so annotation modules need not ship in the distribution.
- **Layer output must be invariant**: the generated `.instance` files and attributes are identical before
  and after each slice; verify by diffing the generated layer (`build/**/generated-layer.xml`) and the
  module build output — this makes the refactor mechanical and reviewable.
- **Public packages**: the core package keeps the platform annotations; moved annotations change imports
  in the declaring modules (mechanical; no runtime reference because retention is `SOURCE`).
- **Runtime path constants**: only the *readers* need the path strings; keep them in the module that owns
  the registry (or in the core `FxLayer` for the shell folders). Never let a runtime module depend on an
  annotation module just for a string.
- **Compile-only exclusion**: once sliced, the annotation modules can be dropped from the cluster
  distribution (they are build-time only), reducing the shipped module count — unless a module still
  references a `FxLayer` constant.

## Migration steps (branch `refactor/annotations-split`, one reviewable commit per slice)

1. **Options** — move `FxOptions*` + processor into `nbfx.options` (or `nbfx.options.annotations`); update
   `EditorOptionsPanel`, `KeyMapOptionsPanel` and any declaring module; verify layer diff, build, tests.
2. **Wizard** — move `FxWizardRegistration` + processor into `nbfx.wizard`; update declarants
   (`nbfx.project.ui`, project kinds in `nbfx-java`).
3. **Property editors** — move `FxPropertyEditorRegistration` + processor into the property-editor module
   (after doc 13 decides the module: `nbfx.nodes.ui` vs `nbfx.propertysheet`).
4. **Project types** — move `FxProjectTypeRegistration` + processor into the project API module (after the
   `nbfx.api.project` extraction into `nbfx-ide`, doc 26 §3.1); update `nbfx.project.ui.*` (java).
5. **`FxLayer` split** — core shell folders stay; feature modules get their own constants; update the
   readers (`OptionsRegistry`, wizard registry, project-kind registry, property-editor registry).
6. **Cleanup** — drop `run-dependency` on the new annotation modules where they are compile-only, exclude
   them from the cluster distribution if unused at runtime, and split the
   `FxAnnotationsProcessorTest` into per-module processor tests.
7. **Docs** — update docs 26/27 (module rows), this doc's status, and the architecture rule below.

## Acceptance

- Build + guardrails + tests green after every slice; **generated layer fragments semantically identical**.
- `nbfx.options`, `nbfx.wizard`, the property-editor module and the project API module own their
  registration annotations; the core `nbfx.annotations` holds only platform-shell registrations.
- No runtime module depends on an annotation module (compile-only), and no `FxLayer` constant mixes layers.
- Adding a new registration type for a feature no longer touches the shared platform module.
- A new architecture rule is recorded: *registration annotations live with the SPI they complement
  (owning layer/module); the core annotation module keeps only shell registrations.*

## Risks / open questions

- **Processor discovery in NetBeans module builds** — verify that a processor hosted in a dependency is
  picked up for the declaring module (existing modules already do this: `nbfx.options` depends on
  `nbfx.annotations`; the split keeps the same relationship, just narrower). Confirm with a minimal
  build.
- **Compile-only modules and the harness** — the module must still be *built* (it is in a cluster), even if
  excluded from the distribution; document how the harness treats build-prerequisite modules
  (`repo-and-build.md`).
- **Annotation packages** — keep a stable package naming (`…nbfx.options` / `…nbfx.options.annotations`)
  and note the (source-level) import churn per slice.
- **`FxLayer` duplication** — path strings are also literals in readers (e.g. `Lookups.forPath`); when
  splitting, prefer one constant per module and use it in both the processor and the reader to avoid
  drift.
