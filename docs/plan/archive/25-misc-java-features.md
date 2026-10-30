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
# 25 — Miscellaneous Java basics (and explicit out-of-scope)

Steps: **Libraries manager** (`feature/libraries-manager`), **Module support** (`feature/module-support`),
**Internationalization** (`feature/i18n`), **JavaBeans** (`feature/beans`), **Task list & bookmarks**
(`feature/tasklist-bookmarks`), **Project groups** (`feature/project-groups`), **Plugins UI parity**
(`feature/plugins-ui`), **JShell** (`feature/jshell`), **Help decision**.

## Why

A last group of smaller but basic Java/IDE features, plus the explicit list of things the plan does **not**
cover so the scope is unambiguous.

## Research (original codebase)

| Feature | Module | Files / Swing | Verdict |
|---|---|---|---|
| **Libraries manager** — J2SE "Libraries" node, `LibraryManager`, library definitions (wrapper/JAR/library), Tools ▸ Libraries | `ide/project.libraries` | 27 / 3, **no forbidden deps** | **reuse directly** (headless, `org.netbeans.api.project.libraries.*`) |
| | `ide/project.libraries.ui` | 12 / 9 | **port** the library-manager dialog / chooser |
| **Module support** — module graph/dependency view, module-info editing/completion | `java/java.module.graph` (+ `java/java.j2semodule`, `java.source.base` module API) | 6 / 2 | **reuse/port** graph model; FX graph view; module-info completion partly exists (`JavaModuleInfoCompletionQueries`) |
| **i18n** — internationalization wizard, "Insert Internationalized String", i18n options | `java/i18n` | 46 / 28 | **port** UI, reuse logic (formats/refactoring of string literals) |
| **JavaBeans** — bean patterns, "Add Property", Beans node, BeanInfo editor | `java/beans` | 59 / 30 | **port**; reuse generator logic (`AddPropertyGenerator` etc.) |
| **Task List / ToDo** — task providers, filters, counts, options | `ide/tasklist.todo`, `ide/tasklist.ui`, `ide/spi.tasklist` | 10 / 3, 48 / 25 | reuse provider SPI; **port** the FX Task List view; ToDo options already in doc 14 |
| **Bookmarks** — toggle/next/prev, bookmarks list, key chooser | `ide/editor.bookmarks` | 30 / 19 | reuse `Bookmark`/`BookmarkList` model; port UI |
| **Project groups** — open/close a group of projects | `ide/projectui` | 94 / 81 | port the project-groups part (doc 02 covers the rest of projectui) |
| **Plugins UI parity** — install/update/remove, update centers, proxy, "Plugins" dialog | `ide/autoupdate.ui` (+ `nbfx.plugins` already exists) | many Swing | **extend `nbfx.plugins`** with the missing settings/update-center/proxy UI |
| **JShell** — Java shell console | `java/jshell.support` (+ JDK `jdk.jshell`, `lib.jshell.agent`) | 120 / 51 | reuse `jdk.jshell` + port the console UI (`nbfx.output`-hosted); also a good **terminal** consumer (doc 15) |
| **Help** — JavaHelp/F1 for non-Javadoc topics | `platform/javahelp` / `ide/javahelp` | — | **decision**: nbfx has no JavaHelp (doc 14); keep F1 = "Show Javadoc" only and document the absence of topic help |

## Target architecture (per item)

- **`nbfx.project.libraries(.ui)`** — reuse `LibraryManager`/`Library`; FX library manager (definitions,
  JARs, wrapper libraries), the "Libraries" node in the projects view and in the J2SE customizer
  (doc 02's Libraries tab), add-library to a project (writes `file.reference.*`/classpath entries).
- **Module support** — module-info completion/highlighting (extend `nbfx.editor`), the
  **module dependency graph** view (reuse `java.module.graph` model), modular project support in the
  build/run path (beyond the module-info template).
- **i18n** — Insert Internationalized String + the wizard (reuse `java/i18n` logic), i18n options.
- **JavaBeans** — bean patterns in the navigator/editor (property/event recognition), "Add Property"
  generator, BeanInfo handling (a JavaBeans node for `*.class`? keep minimal).
- **Task list/bookmarks** — FX views (task list with filters/count, bookmarks list, key chooser), status
  bar counts, options (doc 14).
- **Project groups** — open/close group, "recent groups", project-group manager.
- **Plugins UI** — extend `nbfx.plugins` with update-center management, proxy settings, install/update
  progress (docs 06/09), plugin settings/trust.
- **JShell** — a "Java Shell" view over `jdk.jshell` (snippets, completion, diagnostics), reusing the
  Output window for results and the terminal for `System.in`/`System.out` interactions; integrate with
  run configurations and the console stdin.

## Steps

1. **Libraries manager** (`feature/libraries-manager`) — biggest win for the least effort (no forbidden
   deps): reuse `project.libraries` + FX manager dialog + the Libraries node + customizer integration.
2. **Module support** (`feature/module-support`) — module-info completion/highlighting + the module graph view.
3. **Task list & bookmarks** (`feature/tasklist-bookmarks`) — FX views + providers + counts + options.
4. **i18n** (`feature/i18n`) — insert-i18n-string + wizard.
5. **JavaBeans** (`feature/beans`) — bean patterns + Add Property.
6. **Project groups** (`feature/project-groups`) — group manager.
7. **Plugins UI parity** (`feature/plugins-ui`) — update centers/proxy/progress in `nbfx.plugins`.
8. **JShell** (`feature/jshell`) — shell view (decide priority; shares the terminal/output stack).
9. **Help decision** — record "Javadoc-only help, no JavaHelp topics" (or port `javahelp` if required).

## Acceptance

- Libraries: add/remove libraries and library definitions; a J2SE project's Libraries tab and node reflect
  them; builds resolve the new classpath.
- Module-info completes and highlights; the module graph renders dependencies.
- Task list shows `TODO`/`FIXME` with filters and counts; bookmarks toggle/navigate and persist.
- i18n inserts keys/bundles; Beans "Add Property" generates the pattern.
- Plugins: install/update/remove and update centers work with progress; proxy settings persist.
- JShell evaluates snippets with completion and diagnostics.

## Explicitly out of scope (recorded, so nobody assumes it is planned)

Free-form projects and Eclipse/NetBeans project import; JavaEE/Jakarta EE, web (servlets/JSP/JS), JSF,
JPA/persistence, Spring, databases/servers, REST/web services; the Swing/JavaFX **form editor/palette**
(`form`/`form.kit`, dropped); Java ME/embedded/CVM; WebStart/JNLP (options round-trip only, doc 02);
`native-image`; Kotlin/Groovy/Scala/PHP/Ruby/C++/JavaScript languages; the module-development harness
beyond the apisupport project type (doc 02); JavaHelp topic help; the `nbfx`-external `jellytools`
test-suite tooling.

## Risks / open questions

- **Ordering**: these are independent; slot them after the intelligence/testing/VCS waves or as filler.
- **`project.libraries.ui`** and `java/i18n`/`java/beans` UIs are Swing-heavy — port only what is used.
- **JShell** vs the existing script/console features — decide whether it ships in the first pass (it shares
  the terminal, so it is cheap once doc 15 lands).
- **Plugins**: `autoupdate.services` is already a dependency; the missing part is the UI/settings surface.
