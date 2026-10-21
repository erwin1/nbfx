# 01 — Research findings (evidence)

> **Historical research.** Names in this document reflect the code as researched (some `Fx*` names and
> `FxProject`); the canonical naming is in [`32`](32-api-tiers-and-plugin-contract.md) and the product
> model is `ProjectModel` (`nbfx.project`). The plan itself is [`26`](26-roadmap.md) and
> [`07`](07-small-steps.md).

## 1. The original `javafx` cluster (`../gluon-netbeans-fx/javafx/*`)

| Module | Role |
|---|---|
| `javafx2.project` | Project-type behaviour: wizard/generator, JavaFX properties, run/application/packaging/deployment panels, platform filter/updater, icon annotator, opened hook, problems. |
| `javafx2.platform` | JavaFX runtime detection on a `JavaPlatform` (`JavaFxRuntimeInclusion` = MISSING/PRESENT/INCLUDED, `jfxrt.jar`), default JDK javadoc/sources, platform props. |
| `javafx2.editor` | Full **FXML editor**: SAX parser, model, bean kernel, completion, codegen, CSS, `FXMLOpener` SPI; MIME `text/x-fxml+xml`. |
| `javafx2.scenebuilder` | **External-process** integration (locate a SceneBuilder install, `ProcessBuilder`-launch). |
| `javafx2.kit` | Kit/samples/templates. |
| `javafx2.samples`, `maven.htmlui` | Samples / helper. |

## 2. How the original registers a JavaFX project (project kind)

There is **no JavaFX project type**. `J2SEProjectType.TYPE == "org.netbeans.modules.java.j2seproject"`;
the wizard runs `ProjectGenerator.createProject(dir, J2SEProjectType.TYPE, …)` and sets
`javafx.enabled=true` (`JFXProjectProperties.JAVAFX_ENABLED`). JavaFX behaviour is layered on the J2SE
type via project-type-scoped services, most gated by `javafx.enabled`:

- `@ProjectServiceProvider(service=J2SECategoryExtensionProvider.class, projectType="…j2seproject")`
  for **Application / Packaging / Deployment** (extends the J2SE panels).
- `@ProjectCustomizer.CompositeCategoryProvider.Registration(projectType="…j2seproject", position=450/250)`
  for **Run** and **Deployment**.
- `PlatformFilter` (`JFXProjectPlatformFilter`), `ProjectOpenedHook` (`JFXProjectOpenedHook`),
  `JFXPlatformUpdater`, `ActionProvider`, `IconAnnotator`.

**nbfx mapping:** keep the `ant` kind; recognise JavaFX by *type == j2seproject* **and**
*`javafx.enabled==true`*, and gate JavaFX `FxProjectCustomizerPanel`s / commands with an
`appliesTo(project, kind)` refinement (the nbfx analogue of `J2SECategoryExtensionProvider`).

## 3. nbfx infrastructure (reusable / gaps)

- **Project kinds**: `ProjectKindProvider` / `ProjectKinds`; `AntProjectKindProvider` recognises
  j2seproject + apisupport (`nbfx-java/.../project.ui.ant`).
- **Customizers**: `FxProjectCustomizerPanel` (`@ServiceProvider`, `appliesTo(project, kind)`,
  `parentId`, `position`, `isChanged/isValid/apply/cancel`), framework in `nbfx-ide/.../project.customizer`;
  J2SE panels currently a *flat subset* (`J2SEPropertiesCustomizerPanel` + `Field(key,labelKey)`).
- **Editor**: `EditorService`/`EditorDocument`/`ContentManager`; syntax by extension
  (`SyntaxDecorators`: `css`→CSS, `xml`/`fxml`→XML, else Java). `EditorServiceImpl.createDocument`
  always returns `CodeEditor` — **no pluggable document factory**.
- **Completion**: `CompletionProvider`/`CompletionContext`/`CompletionItem` (`nbfx.api`), with a
  `java.source`-backed pipeline (`JavaSemanticCompletionProvider` + `completion/support/*`).
