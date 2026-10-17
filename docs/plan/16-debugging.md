# 16 — Debugging support (JPDA / JVM)

Steps: **Debug foundation** (`feature/debug-foundation`), **Debug views** (`feature/debug-views`),
**Debug breakpoints** (`feature/debug-breakpoints`), **Debug stepping & editor**
(`feature/debug-stepping-editor`), **Debug evaluate** (`feature/debug-evaluate`), **Debug attach &
monitors** (`feature/debug-attach-monitors`), **Debug build integration**
(`feature/debug-build-integration`), **Debug options** (`feature/debug-options`); later: **Debug remote**
(`feature/debug-remote`), **Debug in-place fixes** (`feature/debug-inplace-fixes`), **Debug visual**
(`feature/debug-visual`).

Decisions 1–4 in §11 are **locked** (see §4 "Decisions").

## 1. Goal and non-goals

**Goal.** Full **JVM debugging parity** with NetBeans for nbfx:

- reuse the headless NetBeans debugger core (`api.debugger`, `api.debugger.jpda`, `debugger.jpda`,
  `debugger.jpda.projects`),
- rebuild the **entire debugger UI in JavaFX** (views, actions, breakpoint customizers, editor
  integration, options),
- wire launching/run-configuration integration through `nbfx.execution` (doc 15),
- JPDA (JDI) features: breakpoints, stepping, call stack, variables/watches, evaluate, monitors,
  attach/remote.

**Non-goals (first pass).** Non-JVM debuggers (NetBeans has no DAP module — a generic DAP layer is a
separate future project), visual debugger, heap walker/profiler, in-place code fixes (hot swap),
Truffle/JS/PHP/ruby debugging.

**Definition of done (v1).** Debug a Java main and a JUnit test from the IDE: set/disable conditional
breakpoints in the gutter and the Breakpoints view, hit them, step (over/into/out/smart/into-next-method/
pop-to-here/run-to-cursor), inspect threads/call stack/locals/this/fields/tables in lazy trees, add
watches, evaluate expressions, see the current-line highlight and navigate to source, stop/restart the
session, attach to a running JVM, and see debuggee output in the Terminal/Output.

## 2. What nbfx has today (class by class)

`nbfx-ide/c.gluonhq.netbeans.nbfx.debug` (~1211 lines). Deps: `nbfx.annotations`, `nbfx.api`,
`nbfx.output`, `nbfx.lib.javafx`, `org.netbeans.api.debugger`, `org.netbeans.api.debugger.jpda`,
`org.netbeans.api.java.classpath`, `org.netbeans.modules.debugger.jpda`, `openide.filesystems`,
`openide.util(.lookup)`.

| Class | Lines | What it does | Fate |
|---|---|---|---|
| `DebugSession` | 379 | launches `java -agentlib:jdwp=…,address=127.0.0.1:0`, reads the port from stdout, `JPDADebugger.attach(host, port, HashMap{name,sourcepath,baseDir})`, exposes threads/frames/variables as FX observables, `ActionsManager` actions, `LineBreakpoint` on the caret line, breakpoint add | **replace** by `nbfx.debug.core` session + launch (keep the services-map knowledge) |
| `DebugView` | 165 | thread `ListView`, call-stack `TableView` (class/method), flat variables `TableView` (name/type/value), toolbar (continue/step over/into/out/stop) + status | **replace** by the FX Debugging view |
| `DebugLauncher` | 95 | runnable Java file (`static void main`) → main class + `ClassPath` EXECUTE/SOURCE + workdir + sourcepath + caret line | **move** to `nbfx.debug.core` launch resolution; extend for run configs |
| `LaunchRequest` | 38 | record (mainClass, classpath, workDir, sourcePath, mainFile, breakpointLine) | **extend** into `DebugLaunchRequest` |
| `ToggleBreakpointCommand` | 81 | gutter/caret line breakpoint toggle via `LineBreakpoint.create` + `DebuggerManager` | **move** to core + hook the editor annotation/action |
| `SessionCommand` | 73 | `Command` base, disabled unless a session is active | **keep/refactor** into the actions model |
| `DebugCommandsProvider` / `DebugMenuRegistrations` / `DebugToolbarRegistrations` | 53/49/48 | Register `DebugFile`/`Continue`/`Step*`/`Stop`/`ToggleBreakpoint` commands + Debug menu (position 50) + toolbar | **extend** with the full action set (Pause, Run to Cursor, Pop, New Watch, Evaluate, Attach, Fix) |
| `DebugFileCommand` | 79 | "Debug File" entry point | **replace** by Debug File/Project/Test via run configs |
| `DebugStatus` | 37 | `ACTIVE` boolean for enablement | **replace** by the session state observable |
| `DebugViewProvider` | 87 | registers the `debug` view (dockable) | **extend** into the multi-view provider |
| `VariableRow` | 27 | flat variable row | **replace** by the variable view model |

