# 00 — Overview (orientation)

nbfx is a **pure-JavaFX IDE** built on the NetBeans runtime — not a plug-in and not "JavaFX support on top
of an IDE". The canonical, maintained plan is the layered roadmap; this page is only the orientation.

**Read in this order**

1. [`26-roadmap.md`](26-roadmap.md) — the layer model (`nbfx-platform` → `nbfx-ide` → `nbfx-java` /
   `nbfx-javafx`), the allocation principles and milestones **M0–M4**.
2. [`27-layer-module-map.md`](27-layer-module-map.md) — which module owns which building block.
3. [`30-api-governance.md`](30-api-governance.md) — API is a promise: packages, versions, signature tests,
   guardrails, registers, budget.
4. [`32-api-tiers-and-plugin-contract.md`](32-api-tiers-and-plugin-contract.md) — T0 internal / T1 shared /
   T2 published plugin API, the plugin contract, trust/signing and **naming conventions**.
5. [`33-swing-retirement.md`](33-swing-retirement.md) — the retirement matrix; a front end is done when
   the Swing modules leave the distribution.
6. [`34-upstream-sync-and-budget.md`](34-upstream-sync-and-budget.md) — upstream cadence, patch budget,
   numeric budgets and the execution board.
7. [`07-small-steps.md`](07-small-steps.md) + [`08-branching-and-workflow.md`](08-branching-and-workflow.md)
   — the backlog and the branch flow.

The topic docs [`01`](01-research.md)–[`29`](29-annotation-modules.md) are detail; they are slotted into
the milestones by 26 §7.

## Scope

A complete IDE: shell and platform services (M0), IDE building blocks (M1), Java/JVM tooling (M2), the
JavaFX project/FXML/SceneBuilder usage layer (M3), breadth (M4). JavaFX is the **UI toolkit** and one
**usage layer** — not the project's purpose.

## Decisions that still hold

| Area | Decision |
|---|---|
| JavaFX project kind | The **NetBeans project type id stays** `org.netbeans.modules.java.j2seproject` with `javafx.enabled=true`; `nbfx.javafx.project` is the **usage module** contributing JavaFX customizers/run/build — not a new project type (docs 03/27) |
| Customizers | **Full parity**, all categories enabled, FXML panels (docs 02/03) |
| SceneBuilder | Embedded design/source editor, kit pinned `com.gluonhq.scenebuilder:kit:26.0.0` (doc 04) |
| Build tooling | Ship Maven **and** Ant; JavaFX Ant now, Maven/Gradle JavaFX via the chooser later |
| WebStart/JNLP | Options round-trip only; legacy deployment runtime not executed |
| Clusters | `nbfx-platform`, `nbfx-ide`, `nbfx-java`, `nbfx-javafx` (+ `libs.*`), layers depend downward only |
| API | Two-tier promise (T0/T1/T2), plugin contract for external consumers (doc 32) |

## Retired framing

The earlier **Part 1 / Part 2** framing (project infrastructure *then* JavaFX) is retired. The work is
organized by **layer and milestone**; doc [`02`](02-part1-infrastructure.md) remains the project-infra
topic detail (M1/M2) and [`03`](03-part2-javafx.md)/[`04`](04-fxml-and-scenebuilder.md) the JavaFX usage
layer detail (M3). The historical MVP draft is [`docs/MVP.md`](../MVP.md) (kept for context only).
