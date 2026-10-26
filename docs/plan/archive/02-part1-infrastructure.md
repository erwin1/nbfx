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
# 02 — Project infrastructure (M1/M2 topic detail)

> Topic detail for the project-model/UI and Java-tooling milestones (see
> [`26-roadmap.md`](26-roadmap.md) and [`27-layer-module-map.md`](27-layer-module-map.md)). Target API
> names are capability names; `Fx*` names in the code inventory are current classes (naming: doc
> [`32`](32-api-tiers-and-plugin-contract.md), mapping: doc [`31`](31-front-end-apis.md)).

Goal: bring the **existing** project types and their customizers/actions/templates to full parity.
Order: project types → Java Platforms → wizard framework + templates → customizers → actions →
activation → run/debug configs → Ant runtime.

Supported types: **J2SE Ant** (`org.netbeans.modules.java.j2seproject`), **apisupport module**,
**apisupport suite**, **Maven**, **Gradle**.

---

## Project types parity

For every supported type, match the original: recognition, project files / *Important Files*, icons,
opened hook, problems, and the per-type context-menu action set (see Build actions / Run/Debug
configurations). Audit each against the original project type.

## Project model — `FxProject` (headless, nbfx-native)

Today nbfx **reuses the stock NetBeans model**: it ships the stock project-type modules
(`nb.cluster.java/extide/apisupport`), resolves projects with `ProjectManager.findProject(root)` (stock
`Project`s) and only **bridges** to the platform to run the open hook/register source roots through
`OpenProjects.open(...)` (`ProjectRegistryImpl.openInPlatform`). `OpenProjects` is the one
`projectuiapi.base` dependency the purity guard currently exempts.

`FxProject` is a headless, nbfx-native implementation of the **same contract**
(`org.netbeans.api.project.Project` + `ProjectFactory`), so the platform services nbfx keeps
(`parsing.indexing`, `java.source`, `api.java.classpath`, editor completion) behave exactly as in stock
NetBeans — without the Swing project-type UI.

Comparison with default NetBeans:

| Aspect | Default NetBeans | nbfx with `FxProject` |
|---|---|---|
| Model owner | each type module (`NbMavenProject`, `J2SEProject`, …) | one nbfx model module (`nbfx.project`): `FxProject` + factory |
| Registration | `@ServiceProvider(service=ProjectFactory)` / `@ProjectServiceProvider` + `Projects/<type>/*` layer | `@FxProjectTypeRegistration` + `NbFx/Projects/<type>/*` layer |
| UI SPIs | `LogicalViewProvider`, `NodeFactory`, `ProjectCustomizer`, `CommonProjectActions` | none — FX tree, FXML customizers, `CommandsProvider`/`ActionIds` |
| "Open" state | `OpenProjects` (`projectuiapi.base`) | nbfx `ProjectRegistry`/`OpenProject`; no `OpenProjects` bridge |
| Type-aware templates | `RecommendedTemplates`/`PrivilegedTemplates` read by the Swing chooser | same annotations, read by the FX chooser via `Project`/`Sources` |
| Swing coupling | type modules pull `nodes`/`awt`/`dialogs` | model is UI-free (core-only chain) |

**What stays identical**: the contract (`Project`, `ProjectManager`, `Sources`, `ClassPathProvider`) and
template creation (`api.templates`). Build/run actions do not change — `FxProject.ActionProvider`
delegates to the existing nbfx commands, so one implementation serves both front ends.

- **Module**: `nbfx.project` (model, UI-free; currently the empty placeholder after the project
  modularity refactoring), deps `org.netbeans.modules.projectapi` + `org.netbeans.api.java.classpath`
  (+ core) — movable to NetBeans core (see `docs/architecture-rules.md`).
