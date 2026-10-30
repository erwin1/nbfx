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
package org.netbeans.modules.nbfx.api.completion;

import org.netbeans.modules.nbfx.api.elements.SourceElementKind;
import org.netbeans.modules.nbfx.api.elements.SourceTypeKind;

import java.util.Objects;

/**
 * Lightweight immutable {@link CompletionItem} implementation.
 */
public record SimpleCompletionItem(String label, String insertText, String sortText,
                                   int sortPriority,
                                   String leftText, String rightText,
                                   SourceElementKind kind, SourceTypeKind typeKind,
                                   int modifiers,
                                   boolean emphasized, boolean deprecated,
                                   String qualifiedName) implements CompletionItem {

    public SimpleCompletionItem {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(insertText, "insertText must not be null");
        sortText = sortText == null ? label : sortText;
        leftText = leftText == null ? label : leftText;
        rightText = rightText == null ? "" : rightText;
        kind = kind == null ? SourceElementKind.OTHER : kind;
        typeKind = typeKind == null ? SourceTypeKind.OTHER : typeKind;
        qualifiedName = qualifiedName == null ? "" : qualifiedName;
    }

    /**
     * Convenience constructor for items that carry no importable {@linkplain #qualifiedName()
     * fully-qualified name}.
     */
    public SimpleCompletionItem(String label, String insertText, String sortText,
                                int sortPriority,
                                String leftText, String rightText,
                                SourceElementKind kind, SourceTypeKind typeKind,
                                int modifiers,
                                boolean emphasized, boolean deprecated) {
        this(label, insertText, sortText, sortPriority, leftText, rightText,
                kind, typeKind, modifiers, emphasized, deprecated, "");
    }

    /**
     * @return weighted left/right text length estimate, based on different font sizes
     */
    @Override
    public double length() {
        return (13d * leftText().length() + 11d * rightText().length()) / (13d * 11d);
    }
}

