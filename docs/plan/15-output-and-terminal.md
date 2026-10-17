# 15 — Output window & real terminal

Steps: **Output window** (`feature/output-window`), **Execution service** (`feature/execution-service`),
**Terminal emulator model** (`feature/terminal-model`), **Terminal PTY backend** (`feature/terminal-pty`),
**Terminal view** (`feature/terminal-view`), **Run/build output integration**
(`feature/run-output-integration`), **Terminal/Output options** (`feature/terminal-options`), **Remote
terminal** (later, `feature/terminal-remote`).

## Why

Today's clean/build/run output is oversimplified: consoles are read-only `TextArea`s with no ANSI, links,
folds, input or limits, the process runner is line-based, the build SPI carries plain text, and Maven runs
in-process via the embedder with no cancellation. A real IDE needs both a **proper Output window**
(NetBeans `core.output2` parity) and a **real terminal** (PTY + VT/xterm emulation) for interactive runs,
shells and tools that need a TTY.

## What nbfx has today

| Piece | File(s) | Limitation |
|---|---|---|
| Output service | `nbfx-platform/c.gluonhq.netbeans.nbfx.output`: `FxOutput`, `FxConsole`, `ConsoleModel`, `FxOutputImpl`, `FxThread` | bespoke API; not `org.netbeans.api.io`/`spi.io` |
| Output view | `OutputViewProvider` | one read-only `TextArea` per console; no ANSI/links/folds/input/limits/search |
| Process runner | `nbfx-ide/c.gluonhq.netbeans.nbfx.run`: `ProcessRunner` | `ProcessBuilder`, stderr merged, `readLine`, text append; no PTY, no cancel/exit API, UTF-8 only |
| Build orchestration | `BuildRunner`, `BuildTool` | mvn/gradle/ant detection; in-process provider or CLI; plain text |
| Build SPI | `nbfx-project.ui.api`: `BuildActionProvider`, `BuildExecution`, `BuildOutput.append(String)`, `BuildProgress` | text-only output; single-line progress; no cancellation contract |
| Maven | `nbfx.project.ui.maven.MavenBuild` | NetBeans **embedder** in-process, Plexus logs via a JUL `Handler`; no cancel, no transfer progress, `run` = `exec:java` (no process/stdin/TTY) |
| Ant / Gradle | `nbfx.build.ant.AntBuild`, `nbfx.project.ui.gradle.GradleBuildActionProvider` | same shape |

## Reuse inventory (original codebase)

| Candidate | Where | Verdict |
|---|---|---|
| Clean IO SPI/API | `platform/api.io` (`InputOutput`, `OutputWriter`, `OutputColor`, `Hyperlink`, `Fold`, `Position`, `IOProvider`) + `platform/spi.io.InputOutputProvider` | **Reuse**; nbfx implements the provider (does not today) |
| Output window | `platform/core.output2` | **Port parts**: `OutWriter.printANSI` (CSI+SGR, xterm palette, bright/faint, carry-over) → FX `Color`; `Lines`/`Storage`/`HeapStorage` design; `Controller`/`OutputTab` are Swing |
| Terminal emulator | `ide/lib.terminalemulator` (Apache-2.0) | **Port where it makes sense**, else fresh (§5.3) |
| Terminal API shape | `ide/terminal` (`IOTerm`, `IOConnect`, `IOEmulation`, `IONotifier`, `IOResizable`) | design reference (wired to legacy `openide.io`) |
| Terminal UI | `ide/terminal.nb` | not reusable (Swing/AWT); reference for tab/pin/font actions |
| PTY / native exec | `ide/dlight.nativeexecution` (`PtyAllocator`, `PtySupport`, `NativeProcessBuilder`, JNA + `bin/.../pty_open`) | reuse option (Apache-2.0) but heavy deps (`extexecution`, `keyring`, `jsch`, `terminal`, `openide.io`); **not chosen** |
| Process model | `ide/extexecution.base` + `ide/extexecution.process` | **clean & reusable** (`ProcessParameters`, `Environment`, `ProcessBuilder`, `BaseExecutionService`, `LineProcessors`); `ide/extexecution` is legacy-IO-coupled → skip |
| JNA | `platform/libs.jna` (Apache-2.0) | not needed with JLine, but available |
| `lib.richexecution` | external (TermApp, **not in this tree**) | not shippable |

## Decisions (locked)

1. **PTY backend: JLine 3** (BSD-3-Clause; already accepted as `nbbuild/licenses/BSD-jline3`). Use
   `org.jline:jline-terminal` + a native provider (`terminal-jna` or `terminal-ffm`), the exec PTY
   (`org.jline.terminal.impl.exec.ExecPty`/`ExecTerminalProvider`), the per-OS termios mappings
   (Linux/macOS/FreeBSD/AIX/Solaris) and signal handling. NetBeans has **no dedicated JLine module** —
   JLine ships inside `ide/libs.graalsdk` (`jline-24.0.0.jar`, used by JShell), so add a proper
   **`libs.jline`** external module (like `libs.jna`) and **align the version with the platform's JShell**.
   No pty4j (EPL-1.0).
