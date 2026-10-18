# 29 — Annotation & processor modules (splitting `nbfx.annotations`)

> **Naming (doc 32).** Target SPI/API names are **capability names**, never toolkit names: `OptionsPanelController`, `WizardIterator`, `ViewRegistration`, `PropertyEditor`, `ProjectModel`. `Fx*` names below refer to **current code** unless explicitly marked *target*; the rename (`refactor/api-naming`) happens **before** the annotation/API splits. Published-plugin tier and conventions: [`32-api-tiers-and-plugin-contract.md`](32-api-tiers-and-plugin-contract.md).

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

**Tier first, then placement.** A registration annotation's module is decided by its **tier** (doc
[`32`](32-api-tiers-and-plugin-contract.md) §2), not by convenience:

1. **T2 annotations → an annotation-only API module** (`nbfx.api.<capability>`): contains the annotation
   and its processor, **no UI and no implementation**, so a plugin can compile against it without
   depending on the feature's UI module. This applies to `@OptionsRegistration` (`nbfx.api.options`),
   `@WizardRegistration` (`nbfx.api.wizard`), `@PropertyEditorRegistration` (`nbfx.api.property`),
   `@ProjectTypeRegistration` (`nbfx.api.project`) and the shell registrations that plugins may use.
2. **T1/internal annotations → inside the owning module** (or a sibling `*.annotations` module) when only
   nbfx layers declare them; the owning module may carry the processor dependency
   (`org.openide.filesystems` + `openide.util.lookup`).

Resulting modules (`plat` = `nbfx-platform`, `ide` = `nbfx-ide`):

```
plat/…nbfx.annotations                 core shell: @Action/@View/@Menu/@Toolbar/@Status (+ FxLayer paths)
plat/…nbfx.api.options                 T2: @OptionsRegistration/@Container/@Sub + processor
plat/…nbfx.api.wizard                  T2: @WizardRegistration + processor
plat/…nbfx.api.property                T2: @PropertyEditorRegistration + processor
ide/…nbfx.api.project                  T2: @ProjectTypeRegistration + processor (after the project API extraction)
```

The annotation modules are **compile-only** for the nbfx feature modules (build prerequisite, no run
dependency); for a **plugin build** they are a normal compile dependency (the plugin declares them in its
`project.xml`). The processor writes into the *compiling* module's layer, so the plugin's declarations are
generated in the plugin's own layer — verify this in the plugin sample (doc 32 §3).

`FxLayer` after the split (all constants **private** — layer paths are implementation detail, not API):

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
- **`FxLayer` is a runtime dependency today** — 22 modules declare a *run* dependency on
  `nbfx.annotations`, and `FxLayer` is used at runtime by `OptionsRegistry`, `StatusElementRegistry`,
  `ViewRegistry`, `WizardRegistry` and `ActionLayerReader` (in `nbfx.api`). The **constant split comes
  first**: move each path constant into its reader (`private static final`), fix `ActionLayerReader` to
  own `REF_SUFFIX`, and only then claim "compile-only". The compile-only cleanup is a prerequisite, not a
  post-split chore.
- **Compile-only (nbfx) vs compile dependency (plugin)**: for nbfx feature modules the annotation module is
  a build prerequisite without a run dependency *after* the constants move; for a **plugin** it is a
  normal compile dependency (the plugin's `project.xml` declares it). The plugin's build is the reason the
  annotation modules must exist as separate API modules (doc 32 §2).
- **Compile-only exclusion**: once sliced and constants-free, the annotation modules can be dropped from
  the nbfx distribution (they are build-time only for nbfx), reducing the shipped module count — but they
  remain part of the **plugin build** contract.

## Migration steps (branch `refactor/annotations-split`, one reviewable commit per slice)

**Prerequisite:** the **constant split** (doc 30 §1.5: `FxLayer` out of the runtime path), tier assignment
(doc 30 §1.1), the rename (`refactor/api-naming`) and `refactor/api-split`'s inventory. The moved
annotations carry **capability names**, never `Fx*`.

1. **Options** — move `@OptionsRegistration`/`Container`/`Sub` + processor into **`nbfx.api.options`**
   (annotation-only, T2); update `nbfx.options` (window/registry) and every declaring module; verify layer
   diff, build, tests, and that a plugin can depend on the annotation module without the UI.
2. **Wizard** — move `@WizardRegistration` + processor into **`nbfx.api.wizard`**; update declarants
   (`nbfx.project.ui`, project kinds in `nbfx-java`).
3. **Property editors** — move `@PropertyEditorRegistration` + processor into **`nbfx.api.property`**
   (the `PropertyModel`/`PropertyEditor` SPI moves there too, per doc 13).
4. **Project types** — move `@ProjectTypeRegistration` + processor into **`nbfx.api.project`** (after the
   project-API extraction, doc 26 §3.1); update `nbfx.project.ui.*`.
5. **`FxLayer` split** — core shell folders stay (private constants); each feature/API module gets its own
   private constants; update the readers (`OptionsRegistry`, wizard registry, project-kind registry,
   property-editor registry).
6. **Cleanup** — drop `run-dependency` on the annotation modules for nbfx modules (compile-only), keep them
   in the cluster for the **plugin build** unless the plugin dependency manifest says otherwise, and split
   the `FxAnnotationsProcessorTest` into per-module processor tests.
7. **Docs** — update docs 26/27/30 (module rows/inventory), this doc's status, and the architecture rule.

## Acceptance

- Build + guardrails + tests green after every slice; **generated layer fragments semantically identical**.
- T2 annotations live in **annotation-only API modules**; the core `nbfx.annotations` holds only the shell
  registrations; the UI modules own their windows/registries, not their plugin annotations.
- A sample plugin compiles against the annotation/API modules **without any UI module** on its classpath.
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
