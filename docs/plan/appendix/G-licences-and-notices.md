# Appendix G — Licences and notices

The tree has **no `LICENSE`/`NOTICE` files for the nbfx distribution** and `lib.javafx` bundles **JavaFX 27**
into an otherwise Apache-2.0 project. That is a release blocker, not a nicety. This appendix fixes the
policy; the check is mechanical (`check-licences`).

## G.1 The components

| Component | Version | Licence | Obligation | Bundled by | Milestone |
|---|---|---|---|---|---|
| Reused NetBeans platform/IDE/Java modules | pinned `0104eeb3da` | Apache-2.0 | keep the upstream `LICENSE`/`NOTICE`; no relicensing | clusters | — |
| **JavaFX / OpenJFX** | **27** (`lib.javafx/javafx.version`) | **GPLv2 + Classpath Exception** | ship the licence text + a notice; the CE covers linking, not relicensing; source offer for the FX modules we distribute | `libs.javafx` | M0 |
| JavaFX `javafx.web` / WebKit | — | LGPL-2.1/BSD (WebKit) | **do not bundle** — not needed by the IDE scope (PLAN §1 non-goals) | — | — |
| async-profiler | per decision | Apache-2.0 | NOTICE; native binaries per OS/arch | `libs.asyncprofiler.*` | M2 |
| JLine | per decision | BSD-3-Clause | NOTICE | `libs.jline` | M0 |
| jgit (Git) | reuse NetBeans' jgit modules | EPL-2.0 | prefer the upstream module over a new bundle | `ide.vcs.git` | M1 |
| JaCoCo (`org.jacoco.core`) | reuse NetBeans' shipped module | EPL-2.0 | the open question in §14.3 remains: published nbfx feature vs reuse | `java.tests` | M2 |
| nbfx source | — | Apache-2.0 | same header as upstream | all modules | — |

## G.2 Rules

1. **Every bundled artefact has a `NOTICE` entry** (name, version, licence, upstream URL) and the licence
   text is shipped in the distribution (`nbfx/THIRDPARTY` or the cluster `LICENSE`).
2. **No GPL/LGPL code beyond JavaFX-with-CE.** `javafx.web` and any WebKit components are excluded; a new
   library with a copyleft licence needs an explicit decision here.
3. **Native artefacts** (async-profiler) are pinned by SHA-256 ([PLAN §4 trust](../PLAN.md)) and listed per
   OS/arch; only the platforms in the support matrix ship.
4. **`libs.*` modules are the only bundling point**; a Copyleft library must never end up inside an `M`
   module's jar (the bytecode guard already forbids foreign packages in M).
5. **The plugin ecosystem** (`api.plugins`) states the licence of the API (Apache-2.0) and that a plugin's
   own licence is its business; the plugin manager displays it.
6. **A language runtime ships as `libs.<language>` with a NOTICE entry** (the upstream `groovy/libs.groovy`
   pattern); an interpreter or agent binary (PHP, async-profiler) is listed per OS/arch with a SHA-256
   ([Appendix I.7](I-language-soundness.md)).

## G.3 The check

`check-licences` (new, PLAN §9): for every file under the built clusters' `modules/`, `lib/` and native
directories, assert a `NOTICE` entry exists; assert `javafx.web` is absent; assert the distribution contains
`LICENSE` and `THIRDPARTY`. Green on day one by adding the two files and the JavaFX entry (the only current
bundle).
