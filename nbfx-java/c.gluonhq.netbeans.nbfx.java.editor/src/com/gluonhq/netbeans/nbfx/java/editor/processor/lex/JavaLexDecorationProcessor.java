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
package com.gluonhq.netbeans.nbfx.java.editor.processor.lex;
import com.gluonhq.netbeans.nbfx.platform.editor.processor.lex.BaseLexDecorationProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gluonhq.netbeans.nbfx.platform.editor.decoration.LineDecoration;
import com.gluonhq.netbeans.nbfx.platform.editor.decoration.TokenCategory;
import com.gluonhq.netbeans.nbfx.platform.editor.processor.semantics.SourceContext;
import com.gluonhq.netbeans.nbfx.platform.editor.processor.semantics.TextPosResult;
import jfx.incubator.scene.control.richtext.TextPos;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;

/**
 * The Java lex decoration processor: it tokenizes a full Java source with the NetBeans
 * Java lexer and produces the {@link LineDecoration}s for its keywords, strings and
 * comments; brace matching walks the same token hierarchy. The source and its derived
 * artifacts are read from the {@link SourceContext} shared with the semantic processors.
 */
public class JavaLexDecorationProcessor extends BaseLexDecorationProcessor {

    private static final Logger LOG = Logger.getLogger(JavaLexDecorationProcessor.class.getName());

    /**
     * Java <em>contextual</em> keywords — they're lexed as {@link JavaTokenId#IDENTIFIER}
     * by the NetBeans Java lexer (no dedicated {@code JavaTokenId} entry) but should be
     * highlighted as keywords when they appear in a class/switch/method declaration.
     */
    private static final Set<String> CONTEXTUAL_KEYWORDS = Set.of(
            "sealed", "permits", "record", "yield");

    /** @param context the {@code JavaSourceContext} shared with the semantic processors */
    public JavaLexDecorationProcessor(SourceContext context) {
        super(context);
    }

    @Override
    protected List<List<LineDecoration>> analyze() {
        if (context.source() == null) {
            return new ArrayList<>();
        }
        int[] lineStarts = context.lineStarts();
        int[] lineLengths = context.lineLengths();
        int lineCount = lineStarts.length;

        // Initialize per-line lists
        List<List<LineDecoration>> results = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            results.add(new ArrayList<>());
        }

        TokenSequence<JavaTokenId> tokenSequence = context.hierarchy().tokenSequence(JavaTokenId.language());
        if (tokenSequence != null) {
            tokenSequence.moveStart();
            while (tokenSequence.moveNext()) {
                try {
                    Token<JavaTokenId> token = tokenSequence.token();
                    JavaTokenId id = token.id();
                    String category = id.primaryCategory();
                    TokenCategory tc = TokenCategory.fromCategory(category);
                    if (id == JavaTokenId.IDENTIFIER && CONTEXTUAL_KEYWORDS.contains(token.text().toString())) {
                        tc = TokenCategory.KEYWORD;
                    }
                    if (tc == null || tc.style() == null) {
                        continue;
                    }
                    int globalStart = tokenSequence.offset();
                    int globalEnd = globalStart + token.length();
                    Map<Integer, List<LineDecoration>> map = TextPosResult.toLineDecorationMap(
                            globalStart, globalEnd, lineStarts, lineLengths, tc.style());
                    map.forEach((line, list) -> results.get(line).addAll(list));
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Error tokenizing source", ex);
                }
            }
        }
        return results;
    }


    @Override
    public List<TextPosResult> findBraceMatchResults(TextPos caret) {
        String source = source();
        if (source == null || source.isEmpty() || caret == null) {
            return List.of();
        }
        BraceMatcher.BraceMatch bm = BraceMatcher.findMatch(context.hierarchy(), caretOffset(caret));
        if (bm == null) {
            return List.of();
        }
        int[] lineStarts = context.lineStarts();
        String style = bm.style();
        TextPosResult origin = TextPosResult.from(bm.origin(), bm.origin() + 1, lineStarts, style);
        if (!bm.matched()) {
            return List.of(origin);
        }
        TextPosResult match = TextPosResult.from(bm.match(), bm.match() + 1, lineStarts, style);
        return List.of(origin, match);
    }

}