Also present: `ActionIds` debug constants (`DEBUG_FILE`, `DEBUG_CONTINUE`, `DEBUG_STEP_INTO/OVER/OUT`,
`DEBUG_STOP`, `TOGGLE_BREAKPOINT`) in `nbfx.api.actions`; editor annotation SPI
(`nbfx.api.editor.EditorAnnotationProvider`/`EditorAnnotation`, `nbfx.editor` `AnnotationSupport` +
`MarkLineNumberDecorator`) for gutter badges.

## 3. Research: the original debugger — reuse vs port, class by class

### 3.1 Headless core — reuse as module dependencies

**`org.netbeans.api.debugger`** — `ide/api.debugger` (38 files; Swing only in the annotation processor).

- `DebuggerManager` — sessions/engines/breakpoints/watches/lookup:
  `getDebuggerManager()`, `getActionsManager()`, `startDebugging(DebuggerInfo)`, `finishAllSessions()`,
  `getCurrentSession()`/`setCurrentSession()`, `getSessions()`, `getDebuggerEngines()`/`getCurrentEngine()`,
  `addBreakpoint`/`removeBreakpoint`/`getBreakpoints()`, `createWatch(index, expr)`,
  `createPinnedWatch(expr, pin)`, `getWatches()`, `removeAllWatches()`, `reorderWatches(perm)`,
  `addDebuggerListener`/`addDebuggerListener(property, l)`, `lookup(folder, service)`,
  `lookupFirst(folder, service)`, `join(ContextProvider, ContextProvider)`.
- `Session` — a session has engines pluggable by language/type; `DebuggerEngine` — per-engine
  `lookupFirst(null, Class)`, `getActionsManager()`, `ContextProvider`.
- `ActionsManager` — dispatch and enablement; the constants (exact, `ide/api.debugger/.../ActionsManager.java`):
  `ACTION_STEP_OVER`, `ACTION_RUN_INTO_METHOD`, `ACTION_STEP_INTO`, `ACTION_STEP_OUT`,
  `ACTION_STEP_OPERATION`, `ACTION_CONTINUE`, `ACTION_START`, `ACTION_KILL`, `ACTION_MAKE_CALLER_CURRENT`,
  `ACTION_MAKE_CALLEE_CURRENT`, `ACTION_PAUSE`, `ACTION_RUN_TO_CURSOR`, `ACTION_POP_TOPMOST_CALL`,
  `ACTION_FIX`, `ACTION_RESTART`, `ACTION_TOGGLE_BREAKPOINT`, `ACTION_NEW_WATCH`, `ACTION_EVALUATE`
  (plus more). Enablement/status through `ActionsProvider.isEnabled(action)`/`doAction(action)`.
- `Breakpoint` (abstract) — `getType()`, `getGroupName()`, `isEnabled()`, plus `PROP_*` change events;
  `Watch`, `Watches` (fixed/pinned), `ContextProvider`, `DebuggerInfo`, `Properties`.
- SPI (`org.netbeans.spi.debugger`): `ActionsProvider` (`Set getActions()`, `doAction(Object)`,
  `isEnabled(Object)`, `postAction`), `ActionsProviderSupport`, `DebuggerEngineProvider`,
  `SessionProvider`, `ContextProvider`, `DebuggerServiceRegistration(s)`.

**`org.netbeans.api.debugger.jpda`** — `java/api.debugger.jpda` (46 files, 3 with AWT).
`JPDADebugger` (`attach(host, port, services)`, `attach(name, services)`, `listen(…)`,
`startListening(…)`, `getState()`, `STATE_STARTING|RUNNING|STOPPED|DISCONNECTED`, `getSuspend()`/
`setSuspend()`, `PROP_STATE`/`PROP_CURRENT_THREAD`/`PROP_CURRENT_CALL_STACK_FRAME`/`PROP_SUSPEND`/
`PROP_THREAD_STARTED`/`PROP_THREAD_DIED`/`PROP_THREAD_GROUP_ADDED`/`PROP_CLASSES_FIXED`/
`PROP_BREAKPOINTS_ACTIVE`, `getAllThreads()`, `getCurrentThread()`, `getCurrentCallStackFrame()`,
`evaluate(expr)`, `waitRunning()`, `canFixClasses()`, `fixClasses(map)`, `canPopFrames()`,
`getSmartSteppingFilter()`, `getBreakpointsActive()`/`setBreakpointsActive()`,
`ENGINE_ID="netbeans-JPDASession/Java"`, `SESSION_ID="netbeans-JPDASession"`).
Types: `JPDAThread(+Group)`, `CallStackFrame` (line, method, class, `getLocalVariables()`/
`getThisVariable()`/`getArguments()`), the `Variable` hierarchy (`LocalVariable`, `ObjectVariable`,
`ArrayVariable`, `Field`, `ClassVariable`, `ClassType`, `This`, `Super`, `ReturnVariable`,
`MutableVariable`), `JPDAWatch`, `Evaluator`.
Breakpoints: `LineBreakpoint` (`create(url, line)`, `PROP_LINE_NUMBER`/`PROP_URL`/`PROP_CONDITION`/
`PROP_STRATUM`/`PROP_SOURCE_NAME`/`PROP_SOURCE_PATH`/`PROP_PREFERRED_CLASS_NAME`/`PROP_INSTANCE_FILTERS`/
`PROP_THREAD_FILTERS`/`PROP_LAMBDA_INDEXES`), `MethodBreakpoint`, `ExceptionBreakpoint`, `FieldBreakpoint`
(+`Field`), `ClassLoadUnloadBreakpoint`, `ThreadBreakpoint`, `JPDABreakpoint`
(`PROP_SUSPEND`/`SUSPEND_ALL|EVENT_THREAD|NONE`, `PROP_HIDDEN`, `PROP_PRINT_TEXT`, `isEnabled`,
`getCondition`). DI cookies: `LaunchingDICookie` (`create(transport, host, address, timeout, …)`),
`AttachingDICookie` (`create(host, port)`, `create(name)`), `ListeningDICookie`.
Helpers: `SourcePathProvider`, `SmartSteppingFilter`/`SmartSteppingCallback`, `DeadlockDetector`,
`MonitorInfo`, `BreakpointsClassFilter`, `BreakpointStratifier`, `InvalidExpressionException`.

