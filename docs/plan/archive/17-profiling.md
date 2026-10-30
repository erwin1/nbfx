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
# 17 — Profiling support

Steps: **Profiler foundation** (`feature/profiler-foundation`), **Open recordings** (`.jfr`/`.hprof`/`.nps`,
`feature/profiler-recordings`), **Streaming JFR** (`feature/profiler-streaming`), **async-profiler backend**
(`feature/profiler-async-profiler`), **CPU profiling** (`feature/profiler-cpu`), **Memory profiling**
(`feature/profiler-memory`), **Locks & threads** (`feature/profiler-locks-threads`), **Attach & live
sampler** (`feature/profiler-attach-sampler`), **Heap walker** (`feature/profiler-heap`), **Profiler
options** (`feature/profiler-options`); later: **snapshot tracer**, **OQL**, **profiling points**,
**snapshot diff UI**, and an optional **legacy agent backend**.

Decisions in §9 are locked (see §4).

## 1. Goal and scope

Give nbfx **profiling**: CPU (sampling + call tree/flat/flame graph), memory (allocations, live set, GC),
locks/threads, telemetry over time, **heap dumps + heap walker**, attach to a running JVM, snapshot
save/load/compare, and profile actions for the project types (Ant/Maven/Gradle), all pure JavaFX.

Two sides:

- **IDE side** — the profiler window/views, analysis model, snapshot storage, options, integrations.
- **Target side ("the other side")** — what must exist in/around the profiled JVM: the recording engine
  (JFR built into the JDK, and/or a native agent such as async-profiler), how it is started (JVM flags vs
  attach), the heap-dump mechanism, and the JDK modules the IDE needs (`jdk.jfr`, `jdk.management.jfr`,
  `jdk.attach`).

## 2. What nbfx has today

**Nothing.** No profiler module, no `profiler` cluster, no profile actions. The build action set already
names a `profile` command (doc
[02](02-part1-infrastructure.md): for Maven the build action list plus **Profile** for Gradle, and the
per-type action tables), and doc [16](16-debugging.md) notes the debugger's heap walk as a later step,
but there is no engine or UI.

## 3. Research: the NetBeans profiler

### 3.1 Module map (`netbeans/profiler/*`, cluster `nb.cluster.profiler`)

| Module | Files / Swing | Role |
|---|---|---|
| `lib.profiler` (`org.netbeans.lib.profiler`) | 337 / 5 | the **engine + agent + heap parser** (JFluid-derived); **module dependencies: none** |
| `lib.profiler.common` | 12 / 2 | shared engine helpers |
| `lib.profiler.charts` | 55 / 21 | Swing chart toolkit |
| `lib.profiler.ui` | 225 / 194 | the results UI (CPU/memory/locks/threads/monitor/JDBC, charts/graphs/components) |
| `profiler` | 85 / 71 | IDE integration: actions, control panel, results/snapshots windows, features |
| `profiler.api` | 40 / 7 | IDE API: `ProjectProfilingSupport(Provider)`, `ProfilerProject`, `JavaPlatform(Manager)Provider`, `GoToSource`, `EditorSupport`, `ProfilerDialogs`, `ProfilerIDESettings`, `ProfilerStorage`, `ActionsSupport`, icons |
| `profiler.nbimpl` | 35 / 7 | IDE glue (settings, dialogs, results manager) |
| `profiler.options` | 8 | Options: `ProfilerOptionsCategory/Container/Panel`, `General/Engine/Snapshots/FiltersOptionsPanel` |
| `profiler.j2se`, `profiler.freeform`, `profiler.nbmodule`, `maven.profiler`, `profiler.projectsupport` | 3+4+2+2+4 | per-project-type **Profile Project** support (`*ProjectProfilingSupportProvider`, `*AntProjectSupportProvider`) |
| `profiler.attach` | 7 | attach wizard + remote-pack exporter (`TargetPlatformEnum`) |
| `profiler.heapwalker` | 125 / 76 | heap walker UI (classes/instances/fields/references, dominators, retained size, memory lint, OQL console) |
| `profiler.snaptracer` | 103 / 78 | snapshot tracer (timeline probes/telemetry) |
| `profiler.oql`, `profiler.oql.language` | 15 / 11 | OQL engine + language over heap dumps |
| `profiler.ppoints` | 57 / 42 | profiling points (snapshot/heapdump/start/stop/stopwatch triggers at code locations) |
| `profiler.utilities` | 5 | misc |
| `debugger.jpda.heapwalk` | 17 / 6 | heap walk from the debugger |

### 3.2 The engine / agent (`lib.profiler`)

- **Client**: `ProfilerClient` (connect/attach, commands, live results), `ProfilerClientListener`,
  `ProfilerEngineSettings`, `TargetAppRunner` (how the target JVM is launched), `ProfilerLogger`,
  `ProfilingEventListener`; `client/` helpers (`AppStatusHandler`, `MonitoredData`,
  `ProfilingPointsProcessor`).
