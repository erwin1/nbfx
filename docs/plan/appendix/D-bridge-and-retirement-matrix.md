# Appendix D — Bridge and retirement matrix

`PLAN.md` §1 says "reuse the headless engines as-is" and §13.11 says "retirement is the deliverable". Neither
is executable until every upstream module has a row: **reuse / bridge / replace / drop**, an owner and a
milestone. This is that matrix, against the tree (Swing file counts from `grep -rl 'javax.swing\|java.awt'`
per module, `netbeans/`).

The three mechanisms are defined in [Appendix A.3](A-toolkit-policy.md): **reuse** (depend on it, it is
headless), **bridge** (keep the upstream API and reimplement its provider/implementation with FX; callers do
not change), **replace** (write our own and drop the upstream module), **drop** (feature out of scope).

## D.1 Upstream UI APIs that reused headless modules *call*

These are the dangerous ones: the module is headless, but it calls a Swing API. Nothing in our code may
import these types, so the mechanism is always *provide a service* or *patch the caller*. The "exemption"
column is what the bridge module must be allowed to carry permanently (Appendix A.6).

| API | Module | Provider-based? | Swing in the signature | Mechanism | Permanent exemption |
|---|---|---|---|---|---|
| `DialogDisplayer` | `platform/openide.dialogs` | yes (`Lookup.getDefault().lookup(DialogDisplayer.class)`, `DialogDisplayer.java:59-60`; module imports `javax.swing.*:39`) | `NotifyDescriptor` options may be `javax.swing.Action`; `Dialog` is the abstract return | **bridge**: register an FX `DialogDisplayer`; accepted options are converted | `javax.swing.Action` (option conversion) |
| `NotificationDisplayer` | `platform/openide.dialogs` | yes (Lookup) | `Action` | **bridge** | `javax.swing.Action` |
| `StatusDisplayer` | `platform/openide.awt` | yes (`StatusDisplayer.java:60-62`) | `getStatusLine()` returns a `JComponent` — **cannot be bridged** | **bridge the text** (`setStatusText` → FX status bar) and **replace** the status line; an embedder of the Swing status line breaks (document it) | none if we never call `getStatusLine()`; else `java.awt` |
| `StatusLineElementProvider` | `platform/openide.awt` | yes | `getStatusLineElement()` returns `java.awt.Component` (`StatusLineElementProvider.java:35`) | **replace** (our `api.status.StatusElement`) | must stay unused |
| `IOProvider` / `InputOutput` | `platform/openide.io` | yes (Lookup) | `InputOutput.select()` takes `javax.swing.Action`; `IOSelect` | **bridge**: register an FX `IOProvider` over `api.output` | `javax.swing.Action` (selection) |
| `ProgressHandleFactory` | `platform/api.progress` | yes (`Lookup.getDefault().lookup(ProgressUIWorkerProvider.class)`, `ProgressHandleFactory.java:229`) | `ProgressHandle` imports `javax.swing.Action`, `java.awt.event.ActionListener` (`ProgressHandle.java:22-27`); the worker SPI is Swing-typed | **bridge**: register an FX `ProgressUIWorkerProvider`; everything else keeps calling `ProgressHandleFactory` | `javax.swing.Action`, `java.awt.event.ActionListener` |
| `Node` / `ExplorerManager` | `platform/openide.nodes/explorer` | — | `getIcon()` returns `java.awt.Image` | **replace** with `api.nodes` (not used today — keep it that way) | must stay unused |
| `TopComponent` / window system | `platform/core.windows` | — | whole API | **replace** with `platform.workspace` (already absent from the cluster) | must stay unused |
| `EditorCookie` / `Document` | `ide/editor.*` | — | `javax.swing.text.Document` | **replace** with `platform.editor` + character offsets (Appendix B) | must stay unused |
| `Keymap` | `platform/api.keymap` | partial | `KeyStroke`-based UI | **replace** with `api.actions.KeyMap` | must stay unused |

## D.2 Upstream modules the nbfx tree depends on today

