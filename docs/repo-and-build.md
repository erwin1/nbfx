# nbfx repository and build

How the nbfx clusters are hosted and built: **apache/netbeans as a git submodule**, a small patch
set applied to it, an **own cluster configuration** generated from the submodule, and an **Ant**
harness for a cross-platform build. Companion to [`plan/PLAN.md`](plan/PLAN.md) (the canonical plan).

## 1. Layout

```
nbfx/
├── build.xml                     # the Ant harness (all targets)
├── nbfx.properties               # configurable properties (pin, overlay mode, clusters)
├── nbfx-local.properties         # optional local overrides (not committed)
├── .gitmodules                   # netbeans submodule
├── netbeans/                     # submodule → apache/netbeans @ pinned SHA
├── nbfx-platform/                # 13 FX modules (platform/ replacements)
├── nbfx-ide/                     # 19 FX modules (ide/ replacements + app shell)
├── nbfx-java/                    #  6 FX modules (java/ + extide/ project types)
├── cluster/
│   └── nbfx-cluster.properties.fragment   # nb.cluster.nbfx-* + clusters.config.nbfx.list
├── patches/                      # upstream patch set (see §4)
├── check-purity.xml              # guardrails (source/deps/bundled classes/packaging)
├── README.md
└── docs/
    ├── plan/PLAN.md              # the canonical plan (docs/plan/)
    └── repo-and-build.md         # this document
```

The nbfx sources are the single source of truth; the `netbeans/` checkout is consumed read-mostly.
History for the FX modules was migrated from `gluon-netbeans-fx` (`netbeansfx-sep26`); the provenance
is recorded in `nbfx.properties` (`nbfx.source.*`).

## 2. Why a submodule + overlay

Each nbfx module's `build.xml` imports `../../nbbuild/templates/projectized.xml` and its
`arch.xml`/`apichanges.xml` reference `../../nbbuild/...`, so the modules must sit exactly one
directory under a NetBeans source root whose sibling is `nbbuild`.

- Ant's `dirset`/`fileset` **drop patterns containing `..`**, so `nb.cluster.nbfx-platform.dir=../nbfx-platform`
  cannot work.
- A **symlinked** cluster dir *is* discovered, and the `../../nbbuild` import then resolves lexically
  to `netbeans/nbbuild`.

Therefore the harness makes the cluster dirs visible under `netbeans/` by an **overlay**:

- `nbfx.overlay=sync` (default): `<sync>` mirrors `nbfx-platform/`, `nbfx-ide/`, `nbfx-java/` into
  `netbeans/`, excluding `build/`. Cross-platform.
- `nbfx.overlay=symlink`: `netbeans/nbfx-*` are symlinks to the real dirs. Unix-only, fastest dev loop.

The overlay paths are written to the submodule's `info/exclude`, so `git status` in the submodule stays
clean; `ant unoverlay` removes them (via `git clean`, which unlinks symlinks rather than following
them).

## 3. Own cluster configuration

`cluster/nbfx-cluster.properties.fragment` holds only the nbfx additions. `ant cluster-config`:

1. restores the pristine submodule `nbbuild/cluster.properties` (so re-runs are idempotent),
2. registers the nbfx cluster dirs in `clusters.config.full.list` (the harness's `ModuleListParser`
   only scans the `FOREST` dirs plus the dirs of clusters listed there — without this the
   `all-<module>` targets for the nbfx clusters are never created),
3. drops `core.windows` from the platform cluster (the pure-JavaFX window-system removal, M9.2/M9.4),
4. appends the fragment (the three `nb.cluster.nbfx-*` definitions and `clusters.config.nbfx.list`),
5. writes the result to `build/cluster.properties` **and installs it as the submodule's
   `nbbuild/cluster.properties`**, because several harness targets read that path directly
   (`-define-cluster-path` builds `cluster.path` from it; packaging targets too). An external
   `-Dclusters.list.file` alone is not sufficient.

The fragment defines the three clusters (dependency direction `platform → nbfx-platform → nbfx-ide →
nbfx-java`) and the lean `clusters.config.nbfx.list`:

```
nb.cluster.nbfx-platform.dir=nbfx-platform   depends nb.cluster.platform
nb.cluster.nbfx-ide.dir=nbfx-ide             depends nb.cluster.nbfx-platform, ide, java
nb.cluster.nbfx-java.dir=nbfx-java           depends nb.cluster.nbfx-ide, java, extide, apisupport

clusters.config.nbfx.list = platform, nbfx-platform, nbfx-ide, nbfx-java, java, ide, extide, apisupport
```

The nbfx clusters are reachable as a normal cluster set via `-Dcluster.config=nbfx`; the nbfx
clusters themselves are not added to `clusters.config.java`/`release`. Registering the dirs in
`full.list` means the dev `full` config (`-Dcluster.config=full`) also includes the nbfx clusters.
Because the `core.windows` removal edits the shared `nb.cluster.platform`, any config built from this
repository also drops the Swing window system — this repository builds the pure-FX app, not the stock
Swing IDE. `ant clean` restores the submodule's `cluster.properties`.

## 4. Patch set

