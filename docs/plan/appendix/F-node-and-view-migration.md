# Appendix F — Node and view migration

`PLAN.md` §5.1 asserts "one model, many views". The tree has **32 files across 18 modules that mention a
`TreeView`/`TreeTableView`/`TableView`/`ListView`, 24 of them constructing one** ([`../figures.md`](../figures.md)), and at least five independent item models. Without an
inventory, M1 is unschedulable. This is the inventory and the migration order.

## F.1 The views today

| View file | Control | Today's item model | Target |
|---|---|---|---|
| `structure.java/…/ui/ClassMemberPanelUI` | `TreeView` | `structure.java` `MemberNode` + `MembersScanner` | `api.nodes` `NodeModel` over a `java.structure` model |
| `structure.java/…/ui/BeanPanelUI` | `TreeView` | `BeanPattern`/`BeanPatternAnalyser` | `api.nodes` `NodeModel` |
| `structure.java/…/ui/ImplementationsPopup` | `ListView` | `InspectedType` | `api.nodes` children list |
| `structure.css/…/CssStructureUI` | `TreeView` | CSS rule rows (`CssScanner`) | `api.nodes` `NodeModel` |
| `structure.xml/…/XmlViewUI` | `TreeView` | XML nodes (`XmlNode`) | `api.nodes` `NodeModel` |
| `findusages/…/ui/UsagesQueryPane` | `TreeView` | `findusages` `UsageNode`/`UsagesModel` | `api.nodes` `NodeModel` + `api.usages` results |
| `findinprojects/…/ui/SearchResultsPane` | `TreeTableView` | search match rows | `api.nodes` `NodeModel` with column `PropertyModel`s |
| `findinprojects/…/ui/IssuesDialog` | `TableView` | problem rows | `platform.nodes` table over `api.nodes` |
| `project.ui.java/…/ProjectNavigatorImpl` | `TreeView` | `ProjectEntry` + `ProjectKindProvider` | `api.nodes` `NodeModel` + `api.project` |
| `project.ui.java/…/FileNavigatorImpl` | `TreeView` | `FileTreeItem` | `api.nodes` `NodeModel` over `FileObject` |
| `propertysheet/…/FxPropertySheet` | `TreeTableView` | `FxProperty`/`FxPropertySet` | `api.nodes` `PropertyModel`/`PropertySetModel` + `api.property` editors |
| `debug/…/DebugView` | `ListView` | debug frames/variables | `api.nodes` tree/table over `api.debug` |
| `vcs/…/GitStatusView` | `TableView` | VCS status rows | `platform.nodes` table over `api.vcs` |
| `plugins/…/PluginsDialog` | `TableView` | plugin rows | `platform.nodes` table |
| `options/…/FxOptionsDialog` | `ListView` | option categories | `platform.nodes` list |
| `options/…/KeyMapOptionsPanel` | `TableView` | keymap rows | `platform.nodes` table |
| `project.customizer/…/FxProjectCustomizerDialog` | `TreeView` | customizer categories | `platform.nodes` tree |
| `editor/…/completion/CompletionPopup` | `ListView` | `CompletionItem` | **not `api.nodes`** — a popup is not an explorer; keep its own list model |
| `editor/…/breadcrumbs/BreadcrumbsPopup` | `ListView` | breadcrumb elements | keep its own model |
| `findinprojects/…/ui/IgnoreListDialog` | `ListView` | ignore patterns | `platform.nodes` list (small) |
| helper/cell/tool files (`NavigatorTreeCell`, `TreeCellDnD`, `TreeScrolling`, `UsageTreeCell`, `SearchTextCombo`, `FxOptionsDialog` cell, `ProjectTreeItem`, and the CSS/XML structure cells) | — | per-view cell/tool classes | collapse into `platform.nodes` cell factories + drag-and-drop |

The table lists the principal views; [`../figures.md`](../figures.md) is authoritative for the file/constructor
counts (32 / 24 today).

Rule that falls out: **panels that are a tree/table of model objects use `api.nodes`; popups (completion,
breadcrumbs) keep their private list model** — they are not explorers and forcing them through the node model
is the over-abstraction [Appendix E.7](E-contracts.md) warns about.

## F.2 The target

- `api.nodes` (M): `NodeModel`, `ChildrenModel`/`ChildFactory`, `PropertyModel`/`PropertySetModel`,
  `ExplorerContext` (selection), `PropertyEditorRef` (opaque, [Appendix A](A-toolkit-policy.md)).
- `platform.nodes`: the FX views (`TreeView`, `TreeTableView`, `TableView`, `ListView`) that render a
  `NodeModel`, the cell factories, drag-and-drop, lazy children, and the property sheet.
- Every view above becomes **configuration + column definitions**, not a new class hierarchy; the per-view
  item classes (`ProjectEntry`, `MemberNode`, `UsageNode`, `FileTreeItem`, …) become `NodeModel`
  implementations or retire.

## F.3 Migration order (one branch each, smallest risk first)

1. `refactor/nodes-model` — introduce `api.nodes` + `platform.nodes`, port the **structure** view
   (`ClassMemberPanelUI`) as the proving ground; it is read-only and self-contained.
2. `refactor/nodes-usages` — usages + search results (read-only trees, `api.usages` already exists).
3. `refactor/nodes-navigator` — the project navigator (+ `FileNavigatorImpl`), the riskiest (drag-and-drop,
   rename/delete hooks in `utils/DeleteActions`, `utils/RenameActions`).
4. `refactor/nodes-properties` — the property sheet onto `PropertyModel`/`api.property`.
5. `refactor/nodes-misc` — VCS status, plugins, options/keymap, customizer, debug, issues.

Acceptance for each: the view file has no item model of its own, no `TreeView` construction outside
`platform.nodes`, and the same behaviour (unit/UI test). Prefix the workstream in W3/W4 and make the node
model a W3 platform service so W4 depends on it.

## F.4 What "one model" buys (and what it must not cost)

Buys: one selection/context implementation (`ExplorerContext`), one lazy-children contract, one
drag-and-drop story, one property/editor model shared by the sheet, outline columns and customizers, and a
single place to enforce accessibility ([Appendix E.3](E-contracts.md)).

Costs to avoid: a node model that must be reimplemented per view is a failure; if a view cannot be expressed
as a `NodeModel` tree/table/list in a few lines, that view keeps its private model (popups). The rule is
"explorer-like → `api.nodes`; popup/overlay → private".