- **SPIs to implement**: `Project`/`ProjectFactory`, `ProjectInformation`, `Sources`,
  `ClassPathProvider`, `ActionProvider` (delegate to the nbfx commands), `ProjectOpenedHook` (indexer
  roots), `RecommendedTemplates`/`PrivilegedTemplates`; later `ProjectProblemsProvider`,
  `SubprojectProvider`. **Not** `LogicalViewProvider`/`NodeFactory`/`ProjectCustomizer` (FX tree +
  FXML customizers instead).
- **Type detection** per kind (Maven `pom.xml`, Gradle `build.gradle`, Ant/J2SE `nbproject`), mirroring
  today's `ProjectKindProvider`.
- **Why**: drop the `projectuiapi.base` bridge and the stock Swing project-type modules from the
  runtime while keeping indexing/classpath/completion; give New File/New Project project-type context.
- **Acceptance**: `ProjectManager.findProject(dir)` returns an `FxProject`; opening a Maven/Gradle/Ant
  project indexes and completes with no `OpenProjects` call; `check-purity` passes with the
  `projectuiapi.base` exemption removed.
- **Depends**: none (independent), but most valuable before project-type-aware template filtering and
  before the stock project-type modules are dropped.

## Java Platforms (prerequisite)

- Reuse **headless** `java.platform` (`JavaPlatformManager`) + `java.j2seplatform` (platform storage in
  `config/Services/Platforms/…`, `platform.ant.name`, sources/javadoc) — parallel to `java.platform` in
  the `java` cluster; the module is `c.gluonhq.netbeans.nbfx.java.platform` (nbfx-java).
- **"Java Platforms" options panel** (`OptionsPanelController`): add (JDK home picker)/remove/rename,
  sources/javadoc, and a **JavaFX-capable** marker + JavaFX home (JDK-provided / separate OpenJFX SDK).
- **JavaFX runtime resolution** per platform (replaces `jfxrt.jar`): JDK8 `jfxrt.jar`, JDK-with-JavaFX
  modules, or OpenJFX SDK → build the JavaFX **module path** (`--module-path … --add-modules
  javafx.controls,javafx.fxml,…`) or classpath for a JavaFX-enabled JDK.
- **Platform filter** in the JavaFX project customizer (port of `JFXProjectPlatformFilter`).

## Wizard framework and templates

### Wizard framework (mirror `org.openide.WizardDescriptor`)

nbfx's framework is minimal (`Wizard.getPages()`, `WizardPanel` with `getComponent`/`isValid`,
`WizardDialog` with Back/Next/Finish; current classes carry the `Fx` prefix until `refactor/api-naming`).
The New Project / New File wizards (and templates) need the original's features (see
[`01-research.md`](01-research.md) §6). Build a shared wizard framework first:

| Original (`WizardDescriptor`) | nbfx today | Target framework |
|---|---|---|
| `Panel<Data>`: `getComponent`, `isValid`, `readSettings`, `storeSettings`, `getHelp` | `getComponent`, `isValid` (+ `getTitle`) | add `readSettings`/`storeSettings`, help |
| `Iterator<Data>` (dynamic `current/hasNext/hasPrevious/next/previous`) + `ArrayIterator` | fixed `List<WizardPanel>` | `WizardIterator` + `ArrayWizardIterator` |
| settings bag (`putProperty`/`getProperty`, `Lookup.Provider`); message/step/image/help props | none | `WizardDescriptor<Data>` with a properties bag + `Lookup`; messages + step list + help |
| `InstantiatingIterator.instantiate()` → `Set`; async/progress variants | `finish()` (void) | `instantiate()` returning created files/objects; async/progress |
| `TemplateWizard` (template `FileObject` + target folder + iterator) | none | `TemplateWizard` on the framework |

Registration stays `@WizardRegistration` / `WizardRegistry` (current: `@FxWizardRegistration`); the
customizer dialog is separate.

### Template infrastructure (reuse existing NetBeans templates)

**All existing templates are already registered** (`@TemplateRegistration` + the `Templates` layer),
merged at runtime into the session `Templates` filesystem by the clusters nbfx ships. We **enumerate
and instantiate** them:

