# 20 — Java hints / inspections

Steps: **Hints engine** (`feature/java-hints-core`), **Editor integration** (`feature/java-hints-editor`),
**Lightbulb & fixes** (`feature/java-hints-fixes`), **Hints configuration** (`feature/java-hints-options`).

## Why

Hints are the everyday-quality feature of the Java editor: error/warning markers, the lightbulb, "apply
fix", and the fixes that implement "Fix Imports", "Surround With", small refactorings and error
corrections. The current plan mentions only a future options tab; the engine, editor integration and UI
are missing.

## What nbfx has today

`nbfx.editor` renders annotations (`AnnotationSupport`, `MarkLineNumberDecorator`, `EditorAnnotationProvider`
from `nbfx.api.editor`) and has completion/breadcrumbs/syntax; there is no hints engine, no lightbulb and
no fix application.

## Research (original codebase)

| Module | Files / Swing | Role | Verdict |
|---|---|---|---|
| `java/spi.java.hints` | 58 / 11 | the **hints SPI + engine**: `Hint`, `HintContext`, `HintFix`, `HintsInvoker` (`org.netbeans.modules.java.hints.spiimpl.hints.HintsInvoker`), severity/options model, `JavaRuleContext` | **reuse** (engine is headless; `HintsInvoker` runs hints and returns fixes) |
| `java/java.hints` | 274 / 79 | the hint/fix implementations (errors, `OrganizeImports`, `MagicSurroundWithTryCatchFix`, `OrigSurroundWithTryCatchFix`, style hints, etc.) + `Inspect` infrastructure | **reuse** the implementations where Swing-free; many fixes are pure model transforms |
| `java/java.hints.ui` | 24 / 14 | options/customizers UI, rule table | **port** to FX (doc 14 options page) |
| `java/java.hints.declarative` | — | declarative hints (`.hint` files) | reuse (headless) |
| `ide/spi.editor.hints` | 27 / 14 | Swing editor-hints UI SPI (annotation/lightbulb plumbing) | **replace** with the FX editor integration (nbfx's annotation SPI) |
| `java/ant.hints`, `java/maven.hints` | — | project/dependency fixes (e.g. wrong classpath, missing JUnit) | reuse logic, FX rendering |
| `java/spellchecker.bindings.java` | — | Java identifiers for the spellchecker | later (spellchecker) |

nbfx's editor annotation SPI (`nbfx.api.editor.EditorAnnotationProvider`) is the hook for hint markers;
the fix popup/lightbulb is new FX work.

## Target architecture

```
nbfx.java.hints.core     (headless model, JavaFX-free)
  HintEngine            run hints via HintsInvoker for a CompilationInfo/range; severities; cancellation;
                        cache per document version
  HintFixApplicator     apply HintFix (JavaFix/ErrorFix) through java.source.base edits; multi-fix ("Fix All")
  HintsSettings         per-hint enable/severity (nbfx.settings); profile-like ordering
  problematic-project   adapters for ant/maven dependency hints
nbfx.ide editor integration (in nbfx.editor / a small nbfx.java.hints.ui)
  HintAnnotation        warning/error annotations (gutter + underline), hover tooltips, error stripe
  LightbulbPopup        FX popup listing fixes per caret position; keyboard (Alt+Enter), preview/apply
  HintsOptionsPanel     Editor ▸ Hints page (doc 14) over HintsSettings (FX rule table)
```

## Steps

1. **Engine** (`feature/java-hints-core`) — depend on `spi.java.hints` (and `java.hints` for the
   implementations); run `HintsInvoker` over a `CompilationInfo` on a background executor; map
   `HintContext`/`HintFix` to nbfx models; severity/settings model; unit tests with Java fixtures.
2. **Editor integration** (`feature/java-hints-editor`) — compute hints on open/save/idle (debounced),
   render annotations (gutter/wave underline/hover) with severities, error stripe integration, "Next/Prev
   Hint" navigation; cancellation and caching by document version.
3. **Lightbulb & fixes** (`feature/java-hints-fixes`) — the lightbulb popup (Alt+Enter) at the caret/line,
   list fixes with descriptions, apply through `HintFixApplicator` (atomic edit + undo), "Fix All" in
   file/project, and the fixes that users expect as commands: **Fix Imports / Organize Imports**
   (`java.hints.OrganizeImports`) and **Surround With** (`SurroundWithTryCatch*`, `MagicSurroundWithTryCatchFix`)
   surfaced as editor actions (shared with doc 21).
4. **Configuration** (`feature/java-hints-options`) — the Editor ▸ Hints options page (rule table, severity,
   enable/disable, search), persisted via `nbfx.settings`, applied live; "Inspect & Transform" later.
5. **Project hints** — adapt `ant.hints`/`maven.hints` findings (broken classpath, missing test libs) into
   the same annotation/fix pipeline (also feeds doc 02's problems UI).

## Acceptance

- Unresolved symbols, unused imports, style issues show the same hints/severities as NetBeans for fixtures.
- Alt+Enter at a position offers the same fixes; applying one edits the file atomically and undoes in one
  step; "Fix Imports" and "Surround With" work as commands.
- The Hints options page changes severity/enabled state live and persists.
- `nbfx.java.hints.core` is JavaFX-free (`check-models`); hints never run on the FX thread.

## Risks / open questions

- **Engine coupling**: `HintsInvoker`'s API and `JavaRuleContext` setup; verify the invocation path is
  Swing-free (it is designed for LSP/editor use).
- **`java.hints` size**: 274 files pulled in — enable selectively (Lookup of rule providers already filters
  by `JavaRuleContext`/settings); watch startup cost (rules load lazily).
- **Perf**: hints on every keystroke are expensive — debounce + per-version caching + cancel; avoid running
  project-scope hints on the FX thread.
- **Fix completeness**: some fixes are `JavaFix`-based (headless), others may render custom UI (port those).
- **Annotation model**: the existing `EditorAnnotation` may need underline/tooltip/click support (extend,
  as in doc 16's editor annotations).
