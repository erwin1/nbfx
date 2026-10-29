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
# Plan — the binary release for all platforms

**Status:** steps 1–5 implemented, with one zip for all platforms (§3); the start-up tests of macOS x64, Windows and Linux are open (§5). **Scope:** `libs.javafx`, `release-bin`, `release-verify`, the
licence files and [RELEASING.md](../RELEASING.md). Supported platforms: **macOS aarch64, macOS x64,
Windows x64, Linux x64**.

## 1. Why

The single `nbfx-<version>-bin.zip` bundles one set of JavaFX jars plus native libraries for three
platforms (`nbfx-libs/c.gluonhq.netbeans.nbfx.libs.javafx/release/modules/`, committed to git):

| Path (under `modules/`) | Content | Platform |
|---|---|---|
| `ext/javafx.*.jar`, `ext/jfx.incubator.*.jar` | JavaFX 27 jars | one set for all platforms |
| `lib/*.dylib` | 8 libraries | macOS **arm64 only** |
| `lib/amd64/*.dll` | 55 files (FX + MSVC/UCRT runtime) | Windows x64 |
| `lib/amd64/linux/*.so` | 11 libraries | Linux x64 |

The layout leans on the NetBeans JNI lookup (`lib/<arch>/<os>`, then `lib/<arch>`, then `lib/`). It has these
problems:

1. **Only macOS aarch64 can work.** OpenJFX's `javafx.graphics` differs per platform in its *classes*,
   not only its natives. The bundled jar holds `com/sun/glass/ui/mac/` and no `win` or `gtk` package, so
   Windows and Linux are expected to fail at toolkit start-up even though their natives are present (not yet
   run on either).
2. **macOS x64 is missing.** An `x86_64` lookup falls through to `lib/` and finds arm64 dylibs.
3. **The source release contains binaries.** `release-src` is `git archive`, so the 17 MB of jars, DLLs
   and dylibs end up in `nbfx-<version>-src.tar.gz`. It should contain no compiled code. JavaFX is
   GPLv2+CE, which the ASF places in Category B (binary form only, after review). nbfx is not an ASF project,
   but RELEASING.md follows the ASF procedure.
4. **The binary's licence files don't match its content.** `THIRDPARTY` lists files the zip cannot use
   as shipped. The ASF rule is that an artefact's licence files account for its exact content.

A NetBeans module's class path cannot vary per OS, so one zip needs one `javafx.graphics.jar` that serves
every platform (§3).

## 2. Target

| Artefact | JavaFX | Launchers |
|---|---|---|
| `nbfx-<v>-src.tar.gz` | none (fetched at build time, pinned) | — |
| `nbfx-<v>-bin.zip` | all four platforms | `bin/netbeans`, `bin/netbeans64.exe` |

With `.asc` and `.sha512` each, a release is **6 files**, as before. In the zip, `libs.javafx` holds:

```
modules/ext/javafx.graphics.jar     the union of the four SDKs' jars (§3)
modules/ext/<other jars>            from the Linux x64 SDK
modules/lib/*.dll                   Windows x64 (with the MSVC/UCRT runtime)
modules/lib/*.so                    Linux x64
modules/lib/aarch64/*.dylib         macOS aarch64
modules/lib/x86_64/*.dylib          macOS x64
legal/javafx/                       the union of the SDKs' licence texts
```

`THIRDPARTY` lists exactly these files.

## 3. Decisions