- **Discovery (headless)**: walk `FileUtil.getConfigFile("Templates")`; folders are categories, data
  files with `template=true` are templates. Read `displayName`, `templateCategory`, `position`,
  `iconBase`, `templateWizardURL`, `instantiatingIterator`, `dataObjectClass`,
  `createFromTemplateHandler`.
- **Creation (headless)**: `DataObject.createFromTemplate(…)` (`openide.loaders`) or `FileBuilder` /
  `CreateFromTemplateImpl` (`platform/api.templates`) — handles encoding, `package`/`name`
  substitution and any `CreateFromTemplateHandler`.
- **FX wizard UI (ours)**: the chooser + a target/parameters page, replacing the Swing `TemplateWizard`
  / `Templates.buildSimpleTargetChooser`; project-type-aware filtering via `PrivilegedTemplates` /
  `RecommendedTemplates` (`ide/projectuiapi.base`) and the target project's source groups.
- **Custom-iterator templates** (`NewJavaFileWizardIterator`, JPA/JSF/forms…): port their parameter
  panels to FX or fall back to a generic target+name page (`createFromTemplate` with defaults). Most
  Java/XML/Other templates need only the generic page.
- **New templates** (nbfx-native, or JavaFX in Part 2) register the same way.

Dependencies: `platform/api.templates` (headless) + `openide.loaders` (already used by
`nbfx.project.ui.java`); the guardrail is unaffected (direct deps on `openide.awt`/`nodes`/… stay
forbidden; the transitive Swing layer is the accepted one).

### New Project templates (parity with `Templates/Project/*`)

- **Java with Ant** (`AntJava`): Java Application, Java Class Library, Java Project with Existing Ant
  Script, Java Free-Form Project.
- **Java with Maven** (`Maven2`): Java Application, Java with Existing Sources.
- **Java with Gradle** (`Gradle`): Java Application.
- **NetBeans Modules** (`APISupport`): Module, Module Suite, Library Wrapper Module.
- **Samples**: as applicable.

Project templates are Swing **wizard iterators** → their generation panels are ported to FX; the created
skeleton (`build.xml`, `nbproject/*`, `pom.xml`, `build.gradle`) uses the same headless project APIs as
the original (`ProjectGenerator`, Maven/Gradle writers).

### New File templates (parity with `Templates/*`)

Reused from the live `Templates` filesystem. Expected categories: **Java** (Class, Interface, Enum, …),
**JavaFX** (FXML, Controller, CSS — Part 2), **XML**, **Other** (Empty File, Properties, Folder),
**NetBeans Module** (Action, …), **Licenses**. Project-type-aware (filtered/enabled by target type;
destination folder/`package` from the selection).

## Customizer full parity (FXML panels)

Reproduce the original category tree exactly. **All categories enabled, no deferred/disabled panels.**

### Panel implementation — FXML

Build panels as **FXML layouts** loaded with `FXMLLoader` + controllers (the lib module bundles
`javafx.fxml`; loaded on the classpath, no `add-opens`):

- base `FxmlCustomizerPanel` (implements `FxProjectCustomizerPanel`) loading `<module>.fxml`, wiring
  `isChanged/isValid/apply/cancel`;
- reusable FXML components: tables (app params, manifest entries, processor options), editable lists
  (source folders, classpaths), combos (platform/runtime/config), file/password pickers (icons,
  keystore, sources/javadoc), radio/checkbox groups (packaging mode, signing type, run-as), sub-dialogs
  (preloader chooser, signing, icons);
- categories/sub-categories data-driven (`parentId` + `position`), rendered by
  `FxProjectCustomizerDialog` (tree left, panel right).

This replaces the current `GridPane`+`TextField` panels (`J2SEPropertiesCustomizerPanel`).

### Gap list (existing J2SE/Ant panels)

