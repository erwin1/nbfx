package com.gluonhq.netbeans.nbfx.platform.editor.codearea;

import org.openide.filesystems.FileObject;

import java.util.Objects;

/**
 * Factory of the language-specific {@link BaseSyntaxDecorator}s, selected by the
 * file extension: CSS files get the lightweight CSS highlighter, XML/FXML files
 * the XML lexer highlighter, everything else the combined lex + Java analysis
 * decorator.
 */
public final class SyntaxDecorators {

    private SyntaxDecorators() {
    }

    /**
     * Creates the syntax decorator for the given file.
     *
     * @param fo the file being edited, never {@code null}
     * @return a new decorator matching the file's language
     */
    public static BaseSyntaxDecorator forFile(FileObject fo) {
        String ext = Objects.requireNonNull(fo).getExt().toLowerCase();
        for (SyntaxDecoratorFactory factory : org.openide.util.Lookup.getDefault().lookupAll(SyntaxDecoratorFactory.class)) {
            if (factory.supports(ext)) {
                return factory.create(fo);
            }
        }
        throw new IllegalStateException("No syntax decorator for ." + ext);
    }
}
