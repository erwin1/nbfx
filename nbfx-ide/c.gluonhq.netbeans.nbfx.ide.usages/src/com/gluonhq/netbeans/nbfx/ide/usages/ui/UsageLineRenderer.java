package com.gluonhq.netbeans.nbfx.ide.usages.ui;

import com.gluonhq.netbeans.nbfx.api.usages.Usage;
import com.gluonhq.netbeans.nbfx.api.usages.UsageHighlighter;
import com.gluonhq.netbeans.nbfx.api.usages.UsageKind;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Splits the line of a usage into styled {@link Run}s: the line number, then the line's text
 * coloured by the registered {@link UsageHighlighter} (if any; style classes {@code usage-keyword},
 * {@code usage-string}, {@code usage-comment}) with the occurrence in bold
 * ({@code usage-occurrence}), as NetBeans' Find Usages window shows it. The runs are plain data:
 * {@link UsageTreeCell} draws them with the text nodes it reuses from row to row.
 */
final class UsageLineRenderer {

    /**
     * One run of a line, with one style.
     *
     * @param text       the run's text, never empty
     * @param styleClass the style class of its colour
     * @param occurrence whether it is (part of) the occurrence, shown in bold
     */
    record Run(String text, String styleClass, boolean occurrence) {
    }

    private UsageLineRenderer() {
    }

    static List<Run> runs(Usage usage) {
        String line = usage.lineText();
        int from = clamp(usage.start() - usage.lineStart(), 0, line.length());
        int to = clamp(usage.end() - usage.lineStart(), from, line.length());
        int lead = 0;
        while (lead < from && Character.isWhitespace(line.charAt(lead))) {
            lead++;
        }
        int trail = line.length();
        while (trail > to && Character.isWhitespace(line.charAt(trail - 1))) {
            trail--;
        }

        List<Run> flow = new ArrayList<>();
        addText(flow, usage.line() + ": ", "usage-line-number", false);

        // A usage inside a block comment: the line alone would not lex as a comment.
        if (usage.kind() == UsageKind.COMMENT) {
            addText(flow, line.substring(lead, from), "usage-comment", false);
            addText(flow, line.substring(from, to), "usage-comment", true);
            addText(flow, line.substring(to, trail), "usage-comment", false);
            return flow;
        }

        List<UsageHighlighter.StyleRange> ranges = styles(usage.file(), line);
        if (ranges.isEmpty()) {
            addText(flow, line.substring(lead, from), "usage-text", false);
            addText(flow, line.substring(from, to), "usage-text", true);
            addText(flow, line.substring(to, trail), "usage-text", false);
            return flow;
        }

        // Split at every style boundary and at the occurrence's, so each run has one style.
        TreeSet<Integer> cuts = new TreeSet<>(List.of(lead, from, to, trail));
        for (UsageHighlighter.StyleRange range : ranges) {
            if (range.end() > lead && range.start() < trail) {
                cuts.add(clamp(range.start(), lead, trail));
                cuts.add(clamp(range.end(), lead, trail));
            }
        }
        Integer previous = null;
        for (int cut : cuts) {
            if (previous != null && cut > previous) {
                String style = "usage-text";
                for (UsageHighlighter.StyleRange range : ranges) {
                    if (range.start() <= previous && range.end() >= cut) {
                        style = styleClass(range.style());
                        break;
                    }
                }
                addText(flow, line.substring(previous, cut), style, previous >= from && cut <= to);
            }
            previous = cut;
        }
        return flow;
    }

    private static List<UsageHighlighter.StyleRange> styles(FileObject file, String line) {
        for (UsageHighlighter highlighter : Lookup.getDefault().lookupAll(UsageHighlighter.class)) {
            if (highlighter.handles(file)) {
                return highlighter.styles(file, line);
            }
        }
        return List.of();
    }

    private static String styleClass(UsageHighlighter.Style style) {
        return switch (style) {
            case KEYWORD, LITERAL -> "usage-keyword";
            case STRING -> "usage-string";
            case COMMENT -> "usage-comment";
            case TEXT -> "usage-text";
        };
    }

    private static void addText(List<Run> flow, String content, String styleClass, boolean occurrence) {
        if (!content.isEmpty()) {
            flow.add(new Run(content, styleClass, occurrence));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
