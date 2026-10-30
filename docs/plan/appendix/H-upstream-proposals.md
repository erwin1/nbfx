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
# Appendix H — Upstream proposals (and where nbfx prototypes them first)

The front end must not absorb the platform's Swing-tainted API surface. This appendix is the agenda that makes
the *original source better too*: each proposal is additive, keeps binary compatibility through the module
system, and is prototyped in nbfx first, so upstream receives a working API with a first customer, not a wish.
`PLAN.md` §13.10 (upstream first) and §11 `chore/upstream-proposals` refer here.

## H.0 The rules a proposal must obey

1. **Additive only.** New modules and new methods; `default` on interfaces; `@Deprecated` never delete.
   A removed method is a rejected proposal.
2. **New module + compatibility wrapper.** The neutral API moves to a new module; the old module keeps a thin
   wrapper and depends on the new one. The pattern already exists upstream: `api.progress` /
   `api.progress.nb` / `api.progress.compat8`.
3. **The tooling carries the move.** `config/ModuleAutoDeps/<code-name-dashes>.xml` records the package move,
   and `RefreshDependencyVersions`/`ModuleDependencies` regenerate dependants' `<specification-version>`
   (`nbbuild/templates/common.xml:118,318`, `nbbuild/build.xml:1286`, `nbbuild/antsrc/…/ModuleDependencies.java`).
   No proposal requires hand-editing 40 `project.xml` files.
4. **`sigtest` proves the surface.** `nbbuild/antsrc/org/netbeans/nbbuild/Sigtest.java` is wired into
   `templates/projectized.xml`; every proposal lands with the signature frozen in a test (the missing half of
   Appendix E.9).
5. **A fallback and a deadline.** nbfx never blocks on a proposal. Each row below states what nbfx does if
   upstream refuses, and by when.
6. **A tracked issue per proposal**, with the nbfx prototype linked — the prototype *is* the patch.

## H.1 `java.source`: split the headless half; make edits applicable without a `Document`

**Problem (measured).** `java.source.base` is 217 files, 39 Swing-touched, and the Swing is in the API surface:
`JavaSource.forDocument(Document)` (`java/source.base/.../JavaSource.java:331`),
`CompilationInfo.getDocument()` (`CompilationInfo.java:368`; the headless route `getText()` is `:172`),
`ModificationResult.Difference.getStartPosition()/getEndPosition()` returning `javax.swing.text.Position`
(`ModificationResult.java:527,531`), `Difference.openDocument()` (`:123`), and `commit()` which only writes
through a `Document` + `AtomicLockDocument`/`LineDocumentUtils`/`DocumentUtilities` (`:170,194-226,318`).
The **rewrite** half is already neutral (`WorkingCopy.rewrite(...)` `:243,325,356`), and
`ModificationResult.getResultingSource(FileObject)` (`:445`) returns text.

**Change.**
- split `java.source.base` → `java.source.base` (parser, index, `ClasspathInfo`, `CompilationInfo` minus
  `getDocument`, the `save/*` diff engine, `WorkingCopy`, `ModificationResult`) + `java.source.document`
  (`DocumentProvider` impl, `forDocument`, the `Document`/atomic-lock bridge);
- add `Difference.getStartOffset()/getEndOffset()` (`int`) and promote the existing private
  `commit(FileObject, List<Difference>, Writer)` (`:216`) to public;
- `@Deprecated` the `Document` entrances for non-editor clients.

**Compatibility.** New module + `ModuleAutoDeps` move; old methods kept and deprecated; spec version bump only.

**Buys nbfx.** The `java.model` bridge (Appendix B) loses `Document`, and refactoring/generation results apply
to the FX text model. Shrinks [_D.3_](D-bridge-and-retirement-matrix.md)'s replace column.

**Fallback (by M2).** Keep `java.model` and apply edits from `getResultingSource`/`getDifferences` offsets.

**Prototype in nbfx.** `java.model` (Appendix B): give its `SourceAnalysis`/edit types **int offsets and a
`Writer`/`TextEdit` apply path** from day one, implemented over today's `java.source`. `platform.editor`'s
`TextOffsets` already proves the offset model works. Step `refactor/java-plumbing`; spike
`spike/java-source-headless` for the apply path.

## H.2 `api.progress`: the SPI is Swing-typed

**Problem.** `TaskModel` uses `DefaultListModel`, `DefaultListSelectionModel`, `EventQueue`, `ListDataListener`
(`platform/api.progress/src/org/netbeans/modules/progress/spi/TaskModel.java:23-31,41`); `ProgressHandle`
imports `javax.swing.Action`/`java.awt.event.ActionListener` (`ProgressHandle.java:22-27`);
`ProgressHandleFactory` selects a Swing-typed `ProgressUIWorkerProvider` via Lookup (`ProgressHandleFactory.java:229`).

