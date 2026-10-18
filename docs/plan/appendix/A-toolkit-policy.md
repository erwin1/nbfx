# Appendix A — Toolkit and dependency policy

The rule that everything else depends on. `PLAN.md` §§2, 3.3, 4 and 9 refer here. It is written against
the actual tree, not against the ideal.

## A.1 What the tiers mean for types

| Tier | May reference | Never references |
|---|---|---|
| **T0** implementation | anything its module needs | — (exports nothing; no spec version) |
| **T1** shared | `org.openide.filesystems`, `org.openide.util*`, `java.*`, `javax.lang.model.*`, `com.sun.source.*` | `javafx.*` (unless the module also says so), `javax.swing.*`, `java.awt.*` |
| **T2-FX** published UI API | `javafx.*` (`Node`, `ObservableValue`, `KeyCombination`, `Image`) + the T1 set | `javax.swing.*`, `java.awt.*`, `javafx.embed.swing.*` |
| **M** published model/backend API | `org.openide.filesystems`, `org.openide.util*`, `java.*`, `javax.lang.model.*` **(converted, see A.4)**, `com.sun.source.*` (JDK `jdk.compiler`) | `javafx.*`, `javax.swing.*`, `java.awt.*`, `org.netbeans.modules.*` internals |

The three degrading questions for any type in a T1/T2-FX/M signature: *which classloader owns it, which
thread may call it, and who disposes it?* If any answer is "the UI toolkit", the type is in the wrong tier.

## A.2 `java.awt` and `javax.swing` — the definitive answer

The tree refutes the blanket ban promised by earlier drafts:

| API | Reality | Who needs it |
|---|---|---|
| `CompilationInfo.getDocument()` | returns `javax.swing.text.Document` (`netbeans/java/java.source.base/.../CompilationInfo.java:46`) | nothing in nbfx calls it today |
| `ChangeInfo` | uses `javax.swing.text.Position` (`netbeans/ide/spi.editor.hints/.../ChangeInfo.java:24`) | the hints SPI, if reused |
| `Node.getIcon(int)` | returns `java.awt.Image` (`netbeans/platform/openide.nodes/.../Node.java:22,434`) | only if `openide.nodes` is reused; nbfx does not |
| `ProgressHandle` | imports `javax.swing.Action`, `java.awt.event.ActionListener` (`netbeans/platform/api.progress/.../ProgressHandle.java:22-27`) | `nbfx.plugins` already depends on it |
| `ChangeListener` | accepted exemption for a headless query (`check-purity.xml:52-54`, `ProjectModuleInfoAccessibilityQuery.java:19`) | accepted today |

**Decision.**

1. **`javax.swing.text` is banned in nbfx sources.** The editor model is the FX rich-text model
   (`jfx.incubator…CodeTextModel`), and the semantic layer deals in character **offsets**, never in a
   `Document` (`TextOffsets.java` is the whole bridge). This is already true today; the guard merely has
   to keep it true. Never call `CompilationInfo.getDocument()`, `EditorCookie.getDocument()`,
   `NbDocument`, whichever module you are in.
2. **`javax.swing` and `java.awt` are banned, with a named-exemption register.** The only accepted types
   are ones an upstream SPI forces into a signature and that carry no UI (today: `ChangeListener`,
   `java.awt.event.ActionListener`). Every exemption is a line in the register (A.6), with an owner and a
   deadline to remove it.
3. **A "no UI" test:** an exemption may reference an interface/constant, never a component
   (`JComponent`, `Window`, `TopComponent`, `SwingUtilities`, `SwingFXUtils`, `JFXPanel`).

## A.3 Upstream reuse: bridge vs replace

"Retire the Swing modules" is not a strategy; for every upstream UI API the plan must choose one of three
mechanisms. Use *reimplement behind the API* whenever the API is already provider-based (a
`@ServiceProvider`/`Lookup` seam exists), because then zero callers change:

