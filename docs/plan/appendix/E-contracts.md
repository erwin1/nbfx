# Appendix E — Contracts

`PLAN.md` promises a plugin API and "the FX thread never blocks", but no SPI has a thread rule, no view has
an owner, and no leak test exists. The tree shows the cost: **134 `subscribe(` call sites, 22 files that
mention `dispose()`** ([`../figures.md`](../figures.md)), 163 per-call `Lookup` uses, 4 registries on `Lookups.forPath`. This appendix is the
part of the API surface a third-party author must be able to read before writing a plugin.

## E.1 Threading contract

Every published type states one of: **FX** (may only be called on the JavaFX application thread), **BG**
(may only be called on a background thread / must be cancellable), **ANY** (thread-safe). The rule for
nbfx-internal code is the same, so a violation is a defect in our code, not only in a plugin's.

| SPI / type | Thread | Notes |
|---|---|---|
| `api.actions.Command#execute` | FX | short UI work; long work is a job over `api.execution`/`api.progress` |
| `api.actions.CommandsProvider#create*` | ANY | returns command objects; UI binding on FX |
| `api.actions.ActionRegistry` / menu building | FX | builds FX controls |
| `api.views.ViewProvider#create` | FX | returns a `Node`; the shell owns it |
| `api.views.ViewManager` open/close | FX | |
| `api.nodes.NodeModel` / `ChildrenModel` | ANY | the model is thread-safe; **view refresh is marshalled to FX by the view** |
| `api.property.PropertyEditor` | FX | editors are controls |
| `api.options.OptionsPanelController` | FX | `apply()` runs on FX; I/O inside goes through a job |
| `api.settings.Settings` get/put | ANY | listener callbacks delivered on the setting thread; UI marshals |
| `api.project.ProjectModel` | ANY | mutations off-FX; the view refreshes on FX |
| `api.build` `BuildExecution` / `api.execution.ExecutionService` | BG | events (`ExecutionHandle`) are delivered to the caller's executor; UI marshals |
| `api.output.OutputConsole#write` | ANY | thread-safe sink |
| `api.search.SearchProvider#search` | BG | must honour the cancellation token |
| `api.editor.EditorService` / `EditorDocument` | FX | text model lives on FX; semantic services take an immutable snapshot (Appendix B) |
| `api.editor.intelligence.*` (`GotoProvider`, `HintProvider`, …) | BG | receive a snapshot; cancellable; results are value types |
| `api.wizard.WizardIterator` panels | FX | `instantiate()`/file I/O may be BG — stated per method |
| `api.debug` / `api.profile` backends | BG | session events marshalled by the view layer |

Tests: a violation detector is hard, but the **contract test per SPI** (call from the wrong thread, expect the
documented failure or a no-op) is feasible and required for the T2-FX/M modules.

## E.2 Disposal and ownership

The current code leaks by construction: 134 `.subscribe(` sites, 22 files with `dispose()`.

Rules:

1. **A subscription is a resource.** `subscribe(...)`/`addListener(...)` through an nbfx API returns a
   `Subscription` (or `AutoCloseable`), never `void`. The owner stores it.
2. **A view owns its resources.** The shell disposes a view's `Subscription`s, bindings and children when
   the view is closed; a view must not register anything the shell cannot reach.
3. **A node owns its children.** `api.nodes` has one parent rule; a handed-over node is owned by the shell
   (§4). Creating a node per read and never releasing it is a leak — the model must cache.
4. **A project owns its caches.** `ProjectModel`/`java.model` caches (`ClasspathInfo`, snapshots) are keyed
   by project and dropped on project close.
5. **Registration is not disposal.** Layer-registered contributions (`@ViewRegistration`) are singletons; the
   *instance* the shell creates is disposable, the registered class is not.

Deliverables: the `Subscription` type; retrofit the 134 sites; a **leak test** that opens/closes a view N
times and asserts (a) the registered-listener count returns to baseline and (b) the view is weakly reachable
after GC; a **journal test** for editor open/close (the prototype's `ExternalChangeSupport`,
`LineHighlightSupport`, `AnnotationSupport` all register listeners).

## E.3 Accessibility

`javafx.scene.AccessibleRole` on every published view/control root, plus accessible text for the semantic
surfaces (usages tree, structure tree, problems list, variables). This is a contract of `api.views` (the
`Node` the plugin returns must carry a role or the shell assigns one) and of `platform.nodes` (the FX tree
views set roles for their cells). M1, not M4 — a published API that produces inaccessible `Node`s cannot be
fixed later without breaking plugins.

## E.4 Error handling and logging

- **Reuse `ErrorReporter`** (`nbfx.api` root, moving to `api.dialogs`) for user-visible failures: report,
  never swallow; `IOException` on save is data (Appendix B `EditorDocument.save`).
- **Logging** is `java.util.logging.Logger` named after the module code-name; the shell installs the UI
  handler (`api.dialogs`) so background failures surface in the notification line.
- **Semantic-layer logs**: `SilentJavacLogs` (in `nbfx.editor`) is a *contract* of `java.model` — javac
  diagnostics are data, not log noise. Keep it there and make it explicit.
- **No `printStackTrace`, no `System.out`** in nbfx code (guard).

## E.5 The `Lookup` rule (precise, testable)

`PLAN.md` §5/§11 say "registries cache `Lookup.Result`s; never `Lookups.forPath` per query". The tree has 163
per-call `Lookup` uses and 4 registries that resolve via `Lookups.forPath` (`OptionsRegistry:47`,
`StatusElementRegistry:67`, `ViewRegistry:60`, `WizardRegistry:50`). Meaning:

