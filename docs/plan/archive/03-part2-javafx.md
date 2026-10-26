<!--

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
# 03 — JavaFX usage layer (M3 topic detail)

> Topic detail for milestone M3 (see [`26-roadmap.md`](26-roadmap.md)); JavaFX is a usage layer on top of
> M0–M2, not the project's purpose. Naming: doc [`32`](32-api-tiers-and-plugin-contract.md).

Goal: add first-class JavaFX support on top of the completed platform/IDE/Java layers
([`02-part1-infrastructure.md`](02-part1-infrastructure.md)). The steps are in
[`07-small-steps.md`](07-small-steps.md).

## New cluster `nbfx-javafx` (mirrors the original `javafx` cluster)

```
nbfx-javafx/
  c.gluonhq.netbeans.nbfx.javafx.lib.scenebuilder   ← nbscenebuilder lib.scenebuilder (bundles the kit)
  c.gluonhq.netbeans.nbfx.javafx.platform           ← javafx2.platform
  c.gluonhq.netbeans.nbfx.javafx.project            ← javafx2.project
  c.gluonhq.netbeans.nbfx.javafx.editor             ← javafx2.editor (FXML model/completion + design/source doc)
  c.gluonhq.netbeans.nbfx.javafx.scenebuilder       ← javafx2.scenebuilder + nbscenebuilder (embedded)
  c.gluonhq.netbeans.nbfx.javafx.kit                ← javafx2.kit (templates)         [optional]
  c.gluonhq.netbeans.nbfx.javafx.samples            ← javafx2.samples                 [optional]
```

- `nb.cluster.nbfx-javafx.dir=nbfx-javafx`, `depends=nb.cluster.nbfx-java` → sits above
  `nbfx-java` → `nbfx-ide` → `nbfx-platform`, parallel to upstream `javafx` above `java`.
- The **general Java Platform** support lives in `nbfx-java` (`c.gluonhq.netbeans.nbfx.java.platform`,
  parallel to `java.platform`); the **JavaFX-specific platform** utils live in `nbfx-javafx`.

## JavaFX project kind & wizard

- **Templates in the chooser** (Part 1, "New Project templates") with **reusable JavaFX wizard pages**
  (`JavaFxWizardPages`: kind Application / FXML / Preloader / Swing / Existing; Java platform + JavaFX
  runtime; main class / FXML name; app name + package; preloader) and a **`JavaFxProjectGenerator` SPI**
  (`AntJavaFxGenerator` now; `MavenJavaFxGenerator` / `GradleJavaFxGenerator` later).
- Recognise j2seproject + `javafx.enabled` as JavaFX; JavaFX panels/commands gated accordingly
  (see [`01-research.md`](01-research.md) §2).
- **AntJavaFxGenerator** creates the J2SE Ant project (`build.xml`, `nbproject/project.xml`,
  `project.properties`, `jfx-impl.xml`), setting JavaFX properties (platform, main class, preloader, app
  metadata). Consider reusing headless `ProjectGenerator` (`ide/project.ant`).
- **Modernised `jfx-impl.xml`**: keep `compile`/`jar`; `run`/`debug` launch with
  `--module-path <fx> --add-modules …` (or classpath); **no** deploy/self-contained/WebStart targets.
  The original relied on the removed **`ant-javafx.jar`** tasks (`fx:jar`/`fx:deploy`/`fx:signjar`),
  which modern JavaFX/JDKs no longer ship — dropped (packaging via jpackage later).
- Port helpers: opened hook, icon annotator, problems, platform updater, generated-files
  helper/interceptor, `ActionProvider`.

## JavaFX customizer extensions

Extend the Part 1 FXML customizer base (as the original extends `CustomizerApplication`/`CustomizerJar`/
`CustomizerRun` via `J2SECategoryExtensionProvider`). Categories and options are in
[`02-part1-infrastructure.md`](02-part1-infrastructure.md) (the J2SE+JavaFX matrix, `javafx.*`
properties): Application/Run, Packaging, Deployment/Signing/Download/Request-Runtime/JS-Callbacks. All
categories enabled (no deferred panels); legacy deployment options round-trip but are not executed.

## Build / run integration

- Build via `nbfx.build.ant`; Run via `nbfx.run` (in-process). Platform/VM args, module path, jar.
- Ant runtime environment fixes from Part 1 ("Ant runtime") are a prerequisite.
- No WebStart/deploy/native execution yet (options persist via the customizer).

## Repo-harness wiring for the new cluster

- `cluster/nbfx-cluster.properties.fragment`: add `nb.cluster.nbfx-javafx.{dir,depends,list}`; add it to
  `clusters.config.nbfx.list`.
- `nbfx.properties`: `nbfx.clusters=…,nbfx-javafx`.
- `build.xml`: add the 4th cluster to the overlay/unoverlay/exclude macros (they hard-code three).
- `check-purity.xml`: add `nbfx-javafx/*/src/**` and `nbfx-javafx/*/nbproject/project.xml`; include it in
  the packaging scan (bundle allowlist for `javafx.web`/`javafx.swing` libs).
- `patches/0010-launcher-clusters`: add `nbfx-javafx`; `patches/0013-rat-exclusions`: add `nbfx-javafx/**`.
- `docs/repo-and-build.md` + `docs/MVP.md`: document the new cluster.

## FXML editing & SceneBuilder

See [`04-fxml-and-scenebuilder.md`](04-fxml-and-scenebuilder.md).
