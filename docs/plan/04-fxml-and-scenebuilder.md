# 04 — FXML editing & embedded SceneBuilder

> Topic detail for milestone M3 (usage layer). Naming: doc [`32`](32-api-tiers-and-plugin-contract.md).

## `EditorDocumentProvider` (design/source editor prerequisite)

Add a pluggable document factory so the FXML editor can present a design/source view:

- **`nbfx.api` (nbfx-platform):**
  ```java
  public interface EditorDocumentProvider {
      boolean accepts(FileObject file);
      EditorDocument createDocument(FileObject file);
      default int position() { return 100; }
  }
  ```
- **`nbfx-editor` (nbfx-ide):** `EditorServiceImpl.createDocument` consults ordered
  `Lookup.getDefault().lookupAll(EditorDocumentProvider.class)`, falls back to `CodeEditor`;
  `createPreviewDocument` unchanged.
- **`nbfx.javafx.editor`** registers a provider for `.fxml` returning the design/source document.

## FXML-aware completion (`nbfx.javafx.editor`)

### Port/rewrite split

`javafx2.editor` is entangled with the old editor/Swing stack (`openide.awt`, `openide.nodes`,
`openide.text`, `core.multiview`, `editor.lib/lib2`, `xml.text/xdm/catalog`) — forbidden in nbfx.

- **Reuse/port (UI-free kernel)**: `model/*` (`FxModel`, `FxNode`, `FxInstance`, `FxClassUtils`,
  `FxTreeUtilities`), `parser/*` (`FxmlParser`, import/script processors), `beans/*` (`FxBean`,
  `BeanModelBuilder`, `BuilderResolver`, `FxBeanCache`, `FxProperty`, `FxEvent`).
- **Rewrite on nbfx SPIs**: completers (class/property/enum/constant/event/handler/id/include/
  resource/attached/static), editor integration, diagnostics, codegen.

### Feature matrix

| Context | Completion |
|---|---|
| `<But¦` | FXML classes from the project classpath |
| `<?import ¦` | package + class imports |
| `<Button ¦` | settable properties + events (`on*`) |
| `alignment="¦"` | enums (`Pos.*`), constants, colors, booleans |
| `onAction="#¦"` | controller handler methods + generate stub |
| `fx:id="¦"` / `#id` | ids + generate `@FXML` field |
| `<fx:include source="¦">` | sibling FXML files |
| `%key`, `@path` | resource bundles / resource URLs |
| `<GridPane.columnIndex>` | attached/static property elements |

Plus **diagnostics** (unresolved class/import, unknown property/event, missing `fx:id`/controller) and
**go-to-declaration** (fx:id ↔ field, handler ↔ method), codegen ("Create Controller", "Create `@FXML`
Field/Method").

### Dependencies

`org.netbeans.modules.java.source` (already used by nbfx.editor), `ide/api.java.classpath`,
`parsing.api`/`parsing.indexing`, `projectapi`, `openide.filesystem`, `nbfx.api`, `nbfx.editor`.

## Embedded SceneBuilder — design/source editor (`nbfx.javafx.scenebuilder`)

- Bundle the kit via the external mechanism (below) in `nbfx.javafx.lib.scenebuilder`.
- Embed `SceneBuilderPane` (library | document | content | inspector) in an FXML **design/source
  editor**: one `EditorDocument` (via `EditorDocumentProvider`) presenting **Design** (SceneBuilder pane)
  ⇄ **Source** (XML editor), the active side publishing the editor context/lookup. Replace with a real
  multiview when nbfx gains one.
- Wire `SBController` (New/Open/Save FXML, undo/redo, clipboard/duplicate/select/delete) to nbfx
  commands; **save** writes the FXML `FileObject` and invalidates the code-editor buffer;
  source↔design sync; undo/redo bridge.
- **DependenciesScanner** uses the project compile classpath to list custom controls.
- Optional later: an "Open in SceneBuilder (external)" fallback, plus a preview window (nbscenebuilder
  feature).

## SceneBuilder kit distribution (external)

- `nbfx.javafx.lib.scenebuilder/external/binaries-list` with the Maven coordinates + SHA1s:
  `com.gluonhq.scenebuilder:kit:26.0.0`, `org.openjfx:javafx-web:<ver>`,
  `org.openjfx:javafx-swing:<ver>`, `org.glassfish:jakarta.json:2.0.1` (maven-resolver excluded).
- `nbproject/project.xml` `<class-path-extension>`s → `release/modules/ext`.
- Extend `patches/0012-ignored-binaries` for the downloaded jars; ensure `downloadbinaries` runs in the
  harness (`bootstrap`/`build`).