- **Server (agent)**: `ProfilerServer` (`main`, `premain` via `ProfilerActivate15`, `System.load` of the
  native lib), `ProfilerInterface` (attach/load), `ProfilerRuntimeCPU{FullInstr,SampledInstr,CodeRegion}`,
  `ProfilerRuntimeMemory`/`ObjAlloc`/`ObjLiveness`/`Sampler`, `ClassLoaderManager` (JVMTI
  `ClassFileLoadHook` instrumentation), `SamplingThread`, `Stacks`, `Threads`/`ThreadInfo`, `Monitors`,
  `GC`, `HeapHistogramManager`, profiling-point handlers.
- **Wire protocol**: `wireprotocol/*` (`Command`, `InitiateProfilingCommand`,
  `InstrumentMethodGroupCommand`, `TakeHeapDumpCommand`, `ThreadDumpResponse`, `VMPropertiesResponse`,
  `CalibrationDataResponse`, `WireIO`, …).
- **Results/snapshots**: `results/*` — `ResultsSnapshot`, `CPUResultsSnapshot`,
  `Memory/Alloc/Liveness/SampledMemoryResultsSnapshot`, `CodeRegionResultsSnapshot`, `JdbcResultsSnapshot`,
  diffs, the CCT node hierarchy (`CPUCCTNode`, `ThreadCPUCCTNode`, `MethodCPUCCTNode`, `Alloc*`, `Lock*`),
  `FlatProfileContainer`, `HeapHistogram`, `ThreadDump`, `VMTelemetryDataManager`,
  `DataFrameProcessor`s, `ExportDataDumper`.
- **Heap parser** (`heap/`, ~90 classes, headless): `Heap`/`HprofHeap`/`HeapFactory`, `JavaClass`,
  `Instance`/`InstanceDump`/`ClassDump`, `Field`/`FieldValue`/`ObjectFieldValue`, `GCRoot`/`HprofGCRoots`,
  `DominatorTree`/`DomMap`, `NearestGCRoot`, `ComputedSummary`/`HeapSummary`, arrays (lazy lists),
  `StringSegment`, `StackTrace`.
- **Instrumentation/classfile**: `instrumentation/*` (`ClassRewriter`, `Injector`, `MethodEntryExit…`,
  `ObjLiveness…`, `CodeRegion…`, `RootMethods`, …), `classfile/*` (parser, `ClassRepository`,
  `ClassLoaderTable`), `filters/*`, `marker/*`, `jps/` (`JpsProxy`/`RunningVM` — attach via
  `sun.jvmstat`), `utils/*`, `global/` (`CommonConstants` — agent states/JDK constants, `Platform`,
  `ProfilingSessionStatus`, `CalibrationDataFileIO`).

**Target-side facts (what the agent needs):**

- Agent files shipped: `jfluid.server.jar`, `jfluid.server.15.jar`, `jfluid.server.cvm.jar`; native libs
  (`nbm.executable.files`: `**/*.so`, hpux `.sl`, `remote-pack-defs/*.sh`).
- Launch: `TargetAppRunner` adds `-agentpath:<native lib>` + `-Xbootclasspath/a:<jfluid-server jars>` +
  classpath (JDK-8 style). Remote packs per platform (`profile-linuxamd64-16.sh`, `-mac-16.sh`,
  `-winamd64-16.bat`, Solaris/HP-UX/CVM…).
- Native sources: `native/src-jdk15/{attach,Classes,HeapDump,Stacks,Threads,Timers,GC,class_file_cache,
  config,common_functions}.c` + build scripts `buildnative-{linux,linux64,mac,windows64-16}.{sh,bat}`.
- **Hard limits:** `instrumentation/JavaClassConstants.JAVA_MAJOR_VERSION = 55` → the class-file parser
  **rejects class files newer than JDK 11** ("Unsupported class file version"); `javac.source=1.8`; attach
  uses `sun.jvmstat` + `tools.jar` (JDK 8-era, `jdk.internal.jvmstat` on 9+); agent states only model
  `JDK_110_BEYOND`.
- **No JFR anywhere in NetBeans** (grep: no `jdk.jfr`/`FlightRecorder` usage) — the profiler is the custom
  agent only.

### 3.3 What that means for nbfx (target = Java > 25)

The NetBeans profiler **agent** cannot be the primary engine on nbfx's target JDK: the class-file version
cap (55 = JDK 11) blocks instrumenting modern classes, the attach path uses removed JDK internals, and the
natives would need rebuilding and re-validating for JDK 25. Modern JDKs also make the agent approach
largely unnecessary: **JFR is built in**, version-matched, and agent-free.

**Decision: agent-free modern backends first.**

- **JFR** — the recording engine (`-XX:StartFlightRecording=…` at launch, or start/stop/dump on a running
  JVM via attach + `jdk.management.jfr.FlightRecorderMXBean`), parsed with **`jdk.jfr.consumer`**. Gives
  `ExecutionSample` (CPU), `ObjectAllocationSample`/`ObjectAllocationInNewTLAB` (allocation),
  `NativeMethodSample`, `JavaMonitorEnter`/`ThreadPark` (locks), `ThreadStart/End`, `GC*`/`G1*`,
  `JVMInformation`/`CPULoad`/`Heap*` (telemetry), `ExceptionThrow`, `Socket/File IO`, `JDBC` (later).
- **async-profiler** (Apache-2.0) — high-fidelity sampling backend: `-agentpath:libasyncProfiler=…`,
  `-e cpu|wall|alloc|lock`, `-o jfr` output → the **same parser**. Ships native libs per OS/arch (§3.5).
