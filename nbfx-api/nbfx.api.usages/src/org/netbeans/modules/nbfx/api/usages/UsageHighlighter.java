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
package org.netbeans.modules.nbfx.api.usages;

import java.util.List;
import org.openide.filesystems.FileObject;

/**
 * Optional language support for colouring one line of a usage result. The Usages view stays
 * language-neutral: a provider returns neutral style spans and the view maps them to its CSS
 * classes.
 */
public interface UsageHighlighter {

    /** Whether this highlighter knows how to style {@code file}. */
    boolean handles(FileObject file);

    /**
     * The styled spans of {@code line}; offsets are relative to the line. Spans may overlap; the
     * renderer resolves the innermost one. Return an empty list for plain text.
     */
    List<StyleRange> styles(FileObject file, String line);

    /** One styled span of a line. */
    record StyleRange(int start, int end, Style style) {
    }

    /** The neutral styles the view knows how to draw. */
    enum Style {
        KEYWORD,
        STRING,
        COMMENT,
        LITERAL,
        TEXT
    }
}