**`org.netbeans.modules.debugger.jpda`** — `java/debugger.jpda` (190 files, 7 with AWT). The engine:

- root: `JPDADebuggerImpl` (state machine, event queue, thread cache, suspend/resume, class-prepare),
  `JPDADebuggerEngineImpl`, `JPDAStepImpl` (single-step mechanics), `SourcePath`.
- `breakpoints/`: `BreakpointImpl`, `LineBreakpointImpl`, `MethodBreakpointImpl`,
  `ExceptionBreakpointImpl`, `FieldBreakpointImpl`, `ClassBreakpointImpl`, `ThreadBreakpointImpl`,
  `ClassBasedBreakpoint`, `JPDABreakpointsActivation`, `BreakpointsEngineListener`,
  `PersistenceManager`, `SourceRootsCache`, `BreakpointsReader`, `BreakpointsFromGroup`.
- `models/`: `AbstractVariable`, `AbstractObjectVariable`, `ObjectLocalVariable`, `ArgumentVariable`,
  `ArgumentObjectVariable`, `ThisVariable`, `SuperVariable`, `ClassVariableImpl`, `ClassFieldVariable`,
  `ObjectFieldVariable`, `ArrayFieldVariable`, `FieldVariable`, `ReturnVariableImpl`,
  `ExceptionVariableImpl`, `Local`, `CallStackFrameImpl`, `JPDAThreadImpl`, `JPDAThreadGroupImpl`,
  `ThreadsCache`, `JPDAWatchImpl`/`JPDAWatchFactory`, `JPDAObjectWatchImpl`, `MonitorInfoImpl`,
  `JPDAClassTypeImpl`, `JPDAArrayTypeImpl`, `ObjectTranslation`, `ShortenedStrings`,
  `VariableMirrorTranslator`.
- `actions/`: `ContinueActionProvider`, `StepActionProvider`, `StepIntoActionProvider`,
  `StepOperationActionProvider`, `StepIntoNextMethod(ActionProvider)`, `KillActionProvider`,
  `PauseActionProvider`, `PopToHereActionProvider`, `StartActionProvider`, `JPDADebuggerActionProvider`,
  `SmartSteppingFilterImpl`/`Wrapper`, `CompoundSmartSteppingListener`, `ActionsSynchronizer`,
  `SuspendController`, `JPDAMethodChooserFactory`, `ActionStatusDisplayCallback`.
- `expr/`: `EvaluationContext`, `TreeEvaluator`, `EvaluatorVisitor`, `CanInterpretVisitor`, `VMCache`,
  `JDIVariable`, `InvocationExceptionTranslated`.
- `console/`: `DebuggerOutput`, `IOManager` (writes the debugger console through **`api.io`**).
- `jdi/`: JDI transport/`VirtualMachine` integration; `remote/`: `RemoteServices`, `RemoteClass`;
  `impl/`, `util/`, `spi/`.
- Deps: `api.debugger`, `api.debugger.jpda`, **`api.io`**, `api.java.classpath`, `libs.javacapi`,
  `editor.document`, `java.source.base`, `projectapi`, `openide.text`, `api.intent` — **no Swing module
  deps**. Swing/AWT code: 6 commented asserts, `VariableMirrorTranslator` (`java.awt.Font` rendering),
  `LineBreakpointImpl`/`BreakpointImpl` (`javax.swing.text.Document`/`ChangeEvent`).

**`org.netbeans.modules.debugger.jpda.projects`** — `java/debugger.jpda.projects` (18 files):
`SourcePathProviderImpl`, `EditorContextSupport`, `CodeCompiler`, `CodeSnippetCompiler`,
`ExpressionScanner`, `ParsedData`, `ConstantPool`, `ClassScanner`, `MethodArgumentsScanner`,
`AST2Bytecode`, `FixClassesSupport`, `IntroduceClass`, `FieldLNCache`, `WeakHashMapActive`,
`PreferredCCParser`, `ASTOperationCreationDelegate`.

### 3.2 Swing modules — cannot depend on; port to JavaFX

