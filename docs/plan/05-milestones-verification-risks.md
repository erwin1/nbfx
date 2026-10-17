# 05 — Verification, risks, decisions

The **step list and order** live in [`07-small-steps.md`](07-small-steps.md) (recommended first step:
**Progress UI parity**); parts are described in [`02-part1-infrastructure.md`](02-part1-infrastructure.md),
[`03-part2-javafx.md`](03-part2-javafx.md) and [`04-fxml-and-scenebuilder.md`](04-fxml-and-scenebuilder.md).

## Verification

- Per step: `ant check-purity` (bundle allowlist for JavaFX web/swing libs), `ant build`, module unit
  tests.
- New tests: task-model wiring (progress UI); platform persistence; project property round-trip parity
  (per category); FXML completion cases; SceneBuilder save→source sync.
- Manual matrix: create/build/run a JavaFX Ant app (macOS + Linux + Windows); open FXML in the
  design/source editor; custom controls via classpath scan.

## Risks

| Risk | Mitigation |
|---|---|
| Kit on the classpath (nbfx loads JavaFX as class-path-extensions); JPMS flags | SceneBuilder-embedding spike; add `-Djavafx.allowjs=true`/`--add-opens` in the launcher if needed |
| Kit pulls `javafx.web`/`javafx.swing` (nbfx purity) | Bundle as libraries (not imported in nbfx sources); allowlist in `check-purity` |
| JavaFX version skew (IDE 27 vs kit 25/26) | Pin kit 26.0.0 / matching; test matrix |
| No Java Platform support yet (prerequisite) | Java Platforms step first; headless `java.j2seplatform` reuse; FX UI only |
| `jfxrt.jar` model obsolete (JavaFX 11+) | OpenJFX SDK / JavaFX-enabled JDK via module path |
| FXML completion is a large port | Stage it; reuse UI-free kernel, rewrite on nbfx SPI |
| Full customizer parity is large | Stage it: J2SE customizers, then Platform customizers, then JavaFX customizers; FXML toolkit first |
| JNLP/WebStart tooling removed upstream | Options stay enabled and round-trip (parity); execution out of scope; jpackage later |
| Ant runtime environment incomplete (no `ant.home`/harness classpath) | Reuse `o.apache.tools.ant.module`'s executor, or set `ant.home`/`ant.version` + harness classpath; verify on a real J2SE project |
| Build actions not gated by the project/enablement; no run/debug configs | Build actions + Run lifecycle + Run configurations steps: project-lookup providers, full command set, enablement everywhere, config model |
| Templates with custom Swing `TemplateWizard.Iterator`s | Reuse discovery + headless `createFromTemplate`; port the few custom panels to FX, else generic target+name page |
| Minimal wizard framework blocks the chooser and multi-page wizards | Build the shared FX wizard framework first |
| Progress UI at parity needs the worker SPI, not a bespoke API | Provide an FX `ProgressUIWorkerProvider` over the headless `Controller` and exclude the Swing `progress.ui`; keep `ProgressHandle`/`ProgressHandleFactory` compatible ([`06-progress-ui.md`](06-progress-ui.md)) |
| New File templates ship their own Swing wizard iterators (27 distinct) | Port incrementally (Java file wizard first); a generic name/location fallback; the model detects them (`TemplateItem.hasCustomIterator()`) — see [`07-small-steps.md`](07-small-steps.md) |
| Excluding a module from the cluster config leaves its previously built jar in the dist (e.g. `progress.ui`, `core.windows`) and it keeps loading | The harness `-prune-excluded` target removes the jar/config/update_tracking for every excluded module |
| Only **one** `ProjectProblemsImplementation` may be registered (Lookup lookup) | Patch `projectui` to drop `BrokenReferencesImpl`, or drop `projectui` after the FxProject step; the SPI lives in `projectuiapi.base`, so a purity exemption is needed until nbfx-native SPIs exist |
| Swing notification/exception dialogs still load (`notifications`, `uihandler`, `uihandler.exceptionreporter`, `lib.uihandler`) | `feature/fx-dialogs` provides the FX layer and excludes + prunes those modules ([`09-…`](09-fx-dialogs-and-notifications.md)) |
| CSS is hard-coded light-only and loaded ad hoc per module, so a real dark theme is impossible without a migration | `feature/theming`: one `ThemeManager`, semantic tokens, migrate every component sheet, follow the OS theme ([`10-…`](10-theming-and-branding.md)) |
| Branding is normally a suite-build concern, but nbfx builds clusters directly with `nbbuild` | Apply the `Branding` task in the nbfx harness (`branding.dir`/`branding.token`) plus a runtime config-FS override; document in `docs/theming.md` |
| Dark-theme contrast/icons; SVG-only template icons cannot be decoded by JavaFX | Deliberate palette/contrast pass; icons keep the coloured kind-chip fallback |
| Template display names are easy to get wrong (attribute vs localizing bundle) | Resolve `SystemFileSystem.localizingBundle` (keyed by the file path) before the `displayName` attribute |

## Deferred / dropped

- **Deferred**: real multiview FXML editor; JavaFX with **Maven**/**Gradle** (inline via the chooser +
  shared pages/generator SPI); jpackage/native bundling *execution*.
- **No deferred customizer panels**: every category enabled with full parity; legacy deployment options
  round-trip, but their *execution* is out of scope.
- **Dropped (for now)**: WebStart/JNLP/browser run *execution*.

## Resolved decisions

1. Customizer panels: **all enabled**, full feature parity (no deferred/disabled panels).
2. FXML kernel: **reuse the original code where possible, otherwise port it**.
3. SceneBuilder kit: pin **`com.gluonhq.scenebuilder:kit:26.0.0`** — latest published; no 27 exists
   (SceneBuilder's line is 15…26).
4. **Reuse stock headless SPIs** (`RecommendedTemplates`, `ProjectProblemsProvider`, …) behind a scoped
   purity exemption until nbfx-native equivalents exist (the FxProject step defines the boundary).
5. **One shared dialog/notification layer** (`nbfx.dialogs`); the wizard keeps its own chrome but
   consumes the same theme tokens.
6. **Theme defaults to System** (follow the OS); light/dark and the editor colour profile are user
   selectable; all CSS is resolved through one brandable place using the NetBeans branding mechanism.
