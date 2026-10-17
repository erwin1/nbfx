# 12 — Nodes & views API (`NodeModel`/`ChildrenModel`/`PropertyModel` + FX views)

Steps: **Nodes & views API** · branch: `feature/nodes-api` · status: planned, additive (can start in
parallel). Follow-ups: **Property sheet & editors** (`feature/property-sheet-editors`, detail in
[`13-property-sheet-and-editors.md`](13-property-sheet-and-editors.md)) and the migration
`refactor/views-on-nodes`.

The property model, editor SPI, property sheet and outline-column binding are detailed in
[`13-property-sheet-and-editors.md`](13-property-sheet-and-editors.md): they are what makes the outline
views actually work off the same model as the Properties window.

## Why

NetBeans' **Nodes API** is the model behind every tree, table, tree-table and the Properties window: one
`Node`/`Children` model drives several views (`ExplorerManager` + `BeanTreeView`/`OutlineView`/
`TableView`/…), supplies the Properties window through `Node.PropertySet`, and offers extensions via
`Node.getLookup()` (cookies are legacy). nbfx cannot use it (`org.openide.nodes`/`openide.explorer` are
forbidden by purity), and today its views each carry their own model.

This is the same kind of move nbfx already made for `org.openide.windows.TopComponent`: a **headless
model** plus **JavaFX views** on top. Here: `nbfx.nodes` (model) + `nbfx.nodes.ui` (views).

## Original Nodes API — what it provides

**Model (`org.openide.nodes`, headless)**
- `Node`: display name / HTML name, description, icon + opened icon, `canRename`/`canDestroy`/`canCopy`,
  `getActions(boolean)` / `getPreferredAction()` / `getNewTypes()`, `getPropertySets()`,
  `getChildren()`, `getLookup()` (and legacy `getCookie`), `getHandle()` (serialization).
- `Children` (`Array`, `Keys<T>`, `SortedArray`, `AsynchChildren`, …) + `ChildFactory<T>`: **lazy /
  asynchronous** children with add/remove/change/reorder events (`NodeListener`, `NodeEvent`,
  `NodeMemberEvent`, `NodeReorderEvent`).
- `Node.Property`, `Node.PropertySet`, `Sheet`, `PropertySupport`, property editors — the Properties
  window model.
- `Node.Cookie`: legacy marker. `Node.getCookie(type)` **already delegates to the Node's `Lookup`**
  (`platform/openide.nodes/.../Node.java`, `getCookie`/`getLookup`); the lookup is the supported
  extension point. New code implements **Lookup only**.
- Composition: `AbstractNode`, `FilterNode`, `Index` (child order), `NodeFactory`/`NodeFactorySupport`
  (`ide/projectuiapi`, used for `Projects/<type>/Nodes`), `NodeOp`/`NodeOperation`/`NodeTransfer`
  (rename/copy/move/delete, DnD).

**Views (`org.openide.explorer`, Swing)**
- `ExplorerManager` (root context + selection) + `ExplorerUtils` (selection ↔ actions global context).
- One model drives `BeanTreeView`, `TreeView`, `OutlineView`/`TreeTableView`, `TableView`, `ListView`,
  `IconView`, `ChoiceView`, `ContextTreeView`, `MenuView`.
- The Properties window is yet another consumer of the same model.

## nbfx today (the gap)

| Concern | Current nbfx | Gap |
|---|---|---|
| View abstraction (TopComponent analogue) | `ViewProvider` + `@FxViewRegistration` + `ViewManager` (nbfx.api.view) | done — the pattern to mirror for nodes |
| Project tree | `AbstractNavigatorProvider<T>` + `TreeView<T>`; `ProjectNavigatorImpl`/`FileNavigatorImpl` build their own entries, loading and persistence (`nbfx.project.ui.java`) | one model per view; no shared node concept |
| Structure/Navigator | `StructureView`/`StructurePanel`/`FxStructurePanel` + per-language code (`nbfx.structure`, `nbfx.structure.java`) | own model again |
| Properties | `FxPropertiesProvider` keyed on `Object context` (`Project`/`File`/`EditorDocument`) → `FxPropertySet`/`FxProperty` (+ editors) rendered by `FxPropertySheet` (`nbfx.propertysheet`) | a parallel model; not node properties (but the property/value/editor model is good and should be reused) |
| Context / selection | `ViewProvider.getLookup()` published to the global `FxActionContext`; `FileContextMenuContributor` / `ElementContextMenuContributor` | no `ExplorerManager`-like root+selection; menus are type-specific |
| Project-type nodes | `ProjectKindProvider` supplies project files/dirs; `@FxProjectNodeFactory` is mentioned in `MVP.md` but not implemented | node factories need a node model |
| Cookies | n/a | implement Lookup only |

## Decisions

1. **Naming: `NodeModel`, not `FxNode`.** `Fx` would suggest a JavaFX dependency, and `Node` clashes with
   `javafx.scene.Node`. The model module is `nbfx.nodes`; the UI module is `nbfx.nodes.ui`.
