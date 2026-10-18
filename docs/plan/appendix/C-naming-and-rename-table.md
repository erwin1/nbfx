# Appendix C — Naming and the rename table

`PLAN.md` §3.2 forbids `Fx*`/`Sw*` names in public API. The tree has **57 public `Fx*` types**, 56 of
them in modules that export packages ([`../figures.md`](../figures.md)), so they are public API under today's `<subpackages>` exports. §7's
migration map moves *modules*, not *names*, so every one survives the split. This appendix is the missing
step: the rename, which is source-compatible and therefore belongs in W1 **before** the API split, so the
new `api.*` modules are born with their final names.

## C.1 The rule, precisely

| Location of the type | Name rule |
|---|---|
| Exported package (public API, any tier) | **no `Fx`/`Sw` prefix, no `Impl`**, capability noun |
| Non-exported package inside any module (implementation) | `Fx*` allowed; `*Impl` allowed |
| A registration **annotation** | `@<Capability>Registration`, in the owning `api.*` module |
| An annotation **processor** | implementation: package-private, in a **non-exported** package |
| A layer-path constant class | `LayerPaths`, sole owner `api.layer` (T1), see C.3 |

The trap today: `nbfx.annotations` and `nbfx.propertysheet` **export their whole subtree**, so their
processors and internal helpers (`FxViewRegistrationProcessor`, `FxPropertyEditors`, `ActionLayerReader`) are
public API by accident. Two thirds of the rename cost is really *moving impl classes out of exported
packages*, and that is a consequence of the split, not a renaming whim.

## C.2 The table (all 57 public `Fx*` types)

Legend: **API** = exported today; **→** target name; **module** = target module (§6/§7); **(impl)** =
not API after the split, `Fx*` may be kept.

### Annotations module (`nbfx.annotations`, 27 types, all exported)

| Today | Target | Target module |
|---|---|---|
| `FxActionRegistration` | `ActionRegistration` | `api.actions` |
| `FxActionReference` / `FxActionReferences` | `ActionRef` / `ActionRefs` | `api.actions` |
| `FxViewRegistration` | `ViewRegistration` | `api.views` |
| `FxViewLocation` | `ViewLocation` | `api.views` |
| `FxMenuRegistration` / `FxMenuRegistrations` | `MenuRegistration` / `MenuRegistrations` | `api.views` |
| `FxToolbarRegistration` / `FxToolbarRegistrations` | `ToolbarRegistration` / `ToolbarRegistrations` | `api.views` |
| `FxStatusRegistration` | `StatusRegistration` | `api.status` |
| `FxStatusAlignment` | `StatusAlignment` | `api.status` |
| `FxOptionsRegistration` / `FxOptionsSubRegistration` / `FxOptionsContainerRegistration` | `OptionsRegistration` / `OptionsSubRegistration` / `OptionsContainerRegistration` | `api.options` |
| `FxPropertyEditorRegistration` | `PropertyEditorRegistration` | `api.property` |
| `FxProjectTypeRegistration` | `ProjectTypeRegistration` | `api.project` |
| `FxWizardRegistration` | `WizardRegistration` | `api.wizard` |
| `FxLayer` | `LayerPaths` | `api.layer` (T1, see C.3) |
| `FxViewRegistrationProcessor`, `FxActionRegistrationProcessor`, `FxMenuRegistrationProcessor`, `FxToolbarRegistrationProcessor`, `FxStatusRegistrationProcessor`, `FxOptionsRegistrationProcessor`, `FxPropertyEditorRegistrationProcessor`, `FxProjectTypeRegistrationProcessor`, `FxWizardRegistrationProcessor` | **(impl)** — move to a non-exported package (e.g. `…api.views.processors`) and make package-private where the build allows | owning `api.*` |

### `nbfx.api` (5 types, exported)

| Today | Target | Target module |
|---|---|---|
| `FxActionRef` | `ActionRef` | `api.actions` |
| `FxApplicationMenu` | `ApplicationMenu` | `api.views` (or `platform.menus`) |
| `FxArea` | `ViewArea` | `api.views` |
| `FxSubmenu` | `Submenu` | `api.views` |
| `FxProgress` | `Progress` | `api.progress` |

