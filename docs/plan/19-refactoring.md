# 19 — Refactoring

Steps: **Refactoring core** (`feature/refactoring-core`), **Rename** (`feature/refactoring-rename`),
**Java refactorings** (`feature/refactoring-java`), **Refactoring actions & UI**
(`feature/refactoring-actions`).

## Why

Refactoring is the largest missing "basic" Java feature: rename, move/copy/delete, introduce/inline,
encapsulate, change parameters, pull up/push down, extract. nbfx has **no** refactoring code.

## What nbfx has today

Nothing. `nbfx.findusages` (usages query + view) and `nbfx.editor` (edits/undo) exist and are the natural
substrate; `java.source.base` (already a dependency) provides the edit engine
(`JavaSource`/`WorkingCopy`/`ModificationResult`/`SourceUtils`/`ElementUtilities`/`TreeUtilities`).

## Research (original codebase)

| Module | Files / Swing | Role | Verdict |
|---|---|---|---|
| `ide/refactoring.api` | 111 / 50 | SPI: `Refactoring`, `RefactoringSession`, `RefactoringPlugin`, `Problem`, `Progress`, `RefactoringUI`, `ParametersPanel`, `RefactoringCommit` | **reuse the model/SPI** (`Refactoring`, `RefactoringSession`, `RefactoringPlugin`, `Problem`, `Progress`); **port** `RefactoringUI`/panels (Swing `JComponent`) |
| `java/refactoring.java` | 207 / 101 | Java refactorings: `RenameRefactoringPlugin`, `MoveClass`, `Introduce*`, `Inline*`, `EncapsulateFields`, `ChangeParameters`, `PullUp`/`PushDown`, `Extract*`, `SafeDelete`, call hierarchy | **reuse the plugin/logic classes** where Swing-free (many are); **port** the `ui/*` dialogs and `RefactoringUI` implementations |
| `java/maven.refactoring`, `java/form.refactoring` | — | project/form-specific plugins | reference (Maven plugin reuse later) |
| `ide/refactoring.api` (java.source integration) | — | atomic edit/undo via `ModificationResult` + `UndoManager` | reuse `java.source.base` |

The upstream refactoring SPI is UI-agnostic at its core (`Refactoring`/`RefactoringSession`/plugins return
`Problem`s and run with `Progress`); only `RefactoringUI` (preview/parameters) is Swing. nbfx implements
its own `RefactoringUI` returning FX nodes.

## Target architecture

```
nbfx.refactoring.core      (headless model, JavaFX-free)
  RefactoringManager       available refactorings per context; scope (selection/caret/project)
  RefactoringExecution     run RefactoringSession/plugins, collect Problems, apply atomically + undo
  plugins/                 Java refactorings (port/adapt refactoring.java plugins): rename, move, copy,
                           delete, safe delete, introduce (variable/constant/field/method/parameter),
                           inline, encapsulate fields, change parameters, pull up/down, extract
nbfx.refactoring.ui        (JavaFX)
  RenamePanel              inline rename + dialog (new name, "rename in comments/strings" options)
  RefactoringPanel         generic: preview (tree of changes), parameters page, "Refactor"/"Cancel",
                           problem list, progress
  RefactoringActions       Refactor menu/context menu, shortcuts, enablement, Instant Rename
```

- Execution is off the FX thread with progress/cancel; the preview model is a list of `ChangeInfo`
  (`FileObject`, element, kind) rendered as an FX tree with double-click navigation (reuse doc 18's views).
- Undo is a single `UndoManager` edit (the SPI's atomic commit), shared with the editor.

## Steps

1. **Core** (`feature/refactoring-core`) — `RefactoringManager` + `RefactoringExecution` over
   `refactoring.api` (`RefactoringSession`, `Problem`, `Progress`; reuse where Swing-free) and
   `java.source.base` edits/undo; scope detection (caret/selection/project); headless tests with fixtures.
2. **Rename** (`feature/refactoring-rename`) — the Java rename plugin (reuse `refactoring.java`
   `RenameRefactoringPlugin`), inline rename in the editor + dialog, options (comments/strings), preview,
   undo, **Instant Rename** (mark occurrences) and "Rename in File".
3. **Java refactorings** (`feature/refactoring-java`) — port/adapt in order: **Move** (class/package),
   **Copy**, **Delete**, **Safe Delete**; **Introduce** Variable/Constant/Field/Method/Parameter,
   **Inline**; **Encapsulate Fields**, **Change Parameters**, **Pull Up/Push Down**, **Extract
   Interface/Superclass**. Each with its findings/Problems mapped to the preview.
4. **Actions & UI** (`feature/refactoring-actions`) — the `Refactor` menu and editor/project context menu
   entries, shortcuts (doc 14 keymap), enablement from the context, the generic `RefactoringPanel`
   (preview + parameters + "Refactor"), problem reporting, and integration with Find Usages ("Refactor
   Usages"/"Find Usages" toggle).

## Acceptance

- Rename refactors a class/method/field/parameter/local across a project with a correct preview, respects
  comments/strings options, and undoes in one step.
- Move/Copy/Delete/Safe Delete, Introduce/Inline, Encapsulate/Change Parameters/Pull-Push/Extract run on
  fixtures and produce the same results as NetBeans.
- All actions are enabled/disabled per context; refactorings run off the FX thread with progress/cancel.
- `nbfx.refactoring.core` is JavaFX-free (`check-models`).

## Risks / open questions

- **Reuse seam**: how much of `refactoring.java` is Swing-free (plugins vs `ui/*`); verify per refactoring
  and port the UI; if a plugin is Swing-coupled, port its logic.
- **`java.source.base` vs `java.source`**: some refactorings may expect `java.source` extras not in
  `java.source.base` — verify; adding `java.source` pulls Swing modules into the cluster (already present
  in the `java` cluster) but nbfx sources must not import them.
- **Preview cost**: building previews for large projects can be slow; reuse the SPI's async
  `Problem`/`Progress` model and cap preview size.
- **Undo integration** with the FX editor's document/undo stack.
- **Instant Rename** needs a mark-occurrences layer (shared with hints, doc 20).
