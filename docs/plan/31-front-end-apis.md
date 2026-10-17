# 31 — Front-end APIs: one SPI, many toolkits

The review's second structural demand: **the toolkit must not be in the SPI, and the SPI must not be
forked per toolkit**. Today nbfx has 57 public `Fx*` types and parallel SPIs (`FxOptionsPanel` vs
`OptionsPanelController`, `FxWizardPanel` vs `WizardDescriptor.Panel`, `FxPropertyEditor`,
`FxViewRegistration`, `FxDialogDescriptor`, …). This doc defines the target: capability-named SPI +
a **front-end component factory** resolved from `Lookup`, so one SPI serves Swing, JavaFX (and later a
web/terminal front end), prototyped on options and proposed upstream.

Governance (versions, `apichanges`, registers, guardrails, budget) is [30-api-governance.md](30-api-governance.md).

## 1. The rule

1. **SPI type names say the capability**, never the toolkit: `OptionsPanelController`, `WizardIterator`,
   `PropertyEditor`, `ViewRegistration`, `DialogDescriptor`, `ProgressPresenter`, `ViewProvider`.
2. **SPI signatures are toolkit-neutral**: no `javafx.*`, no `javax.swing.*`, no `java.awt.*` in an SPI
   package (checkable structurally, doc 30 §6). Where a component must be produced, the SPI exposes a
   *factory* service or returns an opaque handle the front end resolves.
3. **The toolkit lives in the implementation module name** (`…options.ui.fx`) and in private classes.
4. **One SPI, many implementations**: the front end registers its factories in `Lookup`
   (`ComponentFactory`, `DialogFactory`, `MenuFactory`, `StatusFactory`, `ProgressFactory`, …); the SPI
   code never references a toolkit.
5. **Upstreamability is a design requirement**: the factory hook must be additive so the Swing
   implementation in `apache/netbeans` keeps working untouched.

## 2. The pattern (three layers of API, per capability)

```
SPI (toolkit-free)                     Front-end hook (toolkit-free)              Implementation
──────────────────────────────────     ─────────────────────────────────────      ────────────────────
OptionsPanelController                 ComponentFactory                            options.ui.fx
  update()/applyChanges()/isValid()      Object create(Object componentKind,          (FX panels)
  isChanged()/getLookup()/…                Object owner, Object model)
WizardIterator / WizardPanel           ComponentFactory (kind = page)
PropertyEditor                         ComponentFactory (kind = editor)
ViewProvider                           ComponentFactory (kind = view)
DialogDescriptor                       DialogFactory
Command/action registration            MenuFactory/ToolbarFactory/StatusFactory
ProgressHandle presenter               ProgressFactory
NodeModel + views                      NodeViewFactory
```

- The SPI module depends only on the toolkit-free hook (a tiny `…spi` module or the capability's API
  module); the front end depends on the SPI and the toolkit.
- A console/headless test front end can implement the same factories (great for tests).
- The hook is intentionally small: `Object create(kind, owner, model)` with typed `kind` constants and a
  documented `Object` result contract (each front end knows it produces a component for the kind). Java's
  lack of a common component supertype is the reason `Object` is used — document it, do not hide it, and
  add per-kind typed sub-interfaces (`PanelFactory`, `EditorFactory`) where useful.

## 3. Mapping: today's `Fx*` SPI → target