- **Wizard**: `FxWizard` (`getPages()`, `finish()`), `@FxWizardRegistration(category, position)`,
  `WizardRegistry`, `FxWizardDialog`. Existing: `newProject` ("Java with Maven"), `newGradleProject`
  ("Java with Gradle"); `ActionIds.NEW_PROJECT` opens the Maven wizard directly. **No chooser, no
  settings bag, no dynamic iterator, no `TemplateWizard`.**
- **Actions**: `BuildCommands` mirrors `ActionProvider` names; `BuildActionProvider` is registered
  globally *and* `@ProjectServiceProvider`; `BuildActions.providerFor/isActionEnabled`;
  `BuildContextMenuContributor` is marker-file based. No per-type action sets; no run/debug configs.
- **Views/actions/options**: `@FxViewRegistration`, action registry, `FxOptionsPanel`/`OptionsRegistry`.
- **Java**: **no `JavaPlatform` usage anywhere**; a Java Platform manager is a prerequisite.
- **Bundled JavaFX**: `nbfx-platform/...lib.javafx` ships JavaFX 27 as class-path-extensions
  (base/controls/fxml/graphics + jfx.incubator); **javafx.web/javafx.swing not bundled**.
- **Ant**: `nbfx.build.ant` runs Ant in-process via a class-path-extension to the bundled
  `extide/ant/lib/ant.jar`; `AntBuild` sets only `ant.file` (no `ant.home`/harness classpath).

## 4. Embedded SceneBuilder

- Maven artifact **`com.gluonhq.scenebuilder:kit`** — latest **26.0.0** (no 27; the SceneBuilder line is
  15…26). Module `com.gluonhq.scenebuilder.kit`; package namespace `com.oracle.javafx.scenebuilder.kit`.
- Kit POM deps: `javafx-fxml`, **`javafx-web`**, **`javafx-swing`**, `maven-resolver-supplier`
  (excludable), `jakarta.json` 2.0.1.
- `gluonhq/EmbeddedSceneBuilderDemo` (2024) — modern embedding reference: `SceneBuilderPane`
  (library | document(hierarchy+controller) | content | inspector), `SBController` (copy/cut/paste/
  duplicate, select all/none/delete, undo/redo, new/open/save FXML), `DependenciesScanner` (classpath →
  custom controls). Demo runtime flags: `-Djavafx.allowjs=true`,
  `--add-opens=javafx.fxml/javafx.fxml=com.gluonhq.scenebuilder.kit`.
- `svenreimers/nbscenebuilder` (2019) — old integration (Swing `TopComponent` multiview, bundled
  SceneBuilder 8.5). Useful as a **feature list** (multiview, window groups, undo/redo bridge, CSS
  pane, preview), not as code.

## 5. External binaries mechanism

`<module>/external/binaries-list` lines: `<SHA1> <spec>` where `<spec>` is a file name or a Maven
coordinate (`group:artifact:version[@ext]`, e.g. `eu.agrosense.client:lib-javafx:13.03-beta@nbm`).
`nbproject/project.xml` `<class-path-extension>` maps `<binary-origin>` → `<runtime-relative-path>`
(→ `release/modules/ext`). `nbbuild/build.xml` runs `downloadbinaries` over `**/external/binaries-list`.

## 6. Original wizard framework (`org.openide.WizardDescriptor`)

`platform/openide.dialogs/org/openide/WizardDescriptor.java` (extends `DialogDescriptor`) is the model
for the shared framework ([`02-part1-infrastructure.md`](02-part1-infrastructure.md), "Wizard framework"):

- `Panel<Data>`: `getComponent`, `isValid`, `readSettings(Data)`, `storeSettings(Data)`, `getHelp`.
- `Iterator<Data>`: `current/hasNext/hasPrevious/next/previous` + change listeners; `ArrayIterator`,
  `InstantiatingIterator` (`instantiate()` → `Set`), plus `Progress`/`Background`/`Asynchronous`
  variants.
