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
package com.gluonhq.netbeans.nbfx.ide.search.model;

/**
 * One occurrence of the search text in a file - NetBeans' {@code TextDetail}. Offsets count
 * characters in the file's text with {@code '\n'} line separators (the editor's convention);
 * {@code line} and {@code column} are 1-based, as shown to the user.
 *
 * @param start     the offset of the first matched character
 * @param end       the offset after the last matched character
 * @param line      the line the match starts on
 * @param column    the column the match starts at, in that line
 * @param lineStart the offset of that line's first character
 * @param lineText  the text of that line, without its separator
 */
public record TextMatch(int start, int end, int line, int column, int lineStart, String lineText) {

    public int length() {
        return end - start;
    }

    /** Whether the match continues past the end of its first line. */
    public boolean isMultiline() {
        return end > lineStart + lineText.length();
    }
}
