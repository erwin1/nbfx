# nbfx architecture rules

Durable, cross-cutting rules for the nbfx code base. They exist so nbfx can keep reusing NetBeans
infrastructure and, eventually, contribute the reusable parts back.

## Model / UI split: UI-free models with a core-only dependency chain

Keep behaviour that is not a user interface ("model") in **its own module, separate from the JavaFX UI
that presents it, and give the two modules different dependency chains**:

- **Model modules** must not import JavaFX, Swing/AWT or the legacy NetBeans UI
  (`javafx.*`, `javax.swing*`, `java.awt*`, `org.openide.awt`, `org.openide.nodes`,
  `org.openide.explorer`, `org.openide.windows`, `org.openide.dialogs`) and must **depend only on
  NetBeans platform/core APIs** (`org.netbeans.api.*`, `org.netbeans.modules.*` core,
  `org.openide.filesystems`, `org.openide.util`, `org.openide.util.lookup`, …).
- **UI modules** may import JavaFX and depend on the model module (plus the nbfx UI modules).

**Why.** A model module with a core-only dependency chain can later be **moved into the NetBeans core**
and the **Swing front end rebuilt on top of it**, so the pure-JavaFX nbfx front end and the Swing
front end share one implementation instead of keeping two copies in sync.

**Where.** Model modules live in **`nbfx-platform`** (which depends only on `nb.cluster.platform`); the
JavaFX UI that consumes them lives in **`nbfx-ide`** / **`nbfx-java`**. `ant check-purity` enforces the
UI-free part for every module listed in `nbfx.model.src.pattern` (see `check-purity.xml`).

**Prefer reuse.** Before writing a model, check whether the NetBeans core already exposes it headlessly
(for example `org.netbeans.api.templates.FileBuilder` for template creation). Reuse it and keep only
the genuinely new, UI-free parts (for example template *discovery*, which NetBeans ships only inside
its Swing chooser).

**Example.** `com.gluonhq.netbeans.nbfx.templates` is the model (template discovery + creation, no
JavaFX); the New File wizard UI that consumes it lives in the project UI module, mirroring NetBeans'
`ide/projectui` (where the original New File action, wizard and chooser live).

## Observability in models

Model modules must not use `javafx.beans.*` / `javafx.collections.*`: those are JavaFX, so they would
break the core-only chain and the `check-models` guardrail. Model state is observable through **JDK
mechanisms** instead — `java.beans.PropertyChangeSupport` plus explicit nbfx listener/event types, the
way `org.openide.nodes.Node` / `Children` are modelled — and a small **UI adapter** wraps the model in
JavaFX `ObservableList` / `ObservableValue` for binding.

`org.openide.util.Lookup` is fine in a model: it is part of the NetBeans core, and it **replaces the
legacy `Node.Cookie` extension mechanism** (a node's lookup is the extension point; the old
`Node.getCookie(type)` already delegates to it).

**Reference example (planned).** The nodes/views split: `nbfx.nodes` (model — `NodeModel`,
`ChildrenModel`, `ChildFactory<T>`, `PropertyModel`, `PropertySetModel`, `ExplorerContext`; `java.beans`
events, `Lookup` for content, no JavaFX) and `nbfx.nodes.ui` (JavaFX views over the model, FX observable
adapters, property editors). See [`plan/12-nodes-and-views.md`](plan/12-nodes-and-views.md).
