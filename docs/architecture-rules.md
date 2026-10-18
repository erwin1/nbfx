# nbfx architecture rules — index

**The rules are normative in the plan.** This file is a one-screen index: it names each rule and points at
its normative source, so there is exactly one place to change a rule. Do not restate rule detail here — that
is how three documents start disagreeing.

| Rule | Normative source |
|---|---|
| Layers/clusters; downward nbfx edges only; siblings `java` → `javafx`; upstream reuse edges are separate | [`plan/PLAN.md`](plan/PLAN.md) §2, Decision 1/14 |
| No split packages; model modules toolkit-free; one semantic boundary per language | [`plan/PLAN.md`](plan/PLAN.md) §2, Appendix [B](plan/appendix/B-java-source-isolation.md) |
| Module/package/type naming; no `Fx*`/`Impl` in public API; the rename table | [`plan/PLAN.md`](plan/PLAN.md) §3, Appendix [C](plan/appendix/C-naming-and-rename-table.md) |
| Layer paths: one owner (`api.layer.LayerPaths`), writer/reader contract test | [`plan/PLAN.md`](plan/PLAN.md) §3.3, Appendix [C.3](plan/appendix/C-naming-and-rename-table.md) |
| API tiers (T0/T1/T2-FX/M/L) and their allowed types; exemptions | Appendix [A](plan/appendix/A-toolkit-policy.md), [`plan/PLAN.md`](plan/PLAN.md) §4 |
| Governance: explicit `<public-packages>`, `arch.xml`, `apichanges`, spec version 1.0 + bump rule, sigtest | [`plan/PLAN.md`](plan/PLAN.md) §4 |
| Plugin contract and trust; the plugin allowlist | [`plan/PLAN.md`](plan/PLAN.md) §4, Appendix [E.6](plan/appendix/E-contracts.md) |
| UI architecture: one node model (explorer panels only), model-first panels, concrete FX factories (no generic `C`), uniform async | [`plan/PLAN.md`](plan/PLAN.md) §5, Appendix [F](plan/appendix/F-node-and-view-migration.md), Appendix [E.7](plan/appendix/E-contracts.md) |
| The Java semantic boundary (`java.source` confined, `java.model` bridge, no Swing `Document`) | Appendix [B](plan/appendix/B-java-source-isolation.md) |
| Threading per SPI, disposal/ownership + leak test, accessibility, error/logging, the `Lookup` rule | Appendix [E](plan/appendix/E-contracts.md) |
| Reuse / bridge / replace / drop per upstream module; the retirement matrix; the compiler choice | Appendix [D](plan/appendix/D-bridge-and-retirement-matrix.md) |
| Data compatibility (project properties, project-type ids, nb-javac, preferences, formats) | [`plan/PLAN.md`](plan/PLAN.md) §8 |
| Guardrails (implementation of every rule above) | [`plan/PLAN.md`](plan/PLAN.md) §9 |
| Licences and notices | Appendix [G](plan/appendix/G-licences-and-notices.md) |
| Process: one step = one branch; naming steps are source-compatible; upstream-first; retirement is the deliverable | [`plan/PLAN.md`](plan/PLAN.md) §11, §13 |
| Upstream agenda: additive-only, new module + compat wrapper, `ModuleAutoDeps`, `sigtest`-frozen, prototyped in nbfx, fallback recorded, one accepted change per milestone | Appendix [H](plan/appendix/H-upstream-proposals.md) |
| Language registry: `api.language` (M) is the one registry, providers scoped by `LanguageId`, decoration is a provider not a core branch, neutral `ElementRef` vs `java.elements`; core freeze | Appendix [I](plan/appendix/I-language-soundness.md) |

If a rule here and the plan disagree, the plan wins; if the plan and an appendix disagree, the plan states the
rule and the appendix the detail — fix whichever is wrong and commit both.
