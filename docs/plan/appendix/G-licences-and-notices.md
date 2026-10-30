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
# Appendix G — Licences and notices

The tree has root `LICENSE`, `NOTICE`, `THIRDPARTY` and `KEYS` files (added for 0.1.0, [RELEASING.md](../../../RELEASING.md)); `libs.javafx` bundles **JavaFX 27**
into an otherwise Apache-2.0 project, with its licence texts in `libs.javafx/release/legal/javafx/` (unpacked from the pinned SDK, not in git). This appendix fixes the
policy; the check is mechanical (`check-licences`).

## G.1 The components

| Component | Version | Licence | Obligation | Bundled by | Milestone |
|---|---|---|---|---|---|
| Reused NetBeans platform/IDE/Java modules | pinned `0104eeb3da` | Apache-2.0 | keep the upstream `LICENSE`/`NOTICE`; no relicensing | clusters | — |
| **JavaFX / OpenJFX** | **27** (`libs.javafx/javafx-sdk.properties`) | **GPLv2 + Classpath Exception** | ship the licence text + a notice; the CE covers linking, not relicensing; source offer for the FX modules we distribute | `libs.javafx` | M0 |
| JavaFX `javafx.web` / WebKit | — | LGPL-2.1/BSD (WebKit) | **do not bundle** — not needed by the IDE scope (PLAN §1 non-goals) | — | — |
| async-profiler | per decision | Apache-2.0 | NOTICE; native binaries per OS/arch | `libs.asyncprofiler.*` | M2 |
| JLine | per decision | BSD-3-Clause | NOTICE | `libs.jline` | M0 |
| jgit (Git) | reuse NetBeans' jgit modules | EPL-2.0 | prefer the upstream module over a new bundle | `ide.vcs.git` | M1 |
| JaCoCo (`org.jacoco.core`) | reuse NetBeans' shipped module | EPL-2.0 | the open question in §14.3 remains: published nbfx feature vs reuse | `java.tests` | M2 |
| nbfx source | — | Apache-2.0 | the ASF header, the same as Apache NetBeans; checked by `ant rat` | all modules | — |

## G.2 Rules

1. **Every bundled artefact has a `NOTICE` entry** (name, version, licence, upstream URL) and the licence
   text is shipped in the distribution (`nbfx/THIRDPARTY` or the cluster `LICENSE`).
2. **No GPL/LGPL code beyond JavaFX-with-CE.** `javafx.web` and any WebKit components are excluded; a new
   library with a copyleft licence needs an explicit decision here.
3. **Native artefacts** (JavaFX, async-profiler) are pinned by SHA-256 ([PLAN §4 trust](../PLAN.md)) and listed per
   OS/arch; only the platforms in the support matrix ship. They are fetched at build time, never committed, so
   the source release holds no compiled code. The support matrix is macOS aarch64/x64, Windows x64 and Linux
   x64, all in one binary zip whose THIRDPARTY lists exactly its files. Its `javafx.graphics.jar` is the
   union of the four SDKs' jars, a repackaging that THIRDPARTY declares and `release-verify` checks
   against the pinned SDKs ([release-binaries.md](../../release-binaries.md)).
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