**Change.** Neutral `TaskModel`/`ProgressEvent`/`TaskGroup` + cancellation as `Runnable`/`Cancellable`; the
Swing model and `Action` adapters move to `progress.swing`; `api.progress.nb` wraps the old types.

**Buys nbfx.** Removes the permanent `javax.swing.Action`/`ActionListener` exemption in Appendix A.6/D.1.

**Fallback.** Keep the exemption; the bridge stays as D.1 describes.

**Prototype in nbfx.** `api.progress` (M) is already planned as a **neutral** model; implement it there and
write `platform.progress.bridge` registering an FX `ProgressUIWorkerProvider`, so the same neutral model is
proven against both our UI and upstream's Swing SPI. Step `spike/progress-neutral-spi` (M0, cheapest win).

## H.3 `openide.awt`: the neutral half already exists — name it

**Problem.** `platform/openide.awt` holds both the neutral registration model (`ActionRegistration`,
`ActionReference`, `ActionState`, `AlwaysEnabledAction`, `ContextAction`, `Actions`, `ContextActionEnabler`,
`DynamicMenuContent`) and the Swing widgets (`StatusDisplayer`, `ColorComboBox`, `DropDownButton`,
`GraphicsUtils`, `CloseButtonFactory`). nbfx **reinvented the neutral half** as
`@ActionRegistration`/`ActionRef`/`ActionState` because there was no Swing-free module to depend on.

**Change.** Extract the neutral packages to `org.netbeans.api.actions` (new, Swing-free); `openide.awt` depends
on it and keeps the widgets; re-export for compatibility; spec version bump.

**Buys nbfx.** Deletes a duplicate action model; a plugin written for nbfx becomes portable back. Ecosystem win.

**Fallback.** Keep `api.actions` (already exists).

**Prototype in nbfx.** `api.actions` (T2-FX): extract the **FX-free** subset (registration, references, state)
into `api.actions.core` (T1) that is a *class-for-class mirror* of the upstream neutral classes; the FX menu
layer depends on it. The donation is then literally the file set. Step `spike/actions-neutral-core` (M0).

**Same defect class — language API modules.** The Swing-typing of `java.source` is not unique; it is a
platform-wide pattern. **13** upstream modules whose *name says API/SPI* import Swing (generated —
[`../figures.md`](../figures.md)):

| Module | Swing / files |
|---|---|
| `webcommon/web.clientproject.api` | **28 / 64** |
| `php/php.api.phpmodule` | 9 / 20 |
| `php/php.api.framework` | 8 / 16 |
| `php/php.apigen` | 7 / 47 |
| `php/php.api.documentation` | 4 / 7 |
| `enterprise/j2ee.api.ejbmodule` | 3 / 36 |
| `enterprise/api.web.webmodule` | 2 / 17 |
| `enterprise/websvc.clientapi`, `enterprise/websvc.restapi` | 2 each |
| `php/php.api.executable`, `php/php.api.testing`, `enterprise/web.jsfapi`, `enterprise/websvc.websvcapi` | 1 each |

Also `php/php.project` 116/268 and `php/php.refactoring` 18/28; `groovy/groovy.editor` mixes a Swing-free
compiler/AST/completion half with a Swing UI ([Appendix D.7](D-bridge-and-retirement-matrix.md)). The rule:
**any API/SPI module that imports Swing is a split candidate** — the same additive recipe (new module +
compatibility wrapper + `ModuleAutoDeps` + `sigtest`) applies, and the offender list is generated, not
hand-written.

## H.4 `openide.nodes`: headless core, Swing shell, neutral icon source

**Problem.** `Node.getIcon(int)` returns `java.awt.Image`
(`platform/openide.nodes/src/org/openide/nodes/Node.java:22,434`); `Sheet`/`PropertySet` are Swing-shaped;
`openide.explorer` already hosts the views.

**Change.** `openide.nodes` keeps the headless core (children, properties, lookup, display name) plus a neutral
`ImageSource`/`IconDescriptor`; `openide.nodes.swing` holds `java.awt.Image` compatibility and the sheet;
deprecate `getIcon()` in favour of a neutral accessor.

**Buys nbfx.** `api.nodes` (Appendix F) can *wrap* the upstream node model instead of replacing it; the
automatic `java.awt` exemption disappears.

**Fallback.** Keep `api.nodes`/`platform.nodes` as the model; upstream stays untouched.

**Prototype in nbfx.** `api.nodes` (M) defines `IconRef` (resource name + lazy bytes, no `java.awt`/`javafx`);
`platform.nodes` resolves it to an FX `Image`. Render every current icon through `IconRef` and delete direct
`Image`/`ImageUtilities` use. Step `spike/nodes-icon-source` (M1).

