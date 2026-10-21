# 14 — Options system (NetBeans Options port)

> **Naming (doc 32).** Target SPI/API names are **capability names**, never toolkit names: `OptionsPanelController`, `WizardIterator`, `ViewRegistration`, `PropertyEditor`, `ProjectModel`. `Fx*` names below refer to **current code** unless explicitly marked *target*; the rename (`refactor/api-naming`) happens **before** the annotation/API splits. Published-plugin tier and conventions: [`32-api-tiers-and-plugin-contract.md`](32-api-tiers-and-plugin-contract.md).

Step: **Options system: infrastructure + example** · branch: `feature/options-foundation` · status:
planned. The parity work is sliced into per-category steps (below); the settings browser, export/import
and Quick Search are separate steps.

## Why

The NetBeans Options window is the per-application settings UI: category list + sub-panels, a keyword
search, an OK/Apply/Cancel lifecycle with changed/valid tracking, and deep links
(`OptionsDisplayer.open("Editor/Formatting")`). nbfx already has a **stub** (`nbfx.options`,
`FxOptionsDialog`, `FxOptionsPanel`, `OptionsRegistry`, two panels) and the registration annotations,
but the stub is flat: no containers/tabs, no search, no change/validity, no settings abstraction. This
plan completes it as a pure-JavaFX system and ports the categories in steps.

## Research: how NetBeans does it

Evidence from `platform/options.api` (`org.netbeans.spi.options` / `org.netbeans.modules.options`):

| Piece | Reference | Notes |
|---|---|---|
| Panel contract | `spi/options/OptionsPanelController.java` | `getComponent(Lookup masterLookup)` (called before `update()`), `update()`, `applyChanges()` (**off EDT**), `cancel()`, `isValid()`, `isChanged()`, `getLookup()`, `getHelpCtx()`, `add/removePropertyChangeListener` (`PROP_VALID`/`PROP_CHANGED`/`PROP_HELP_CTX`), `handleSuccessfulSearch(text, matched)`, `setCurrentSubcategory(path)` |
| Category metadata | `spi/options/OptionsCategory.java` | `getCategoryName`, `getTitle`, `getIcon`, `create()` |
| Sub-panel metadata | `spi/options/AdvancedOption.java` | a tab inside a container |
| Displayer | `api/options/OptionsDisplayer.java` | `open()`, `open(path)`, `open(path, modal)`, `isOpen()`; ids `ADVANCED`, `KEYMAPS`, `FONTSANDCOLORS`, `EDITOR`, `GENERAL`; path `category/subcategory` |
| Registrations | `OptionsPanelControllerProcessor.java` | `@TopLevelRegistration`, `@ContainerRegistration` (package), `@SubRegistration` (`location` = container id, default `Advanced`), `@Keywords`/`@KeywordsRegistration` → layer `OptionsDialog/…` + `OptionsDialog/Keywords/…`; attrs `categoryName`/`iconBase`/`keywords`/`keywordsCategory`/`position`/`advancedOptionsFolder` |
| Window | `modules/options/OptionsDisplayerImpl.java` (650) | OK/Apply/Cancel (+ "Advanced Options" classic, Export/Import when non-empty); Apply enabled when changed; OK gated by validity; applies off-EDT with progress |
| Window body | `modules/options/OptionsPanel.java` (1413) | category list + tabbed content + quick-search field; filters **categories and tabs** by keywords; dispatches `handleSuccessfulSearch`; remembers the current category; help |
| Category model | `modules/options/CategoryModel.java` (437) | discovers categories from `OptionsDialog` + `Lookup`; master lookup; aggregates `isChanged`/`isValid`/`save`/`cancel`; current category |
| Containers | `modules/options/TabbedController.java` (323) | container = tabs; lazily creates a sub-panel on first selection; aggregates validity/change; forwards search |
| Advanced | `advanced/AdvancedPanel.java` + `advanced/Model.java`; layer `Advanced.instance` | the Miscellaneous/Advanced container |
| Search provider | `modules/options/QuickSearchProvider.java` (177) | feeds the IDE Quick Search (`o …`) → `OptionsDisplayer.open(path)` |
| Export/import | `modules/options/export/OptionsExportModel.java` (1204) + `OptionsChooserPanel.java` (817) | file-based: copies **userdir paths** matching declarative `OptionsExport/<category>/<item>` include/exclude patterns into/out of a zip (`build.info`, `enabled-items.info`); import accepts a zip **or a userdir**; keyring/passwords excluded |
| Legacy tree | `modules/options/classic/*` (the "Advanced Options" button) | old Options tree over the Settings/Services filesystem |

