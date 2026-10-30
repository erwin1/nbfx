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
package org.netbeans.modules.nbfx.api.editor;

/**
 * The caret position of a document, together with the size of its selection, as shown in the
 * status bar.
 * <p>
 * {@code row} and {@code column} are one-based, so they can be displayed as they are. When there
 * is no selection, {@code selectedRows} and {@code selectedColumns} are both zero.
 *
 * @param row             the one-based line the caret is on
 * @param column          the one-based column the caret is at
 * @param selectedRows    the number of lines the selection spans, or 0 when nothing is selected
 * @param selectedColumns the number of characters selected, or 0 when nothing is selected
 */
public record CaretInfo(int row, int column, int selectedRows, int selectedColumns) {

    /**
     * A caret at {@code row}:{@code column} with no selection.
     *
     * @param row    the one-based line the caret is on
     * @param column the one-based column the caret is at
     * @return the caret position without a selection
     */
    public static CaretInfo at(int row, int column) {
        return new CaretInfo(row, column, 0, 0);
    }

    /**
     * Whether a non-empty selection is held.
     *
     * @return {@code true} when at least one character is selected
     */
    public boolean hasSelection() {
        return selectedRows > 0 && selectedColumns > 0;
    }
}