| Today (public `Fx*`) | Target SPI (capability) | Front-end hook | FX impl module | Doc |
|---|---|---|---|---|
| `FxOptionsPanel`, `FxOptionsRegistration`, `FxOptionsContainerRegistration`, `FxOptionsSubRegistration` | `OptionsPanelController`, `OptionsRegistration`, `OptionsContainerRegistration`, `OptionsSubRegistration` | `ComponentFactory` (kind `OPTIONS_PANEL`) | `nbfx.options.ui.fx` | 14 |
| `FxWizardPanel`, `FxWizardIterator`, `FxWizardRegistration`, `FxWizardDialog` | `WizardPanel`, `WizardIterator`, `WizardRegistration` (+ `WizardUI`) | `ComponentFactory` (kind `WIZARD_PAGE`), `DialogFactory` | `nbfx.wizard.ui.fx` | 07 |
| `FxPropertyEditor`, `FxPropertyEditorRegistration` | `PropertyEditor`, `PropertyEditorRegistration` | `ComponentFactory` (kind `PROPERTY_EDITOR`) | `nbfx.propertysheet.ui.fx` | 13 |
| `FxViewRegistration`, `ViewProvider`, `FxViewLocation` | `ViewRegistration`, `ViewProvider`, `ViewLocation` | `ComponentFactory` (kind `VIEW`) | `nbfx.view.ui.fx` | 12 |
| `FxDialogDescriptor`, `FxDialog*` | `DialogDescriptor`, `Dialog` | `DialogFactory` | `nbfx.dialogs.ui.fx` | 09 |
| `FxActionRegistration`, `FxActionReferences`, `FxActionReference`, `FxActionContext` | `ActionRegistration`, `ActionReferences`, `ActionReference`, `ActionContext` | `MenuFactory`/`ToolbarFactory` | `nbfx.actions.ui.fx` | 00 |
| `FxMenuRegistration(s)`, `FxToolbarRegistration(s)`, `FxStatusRegistration`, `FxStatusAlignment` | `MenuRegistration(s)`, `ToolbarRegistration(s)`, `StatusRegistration`, `StatusAlignment` | `MenuFactory`/`ToolbarFactory`/`StatusFactory` | `nbfx.menus.ui.fx`, `nbfx.statusbar.ui.fx` | 00 |
| `FxProgress*` presenter | `ProgressPresenter`/`ProgressFactory` | `ProgressFactory` | `nbfx.progress.ui.fx` | 06 |
| `FxOutput`, `FxConsole` | `InputOutputProvider` (`api.io`/`spi.io` are already the toolkit-free contract) + presenter hook | `IOFactory` | `nbfx.output.ui.fx` | 15 |
| `NodeModel`/`ChildrenModel`/`PropertyModel`/`ExplorerContext` (already capability-named) | keep | `NodeViewFactory` | `nbfx.nodes.ui.fx` | 12/13 |
| `FxLayer` public path constants | `private static final` per processor/reader | — | (each module) | 29 |
| Debug/profiler/editor view models (nbfx-native) | keep, but name toolkit-neutrally (`DebugViewFactory`, `ProfilerViewFactory`, `EditorPresentation`) | own factories | `*.ui.fx` | 16/17 |

Note: the **layer-generated registration** stays; the *annotation* and the *layer path* are the toolkit-free
description (annotations are compile-time, paths are private), and the *components* come from the factory.
That is the whole point: declarative registration (platform idiom) + injected presentation.

## 4. Migration mechanics

1. **One capability at a time**, in this order (risk-ordered, each with a shim so nothing breaks):
   options → wizard → dialogs → status/toolbar/menu → property editors → views → progress → output.
2. In each slice: introduce the toolkit-free hook (`ComponentFactory` + typed kinds or a typed sub-interface),
   rename the SPI types (doc 30 §3), move the FX classes to `…ui.fx`, register the FX factory via
   `@ServiceProvider`, keep the generated layer output **identical**, update `arch.xml`/`apichanges.xml`.
3. **Upstream the hook** for the first slice (options): add `OptionsComponentFactory` to
   `platform/options.api` with a Swing implementation in `core.ui`/`options.ui` (no behaviour change), and
   a `default` fallback so existing controllers keep compiling. Then repeat the pattern for wizard,
   property sheet, dialogs, views, progress.
4. **Freeze**: no new `Fx*` public type and no new toolkit import in an SPI package after
   `refactor/api-naming` lands; the structural check (doc 30 §6) enforces it.
5. **Do it early**: before M1's API freeze, so the five milestones do not multiply the old shape.

## 5. What stays nbfx-specific (not an upstream proposal yet)

- the FX component kit (outline, property sheet, output, terminal, diff, charts, palette), the shell/docking,
  theme tokens (doc 10), and the nbfx-native diagnostic view models (debugger/profiler/editor) — named
  toolkit-neutrally and reviewed internally;
- the `nbfx-*` cluster layout and the OS/arch module enablement (doc 17 §3.6).

## 6. Why this is worth the churn

- **Upstreamable**: the SPI split is the contribution; nbfx stops being a fork with extra steps and becomes
  a second front end for the platform (doc 30 §10).
- **Testable**: a headless/console factory makes SPI tests possible without a UI.
- **Honest contracts**: one SPI, many implementations; no toolkit name in any promise.
- **Smaller surface**: 57 `Fx*` types collapse into capability SPI + a handful of factory kinds.

## 7. Risks

- **Upstream review latency** — keep the nbfx-side shim until accepted; the shim is private and removed
  when the upstream API lands.
- **`Object create(...)` erodes type safety** — mitigate with typed sub-interfaces per kind and a single
  resolution point in the front end.
- **Rename churn** — that is why it is one step (`refactor/api-naming`) executed before M1, mechanical,
  with layer output invariance and the guardrail to stop regression.
- **Two APIs during transition** — forbid adding features to the old `Fx*` shape; the shims are
  deprecation-only and have removal milestones in `apichanges.xml`.
