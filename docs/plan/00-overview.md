# 00 — Overview

## Goal & scope

Give nbfx the JavaFX application experience of the original NetBeans JavaFX cluster:

- **JavaFX Ant project** support (port of `javafx2.project`) — template/wizard, project behaviour,
  run/build, packaging.
- **Java Platforms** support (nbfx has none today) — required by the JavaFX project (platform/runtime).
- **FXML editing with completion** (port of the useful core of `javafx2.editor`).
- **Embedded SceneBuilder** using the modern `com.gluonhq.scenebuilder:kit` (26.0.0) as a
  design/source editor.
- **Customizer parity** — every option the original exposed is present and round-trips.

Out of scope *for execution now*: WebStart/JNLP/browser run, deployment, signing, native bundling
(jpackage). The customizer UI still covers them.

## Two parts

- **Part 1 — complete the existing project infrastructure (no JavaFX)** → [`02-part1-infrastructure.md`](02-part1-infrastructure.md).
- **Part 2 — JavaFX project support** → [`03-part2-javafx.md`](03-part2-javafx.md).

The original code findings that inform both are in [`01-research.md`](01-research.md); FXML completion
and SceneBuilder are in [`04-fxml-and-scenebuilder.md`](04-fxml-and-scenebuilder.md); milestones, risks
and decisions are consolidated in [`05-milestones-verification-risks.md`](05-milestones-verification-risks.md).

## Locked decisions

| Area | Decision |
|---|---|
| First targets | **Ant + JavaFX** only. Maven/Gradle JavaFX later. |
| Parts | **Part 1**: complete the existing infrastructure (project types, Java Platforms, wizard framework, New Project/New File templates, customizers, build actions/activation, run/debug configs). **Part 2**: JavaFX. |
| Build tooling | Ship **both**: Maven (existing) **and** Ant (bundled Apache Ant + the J2SE harness). JavaFX Ant projects use the Ant path; Maven/Gradle JavaFX come later via the chooser. |
| Project templates | Introduce a shared **New Project + New File template chooser and infrastructure** first; project types are chosen inline there. |
| Project kind | **Mirror the original**: a JavaFX project *is* the J2SE Ant project (`org.netbeans.modules.java.j2seproject`) with `javafx.enabled=true`; no new project type. |
| Customizer | **Full parity** with the original J2SE + JavaFX customizer — **all categories enabled** (no deferred/disabled panels). |
| Customizer UI | Panels built with **FXML** (`FXMLLoader` + controllers), not hand-built nodes. |
| Customizer order | **Complete the existing J2SE/Ant customizer panels first**, then add the JavaFX extensions. |
| FXML | **FXML-aware completion** is in scope; kernel **reused where possible, otherwise ported**. |
| SceneBuilder | **Embedded**, as a **design/source editor** for `.fxml` (to become a real multiview later). |
| SceneBuilder kit | **Downloaded via the netbeans `external/` mechanism** (not committed); pin **`com.gluonhq.scenebuilder:kit:26.0.0`** (latest — no 27 exists). |
| Cluster | A **separate `nbfx-javafx` cluster**, mirroring the original `javafx` cluster. |
| Progress UI | **Full parity** ([`06-progress-ui.md`](06-progress-ui.md)): an FX `ProgressUIWorkerProvider` over the headless progress `Controller` (multi-task status line + **all-tasks popup** + per-task details/actions); exclude the Swing `progress.ui`. |
| WebStart | Customizer options are **enabled and round-trip** (full parity); the legacy deployment *runtime* is not executed (its ant tooling no longer ships). |