| Existing category | Current (nbfx) | Missing vs. original |
|---|---|---|
| **Sources** | `src.dir`, `test.src.dir` (text) | source-folder list add/remove, test roots, includes/excludes, source level, `source.encoding` |
| **Libraries** | `javac.classpath`, `run.classpath` | processor path (`javac.processorpath`), `test.classpath`, shared-vs-dedicated libraries folder, per-entry add/remove/edit, build dependencies |
| **Build ▸ Compiling** | `javac.source`, `javac.target` | `javac.compilerargs`, `build.classes.dir`, `build.classes.excludes`, endorsed classpath, annotation processing (`annotation.processing.*`), test-compile settings |
| **Build ▸ Packaging** | `dist.jar` | `dist.dir`, `dist.archive.excludes`, `jar.compress`, manifest (`manifest.file`, `manifest.available`) |
| **Build ▸ Documenting** | window title, author, version | `javadoc.private/.notree/.use/.nonavbar/.noindex/.splitindex/.encoding/.additionalparam` — **verify keys** (nbfx uses `javadoc.window.title`; original is `javadoc.windowtitle`) |
| **Run** | `main.class`, `application.args` | configuration selector (`nbproject[/private]/configs/*.properties`), `run.classpath`, `run.jvmargs`, `run.work.dir`, main-class browse |
| **Application** | `application.title`, `application.vendor` | `application.version`/`home` (as applicable) |
| **License Headers** | `project.license` | `project.licensePath` (custom file), default/none modes |
| **Ant/module panels** (Module, Versioning, Display, Build) | name/category/descriptions, spec version, source/target | complete the apisupport option sets |

Also: **verify every property key**, and give each panel proper `isValid()`/`isChanged()`.

### Full option matrix (J2SE + JavaFX categories)

| Category | Sub-category | Options (project properties) |
|---|---|---|
| **Sources** | | project folder, source folders (`src.dir`), test folders (`test.src.dir`), includes/excludes, source level, `source.encoding` |
| **Libraries** | | compile/test/run classpath (`javac.classpath`, `test.classpath`, `run.classpath`), processor path (`javac.processorpath`), shared vs dedicated libraries folder, `file.reference.*`, `copylibs.excludes` |
| **Build** | **Compiling** | `javac.compilerargs`, `javac.source`/`target`, `build.classes.dir`, `build.classes.excludes`, endorsed classpath, annotation processing, test-compile settings |
| | **Packaging** | `dist.dir`, `dist.jar`, `dist.archive.excludes`, `jar.compress`, manifest; JavaFX: `javafx.deploy.icon`, `.icon.native`, `.splash`, `javafx.binarycss`, `native.bundling.enabled/type`, `javafx.rebase.libs`, `keep.javafx.runtime.on.classpath`, `dist.signed.jar` |
| | **Documenting** | `javadoc.*` (private, notree, use, nonavbar, noindex, splitindex, author, version, windowtitle, encoding, additionalparam) |
| | **Deployment** | **Runtime**: `javafx.deploy.request.runtime`, `platform.active`; **WebStart**: `javafx.deploy.backgroundupdate`, `.allowoffline`, `.installpermanently`, `.adddesktopshortcut`, `.addstartmenushortcut`, `javafx.deploy.embedJNLP`, `javafx.deploy.includeDT`, `javafx.deploy.permissionselevated`, `javafx.deploy.disable.proxy`; **Download mode**: `download.mode.lazy.jars`; **Signing**: `javafx.signing.*`; **JS callbacks**: `javafx.jscallback.*`; JSE: `keep.javafx.runtime.on.classpath` + "Switch to JavaFX" |
| **Run** | | configuration selector; `main.class`/`javafx.main.class`, `javafx.fallback.class`; JavaFX platform + runtime (`platform.active`, `platform.ant.name`, runtime combo, **Manage Platforms…**); `run.jvmargs`, `run.work.dir`, `application.args`; app params (`javafx.param.*`); `javafx.run.width`/`height`; `javafx.run.htmltemplate`(+`.processed`), `javafx.run.inbrowser`(+path/args/undefined), `javafx.run.as`; preloader (`javafx.preloader.*`); `javafx.swing` |
| **Application** | | `application.title`, `application.vendor`; JavaFX main class, app params, manifest entries (`javafx.manifest.entry.*`), preloader |
| **License Headers** | | `project.license`, `project.licensePath`, `license.default` |

