# Appendix I — Language soundness (the second-implementation test)

`PLAN.md` §1 declares Web/PHP/Groovy non-goals. That is a valid **product** decision, but it is not a licence
for an untested architecture: **one implementation is not an abstraction.** This appendix is the test the plan
was missing, and the four holes it found in the current design. It is exercised by the conformance test already
on the critical path (the XML/CSS split) and by three time-boxed soundness spikes.

## I.1 The test that found the holes

The prototype already edits a second and third language — XML and CSS — and the plan already splits them into
`ide.editor.{xml,css}` (W1). But the seam they use today is a `switch`:

- `SyntaxDecorators.java:20-30` — `switch (fo.getExt()) { "css" → CssSyntaxDecorator; "xml"/"fxml" →
  XmlSyntaxDecorator; default → JavaSyntaxDecorator }`;
- `EditorBuffer.java:125` — `decorator = SyntaxDecorators.forFile(fileObject);`
- and `api.editor` — the published seam — has **no decoration/lexer/highlight SPI**: 11 types, the editor
  document/service, annotations, hyperlink, caret, settings.

So a new language (or your own XML/CSS split) edits `platform.editor`. That is the definition of a hole.

## I.2 The four holes

| # | Hole | Evidence |
|---|---|---|
| **L1** | the language seam is a `switch` in the core, not an SPI | `SyntaxDecorators.java:20-30`, `EditorBuffer.java:125`; `api.editor` has no decoration SPI |
| **L2** | provider SPIs carry no language/content-type scope | `api.editor`, `api.completion`, `api.breadcrumbs`, `api.usages`, `api.editor.intelligence` providers have no `LanguageId`; with one language that is harmless, with two a Groovy provider fires on a Java file |
| **L3** | `api.elements` is Java wearing a neutral name, in the platform cluster | `SourceModifiers.toModifierBits` returns `java.lang.reflect.Modifier` bits and matches `javax.lang.model.element.Modifier` by name (`SourceModifiers.java:19-24`); `SourceElementKind`/`SourceTypeKind` encode Java kinds |
| **L4** | non-Java capabilities have no home | no `api.servers`/`api.deploy`; `api.build` assumes a build (PHP has none); `libs.*` has no language-runtime policy ([Appendix G](G-licences-and-notices.md)) |

**L4b — the Swing-in-the-API defect is not Java-specific.** In the other clusters the same defect recurs:
`php/php.api.phpmodule` is **9/20 files Swing-touching (45 %)** — a Swing-typed *API* module — `php/php.project`
116/268, `php/php.refactoring` 18/28 (64 %). So [Appendix H](H-upstream-proposals.md) names a *class* of
offender, not one Java module.

## I.3 The target: a language registry, and a neutral element reference

```java
// api.language (M) — toolkit-free
public record LanguageId(String id, String mimeType) {}
public interface LanguageRegistration {          // @ServiceProvider
    LanguageId id();
    Lexer lexer();                                // token source over a CharSequence snapshot
    Optional<ParserService> parser();             // immutable snapshots for analysis (Appendix E.1 threading)
}
public interface Languages {                      // Lookup service
    LanguageId of(FileObject file);                                   // by MIME
    Optional<LanguageRegistration> registration(LanguageId id);
    <P> List<P> providers(LanguageId id, Class<P> spi);               // completion / hints / usages / …
}
```

- **Decoration SPI** in `api.editor`: `SyntaxHighlightProvider { LanguageId language(); … }` (or a
  `LexerRegistration`); `platform.editor` resolves decorators through `Languages`, never by extension.
- **Every provider SPI declares its scope** (`LanguageId`); `Languages.providers(id, spi)` filters. That is the
  L2 fix, and it is what makes the plugin promise true for a second language.
- **L3 decision**: the neutral `ElementRef` (opaque kind string + `SourceLocation` + display) and
  `SourceLocation`/`ElementIcons` live in `api.language` (keep neutral); the Java vocabulary
  (`SourceElementKind`, `SourceTypeKind`, `SourceModifiers`) moves to **`java.elements`**. No
  `java.lang.reflect.Modifier` bits in a neutral API.

## I.4 Core freeze — how a spike stays honest

A language cluster **implements only existing SPIs** (`M`/T2-FX). Any core change it needs is a **hole**: record
it, fix it in the API (before the M5 freeze), and never special-case it in the language module. Checked by
`check-languages` (PLAN §9): `platform.*` and the neutral `api.*` must not reference a `LanguageId`/content type
(no `if (java)` in the core), and language modules depend only downward.

## I.5 The soundness spikes (validation, not features)

Time-boxed (2–3 weeks each); the deliverable is a **hole list + the smallest core API fix**, never a language
IDE. Placed after `platform.editor` + `api.*` land (M1) and **before the plugin API freeze (M5)**; output
updates [Appendix D](D-bridge-and-retirement-matrix.md) (new rows) and [Appendix H](H-upstream-proposals.md).

| Spike | Language facts | What it tests | Stop condition |
|---|---|---|---|
| `spike/lang-groovy` | JVM; jar via `libs.groovy`; **no build, no server** | decoration, completion, structure, goto, usages, run (`api.execution`); `api.language`; neutral `ElementRef` | a file is highlighted, completed and run without editing core |
| `spike/lang-php` | non-JVM; **no build**; interpreter + dev server | `api.build` tolerates "no build"; `api.execution` (interpreter/PTY); whether `servers` must be core | decide `api.servers` with evidence, not taste |
| `spike/web-javaee` | **thin slice**: one war project type + one deploy action to one container | `api.servers`/`api.deploy` as first-class or out-of-core | the slice deploys without touching `platform` |

Rules: no feature growth (the cut list kills a spike that grows); each spike has an owner (§10.4); a spike that
must edit core has *found a hole* and the fix lands in the API.

## I.6 Measured opportunity (generated — [`../figures.md`](../figures.md))

| Cluster | modules | java files | Swing-touching |
|---|---|---|---|
| groovy | 11 | 259 | 73 (28 %) |
| php | 38 | 1833 | 475 (26 %) |
| enterprise (JavaEE) | 117 | 5079 | 1269 (25 %) |
| webcommon | 59 | 1415 | 420 (30 %) |

~75 % of each cluster is headless engine, so `reuse engine / replace UI` generalizes at roughly **3:1**:
`groovy/groovy.antproject` is 16 files **0 Swing**, `groovy/libs.groovy` 0, `groovy/maven.groovy` 1,
`groovy/groovy.editor` 134 with 34 Swing. The plan's reuse/bridge matrix is therefore *sound in shape* for
languages it does not target — and the spikes prove it or break it.

## I.7 What the plan requires

1. `refactor/editor-split` (W1) delivers the decoration SPI + `api.language`, and its acceptance is the
   conformance test: **XML and CSS are modules, and `platform.editor` contains no language branch.**
2. `api.elements` is split per I.3 (Decision 20); the Java vocabulary moves to `java.elements`.
3. The three spikes (W11) run between M1 and the M5 freeze; their findings update D/H.
4. `check-languages` guards the core freeze (§9).
5. A language runtime ships as `libs.<language>` with a NOTICE entry ([Appendix G](G-licences-and-notices.md)).
6. The other clusters' figures stay generated (they are, in [`../figures.md`](../figures.md)).

Non-goals stay non-goals; the architecture is what must hold. **A non-goal is not a licence for a hole.**
