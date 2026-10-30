package com.gluonhq.netbeans.nbfx.ide.editor.css.usages;

import com.gluonhq.netbeans.nbfx.ide.editor.css.usages.CssUsageScanner.Symbol;
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
