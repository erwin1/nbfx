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
# 13 — Property model, editors, property sheet and outline columns

> **Naming (doc 32).** Target SPI/API names are **capability names**, never toolkit names: `OptionsPanelController`, `WizardIterator`, `ViewRegistration`, `PropertyEditor`, `ProjectModel`. `Fx*` names below refer to **current code** unless explicitly marked *target*; the rename (`refactor/api-naming`) happens **before** the annotation/API splits. Published-plugin tier and conventions: [`32-api-tiers-and-plugin-contract.md`](32-api-tiers-and-plugin-contract.md).

Step: **Property sheet & editors** · branch: `feature/property-sheet-editors` · status: planned,
depends on [`12-nodes-and-views.md`](12-nodes-and-views.md) (`feature/nodes-api`); feeds the
`refactor/views-on-nodes` migration.

## Why this is a step of its own

The property sheet and the outline/tree-table are **two renderings of the same node properties**:

- NetBeans' `OutlineView` takes its non-tree columns from `Node.Property` names
  (`setPropertyColumns(String... namesAndDisplayNames)`, `OutlineView.java:631`) and resolves each column
  against the node's property sets; `TreeTable` does the same for the table part.
- The Properties window renders those same `Node.PropertySet`s for the current selection — including
  several nodes at once (aggregation), expert mode and custom editors.

If the model only serves the sheet, the outline grows a second model and the two drift — which is exactly
today's nbfx situation. The whole node work therefore has one crisp acceptance:

> **One property model. An edit in the property sheet is visible in the outline cell, and an edit in an
> outline cell is visible in the property sheet.**

That goal drives the editor SPI (shared by both surfaces), column binding, multi-selection aggregation and
the validation/commit protocol. It is worth its own step so the risk is reviewable in isolation.

## What nbfx has today

| Piece | Where | Limitation for the outline/sheet goal |
|---|---|---|
| `FxProperty` / `FxPropertySet` / `SimpleFxProperty` / `SimpleFxPropertySet` | `nbfx-ide/...nbfx.propertysheet` | interfaces carry `javafx.beans.value.ObservableValue valueObservable()` (not core-movable); a set is a flat list; no stable identity contract beyond `name`, no editor id, allowed values, format/sort, async or validity state |
| `FxPropertyEditor` / `FxPropertyEditors` | same module | `@ServiceProvider` matched **only** by `valueType`; `Node createEditor(FxProperty)`; no text `parse`/`format`, no custom-editor lifecycle, no `PropertyEnv`, no commit/validation protocol |
| `FxPropertySheet` | same module | Name/Value `TreeTableView`, expert checkbox, one `Object` context → `FxPropertiesProvider`; no multi-selection, no async states, no column binding, no persisted layout |
| `FxPropertiesProvider` (+ project/file/editor-document providers) | same module | context-object → sets; must become node property contributors |
| Navigator / Structure views | `project.ui.java` (`ProjectNavigatorImpl`, `FileNavigatorImpl`, `AbstractNavigatorProvider`), `structure` | plain `TreeView<T>`; no property columns and no in-cell editing |

## 1. `PropertyModel` contract (model, `nbfx.nodes`)

