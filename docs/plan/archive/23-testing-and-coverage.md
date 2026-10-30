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
# 23 — Testing & code coverage

Steps: **Test runner core** (`feature/test-runner`), **Test results UI** (`feature/test-results-ui`),
**Run/debug tests & configurations** (`feature/test-run-configs`), **Code coverage** (`feature/code-coverage`).

## Why

"Test Project/File", the Test Results window, per-method re-run, go-to-failure and code coverage are part
of the basic Java workflow. The plan so far only has "Debug Test" inside the debug build-integration step
and JUnit *file templates*.

## What nbfx has today

New File templates for JUnit/TestNG (doc 02/07) and the `debug`/`test` build commands in the action set
(doc 02); no test runner, no results view, no coverage.

## Research (original codebase)

| Module | Files / Swing | Role | Verdict |
|---|---|---|---|
| `java/java.testrunner` | 11 / 3 | test **SPI + framework** (`TestRunner`, `TestSession`, `Testcase`, `DefaultPlugin`, `CommonTestUtil`, `JavaRootsProvider`, `GoToOppositeAction`), Ant session support | **reuse** (mostly headless; only `openide.awt`/`openide.dialogs` deps) |
| `java/java.testrunner.ui` | 12 / 4 | the generic **Test Results** window UI | **port** to FX (results tree, output, statistics) |
| `java/junit` + `java/junit.ant` | 25 + — | JUnit 3/4/5 integration: generators (`JUnit3/4/5TestGenerator`), `JUnitExecutionManager`, `JUnitAntLogger`, `JUnitOutputReader`, `JUnitSettings`, test classes/suites | **reuse** the runner/parser logic |
| `java/junit.ui`, `java/junit.ant.ui` | 24 / 18, — | JUnit UI (results/details/customizers) | **port** |
| `java/testng`, `java/testng.ant`, `java/testng.ui` | — / 27 / 16 | TestNG integration | **reuse logic**, port UI |
| `java/maven.junit`, `java/maven.junit.ui` | — | Maven Surefire integration | reuse (report parsing), port UI |
| `extide/gradle.test` | — | Gradle test tasks | reuse |
| `ide/gsf.codecoverage` | — | framework-agnostic coverage model (`CoverageProvider`, file stats) | **reuse** the model; port UI |
| `java/maven.coverage` | — | JaCoCo integration for Maven (`org.jacoco.core`) | reuse/adapt |
| `java/gradle.java.coverage` | — | JaCoCo for Gradle + **bundled `org.jacoco.core-0.8.12.jar`** (EPL-2.0 + notice) | reuse the bundled library |
| `ide/gsf.testrunner`, `java/java.testrunner` (Ant) | — | generic runner/ant wiring | reference |

Licence: JaCoCo is EPL-2.0 and **already shipped in the NetBeans tree** with a notice
(`java/gradle.java.coverage/external/org.jacoco.core-0.8.12-license.txt`), so bundling/reusing it is
policy-wise settled.

## Target architecture

```
nbfx.test.core        (headless model, JavaFX-free)
  TestRunner SPI      discover tests for a file/package/project; run (per-project-type: Ant/Maven/Gradle)
  TestSession         results tree (suite/case), states, durations, output, failure/skip, re-run failed
  ReportParsers       JUnit XML, Surefire/TNG reports, Gradle test output
  CoverageEngine      JaCoCo agent/exec + org.jacoco.core report analysis over SOURCE roots
nbfx.test.ui          (JavaFX)
  TestResultsView     dockable: tree of sessions/results, output pane, statistics, filters, actions
  CoverageView        per-package/class/method/line coverage, editor line highlighting, "Show coverage"
  Actions             Test File/Project/Package, Run/Debug Test, Re-run Failed, Go to Failure/Source
```

- Runs execute through `nbfx.execution` (doc 15) — process-based, cancellable, output into the Output
  window and/or the results view; Maven/Gradle use their test goals with report parsing.
- Test results can also arrive from the debugger (doc 16) when "Debug Test" is used.

## Steps

1. **Test runner core** (`feature/test-runner`) — the `TestRunner` SPI + discovery (file/package/project),
   per-project-type runners (Ant/Maven/Gradle) over `nbfx.execution`, result model + report parsers (JUnit
   XML/Surefire/TNG), cancellation, go-to-failure mapping. Reuse `java.testrunner`/`junit` logic where
   possible.
2. **Test results UI** (`feature/test-results-ui`) — the dockable FX Test Results view (session tree, test
   tree, output pane, statistics bar, navigation between failures), actions (re-run failed, re-run test,
   stop, clear), integration with the Output window and the status line (progress via doc 06).
3. **Run/debug tests & configurations** (`feature/test-run-configs`) — "Test File/Project/Package",
   "Run/Debug Test", the test-related run configuration fields (doc 02), and debug-test integration
   (doc 16's build-integration step).
4. **Code coverage** (`feature/code-coverage`) — JaCoCo integration using the bundled `org.jacoco.core`
   (agent for tests launched by nbfx, `.exec`/report analysis over SOURCE classpaths), the coverage model
   (reuse `gsf.codecoverage`), the FX coverage view (project/package/class/method/line statistics) and
   editor line highlighting; enable/disable in test configurations; Maven/Gradle coverage tasks.

## Acceptance

- Test File/Project runs the right tests for Ant/Maven/Gradle, shows them in the Test Results view, and
  "Go to Failure" opens the failing line; failed re-runs are one action.
- Debug Test reaches a breakpoint in the test (with doc 16).
- Coverage: a run produces line/class/project coverage, highlighted in the editor, and reports match
  JaCoCo's own numbers.
- `nbfx.test.core` is JavaFX-free (`check-models`).

## Risks / open questions

- **Reuse seam**: `java.testrunner`/`junit` mix logic and Swing; port the UI, reuse the runners/parsers
  (verify each class).
- **Report formats** vary by framework/version (Surefire XML, TNG, Gradle); parser coverage needs fixtures.
- **JaCoCo agent attachment**: offline instrumentation vs agent via `-javaagent` (path to the bundled jar);
  argument building per project type; EPL notice retained.
- **Debug Test** coordination: starting the test JVM with both JDWP and the JaCoCo agent.
- **Large suites**: results view virtualization and output volume (reuse the output window limits).
