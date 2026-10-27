# W0 scenario — how to run

Three levels, from fastest to most realistic.

## 1. Service-level headless test (CI gate)

```sh
cd netbeans/nbbuild
ant -f ../nbfx-ide/c.gluonhq.netbeans.nbfx.launcher/build.xml test
```

`ScenarioTest` uses the module system, glass headless
(`-Dglass.platform=Headless -Dprism.order=sw -Djava.awt.headless=true`), copies the fixture to a
temporary git repository, opens it, edits and saves `Main.java`, builds and runs it through
`BuildRunner`, asserts the marker in `FxOutput`, commits and asserts a clean status. It also records
the phase budget (see [`w0-budget-baseline.md`](w0-budget-baseline.md)).

## 2. Full-application smoke (real shell, exits by itself)

```sh
NBFX_SCENARIO="$PWD/nbfx-tests/fixtures/ant-hello" \
NBFX_SCENARIO_REPORT=/tmp/nbfx-scenario-report.json \
JAVA_TOOL_OPTIONS="-Dglass.platform=Headless -Dprism.order=sw -Djava.awt.headless=true" \
ant run
```

`ScenarioRunner` (started by `FxGuiRunLevel` after the shell is usable) does the same scenario through
the **running application**: `ProjectOpener`/`ProjectRegistry`, the editor service, a named output
console, Ant build/run, a git commit; it writes `{"ok": true, "work": "…", "error": null}` to
`NBFX_SCENARIO_REPORT` and exits with 0/1. Without the headless `JAVA_TOOL_OPTIONS` it opens a real
window.

## 3. Startup budget

```sh
NBFX_BUDGET=/tmp/nbfx-budget.json ant run
ant -f check-purity.xml check-budget -Dnbfx.budget.file=/tmp/nbfx-budget.json
```

See [`w0-budget-baseline.md`](w0-budget-baseline.md) for the marks and the cold/warm/windowed
references.

## Fixture

`nbfx-tests/fixtures/ant-hello` — a real J2SE Ant project adapted from the upstream `SampleProject`
(with `build-impl.xml`); `hello.Main` prints `NB_FX_SCENARIO_OK`. The build has `build`/`test-unit`
aliases for the IDE commands. Tests copy it, so the checked-in fixture stays clean.