| Upstream module | Consumer(s) in nbfx | Mechanism | Our module | Milestone |
|---|---|---|---|---|
| `org.netbeans.modules.projectuiapi.base` | `launcher`, `ui` (both exempted in `check-purity.xml:70-92`) | **replace**: `OpenProjects` bridge → `api.project` model | `platform.shell`, `platform.launcher` | M0 |
| `org.netbeans.modules.parsing.indexing` | `launcher`, `ui`, `findusages`, `project.ui.java` | **reuse** (headless index) + the indexing workstream (§D.5) | `java.model` / `ide.search` | M0/M1 |
| `org.netbeans.api.java`, `api.java.classpath` | `editor`, `findusages`, `structure.java`, `project.ui.java`, `debug` | **reuse** (headless) | java layer | M1/M2 |
| `org.netbeans.modules.java.source.base` (`org.netbeans.api.java.source`) | `editor`, `findusages`, `structure.java`, `project.ui.java` | **bridge** (`java.model`) | `java.model`, `java.editor` … | M1 |
| `org.netbeans.libs.javacapi` / `nbjavacapi` | `editor`, `findusages`, `structure.java` | **reuse**, decision in §D.6 | `java.model` | M1 |
| `org.netbeans.modules.java.project` | `findusages`, `project.ui.java` | **reuse** the headless model (`Sources`, `SourceGroup`, `ClassPathProvider`); **replace** `java.project.ui`/`LogicalViewProvider` (`Node`) | `java.projecttype.*`, `java.navigation` | M1/M2 |
| `org.openide.loaders` | `project.ui.java` | **reuse** `DataObject`/`DataFolder` API; **replace** `DataNode` (Swing) — never import it | `ide.project.ui` | M1 |
| `org.netbeans.api.progress` | `plugins` | **bridge** (§D.1) | `platform.progress` | M0 |
| `org.netbeans.api.debugger.jpda`, `org.netbeans.modules.debugger.jpda` | `debug` | **reuse** the engine (JPDA session/breakpoints); **replace** `debugger.jpda.ui` (Swing views/actions) | `java.debug` + `ide.debug` | M2 |
| `org.netbeans.modules.maven`, `…maven.embedder` | `project.ui.maven` | **reuse** (embedder until builds move to a process); friend patch `0003` until then | `java.projecttype.maven` | M2 |
| `org.netbeans.modules.libs.gradle` | `project.ui.gradle` | **reuse** | `java.projecttype.gradle` | M2 |
| `org.netbeans.modules.autoupdate.services` | `plugins` | **reuse** (headless update engine); plugin-manager UI is ours | `ide.plugins` | M1 |
| `org.netbeans.core.startup` | `launcher` | **reuse** with friend patch `0001` (`RunLevel`); propose upstream | `platform.launcher` | M0 |
| `org.netbeans.modules.defaults` | `launcher` | **reuse** (layer defaults) | `platform.launcher` | M0 |
| `org.netbeans.modules.queries`, `java.lexer`, `lexer`, `xml.lexer` | `editor`, `findusages`, `project.ui.java` | **reuse** (headless tokenizers/queries) | java/ide layers | M1 |
| `org.netbeans.modules.projectapi` | 9 modules | **reuse** (headless `Project` model) | all layers | — |
| `nbjunit`, `libs.junit4` | tests | **reuse** (test only) | — | — |

## D.3 The editor and Java-tooling matrix (50 upstream modules in the measured set — [`../figures.md`](../figures.md))

Swing ratio = files importing `javax.swing`/`java.awt` ÷ Java files (measured). This table is what makes M1
and M2 schedulable.

### Replace — Swing core, the editor itself (our `platform.editor` / `ide.editor.*`)