2. **Emulator: a fresh VT/xterm model in `nbfx.terminal.model`**, **porting the well-tested pieces** of
   `lib.terminalemulator` where they make sense (§5.3). Do not port the Swing `Term`/`Screen`.
3. **Emulation is two steps**: the headless model (`feature/terminal-model`) and the JavaFX view
   (`feature/terminal-view`).
4. **Run defaults to the Terminal** (PTY). Each run configuration has an `Output | Terminal` choice
   (default Terminal); the Output window remains the home of build/test and non-interactive runs.
5. **Maven builds move to a separate process entirely** — no embedder for build/run/test. The Maven
   **embedder stays only for project model/classpath/metadata** (`NbMavenProject`), not for executing
   goals.

## Target architecture

### 5.1 `nbfx.output` (UI) — Output window parity

- Implement `org.netbeans.spi.io.InputOutputProvider` on a JavaFX view; register it so
  `org.netbeans.api.io.IOProvider` resolves to nbfx. Keep `FxOutput`/`FxConsole` as a thin facade while
  migrating callers.
- View: one tab per `InputOutput`, virtualised styled-text rendering (not `TextArea`), theme tokens.
- Features: `print`/`println`, `OutputColor`, `Hyperlink` (click → editor), `Fold`, `Position`,
  "important/error" lines, Reset/Clear, input (`getIn`) with a prompt, line/byte limits with trimming,
  search, "Clear All", tab naming/lifecycle.
- **ANSI SGR**: port `core.output2`'s `printANSI` (CSI+SGR, xterm palette, bright/faint, carry-over
  across writes) with FX `Color`; strip non-SGR control (that belongs to the Terminal).

### 5.2 `nbfx.execution` (model, headless) — process layer

- `ExecutionService`/`ExecutionDescriptor` (command/argv, working dir, `Environment`, charset, redirects,
  output mode `PIPE|PTY`, cancel, exit listener), `ExecutionHandle` (cancel, exit status, stdin writer),
  output pumps (out/err split, line/raw modes), progress hook.
- PTY mode is the seam the Terminal uses; PIPE mode the Output window. Same API for build/run/test.
- Reuse `extexecution.base`/`extexecution.process` where it fits; do not depend on `ide/extexecution`.

### 5.3 `nbfx.terminal.model` (model, headless) — VT/xterm emulator

- Public surface: feed bytes; render model (lines with attribute runs); resize(cols, rows) + reflow hook;
  scrollback get/trim; selection/word-delineation reference; title/icon (OSC 0/2); cwd (OSC 7); cursor
  position/visibility; bell; input encoder (key descriptor → bytes); PTY size callback (`IOResizable`
  analogue); emulation level (dumb/ANSI/dtterm/xterm).