Category inventory (65 registrations IDE-wide: 4 top-level, 8 containers, 53 subs). Java/JavaFX-relevant
subset — see the parity matrix in §7.

## What nbfx has today

`nbfx-platform/c.gluonhq.netbeans.nbfx.options` (cluster `nbfx-platform`):

- `FxOptionsPanel` — `getId/getDisplayName/getCategory/getPosition/getComponent/load/apply/cancel/isValid`
- `OptionsRegistry` — reads a **flat** `NbFx/Options` folder only (no containers/sub-tabs/icons/keywords)
- `OptionsRegistration` — record (id, displayName, category, position, panel)
- `FxOptionsDialog` — category `ListView` + one panel; OK/Apply/Cancel applies **every** panel if valid;
  no tabs, no search, no change tracking, no error markers
- panels: `EditorOptionsPanel` (line numbers/breadcrumbs via `EditorSettings`), `KeyMapOptionsPanel`
  (commands table via `KeyMap`)
- `OptionsCommandsProvider`/`SelectOptionsCommand` (`ActionIds.SELECT_OPTIONS`, `Shortcut+,`) and
  `OptionsMenuRegistrations` (`Menus/Tools`)
- annotations `@FxOptionsRegistration` / `@FxOptionsContainerRegistration` / `@FxOptionsSubRegistration`
  + `FxOptionsRegistrationProcessor` already generate `NbFx/Options/…` layer entries
  (`FxLayer.OPTIONS`)

Persistence is direct `NbPreferences` (`KeyMapImpl`, `AppState`, `ViewStateStore`, `FindSettings`,
`UsagesSettings`, `NavigatorSettings`); `EditorSettings` is an in-memory service filled by the launcher.
There is no shared settings abstraction and no model module for settings.

Purity already forbids `javax.swing`/`java.awt`/`javafx.embed.swing`/`openide.awt|nodes|explorer|
windows|dialogs` in nbfx sources/deps, so the NetBeans options SPI (Swing `JComponent`) cannot be reused
and there is no Swing bridge.

## Decisions

1. **No Swing, no `org.netbeans.modules.options.api` dependency**; new nbfx SPI returning
   `javafx.scene.Node`. Do not embed panels via `SwingNode`.
2. **Keep `nbfx.options` as one UI module** (shell + SPI + registry + search + settings binding). No
   model/UI split: the SPI returns a JavaFX `Node`, so the reusable core-only part is the **settings
   data**, not the panel contract. (Recorded as the documented exception to the model/UI rule.)
3. **Settings layer: a new `nbfx.settings` model module** (core-only, JavaFX-free, `check-models`-
   guarded) with a typed `Settings` facade over `NbPreferences`; the FX binding helper lives in the UI
   (`nbfx.options`). Features and panels read the same keys.
4. **Infrastructure + example first, parity stepwise.** The foundation step lands the shell, SPI,
   registry, settings module and the **Editor (container) + Keymap** panels as the working examples;
   each further category is its own step with its own branch and acceptance. The parity matrix (§7)
   marks every panel as *first pass*, *later* or *dropped*, so no panel ships dead.
5. **Name it `OptionsPanelController`** (capability name; doc 32) and adopt the NetBeans method contract (`getComponent(Lookup)`,
   `update`, `applyChanges`, `isChanged`, `getLookup`, `getHelpCtx`, `handleSuccessfulSearch`,
   `PROP_CHANGED`/`PROP_VALID`).
6. **Containers and sub-panels are first-class** (`@OptionsContainerRegistration` +
   `@OptionsSubRegistration`), mirroring `TabbedController`.
7. **Change/validity is event-driven** with a polling fallback before OK/Apply; OK is blocked while any
   panel is invalid and shows an error marker on its category/tab.
8. **Apply runs off the FX thread** through the progress API when a panel reports slow work.
9. **Panels live in the owning feature module** (mirroring `ide/options.editor`, `platform/options.keymap`,
   `extide/options.java`): `nbfx.theme` → Appearance, `nbfx.editor` → Editor/Fonts & Colors,
   platform/Maven/Gradle/Ant modules → Java, etc. `nbfx.options` must not depend on every feature.
10. **Advanced settings browser as a development aid**, behind a show/hide toggle (hidden by default).
11. **`EditorSettings` migrates**: the existing `nbfx.api.editor.EditorSettings` (`BooleanProperty`) moves to
    `nbfx.settings` (M, toolkit-free) + a UI adapter in `refactor/module-moves`; the editor and launcher
    consume the new service (doc 30 §1.5).