| Module | Size / forbidden deps | Port |
|---|---|---|
| `org.netbeans.spi.debugger.ui` (`ide/spi.debugger.ui`) | 94 files, 76 Swing; `openide.awt`, `openide.dialogs`, `openide.nodes`, `openide.windows` (+ editor/projectui) | `ViewFactory` (builds view components from registered models), `EngineComponentsProvider`, `DebuggingView`, `BreakpointType`/`AttachType` (+`JComponent` customizers), `CodeEvaluator`, `EditorContextDispatcher`, `Controller`/`PersistentController`/`ViewLifecycle`, `MethodChooser`, `ToolTipUI`, `BreakpointAnnotation`, `EditorPin`, `PinWatchUISupport`, `ColumnModelRegistration(s)`, `Constants` |
| `org.netbeans.spi.viewmodel` (`ide/spi.viewmodel`) | 47 files, 20 Swing; `openide.awt`, `openide.nodes`, `openide.windows`, `openide.explorer` | mirror the **model interfaces** (`Model`, `TreeModel`, `NodeModel`, `TableModel`, `Column`, `AsynchronousModel`, filters, `Models`, `ModelListener`); re-implement renderers (`OutlineTable`, `TableRendererModel`, cell editors) with FX `TreeTableView`/`TableView` |
| `org.netbeans.modules.debugger.jpda.ui` (`java/debugger.jpda.ui`) | 136 files, 103 Swing; `openide.awt`, `openide.dialogs`, `openide.nodes`, `openide.windows` | the full JPDA UI (class list in §3.4) |
| `org.netbeans.modules.debugger.jpda.projectsui` | 19 files, 14 Swing | debugger source-roots/source-path UI (project customizer) |
| `org.netbeans.modules.debugger.jpda.ant`, `org.netbeans.modules.ant.debugger` | deps clean but Ant-specific | Ant debug models/annotations/`AntDebugger` — reimplement in `nbfx.build.ant` or reuse selectively |
| `debugger.jpda.visual`, `profiler/debugger.jpda.heapwalk` | — | out of scope |

### 3.3 Wiring model (to mirror)

- **Views are model-driven.** `ViewFactory` builds a view component from `spi.viewmodel` models
  registered per **session id** in the layer:
  `java/debugger.jpda*/src/META-INF/debugger/netbeans-JPDASession/<ViewName>/{Models…}` (JPDA registers
  `LocalsView`, `WatchesView`, `SourcesView`, `ResultsView`, …; the engine registers
  `DebuggerEngineProvider`, `SourcePath`, smart-stepping).
- **Actions** are `ActionsProvider`s registered per context; `ActionsManager` aggregates them, delegating
  `isEnabled`/`doAction`; the UI shows them as buttons/menu items.
- **Breakpoints** live in `DebuggerManager`; the UI adds `BreakpointType` customizers; the editor shows
  annotations.
- **Console output** goes through `api.io` (`console/DebuggerOutput`) → the Output window (doc 15).
- **Session/engine lookup** by class (`session.lookupFirst(null, JPDADebugger.class)`).

### 3.4 `debugger.jpda.ui` class inventory (port targets)