Add to the node model (see [`12-nodes-and-views.md`](12-nodes-and-views.md#target-architecture)):

```java
interface PropertyModel {
    String getName();                  // stable identity — PropertyColumn references this; document it
    String getDisplayName();
    String getShortDescription();      // tooltip / help panel

    Class<?> getValueType();
    Object getValue();                 // null is a legal value (and the "mixed" value)
    void setValue(Object value) throws IllegalArgumentException;  // message is user-visible
    boolean isWritable();
    boolean isExpert();
    boolean isPreferred();
    Object getDefaultValue();
    boolean isDefaultValue();

    List<?> getAllowedValues();        // non-null/empty = combo (enum-like)
    String getEditorId();              // explicit editor; null = resolve by type/allowed values -> replace by PropertyEditorRef (doc 30 §4)
    boolean canEditAsText();           // false = custom editor only (NetBeans "canEditAsText")
    boolean supportsCustomEditor();    // NetBeans "suppressCustomEditor" is the inverse

    String format(Object value);       // text form for cells and text editors
    Object parse(String text) throws IllegalArgumentException;
    Object getSortKey();               // outline sorting; default = getValue()

    LoadState getLoadState();          // READY / LOADING / ERROR
    String getErrorMessage();

    void addPropertyChangeListener(PropertyChangeListener l);
    void removePropertyChangeListener(PropertyChangeListener l);
}
```

`PropertySetModel`: stable `name`, `displayName`, `shortDescription`, `isExpert()`,
`List<PropertyModel> getProperties()`, change listeners.

Rules that keep the sheet and the outline coherent:

- **Identity is `name`.** Columns persist/compare by `(setName, propertyName)`; a renamed property breaks
  saved columns — state that as a contract and keep names stable.
- **One change path.** Value/state/writability/validity changes fire as property-change events
  (`java.beans`-style; no JavaFX in the model); the UI adapter turns them into `ObservableValue` /
  `ObservableList` for cells and sheet rows.
- **Formatting/parsing is model-provided** (`format`/`parse`), so a text cell, an inline text editor and a
  column renderer all agree on the representation.
- **Errors are values, not silent failures**: `setValue` throws `IllegalArgumentException` with a
  user-visible message; the editor shows it and keeps the previous value.
- **Async is explicit**: a property can be `LOADING` (the sheet shows a spinner, the outline a
  placeholder) or `ERROR` (message/tooltip); nothing blocks the FX thread.

The JavaFX `ObservableValue` member of `FxProperty` is **dropped** from the model; `SimpleFxProperty` /
`SimpleFxPropertySet` become model-side simple implementations with `PropertyChangeSupport`, and the FX
adapter (step 2) supplies observables to the UI.

## 2. Multi-selection aggregation (model + sheet)

The sheet (and any bulk outline action) must handle N selected nodes:

```java
AggregatePropertyModel implements PropertyModel   // merges same-name properties across nodes
    boolean isMixed();                            // values differ → "<mixed>" (rendered by the UI)
    boolean isMixedWritable();                    // writable only if all are writable
    // setValue applies to all; partial failure → aggregate error message, no silent half-update
```

- Property **sets** merge by name (`AggregatePropertySetModel`); a property missing from one node is
  shown as missing/disabled, not silently dropped.
- Aggregation lives in the model (testable, headless); the `"<mixed>"` string and disabled styling are
  UI concerns.
- NetBeans analogue: `PropertySheet`/`Sheet` with several nodes selected.

Acceptance: selecting two nodes shows the common properties once; editing applies to both; mixed values
render distinctly; a read-only node makes the aggregate property read-only.

## 3. Editor SPI and built-ins (UI, `nbfx.nodes.ui`)

Both the sheet row and the outline cell use the same editor stack:

```java
interface PropertyEditor {                 // UI SPI, @ServiceProvider
    boolean supports(PropertyModel p);
    Node createInlineEditor(PropertyModel p, PropertyEnv env);
    Node createCustomEditor(PropertyModel p, PropertyEnv env);   // null = no custom editor
}

interface PropertyEnv {                    // NetBeans PropertyEnv analogue
    Object getValue(); void setValue(Object v);
    boolean isEditable(); boolean isValid(); void setState(State s, String message);
    void addChangeListener(...);           // custom-editor OK/Cancel lifecycle
}
```

- **Resolution order** in `PropertyEditorRegistry`: explicit `editorId` → exact/assignable `valueType` →
  `allowedValues` (combo) → text fallback → read-only label. Register `editorId`s for special editors
  (e.g. `"path"`, `"color"`, `"classpath"`).
- **Built-ins** (promote the existing ones): text (`String`, `char`, `canEditAsText`), boolean (checkbox),
  number (`int`/`long`/`float`/`double`; `format`/`parse`, invalid input keeps focus and shows the error),
  enum/combo (`allowedValues`, editable combo when `canEditAsText`), file/folder (text + chooser),
  color (JavaFX `ColorPicker`), custom dialog (via `editorId` → `createCustomEditor`).
- **Inline lifecycle**: `startEdit` / `commitEdit` / `cancelEdit`; commit on Enter and on focus loss;
  Esc reverts; validation styles use theme tokens (works in light/dark); dispose listeners and editors
  when the row/cell is recycled or the node disappears (TreeTableView virtualization).
- **Custom editors**: reuse the `nbfx.dialogs`/wizard dialog host (owner window), OK/Cancel gated by
  `PropertyEnv.isValid()`, dispose on close; no layout jump in the sheet while the dialog is open.
- Existing `TextPropertyEditor`, `BooleanPropertyEditor`, `NumberPropertyEditor`, `EnumPropertyEditor`
  and `FxPropertyEditor` are moved/renamed onto this SPI; no behaviour change until migrated.

## 4. Property sheet UI (`nbfx.propertysheet`, on the node model)

- **Structure**: `TreeTableView` — property **sets** as expandable groups, properties as rows; columns
  **Name**, **Value** (editor cell), optional **Description** (or a bottom help panel bound to the
  selected row, like NetBeans).
- **Modes**: expert on/off; grouped (sets) vs flat; sort by name; remember expanded sets, column
  order/widths and sort per user (`NbPreferences`, keyed by view/customizer id).
- **Multi-selection**: aggregated sets/properties; `"<mixed>"` value; editing writes to all; read-only
  aggregate when any node is read-only.
- **Editors**: inline when `canEditAsText`, otherwise a `…` button opening the custom editor; a row shows
  loading (spinner) / error (message) states.
- **Refresh**: rebuild on selection/context change; subscribe to each node's and property's events and
  **unsubscribe** from the previous selection (weak listeners to avoid leaks); a property event updates
  only its row.
- **Keyboard**: F2/Enter starts editing, Esc cancels/reverts, Tab/arrows navigate, typing starts editing.
- **Empty states**: "No properties" / "Multiple objects selected" (localized).

`FxPropertySheet` becomes the implementation of this view (renamed `PropertySheet`); the
`Project/File/EditorDocumentPropertiesProvider`s become node property contributors in the migration step,
with the old `FxPropertiesProvider` kept as a thin adapter until all callers (customizers, FXML inspector)
are migrated.

## 5. Outline / tree-table columns (`nbfx.nodes.ui`)

- **Column model** (view configuration, not the property model):

```java
final class PropertyColumn {
    String propertySet;     // null = search all sets
    String propertyName;    // stable PropertyModel name
    String title;
    double width;
    boolean editable; boolean sortable;
    Comparator<?> comparator;   // null = model sortKey/format order
    String editorId;            // optional override
}
```

- **Component**: `OutlineView` over an `ExplorerContext` — column 0 = node display name + icon/badges,
  the rest = `PropertyColumn`s. A property that is missing on a node renders an empty, non-editable cell.
- **Cells**: read the property (or a computed cell value), render `format(value)`, edit **inline with the
  same editor registry** and write back through `setValue`; `LOADING`/`ERROR` render as the sheet does.
- **Sorting**: by `getSortKey()` (fallback: formatted value); multi-column sort later.
- **Propagation**: property change events refresh the cell (adapter → `ObservableValue`) and the sheet row
  simultaneously — this is the acceptance proof for the shared model.
- **Default columns are view-owned**, not model-owned: e.g. the Files view supplies
  Name / Type / Size / Last Modified / Path; the project navigator supplies its own set. Column
  order/widths/sort persist per view id.
- **Performance**: `TreeTableView` virtualization — no per-cell node/editor creation; editors are created
  only while a cell is editing and disposed on cell reuse.

## 6. Threading

- Property values may be computed in the background: `LOADING` → `READY`/`ERROR` via events; the adapter
  applies values on the FX thread.
- Cancel outstanding work when the selection/node changes (sheet) or the row is recycled (outline); never
  block the FX thread on a property getter.

## 7. Placement, ordering, parallelism

- **Step 2**: `feature/property-sheet-editors` — `PropertyModel` additions (aggregation, states),
  editor SPI + built-ins, the property sheet, `PropertyColumn` + outline cell binding, and the
  `nbfx.propertysheet` migration. **Depends on** `feature/nodes-api`; **before**
  `refactor/views-on-nodes`.
- **Parallel safety**: additive to `nbfx.nodes` / `nbfx.nodes.ui` / `nbfx.propertysheet`; it does not
  touch the navigators or Structure (those move in the migration step).
- The migration step then uses this stack for the Properties window, the navigator Files view columns and
  the Structure view, and removes the parallel `FxProperty*` model.

## 8. Tests & acceptance

- **Model** (headless): identity/name stability, value + writable/expert/default, allowed values,
  `format`/`parse` round-trip, `setValue` error contract, `LOADING`/`ERROR` transitions, event delivery,
  aggregation (mixed value, mixed writability, apply-to-all, partial failure).
- **Editors** (headless glass): text/number/boolean/enum parse-format, invalid input (error shown, old
  value kept), custom-editor `PropertyEnv` validity gating, listener disposal on cell recycle.
- **Sheet**: set grouping, expert toggle, multi-selection aggregation, commit/cancel/revert, refresh on
  property events, persistence of layout.
- **Outline**: column binding by `(set, property)`, formatted rendering, sorting, inline edit → sheet
  update, missing property, node removed while editing.
- **Manual matrix**: sheet ↔ outline sync, light/dark (theme tokens), two-node selection, custom editor,
  async property, editor in a recycled cell.
- **Step acceptance**: property sheet and outline work off one property model; editing in either is
  reflected in the other; no JavaFX in `nbfx.nodes`.

## 9. Risks

- **Aggregation semantics** (mixed/invalid/partially writable) — define them in the model and test them.
- **Inline editing in a virtualized TreeTableView** — cell reuse vs editor lifecycle; dispose on
  `updateItem(null/…)` and on node removal.
- **Commit vs revert** — focus-loss commit, Esc revert, validation styling; keep the old value on error.
- **Identity contract** — columns and persisted layout reference `(set, property)` names; document
  stability and fail soft when a property disappears.
- **Async + cancellation** — stale values must not overwrite a newer selection.
- **Custom editors** owning windows and listeners — dispose on close and on selection change.
- **Listener leaks** — sheet and outline both listen; unsubscribe from the old selection, prefer weak
  listeners.
- **Performance** — many properties/nodes: no eager editors, no per-cell model copies.
- **Parity** — expert/default/read-only semantics and NetBeans hints (`canEditAsText`,
  `suppressCustomEditor`, `title`); grouped sets vs tabs (recommend expandable groups).
- **Temporary double model** — keep `FxProperty*` as an adapter only until every caller is migrated; do
  not grow new features on it.
