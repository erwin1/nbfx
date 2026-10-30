package com.gluonhq.netbeans.nbfx.platform.editor.codearea;

import com.gluonhq.netbeans.nbfx.platform.editor.decoration.LineDecoration;
import com.gluonhq.netbeans.nbfx.platform.editor.processor.lex.BaseLexDecorationProcessor;
import com.gluonhq.netbeans.nbfx.platform.editor.processor.semantics.SourceContext;
import java.util.List;
import org.openide.filesystems.FileObject;

/**
 * Syntax decorator for files without a registered language: it adds no token, semantic or
 * caret decorations. Used when no {@link SyntaxDecoratorFactory} supports the file extension.
 */
public final class PlainTextSyntaxDecorator extends BaseSyntaxDecorator {

    public PlainTextSyntaxDecorator(FileObject file) {
        super(file);
    }

    @Override
    protected BaseLexDecorationProcessor getLexProcessor(FileObject file) {
        return new BaseLexDecorationProcessor(new SourceContext(file, null)) {
            @Override
            protected List<List<LineDecoration>> analyze() {
                return List.of();
            }
        };
    }
}