- **JMX live sampler** — `ThreadMXBean.dumpAllThreads`/`getThreadCpuTime` for a lightweight, no-recording
  "Sampler" (thread states, hot stacks, live thread dump).
- **Heap** — dump with `HotSpotDiagnosticMXBean.dumpHeap(path, live)` (JDK, no agent) and parse with the
  **reusable headless `org.netbeans.lib.profiler.heap`** (or an extraction into `nbfx.profiler.heap`).
- **Legacy agent** — kept as an optional backend behind an SPI only if someone raises the class-file cap
  and rebuilds the natives; default off, documented as high-risk/out of scope.

**Target-side requirements ("the other side"):**

- JFR: nothing to install (JDK 11+). IDE needs `--add-modules jdk.jfr,jdk.management.jfr`.
- Attach/JMX control and heap dumps: `--add-modules jdk.attach,jdk.management` (local attach; optionally
  `-Dcom.sun.management.jmxremote` for remote).
- async-profiler: the bundled `libasyncProfiler.{so,dylib}` (per OS/arch); `-agentpath:…=start,event=…,
  file=*.jfr`.
- Heap: no agent; optional `-XX:+HeapDumpOnOutOfMemoryError`.
- Legacy NetBeans agent (optional/later): `jfluid-server*.jar` + native lib + `-agentpath` +
  `-Xbootclasspath/a`; blocked by the JDK-11 class-file cap.

### 3.4 Reuse matrix

| Piece | Verdict |
|---|---|
| `lib.profiler.heap` (HPROF parser/analysis) | **reuse** (headless, zero module deps); consider extracting to `nbfx.profiler.heap` |
| `lib.profiler` client/server/wireprotocol/instrumentation/classfile/jps | **not usable** on JDK 25 (cap 55, jvmstat/attach, natives) → optional legacy backend after patching |
| `lib.profiler.results` (snapshots/CCT) | **partly reusable**: `.nps` **reader** (import) + separable CCT/flat/diff algorithms; the agent wire path is not. Writing `.nps` is **not** promised — it is a private format (doc 32) |
| `lib.profiler.ui`, `lib.profiler.charts` | **replace** with FX views + a small FX chart/flame-graph toolkit |
| `profiler`, `profiler.heapwalker`, `profiler.snaptracer`, `profiler.ppoints`, `profiler.oql*` | **port** the UI/feature set to FX (parity map below), features phased |
| `profiler.options` | port into the Options system (doc 14), with JFR/backend settings instead of the agent's |
| `profiler.api`, `profiler.projectsupport`/`j2se`/`freeform`/`nbmodule`/`maven.profiler`, `profiler.attach` | design reference for Profile Project/attach; reimplement over run configs + `nbfx.execution` (doc 15) |
| `debugger.jpda.heapwalk` | later: heap walk from the debugger, reusing the nbfx heap model |

### 3.5 async-profiler backend — what a full integration needs

async-profiler (v4.5, **Apache-2.0**; officially maintained for **Linux x64/arm64** and **macOS
x64/arm64** — **no Windows** build) is the best-fidelity agent: CPU/wall/alloc/lock sampling without
safepoint bias, native frames, allocation and lock profiling, hardware/software perf counters, and
**JFR output** (`-o jfr`) readable by `jdk.jfr.consumer`. It is a first-class backend (not a
nice-to-have): it removes JFR's sampling caveats.

Concrete work items:

1. **Bundle the binaries.** Native libs are `libasyncProfiler.so` (Linux) / `libasyncProfiler.dylib`
   (macOS), per arch (x64/arm64). They cannot be loaded from inside a jar, so ship them per OS/arch (see
   §3.6) under `release/modules/lib/asyncprofiler/…` with the Apache-2.0 license, and **extract/copy to a
   version-keyed cache dir** at first use (stream copy avoids macOS xattrs/quarantine; read-only installs
   are fine); keep the lib path overridable in settings.
2. **Lib selection + validation.** Pick by `os.name`/`os.arch` (via the OS/arch module, §3.6); verify the
   file loads (`Agent_OnAttach` handshake/probe); surface a clear "async-profiler not available on this
   OS/arch — falling back to JFR" state and disable the option.
3. **Launch mode.** Append the JVMTI agent argument to the run command (`nbfx.execution`, doc 15):
   `-agentpath:<lib>=start,event=<cpu|wall|alloc|lock|itimer|ctimer>,interval=10ms,file=<session>.jfr`,
   plus `loop`/`jfrsync` for long/continuous sessions, and `%p`-style placeholders. Works on JDK 25 with
   **no extra flags**.
4. **Dynamic attach mode.** Use `jdk.attach` (`VirtualMachine.attach(pid)` + `loadAgentLibrary(lib,
   options)`, or ship the `asprof` launcher) to start/stop/dump on a running JVM. **JDK 21+ caveat**:
   dynamic agent loading requires `-XX:+EnableDynamicAgentLoading` in the target — detect it, offer to
   relaunch with the flag, or use launch mode. Attach requires the same user; JDK 25 is fine.
