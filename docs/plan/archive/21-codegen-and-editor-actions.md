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
# 21 — Code generation & editor actions

Steps: **Generate code** (`feature/codegen-generate`), **Imports & Surround With**
(`feature/fix-imports`), **Editor actions parity** (`feature/editor-actions-parity`).

## Why

The everyday authoring features: "Insert Code"/Generate (constructor, getters/setters, override/implement,
`equals`/`hashCode`/`toString`, delegate, add property), Fix/Organize Imports, Surround With, insert
Javadoc; plus the editor actions that go with them (format/reformat, fold, indentation, insert template,
mark occurrences, bracket matching, error stripe, toggle comment, macros).

## What nbfx has today

`nbfx.editor` (completion, breadcrumbs, annotations, syntax, find/replace), `nbfx.editor.actions`
(cut/copy/paste/undo/redo/save/find), `nbfx.api.editor` (annotation SPI, `EditorSettings`). No generation,
no imports handling, no format/fold/template actions beyond what the code area provides.

## Research (original codebase)

| Piece | Where | Verdict |
|---|---|---|
| Java code generators (constructor, getters/setters, delegate, override/implement, `equals`/`hashCode`/`toString`) | `java.source.base` **`GeneratorUtilities`** (already a dep, headless) + the Java generate actions in `java/java.editor`; the generic "Generate" UI SPI in `ide/editor` (`org.netbeans.modules.editor.codegen`) | **reuse `GeneratorUtilities`**; **port** the UI (generate dialog, insertion targets) |
| **Organize/Fix Imports** | `java/java.hints` (`org.netbeans.modules.java.hints.OrganizeImports`) — a hint/fix; also `java.sourceui` helpers | **reuse via the hints engine** (doc 20) and surface as commands |
| **Surround With** | `java/java.hints/errors` (`MagicSurroundWithTryCatchFix`, `OrigSurroundWithTryCatchFix`, …) | reuse via hints; the surround-with chooser (try/catch, if, for, while, do, {}, comment, region) is a small FX popup |
| **Insert Javadoc** | `java/javadoc` (`org.netbeans.modules.javadoc.hints.JavadocGenerator`) | reuse the generator; port the command (doc 22 covers the Javadoc view) |
| Editor format/reformat, indent, fold, templates, mark occurrences, brackets, error stripe, toggle comment, macros | `ide/editor.global.format`, `editor.fold`, `editor.indent`, `editor.codetemplates`, `editor.bracesmatching`, `editor.errorstripe`, `editor.lib`, `editor.macros` | **port to FX editor actions** (the settings already come from doc 14's Editor options) |

## Target architecture

- **`nbfx.editor.codegen`** (headless): `CodeGenerator` adapters over `GeneratorUtilities` (constructors,
  accessors, delegate methods, override/implement, `equals`/`hashCode`/`toString`, add property), returning
  edit plans applied through `java.source.base` (`ModificationResult`) atomically.
- **`nbfx.editor.ui` (FX)**: the **Generate** dialog (checkbox tree of members + insertion point + preview),
  **Surround With** popup, **Organize Imports** command, **Insert Javadoc** command, plus the editor action
  set (format/reformat, fold/unfold, indent, insert template, toggle comment, mark occurrences, bracket
  match, next/prev error, macros).
- Imports handling reuses the hints engine (doc 20): "Fix Imports"/"Organize Imports" are commands that run
  the corresponding fix on the current file/project with progress.

## Steps

1. **Generate code** (`feature/codegen-generate`) — adapters over `GeneratorUtilities` + the FX Generate
   dialog (members/insertion/preview) + commands (Alt+Insert style) + atomic edit/undo + tests.
2. **Imports & Surround With** (`feature/fix-imports`) — "Fix Imports"/"Organize Imports" and the
   Surround With chooser over the hints engine; shortcuts; project-scope organize (with progress).
3. **Editor actions parity** (`feature/editor-actions-parity`) — format/reformat document & selection,
   fold/unfold/expand, indent, insert code template, toggle comment, mark occurrences (also used by doc 18
   navigation and doc 19 instant rename), bracket matching/go-to-bracket, error stripe navigation
   (next/prev hint/error), editor macros, and a status-line caret/selection already provided by nbfx.
   Expose all as commands/menu/context-menu entries with keymap entries (doc 14).
4. **Insert Javadoc** — command that runs `JavadocGenerator` at the caret (doc 22 handles the viewing side).

## Acceptance

- Generate inserts constructors/getters/setters/override/implement/`equals`/`hashCode`/`toString`/delegate
  at a chosen insertion point, with an undoable edit and correct imports.
- Fix/Organize Imports resolve unresolved types and remove unused imports for fixtures.
- Surround With wraps selections in try/catch/if/for/while/do/block/comment/region.
- Format/reformat, fold, indentation, template insertion, toggle comment, mark occurrences, bracket match
  and next/prev error work from the editor and the menus with the configured options.
- Headless tests for generators/imports; UI headless tests for the dialogs.

## Risks / open questions

- **Generators availability**: confirm the specific `GeneratorUtilities` methods for each member in the
  `java.source.base` version nbfx uses; some generators live in `java.source`/`java.editor` (verify;
  add only if Swing-free parts are needed).
- **Insertion point/preview** UX parity (member vs caret insertion, ordering, modifiers).
- **Organize Imports scope** (file vs project) performance and the "fix all" undo granularity.
- **Formatting**: nbfx's `editor.global.format`-equivalent must use the Java formatting settings
  (doc 14's Formatting panel) — align the two.
- **Mark occurrences** is shared by three plans (18/19/20/21) — implement once in the editor layer.
