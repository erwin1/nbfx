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
package org.netbeans.modules.nbfx.api.breadcrumbs;

import org.netbeans.modules.nbfx.api.elements.SourceElementKind;
import org.netbeans.modules.nbfx.api.elements.SourceTypeKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable-by-construction {@link BreadcrumbElement} implementation.
 *
 * <p>Providers build the tree top-down: create the root with
 * {@link #root(String, int, int, SourceElementKind, SourceTypeKind, int)} and append nested
 * elements with {@link #addChild(String, int, int, SourceElementKind, SourceTypeKind, int)},
 * which wires the parent link and keeps children in insertion order. Once built, the tree is
 * safe to publish across threads as long as it is no longer mutated.</p>
 */
public final class SimpleBreadcrumbElement implements BreadcrumbElement {

    private final String label;
    private final int startOffset;
    private final int endOffset;
    private final SourceElementKind kind;
    private final SourceTypeKind typeKind;
    private final int modifiers;
    private final SimpleBreadcrumbElement parent;
    private final List<BreadcrumbElement> children = new ArrayList<>();

    private SimpleBreadcrumbElement(String label, int startOffset, int endOffset,
                                    SourceElementKind kind, SourceTypeKind typeKind,
                                    int modifiers, SimpleBreadcrumbElement parent) {
        this.label = Objects.requireNonNull(label, "label must not be null");
        if (startOffset < 0) {
            throw new IllegalArgumentException("startOffset out of bounds: " + startOffset);
        }
        if (endOffset < startOffset) {
            throw new IllegalArgumentException("endOffset before startOffset: " + endOffset);
        }
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.kind = kind == null ? SourceElementKind.OTHER : kind;
        this.typeKind = typeKind == null ? SourceTypeKind.OTHER : typeKind;
        this.modifiers = modifiers;
        this.parent = parent;
    }

    /**
     * Creates a parentless root element.
     *
     * @param label element label
     * @param startOffset start offset from document start
     * @param endOffset end offset from document start
     * @param kind semantic category, {@code null} defaults to {@link SourceElementKind#OTHER}
     * @param typeKind type hint, {@code null} defaults to {@link SourceTypeKind#OTHER}
     * @param modifiers modifier bit mask, compatible with {@link java.lang.reflect.Modifier}
     * @return new root element
     */
    public static SimpleBreadcrumbElement root(String label, int startOffset, int endOffset,
                                               SourceElementKind kind, SourceTypeKind typeKind,
                                               int modifiers) {
        return new SimpleBreadcrumbElement(label, startOffset, endOffset, kind, typeKind, modifiers, null);
    }

    /**
     * Creates a new element nested under this one and appends it to {@link #children()}.
     *
     * @param label element label
     * @param startOffset start offset from document start
     * @param endOffset end offset from document start
     * @param kind semantic category, {@code null} defaults to {@link SourceElementKind#OTHER}
     * @param typeKind type hint, {@code null} defaults to {@link SourceTypeKind#OTHER}
     * @param modifiers modifier bit mask, compatible with {@link java.lang.reflect.Modifier}
     * @return the newly created child element
     */
    public SimpleBreadcrumbElement addChild(String label, int startOffset, int endOffset,
                                            SourceElementKind kind, SourceTypeKind typeKind,
                                            int modifiers) {
        SimpleBreadcrumbElement child =
                new SimpleBreadcrumbElement(label, startOffset, endOffset, kind, typeKind, modifiers, this);
        children.add(child);
        return child;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public int startOffset() {
        return startOffset;
    }

    @Override
    public int endOffset() {
        return endOffset;
    }

    @Override
    public BreadcrumbElement parent() {
        return parent;
    }

    @Override
    public List<BreadcrumbElement> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public SourceElementKind kind() {
        return kind;
    }

    @Override
    public SourceTypeKind typeKind() {
        return typeKind;
    }

    @Override
    public int modifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        return "SimpleBreadcrumbElement[" + label + ", " + startOffset + "-" + endOffset + ", " + kind + "]";
    }
}
