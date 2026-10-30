package com.gluonhq.netbeans.nbfx.java.usages.query;

import com.gluonhq.netbeans.nbfx.api.usages.UsageHighlighter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/** Colours one line of Java source for the Usages view. */
@ServiceProvider(service = UsageHighlighter.class)
public final class JavaUsageHighlighter implements UsageHighlighter {

    /** Identifiers the lexer does not know as keywords but the editor colours as such. */
    private static final Set<String> CONTEXTUAL_KEYWORDS = Set.of("sealed", "permits", "record", "yield");

    @Override
    public boolean handles(FileObject file) {
        return file != null && "java".equalsIgnoreCase(file.getExt());
    }

    @Override
    public List<StyleRange> styles(FileObject file, String line) {
        List<StyleRange> result = new ArrayList<>();
        TokenSequence<JavaTokenId> sequence = TokenHierarchy.create(line, JavaTokenId.language())
                .tokenSequence(JavaTokenId.language());
        if (sequence == null) {
            return result;
        }
        sequence.moveStart();
        while (sequence.moveNext()) {
            Token<JavaTokenId> token = sequence.token();
            Style style = styleOf(token);
            if (style != null) {
                result.add(new StyleRange(sequence.offset(), sequence.offset() + token.length(), style));
            }
        }
        return result;
    }

    private static Style styleOf(Token<JavaTokenId> token) {
        JavaTokenId id = token.id();
        if (id == JavaTokenId.IDENTIFIER) {
            return CONTEXTUAL_KEYWORDS.contains(token.text().toString()) ? Style.KEYWORD : null;
        }
        return switch (id.primaryCategory()) {
            case "keyword", "keyword-directive" -> Style.KEYWORD;
            case "string", "character" -> Style.STRING;
            case "comment" -> Style.COMMENT;
            case "literal" -> Style.LITERAL;
            default -> null;
        };
    }
}