1. **Layer resolution happens once**, at registry initialisation, into a retained `Lookup.Result`.
2. **Iteration uses `Result.allInstances()`** (fires on change) or a cached `List`, never a fresh
   `Lookups.forPath(...).lookupAll(...)` per user action.
3. **A "call" is a user action, a repaint, or a document change** — the testable definition. The guard/unit
   test counts `forPath` invocations across N actions (must stay constant).
4. **Singleton services** are `@ServiceProvider`; contributions are layer registrations (Decision 4).
5. **The active context** (what a command acts on) is a `Lookup` published by the shell when focus changes —
   computed once per context change, not per command lookup.

## E.6 Plugin allowlist (what §4 must actually say)

§4 currently allows only `org.openide.util`, `org.openide.filesystems`, `org.openide.util.lookup`. That is not
enough to build a plugin. The allowlist becomes:

| Allowed for a plugin (T2-FX/M modules + platform core) | Never |
|---|---|
| `org.openide.filesystems` (`FileObject`, `FileUtil`) | T0 modules |
| `org.openide.util` / `util.lookup` (`Lookup`, `Lookup.Provider`) | layer paths (`LayerPaths`) |
| `org.openide.loaders` (`DataObject`, `DataFolder`) — **API only, never `DataNode`** | `*Impl` classes, view layouts, theme CSS |
| `org.netbeans.api.project` (`Project`, `ProjectInformation`, `LookupProvider`) | `org.netbeans.modules.*` internals |
| `org.openide.util.NbBundle` / `NbPreferences` | `javafx.embed.swing.*` |

The published API modules of the extension points the plugin implements; nothing else. State it as a table in
`api.plugins`' `arch.xml` (the plugin-author doc).

### E.6.1 Registering things (what a plugin actually does)

| The plugin wants to… | It registers | Mechanism |
|---|---|---|
| add a command / menu item | `@ActionRegistration` + `@ActionReference` | layer, via `api.actions` |
| add a view | `@ViewRegistration` | layer, via `api.views` |
| add an options panel | `@OptionsRegistration` | layer, via `api.options` |
| provide a service | `@ServiceProvider(service = X.class)` | `META-INF/services` |
| handle a file type | a `DataLoader`/`MultiFileLoader` + MIME type | `openide.loaders` (`DataObject`/`DataFolder`) — **never `DataNode`**; using `DataObject` implies `openide.nodes` (D.2.1) |
| add a project type | `@ProjectTypeRegistration` + `ProjectFactory`/`LookupProvider` | `api.project` + `org.netbeans.api.project` |
| add a language (lexer, completion, structure, goto, usages) | `LanguageRegistration` + providers scoped by `LanguageId` | `api.language`; **never** a global `@ServiceProvider` (Appendix I.3) |
| build the plugin | — | an **NBM** via the NetBeans `nbm` Ant harness (what the module system expects), JDK 25, compiled against the T2-FX/M modules it implements |

The quickstart (skeleton plugin + build + a view and a command) is the deliverable
`feature/plugin-quickstart` (W8); a `nbm-maven-plugin` path is documented only if a maintained version exists.
Without this, M5's exit ("a third-party plugin builds") is untestable.

## E.7 Typed factories — remove the speculative generics

`PLAN.md` §5.2 proposes `OptionsPanelFactory<C>`, `WizardUi<C>`, `PropertyEditorFactory<C>`, `ViewFactory<C>`
and forbids a raw `Lookup.lookup(OptionsPanelFactory.class)`. **There is exactly one front end (JavaFX).**
NetBeans `Lookup` cannot resolve a parameterised type, so the only way to obtain `OptionsPanelFactory<Node>`
is a cast — i.e. the plan forbids the mechanism it needs. Tulach's rule applies: *two implementations make a
generalisation; one implementation makes a parameter.*

Decision: **publish concrete FX types** (`OptionsPanelFactory` returning `javafx.scene.Node`), which is
correct for a T2-FX tier, and put the portability in the **data model**, not the component type:

```java
// T2-FX: one front end, so Node is honest
public interface OptionsPanelFactory { javafx.scene.Node create(PanelModel model); }

// M: the portable part — toolkit-free, testable headless
public interface PanelModel { List<FieldModel> getFields(); Validator getValidator(); List<String> getActionIds(); }
public interface FieldModel { String getLabel(); PropertyEditorRef getEditor(); boolean isEnabled(); }
```

A future non-FX front end binds **`PanelModel`** (a table of fields/validators/commands), not a component
factory. If a *second component toolkit* ever appears, the mechanism is a `Class<C>` token on the factory
plus one centralised checked adapter — sketch it then, not now. Update §5.2 and Appendix A accordingly; the
`WizardUi<C>`/`ViewFactory<C>` names disappear.

## E.8 i18n

Already real in the tree (54 `Bundle.properties`, 136 files using `NbBundle`). Keep it and guard it: every
user-visible string is a `NbBundle.getMessage` from the module's `Bundle.properties`, including generated
layer metadata (`displayName` `#key`). A missing key is a build error, not a runtime placeholder.

## E.9 The signature book (what Appendix E is not yet)

This appendix fixes the *contracts*. The per-module **exported class list + signatures** (the `arch.xml`
`<api>` content) is a large, mechanical deliverable per governed module; it belongs with the `arch.xml` files
in `feature/api-governance` (W2), not in a prose appendix. What is fixed here: the thread column, the
disposal rule, the a11y requirement, the `Lookup` rule, the plugin allowlist, and the removal of the
generic-`C` factories.