| Upstream | Swing/files | Note |
|---|---|---|
| `ide/editor.lib2` | 122/164 | the Swing editor core — replaced by the FX code area |
| `ide/editor.lib` | 110/162 | ditto |
| `ide/editor` | 51/63 | the mux/kit layer |
| `ide/editor.document` | 32/47 | `javax.swing.text.Document` model — replaced by the FX text model + offsets |
| `ide/editor.completion` | 22/30 | completion popup — replaced by our popup; **keep the SPI shape** in `api.editor` |
| `ide/editor.codetemplates` | 20/32 | templates UI — replaced; the template model is `api.templates` (M) |
| `ide/editor.search` | 20/21 | replaced by our search bar |
| `ide/editor.bookmarks` | 19/30 | replaced by `ide.bookmarks` |
| `ide/editor.bracesmatching` | 17/19 | replaced by `platform.editor` brace matcher (already exists in the prototype) |
| `ide/editor.fold` + `ide/editor.fold.nbui` | 16/34 + 13/16 | replaced by FX folding |
| `ide/editor.guards` | 11/22 | replaced (typing guards are in `platform.editor`) |
| `ide/editor.indent` | 11/13 | replaced; the *rules* (`editor.indent.support`, `editor.indent.project`) are partly reusable |
| `ide/editor.deprecated.pre65formatting` | 11/16 | **drop** |
| `ide/editor.errorstripe` | 10/12 | replaced by our annotation/mark layer (keep `editor.errorstripe.api` as a shape reference only) |
| `ide/editor.breadcrumbs` | 8/8 | replaced by the prototype's breadcrumbs |
| `ide/editor.structure` | 6/15 | replaced by `ide.structure` |
| `ide/editor.settings.lib` | 1/17 | replaced (prefs model reused via `api.settings`) |
| `ide/editor.settings` | 5/11 | UI replaced; storage model reused |
| `ide/editor.actions` | 12/12 | replaced by `api.actions` |
| `ide/editor.macros` | 8/10 | **drop** (or M4) |
| `ide/editor.plain`, `ide/editor.plain.lib` | 1/1, 0/2 | **drop** (we are not a plain-text kit) |
| `ide/editor.kit` | 0/0 | empty wrapper — drop |

### Replace — the Java editor (our `java.editor` / `ide.editor.*`)

| Upstream | Swing/files | Note |
|---|---|---|
| `java/java.editor` | 88/117 | the Java editor kit/mux — replaced |
| `java/java.editor.base` | 9/14 | completion/actions — replaced; **its logic is what the prototype reimplemented over `java.source`** |
| `java/java.editor.lib` | 1/8 | replaced |
| `java/java.sourceui` | 13/26 | hyperlinks/completion UI — replaced |

### Bridge / reuse — the headless Java model and parsing (our `java.model`)

| Upstream | Swing/files | Mechanism |
|---|---|---|
| `java/java.source.base` | 39/217 | **bridge**: `ClasspathInfo`/`JavaSource`/`CompilationInfo` behind `java.model`; the 39 Swing files are the `Document`-based helpers we must not call (`getDocument()`) |
| `java/java.source` | 24/50 | **bridge**: `Source`/`WorkingCopy`; the `Document` half is not used |
| `java/java.source.queries` | 0/10 | **reuse** (headless queries API) |
| `java/java.source.queriesimpl` | 1/4 | **reuse** (index-backed query impl) |
| `java/java.source.ant` | 1/2 | **reuse** |
| `java/java.source.compat8` | 0/1 | **reuse** |
| `java/java.hints` | 70/274 | **bridge**: reuse the *computation* (`HintsInvoker`, `JavaHintsProvider`), replace the presentation and the `Fix` plumbing; `ChangeInfo` carries `javax.swing.text.Position` (`ChangeInfo.java:24`) — convert |
| `java/java.hints.declarative` | 13/40 | **bridge**: reuse the rule parser/engine, replace the UI |

### Reuse / partly reuse — editor services that are not the editor

| Upstream | Swing/files | Mechanism |
|---|---|---|
| `ide/editor.settings.storage` | 10/15 | **reuse the storage** (preferences), replace the settings UI |
| `ide/editor.tools.storage` | 0/2 | **reuse** |
| `ide/editor.util` | 7/22 | **reuse the headless half** (position/`CharSequence` utilities), replace the rest |
| `ide/editor.indent.support` | 1/1 | **reuse** the rule SPI; replaced by our indenter |
| `ide/editor.indent.project` | 2/4 | **reuse** |
| `ide/editor.global.format` | 2/2 | **reuse/bridge** (format action is ours) |
| `ide/editor.autosave` | 3/4 | **reuse** the timer, our persistence |
| `ide/editor.errorstripe.api` | 1/5 | **reuse as a shape reference only** (the API, not the impl) |

Counts to keep in view: the replaced editor surface is ~600 upstream Java files; the reused Java model is
~300. This is the M1/M2 critical path (PLAN §10).

## D.4 Retirement matrix (what must be *absent*, per milestone)

