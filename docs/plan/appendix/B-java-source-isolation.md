<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
# Appendix B — Containing `java.source` (the Java semantic layer)

Answers directly: **does the prototype use `java.source`, how, and can we hide it so it does not creep
into the new codebase?**

Short answer: **yes it uses it, in a bounded and already adapter-shaped way (4 modules; 43 files import
`org.netbeans.api.java.source`, 62 touch the semantic surface — [`../figures.md`](../figures.md)), and it
never touches `javax.swing.text.Document`.** Containment is therefore mostly *formalisation and a guard*,
not a rewrite: one bridge module owns the coupling, the Java layer is the only place it may appear, and
everything else keeps talking to the toolkit-free seams that already exist.

## B.1 The facts (audit of the tree)

Modules that depend on `org.netbeans.modules.java.source.base` (the module that provides the
`org.netbeans.api.java.source` package): `nbfx.editor`, `nbfx.findusages`, `nbfx.structure.java`,
`nbfx.project.ui.java`. They also pull `org.netbeans.api.java`, `org.netbeans.api.java.classpath`,
`org.netbeans.libs.javacapi`, `org.netbeans.modules.java.lexer`, `org.netbeans.modules.lexer`,
`org.netbeans.modules.queries`, `org.netbeans.modules.java.project`.

| Module | Files importing the Java semantic surface | Shape |
|---|---|---|
| `nbfx.editor` | ≈36 of 88 (`completion/support/Java*` ×17, `completion/Java*CompletionProvider`, `processor/semantics/*` ×12, `processor/lex/{JavaLexDecorationProcessor,BraceMatcher}`, `breadcrumbs/JavaBreadcrumbsScanner`, `codearea/JavaSyntaxDecorator`, `codearea/CssSyntaxDecorator`) | in-editor semantic completion, mark-occurrences, highlights, diagnostics, Java breadcrumbs |
| `nbfx.structure.java` | ≈11 (`model/{JavaSources,MembersScanner,GoToResolver,TypeResolver,InspectedType,BeanPatternAnalyser,OverriddenScanner,MemorySources,MemberNode,BeanPatternsScanner}`) | members/navigator/go-to scanners |
| `nbfx.findusages` | 5 (`query/{JavaSearch,SourceSet,UsageScanner,UsagesTarget,MemorySources}`) | batch Java usage search |
| `nbfx.project.ui.java` | 1 (`utils/TreeNav`) | open-at-caret navigation |

**No `javax.swing.text.Document` anywhere.** `grep -rn 'javax.swing.text.Document' nbfx-*/**/src` finds a
single Javadoc mention (`TextOffsets.java:9`). `CompilationInfo.getDocument()` is never called. The editor
model is the FX rich-text model and the semantic layer deals in **character offsets**.

## B.2 How it is used (the pattern)

1. **Headless compilation over an in-memory `FileObject`.** Text comes from the editor buffer, is copied
   into a memory filesystem, and compiled as a file — never as a Swing `Document`:
   - `JavaSourceContext.java:87-92` — `InMemoryFileSystem.createFileObject(fileObject, source)` then
     `JavaSource.create(cpInfo, inMemoryFo)`;
   - `JavaSources.java:40-41`, `MembersScanner.java:149-150`, `GoToResolver.java:264-265`,
     `SourceSet.java:249-261` — the same `ClasspathInfo.create(owner)` + `JavaSource.create(...)` pattern,
     `forFileObject(file)` when the content is unmodified.
2. **Tasks, not documents.** `CancellableTask<CompilationController>` / `runUserActionTask` / `Phase`;
   context held in an immutable `JavaSourceContext.Snapshot`.
3. **Trees + `SourcePositions` → offsets.** `com.sun.source.tree.*` and `com.sun.source.util.{Trees,
   SourcePositions,TreePath,TreePathScanner}`; results converted to offsets.
4. **Semantic queries.** `controller.getClasspathInfo().getClassIndex()`, `ElementHandle<TypeElement>`,
   `TreeUtilities`, `TypeElement` for completion.