- a **settings/data bag** (`putProperty`/`getProperty`; the descriptor is a `Lookup.Provider`) and
  UI properties (`PROP_ERROR_MESSAGE`, `PROP_WARNING_MESSAGE`, `PROP_INFO_MESSAGE`, `PROP_CONTENT_DATA`
  step list, `PROP_IMAGE`, help).
- `getInstantiatedObjects()`.
- `TemplateWizard extends WizardDescriptor` (openide.loaders) adds the template `FileObject` + target
  folder + the template's `instantiatingIterator`.

## 7. Original template infrastructure

- Templates are registered via `@TemplateRegistration` (`platform/api.templates`) and/or the
  `Templates/<category>` layer; all are merged at runtime into the session **`Templates` filesystem**
  (populated by the clusters nbfx ships).
- Metadata attributes: `displayName`, `templateCategory`, `position`, `iconBase`, `templateWizardURL`,
  `instantiatingIterator`, `dataObjectClass`, `createFromTemplateHandler`.
- Headless creation: `DataObject.createFromTemplate(…)` (`openide.loaders`) /
  `FileBuilder` / `CreateFromTemplateImpl` (`platform/api.templates`).
- Swing UI: `NewFile extends ProjectAction` + `NewFileWizard extends TemplateWizard` +
  `Templates.buildSimpleTargetChooser` (`ide/projectuiapi`), and per-template `TemplateWizard.Iterator`s
  (e.g. `NewJavaFileWizardIterator`).

## 8. Progress API & UI (evidence)

- **Headless core** `platform/api.progress`: public `ProgressHandle` (`start`/`progress`/
  `switchToIndeterminate`/`switchToDeterminate`/`suspend`/`setDisplayName`/`setInitialDelay`/
  `addDefaultAction`/`finish`; `createHandle`/`createSystemHandle`) and SPI
  `org.netbeans.modules.progress.spi` (`Controller`, `InternalHandle`, `ProgressEvent`, `TaskModel`,
  `ProgressUIWorker`, `ProgressUIWorkerWithModel`, `RunOffEDTProvider`).
- **NB API** `platform/api.progress.nb`: `ProgressHandleFactory`, `ProgressUtils`,
  `AggregateProgressFactory`, and the UI bridge SPI `ProgressUIWorkerProvider` /
  `ExtractedProgressUIWorker` / `SwingController`.
- **Swing UI** `platform/progress.ui`: `ProgressUI`/`ProviderImpl`, `StatusLineComponent` (status-line
  aggregate), `ListComponent` + `PopupPane` (the all-tasks popup), `NbProgressBar`, `ProgressListAction`
  ("View → Progress"), `AbstractWindowRunner`/`RunOffEDTImpl`, `CancelAction`.
- **nbfx today**: `FxProgress` (single slot), `FxProgressImpl`, `ProgressStatusElement`, `BuildProgress`
  — no multi-task model, no popup/details, no per-task actions. See
  [`06-progress-ui.md`](06-progress-ui.md).

## 9. Reference inventory

- Original modules: `javafx/javafx2.{project,platform,editor,scenebuilder,kit,samples}`.
- Original key classes: `JFXProjectProperties`, `JFXProjectConfigurations`, `JFXProjectPlatformFilter`,
  `JFXPlatformUpdater`, `J2SEProjectType`, `JavaFXProjectWizardIterator`, `JFXProjectGenerator`;
  `ui/JFX{Application,Run,Packaging,Deployment,Signing,Icons,DownloadMode,RequestRuntime,
  JavaScriptCallbacks}Panel`, `JSEDeploymentPanel`; `editor/completion/impl/*`,
  `editor/{model,beans,parser}/*`, `editor/spi/FXMLOpener`.