| Port from `lib.terminalemulator` | Write fresh |
|---|---|
| CSI dispatch tables + SGR/DEC-mode handling patterns from `InterpDtTerm`/`InterpANSI`/`InterpProtoANSI`; `Ops` operation enumeration; `Attr`/`AttrSave` semantics | Screen/buffer/**scrollback** data structures (model/UI split, FX rendering, efficient resize/reflow) |
| `LineDiscipline` semantics (EOL/BS/DEL/echo/wrap/reverse-wrap, insert/delete, alt-screen) | Public `TerminalModel` API + change events (`java.beans`-style, no JavaFX) |
| `StreamTerm` reader-thread pattern; upstream `TermTest` **fixtures** | Input encoder (JavaFX key event → escape bytes); keymap/mouse later |
| | 256/truecolor + theme mapping; OSC (title/cwd/links); modern xterm bits the old code lacks; selection/word delineation |

### 5.4 `nbfx.terminal.ui` (UI) + PTY backend (JLine-based)

- **PTY backend**: `nbfx.execution` PTY mode implemented over JLine (`ExecPty`/`ExecTerminalProvider`,
  native provider for termios/signals); `PtyProcess` interface (stdin/stdout bytes, resize, signals, exit,
  `tty` name) so the backend stays swappable.
- **View**: JavaFX `Canvas` glyph grid (attributes as runs), scrollback, cursor blink, selection +
  copy/paste, find, font zoom, resize → `setWindowSize`, theme colours (ANSI 16 + 256 × theme).
- **View/tabs**: `ViewProvider`, New/Close/Rename/Pin, "Open Terminal Here", "Run in Terminal", "Open in
  External Terminal"; Remote later.

### 5.5 Options (doc 14)

- **Terminal** category: font family/size, palette/theme, scrollback, cursor shape/blink, shell/argv,
  environment, line discipline, bell; **Output** options: line limit, colors, timestamps. Persisted via
  `nbfx.settings`.

## Integration

- **Run** (default **Terminal**): always a separate `java` process (classpath/module path computed from
  the project model), PTY mode; Output-window mode available per configuration. Fixes stdin,
  `System.console()`, ANSI and interactive apps. `exec:java` as fallback only.
- **Build/test**: Output window over PIPE.
  - **Maven (process-only)**: run `mvn`/`mvnw` with the options-system settings (home, `settings.xml`,
    offline), environment from the JDK platform options; **cancel = destroy the process tree**; progress
    from the process lifecycle (Tooling API later). The embedder is no longer used to execute goals.
  - **Ant**: `BuildListener`/`LogListener` adapter → `OutputWriter`; cancel via the process.
  - **Gradle**: CLI, `--console=plain` into the Output window; "Run in Terminal" for the rich console.
- **Tab naming/lifecycle** `project (build|run|test)`; Output actions (Clear, Clear All, Find, Next/Prev
  Error, Rerun); error lines → editor hyperlinks.

## Steps / branches

| Step | Branch | Depends | Scope |
|---|---|---|---|
| Output window (api.io + ANSI + FX view) | `feature/output-window` | theming (styles) | rewrite `nbfx.output` on `spi.io`/`api.io`; port SGR colours; links/folds/input/limits; migrate callers; tests |
| Execution service | `feature/execution-service` | — | `nbfx.execution` (PIPE/PTY seam, env, charset, cancel, exit, pumps); adopt in `nbfx.run`; progress |
| Terminal emulator model | `feature/terminal-model` | — | fresh VT/xterm model + ported pieces; escape-sequence fixtures; input encoder |
| Terminal PTY backend | `feature/terminal-pty` | execution-service | JLine-based `PtyProcess` (resize, signals, exit, `tty`); `libs.jline`; licence entries; tests |
| Terminal view | `feature/terminal-view` | terminal-model, terminal-pty, theming | Canvas renderer, scrollback, selection/copy, find, resize, colours; view/tabs/actions |
| Run/build output integration | `feature/run-output-integration` | execution-service, output-window, terminal-view | run = process with **Terminal default**; **Maven as a process** (cancel); Ant/Gradle output; tabs; hyperlinks |
| Terminal/Output options | `feature/terminal-options` | doc 14 options foundation | Terminal + Output option panels via `nbfx.settings` |
| Remote terminal (later) | `feature/terminal-remote` | terminal-view | SSH/remote sessions (`dlight.terminal` analogue) |

**Ordering**: `output-window` and `terminal-model` can run in parallel; `terminal-pty` needs the execution
seam; `terminal-view` needs the model + PTY; integration last, after the Output-window and run-as-process
changes land.

## Tests & acceptance

- **Output**: SGR parsing (colours, bright/faint, split writes, reset), hyperlink callbacks, input prompt,
  limits/trimming, tab lifecycle.
- **Execution**: start/cancel/exit, env/cwd/charset, PIPE vs PTY, out/err ordering, back-pressure.
- **Terminal model**: escape-sequence fixtures (CSI movement/erase/SGR, DEC private modes, scroll regions,
  tabs, OSC title/cwd, alt-screen, line discipline echo), resize/reflow, scrollback, input encoding.
- **Terminal view (headless glass)**: glyph grid from the model, selection/copy, resize → `setWindowSize`,
  theme colours.
- **Integration (manual + scripted)**: `vim`/`top` render and respond; run a `java` app in the terminal
  with stdin (`Scanner`) and `System.console() != null`; window resize; a JLine CLI works;
  `mvn package` in a process shows output and **cancels**; `ant` output; "Run in Terminal" from the
  navigator.
- **Purity**: `nbfx.execution`/`nbfx.terminal.model` JavaFX-free (`check-models`); UI modules no
  Swing/AWT; `libs.jline` gets `licenseinfo`/`nbbuild/licenses` entries.

## Risks / open items

- **JLine version alignment** with the platform's JShell JLine; whether to add `libs.jline` or reuse the
  shipped jar. `ExecPty` may rely on an external helper (`script`/`socat`) on some platforms — verify per
  OS before relying on it; the native `terminal-jna`/`terminal-ffm` provider is the fallback.
- **Fresh model scope**: v1 = SGR + cursor + erase + scroll regions + alt-screen + title/cwd + resize +
  line discipline; mouse reporting, sixel/graphics, reflow, truecolor and bracketed paste later.
- **Reflow on resize** is the hardest part of a grid model; decide the wrap policy (most terminals keep
  non-reflowed lines).
- **Maven as a process**: loses in-JVM speed; needs the options-system settings and a JDK/`JAVA_HOME` from
  the platform options; verify the project model still refreshes after builds.
- **Performance**: Canvas glyph grid vs virtualised `TextFlow`; redraw strategy; scrollback limits.
- **Licensing**: JLine BSD-3 (already accepted); add `licenseinfo.xml` entries.
