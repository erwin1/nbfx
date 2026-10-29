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
package com.gluonhq.netbeans.nbfx.api.breadcrumbs;

import com.gluonhq.netbeans.nbfx.api.elements.SourceElementKind;
import com.gluonhq.netbeans.nbfx.api.elements.SourceTypeKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One node of a breadcrumbs path shown at the bottom of a code editor.
 *
 * <p>Elements form a tree that mirrors the structural nesting of the source file
 * (for instance {@code class → method → if statement}). Providers hand the editor the
 * <em>deepest</em> element that encloses the caret; the editor reconstructs the visible
 * path by walking {@link #parent()} up to the root.</p>
 *
 * <p>Icon rendering reuses the completion vocabulary: {@link #kind()}, {@link #typeKind()}
 * and {@link #modifiers()} carry the same semantics as
 * {@link com.gluonhq.netbeans.nbfx.api.completion.CompletionItem}.</p>
 */
public interface BreadcrumbElement {

    /**
     * Text shown for this element in the breadcrumbs bar and its popup.
     *
     * @return element label
     */
    String label();

    /**
     * Document offset of the beginning of this element. This is the caret
     * target when the user navigates to the element.
     *
     * @return start offset from document start
     */
    int startOffset();

    /**
     * Document offset of the end of this element. Together with
     * {@link #startOffset()} it defines the span used to locate the element
     * enclosing a caret position.
     *
     * @return end offset from document start
     */
    int endOffset();

    /**
     * Enclosing element, or {@code null} when this element is the root of the path.
     *
     * @return parent element, defaults to {@code null}
     */
    default BreadcrumbElement parent() {
        return null;
    }

    /**
     * Nested elements in source order, shown in the popup that opens when the
     * user right-clicks this element.
     *
     * @return ordered child elements, defaults to an empty list
     */
    default List<BreadcrumbElement> children() {
        return List.of();
    }

    /**
     * General semantic category used for icon/styling.
     *
     * @return element kind, defaults to {@link SourceElementKind#OTHER}
     */
    default SourceElementKind kind() {
        return SourceElementKind.OTHER;
    }

    /**
     * Detailed type hint when {@link #kind()} is {@link SourceElementKind#TYPE}.
     *
     * @return type kind, defaults to {@link SourceTypeKind#OTHER}
     */
    default SourceTypeKind typeKind() {
        return SourceTypeKind.OTHER;
    }

    /**
     * Modifier flags, compatible with {@link java.lang.reflect.Modifier}.
     *
     * @return modifier bit mask
     */
    default int modifiers() {
        return 0;
    }

    /**
     * Builds the breadcrumbs path ending at this element by walking
     * {@link #parent()} up to the root.
     *
     * @return path from root (first) to this element (last), never empty
     */
    default List<BreadcrumbElement> path() {
        List<BreadcrumbElement> path = new ArrayList<>();
        for (BreadcrumbElement element = this; element != null; element = element.parent()) {
            path.add(element);
        }
        Collections.reverse(path);
        return path;
    }
}
