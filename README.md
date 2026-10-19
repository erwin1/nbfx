# nbfx — a pure-JavaFX IDE frontend on the NetBeans runtime

This repository hosts the **nbfx** work: a **pure-JavaFX** re-implementation of the NetBeans IDE
shell. It reuses the headless NetBeans platform/IDE services (project model, parsing, java.source,
indexing, autoupdate, JPDA, templates, …) and replaces every Swing view/component with a JavaFX one.
There is no NetBeans Swing window: `core.windows` is dropped from the nbfx clusters and the shell is
owned by the JavaFX application.

The full roadmap, milestones and history live in **[`docs/MVP.md`](docs/MVP.md)** (the canonical
plan); the repository layout, upstream patch set and build harness are documented in
**[`docs/repo-and-build.md`](docs/repo-and-build.md)**. This file is the short project overview.

## Repository

This repository hosts the nbfx clusters and consumes apache/netbeans as a **git submodule**
(`netbeans/`). The build is driven by the Ant harness [`build.xml`](build.xml) (no shell scripts); see
[`docs/repo-and-build.md`](docs/repo-and-build.md).

## Clusters

The FX modules are split into three clusters that parallel the NetBeans clusters, so a new FX module
goes next to the NetBeans module it replaces. Code-name-bases are unchanged; only the cluster/dir
layout differs. Dependency direction is `platform → ide → java`.

| Cluster | Dir | Contents |
|---|---|---|
| `nb.cluster.nbfx-platform` | `nbfx-platform/` | FX replacements for platform-level modules: `annotations`, `lib.javafx`, `api`, `context`, `docking`, `windows`, `statusbar`, `menus`, `macos`, `output`, `options`, `wizard`, `actions`. |
| `nb.cluster.nbfx-ide` | `nbfx-ide/` | FX replacements for ide-level modules + the app shell: `editor`, `editor.actions`, `file.actions`, `findinprojects`, `findusages`, `propertysheet`, `structure`, `project`, `project.ui`, `project.ui.api`, `project.actions`, `project.context`, `project.customizer`, `plugins`, `vcs`, `debug`, `run`, `ui`, `launcher`. |
| `nb.cluster.nbfx-java` | `nbfx-java/` | FX replacements for java/extide-level modules: `project.ui.java`, `structure.java`, `project.ui.maven`, `project.ui.gradle`, `project.ui.ant`, `build.ant`. |

## Build and test

The nbfx clusters are built on top of an **apache/netbeans git submodule**. The Ant harness at the
repository root initialises the submodule, applies the required upstream patches, generates the
cluster configuration and runs the build:

```sh
ant bootstrap      # submodule + upstream patches + cluster config + overlay
ant build          # build the 'nbfx' cluster config
ant run            # launch the FX app on the lean cluster set
ant module -Dmodule=<cluster>/<module>   # rebuild one module into the dist (seconds)
ant check-purity   # forbidden Swing/AWT imports & deps, bundled classes, no core.windows jar
ant update-upstream -Dnbfx.submodule.commit=<sha>
ant clean
```

See [`docs/repo-and-build.md`](docs/repo-and-build.md) for the layout, the upstream patch set and how
the cluster configuration is generated. Unit tests of a single module:

```sh
ant -f netbeans/<cluster-dir>/<module-dir>/build.xml test-unit \
    -Dnetbeans.dest.dir="$PWD/netbeans/nbbuild/netbeans"
```

## Architecture in a nutshell

- **Registration is layer-driven.** Annotations on the owning class emit `NbFx/*` layer entries; the
  shell builds menus, tool bars, views, context menus, status elements, wizards and options from that
  layer (`@FxActionReference` references an action by id; `mimeType` restricts a Navigator context
  menu entry to a content type).
- **Global action context = the active component's lookup.** `NbfxGlobalContextProvider` (a
  `ContextGlobalProvider`, `position = 100`) feeds the nbfx selection into
  `Utilities.actionsGlobalContext()` via a stable `Lookups.proxy`. The shell publishes the active
  **component** — a tool view *or* an editor (`ActiveComponentContext`) — from the focused pane's
  selected tab; editors win over the follower-view fallback.
- **Navigator is service-based.** Structure/Navigator panels implement `FxStructurePanel`
  (`contentTypes()`), registered with `@ServiceProvider` and resolved by the active file's MIME;
  `nbfx.structure.java` ports the full Java Navigator (members, bean patterns, go-to).
- **Views observe the open-project model.** Navigators are plain `ViewProvider`s (the
  `@FxViewRegistration(navigator = true)` flag is the marker) that observe `ProjectRegistry` and
  build/drop their trees; they publish their own selection through `ViewProvider.getLookup()`.
- **Purity.** `nbfx` sources/JARs must not import Swing/AWT/legacy-window packages; the macOS menu and
  one headless accessibility SPI type are the only quarantined exceptions. `core.windows` is excluded
  from the clusters and a packaging guard (in `check-purity.xml`) asserts no `core.windows` jar ships
  in the three nbfx clusters.

## Known caveats

- GUI behaviour is verified manually with `ant tryme`; most modules have no automated GUI tests.
- On macOS, running with `-Djava.awt.headless=true` hits a known `HeadlessException` in the launcher
  (an environment artifact, not a code path).