### H.4.1 `DataObject`: make the node delegate optional

The coupling is not just the static initializer: `DataObject implements Node.Cookie` (`DataObject.java:58`),
the node delegate is in the API (`getNodeDelegate()` `:302`, `createNodeDelegate()` `:406`), cookies are typed
on `Node.Cookie` (`:1142`), and `getLookup()` returns the node's Lookup (`:1174`) — the recommended override
still uses `CookieSet`, which lives in `openide.nodes` (`CookieSet.java:41`). The static
`BEING_CREATED = Node.EMPTY.cloneNode()` (`:126`) is only the trigger; the javadoc already warns that the
default `getLookup()` has “significant potential to deadlocks” (`:1155`).

**Change.** (a) make `BEING_CREATED` lazy; (b) move cookie/lookup storage out of `Node` (a neutral
`openide.cookies` module) so `getCookieSet().getLookup()` no longer needs `openide.nodes`; (c) deprecate
`getNodeDelegate()` for non-UI clients. Additive; `openide.loaders` keeps the old types.
**Buys nbfx.** `openide.loaders` becomes usable without initializing `openide.nodes`, so PR #12's workaround can
go and `openide.nodes` can be pruned once its 388 dependants retire.
**Prototype in nbfx.** `ide.project.ui`/`java.model` avoid `getLookup()`/`getNodeDelegate()` where they can;
measure with `spike/loaders-node-free`. **Fallback:** keep PR #12 and `openide.nodes`, and say so in §1.

### H.4.2 `core.ui`: move the Swing warm-up out of the platform