2. **The model is JavaFX-free.** No `javafx.beans.*` / `javafx.collections.*` (they would break the
   core-only chain and the `check-models` guardrail). Observability uses JDK `java.beans
   .PropertyChangeSupport` plus explicit nbfx listener/event types — exactly how `org.openide.nodes`
   models `Node`/`Children`. The **UI adapter** (`nbfx.nodes.ui`) wraps the model in JavaFX
   `ObservableList`/`ObservableValue` for binding. (`nbfx.api`'s existing `javafx.beans` usage in
   `OpenProject`/`ProjectRegistry` is API/UI-facing and stays; this is a deliberate difference for the
   core-movable node model.)
3. **Cookies → `Lookup`.** `NodeModel.getLookup()` is the only extension mechanism; no Cookie API.
   `org.openide.util.Lookup` is core, so it is allowed in the model.
4. **Drop serialization/beans**: no `Node.Handle`, `Index`, `BeanNode`; views keep their own persisted
   state (the navigators already do).
5. **One model, many views.** A shared `ExplorerContext` (root + selection) is what makes tree,
   tree-table and table display the same nodes.

## Target architecture

### `nbfx.nodes` — model (headless, no `javafx.*`; add to `check-models`)

```java
// sketch, not final API
interface NodeModel {
    String getId();
    String getDisplayName();
    String getHtmlDisplayName();        // optional decoration (badges/markup)
    String getDescription();
    String getIconId();                 // logical icon key, resolved by the UI
    List<String> getIconBadges();       // e.g. VCS status decorations
    Lookup getLookup();                 // replaces Node.Cookie
    ChildrenModel getChildren();
    List<PropertySetModel> getPropertySets();
    boolean canRename(); boolean canDelete(); boolean canCopy();
    List<String> getActionIds(boolean context);  // command ids, resolved by the UI
    String getPreferredActionId();
    List<String> getNewTypeIds();       // template ids ("New" submenu)
    void addPropertyChangeListener(PropertyChangeListener l);
    void removePropertyChangeListener(PropertyChangeListener l);
}

interface ChildrenModel {
    List<NodeModel> getNodes();
    boolean add(NodeModel... nodes);
    boolean remove(NodeModel... nodes);
    void addNodeListener(NodeListener l);
    void removeNodeListener(NodeListener l);
}

abstract class ChildFactory<T> implements ChildrenModel { // lazy/async
    protected abstract NodeModel[] createNodes(T key);
    protected abstract T createKey();                    // or keys(...)
    public void refresh();
}
```

- Action/new-type references are **plain `String` ids** so the model does not depend on `nbfx.api`
  (which uses JavaFX); the UI resolves them through `ActionRegistry`/`ActionIds` and `WizardRegistry`.
- `PropertyModel` / `PropertySetModel` — promote the existing `FxProperty`/`FxPropertySet` and rename
  them: stable name, display name, short description, value type, value/writable/expert/default, allowed
  values, editor id, `format`/`parse`, sort key, async state and change events. The JavaFX
  `ObservableValue` member is dropped from the model; the UI adapts. Aggregation for multi-selection
  (`AggregatePropertyModel`/`AggregatePropertySetModel`) lives here too. The full contract, the editor
  SPI, the property sheet and the outline-column binding are in
  [`13-property-sheet-and-editors.md`](13-property-sheet-and-editors.md).
- `ExplorerContext` — root nodes + selection, observable through listeners (the `ExplorerManager`
  analogue; no JavaFX).
- Dependencies: JDK + `org.openide.util` (`Lookup`, `Cancellable`); **`nbfx-platform`** so the chain stays
  core-only.

### `nbfx.nodes.ui` — JavaFX

- Reusable view components over `ExplorerContext`: **tree**, **tree-table** (OutlineView analogue),
  **table**, **list** — all attach to the same model; a `ViewProvider` embeds one.
- FX adapters: `ObservableList<NodeModel>` / `ObjectProperty<NodeModel>` and selection → the global
  `FxActionContext` (the `ExplorerUtils` analogue).
- Reusable **property editors** (SPI + built-ins) and the **outline column binding** cell stack shared
  with the property sheet — see [`13-property-sheet-and-editors.md`](13-property-sheet-and-editors.md);
  node actions / context menu / preferred action rendering; icon/badge resolution; drag & drop and node
  operations (follow-up). The property **sheet view** itself stays in `nbfx.propertysheet` on top of the
  model.
- Dependencies: `nbfx.nodes`, `nbfx.api` (commands/actions), `nbfx.lib.javafx`, plus the shell modules.

## Mapping