- **Source of JavaFX: the OpenJFX SDK zips** (Gluon's download server), one per platform, pinned by version
  and **SHA-256** ([Appendix G](plan/appendix/G-licences-and-notices.md) rule 3). The SDK has natives as
  separate files and the per-platform licence texts. Maven Central's classifier jars embed the natives in
  the jar, and JavaFX would then unpack them to `~/.openjfx/cache` at run time; they would also mean
  about 24 pins instead of 4.
- **One zip for all platforms, not one per platform.** The per-platform zips were built first (commit
  `f54e91c`) and replaced. One download that runs everywhere, as Apache NetBeans ships, is worth about
  3 MB (≈1.4%) more per download, and the merged jar below. It works because:
  - **`javafx.graphics.jar` merges cleanly.** In the four JavaFX 27 SDKs, every entry the platforms share
    is byte-identical. The rest are disjoint platform packages: Windows 331 entries, Linux 32, macOS 64,
    and 319 OpenGL entries shared by macOS and Linux. The two macOS jars are identical. JavaFX picks its
    platform code at run time, so the union serves all four. The other jars differ between the SDKs only
    by a build timestamp (`VersionInfo`) and compiled CSS (`.bss`) of the same 27+30 build; the Linux
    SDK's are used.
  - **The natives don't collide.** `StandardModule.findLibrary` looks in `modules/lib/`, then
    `modules/lib/<os.arch>/`, then `modules/lib/<os.arch>/<os>/`, and library names differ per OS
    (`glass.dll`, `libglass.so`, `libglass.dylib`). Only the two macOS architectures need their own dir,
    which `os.arch` (`aarch64`, `x86_64`) selects.
  - **The licence texts merge:** same-named texts are identical in all four SDKs.
  - **The cost:** the merged `javafx.graphics.jar` is a repackaging. No upstream file has its checksum.
    `THIRDPARTY` says so and names the four SDKs with their SHA-256, and `release-verify` checks that the
    jar is exactly the union of the pinned SDKs' jars.
- **Builds run once.** Only `libs.javafx`'s files are platform-specific. The modules compile against the
  JavaFX API, which is the same on every platform, so the build uses the build machine's SDK and
  `release-bin` swaps in the universal set.
- **Dev loop:** `ant bootstrap` fetches the **build machine's** SDK, so `ant run` keeps working.
  `-Dnbfx.javafx.platform=<p>` overrides it.
- **Launchers:** `bin/netbeans` and `bin/netbeans64.exe`, without the 32-bit `netbeans.exe` (there is no
  32-bit JDK 25). The import-class override in `etc/netbeans.conf` (commit `1f3468f`) serves both.

## 4. Steps

Each step builds and is reviewable on its own.

### Step 1 — JavaFX out of git, fetched and pinned (done)

- `nbfx-libs/c.gluonhq.netbeans.nbfx.libs.javafx/javafx-sdk.properties` pins the SDK: the version
  (replacing `javafx.version`) and, per platform, the SDK's platform id and the zip's SHA-256 (the values
  Gluon publishes as `<zip>.sha256`).
- Target `javafx-fetch` in `build.xml` (`bootstrap` runs it before `overlay`), via the macro
  `nbfx-javafx-unpack`, which step 2 reuses with a staging dir:
  - downloads the SDK to `~/.hgexternalcache/<sha256>-<zip>`, where the NetBeans build keeps its
    downloads. It survives `ant clean`, and CI already caches it. The SHA-256 is checked like `-rat-jar`'s;
  - unpacks the six bundled jars to `release/modules/ext/`, the natives flat to `release/modules/lib/`
    (without media/WebKit), and the six modules' licence texts to `release/legal/javafx/`. It touches the
    files so the harness copies them over an older build;
  - skips all of it when `build/javafx-sdk.stamp` records the same platform and checksum;
  - mirrors `modules/lib/` and `legal/javafx/` into an existing built cluster, because the harness never
    deletes there. This also removes the old `lib/amd64`, `lib/amd64/linux` layout from earlier builds.
- `nbfx.javafx.platform` defaults to the build machine and fails on anything else. Override it with
  `-Dnbfx.javafx.platform=<id>`.
- The committed `release/modules/` and `release/legal/javafx/` are gone from git and git-ignored. They
  were the Maven Central jars with the natives taken out, the same JavaFX 27+30 build as the SDK.
  `project.xml` and patch `0012` are unchanged.
- The six modules whose tests put `libs.javafx/release/modules/lib` on `java.library.path` now get the
  build machine's natives there, including on CI (Linux).
- **Checked:** all four platforms unpack (natives of the right format, Windows with its 41
  `api-ms-win-*` DLLs as before); an unknown id fails; a second run is skipped; `ant bootstrap build`
  and `ant run` on macOS aarch64 start without module errors.

### Step 2 — the universal zip (done)

`release-bin` (`release/release.xml`):

1. builds once, as before, including `-dist-launcher` and the LICENSE/NOTICE additions;
2. runs upstream `zip-cluster-config` once (`build/release/tmp/build.zip`);
3. assembles `libs.javafx`'s universal part with the macro `nbfx-javafx-universal`:
   - unpacks the four SDKs with step 1's `nbfx-javafx-unpack`;
   - unpacks the three distinct `javafx.graphics.jar`s over each other and zips the result;
   - takes the Linux SDK's other jars;
   - puts the natives in the layout of §2;
   - merges the licence texts;
4. regenerates `libs.javafx`'s `update_tracking` file, which lists every file with its CRC-32, with
   `release/tools/UpdateTracking.java`;
5. writes `nbfx-<v>-bin.zip`:
   - the build's zip without `nbfx-libs/modules/{ext,lib}/`, `legal/javafx/`, that tracking file and
     `bin/netbeans.exe`. `zipfileset src=` keeps the entries' modes, so `bin/netbeans` stays executable;
   - plus the universal part, the tracking file, `THIRDPARTY` and `KEYS`;
6. deletes the temporary files and any `nbfx-<v>-bin*.zip` of an earlier run, including per-platform ones.

### Step 3 — licence files (done)

- The zip's `THIRDPARTY` comes from `release/bin-THIRDPARTY.txt`:
  - the four SDKs with their URL and SHA-256;
  - the sorted list of the JavaFX files in the zip;
  - how `javafx.graphics.jar` was merged;
  - the MSVC/UCRT runtime section.
- The `LICENSE` OpenJFX addendum (`release/bin-LICENSE-nbfx.txt`) is unchanged.
- The root `THIRDPARTY`, which goes into the source release, says that the source bundles nothing and the
  build downloads JavaFX.

### Step 4 — verify (done)

`release-verify` (also run by `release-verify.yml`):

- expects exactly the 6 files, each by name; any other file fails, such as a per-platform zip;
- the source release: Rat, and no `.jar`/`.class`/`.dll`/`.dylib`/`.so`/`.exe`;
- the binary:
  - the licence files and the OpenJFX mention in `LICENSE`;
  - no `javafx.web`;
  - `release/tools/CheckBinary.java`, run against the four pinned SDKs, which `release-verify` unpacks
    itself rather than trusting the build. It checks:
    - every native has the format and CPU of its place (`*.dll` PE x64 and `*.so` ELF x86-64 in `lib/`,
      Mach-O arm64 in `lib/aarch64/`, Mach-O x86_64 in `lib/x86_64/`, from the file headers), all four
      groups are there and nothing else is;
    - `javafx.graphics.jar` is exactly the union of the SDKs' jars (every entry, same CRC-32 and size, and
      nothing else), with the mac, win and gtk glass toolkits;
    - `bin/` holds `netbeans` (executable) and `netbeans64.exe`, nothing else.

