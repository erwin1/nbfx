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
package org.netbeans.modules.nbfx.ide.editor.css.usages;

import org.netbeans.modules.nbfx.ide.editor.css.usages.CssUsageScanner.Symbol;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Checks what {@link CssUsageScanner} offers Find Usages in a style sheet. */
public class CssUsageScannerTest {

    private static List<String> symbols(String text) {
        return CssUsageScanner.scan(text).stream()
                .map(symbol -> symbol.kind() + " " + symbol.text())
                .toList();
    }

    @Test
    public void readsClassesIdsAndColours() {
        String text = """
                .button, #root {
                    -fx-background-color: #ff8800;
                    -fx-text-fill: white;
                }
                """;
        assertEquals(List.of("CLASS .button", "ID #root", "COLOR #ff8800", "NAMED_COLOR white"), symbols(text));
    }

    @Test
    public void ignoresCommentsStringsAndDeclarationsWithoutColour() {
        String text = """
                /* .ignored { color: #fff; } */
                .real {
                    -fx-content: ".fake";
                    -fx-font-size: 12;
                }
                """;
        assertEquals(List.of("CLASS .real"), symbols(text));
    }

    @Test
    public void findsTheSymbolAtTheCaret() {
        String text = ".button { -fx-text-fill: #ff8800; }";
        List<Symbol> symbols = CssUsageScanner.scan(text);

        assertEquals("CLASS", CssUsageScanner.at(symbols, text.indexOf("button")).kind().name());
        assertEquals("COLOR", CssUsageScanner.at(symbols, text.indexOf("ff8800")).kind().name());
        assertNull(CssUsageScanner.at(symbols, text.indexOf("fill")));
    }
}