5. **Three copies of the same plumbing.** `MemorySources.copyOf` exists twice (byte-identical idea:
   `nbfx.findusages/query/MemorySources.java:27`, `nbfx.structure.java/model/MemorySources.java:44`) and
   `InMemoryFileSystem.createFileObject` is a third (`editor/processor/semantics/InMemoryFileSystem.java:30`).

## B.3 The seams already in place (keep them; they are the containment)

These are toolkit-free and Java-free, and they already carry the cross-layer traffic:

- `api.editor.EditorDocument` — FX `Node`, `FileObject`, offsets, dirty/save; the action-context lookup.
- `api.editor.OpenSources` — `Set<FileObject> modifiedFiles()` + `String textOf(FileObject)`; exactly the
  "unsaved text for a parser running outside the editor" seam. `findusages` consumes it today
  (`SourceSet.java`, `MemorySources.copyOf(file, openTextOf(file))`).
- `api.language` (`ElementRef`, `SourceLocation`, `ElementIcons`) + `java.elements` — the element boundary (`SourceElementKind`, `SourceTypeKind`,
  `SourceModifiers`, `SourceLocation`, `ElementIcons`), and the nb-javac-safe conversion (Appendix A.4).
- `api.editor.EditorSettings` — editor preferences, no Java types.

## B.4 Target architecture

```
java layer (may import org.netbeans.api.java.source / libs.javacapi):
  java.model        T0 bridge: ClasspathInfo cache, in-memory FileObject, task runner,
                    com.sun.source positions -> offsets, nb-javac normalisation,
                    converts RM elements into `java.elements`/`api.language.ElementRef` value types
  java.editor       Java semantics of the editor: lexer decoration, semantic completion,
                    mark-occurrences, highlights, diagnostics, Java breadcrumbs
  java.structure    members/navigator/go-to scanners
  java.usages       Java provider for the generic usages engine (implements api.usages)
  java.navigation   open-at-caret / TreeNav (from project.ui.java)
  java.projecttype.* build/model (unchanged)

platform/ide (must NOT import java.source):
  platform.editor   the FX code area, buffers, decorations, search, completion popup  (from nbfx.editor)
  platform.editor?  xml/css decorators are language modules, not core
  ide.usages        generic usages engine + tree UI, over api.usages
```

Edges: `java.* → ide.* → platform.* → api.* → libs.*`, and `java.usages` registers into the generic
`ide.usages` engine through `api.usages` — so there is **no upward `ide → java` edge**.

Module split of today's `nbfx.editor` (88 files): the Java-free core (~50 files: `CodeEditor`,
`EditorBuffer`, `codearea/*` except Java, `codearea/search/*`, `completion/{CompletionController,Popup,
ItemCell}`, `decoration/*`, `processor/lex/BaseLexDecorationProcessor`, `EditorServiceImpl`,
`EditorSettingsImpl`) stays in `platform.editor`; the Java subset (~36 files) moves to `java.editor`; the
XML/CSS decorators become `ide.editor.xml` / `ide.editor.css` (or one `ide.editor.languages`).

### The bridge is thin (deliberately)

Do **not** wrap `ClassIndex`/completion behind a generic "language model" abstraction; that is an
abstraction over an abstraction and it will cost more than it saves. `java.model` does four mechanical
things and nothing else — and it is a **`Lookup` service with a project-scoped lifecycle**, not a bag of
statics (W1 `refactor/objects-not-statics`; the cache is project-owned, Appendix E.2 rule 4):

```java
// T0, java layer — obtained from SourceAnalysis.forProject(Project); dropped when the project closes
public interface SourceAnalysis {
    FileObject inMemoryFile(FileObject original, String text);          // + line-separator handling
    ClasspathInfo classpath(FileObject owner, boolean refresh);         // one cache, replaces 3 copies
    <R> R read(FileObject file, String text, Function<CompilationInfo, R> body);   // task packaging
    TaskHandle analyze(FileObject file, String text, CancellableTask<CompilationController> task);
    int toOffset(CompilationInfo info, long pos);                       // the TextOffsets counterpart
    long toPos(CompilationInfo info, int offset);
    SourceElementKind kindOf(Element e);                                // nb-javac-safe, A.4
    Set<?> modifiersOf(Element e);                                      // SourceModifiers.toModifierBits
}
```