That catches problem 1 for good. A copy of the zip with the Linux SDK's `javafx.graphics.jar` fails with
397 errors, the missing mac and win toolkits among them.

### Step 5 — documentation (done)

- [RELEASING.md](../RELEASING.md): the artefact table, the `release-bin`/`release-verify` rows and a
  start-up test per platform. `release/release-notes.md` and the release checklist issue template follow.
- [Appendix G](plan/appendix/G-licences-and-notices.md) rule 3 covers JavaFX: pinned, fetched at build
  time, the support matrix, the merged jar. **Open:** recording the review of JavaFX's GPLv2+CE as a
  decision is for the project to do.
- `release-draft.yml` and `release-verify.yml` already handle every `nbfx-*` file. No change needed.

## 5. Testing

| Platform | How |
|---|---|
| macOS aarch64 | locally: unzip, start with an empty `--userdir`, check `messages.log`. **Done** for the universal zip, natives from `lib/aarch64/` |
| macOS x64 | locally under Rosetta 2 with an x64 JDK 25 (`arch -x86_64`) |
| Windows x64 | the test release on a Windows machine |
| Linux x64 | the test release, or an `linux/amd64` container with Xvfb (slow under emulation) |

For each one, check `var/imported` and a log without "was needed and not found", `NoClassDefFoundError`
or glass/prism errors.

**macOS:** test the zip **downloaded through a browser**, not the local build output. The quarantine
attribute and Gatekeeper treat the bundled dylibs differently from a locally built tree.

## 6. Out of scope / open

- macOS `.app`/`.dmg`, signing and notarization, and Windows installers.
- Linux aarch64 and Windows arm64: another SDK each, and `lib/` would need per-architecture dirs for them
  too.
- JavaFX 27.0.1 is out (same download server). Moving to it means changing `javafx-sdk.properties`,
  which is a separate change from this one.