11. **Export/import, including importing an export from the original NetBeans**, is its own step.

## Target architecture

### SPI (`nbfx.options`)

```java
public interface OptionsPanelController {
    String getId();
    Node getComponent(Lookup masterLookup);
    void update();
    void applyChanges();
    void cancel();
    boolean isValid();
    boolean isChanged();
    default Lookup getLookup() { return Lookup.EMPTY; }
    default void handleSuccessfulSearch(String searchText, List<String> matchedKeywords) {}
    default void setCurrentSubcategory(String path) {}
    default HelpCtx getHelpCtx() { return null; }   // no JavaHelp in nbfx; may become a no-op
    void addPropertyChangeListener(PropertyChangeListener l);   // PROP_CHANGED / PROP_VALID
    void removePropertyChangeListener(PropertyChangeListener l);
}
```

`PROP_CHANGED = "changed"`, `PROP_VALID = "valid"`.

### Registry / model

- `OptionsCategoryDescriptor` (id, displayName, iconName, keywords, keywordsCategory, position,
  container, sub-panels).
- `OptionsRegistry` reads `NbFx/Options` (files = single panels, folders with `container=true` =
  containers, their children = sub-panels), merged with Lookup instances, ordered by `position`
  (`Advanced` by display name), resolving `#key` bundle names/icons.
- `OptionsSearch` — all-words keyword matching per category and per sub-panel; returns the target path
  and the matched keywords.
- `OptionsWindowModel` — current category/sub, master lookup, `isChanged()`/`isValid()` aggregation,
  `save()`/`cancel()`.
- `OptionsDisplayer` (Lookup service + static facade) — `open()`, `open(String path)`,
  `open(String path, boolean modal)`, `isOpen()`, remembers the last category.

### Window (`OptionsWindow`)

- Search field + category list (icons, separators, themed selection/hover) on the left; content
  (single panel, or `TabPane` for a container) on the right; bottom bar OK / Apply / Cancel + Help.
- Lazy panel creation; `getComponent(masterLookup)` before `update()`; Apply enabled iff changed; OK
  blocked iff invalid (+ error icon/tooltip); Esc/close = cancel (confirm when changed); apply
  off-thread with progress and refresh; dispose on close.
- Search filters categories/tabs, selects the first match and dispatches `handleSuccessfulSearch`;
  Esc clears.
- Reuse `nbfx.dialogs` for chrome and `nbfx.theme` for `options.css`.

### Annotations / processor (fixes)

- Keep `@OptionsRegistration` (top-level single panel), `@OptionsContainerRegistration` (package),
  `@OptionsSubRegistration` (`location`).
- Processor: also write `keywords`/`keywordsCategory` for sub-panels; keep the container folder +
  `container` attribute; add `@OptionsKeywords` (repeatable) for panel-level search keywords
  (NetBeans `@Keywords`).

### `nbfx.settings` (model, core-only)

```java
public interface Settings {
    String  getString(String key, String def);
    int     getInt(String key, int def);
    boolean getBoolean(String key, boolean def);
    void putString/putInt/putBoolean(...);
    void reset(String key);          // back to default
    boolean isDefault(String key);
    Set<String> keys();              // for the browser and export
    void addChangeListener(SettingsListener l);   // java.beans-style; no JavaFX
    void removeChangeListener(SettingsListener l);
}
public final class NbSettings implements Settings {  // over NbPreferences
    public static Settings forModule(Class<?> moduleClass);
    public static Settings forPath(String path);
}
```

- The FX binding helper (`SettingsBindings`: `BooleanProperty`/`IntegerProperty`/`StringProperty` ↔ key,
  with listener cleanup) lives in `nbfx.options`, keeping `nbfx.settings` JavaFX-free.
- Key convention: `nbfx.<feature>.<key>` mapped onto `NbPreferences` nodes, documented per feature;
  panels and headless feature code use the same keys.