Include any further category the target J2SE adds (e.g. `Formatting`). Options tied to legacy
deployment tooling still round-trip; their *execution* is out of scope.

### Per-type customizer parity

| Type | Categories |
|---|---|
| **J2SE Ant** | Sources, Libraries, Build ▸ {Compiling, Packaging, Documenting}, Run, Application, License Headers (+ Formatting) |
| **apisupport module** | Sources, Component, Libraries, Compiling, Packaging, Versioning, Display, Java Platform, Formatting |
| **apisupport suite** | Sources, Libraries, Basic Branding, Formatting |
| **Maven** | Basic, Sources, Configurations, Build ▸ {Compile, Actions}, Run, License Headers, Formatting (+ Repositories) |
| **Gradle** | Project Info, Configurations, Build ▸ {Compile, Actions}, License Headers, Formatting |

## Actions parity (project-lookup `ActionProvider` model)

**Original strategy:** headless `org.netbeans.spi.project.ActionProvider` command set
(`build/clean/rebuild/compile.single/run/run.single/test/test.single/test.parallel/debug/debug.single/
debug.test.single/debug.stepinto/profile`), registered per project type in the Lookup, with
`isActionEnabled(command)` gating and `invokeAction` performing. Per-type menus come from
`Projects/<type>/Actions`.

**nbfx gaps:** context menu is marker-file based (ignores the project provider and `isActionEnabled`);
enablement not wired into menu/toolbar/context menu; command set is a subset (no `debug`, `*.single`,
`profile`); `Run` not gated on a main class/config; two `AntBuildActionProvider`s; no per-type action
sets.

**Tasks:** adopt the project-lookup provider strategy (marker file only as fallback); define the full
command set; wire enablement everywhere; register per-type action sets.

### Per-type action sets

| Type | Actions |
|---|---|
| **J2SE Ant** | New File, Build, Rebuild, Clean, Run, Debug, Test, Javadoc, Set Main, Open Subprojects, Close, Rename/Move/Copy/Delete, Find, Customize |
| **apisupport module** | (same as J2SE Ant) |
| **apisupport suite** | New File, Build, Rebuild, Clean, Run, Debug, Test, Set Main, Open Subprojects, Close, Rename/Move/Delete, Find, Customize |
| **Maven** | Build, Rebuild, Clean, Run, Debug, Test, Javadoc, Set Main, Open Subprojects, Close, Rename/Move/Copy/Delete, Find, Customize |
| **Gradle** | the Maven set **+ Profile** and a **Broken Project** action |

## Run lifecycle / activation (esp. Run)

Enable/disable from the **active project/selection** (not marker files); gate `Run` on a runnable main
class / valid configuration; model the **run lifecycle** (disable/replace `Run` while running, provide
`Stop`). Bind `debug`/`debug.single` to `nbfx.debug`.

## Run/Debug configurations

Implement the configuration model: property groups in `nbproject/configs/<name>.properties` (shared) and
`nbproject/private/configs/<name>.properties` (private), active config in
`nbproject/private/config.properties`. Run customizer combo (New/Delete/Rename) with **config-scoped**
values (main class, args, VM options, working dir, platform/runtime); extension points mirroring
`J2SECategoryExtensionProvider` / deprecated `J2SERunConfigProvider.ConfigChangeListener`. Run/Debug
resolve the **active** config's properties.

## Ant runtime & build tooling