### Platform units that export today (25 types)

| Today | Target | Target module |
|---|---|---|
| `FxOptionsDialog` / `FxOptionsPanel` | `OptionsWindow` / `OptionsPanelHost` | `platform.options` |
| `FxOutput` / `FxOutputImpl` / `FxConsole` | `OutputConsole` / `OutputConsoleImpl` / `ConsolePanel` | `api.output` / `platform.output` |
| `FxProjectCustomizerDialog` / `FxProjectCustomizerPanel` / `FxProjectCustomizers` | `CustomizerDialog` / `CustomizerPanel` / `Customizers` | `platform.project.customizer` (impl) / `api.project.customizer` |
| `FxProperty` / `FxPropertySet` / `FxPropertyEditor` / `FxPropertyEditors` / `FxPropertiesProvider` / `FxPropertySheet` | `PropertyModel` is already the API name in `api.nodes`; the rest are **(impl)** in `platform.nodes` | `api.nodes` / `api.property` / `platform.nodes` |
| `FxStatusBar` / `FxStatusElement` | `StatusBar` / `StatusElementHolder` **(impl)** | `platform.status` |
| `FxWindowSystem` / `FxWindowSystemListener` | `WindowSystem` / `WindowSystemListener` **(impl)** | `platform.workspace` |
| `FxWizard` / `FxWizardDialog` / `FxWizardPanel` | `Wizard` / `WizardDialog` / `WizardPanelHost` **(impl; the API is `api.wizard.WizardIterator/WizardPanel`)** | `platform.wizard` |
| `FxStructurePanel` | `StructurePanel` **(impl)** | `ide.structure` |
| `FxGuiRunLevel` | `GuiRunLevel` **(impl)** | `platform.launcher` |
| `FxProgressImpl` | `ProgressOverlay` **(impl)** | `platform.progress` |
| `FxActionContextImpl` | `ActionContextImpl` **(impl)** | `platform.actions` |

## C.3 Where the layer paths live

The split moves the annotations into their `api.*` modules, but the folder names (`NbFx/Views`, `NbFx/Menus`,
…) are shared by every processor and every reader. Duplicating them per module (the previous plan) is
rejected (PLAN §3.3): no test can compare two privates. So there is **one** owner:

- module `com.gluonhq.netbeans.nbfx.api.layer` (cluster `nbfx-api`), tier **T1** (nbfx-internal, versioned,
  not part of the published plugin API);
- class `LayerPaths` with the folder constants (`ACTIONS`, `MENUS`, `TOOLBARS`, `CONTEXT_MENUS`, `VIEWS`,
  `STATUS`, `PROPERTY_EDITORS`, `OPTIONS`, `PROJECTS`, `WIZARDS`, `REF_SUFFIX`);
- every processor writes through it, every registry reads through it;
- the contract test (PLAN §3.3) builds a layer from a sample annotated class and reads it back through the
  reader API, so writer and reader cannot drift.

This replaces the "`api.layer`: not planned" note in the old §6.1 and the "private constants" rule in §3.3.

## C.4 The step

`refactor/api-naming` — a mechanical rename with `git mv` + `sed`-style edits, **no behaviour change**,
landed in W1 **before** `refactor/api-split`:

1. rename the annotation/API types per C.2 and move the processors out of exported packages (C.1);
2. introduce `api.layer.LayerPaths` and switch all processors/registries to it;
3. update `arch.xml`, `apichanges.xml`, `Bundle.properties` keys and the module `project.xml` references;
4. acceptance: `check-naming` (PLAN §9) is green — **no `Fx*`/`Impl` in an exported package** — and the
   layer contract test passes.

**Watch out.** This changes public type names, so it is source-incompatible for *nbfx-internal* consumers
(the ~20 modules that use the annotations). Pre-1.0 that is allowed; the rule "code-names and API names
freeze at 1.0" (PLAN §8) applies. Do not mix this step with behaviour changes.
