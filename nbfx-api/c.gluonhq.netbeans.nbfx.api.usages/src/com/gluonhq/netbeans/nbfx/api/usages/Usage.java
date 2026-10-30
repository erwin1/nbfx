package com.gluonhq.netbeans.nbfx.api.usages;

import org.openide.filesystems.FileObject;

/**
 * One occurrence found by a query. Offsets count characters of the file's text with one
 * {@code '\n'} per line break, the convention of the editor API
 * ({@code EditorDocument.selectRange}), whatever the line separator on disk.
 *
 * @param file       the file, always the one on disk (never an in-memory substitute)
 * @param start      offset of the occurrence's name
 * @param end        end offset (exclusive) of the occurrence's name
 * @param line       1-based line of {@code start}
 * @param lineStart  offset of the first non-blank character of that line, where {@link #lineText}
 *                   begins - so {@code start - lineStart} is the occurrence's column in it
 * @param lineText   the trimmed text of the line (of the lines, when the occurrence spans several)
 * @param kind       what the occurrence is: a reference, a variable access, an import, a comment, ...
 * @param inTestRoot the file lives under a test source root
 * @param context    where the occurrence lives
 */
public record Usage(FileObject file, int start, int end, int line, int lineStart, String lineText,
        UsageKind kind, boolean inTestRoot, UsageContext context) {

    /** The occurrence's text as it appears in {@link #lineText}, when the span lies within it. */
    public String occurrence() {
        int from = start - lineStart;
        int to = end - lineStart;
        return from >= 0 && to <= lineText.length() && from <= to ? lineText.substring(from, to) : "";
    }
}
