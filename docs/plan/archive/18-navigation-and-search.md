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
# 18 — Navigation & search (Java editor intelligence foundation)

Steps: **Editor intelligence foundation** (`feature/editor-intelligence-core`), **Go to / Quick Search**
(`feature/goto-search`), **Editor navigation & hyperlinks** (`feature/editor-navigation`), **Find Usages
parity** (`feature/find-usages-parity`), **Hierarchies** (`feature/hierarchies`).

## Why

The daily-driver gap in the current plan: no **Go to File/Type/Symbol/Line**, no global **Quick Search**,
no **Go to Declaration/Implementation/Overridden/Super**, no **Call/Type Hierarchy**. Find Usages exists
in nbfx but needs parity. Everything else in the Java-intelligence wave (refactoring, hints, codegen)
builds on this foundation.

## What nbfx has today

- Modules: `nbfx.editor` (code completion, breadcrumbs, syntax decoration, annotations), `nbfx.editor.actions`
  (cut/copy/paste/undo/redo/find/replace/save), `nbfx.findusages` (its own `UsagesQuery`, `UsageFilters`,
  `UsagesView`, tree cells, menu contributors), `nbfx.structure` + `nbfx.structure.java` (navigator),
  `nbfx.project.ui.api` (build actions), `nbfx.api` (views/actions/status).
- **The semantic foundation is already in the runtime and already a dependency**: `nbfx` depends on
  `org.netbeans.modules.java.source.base`, which contains the full `org.netbeans.api.java.source` surface
  — `JavaSource`, `WorkingCopy`, `CompilationController`/`CompilationInfo`, `SourceUtils`,
  `ElementUtilities`, `ClassIndex`, `TreeUtilities`/`TreeMaker`, `GeneratorUtilities`,
  `ModificationResult`, `ElementHandle`/`TreePathHandle`/`DocTreePathHandle`, `PositionConverter`,
  `TypeUtilities` — plus `parsing.indexing`, `java.lexer`, `java.project`, `api.java`,
  `api.java.classpath`.
- **Missing**: Go to…, Quick Search, go-to-declaration/implementation/overridden/super, hyperlinks,
  call/type hierarchy, usages parity (scopes/filters/actions).

## Research (original codebase)

| Module | Files / Swing | Role | Verdict |
|---|---|---|---|
| `ide/jumpto` | 55 / 27 | Go to File/Type/Symbol/Line, search history, settings, `jumpto.quicksearch` providers | **port the providers** (they use `ClassIndex`/`FileObject`, Swing-free logic) + FX popup |
| `platform/spi.quicksearch` | 21 / 14 | Quick Search SPI (`SearchProvider`, `SearchRequest`, `SearchResponse`) | **reuse the SPI** (headless interfaces) + FX popup; port the provider registrations |
| `java/java.navigation` | 44 / 39 | Go to Declaration/Implementation, Call/Type Hierarchy views, hyperlinks | **port the providers/models**, FX hierarchy views; the module itself pulls `openide.awt/nodes/windows` |
| `java/refactoring.java` (`callhierarchy/*`) | — | Call hierarchy model/actions | reference (shared with doc 19) |
| `ide/gototest` | 5 / 3 | Go to Test / Go to Source | **port** (small) |
| `java.source.base` (already a dep) | 50 / 30 | `ElementUtilities`, `SourceUtils`, `ClassIndex`, `WorkingCopy`, … | **reuse directly** (headless) |
| `parsing.indexing` (already a dep) | — | index queries (`QuerySupport`, `IndexingSupport`) | **reuse** for symbol/type search |

Notes: `TypeIndex`/`UsagesQuery` do not exist upstream — nbfx's `nbfx.findusages` already implements its
own usages query over `ClassIndex`/parsing; upstream Find Usages also goes through `ClassIndex`
(`getResources`/`getElements`). Keep the nbfx query and extend it.

## Target architecture