5. **Recording control.** Launch mode writes the `.jfr` (`file=`) and flushes at exit; manual stop/dump
   becomes attach-style `stop`/`dump`, or in-process control via the `tools.profiler:async-profiler` Java
   API if we ever run inside the target. **JFR is the only storage format** so the analyzer is shared with
   the JFR backend.
6. **Parsing.** Read async-profiler `.jfr` with `jdk.jfr.consumer` **name-based** (standard
   execution-sample events plus async-profiler events such as `profiler.Span`; native frames,
   allocation/lock events). Validate with fixtures recorded by both JFR and async-profiler and assert the
   same model output where data overlaps. `jfr-converter.jar` exists if a lossy format ever needs
   normalising.
7. **Options** (doc 14): backend (JFR / async-profiler), event mode (cpu/wall/alloc/lock/native-mem/perf
   counters), interval, continuous/`loop`, lib path override, and the JFR settings file for the JFR
   backend.
8. **Permissions/limits.** macOS uses `AsyncGetCallTrace` (no `perf` permission needed); Linux
   `perf_event_paranoid`/`perf` permissions matter only for perf-counter/`ctimer` modes;
   containers/seccomp can block `perf_event_open`; extracted dylibs are fine outside the app bundle (the
   *target JVM* loads them; a plain JDK isn't hardened) — note hardened-runtime/notarised builds (§ below).
9. **Licensing/attribution.** Apache-2.0 (verified) → redistributable; include the LICENSE/attribution
   and list it in `licenseinfo` (the repo has no NOTICE file; the static `libstdc++` link is covered by
   the GCC runtime exception; the trademark clause just means "describe it, don't imply endorsement").
10. **Fallback + tests.** Windows/unsupported arch/missing lib → JFR only; unit tests for arg building,
    lib selection/extraction and parsing; integration tests on macOS and Linux (launch and attach); a CI
    matrix per OS.

**Bundling mechanics.** nbfx already ships platform natives the same way in
`nbfx-platform/c.gluonhq.netbeans.nbfx.lib.javafx/release/modules/lib/` (e.g. `libprism_mtl.dylib`,
`libjavafx_iio.dylib`, `amd64/*.dll`, jars under `release/modules/ext/`), with no `external/binaries-list`
of its own — the precedent is to **vendor the binaries in `release/`**. Proposed layout:

```
c.gluonhq.netbeans.nbfx.libs.asyncprofiler.linux-x64/   release/modules/lib/asyncprofiler/libasyncProfiler.so
c.gluonhq.netbeans.nbfx.libs.asyncprofiler.linux-aarch64/…
c.gluonhq.netbeans.nbfx.libs.asyncprofiler.macos-x64/   …/libasyncProfiler.dylib
c.gluonhq.netbeans.nbfx.libs.asyncprofiler.macos-aarch64/…
+ license file per module (Apache-2.0)
```

Alternative for quarantine/notarisation-safety: ship the libs **inside a jar** under
`release/modules/ext/` and **extract via `getResourceAsStream`** to a version-keyed user cache (stream
copy creates a fresh file without macOS xattrs) — recommended if nbfx ships a notarised `.app`, since
unsigned Mach-O files inside the bundle can trip notarisation; re-signing the dylibs is the other option.
Size: ≈1–2 MB per lib. Pin **4.5** and record SHA-256 for reproducibility.

### 3.6 OS/arch-based source-module enablement (recorded)

The platform already supports per-OS/arch **module enablement**, so the async-profiler natives ship in
OS/arch-specific modules and only the matching one is enabled (and, for per-platform distributions, only
that one is built into the cluster):

- `OpenIDE-Module-Requires: org.openide.modules.os.MacOSX` / `.Linux` / `.Windows` / `.Unix` /
  `.PlainUnix` / `.Solaris` (exact tokens present in the tree; the macOS one is `MacOSX`, **not** `Mac`).
- `org.openide.modules.arch.x86_64` / `amd64` / `aarch64` / `arm` / `x86` / … (present in the tree).
- The nbfx harness can additionally filter modules per platform when building a distribution
  (`cluster/*.properties.fragment` + `-prune-excluded`).

Proposed module layout (only the current OS/arch enables):

```
c.gluonhq.netbeans.nbfx.libs.asyncprofiler.linux-x64      Requires: os.Linux  + arch.x86_64
c.gluonhq.netbeans.nbfx.libs.asyncprofiler.linux-aarch64  Requires: os.Linux  + arch.aarch64
c.gluonhq.netbeans.nbfx.libs.asyncprofiler.macos-x64      Requires: os.MacOSX + arch.x86_64
c.gluonhq.netbeans.nbfx.libs.asyncprofiler.macos-aarch64  Requires: os.MacOSX + arch.aarch64
```

each carrying `release/modules/lib/asyncprofiler/libasyncProfiler.{so,dylib}` + its license, and a tiny
service exposing the lib path (so `nbfx.profiler.core` looks the service up; a missing service ⇒ JFR
fallback). Windows simply has no backend module ⇒ JFR, the same as an unsupported arch. (The same
mechanism covers future native backends.)

## 4. Target architecture

Decisions locked: agent-free backends first (JFR default, async-profiler where a module exists, JMX
sampler); NetBeans agent out (optional legacy step); `nbfx.profiler.core` + `nbfx.profiler.ui` split;
heap parser reused; `.jfr`/`.hprof` (+ optional nbfx snapshot) formats; one analysis model fed by many
sources.

### 4.1 Modules

```
nbfx.profiler.core   (model, headless, JavaFX-free, check-models)
  session/   ProfilerSession, ProfilerSessionState, ProfileRequest (launch/attach),
             ProfilerBackends (registry), SnapshotStore (.jfr/.hprof/native)
  backend/   ProfilerBackend SPI; JfrBackend, AsyncProfilerBackend, JmxSamplerBackend,
             (optional LegacyAgentBackend)
  rec/       RecordingControl (start/stop/dump/save/load), Attach (jdk.attach + FlightRecorderMXBean),
             StreamingAggregator (incremental, bounded)
  model/     StackTree/CCT (call tree), FlatProfile, FlameGraph, AllocationProfile, LiveSet, LockProfile,
             ThreadStates, Telemetry, EventKind, Frame, MethodInfo, SourceResolver
  heap/      HeapDump (HotSpotDiagnosticMXBean) + heap model (reuse org.netbeans.lib.profiler.heap)
  compat/    NpsReader/Writer (legacy snapshot), HprofLoader
  diff/      SnapshotDiff (CPU/allocation comparisons)
  act/       ProfileActions (Profile Project/File/Test), enablement
nbfx.profiler.ui     (UI, JavaFX)
  ui/        ProfilerViewProvider (window), ProfilerOutline (TreeTableView), FlameGraphCanvas,
             TimelineCanvas, ChartPane, ActionsPanel
  views/     SnapshotResults, CpuView, MemoryView, LocksView, ThreadsView, TelemetryView,
             SamplerView, HeapWalkerView, SnapshotsView (list/save/load/compare)
  dialogs/   AttachDialog, ProfileSettingsDialog, SnapshotInfoDialog, CompareDialog
  options/   ProfilerOptionsPanel (Options system, doc 14)
```

`nbfx.profiler.core` must be JavaFX-free (`check-models`); it uses `jdk.jfr`/`jdk.management.jfr`/
`jdk.attach`/`jdk.management` (JDK modules) and the reused heap parser.

### 4.2 Backend SPI (the "other side" seam)

```java
interface ProfilerBackend {
    String id();                        // "jfr" | "async-profiler" | "jmx-sampler" | "legacy-agent"
    boolean supports(RecordingMode mode);      // CPU | WALL | ALLOC | LOCK | LIVE | HEAP
    void applyLaunchArgs(List<String> cmd, ProfileRequest req);      // -XX:StartFlightRecording / -agentpath
    RecordingHandle start(ProfileTarget target, ProfileRequest req); // launch or attach
    byte[] dump(RecordingHandle handle);        // .jfr / hprof
    void stop(RecordingHandle handle);
}
```

- `JfrBackend` — launch flags (`-XX:StartFlightRecording=filename=…,settings=profile,dumponexit=true`) and
  attach path (`jdk.attach` → `FlightRecorderMXBean.newRecording/startRecording/stopRecording/copyTo`).
- `AsyncProfilerBackend` — `-agentpath:…start,event=…,file=…jfr`; requires the native lib (from the OS/arch
  module or a settings override); output in JFR for a single parser.
- `JmxSamplerBackend` — no file: periodic `dumpAllThreads` (~10–50 ms), aggregate stacks/thread states;
  also served live in the Sampler view.
- `LegacyAgentBackend` — optional/deprecated; blocked by the JDK-11 class-file cap.

### 4.3 Analysis model (from JFR events)

- **Stack tree / CCT**: aggregate `jdk.ExecutionSample` (CPU) or `jdk.ObjectAllocationSample` (alloc) by
  thread → frame stack → method; nodes carry sample counts/weights and can be shown as a tree
  (hotspots/callers/callees), flat table, or flame graph.
- **Flat profile**: per-method self/total counts, class, package; sortable/filterable.
- **Callers/callees**: reverse index over samples.
- **Allocation**: bytes/count per class and per allocation site; live-set from
  `ObjectAllocationSample` + GC events (`GCHeapSummary`, `ObjectCount`) where available.
- **Locks**: `JavaMonitorEnter` (blocked time), `ThreadPark`, contention aggregation per monitor/owner.
- **Threads**: states over time from `jdk.ThreadStart/End` + sampled states; thread dumps (JFR
  `jdk.ThreadDump` / JMX).
- **Telemetry**: `CPULoad`, `HeapSummary`/`GCHeapSummary`, `GarbageCollection` (pauses/counts),
  `MemoryUsage`, `ThreadCount`, `ClassLoad/Unload` → timeline charts.
- **Source mapping**: `Frame` (class/method/line/bci) → project source via `SourceResolver`
  (`OpenSources`), for go-to-source and a source view next to the profile.
- **Snapshots**: `.jfr` (JFR/async-profiler), `.hprof` (heap), and an nbfx analysis snapshot (optional,
  for fast reopen of processed data) in the profiler dir.

### 4.4 UI (FX) — parity map

| NetBeans | nbfx view | Phase |
|---|---|---|
| `ProfilerTopComponent`/`ProfilerWindow`/`WelcomePanel` (control panel, session list) | Profiler window + session/snapshot list | 1 |
| `MethodsFeature(UI)` (CPU: call tree, hot methods, callers/callees) | CPU view (tree/flat/callers/callees) + **flame graph** | 2 |
| `ObjectsFeature(UI)` (allocations, live bytes, per class) | Memory view | 3 |
| `LocksFeature(UI)` | Locks view | 4 |
| `ThreadsFeature(UI)`, `ThreadDumpWindow`, `TakeThreadDumpAction` | Threads view + thread dumps | 4 |
| `MonitorFeature(UI)` (telemetry), `snaptracer` | Telemetry/timeline view (JFR events) | 4 (tracer later) |
| `ResultsView`/`ResultsPanel`/`LiveResultsPanel`, `SnapshotsWindow`, `CompareSnapshotsAction`, `Save/LoadSnapshotAction` | Results + Snapshots list + save/load/compare + live results | 2 |
| `ClassMethodSelector`, `FilterSelector`, `ProjectSelector`, `JavaPlatformSelector` | profile settings dialogs | 2 |
| `HeapWalkerUI` (classes/instances/fields/references/dominators/retained/merge paths/memory lint) | Heap walker view | 6 |
| `OQLController`/`OQLQueryCustomizer` | OQL console (later) | later |
| `ProfilingPointsWindow`/wizard (snapshot/heapdump/start/stop/stopwatch/loadgen) | Profiling points (later) | later |
| `SQLFeature(UI)`, `JdbcCCTProvider` | SQL/JDBC view (JFR JDBC events) | later |
| `AttachWizard`/`RemotePackExporter` | Attach dialog (host/port/pid) + remote later | 5 |
| `RunCalibrationAction`, `RunGCAction` | GC action (JMX `System.gc()`); calibration dropped (JFR) | 5 |
| `General/Engine/Snapshots/FiltersOptionsPanel` | Profiler options panel (backend, JFR settings, async-profiler path, snapshot dir, filters/root methods) | 7 |
| `LoadGenProfilingPoint`/load generator | later | later |

Persistence of view state and settings via `nbfx.settings` (doc 14).

### 4.5 Synergy: streaming JFR, open HPROF/JFR recordings, and the old profiler

The key insight is **one analysis model, many sources** — every view (the old profiler's "features") works
identically on live data and on opened recordings:

```
sources                              analysis model (nbfx.profiler.core)        views (nbfx.profiler.ui)
  live JFR stream (RecordingStream / FlightRecorderMXBean.openStream)
  JFR file (.jfr)                    StackTree/CCT, FlatProfile,               CPU / Memory / Locks /
  async-profiler JFR (.jfr)   ───▶   AllocationProfile, LiveSet, LockProfile, ─▶ Threads / Telemetry /
  HPROF heap dump (.hprof)           ThreadStates, Telemetry, HeapModel        Heap walker / Snapshots
  legacy NetBeans .nps
```

**Streaming JFR backend.**

- Local/launch mode: `-XX:StartFlightRecording=filename=…` and, for a live view, tail the `.jfr` with
  `jdk.jfr.consumer.RecordingFile` (poll) or run in-process with `RecordingStream` (same JVM only).
- Attached/remote target: `jdk.management.jfr.FlightRecorderMXBean` — `newRecording`, `startRecording`,
  `stopRecording`, **`openStream`/`readStream`** for incremental event streaming (JMX), `copyTo` to dump.
- **Incremental aggregation**: aggregate events as they arrive into the model (bounded memory, ring
  buffers for raw events, "spill to recording file" for later deep analysis); the UI refreshes on a
  cadence; "snapshot while recording" freezes the current aggregation into an immutable snapshot;
  pause/resume; `jfrsync` aligns async-profiler samples with the JFR timeline.

**Reuse from the old profiler** (module `org.netbeans.lib.profiler` has **zero module deps**, so it can be
depended on just for the headless parts):

- `heap/*` — **full reuse**: `HprofHeap`/`Heap`, `DominatorTree`/`DomMap`, `NearestGCRoot`,
  `ComputedSummary`/`HeapSummary`, lazy arrays, `GCRoot`s. This is the HPROF-format answer and the base of
  the heap walker.
- `results/*` — **selective reuse**: `ResultsSnapshot.readFromStream` (the `.nps` format) to **open legacy NetBeans profiler snapshots**
  (read-only: writing a private format is a promise we do not own); `CPUResultsSnapshot`/`CCTNode`
  traversal to map `.nps` into our model; `CCTFlattener`/`FlatProfileBuilder`/`CPUResultsDiff`/
  `AllocMemoryResultsDiff` where separable from the wire model; `ExportDataDumper` for HTML/CSV export;
  `ThreadDump`/`HeapHistogram`.
- Not reusable: `wireprotocol`/`server`/`instrumentation`/`classfile`/`jps` (agent, JDK-11 cap) and all of
  `lib.profiler.ui`/`lib.profiler.charts` (Swing).

**Old UI → FX with the same feature contract.**

- The NetBeans features (`MethodsFeature`, `ObjectsFeature`, `LocksFeature`, `ThreadsFeature`,
  `MonitorFeature`, `SQLFeature`) and windows (`ResultsView`/`LiveResultsPanel`, `SnapshotsWindow`,
  `SnapshotResultsWindow`, `CompareSnapshotsAction`, `LoadSnapshotAction`, `HeapWalkerUI`,
  `OpenHeapWalkerAction`, `HprofDataObject`, `NpsDataObject`) define the **view contract**; nbfx keeps the
  same tabs/structure but binds them to the shared model, so live and recorded data light up the same
  views (live vs snapshot = `LiveResultsPanel` vs `ResultsPanel`).
- **Open recordings as files**: `.jfr` (JFR/async-profiler), `.hprof` (heap), `.nps` (legacy) get a
  Profiler data object/loader + "Open in Profiler"/"Open Heap Walker"/"Compare" actions, drag & drop and
  recent files, integrated with the nbfx navigator/file actions.

**Other synergies.**

- **Debugger** (doc 16): heap walk from a debug session, thread dumps/JFR from a session, and the same
  frame→source resolver for go-to-source from profile/heap views.
- **Interop/export**: open `.jfr` in JMC (external), export collapsed stacks/HTML flame graph/CSV of the
  flat profile; import `.nps`/`.hprof` for comparison.
- **Continuous profiling**: `loop`/`jfrsync` (async-profiler) and JFR `maxAge`/`maxSize` recordings feed
  the same incremental aggregation, so "always-on" recording is just another source.

## 5. Integration

- **Profile Project/File/Test** — run configurations + `nbfx.execution` (doc 15): build the command, add
  the backend flags (`-XX:StartFlightRecording=…` or async-profiler `-agentpath`), run in Output/Terminal,
  and open the resulting recording; for Ant/Maven/Gradle the command comes from the project type
  (mirroring `profiler.projectsupport`/`maven.profiler`).
- **Attach** — pick a local JVM (`jdk.attach` `VirtualMachine.list()`) or host/port; start/stop/dump JFR
  via `FlightRecorderMXBean` (JMX) or `DiagnosticCommand`.
- **Snapshots** — save/load/compare `.jfr`; save `.hprof`; open from the Projects/Files navigator.
- **Editor** — go-to-source from a frame/method/allocation site; "Profile File" from the editor.
- **Debugger** — later: heap walk sharing the nbfx heap model (doc 16's later heap-walk step).
- **Options** — the Profiler category (doc 14).

## 6. Steps / branches

| Step | Branch | Depends | Scope |
|---|---|---|---|
| Profiler foundation | `feature/profiler-foundation` | Execution service (15) | `nbfx.profiler.core` (session, `ProfilerBackend` SPI, JFR recording start/stop/dump/save/load, analysis model skeleton, snapshot store), reuse of the headless `lib.profiler` parts (heap parser + `.nps` reader) + `nbfx.profiler.ui` shell (window, snapshot list, empty results); JDK module wiring (`jdk.jfr`, `jdk.management.jfr`, `jdk.attach`, `jdk.management`); basic options entry |
| Open recordings (`.jfr`, `.hprof`, `.nps`) | `feature/profiler-recordings` | foundation | file loaders + "Open in Profiler"/"Open Heap Walker"/"Compare" actions, drag & drop; map JFR/async-profiler, HPROF and legacy `.nps` into the shared model; exports (collapsed/HTML/CSV) |
| Streaming JFR (live views) | `feature/profiler-streaming` | foundation | incremental aggregation (bounded memory, ring buffers, spill), `RecordingStream`/`FlightRecorderMXBean.openStream`, live refresh, snapshot-while-recording, pause/resume, `jfrsync` timeline alignment |
| async-profiler backend | `feature/profiler-async-profiler` | foundation | the per-OS/arch `libs.asyncprofiler.*` modules (native libs, license, lib-path service), `AsyncProfilerBackend` (launch `-agentpath` + dynamic attach), JFR output/parsing, options, JFR fallback |
| CPU profiling | `feature/profiler-cpu` | foundation | launch/recording, call tree/flat/callers/callees, flame graph, go-to-source, save/load/compare snapshots |
| Memory profiling | `feature/profiler-memory` | foundation | allocations by class/site, live set, GC/memory events |
| Locks & threads | `feature/profiler-locks-threads` | foundation | contended monitors/time, thread states timeline, thread dumps |
| Attach & live sampler | `feature/profiler-attach-sampler` | foundation | attach to a running JVM (`jdk.attach` + `FlightRecorderMXBean`), JMX live sampler view |
| Heap walker | `feature/profiler-heap` | foundation, recordings | heap dump via `HotSpotDiagnosticMXBean`, heap model (reuse `lib.profiler.heap`), FX heap walker (classes/instances/fields/references/dominators/retained size) |
| Profiler options | `feature/profiler-options` | Options foundation (14) | full Profiler options panel |
| Snapshot tracer | `feature/profiler-snaptracer` (later) | locks/threads, attach | JFR-based timeline/tracer |
| OQL | `feature/profiler-oql` (later) | heap walker | OQL over heap dumps |
| Profiling points | `feature/profiler-ppoints` (later) | cpu/memory | profiling points (snapshot/heapdump/start/stop/stopwatch) |
| Snapshot diff UI | `feature/profiler-compare` (later) | cpu, memory | compare view |
| Legacy agent backend | `feature/profiler-legacy-agent` (optional, high risk) | — | raise the class-file cap to JDK 25, rebuild natives, validate; default off |

**Ordering**: foundation first (after the execution service); recordings + streaming next (they define the
one-model/many-sources contract and the live views); async-profiler right after the foundation; CPU next
(defines the analysis model depth); memory/locks/threads follow in parallel; attach/sampler; heap; options
after doc 14. All later features are additive.

## 7. Tests & acceptance

- **Engine (headless)**: JFR `.jfr` fixtures (recorded with `jdk.jfr` in tests) → stack-tree/flat/
  allocation/lock/telemetry aggregation; frame→source mapping; snapshot save/load; diff; backend SPI with
  a fake backend; incremental aggregation bounds.
- **Heap**: `.hprof` fixture → classes/instances/references/dominators/retained size; large-dump
  performance.
- **UI (headless glass)**: results rendering, tree/flat tables, flame-graph canvas layout, timeline
  charts, heap-walker navigation, options panel.
- **Integration (manual + scripted)**: profile a Java app (CPU samples, hot methods/flame graph, go to
  source), allocations by site, lock contention, thread states; attach to a running JVM and record;
  async-profiler backend on macOS/Linux (launch and attach); open `.jfr`/`.hprof`/`.nps` files; heap dump
  + walk a known graph (retained-size sanity); compare two snapshots; Profile Project for Ant/Maven/Gradle.
- **Purity**: `nbfx.profiler.core` JavaFX-free (`check-models`); no Swing imports; JDK module
  `--add-modules` documented; `libs.asyncprofiler.*` modules carry licenses and OS/arch requires.

## 8. Risks / open questions

- **Engine choice**: JFR is the safe default (agent-free, JDK 25) but is **sampling-based** — no exact
  invocation counts/instrumented call trees like the NetBeans agent; async-profiler closes most of the gap
  (wall-clock/alloc/lock fidelity).
- **NetBeans agent staleness**: class-file cap 55 (JDK 11), `sun.jvmstat`/`tools.jar` attach, native libs;
  a legacy backend needs non-trivial patching + native rebuilds (optional, high risk).
- **async-profiler**: macOS arm64/x64 + Linux x64/arm64 only (no Windows); `perf_event` availability
  (containers/`perf_event_paranoid`) only for perf-counter modes; `-agentpath` quoting; JDK 21+ dynamic
  attach needs `-XX:+EnableDynamicAgentLoading`; keep JFR output for one parser.
- **JFR data model**: method-level attribution is sample-based; line numbers/BCI available; large
  recordings (GBs) need streaming parse and incremental aggregation.
- **Attach/modules**: local attach needs `jdk.attach`; JMX remote needs `-Dcom.sun.management.jmxremote`;
  macOS/Windows permissions; the nbfx launcher must add `--add-modules`.
- **Heap dumps**: `dumpHeap` pauses the VM briefly and writes a full dump (can be GBs); must run off the
  FX thread with progress/cancel; the reused parser maps the file (watch disk/IO).
- **Flame graph/timeline rendering** performance in FX (Canvas vs scene graph); aggregated-sample memory.
- **Snapshot formats**: `.jfr` (JFR/async-profiler), `.hprof` (heap), optional nbfx analysis snapshot;
  legacy `.nps` import (write optional).
- **What to reuse vs extract**: depending on `org.netbeans.lib.profiler` (whole engine+agent jar) only for
  the heap/results packages, or extracting them into `nbfx.profiler.heap`/`compat` — decide during the
  foundation step (recommend extract if the dependency proves awkward).
- **Options depth**: which engine settings are user-visible (JFR `settings` file, `maxAge/maxSize`,
  async-profiler event/interval, sampler period, filters/root methods).

## 9. Decisions (locked)

1. **Agent-free first**: JFR as the primary engine + async-profiler as a first-class backend (where a
   module exists) + JMX live sampler; heap via JMX dump + the reused NetBeans heap parser.
2. **NetBeans agent out** (optional `feature/profiler-legacy-agent` only, default off) — the JDK-11
   class-file cap makes it unusable on nbfx's JDK 25 target without patching.
3. **`nbfx.profiler.core` + `nbfx.profiler.ui`** split; the heap parser (and selectively `results`/`.nps`)
   reused headless.
4. **Snapshot formats**: `.jfr` + `.hprof` (+ optional nbfx analysis snapshot, `.nps` import); parity
   features phased (tracer/OQL/profiling points/loadgen later).
5. **async-profiler is a first-class backend** (own step, right after the foundation), bundled per OS/arch
   as `libs.asyncprofiler.*` modules, launch (`-agentpath` + JFR) and dynamic attach; JFR remains the
   always-available default and the Windows path.
6. **OS/arch-based module enablement** (§3.6): per-OS/arch native modules gated by
   `org.openide.modules.os.MacOSX|Linux|Windows|Unix` + `org.openide.modules.arch.x86_64|aarch64|…`; a
   missing module ⇒ JFR fallback; the harness can additionally filter modules per platform.
7. **One model, many sources** (§4.5): live JFR stream, `.jfr` (JFR and async-profiler), `.hprof` and
   legacy `.nps` all feed one analysis model and the same FX views; the old profiler's feature/tab
   structure is the view contract.