Views/models: `DebuggingNodeModel`, `DebuggingTreeModel`, `DebuggingTableModel`,
`DebuggingTreeExpansionModelFilter`, `DebuggingMonitorModel`, `DebuggingViewSupportImpl`,
`ThreadsNodeModel`, `ThreadsTableModel`, `ThreadsTreeModel`, `ThreadsTreeExpansionModel`,
`CallStackNodeModel`, `CallStackTableModel`, `CallStackTreeModel`, `HiddenCallStackFramesFilter`,
`LocalsTreeModel`, `VariablesNodeModel`, `VariablesTableModel`, `VariablesTableHTMLModel`,
`VariablesTreeModelFilter`, `VariablesTreeModelFilterSI`, `VariablesPropertyEditorsModel`,
`VariablesTreeExpansionModel`, `WatchesModel`, `WatchesNodeModelFilter`, `WatchesColumnModels`,
`WatchesSwitchViewAction`, `SessionsTableModelFilter`, `SourcesModel`, `SourcesView`,
`SourcesSessionProvider`, `BreakpointsNodeModel`, `BreakpointsTreeModelFilter`,
`BreakpointsHitCountModel`, `BreakpointsExpandableGroup`, `BreakpointModelActiveSessionFilter`,
`MonitorModel`, `JPDAAsynchronousModel`, `JPDAWatchRefreshModel`, `BoldVariablesTableModelFilter`,
`LabelVarsFilter`, `NumericDisplayFilter`, `JavaVariablesFilter`, `VariablesFormatterFilter`,
`FixedWatchesManager`.
Breakpoint UI: `LineBreakpointType`/`LineBreakpointPanel`, `MethodBreakpointType`/`MethodBreakpointPanel`,
`ExceptionBreakpointType`/`ExceptionBreakpointPanel`, `FieldBreakpointType`/`FieldBreakpointPanel`,
`ClassBreakpointType`/`ClassBreakpointPanel`, `ThreadBreakpointType`/`ThreadBreakpointPanel`,
`JPDABreakpointCustomizer`.
Actions: `DebuggingActionsProvider(+Filter)`, `CallStackActionsProvider`, `ThreadsActionsProvider`,
`VariablesActionsProvider`, `WatchesActionsProvider`, `BreakpointsActionsProvider`,
`JPDASessionActionsProvider`, `NewWatchActionProvider`, `EvaluateActionProvider`,
`RunToCursorActionProvider`, `RunIntoMethodActionProvider`, `MakeCallerCurrentActionProvider`,
`MakeCalleeCurrentActionProvider`, `ToggleMethodFieldBreakpointAction`, `SetAsCurrentThreadGutterAction`,
`PendingActionsFilter`, `ActionsPanel`.
Evaluate/values: `JPDACodeEvaluator`, `EvaluatorTreeModel`, `EvaluatorNodeModelFilter`,
`EvaluatorTableModelFilter`, `EvaluatorVarsTreeModel`, `ValuePropertyEditor`, `MultilinePanel`,
`BigStringCustomEditor`, `OutlineComboBox`, `VariableFormatterEditPanel`, `StorablePanel`.
Stepping/engine UI: `SmartSteppingImpl`, `ComputeInlineValues`, `InlineValueComputerImpl`,
`InlineValueProviderImpl`, `JPDAMethodChooserFactoryUIImpl`, `MethodChooserSupport`,
`MethodsCompletionProvider`, `ClassCompletionProvider`, `ElementCompletionItem`, `ClassSearchScopeType`,
`JavaUtils`.
Editor glue: `EditorContextBridge`, `CurrentThreadAnnotationListener`, `FileMoveBreakpointsHandler`,
`SetAsCurrentThreadGutterAction`, `ToolTipSwitchViewAction`, `JPDADVFrame/Thread/ThreadGroup`,
`JavaMethod/Field/ClassNbDebugEditorKit`.
Options: `JavaDebuggerOptionsPanelController`, `JavaDebuggerPanel`, `OptionsInitializer`,
`CategoryPanelGeneral`, `CategoryPanelFormatters`, `CategoryPanelStepFilters`, `CheckDeadlocksAction`,
`DisablingCellRenderer`, `ConnectPanel`, `JPDAAttachType`.

## 4. Decisions (locked)

1. **v1 scope = the eight steps in §7**; inline values, formatters beyond the basics, visual debugger,
   heap walk/profiler and hot swap are later.
2. **Reuse the headless core** (`api.debugger`, `api.debugger.jpda`, `debugger.jpda`,
   `debugger.jpda.projects`) and **rebuild the UI in JavaFX**; do not depend on `spi.debugger.ui`,
   `spi.viewmodel` or `debugger.jpda.ui` (all pull `openide.awt/nodes/windows/explorer`).
3. **Split the module**: `nbfx.debug.core` (headless model, `check-models`) + `nbfx.debug` (JavaFX UI).
4. **Launch via nbfx**: start the debuggee with `nbfx.execution` (doc 15, Terminal default) and attach
   with `JPDADebugger.attach(host, port, services)`; do **not** use `JPDADebugger.launch` (the engine's
   launcher pulls the legacy IO/execution stack).
5. **nbfx-native view models** (mirroring `spi.viewmodel`), not a dependency on the Swing module.
6. **Actions** via `ActionsManager` `ACTION_*` + nbfx `Command`s (no `javax.swing` imports).
7. **JUnit/Ant/Maven/Gradle debug** in the build-integration step; **attach** in the attach/monitors step;
   **remote** later.

## 5. Target architecture

### 5.1 Modules

```
nbfx.debug.core   (headless model, JavaFX-free, check-models)
  core/    DebuggerSession, DebuggerSessionState, DebugLaunchRequest, LaunchResolver,
           AttachRequest, DebuggerSessions, SourcePaths, JdwpAddress
  model/   DebugNode, DebugTreeModel, DebugTableModel, DebugColumn, DebugAsynchronousModel,
           DebugModelListener, DebugModels, filters
  impl/    ThreadsModel/Node, CallStackModel, VariablesModel/Node (lazy), WatchesModel,
           BreakpointsModel, SourcesModel, SessionsModel, MonitorsModel
  bp/      BreakpointManager, BreakpointDescriptor + Line/Method/Exception/Field/Class/Thread descriptors
  ctx/     DebugContext (current thread/frame), EditorContextSupport, SourceNavigator
  act/     DebugActions (ACTION_* -> Command), ActionEnablement, ActionLabels
  ann/     DebugAnnotations (breakpoint glyphs, current line), AnnotationSink SPI

nbfx.debug        (UI, JavaFX)
  ui/      DebugViewProvider, DebugOutlineView (TreeTableView renderer), cell/column renderers, ActionsPanel
  views/   DebuggingView, SessionsView, ThreadsView, VariablesView, WatchesView, BreakpointsView,
           SourcesView, ResultsView
  bp/ui/   per-type breakpoint customizers
  dialogs/ EvaluateDialog, AttachDialog, NewWatchDialog, VariableFormatterDialog
  editor/  DebugEditorAnnotations (EditorAnnotationProvider impl), source navigator wiring
  options/ JavaDebuggerOptionsPanel (Options system, doc 14)
```