```
nbfx.editor.intelligence   (headless model, JavaFX-free)
  SemanticAccess     lifecycle of JavaSource/WorkingCopy (cancel, errors, off-FX executor)
  SourceOpener       FileObject + offset -> open/select in the editor (OpenSources integration)
  GotoProvider SPI   kind (TYPE|SYMBOL|FILE|LINE|PACKAGE|MEMBER), query, results with kind/icon/context
  GotoRegistry       Lookup aggregation of providers; recent searches; caching
  NavigationProvider declaration | implementation | overridden | super (ElementUtilities/SourceUtils)
  UsagesProvider     scopes (file/project/all projects), filters (read/write/kind), cancellation
  HierarchyProvider  call hierarchy (callers/callees), type hierarchy (supertypes/subtypes)
nbfx.editor.ui             (JavaFX)
  GotoPopup          keyboard-first popup: categories, incremental filtering, preview, history
  QuickSearchPopup   global search over registered spi.quicksearch providers
  HierarchyView      dockable TreeTableView (call/type)
  HyperlinkHandler   Ctrl/Cmd-click + hover in the code area
```

- All provider calls run on a background executor with cancellation; results are `FileObject`s + offsets
  opened through `OpenSources`.
- Actions/keymap: `Navigate` menu + editor context menu + `Quick Search` action (doc 14 keymap); the
  existing `nbfx.api.actions` command infrastructure.

## Steps

1. **Foundation** (`feature/editor-intelligence-core`) — `SemanticAccess` + `SourceOpener` + the provider
   SPIs; headless tests over fixture projects/JDK sources; document the `java.source.base` reuse.
2. **Go to / Quick Search** (`feature/goto-search`) — `TYPE`/`SYMBOL`/`FILE`/`LINE`/`PACKAGE`/`MEMBER`
   providers (port `jumpto` logic; `ClassIndex` for types/symbols); the FX popup (categories, incremental
   filter, preview, history, keyboard); register `spi.quicksearch` providers and add the Quick Search
   action.
3. **Editor navigation** (`feature/editor-navigation`) — declaration/implementation/overridden/super +
   Ctrl/Cmd-click hyperlinks + "Go to Source/Test" (`gototest`); mark occurrences integration (shared with
   doc 19's instant rename).
4. **Find Usages parity** (`feature/find-usages-parity`) — extend `nbfx.findusages`: scopes
   (file/project/all), filters (read/write/kind), flat/tree, actions (go-to, copy, exclude, refresh),
   results on the nodes/outline views (docs 12/13), correct progress/cancel.
5. **Hierarchies** (`feature/hierarchies`) — call hierarchy (callers/callees, filter, go-to) and type
   hierarchy (supertypes/subtypes) as dockable FX views.
6. **Actions & menus** — `Navigate` menu entries, editor context menu, shortcuts, enablement; help keys
   for the popups.

## Acceptance

- Ctrl/Cmd-click and "Go to Declaration/Implementation" navigate correctly (own code and libraries).
- Go to Type/Symbol finds JDK and project elements; Go to Line; Quick Search lists registered providers.
- Find Usages reports the same references as the original for a fixture project, with read/write filters.
- Call/Type Hierarchy expand/collapse and navigate; views are dockable and theme-aware.
- Headless tests for providers/queries; no JavaFX in the intelligence module (`check-models`).

## Risks / open questions

- **Index warm-up**: `ClassIndex` results are partial while scanning — the popup must show progress and
  refresh; cold-start search can miss types.
- **Threading/cancellation**: long queries (usages, hierarchy) must be cancellable and never block FX.
- **Result volume**: paging/limits for Go to Symbol and usages.
- **Semantic API availability**: `java.source.base` gives the API, but some features (e.g. specific
  implementation resolution) may need `java.source`/`java.navigation` extras — verify per provider.
- **Ctrl/Cmd-click conflicts** with the code area's existing selection handling.