- Existing call sites (`KeyMapImpl`, `AppState`, `ViewStateStore`, `FindSettings`, `UsagesSettings`,
  `NavigatorSettings`, the launcher's editor settings) migrate opportunistically, not in the foundation
  step.
- `nbfx.settings` is added to the `check-models` guardrail (the guardrail lands with the templates work;
  if it is not merged yet, the foundation step adds `nbfx.settings` to it).

### Advanced settings browser (debug aid)

- `PreferencesBrowserPanel` — a `TreeView` over the `NbPreferences` tree (node/key/value/type columns),
  filter field, add/edit/remove, live refresh; value editing later reuses the property-sheet editors
  (doc 13). Also shows the registered categories/panels/keys, which makes options development much
  easier.
- **Show/hide toggle**: hidden by default. Shown when the developer JVM flag `-Jnbfx.options.debug=true`
  is set or when the user enables "Show advanced preferences" inside the Advanced category; the choice
  is persisted in `nbfx.settings`. The `Advanced` category itself is shown when it has contributed
  panels or when the browser is enabled (mirroring NetBeans' `advancedOptionsNotEmpty()`).

## Parity matrix

Status: **first pass** = ported in the foundation/early steps; **later** = planned, explicitly not
shipped yet; **dropped** = not applicable to nbfx.

| Category | Panel | Status |
|---|---|---|
| General | General (proxy, web browser) | later |
| Appearance | Theme (System/Light/Dark) | first pass (`feature/options-appearance`) |
| Appearance | Look and feel / Windows / Document tabs | dropped (nbfx has no LaF choice; own window system) |
| Editor | General (line numbers, breadcrumbs, caret, wrap) | first pass (foundation, the example) |
| Editor | Formatting / Indentation (tab size, expand tabs, indent) | first pass |
| Editor | Code Templates | later (needs editor support) |
| Editor | Folding | later (needs editor support) |
| Editor | On Save (trailing whitespace, format) | later |
| Editor | Hints / MarkOccurrences / InlineHints | later (only if the editor implements them) |
| Editor | Code Completion | later |
| Editor | Macros | later |
| Editor | AutoSave | later |
| Editor | Language Servers | later |
| Fonts & Colors | Syntax colour profiles (light/dark) | first pass (maps to `nbfx.theme` `-code-*` palettes) |
| Keymaps | Command shortcuts (exists) | first pass (migrated in the foundation) |
| Keymaps | Profiles, conflict detection, import/export | later |
| Java | Java Platform (JDK, sources/javadoc, JavaFX home) | first pass (`feature/options-java`, after Java Platforms) |
| Java | Maven (home, settings.xml, offline) | first pass |
| Java | Gradle (home, JDK) | first pass |
| Java | Ant (home, properties) | first pass |
| Java | SceneBuilder (kit/home) | first pass |
| Java | Java Debugger / JShell / Form Editor / Nashorn | later or dropped (no debugger/form editor) |
| Team | Versioning / ToDo / Issue Tracking | later (`feature/options-misc`) |
| Miscellaneous/Advanced | Preferences browser | first pass (debug aid) |
| Miscellaneous/Advanced | Output, Diff, Terminal, GoTo, File Associations, Janitor, Versioning, Spellchecker | later |
| Plugins | Plugins (separate dialog) | dropped (own `nbfx.plugins` dialog) |

## Steps

### A. Options foundation + example (`feature/options-foundation`) — doc 14

- `nbfx.settings` model module (`Settings`/`NbSettings`, `SettingsBindings` in `nbfx.options`), tests.
- SPI on the NetBeans contract + `PROP_CHANGED`/`PROP_VALID`; annotations/processor fixes
  (sub keywords, container attr, `@OptionsKeywords`).
- Registry: single panels, containers and sub-panels; bundle names/icons; ordering.
- Window: category list, tabs, keyword search + `handleSuccessfulSearch`, changed/valid aggregation +
  error markers, OK/Apply/Cancel (off-thread apply), deep links, last-category memory.
- `OptionsDisplayer` + `Options.open(path)` facade; Tools ▸ Options wiring unchanged.
- **Examples**: `Editor` as a *container* with a `General` sub-panel (line numbers, breadcrumbs, caret,
  wrap — bound through `Settings`), and `Keymaps` as a top-level panel (migrated to the new contract).
- `options.css` on `nbfx.theme`; dialog chrome on `nbfx.dialogs`.
- Docs: this file + step/branch/index updates.
- **Acceptance**: the window opens from Tools ▸ Options / `Shortcut+,`; Editor tabs switch; search finds
  and selects; changing a value enables Apply; an invalid panel blocks OK with an error marker; values
  persist via `nbfx.settings` and are read back; `nbfx.settings` passes `check-models`; purity green.

### B. Settings/preferences browser (debug aid) (`feature/options-settings-browser`)

- The `PreferencesBrowserPanel` in the Advanced category, with the show/hide toggle
  (`-Jnbfx.options.debug=true` / "Show advanced preferences", persisted).
- Acceptance: browse/filter/edit/remove preferences; the toggle works; hidden by default; used to
  inspect and debug the other panels.

### C. Appearance category (`feature/options-appearance`)

- `Appearance` container contributed by `nbfx.theme` with the `Theme` sub-panel (System/Light/Dark,
  live switch, `View ▸ Theme` stays as a shortcut). Depends on the theming step.
- Acceptance: the Theme panel switches and persists; the same state as `View ▸ Theme`.

### D. Editor + Fonts & Colors categories (`feature/options-editor`)

Ordered sub-steps (one branch, reviewable commits — or split later):

1. `Editor ▸ General` — extend the foundation example (caret/selection, wrap, highlight line) through
   `Settings`, with editor support.
2. `Editor ▸ Formatting/Indentation` — tab size, expand tabs, indent/continuation indent; honour them in
   the nbfx editor.
3. `Fonts & Colors` — colour profiles (light/dark) over the `-code-*` tokens; independent profile
   selection; preview.
4. `Editor ▸ Code Templates` — abbreviation table (if the editor supports templates).
5. `Editor ▸ Folding`, `On Save`, `Hints/MarkOccurrences/InlineHints`, `Code Completion`, `Macros`,
   `AutoSave`, `Language Servers` — each only when the corresponding editor feature exists, else keep
   them out of the matrix ("later").

- Acceptance per panel: values persist and visibly affect the editor; invalid input is blocked.

### E. Java/JavaFX category (`feature/options-java`)

`Java` container: Java Platform (add/remove/rename, sources/javadoc, JavaFX home — from the Java
Platforms step), Maven, Gradle, Ant, SceneBuilder. Depends on the Java Platforms step and the build
modules. Debugger/JShell/Form Editor/Nashorn stay out (dropped/later).

### F. Miscellaneous/Advanced + Team (`feature/options-misc`)

- Advanced container: the settings browser (B) plus contributed tabs (Output, Diff, Terminal, GoTo, File
  Associations, Janitor, Versioning, Spellchecker, …) as those features land.
- Team container: Versioning, ToDo, Issue Tracking.

### G. Export/import options (`feature/options-export`)

- Mirror the NetBeans mechanism: declarative `NbFx/OptionsExport/<category>/<item>` entries with
  include/exclude patterns over **userdir paths**; export builds a zip with a `build.info` /
  `enabled-items.info` manifest; import accepts an nbfx zip **or a userdir**, excludes keyring/password
  files, and asks for a restart where the setting is read at startup.
- **Import from the original NetBeans**: point the importer at a NetBeans options export zip (or a
  NetBeans `config` folder); the shared headless modules (editor preferences, Maven/Gradle settings, Java
  platforms, keymap) map path-for-path, and a per-category key map covers the rest, with a report of
  imported/skipped/unknown keys.

### H. Global Quick Search provider (`feature/options-quicksearch`)

- A `SearchProvider` (`nbfx.api.quicksearch` equivalent) that matches options keywords and opens
  `OptionsDisplayer.open(path)` (NetBeans `o …`), plus `handleSuccessfulSearch` highlighting.

## Tests & acceptance (system-wide)

- **Settings**: `NbSettings` round-trip, defaults/reset, change events, `keys()`; `SettingsBindings`
  two-way binding + cleanup.
- **Registry**: single/container/sub discovery from layer + Lookup, ordering, icons, bundle names,
  container flag; processor output for all three annotations.
- **Search**: all-words matching, category + tab filtering, matched-keyword reporting, reset.
- **Window**: Apply on change, OK blocked on invalid with error marker, save/cancel order (containers
  forward to all sub-panels), master lookup, deep link `open("Editor/Formatting")`, last-category
  memory, dispose.
- **Per panel**: values persist and the feature reads them back; defaults + reset; validation.
- **Purity**: `nbfx.options` has no forbidden imports/deps; `nbfx.settings` has no JavaFX
  (`check-models`).

## Risks / open items

- **Editor option depth**: Formatting/Hints/Code Templates are per-language and layer-driven in NetBeans;
  port the minimum that the nbfx editor honours and keep the matrix honest (no dead panels).
- **Data compatibility**: panels must write the same `NbPreferences` keys as the headless modules that
  consume them; document the key convention.
- **Async apply** + re-entrancy + partial failure.
- **Invalid-state UX** (error marker/tooltip on category/tab) needs a design.
- **Legacy NetBeans import** mapping is best-effort (unknown keys reported); the export format is
  path-based, so shared modules import cleanly while nbfx-specific keys need the key map.
- **Help**: no JavaHelp in nbfx; `getHelpCtx()` may be dropped.
- **Keymap profiles** (later) are a sizeable feature on their own.
