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
# 22 — Javadoc viewing & completion

Steps: **Show Javadoc** (`feature/javadoc-view`), **Javadoc index & completion** (`feature/javadoc-index`).

## Why

"Show Javadoc" (F1/context), the Javadoc window and Javadoc completion are part of the basic Java editing
experience; today only the Javadoc *build* action and options exist in the plan (doc 02).

## What nbfx has today

The Javadoc **build** action (Ant/Maven/Gradle) and Documenting options (doc 02); `java.source.base`'s
`DocTreePathHandle`/`DocTrees` for reading doc comments; no viewer and no completion.

## Research (original codebase)

| Module | Files / Swing | Role | Verdict |
|---|---|---|---|
| `java/javadoc` | 39 / 16 | "Show Javadoc" action/view (`org.netbeans.modules.javadoc.*`), javadoc **index** over platform/sources, `JavadocGenerator` (insert comments), javadoc hints | **reuse** the index/generator logic; **port** the viewer/vendor URL UI |
| `java.source.base` | dep | `DocTreePathHandle`, `DocTrees`, element doc resolution | reuse (headless) |
| `java/java.editor` | — | Javadoc **completion** (`JavadocCompletion`) | **port** to the nbfx completion provider |

## Target architecture

- **`nbfx.javadoc`** (headless): `JavadocResolver` — element → doc URL/content (sources from
  `ClassPath.SOURCE`/`BOOT`, `DocTreePathHandle`, platform javadoc roots); `JavadocIndex` over platform
  javadoc (reuse `java/javadoc` index logic) for search/type-ahead; `JavadocCommentGenerator` (reuse
  `JavadocGenerator`) for inserting comments.
- **UI (FX)**: `ShowJavadocPopup`/window (rendered HTML from doc comments or the javadoc URL; opening the
  external browser when only a URL exists), actions on editor caret / navigator selection / API element /
  usages/hierarchy views, and the "Javadoc window" (dockable, follows selection) + search box.
- **Completion**: a Javadoc completion contributor in `nbfx.editor` (tags, `@param`/`@return`/@throws`,
  element names, HTML tags), plus **insert Javadoc comment** (shared with doc 21).

## Steps

1. **Show Javadoc** (`feature/javadoc-view`) — `JavadocResolver` + FX popup/window + actions in the editor
   context menu, `Navigate`/`Help` menus, and the navigator/views; keyboard F1; "Show Javadoc in Browser".
2. **Index & completion** (`feature/javadoc-index`) — the javadoc index (reuse `java/javadoc` logic) with
   the Javadoc window's search/type-ahead, and the editor's Javadoc completion contributor; platform
   sources/javadoc roots come from Java Platforms (doc 02).

## Acceptance

- F1 on an editor element shows its doc comment; on a library element shows the JDK javadoc (or the URL).
- The Javadoc window follows selection and supports search.
- Javadoc completion offers tags/params/`@throws` and HTML; insert-Javadoc generates a complete comment.
- Headless tests for doc resolution/index; no FX in the resolver.

## Risks / open questions

- **Doc availability**: platform javadoc/sources may be missing — fall back to a URL/browser and a clear
  message; nbfx has no JavaHelp, so "Help" for other topics stays out (documented in doc 25).
- **Rendering**: doc comments are HTML; use a JavaFX `WebView` (javafx.web) or a limited HTML renderer —
  decide (WebView pulls `javafx.web`; a light renderer avoids it).
- **Index warm-up** for large JDK javadocs; build lazily per platform root.
- **`java/javadoc` module** pulls Swing — reuse only headless classes or port them.
