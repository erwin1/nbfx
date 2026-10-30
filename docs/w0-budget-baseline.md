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
# W0 budget baseline

The headless scenario test records its phases to `budget.json` in the test work directory and prints a
`[budget]` line. It is the first reproducible baseline; the plan's `check-budget` can compare against it
once the numbers are stable.

## How to measure

```sh
cd netbeans/nbbuild
ant -f ../nbfx-ide/c.gluonhq.netbeans.nbfx.launcher/build.xml test
```

Artifact: `nbfx-ide/c.gluonhq.netbeans.nbfx.launcher/build/test/unit/work/*/budget.json`
(the exact directory is printed by the test). The test uses **glass headless**
(`-Dglass.platform=Headless -Dprism.order=sw -Djava.awt.headless=true`).

## Reference run (macOS, JDK 25, glass headless)

| Metric | ms | What it covers |
|---|---:|---|
| `jvmToFxReadyMs` | 6469 | JVM start → FX toolkit ready, including the NetBeans module system |
| `fxReadyToOpenMs` | 542 | project open (`ProjectRegistry`/`OpenProjects`) |
| `openToEditMs` | 480 | create the editor, insert a line, save |
| `editToBuildMs` | 628 | `ant build` through `BuildRunner` |
| `buildToRunMs` | 610 | `ant run` through `BuildRunner` + marker assertion |
| `totalAfterFxMs` | 2262 | the scenario after FX is ready |

## App startup (real shell, headless)

Five runs of the real application in budget mode:

```sh
NBFX_BUDGET=/tmp/nbfx-budget.json \
JAVA_TOOL_OPTIONS="-Dglass.platform=Headless -Dprism.order=sw -Djava.awt.headless=true" \
ant run
```

| Mark | Median ms | What it covers |
|---|---:|---|
| `modulesValidated` | 352 | `LauncherInstall.validate`: module system ready for early work |
| `platformReady` | 835 | `FxGuiRunLevel`: modules enabled, platform up |
| `fxStart` | 877 | `JavaFXLaunchApp.start` entered |
| `usable` | 1462 | the shell is shown (window system `show` / `stage.show`) |

The first (cold) run was 3323 ms; subsequent runs settle around 1.4–1.5 s. The marks are relative to the
JVM start. The budget mode waits for `usable`, writes the JSON and exits (`Platform.exit`).

A **windowed** run (macOS, real display, no headless flags) measured:

| Mark | ms |
|---|---:|
| `modulesValidated` | 552 |
| `platformReady` | 1143 |
| `fxStart` | 1256 |
| `usable` | 1978 |

`docs/w0-budget-baseline.json` is the **cold** startup baseline used by `check-budget`
(614 / 2088 / 2177 / 3323); the table above is the warm median. A fresh app start is cold, so the
check compares the cold values and a warm run passes with margin.

## check-budget

```sh
NBFX_BUDGET=/tmp/nbfx-budget.json ant run        # writes the JSON
ant -f check-purity.xml check-budget -Dnbfx.budget.file=/tmp/nbfx-budget.json
```

`nbfx-tests/tools/BudgetCheck.java` compares the marks with the committed baseline and fails on a
regression beyond **20 % or 100 ms** (whichever is larger). Marks missing from the current run are
reported but do not fail. Verified: the cold baseline passes; a synthetic `usable=5000` fails.

## Caveats (deliberate)

- **Harness startup, not the shell's “usable”.** The test sets `nbfx.test`, so `FxGuiRunLevel` does not
  launch `JavaFXLaunchApp`; `jvmToFxReadyMs` is a lower bound for the app's startup. A `--budget` mode on
  the real app (platform ready → FX start → `stage.show`) is the follow-up for the true “usable” metric.
- **CLI Ant**, not the in-process build providers: the fixture is a plain Ant project and `BuildRunner`
  falls back to the CLI. In-process providers will change the build/run numbers.
- **Machine-dependent.** The reference is one developer machine; CI records its own numbers. Compare
  medians of 5 runs, not single runs, once the baseline is automated.

## Next

1. Wire `check-budget` into `feature/ci` after `feature/ci` and this branch share a base (the CI
   branch is a sibling of the app-budget branch; merge #14/#15/#16/#17 first, then add the step).
2. Keep the harness table for the scenario phases; the app tables are the “usable” baseline.
3. Re-baseline on the CI runner (cold, Linux) before making the check blocking there.
