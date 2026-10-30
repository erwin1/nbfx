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
| **L2** | provider SPIs carry no language/content-type scope, and registration is global | `api.editor`, `api.completion`, `api.breadcrumbs`, `api.usages`, `api.editor.intelligence` providers have no `LanguageId`; `CompletionController.java:305` does `Lookup.getDefault().lookupAll(CompletionProvider.class)` with no language filter, and the Java providers are plain `@ServiceProvider` (`JavaLexicalCompletionProvider.java:77`, `JavaSemanticCompletionProvider.java:180`) |
| **L3** | `api.elements` is Java wearing a neutral name, in the platform cluster | `SourceModifiers.toModifierBits` returns `java.lang.reflect.Modifier` bits and matches `javax.lang.model.element.Modifier` by name (`SourceModifiers.java:19-24`); `SourceElementKind`/`SourceTypeKind` encode Java kinds |
| **L4** | non-Java capabilities have no home | no `api.servers`/`api.deploy`; `api.build` assumes a build (PHP has none); `libs.*` has no language-runtime policy ([Appendix G](G-licences-and-notices.md)) |
| **L5** | no structure/navigator SPI | `api.editor.intelligence` lists goto/hierarchy/usages/hints/refactoring/codegen but **no `StructureProvider`**; `ide.structure`/`java.structure` have no published contract, so a language cannot contribute a structure view |
| **L6** | the usages *request* is Java-hardcoded | `api.usages` has the results (`UsagesProvider`, `UsageResult`, `UsageKind`) but no target; today's request imports `com.sun.source.*`, `javax.lang.model.element.*`, `ElementHandle`, `TreeUtilities` (`nbfx.findusages/query/UsagesTarget.java:3-20`) |
| **L7** | an unknown language silently becomes Java | `SyntaxDecorators.java:28` — `default -> new JavaSyntaxDecorator(fo)`; a Groovy file opened with no Groovy module gets Java lexing |

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
- **Handles are language-defined strings** (`ElementRef.handle()`), resolved by the language. "Kind + location"
  cannot express *the type X, wherever it is* — hierarchy, find-references and refactoring need a handle, not a
  location. And no provider may compare `javax.lang.model`/`com.sun.source` identity across languages (the
  nb-javac hazard, [Appendix A.4](A-toolkit-policy.md)) — which is exactly what `SourceModifiers.java:19-24`
  already works around.
- **Fibonacci the unknown-language path**: an unregistered MIME resolves to a **plain-text** `LanguageId`
  (no lexer, no analysis), never to Java (L7). Opening `.groovy` without a Groovy module must degrade, not
  mis-run the Java parser.
- **Language-scoped registration** (L2 made mechanical): a language declares its providers in its
  `LanguageRegistration` (or via `@LanguageProviderRegistration(language=…)`); the host asks
  `Languages.providers(id, spi)` — never a global `lookupAll`.
- **Two new SPIs**: `StructureProvider` (structure/navigator, scoped) and `UsagesTarget` (opaque
  `ElementRef` + `LanguageId` + location) in `api.editor.intelligence`/`api.usages` (L5, L6).

## I.4 Core freeze — how a spike stays honest

A language cluster **implements only existing SPIs** (`M`/T2-FX). Any core change it needs is a **hole**: record
it, fix it in the API (before the M5 freeze), and never special-case it in the language module. Checked by
`check-languages` (PLAN §9): `platform.*` and the neutral `api.*` must not reference a `LanguageId`/content type
(no `if (java)` in the core), and language modules depend only downward.

**Proved in CI, not by hand.** The freeze is tested by the **conformance fixture** (`feature/language-conformance`,
M0/M1): a test-only module registers `LanguageId("text/x-nbfx-conformance")` with decoration, completion,
structure, goto and usages, and a test opens such a file and asserts the features appear — with the core
untouched. That is the cheapest possible second language and it belongs in the build; Groovy/PHP/web are the
manual confirmation, not the proof.

**Don't freeze a wrong abstraction.** `api.language` ships `arch.xml category="devel"` and is **not frozen**
until the fixture **and** one real second language (Groovy) pass. Freezing an abstraction that has only ever
seen Java is how the `OptionsPanelController`-class mistakes ship.

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

**The web spike gets a shape, not a mandate.** Give `spike/web-javaee` a target to decide against:
`ServerInstance` (id, display, state), `DeployTask(FileObject artifact) → DeployProgress`, built on
`api.execution`/`api.progress`. The spike answers "core capability or out-of-core" with a concrete shape rather
than taste.

**The Groovy path is cheap, and it is concrete.** `groovy/groovy.editor`'s headless packages are real — 0 Swing
files in `api/elements` (+ `ast`, `common`), `compiler` (+ `error`), `completion/inference`, `completion/util`,
`java`, `occurrences`, `spi/completion`. So the spike reuses Groovy's compiler/AST/completion inference and
replaces only the UI. (`groovy/editor/api/elements` being *Groovy's own* `api.elements` independently confirms
Decision 20: a language's element model stays in its layer.)

**Spike hygiene (or the spikes become products).** Each spike has a branch, a time box, an owner, a **kill
criterion**, and the rule that spike code is **deleted** unless it survives as a real (optional) provider
module. A spike that grows a feature list is killed by the cut list, before it reaches the critical path.

## I.6 Measured opportunity (generated — [`../figures.md`](../figures.md))

Count **modules**, not files: a module with a single `javax.swing.Action` is neither reusable nor replaceable —
it must be **bridged** (the `ProgressHandle` situation). File shares overstate reuse:

| Cluster | modules | **Swing-free modules** | java files | Swing-touching |
|---|---|---|---|---|
| groovy | 11 | **4** | 259 | 73 |
| php | 38 | **5** | 1833 | 475 |
| enterprise (JavaEE) | 117 | **47** | 5079 | 1269 |
| webcommon | 59 | **22** | 1408 | 420 |

So Groovy is **4 reusable / 7 mixed-or-replace**, not "72 % reusable" — and the mixed modules are exactly where
the holes are, which is why the spikes target them. The four Swing-free modules are
`groovy/groovy.antproject` (16 files), `groovy/libs.groovy`, `groovy/gradle.groovy` and `groovy/groovy.kit`;
the headless half of `groovy/groovy.editor` (I.5) is the additional reusable surface once its UI is split.

## I.7 What the plan requires

1. `refactor/editor-split` (W1) delivers the decoration SPI + `api.language`, and its acceptance is the
   conformance test: **XML and CSS are modules, and `platform.editor` contains no language branch.**
2. `feature/language-conformance` (W1/W2): the **fake-language fixture** in CI — the automated proof of the core
   freeze (I.4).
3. `api.elements` is split per I.3 (Decision 20); the Java vocabulary moves to `java.elements`; handles are
   language-defined strings; the unknown language falls back to **plain text** (L7).
4. Two SPIs are added: `StructureProvider` ([§6.1](../PLAN.md)) and `UsagesTarget` (L5, L6).
5. Language-scoped registration is normative (I.3, [Appendix E.6.1](E-contracts.md)); the host never does a
   global `lookupAll` on a language provider.
6. The three spikes (W11) run between M1 and the M5 freeze, with a kill criterion (I.5); their findings update
   D/H.
7. `check-languages` guards the core freeze (§9); `api.language` stays `devel` until the fixture and Groovy pass.
8. A language runtime ships as `libs.<language>` with a NOTICE entry ([Appendix G](G-licences-and-notices.md)).
9. The other clusters' figures stay generated (they are, in [`../figures.md`](../figures.md)).

Non-goals stay non-goals; the architecture is what must hold. **A non-goal is not a licence for a hole.**