### 5.2 View-model layer (nbfx-native, mirrors `spi.viewmodel`)

```java
interface DebugNode { String getDisplayName(); String getIconId(); }   // + tooltip/HTML value
interface DebugTreeModel<T> {                       // lazy, async
    List<T> getChildren(T parent);
    boolean isLeaf(T node);
    void addModelListener(DebugModelListener l);    // structural changes
}
interface DebugTableModel<T> {
    int getColumnCount(); String getColumnName(int i);
    Object getValueAt(T node, int i);               // display value / HTML
}
interface DebugColumn { String id(); String displayName(); int width(); boolean sortable(); }
interface DebugAsynchronousModel { void waitForLoading(); void cancelLoading(); }
```

Models are **pure data adapters** over the JPDA API (no FX types); events use a small
`DebugModelListener`; the UI wraps them in `ObservableList`/`TreeItem` adapters on the FX thread. Lazy
children load off-thread with cancellation when a node collapses or the session ends.

### 5.3 Session lifecycle and refresh semantics

- States: `STARTING → RUNNING ⇄ STOPPED → DISCONNECTED` (from `JPDADebugger.STATE_*`), plus `TERMINATED`
  for the nbfx session wrapper (process exit). Suspend reason (breakpoint/step/exception/interrupt) is
  derived from the current frame/last event for the status text.
- On stop: refresh threads (from the root thread group), current thread, call stack, current frame,
  variables, and the editor highlight.
- While running: no variable/call-stack refresh; actions disabled as appropriate; the thread list follows
  `PROP_THREAD_STARTED`/`PROP_THREAD_DIED`.
- Event coalescing: batch property-change storms into one FX update (≈50 ms debounce); JDI calls on a
  dedicated debugger executor, never the FX thread.
- Session end: cancel model loads, dispose listeners, clear the highlight, mark the status inactive, keep
  the log in the Output/Terminal.

### 5.4 Launch / attach

- **Launch** (Debug File/Project/Test): build the command (run configuration, or `LaunchResolver`'s file
  resolution), append
  `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:0` (or a pre-chosen free port),
  start it with `nbfx.execution` in **Terminal** (default) or Output mode, then discover the address:
  - `address=127.0.0.1:0` makes the JVM print `Listening on 127.0.0.1:<port>` (wording varies by
    version/transport; the current code already parses it) — robust against collisions;
  - fallback: choose a free port and use `address=127.0.0.1:<port>`.
- **Attach**: `JPDADebugger.attach(host, port, services)` with
  `services = { Map.of("name", sessionName, "sourcepath", ClassPath, "jdksources", ClassPath, "baseDir", File) }`
  (the documented upstream contract; nbfx wraps it behind **typed** session parameters/services and
  converts to the map only at the engine boundary — no stringly-typed API, doc 30 §4)
- Suspend policy: `suspend=y` initially (catch early breakpoints/exceptions), configurable.
- Stop: `ACTION_KILL` + destroy the process tree; restart: stop + relaunch.

### 5.5 Breakpoints

- `BreakpointManager` wraps `DebuggerManager` (`addBreakpoint`/`removeBreakpoint`/`getBreakpoints`,
  listeners) with per-type descriptors.
- **Types and fields** (API `PROP_*`):
  - Line: file+line, condition, hit count, suspend policy, group, thread filter, class filter, instance
    filters, print text, enabled, hidden, stratum.
  - Method: class + method name/signature, condition, hit count, suspend, print text.
  - Exception: class (or subclass), caught/uncaught, condition, suspend, print text.
  - Field: class + field, access/modification, condition, hit count, print text.
  - Class load/unload: class filter, exclusion, break on load/unload.
  - Thread: thread start/death.
- **Persistence**: the engine stores breakpoints (its `PersistenceManager`); nbfx persists only view state
  and the "breakpoints active" toggle (`JPDADebugger.setBreakpointsActive`) via `nbfx.settings`.
- **Editor**: annotations for enabled/disabled/conditional/invalid breakpoints, gutter toggle, caret-line
  toggle action, drag-to-move later.

### 5.6 Actions

| `ACTION_*` | nbfx command | Notes |
|---|---|---|
| `ACTION_START` | (re)start session | Debug Project/File/Test entry points |
| `ACTION_CONTINUE` | Continue | |
| `ACTION_PAUSE` | Pause | disabled unless running |
| `ACTION_STEP_OVER` / `INTO` / `OUT` | Step Over/Into/Out | |
| `ACTION_STEP_OPERATION` | Step Operation | |
| `ACTION_RUN_INTO_METHOD` | Run Into Method | engine's method chooser |
| `ACTION_RUN_TO_CURSOR` | Run to Cursor | from the editor caret |
| `ACTION_POP_TOPMOST_CALL` | Pop Topmost Call | `canPopFrames` |
| `ACTION_MAKE_CALLER_CURRENT` / `ACTION_MAKE_CALLEE_CURRENT` | Make Caller/Callee Current | call-stack context menu |
| `ACTION_NEW_WATCH` | New Watch | from selection |
| `ACTION_EVALUATE` | Evaluate | evaluate window |
| `ACTION_FIX` | Apply Code Changes | hot swap (later) |
| `ACTION_RESTART` | Restart | |
| `ACTION_KILL` | Finish | stop the session |
| `ACTION_TOGGLE_BREAKPOINT` | Toggle Breakpoint | editor/gutter |