| NetBeans | nbfx |
|---|---|
| `Node` | `NodeModel` |
| `Children` / `Children.Keys` / `ChildFactory` | `ChildrenModel` / `ChildFactory<T>` |
| `Node.Property` / `PropertySet` / `Sheet` | `PropertyModel` / `PropertySetModel` (promoted from `FxProperty`/`FxPropertySet`; contract in doc 13) |
| `PropertyEditor` / `PropertyEnv` / `PropertyPanel` | `PropertyEditor` / `PropertyEnv` / editor registry (doc 13) |
| `Node.Cookie` / `getCookie` | `NodeModel.getLookup()` only |
| `ExplorerManager` + `ExplorerUtils` | `ExplorerContext` + `FxActionContext` bridge |
| `BeanTreeView` / `OutlineView` / `TableView` / `ListView` | FX view components over `ExplorerContext` |
| `OutlineView.setPropertyColumns(...)` / `TreeTable` columns | `PropertyColumn` + `OutlineView` cell stack (doc 13) |
| `NodeFactory`/`NodeFactorySupport` + `Projects/<type>/Nodes` | `@FxProjectNodeFactory` + per-kind node contributors |
| `NodeListener` / `NodeMemberEvent` | `NodeListener`/`ChildrenListener` (java.beans-style) |
| `NodeOp` / `NodeTransfer` | node operations (follow-up) |
| `Index`, `Handle`, `BeanNode` | dropped |

## Migration

1. **`nbfx.nodes` model + tests** (no UI). Add it to `check-models`; unit tests for children
   events/lazy loading/cancel, property changes, selection.
2. **`nbfx.nodes.ui` views + example**; existing views untouched.
3. **Property sheet & editors** (`feature/property-sheet-editors`, detail doc
   [`13`](13-property-sheet-and-editors.md)): editor SPI/built-ins, `PropertyColumn` + outline cell
   binding, multi-selection aggregation, and the **Properties window** onto node property sets — the
   `Project/File/EditorDocumentPropertiesProvider`s become node contributors (or a
   `NodePropertyProvider` SPI keyed by node type); `FxPropertySheet` renders the node's sets.
4. **Project navigators** (`ProjectNavigatorImpl`, `FileNavigatorImpl`) onto nodes + the generic tree
   view; `ProjectKindProvider` produces children/child factories instead of navigator entries; keep the
   existing background loading/cancellation as `ChildFactory`.
5. **Structure view** onto nodes (the language providers produce nodes).
6. **Node actions / new-types / preferred action**, then **operations/DnD** (rename/delete/copy/paste).

## Placement / ordering

- New step **Nodes & views API** → `feature/nodes-api` (Part 1 foundation, next to
  `feature/theming`/`feature/fx-dialogs`). **Parallel**: new modules, additive, no changes to existing
  views.
- Step **Property sheet & editors** → `feature/property-sheet-editors` (detail
  [`13`](13-property-sheet-and-editors.md)); depends on `nodes-api`, additive, before the migration.
- Follow-up **Views on nodes** → `refactor/views-on-nodes` (step 3–5 above) — a refactor of working
  views; keep the old view until its replacement is proven, migrate view by view.
- **Ordering**: land `nodes-api` **before** `property-sheet-editors`, and both **before** the `FxProject`
  node factories (`@FxProjectNodeFactory`) and the project navigator/Structure/Properties parity work, so
  nodes and properties are written once.

## Tests & acceptance

- Model unit tests (headless): children add/remove/change/reorder, lazy `ChildFactory` loading and
  cancellation, property change notifications, selection events, lookup content.
- Property model/editor/sheet/outline tests: see
  [`13-property-sheet-and-editors.md`](13-property-sheet-and-editors.md#8-tests--acceptance).
- UI tests (headless glass): view adapters (observable list/selection), property sheet binding, node
  context menu mapping; manual matrix for tree/tree-table/table on the same model.
- Migration acceptance: navigator, Structure and Properties behave identically to today; the Properties
  window shows node property sets; context menus come from node actions; one model drives several views.
- `ant check-purity` passes with `nbfx.nodes` in the model guardrail (no JavaFX imports).

## Risks

- **Refactor breadth**: three working views (navigator, Structure, Properties) move to the model;
  mitigate by making the model/views additive first and migrating one view per branch/PR.
- **Async children + FX thread discipline**: fold the navigators' existing loading/cancellation into
  `ChildFactory` instead of duplicating it.
- **Action-id coupling**: the model carries ids; the UI resolves them — keep the id/command vocabulary in
  `ActionIds` and document it.
- **Property editor / sheet / outline parity**: the existing editors must keep working; the specific
  risks (aggregation, virtualized cell editing, commit/revert, async, identity contract, listener leaks)
  are listed in [`13-property-sheet-and-editors.md`](13-property-sheet-and-editors.md#9-risks).
- **Decorations/badges** (VCS status, errors) need a model hook (icon badges / HTML name).
- **`nbfx.api` inconsistency**: `OpenProject`/`ProjectRegistry` use JavaFX observables; the node model
  deliberately does not. Future API models should follow the node-model rule.
