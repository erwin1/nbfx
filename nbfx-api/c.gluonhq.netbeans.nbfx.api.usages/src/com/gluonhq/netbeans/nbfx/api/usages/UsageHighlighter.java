package com.gluonhq.netbeans.nbfx.api.usages;

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