`platform/core.ui/.../warmup/ContextMenuWarmUpTask.java:52,61` initializes `Node` on the AWT thread at startup.
For a non-Swing front end it is pure risk (the AWT frame in issue #10). Make the warm-up a Swing-only module or
conditional on the window system, so a JavaFX front end never pays for it.

## H.5 The editor SPI: make the text model an implementation detail

**Problem.** `editor.lib2` exports `org.netbeans.spi.editor.document`, `…highlighting`, `…typinghooks`,
`org.netbeans.api.editor.caret` — all `Document`/`JTextComponent`-shaped (Appendix D.3: ~600 upstream files
"replace").

**Change.** `editor.model`: neutral `TextSnapshot`, `Position`, `TextEdit`, `Highlight`, `TypingHook`;
`Document` becomes one adapter; `editor.swing` on top. Experimental module first, SPI review second.

**Buys nbfx.** Turns part of D.3's replace column into reuse, and makes a third-party front end possible.

**Fallback.** Keep the nbfx replacement (the current plan).

**Prototype in nbfx.** This *is* `platform.editor`: define the neutral view model (`TextSnapshot`, `Position`,
`TextEdit`, `Highlight`, `TypingHook`) over `jfx.incubator…CodeTextModel` and implement every editor service
against it. Publish it as `arch.xml category="devel"` with an `apichanges` entry "candidate for upstream
`editor.model`". Step `spike/editor-model-spi` (M1, biggest; sequence last).

## H.6 The tooling (`nbbuild`): this is the "auto dependency wiring" work

The mechanism already exists (`nbbuild/antsrc/org/netbeans/nbbuild/`): `ModuleDependencies`, `FixDependencies`,
`RefreshDependencyVersions`, `Sigtest`, `CreateModuleXML`, `JarWithModuleAttributes`, plus
`config/ModuleAutoDeps/<cnb>.xml`. Extend it — do not build a parallel system:

1. **Tier-aware dependency check**: `ModuleDependencies`/`FixDependencies` read a module-declared tier/allowlist
   (nbfx's `nbproject/nbfx-policy.properties`, Appendix A.5) and fail on a denylisted import family outside its
   owner. This turns nbfx's `check-sources` into an `ant` task any cluster can use.
2. **`OpenIDE-Module-Public-Packages` derived from `arch.xml`** (`CreateModuleXML`/`JarWithModuleAttributes`):
   explicit packages only, never `<subpackages>`, and fail on `exported ⊄ arch`.
3. **`Sigtest` in every API module's `verify`** (upstream already has the class) — the missing half of E.9.
4. **`ModuleAutoDeps` for moves** (H.1–H.5): the XML records the split, `RefreshDependencyVersions` bumps
   dependants.
5. **A split-package check** (one XML+class-file scan): the module system forbids it, but nothing asserts it.

**Fallback.** nbfx keeps `check-purity.xml` (PLAN §9).

**Prototype in nbfx.** Implement the five checks as **portable ant tasks** (no nbfx dependency) under
`nbfx-tools/` (the nbfx `nbbuild` side), run them in nbfx's `check-purity`, and donate the files. Steps
`refactor/guardrails` + `chore/upstream-module-tools` (M0; cheapest and immediately useful to NetBeans).

## H.7 Friend patches → upstream public APIs

`patches/0001-friend-core-startup.patch` (`RunLevel` in `org.netbeans.core.startup`),
`patches/0003-friend-maven-embedder.patch` (`org.netbeans.modules.maven.embedder`); `0002` is deletable
(`maven.api` is public).

**Change.** Propose a public `RunLevel` (an `org.netbeans.api.modules`/`core.startup` API surface) and a
supported Maven-embedder facade. **Fallback.** Keep the patch with a register entry (Appendix D.2).
**Prototype in nbfx.** `platform.launcher` uses the friend today: refine the *shape* we need into a small nbfx
interface + adapter and keep the friend call behind it, so the proposal is a diff. Steps `spike/runlevel-api`
(M0), `spike/maven-embedder-facade` (M2).

## H.8 Prototyping map (what to write, where, and what makes it credible)

| Proposal | nbfx prototype module / step | Credibility test (the evidence we attach to the proposal) |
|---|---|---|
| H.1 `java.source` split | `java.model` + `spike/java-source-headless` | apply a refactoring to the FX model via int offsets + `Writer`, no `Document` on the classpath of the test |
| H.2 `api.progress` neutral | `api.progress` (M) + `platform.progress.bridge` | the same neutral model drives the nbfx progress UI **and** an FX `ProgressUIWorkerProvider` |
| H.3 `openide.awt` neutral | `api.actions.core` + `spike/actions-neutral-core` | the file set is a 1:1 mirror of upstream's neutral classes; a sample plugin compiles against it |
| H.4 `openide.nodes` headless | `api.nodes` `IconRef` + `spike/nodes-icon-source` | every icon in the app resolves through `IconRef`; no `java.awt`/`javafx` in `api.nodes` |
| H.4.1 `DataObject` node-free | `spike/loaders-node-free` | `getLookup()`/`getCookie()` work without initializing `openide.nodes` |
| H.5 `editor.model` | `platform.editor` + `spike/editor-model-spi` | the FX editor implements the neutral SPI; a headless test drives it without a window |
| H.6 `nbbuild` tooling | `nbfx-tools/` + `check-purity.xml` + `sigtest` on `nbfx.api`/`nbfx.annotations` | the five checks run on nbfx with zero nbfx-specific code |
| H.7 friend APIs | `platform.launcher` + `spike/runlevel-api`, `spike/maven-embedder-facade` | the friend call sits behind a one-file adapter; the proposal is a diff of that file |

Order of attack (value × cost): **H.6, H.2, H.1, H.3, H.4, H.7, H.5.** H.6 and H.2 are small and immediately
useful upstream; H.1 pays nbfx back at M1; H.5 is the only architectural one and is sequenced last.

## H.9 What the plan requires

- A tracked issue per proposal (upstream + nbfx), created before the prototype branch lands.
- The prototype is the first customer proof; the branch may not change the neutral SPI after the proposal is
  filed without reopening it (the SPI is frozen by `sigtest`).
- **At least one accepted upstream change per milestone** (PLAN §13.10): H.6/H.2 are M0 candidates, H.1/H.3
  M1, H.4/H.7 M2, H.5 M2+.
- If a proposal is refused, the fallback above is adopted explicitly and the row is struck — no silent drift.

## H.10 Filing mechanics and SLA (being explicit)

| Aspect | Rule |
|---|---|
| Tracker | one GitHub issue in `apache/netbeans` per proposal (the upstream change) plus one in the nbfx tracker linking the prototype; the nbfx issue is closed only when the upstream one is resolved |
| Form | the PR is the **prototype diff**, reduced to the neutral change — no nbfx-specific code, no rename noise |
| Size (est.) | **S**: H.2, H.6, H.7 · **M**: H.1, H.3 · **L**: H.4, H.5 (H.5 needs an experimental module first) |
| Reviewer | the `nbbuild`/platform module owner for tooling (H.6), the `ide/` editor owners for H.5, the `java/` owners for H.1; the review must include the `arch.xml`/`apichanges` discipline of §4 |
| Evidence attached | the nbfx prototype, `sigtest` output, and the first-customer test (H.8's credibility column) |
| Upstream acceptance | merged **and** additive (the old API remains) **and** a `sigtest` added |
| Who files | an nbfx owner from §10.4; no proposal is filed without a named owner |
| SLA | no upstream decision within **two milestones** → adopt the fallback, close the row, and re-open only if an upstream owner picks it up. No row may sit "in progress" indefinitely |