Consumers in `java.editor`/`java.structure`/`java.usages` may still use `CompilationInfo` and
`com.sun.source.*` (JDK/`java.source` types) — they are *Java-layer* code. What they must not do is hand
those types to a non-Java module; they return `java.elements`/`ElementRef` values, offsets, or `FileObject`s.

## B.5 The policy (machine-checkable)

1. `org.netbeans.api.java.source.*`, `org.netbeans.libs.javacapi*` are importable **only** in modules whose
   metadata declares `java-semantics` (today: `java.model`, `java.editor`, `java.structure`, `java.usages`,
   `java.navigation`). The guard fails elsewhere.
2. `platform.*` and `ide.*` must not import them; they use `api.editor`/`api.language`/`api.usages` only.
3. No `javax.swing.text.Document` (Appendix A.2). Offsets and `FileObject` are the currency.
4. No `ElementHandle`/`TypeElement`/`ClassIndex` in any `api.*` signature (Appendix A.4).
5. `java.model` has no `public-packages` (T0): it is implementation, published nowhere, and may change.

## B.6 Migration (steps, each green alone)

1. **`refactor/guard-java-source`** — add the denylist/allowlist guard (Appendix A.5) with the *current*
   allowlist: 4 modules may import `java.source`. Green on day one; no code change. This alone stops the
   creep.
2. **`refactor/java-plumbing`** — extract `java.model` (`Classpaths`, `SemanticTasks`, `Positions`,
   `Elements`) from the three duplicated helpers and the position conversions; no behaviour change; unit
   tests move with the code.
3. **`refactor/editor-split`** — split `nbfx.editor` into `platform.editor` (chromium: no language) +
   `java.editor` (Java) + `ide.editor.{xml,css}`; `nbfx.editor` becomes `platform.editor`. Accept:
   `platform.editor` has no `java.*` import.
4. **`refactor/usages-split`** — `api.usages` (M: `UsagesProvider`, `UsageResult`) + `ide.usages`
   (generic engine + UI) + `java.usages` (Java provider over `java.model`); remove the `ide → java` edge.
5. **`refactor/java-navigation`** — move `TreeNav` out of `project.ui.java` into `java.navigation`.
6. **`refactor/java-source-shrink`** *(optional, later)* — if a non-Java module ever needs semantic data,
   extend `api.language`/`java.elements` rather than exposing `java.source`. Only extend when a second consumer exists.

## B.7 Risks and non-goals

- **Perf, not purity, is the real risk.** `ClasspathInfo.create` is expensive; the bridge must keep the
  per-file cache and the refresh-on-project-open behaviour that `JavaSourceContext.refreshClasspath`
  implements today (`JavaSourceContext.java:60-70`).
- **nb-javac.** The IDE may run against nb-javac's `javax.lang.model` copy; never compare element identity
  across the boundary — convert by name (Appendix A.4; the code already does this in `SourceModifiers`).
- **`libs.javacapi` vs `libs.nbjavacapi`** are separate modules; `java.model` is the one place that picks.
- **Non-goal:** a full "language model" SPI. Java is the only semantic language for the foreseeable
  roadmap; the abstraction lives at `api.language` + `api.usages`, not at `java.source`.
- **Also hide the FX internals leak.** `nbfx.editor` reaches into `com.sun.jfx.incubator.scene.control.
  richtext.{RichTextAreaSkinHelper,VFlow,CaretInfo}` (`CompletionController.java:9-11`). Those are
  non-public JavaFX internals; keep them behind one adapter (`platform.editor` `RichTextView`) so an FX
  upgrade touches a single file.