Action providers are registered per context (Debugging/Threads/CallStack/Variables/Watches/Breakpoints) so
enablement follows the selection.

### 5.7 Editor integration

- **Annotation SPI** (existing `nbfx.api.editor.EditorAnnotationProvider` + `EditorAnnotation` +
  `MarkLineNumberDecorator`): add debug kinds — `breakpoint` (enabled/disabled/conditional/invalid),
  `currentPC` (current line, arrow + highlight), `callStack` (other frames), and tooltips.
- **Toggle**: gutter click toggles a `LineBreakpoint`; the annotation follows the text (markers) like the
  existing override badges.
- **Navigation**: call-stack frame → open the source (`OpenSources`) and position the caret; `EditorContext`
  round-trip (caret → frame) via `DebugContext`/`ContextProvider`.
- **Source resolution**: `SourcePaths` builds SOURCE classpaths (+ JDK sources) per project type, needed for
  stepping into libraries.

## 6. UI: views and their detail

| View | Content | Models | Actions | Refresh |
|---|---|---|---|---|
| **Debugging** | toolbar + threads banner + call stack (top) + variables (bottom) | `Debugging*` models | all stepping/evaluate/stop/restart | on stop, thread/frame change |
| **Sessions** | one row per session (name, state, location) | `SessionsModel` | activate/finish/restart | session events |
| **Threads** | thread-group tree, state, current marker | `ThreadsModel` (+ lazy groups) | make current, suspend/resume | thread events, on stop |
| **Call Stack** | frames (method, class, line, location) | `CallStackModel` | pop, make caller/callee current, open source | on stop/thread change |
| **Variables / Locals** | lazy tree: this, arguments, locals, fields, statics, arrays, map entries | `VariablesModel` (+ formatters/filters) | new watch, evaluate, format, open/toString, copy | on frame change |
| **Watches** | fixed + selection watches, expression, value, type | `WatchesModel` | new/delete/reorder/refresh, pin | on stop, manual refresh |
| **Breakpoints** | grouped, enabled checkbox, hit count, condition | `BreakpointsModel` | enable/disable, customizer, delete, go-to | breakpoint events |
| **Sources** | source roots for the session | `SourcesModel` | open, add/remove | session start |
| **Results** | evaluation/test results (later) | `ResultsModel` | — | — |

Shared FX renderer: a `TreeTableView` with name/value columns (`DebugOutlineView`) + per-view column sets,
context menus from the action providers, lazy expansion, theme-aware styling.

## 7. Steps (detailed tasks)

### Step 1 — `feature/debug-foundation`
- Create `nbfx.debug.core` (`check-models`): `DebugLaunchRequest`, `LaunchResolver` (port `DebugLauncher`),
  `DebuggerSession` (states, `JPDADebugger.attach`, services map, listeners, executor), `DebuggerSessions`
  registry, `JdwpAddress` (JDWP args + address discovery), `DebugActions` mapping + enablement.
- Rework `nbfx.debug` UI: `DebugOutlineView` over the view-model layer; migrate `DebugView` to the new
  models; `DebugViewProvider` with the Debugging view.
- Move `ToggleBreakpointCommand` to core + editor annotation for toggle.
- Start the debuggee via `nbfx.execution` (Terminal default).
- **Acceptance**: Debug File starts a JVM in the Terminal, attaches, hits a caret breakpoint, shows
  threads/call stack/variables, steps, stops; `nbfx.debug.core` has no JavaFX.

### Step 2 — `feature/debug-views`
- Implement the view-model interfaces + models: threads (groups, lazy), call stack, variables (lazy:
  this/args/locals/fields/statics/arrays), watches, sources, sessions, results.
- FX views + columns + context menus + lazy expansion + copy; view state persisted.
- **Acceptance**: all views render and update on stop/thread/frame change; watches add/remove; values
  expand lazily; no FX-thread blocking.

### Step 3 — `feature/debug-breakpoints`
- `BreakpointManager` + descriptors for all six types; persistence; "breakpoints active".
- FX customizers per type with the fields from §5.5; validation.
- Breakpoints view (grouping, hit counts, enable/disable, go-to) + editor annotations
  (enabled/disabled/conditional/invalid) + gutter toggle.
- **Acceptance**: create/disable/delete each type from the editor and the view; conditions and hit counts
  work; breakpoints survive restart.

### Step 4 — `feature/debug-stepping-editor`
- Wire all stepping actions (§5.6), smart stepping (`SmartSteppingFilter`), run-to-cursor, pop-to-here,
  make caller/callee current; per-context action providers.
- Current-line/current-frame highlight; source navigation; `DebugContext` ↔ editor caret.
- **Acceptance**: stepping follows source; the highlight moves; pop/run-to-cursor work; the call-stack
  context menu behaves as in NetBeans.