- Also: `org.openide.WizardDescriptor`, `org.openide.loaders.TemplateWizard`, `platform/api.templates`
  (`TemplateRegistration`, `FileBuilder`, `CreateFromTemplateImpl`), `org.netbeans.spi.project.ActionProvider`,
  `J2SEActionProvider`, `J2SERunConfigProvider`/`J2SECategoryExtensionProvider`;
  `org.netbeans.api.progress.ProgressHandle`, `ProgressHandleFactory`, `progress.spi.Controller`/`TaskModel`,
  `ProgressUIWorkerProvider`, `platform/progress.ui` (`StatusLineComponent`, `ListComponent`, `PopupPane`).
- nbfx touch points: `nbfx.api.{editor,completion,view,actions}`, `nbfx.editor` (`EditorServiceImpl`,
  `SyntaxDecorators`), `nbfx.project.customizer` (`FxProjectCustomizerPanel`, `FxProjectCustomizers`),
  `nbfx.project.ui.api` (`ProjectKindProvider`), `nbfx.project.ui.ant` (`AntProjectKindProvider`,
  `J2SECustomizerPanel`), `nbfx.wizard` (`FxWizard*`), `nbfx.run`, `nbfx.build.ant`, `nbfx.options`.
- SceneBuilder: `gluonhq/scenebuilder` (kit 26.0.0), `gluonhq/EmbeddedSceneBuilderDemo` (reference),
  `svenreimers/nbscenebuilder` (feature reference).

## 10. Template wizards: metadata and custom iterators (evidence)

- Templates are `Templates/*` entries with `template=true`. The metadata the chooser uses:
  `displayName`, `iconBase`, `position`, `templateCategory`, `requireProject`, `template.openFile`,
  `instantiatingWizardURL`, `instantiatingIterator`, `javax.script.ScriptEngine`.
