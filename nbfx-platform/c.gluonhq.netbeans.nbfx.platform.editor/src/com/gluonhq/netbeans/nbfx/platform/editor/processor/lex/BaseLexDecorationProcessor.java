/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gluonhq.netbeans.nbfx.platform.editor.processor.lex;

import com.gluonhq.netbeans.nbfx.platform.editor.decoration.LineDecoration;
import com.gluonhq.netbeans.nbfx.platform.editor.processor.semantics.SourceContext;
import com.gluonhq.netbeans.nbfx.platform.editor.processor.semantics.TextPosResult;
import jfx.incubator.scene.control.richtext.TextPos;

import java.util.List;
import java.util.Objects;

/**
 * Base class of the language-specific lex decoration processors: each one tokenizes a
 * full text source and produces the {@link LineDecoration}s for its tokens, cached per
 * line and lazily rebuilt after {@link #invalidate()}.
 *
 * <p>The source text and its lexical artifacts (line starts / lengths, token hierarchy)
 * live in the {@link SourceContext} the processor is created with — the Java processor
 * shares a {@code JavaSourceContext} with the semantic processors, the standalone
 * processors (CSS, XML) own a plain one.</p>
 *
 * <p>Brace matching walks the context's token hierarchy with the {@link BraceMatcher}
 * (no results for languages without a Java-lexer hierarchy); mark occurrences return no
 * results until a subclass overrides them.</p>
 */
public abstract class BaseLexDecorationProcessor {

    protected final SourceContext context;

    private List<List<LineDecoration>> cachedLines;

    protected BaseLexDecorationProcessor(SourceContext context) {
        this.context = Objects.requireNonNull(context);
    }

    /** Returns the {@link SourceContext} holding the source and its lexical artifacts. */
    public final SourceContext context() {
        return context;
    }

    /**
     * Updates the cached source. When the new text differs from the currently cached
     * one, every derived artifact is nullified and rebuilt on next access.
     *
     * @param newSource the new source text, or {@code null} to invalidate
     */
    public final void setSource(String newSource) {
        if (Objects.equals(context.source(), newSource)) {
            return;
        }
        context.setSource(newSource);
        cachedLines = null;
        onInvalidated();
    }

    /** Clears the cached source and every derived artifact. Called when the document content changes. */
    public final void invalidate() {
        context.invalidate();
        cachedLines = null;
        onInvalidated();
    }

    /** Returns the source of the last scan, or {@code null} after {@link #invalidate()}. */
    public final String source() {
        return context.source();
    }

    /**
     * Returns the cached {@link LineDecoration}s for the given paragraph index.
     * If the cache is empty, the full source is re-analyzed first.
     *
     * @param lineIndex  the paragraph index
     * @return decorations for the line, or an empty list if out of range
     */
    public final List<LineDecoration> getLineDecorations(int lineIndex) {
        ensureAnalyzed();
        if (lineIndex < 0 || lineIndex >= cachedLines.size()) {
            return List.of();
        }
        return cachedLines.get(lineIndex);
    }

    /**
     * Finds the matching brace pair for the brace token adjacent to the caret, walking
     * the context's token hierarchy; no results for languages whose hierarchy has no
     * Java token sequence (e.g. XML).
     *
     * @param caret the current caret position in document coordinates
     * @return 0, 1 (unmatched origin) or 2 (origin + match) single-character
     *         {@link TextPosResult}s styled with the brace-match / brace-mismatch style
     */
    public List<TextPosResult> findBraceMatchResults(TextPos caret) {
        return List.of();
    }

    /**
     * Finds every occurrence of the name at the caret; no results for languages
     * whose occurrences are computed elsewhere (or not at all).
     *
     * @param caret the current caret position in document coordinates
     * @return the occurrence highlight ranges, or an empty list
     */
    public List<TextPosResult> findOccurrenceResults(TextPos caret) {
        return List.of();
    }

    /** Converts a caret position into a global character offset. */
    protected final int caretOffset(TextPos caret) {
        String source = context.source();
        int[] lineStarts = context.lineStarts();
        int lineStart = caret.index() < lineStarts.length
                ? lineStarts[caret.index()] : source.length();
        return Math.min(lineStart + caret.offset(), source.length());
    }

    /** Rebuilds the per-line decoration cache when it was invalidated. */
    protected final void ensureAnalyzed() {
        if (cachedLines == null) {
            cachedLines = analyze();
        }
    }

    /** Tokenizes the full source and returns the text-style decorations grouped by line. */
    protected abstract List<List<LineDecoration>> analyze();

    /** Invalidation hook for the subclass' derived artifacts (selector ranges...). */
    protected void onInvalidated() {
    }
}