Applied to the submodule working tree by `ant patches` (canonical copies in `patches/`). Three
declarations are compile-critical (internal-package access); the rest are build/packaging/launcher
hooks.

| Patch | File | Change |
|---|---|---|
| `0001-friend-core-startup` | `platform/core.startup/nbproject/project.xml` | `<friend>com.gluonhq.netbeans.nbfx.launcher</friend>` (RunLevel SPI) |
| `0002-friend-maven` | `java/maven/nbproject/project.xml` | `<friend>com.gluonhq.netbeans.nbfx.project.ui.maven</friend>` |
| `0003-friend-maven-embedder` | `java/maven.embedder/nbproject/project.xml` | `<friend>com.gluonhq.netbeans.nbfx.project.ui.maven</friend>` |
| `0010-launcher-clusters` | `nb/ide.launcher/netbeans.clusters` | add `nbfx-platform`, `nbfx-ide`, `nbfx-java` |
| `0011-tryme-clusters` | `nbbuild/build.xml` | `tryme` passes `--clusters` from `clusters.config.nbfx.list` + JMS flags |
| `0012-ignored-binaries` | `nbbuild/antsrc/…/extlibs/ignored-binaries` | allow the bundled JavaFX jars **and native libs** |
| `0013-rat-exclusions` | `nbbuild/rat-exclusions.txt` | exclude the nbfx dirs from Rat |
| `0020-javadoc` (optional) | `nbbuild/build.properties`, `nbbuild/javadoctools/*` | `com.gluonhq.*` javadoc entries |

Patch hygiene: the submodule is pinned; `ant patches` applies with `git apply --3way` and stamps the
revision. `ant patches-check` verifies every patch still applies, and `ant update-upstream` bumps the
revision and re-applies. `-Dforce.patches=true` forces re-application.

## 5. Build commands

```sh
ant bootstrap      # submodule + patches + cluster config + overlay
ant build          # build the 'nbfx' cluster config
ant run            # launch the FX app on the lean cluster set
ant check-purity   # source/deps/bundled-class/packaging guardrails
ant update-upstream -Dnbfx.submodule.commit=<sha>
ant clean
```

Equivalent low-level form (what `ant build` runs in the submodule):

```sh
cd netbeans/nbbuild
ant -Dcluster.config=nbfx -Dclusters.list.file="$PWD/../../build/cluster.properties" build
```

`ant help` lists all targets; `ant print-config` prints the effective configuration. Any property can
be overridden with `-Dname=value` (e.g. `-Dnbfx.overlay=symlink`).

`build` and `run` depend on `bootstrap`, so they re-run the overlay first: edits in the outer
`nbfx-platform/`, `nbfx-ide/` and `nbfx-java/` directories are synced into `netbeans/` automatically on
every build.

## 6. Upstream changes

Because the direct upstream dependency is a moving target, the model is:

- **Pin** the submodule to a tested revision (`nbfx.submodule.commit`).
- **Re-verify** the patch set after a bump (`ant patches-check`); the friend declarations are stable
  insertions, while `0011-tryme-clusters` is the most context-sensitive.
- **Regenerate** the own cluster config at every build (it is never committed), so upstream changes to
  `nbbuild/cluster.properties` are picked up automatically.

`update-upstream` performs: fetch → `reset --hard` (discard the applied patches + generated
`cluster.properties`) → checkout the new revision → drop the patch stamps → tear down the overlay →
`patches-check` → `bootstrap`. Afterwards update `nbfx.submodule.commit` in `nbfx.properties` and the
submodule gitlink, and commit. `submodule` only runs `git submodule update --init` when the submodule
is not yet initialised, so re-running it never clobbers the applied patches.

The nbfx sources themselves now live only in this repository (migrated with history from
`gluon-netbeans-fx`; provenance is recorded in `nbfx.properties`). There is no separate upstream to
sync from — only the `netbeans` submodule pin is updated over time.

## 7. Verification

- `ant bootstrap build` on a clean checkout produces `netbeans/nbbuild/netbeans/nbfx-{platform,ide,java}`.
- `ant run` starts the FX app on the lean set; no `core.windows` jar ships in the nbfx clusters.
- `ant check-purity` reports `0/0/0/0` (sources, deps, bundled classes, packaging).
- Session/Recent Projects survive (preferences paths are unaffected by the layout).

## 8. Known constraints

- **Bundled JavaFX runtime.** `nbfx-platform/c.gluonhq.netbeans.nbfx.lib.javafx` commits the JavaFX 27
  runtime (~16 MB): `release/modules/ext/*.jar` plus natives for macOS, Windows x64 (`amd64/`) and
  Linux x64 (`amd64/linux/`). There is **no arm64** build yet. The `0012` patch keeps these out of the
  external-binaries licence check; migrating them to the `external/` download mechanism is a follow-up.
- **`lib.javafx` cluster placement.** Several modules reference
  `../c.gluonhq.netbeans.nbfx.lib.javafx/release/modules/lib` via `test-unit-sys-prop.java.library.path`,
  so `lib.javafx` must stay in `nbfx-platform`.
- **Compatibility.** The nbfx sources were forward-ported from the `gluon-netbeans-fx` base; a bump to a
  newer NetBeans revision may require source fixes in addition to patch rebasing.
