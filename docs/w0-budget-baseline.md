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

## Caveats (deliberate)

- **Harness startup, not the shell's “usable”.** The test sets `nbfx.test`, so `FxGuiRunLevel` does not
  launch `JavaFXLaunchApp`; `jvmToFxReadyMs` is a lower bound for the app's startup. A `--budget` mode on
  the real app (platform ready → FX start → `stage.show`) is the follow-up for the true “usable” metric.
- **CLI Ant**, not the in-process build providers: the fixture is a plain Ant project and `BuildRunner`
  falls back to the CLI. In-process providers will change the build/run numbers.
- **Machine-dependent.** The reference is one developer machine; CI records its own numbers. Compare
  medians of 5 runs, not single runs, once the baseline is automated.

## Next

1. Add `--budget` to the launcher (marks at `LauncherInstall.validate`, `FxGuiRunLevel.run`,
   `JavaFXLaunchApp.start` and `stage.show`) and write the same JSON.
2. Run 5×, take the median, commit the baseline.
3. Wire `check-budget` (fail on >20 % regression) into `feature/ci`.