| Upstream API | Mechanism | Why |
|---|---|---|
| `DialogDisplayer`, `NotifyDescriptor` | **reimplement behind the API** (register an FX implementation for the same service) | provider-based; many reused modules call it |
| `ProgressHandleFactory` | **reimplement behind the API** (provide an FX `Provider`); keep the Swing-typed `ProgressHandle` only inside the bridge | `ProgressHandle` itself is Swing-typed, so it can never cross into M — see A.2 |
| `StatusDisplayer`, `IOProvider` | **reimplement behind the API** | provider-based; output is a platform service |
| `TopComponent`, window system (`core.windows`) | **replace** with nbfx `platform.workspace` | the API *is* the Swing window system; no provider seam worth keeping |
| `Node`/explorer | **replace** with `api.nodes` (M) | `Node` pulls `java.awt.Image` and the explorer; nbfx already ignores it |
| `EditorCookie`, `Document`-based editor services | **replace** with `api.editor` + offsets | the Swing text model is the thing being replaced |
| hints/refactoring (`Fix`, `ChangeInfo`) | **bridge** in the Java layer, convert to nbfx value types (`api.elements`) | `ChangeInfo` is Swing-typed; the UI is already ours |

The work is the *table*, per upstream module, with an owner and a milestone. This appendix fixes the
policy; the per-module matrix is Appendix B (Java) and the still-to-write `appendix/C` (platform/IDE).

## A.4 `javax.lang.model` and nb-javac: convert at the boundary, never expose

`javax.lang.model.*` and `com.sun.source.*` are stable JDK (`jdk.compiler`) APIs and may appear in M as
*inputs*. They must not appear in nbfx **value types**, because the IDE can run on **nb-javac**, which
bundles its own `javax.lang.model.element.Modifier` class: an identity check against the JDK enum silently
never matches. The tree already solved this once, and that solution is the rule:

- `api.elements.SourceModifiers.toModifierBits(Set<?>)` matches flags **by enum name**, exactly because
  "at runtime the set may hold constants of nb-javac's own copy" (`SourceModifiers.java:19-24`).

So: the bridge converts element/modifier/type information into nbfx value types (`SourceElementKind`,
`SourceTypeKind`, `SourceModifiers` bits, `SourceLocation`, strings, offsets) before it leaves the Java
layer. `ElementHandle`, `TypeElement`, `ClassIndex` never appear in an `api.*` signature.

## A.5 The rule of confinement (the mechanism that stops the creep)

Two rules, both machine-checkable:

1. **The denylist** — nbfx sources outside the declared owners must not import
   `org.netbeans.api.java.source.*`, `org.netbeans.libs.javacapi*`, `javax.swing.*`, `java.awt.*`,
   `org.openide.awt|nodes|explorer|windows|dialogs`, `org.netbeans.core.windows`,
   `org.netbeans.modules.projectuiapi.base`, `javafx.embed.swing.*`.
2. **The allowlist** — a module declares in its metadata which denylisted import families it may use, and
   the guard fails on any import outside that declaration. The Java layer's modules are the only ones that
   may declare `java.source`; today that is 4 modules and ~50 files (Appendix B).

The guard must **check references, not just bundled classes**: `check-jars` (`check-purity.xml:93-116`)
greps the *file list* of three cluster directories, so a class that imports a JDK `javax.swing` type from
the platform classloader passes. Use `jdeps` (or a bytecode scan with an explicit allowlist) for
references, and extend both `check-sources` and `check-jars` to the new cluster directories
(`nbfx-libs`, `nbfx-api`, `nbfx-javafx`).

## A.6 Exemption register (format)

```
| file / module                | forbidden family      | reason (upstream API forces it) | owner | remove by |
|------------------------------|----------------------|--------------------------------|-------|-----------|
| nbfx.macos MacApplicationMenu| java.awt.* (Desktop)  | no FX API for the macOS app menu |  ?    | M4        |
| java.projecttype.java …Query | javax.swing.event.ChangeListener | headless query SPI | ? | when the SPI moves |
```

An expired exemption fails the build. No exemption for `javax.swing.text.Document` in any tier.

## A.7 What to test

- deny/allow import guard per module (compile-time, fast);
- `jdeps` reference scan on the built jars, per tier (M vs T2-FX vs T1);
- a source scan for `getDocument()`/`Document` (must find none);
- a "no UI type in an exemption" scan (A.2.3);
- the layer-path contract test (see `PLAN.md` §3.3);
- the module-graph test (no cycles, no upward layer edge; upstream reuse edges allowed).