Ship **both** Maven and Ant. nbfx already has: the bundled Apache Ant (`extide/ant/lib`, used by
`nbfx.build.ant` via a class-path-extension), an in-process `AntBuild`, the `ant` project kind, and the
J2SE harness (`java.j2seproject`) + `ide/project.ant`. To add/fix:

1. `nbfx.javafx.project` (Part 2) and other Ant-consuming modules depend on
   `org.apache.tools.ant.module` + `java.j2seproject` + `ide`'s `project.ant`.
2. Provide a correct **Ant runtime environment**: `ant.home`/`ant.version` + the harness classpath
   (`copylibstask`, junit/debug tasks) — current `AntBuild` only sets `ant.file`. Prefer reusing
   `o.apache.tools.ant.module`'s executor (`AntBridge`/`AntTargetExecutor`).
3. Ensure the bundled Ant ships in the lean run set (`extide` already in `config.nbfx.list`).
4. **Dedupe** the two `AntBuildActionProvider`s (`nbfx.build.ant` in-process vs `nbfx.project.ui.ant` CLI).

## Progress UI parity

Bring the progress UI to parity with the NetBeans default: an FX `ProgressUIWorkerProvider` over the
headless progress `Controller`, a multi-task status line, an **all-tasks popup** with **per-task
details** and actions (cancel/pause/view), system/background tasks, and "View → Progress"; exclude the
Swing `progress.ui`. Full detail in [`06-progress-ui.md`](06-progress-ui.md).

## Project problems UI (FX)

The stock NetBeans project-problems dialogs are Swing (`ide/projectui`'s `BrokenReferencesImpl` →
`BrokenReferencesAlertPanel` + `BrokenReferencesCustomizer`). Port them to FX as a
`ProjectProblemsImplementation`: an FX alert (“Open Project”, *Resolve Problems…* / *Close*, *show again*)
and an FX problems dialog listing each provider problem with its resolver and a notification line. Only
one `ProjectProblemsImplementation` may be registered, so the stock one must be removed (patch
`projectui`, or drop it after the FxProject step). The provider/resolver SPI
(`org.netbeans.spi.project.ui.ProjectProblemsProvider` / `ProjectProblemResolver`) is headless but lives
in `projectuiapi.base`, which currently needs a purity exemption (like the launcher/UI/project.ui). Full
step in [`07-small-steps.md`](07-small-steps.md).

## FX dialogs & notifications (UI)

The application has no shared dialog/notification layer: code builds ad-hoc `Dialog`/`Alert`s and the
Swing `notifications` and `uihandler` modules are still shipped. Introduce `nbfx.dialogs` — a dialog
descriptor + `Dialogs` service with options/roles, a notification line and “show again”, plus FX
notifications (slide-in toasts, a notification centre) and FX exception reporting (`ErrorReporter`
reimplemented, uncaught-exception hook); exclude/prune `notifications`, `uihandler`,
`uihandler.exceptionreporter`, `lib.uihandler`. Full detail (including the chrome ideas) in
[`09-fx-dialogs-and-notifications.md`](09-fx-dialogs-and-notifications.md). The project-problems UI builds
on it.

## Theming & branding (light/dark, CSS via branding)

Add a default light and dark theme (following the OS), editor syntax palettes (NetBeans/NetBeansDark
semantics), and route all CSS loading through one brandable place using the NetBeans branding mechanism
(`branding.dir` + `branding.token`, applied by the harness; runtime override folder). One `ThemeManager`
in a new `nbfx.theme` module applies the palettes to the shell, editor, dialogs and wizard. Full detail in
[`10-theming-and-branding.md`](10-theming-and-branding.md).

## Acceptance (Part 1)

Every supported project type has full parity — customizers, New Project/New File templates, per-type
build/run/debug actions with correct enablement, and run/debug configurations — with guardrails green
and **no JavaFX-project support yet**. Additionally: project problems are resolved in FX dialogs, no
Swing notification/exception dialogs load, and the app has working light/dark themes with brandable CSS.
