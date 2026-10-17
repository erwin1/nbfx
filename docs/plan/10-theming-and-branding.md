# 10 — Theming & branding (light/dark, CSS via the NetBeans branding mechanism)

Step: **Theming & branding** · branch: `feature/theming` · status: planned, can start in parallel with
the wizard/New File topics (foundation).

## Goals

1. A **default light and dark theme** for the whole nbfx application (shell, editor, dialogs, wizard,
   popups) that follows the operating system by default.
2. **Editor syntax colouring profiles** with classical NetBeans semantics (NetBeans / NetBeansDark),
   selectable independently from the UI theme.
3. **All CSS loading goes through one brandable place**, using the NetBeans branding mechanism, so a
   platform application or IDE derivative can override themes, styles and strings without patching code.

## Current state (evidence)

- CSS is loaded ad hoc, per component, with hard-coded resources:
  - `nbfx.ui`: `JavaFXLaunchApp` → `scene.getStylesheets().add(getResource("styles.css"))`;
    `NbfxTabPane` → the same `styles.css`.
  - `nbfx.wizard`: `FxWizardDialog` adds `wizard.css` to the DialogPane.
  - `nbfx.editor`: `CodeEditor` adds `codeeditor.css`; `CompletionPopup`/`CompletionItemCell` add
    `completion-popup.css`; `BreadcrumbsPopup` adds `breadcrumbs-popup.css`.
  - `nbfx.findinprojects`: `FindInProjectsDialog`, `IssuesDialog`, `IgnoreListDialog`, `SearchView` add
    `search.css` (the dialogs also copy the owner scene's stylesheets).
- `styles.css` is **light-only**: hard-coded `#2775BF`, `white`, `black`, `#D3D3D3`, `lightgray`.
  There is no dark variant and no runtime theme switch (grep for `dark`/`theme`/`colorScheme` is empty).
- The editor already defines syntax colours as looked-up colours in `codeeditor.css`
  (`-code-keyword-color`, `-code-string-color`, `-code-comment-color`, `-code-field-color`,
  `-code-method-color`, `-code-class-color`, `-code-css-*`, `-code-xml-*`, `-code-hyperlink-color`) but
  with light backgrounds (`white`, `#F9F4E0`).
- nbfx has **no branding wiring**: `nbfx.properties` has no branding keys, the harness does not apply
  the NetBeans `Branding` task, and there is no `branding/` directory.

## Mechanisms available

- **JavaFX follows the platform** (verified against the bundled JavaFX 27 `javafx.graphics.jar`):
  `Platform.getPreferences()` → `ColorScheme` (`LIGHT`/`DARK`), `backgroundColor`, `foregroundColor`,
  `accentColor`, `reducedMotion`, `reducedTransparency`, `reducedData`; `Scene.getPreferences()` is
  settable per scene (e.g. force reduced motion for a dialog).
- **NetBeans branding** (verified in the submodule):
  - `nbbuild/netbeans/harness/suite.xml` runs
    `<branding cluster="${cluster}" overrides="${branding.dir}" token="${branding.token}" …/>`.
  - `nbbuild/antsrc/org/netbeans/nbbuild/Branding.java` walks the overrides directory (per-module
    branding jars/dirs) and packs the overrides into the cluster's module jars — i.e. **any module
    resource (including CSS) can be replaced by a brand**.
  - `org.openide.util.NbBundle.setBranding(token)` selects `Bundle_<token>.properties` variants.
  - There is **no** config-filesystem `Branding` folder convention to rely on.

## Target architecture

### Module `nbfx.theme` (new, `nbfx-platform`)

Brandable resources + one service; it is UI (the theme manager touches scenes/dialogs), while the
palette/manifest data is plain CSS/properties and could be split into a UI-free model later.

```
nbfx.theme/
  src/.../theme/
    theme-light.css        # palette: -fx-base, -fx-background, -fx-control-inner-background,
    theme-dark.css         #          -fx-text-base-color, -fx-accent, -fx-selection-bar,
                           #          -fx-focus-color, nbfx-specific tokens
    app.css                # theme-agnostic structural rules (shell, lists, tabs, status bar…)
    editor-light.css       # syntax palette: -code-* (NetBeans semantics)
    editor-dark.css        # syntax palette: -code-* (NetBeansDark semantics)
    theme.properties       # brandable manifest: stylesheets per theme/order, editor profiles, defaults
    ThemeManager.java      # @ServiceProvider: active theme, apply(Scene/DialogPane), OS following,
                           #                  persistence, observable active theme
```

- **Semantic tokens only** in component CSS: no literal colours. The palettes define the JavaFX
  looked-up colours; `app.css` and the modules' sheets reference them.
- `theme.properties` (a module resource, therefore brandable) declares:
  - `theme.default=system|light|dark`
  - `theme.system.stylesheets=…`
  - `theme.light.stylesheets=theme-light.css,app.css`
  - `theme.dark.stylesheets=theme-dark.css,app.css`
  - `editor.light=editor-light.css`, `editor.dark=editor-dark.css`
  - optional accent override (`accent.color=…`)

### `ThemeManager`

- Applies the active stylesheets to every scene/dialog (and to new windows created later).
- Default `SYSTEM`: bind to `Platform.getPreferences().colorSchemeProperty()`; switch live.
- Manual override: `View ▸ Theme` (System/Light/Dark) and `Options ▸ Appearance ▸ Theme`; persisted in
  `NbPreferences` (per user).
- Exposes the active theme as an observable so images/icons/editor palettes can react.
- Applies `reducedMotion` from the platform to transitions (dialogs/wizard/notifications).

### Branding

- **Build time (standard NetBeans mechanism):**
  - `nbfx.properties`: add `nbfx.branding.dir=branding` and `nbfx.branding.token=nbfx` (token must match
    `[a-z][a-z0-9]*(_[a-z][a-z0-9]*)*`, as `Branding.java` enforces).
  - Harness `build.xml`: after the cluster build, run the NetBeans `Branding` task over the nbfx
    clusters with those values (mirroring `harness/suite.xml`).
  - A derivative drops its overrides in `branding/<module>/…` (theme CSS, `theme.properties`, images,
    `Bundle_<token>.properties`) and gets them baked into the distribution.
- **Runtime override (for experiments / branded deployments without repackaging):** `ThemeManager`
  first looks for the manifest and stylesheets under a config-filesystem folder (e.g. `NbFx/Theme/`);
  a derivative can contribute a layer folder. Falls back to the `nbfx.theme` resources.
- **Documentation:** `docs/theming.md` — how to create a brand, which files/keys are brandable, how to
  add a fully custom theme, and the token/key reference.

### CSS migration (removes the scattering)

1. Split `styles.css` into `theme-light.css`/`theme-dark.css` (palette) + `app.css` (structure);
   convert every hard-coded colour to a semantic token.
2. Every module keeps its own component sheet (owner of the selectors) but does **not** load it by
   `getResource`; the sheet is applied through `ThemeManager` (or imported from `app.css`). Migrate
   `wizard.css`, `codeeditor.css`, `completion-popup.css`, `breadcrumbs-popup.css`, `search.css` and the
   future `dialogs.css`.
3. Dialogs that copy the owner scene's stylesheets (`IssuesDialog`, `IgnoreListDialog`) use
   `ThemeManager` instead, so they get the theme and stay brandable.
4. Dark-theme assets: audit icons/images (the wizard already loads PNG/GIF platform icons via the class
   loader and falls back to chips for SVG-only icons; dark variants may be needed).

### Editor syntax colouring profiles

- Keep the existing `-code-*` variable names and provide **light and dark palettes** matching the
  classical NetBeans profiles (keyword/string/comment/field/method/class/CSS/XML/…).
- Default: the editor palette follows the UI theme. Allow an independent profile selection (e.g. light
  UI + NetBeansDark editor) in `Options ▸ Editor ▸ Syntax Colouring`, persisted.
- Optional later: load the original NetBeans profile XMLs from the `Editors/Preferences` layer and map
  their token names onto the `-code-*` variables, if exact profile parity is wanted.

## Acceptance

- Runtime switch System/Light/Dark works for the whole app (shell, editor, dialogs, wizard, popups) and
  persists; an OS theme change is followed when set to System.
- No light-only hard-coded colours remain in nbfx CSS; all component sheets resolve through the theme.
- Editor syntax palettes work in both themes; the profile can be selected independently.
- Branding: a `branding/` override + `-Dnbfx.branding.token=…` rebuild yields the branded CSS/strings, and
  the runtime override folder is honoured.
- `ant build` + `ant check-purity` green; headless tests for `ThemeManager` (state, persistence, manifest
  parsing) and a manual light/dark matrix.

## Ordering / dependencies

- **Depends**: none.
- **Parallel**: with `feature/fx-dialogs` and the wizard/New File topics. The only shared contract is the
  **semantic token names** — agree them up-front so the dialogs/notifications and wizard CSS consume the
  theme instead of hard-coding colours.
- Land this before claiming an app-wide dark theme, and before the `fx-dialogs` step finalises its
  stylesheet (they can also proceed concurrently on the agreed tokens).

## Risks

- **Branding in a non-suite harness**: nbfx builds clusters directly with `nbbuild`; the `Branding` task
  must be invoked by our harness (it is normally a suite-build concern). The runtime config-FS override
  is the fallback that keeps customisation simple.
- **Contrast/accessibility** of the dark palette (and the editor syntax colours) needs a deliberate pass.
- **Icons for dark** (some platform icons are authored for light backgrounds; SVG-only icons cannot be
  decoded by JavaFX and already fall back to kind chips).
- **Third-party CSS** (e.g. RichTextFX/editor internals) may set colours outside our control.
- **Preference migration**: the manual theme/profile preference is new; keep `System` as the default so
  existing users see no surprise.