Asserted by `check-packaging` (PLAN §9). Absent from `nbfx*/modules` and the `netbeans/` clusters the app runs
on; not merely unused.

| Milestone | Modules that must be gone (or replaced in place) |
|---|---|
| M0 | `core.windows` (already), `projectuiapi.base` bridge (our code no longer depends), the status/output/dialog/progress **providers** we replaced, `ide/editor*` from the runtime path (the app does not load them) |
| M1 | `ide/editor.lib`, `ide/editor.lib2`, `ide/editor.document`, `ide/editor.completion`, `ide/editor.search`, `ide/editor.bracesmatching`, `ide/editor.codetemplates`, `ide/editor.fold*`, `ide/editor.guards`, `ide/editor.indent*`, `ide/editor.actions`, `ide/editor.structure`, `ide/editor.settings.lib`, `ide/editor.errorstripe`, `ide/editor.bookmarks` |
| M2 | `java/java.editor`, `java/java.editor.base`, `java/java.editor.lib`, `java/java.sourceui`, `java/java.hints`' UI half, `java/java.project.ui` |
| M3 | `java/java.j2seproject`'s Swing project UI, SceneBuilder's Swing shell (if any) |
| Later | `java/java.source`'s `Document` half, `platform/api.keymap` UI |

## D.5 The indexing workstream (currently missing)

`ClasspathInfo`/completion are meaningless without the parser + index. `parsing.indexing` is already pulled by
`launcher`, `ui`, `findusages` and `project.ui.java`, but the plan has no *workstream* for it. Add
`feature/indexing` (M0/M1):

- reuse `parsing.api`/`parsing.indexing`; index the opened project on open, with visible progress;
- define the memory budget (index + javac model) in the `check-budget` baseline;
- define what happens when the index is incomplete (completion degrades, no blocking);
- cancellation: every semantic request is a cancellable background job (Appendix E).

## D.6 Compiler choice (decision, not a footnote)

`libs.javacapi` (JDK `javac`) vs `libs.nbjavacapi` (nb-javac) is a project decision with three consequences:
the `javax.lang.model` identity hazard (Appendix A.4), the source-level support (nb-javac tracks newer
features on older JDKs), and the licence/bundle size. **Decision proposed:** JDK javac by default, nb-javac
only where the project's source level requires it; the choice is owned by `java.model` (Appendix B) and
recorded in the module metadata.

## D.7 Other language clusters (the second-implementation check)

Measured ([`../figures.md`](../figures.md)); ~75 % of each cluster is headless, so the reuse / bridge / replace
matrix generalizes at roughly **3:1**:

Count **modules**, not files (a module with one `javax.swing.Action` must be bridged, not reused):

| Cluster | modules | Swing-free modules | java files | Swing-touching |
|---|---|---|---|---|
| groovy | 11 | 4 | 259 | 73 |
| php | 38 | 5 | 1833 | 475 |
| enterprise (JavaEE) | 117 | 47 | 5079 | 1269 |
| webcommon | 59 | 22 | 1415 | 420 |

`groovy/groovy.antproject` (16 files, **0 Swing**), `groovy/libs.groovy`, `groovy/gradle.groovy` and
`groovy/groovy.kit` are reusable as-is; `groovy/groovy.refactoring` (47, 13) splits like the Java editor; and
`groovy/groovy.editor`'s **headless half** (`api/elements` +`ast`/`common`, `compiler` +`error`,
`completion/inference`, `completion/util`, `java`, `occurrences`, `spi/completion` — all 0 Swing) is reusable
once its UI is split, making `groovy.editor` a further upstream split candidate (Appendix H). But **the
Swing-in-the-API defect recurs**: `php/php.api.phpmodule` is **9/20 Swing (45 %)** — a
Swing-typed API module — `php/php.project` 116/268, `php/php.refactoring` 18/28. So
[Appendix H](H-upstream-proposals.md)'s splits apply to *any* language API module, not just `java.source`. The
architecture is proven or broken by the spikes of [Appendix I](I-language-soundness.md). The measured
**offender list** (API/SPI modules that import Swing, 13 of them) is generated in
[`../figures.md`](../figures.md) and analysed in [Appendix H](H-upstream-proposals.md).
