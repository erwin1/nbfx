# nbfx — plan

**The plan is [`PLAN.md`](PLAN.md).** It is the single, canonical document; read it top to bottom.

| Document | Purpose |
|---|---|
| [`PLAN.md`](PLAN.md) | Vision, layers/clusters, naming conventions, API tiers and governance, module catalogue, migration map, guardrails, roadmap (M0–M5), workstreams, decisions |
| [`figures.md`](figures.md) | **Generated** — every number the plan states, derived from the tree by [`tools/Counts.java`](tools/Counts.java); `Counts.java --check` fails when it is stale |
| [`appendix/A-toolkit-policy.md`](appendix/A-toolkit-policy.md) | What each tier may reference; the `java.awt`/`javax.swing` decision; bridge-vs-replace per upstream API; nb-javac rule; exemption register; guard mechanism |
| [`appendix/B-java-source-isolation.md`](appendix/B-java-source-isolation.md) | How the prototype uses `java.source`, why it is already contained, the `java.model` bridge, the module split and the migration steps |
| [`appendix/C-naming-and-rename-table.md`](appendix/C-naming-and-rename-table.md) | The naming rule per location and the rename table for the 53 public `Fx*` types; `api.layer.LayerPaths` |
| [`appendix/D-bridge-and-retirement-matrix.md`](appendix/D-bridge-and-retirement-matrix.md) | Every upstream module: reuse / bridge / replace / drop, exemptions, the 45 editor/Java modules, retirement per milestone, indexing, compiler choice |
| [`appendix/E-contracts.md`](appendix/E-contracts.md) | Threading per SPI, disposal/ownership + leak test, accessibility, error/logging, the `Lookup` rule, plugin allowlist, removal of generic `C` factories |
| [`appendix/F-node-and-view-migration.md`](appendix/F-node-and-view-migration.md) | The 25 view files, their item models, the `api.nodes` target and the migration order |
| [`appendix/G-licences-and-notices.md`](appendix/G-licences-and-notices.md) | JavaFX 27 (GPLv2+CE), native/lib licences, NOTICE rules, `check-licences` |
| [`appendix/H-upstream-proposals.md`](appendix/H-upstream-proposals.md) | The Swing-free upstream agenda (`java.source`, `api.progress`, `openide.awt`, `openide.nodes`, editor SPI, `nbbuild` checks, friend APIs) and the nbfx prototypes that prove each |
| [`appendix/I-language-soundness.md`](appendix/I-language-soundness.md) | The second-implementation test: the four language holes, the `api.language` design and core freeze, the Groovy/PHP/web spikes, and the measured reuse ratio across the other clusters |
| [`../architecture-rules.md`](../architecture-rules.md) | A one-screen **index** of every rule and its normative source (no rule detail — that lives in the plan/appendices) |
| [`archive/README.md`](archive/README.md) | Historical research and earlier plan revisions — **evidence only, not maintained** |
| [`../repo-and-build.md`](../repo-and-build.md) | Build harness, clusters, submodule patches |
| [`../MVP.md`](../MVP.md) | Historical MVP draft (superseded by the plan) |

**Where to start**

1. [`PLAN.md`](PLAN.md) §1–§5 — what nbfx is, the layers, the naming and the API rules.
2. [`PLAN.md`](PLAN.md) §6 — the module you are looking for (catalogue per cluster).
3. [`PLAN.md`](PLAN.md) §10–§11 — the milestone you are in and the workstream to pick.
4. The archived research when you need the NetBeans evidence for a decision.

**Current state.** Milestones and exit criteria are in §10; the workstream branch families are in §11.
Nothing in `archive/` is part of the plan; when the plan and a document disagree, the plan wins.
