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
package com.gluonhq.netbeans.nbfx.editor.codearea;

import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * Conversions between a {@link StyledTextModel}'s paragraph/column positions and character offsets
 * from the start of the content, counting one character per line break regardless of the line
 * separator - the offsets of a {@code javax.swing.text.Document} and of the NetBeans source model
 * ({@code TreePathHandle}, {@code PositionBounds}).
 */
public final class TextOffsets {

    private TextOffsets() {
    }

    /** The offset of {@code pos}, clamped to the content; {@code 0} for {@code null}. */
    public static int offsetOf(StyledTextModel model, TextPos pos) {
        if (pos == null) {
            return 0;
        }
        int count = model.size();
        int paragraph = Math.clamp(pos.index(), 0, Math.max(0, count - 1));
        int offset = 0;
        for (int i = 0; i < paragraph; i++) {
            offset += model.getPlainText(i).length() + 1;
        }
        int length = count == 0 ? 0 : model.getPlainText(paragraph).length();
        return offset + Math.clamp(pos.offset(), 0, length);
    }

    /** The position at {@code offset}, clamped to the content. */
    public static TextPos positionOf(StyledTextModel model, int offset) {
        int count = model.size();
        int remaining = Math.max(0, offset);
        for (int i = 0; i < count; i++) {
            int length = model.getPlainText(i).length();
            if (remaining <= length) {
                return TextPos.ofLeading(i, remaining);
            }
            remaining -= length + 1;
        }
        return count == 0 ? TextPos.ZERO : model.getDocumentEnd();
    }

    /** The whole content with paragraphs joined by {@code '\n'}, i.e. the text these offsets index. */
    public static String text(StyledTextModel model) {
        StringBuilder sb = new StringBuilder();
        int count = model.size();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(model.getPlainText(i));
        }
        return sb.toString();
    }
}
