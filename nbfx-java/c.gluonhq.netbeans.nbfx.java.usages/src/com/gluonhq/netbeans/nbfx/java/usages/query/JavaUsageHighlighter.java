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
package com.gluonhq.netbeans.nbfx.java.usages.query;

import com.gluonhq.netbeans.nbfx.api.usages.UsageHighlighter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/** Colours one line of Java source for the Usages view. */
@ServiceProvider(service = UsageHighlighter.class)
public final class JavaUsageHighlighter implements UsageHighlighter {

    /** Identifiers the lexer does not know as keywords but the editor colours as such. */
    private static final Set<String> CONTEXTUAL_KEYWORDS = Set.of("sealed", "permits", "record", "yield");

    @Override
    public boolean handles(FileObject file) {
        return file != null && "java".equalsIgnoreCase(file.getExt());
    }

    @Override
    public List<StyleRange> styles(FileObject file, String line) {
        List<StyleRange> result = new ArrayList<>();
        TokenSequence<JavaTokenId> sequence = TokenHierarchy.create(line, JavaTokenId.language())
                .tokenSequence(JavaTokenId.language());
        if (sequence == null) {
            return result;
        }
        sequence.moveStart();
        while (sequence.moveNext()) {
            Token<JavaTokenId> token = sequence.token();
            Style style = styleOf(token);
            if (style != null) {
                result.add(new StyleRange(sequence.offset(), sequence.offset() + token.length(), style));
            }
        }
        return result;
    }

    private static Style styleOf(Token<JavaTokenId> token) {
        JavaTokenId id = token.id();
        if (id == JavaTokenId.IDENTIFIER) {
            return CONTEXTUAL_KEYWORDS.contains(token.text().toString()) ? Style.KEYWORD : null;
        }
        return switch (id.primaryCategory()) {
            case "keyword", "keyword-directive" -> Style.KEYWORD;
            case "string", "character" -> Style.STRING;
            case "comment" -> Style.COMMENT;
            case "literal" -> Style.LITERAL;
            default -> null;
        };
    }
}
