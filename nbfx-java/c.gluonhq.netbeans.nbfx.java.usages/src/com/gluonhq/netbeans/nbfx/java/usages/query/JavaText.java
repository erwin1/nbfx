package com.gluonhq.netbeans.nbfx.java.usages.query;

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
