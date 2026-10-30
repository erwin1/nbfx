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
package org.netbeans.modules.nbfx.java.usages.query;

import org.netbeans.api.java.source.CompilationInfo;

/** Conversions between editor offsets (one {@code '\n'} per line break) and javac offsets. */
final class JavaText {

    private JavaText() {
    }

    /**
     * A caret offset in the editor's {@code '\n'}-counted text, moved to the text {@code info}
     * compiles - the same text unless the file was read from disk with {@code \r\n} line breaks.
     */
    static int textOffset(CompilationInfo info, int offset) {
        String text = info.getText();
        if (text.indexOf('\r') < 0) {
            return offset;
        }
        int remaining = offset;
        int i = 0;
        while (i < text.length() && remaining > 0) {
            if (text.charAt(i) != '\r') {
                remaining--;
            }
            i++;
        }
        return i;
    }

    /** The offset in the editor's {@code '\n'}-counted text of {@code offset} in the compiled {@code text}. */
    static int editorOffset(CharSequence text, int offset) {
        int carriageReturns = 0;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\r') {
                carriageReturns++;
            }
        }
        return offset - carriageReturns;
    }
}