- **Folder-level rules** (all verified in `ide/projectui`):
  - `simple=false` hides a whole folder — `NewTemplateAction.acceptObj`, `TemplateWizard1.acceptDataObject`,
    `TemplateChooserPanelGUI.isFolderOfTemplates`. In the dist: `Templates/Project`, `Privileged`,
    `Recent`, `Services`, `J2SEModule`, `NetBeansModuleDevelopment-files`.
  - `templateCategory=invisible` hides a folder (e.g. `Templates/Licenses`, issue #102508);
    `helper-files` (e.g. `Templates/Properties`) is filtered like a normal category
    (`TemplateChooserPanelGUI.hasChildren`).
  - A folder may itself be a template (e.g. `Templates/Other/Folder`).
- **Display names**: `SystemFileSystem.localizingBundle` keyed by the *file path*
  (`FileSystem.annotateName`), e.g. `Templates/GUIForms/JDialog.java=JDialog Form` in
  `org.netbeans.modules.form.resources.Bundle`. Annotation-generated templates use a `bundlevalue`
  `displayName` instead.
- **Project-type filtering**: `OpenProjectList.isRecommended` intersects a template's
  `templateCategory` with the project's `RecommendedTemplates`; empty recommended types means “show all”.
- `Templates/Project` holds project templates (New Project) and is excluded from New File.
- **27 distinct custom iterators** for non-Project templates: `java.project.ui` ×2 (Java file wizard:
  Class/Interface/Enum/Record/… and the package/module-info variants), `junit.ui.wizards` (9),
  `beans.resources.templates.Templates` (5), `testng.ui.wizards` (2), `apisupport.project.ui.wizard.*`
  (13), `maven.osgi.templates`, `java.j2semodule`, `css.prep.ui.wizard`, `selenium2.*` (3),
  `javascript.karma`.
- `TemplateWizard` uses a template's `instantiatingIterator` when present and otherwise falls back to the
  generic target chooser. Headless creation (`FileBuilder.build()` → `CreateFromTemplateImpl.prepare()`)
  collects parameters through `CreateFromTemplateAttributes` and delegates to a
  `CreateFromTemplateHandler`; the Java templates are freemarker-scripted.

## 11. Project problems UI (evidence)

- **Swing implementation**: `ide/projectui/src/org/netbeans/modules/project/ui/problems/`
  `BrokenReferencesImpl` (`@ServiceProvider(service = ProjectProblemsImplementation.class)`) shows two
  `DialogDescriptor`s:
  1. `BrokenReferencesAlertPanel` — “Open Project”, *Resolve Problems…* / *Close*, warning;
  2. `BrokenReferencesCustomizer` — “Resolve Project Problems”, per-problem resolvers.
- **Trigger**: `org.netbeans.api.project.ui.ProjectProblems.showAlert(project)` (`ide/projectuiapi`) →
  the SPI implementation, when a project reports problems.
- **Problem providers**: J2SE `RuntimePlatformProblemsProvider`, `ModulePathsProblemsProvider`; Maven
  `JavaPlatformProblemProvider`, `MavenModelProblemsProvider`, `MissingModulesProblemProvider`.
- **Only one implementation may be registered** (`Lookup.lookup(ProjectProblemsImplementation.class)`),
  so an FX port must replace `BrokenReferencesImpl` (patch `projectui` or drop it after the FxProject
  step).
- Suppression: `BrokenReferencesSettings` preference `showAgainBrokenRefAlert` and
  `-J-DBrokenReferencesSupport.suppressBrokenRefAlert=true`.

## 12. Notifications and exception reporting (evidence)

- **Shipped Swing modules**: `ide/notifications` (`org-netbeans-modules-notifications.jar`),
  `platform/uihandler` + `uihandler.exceptionreporter` + `lib.uihandler`
  (`org-netbeans-modules-uihandler.jar`, `org-netbeans-lib-uihandler.jar`).
- **nbfx today**: no notification layer; `ErrorReporter` (`nbfx.api`) is a `javafx.scene.control.Alert`
  wrapper with **30** call sites; no uncaught-exception handler; many ad-hoc `Dialog<ButtonType>`/`Alert`
  usages (`FxProjectCustomizerDialog`, `PluginsDialog`, `GitStatusView`, `IgnoreListDialog`,
  `FindInProjectsDialog`, `IssuesDialog`, `FxOptionsDialog`, `DeleteActions`, `CloseConfirmation`).

## 13. Theming, CSS and branding (evidence)

- **CSS loading is scattered** (all with `getResource(...)`): `styles.css` (`JavaFXLaunchApp`,
  `NbfxTabPane`), `wizard.css` (`FxWizardDialog`), `codeeditor.css` (`CodeEditor`),
  `completion-popup.css` (`CompletionPopup`, `CompletionItemCell`), `breadcrumbs-popup.css`
  (`BreadcrumbsPopup`), `search.css` (`FindInProjectsDialog`, `IssuesDialog`, `IgnoreListDialog`,
  `SearchView`). The two find dialogs also copy the owner scene's stylesheets.
- `styles.css` is light-only (hard-coded `#2775BF`, `white`, `black`, `#D3D3D3`, `lightgray`); there is
  no theme switch. The editor already declares looked-up syntax colours (`-code-keyword-color`,
  `-code-string-color`, …, `-code-xml-*`) but on light backgrounds (`white`, `#F9F4E0`).
- **JavaFX 27 platform preferences** (verified in the bundled `javafx.graphics.jar`):
  `Platform.getPreferences()` → `ColorScheme` (`LIGHT`/`DARK`), `backgroundColor`, `foregroundColor`,
  `accentColor`, `reducedMotion`, `reducedTransparency`, `reducedData`; `Scene.getPreferences()` is
  settable per scene.
- **NetBeans branding**: `harness/suite.xml` runs
  `<branding cluster="${cluster}" overrides="${branding.dir}" token="${branding.token}" …/>`;
  `nbbuild/antsrc/…/Branding.java` packs per-module brand overrides into the cluster's module jars, so
  any resource (including CSS) can be replaced by a brand; `NbBundle.setBranding(token)` selects
  `Bundle_<token>.properties`. There is no config-filesystem `Branding` folder convention.