### Step 5 — `feature/debug-evaluate`
- Evaluate window (`JPDADebugger.evaluate`, completion), watches from selection, variable formatters
  (settings-based), value editors (string/number/boolean/char/array/object `toString`), multiline/big
  strings, numeric display filter, value tooltips.
- **Acceptance**: evaluating an expression returns a value; editing a primitive/string writes back;
  formatters apply.

### Step 6 — `feature/debug-attach-monitors`
- Attach dialog (host/port) + "Debugging by socket"; monitors view + deadlock detection + "Check
  Deadlocks"; session restart; suspend/resume.
- **Acceptance**: attach to a running JVM; monitors list; deadlock check reports.

### Step 7 — `feature/debug-build-integration`
- Debug Project/Test via run configurations; Ant/Maven/Gradle debug goals through `nbfx.execution`
  (doc 15); JUnit debug; stop/relaunch; debuggee output in Terminal/Output.
- **Acceptance**: "Debug Project"/"Debug Test" launch and break for Ant, Maven and Gradle projects; stop
  kills the process tree.

### Step 8 — `feature/debug-options`
- Java Debugger options panel (transport/launcher defaults, step filters, formatters) via the Options
  system (doc 14).
- **Acceptance**: the panel persists and affects new sessions.

### Later
`feature/debug-remote` (SSH/remote sessions), `feature/debug-inplace-fixes` (hot swap),
`feature/debug-visual`, heap walk/profiler.

## 8. Tests

- **Unit (headless)**: launch-request resolution, JDWP argument/address, services map, session state
  machine, model adapters over fake JPDA objects, lazy children, action enablement, breakpoint descriptor
  round-trip, source-path assembly.
- **Integration (headless)**: start a small test JVM with JDWP and exercise `DebuggerSession` end to end
  (attach, breakpoint hit, step, read variables) without UI.
- **UI (headless glass)**: `DebugOutlineView` rendering/lazy expansion/context menus, breakpoint
  customizer validation, editor annotations (markers follow edits).
- **Manual matrix**: light/dark; big data structures; multi-thread stepping; conditional/hit-count
  breakpoints; exception breakpoints (caught/uncaught); pop-to-here; attach; debug a JUnit test for
  Ant/Maven/Gradle; restart/stop; disconnection.
- **Purity**: `nbfx.debug.core` JavaFX-free; no `javax.swing`/`java.awt` imports in nbfx sources;
  guardrails green.

## 9. Risks and open questions

- **Scope**: ~100 Swing classes; port view-by-view, keep each step reviewable.
- **Engine Swing/AWT touchpoints** (`VariableMirrorTranslator` `java.awt.Font`, breakpoint impls'
  `javax.swing.text.Document`, commented asserts): module-level only; confirm nothing forces AWT at
  runtime in the nbfx headless runtime.
- **`openide.text` + `projectapi` deps of the engine** (headless): confirm they stay in the nbfx runtime.
- **`api.io` console**: doc 15's Output window must be the `InputOutputProvider` for the debugger console.
- **Address discovery**: JDK wording varies; keep the free-port fallback and a clear error.
- **Action objects**: engine providers create `javax.swing.Action`s; we rely on `ActionsManager` constants
  + `isEnabled` and supply our own labels/icons (fallback: reflection).
- **Source navigation** needs SOURCE classpaths + JDK sources for Maven/Gradle/Ant (`Java Platforms`,
  doc 02).
- **Editor annotation SPI** may need extending (lines/columns, tooltips, click actions).
- **Deadlock detection/monitors** and **inline values** are JPDA-heavy; dedicated steps.

## 10. Reference appendix (paths)

- nbfx (today): `nbfx-ide/c.gluonhq.netbeans.nbfx.debug/src/...`; editor annotations in
  `nbfx-ide/...nbfx.editor/.../codearea/AnnotationSupport.java`,
  `nbfx-platform/...nbfx.api/.../editor/EditorAnnotationProvider.java`.
- Headless core to reuse: `ide/api.debugger/src/org/netbeans/{api,spi}/debugger/*`,
  `java/api.debugger.jpda/src/org/netbeans/api/debugger/jpda/*`,
  `java/debugger.jpda/src/org/netbeans/modules/debugger/jpda/{,actions,models,breakpoints,expr,jdi,console,remote}`,
  `java/debugger.jpda.projects/src/org/netbeans/modules/debugger/jpda/projects/*`.
- Swing UI to port: `ide/spi.debugger.ui/src/org/netbeans/spi/debugger/ui/*`,
  `ide/spi.viewmodel/src/org/netbeans/spi/viewmodel/*`,
  `java/debugger.jpda.ui/src/org/netbeans/modules/debugger/jpda/ui/*`,
  `java/debugger.jpda.projectsui/src/*`, `java/debugger.jpda.ant/src/*`, `java/ant.debugger/src/*`.
- Wiring evidence: `java/debugger.jpda*/src/META-INF/debugger/netbeans-JPDASession/*`,
  `ActionsManager.java` (constants), `JPDADebugger.java` (`attach` services contract),
  `JPDABreakpoint.java`/`LineBreakpoint.java` (breakpoint properties).
