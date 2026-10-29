package com.gluonhq.netbeans.nbfx.ide.editor.css.codearea;
import com.gluonhq.netbeans.nbfx.platform.editor.codearea.BaseSyntaxDecorator;

import com.gluonhq.netbeans.nbfx.platform.editor.processor.lex.BaseLexDecorationProcessor;
import com.gluonhq.netbeans.nbfx.ide.editor.css.processor.lex.CssLexDecorationProcessor;
import com.gluonhq.netbeans.nbfx.platform.editor.processor.semantics.SourceContext;
import org.openide.filesystems.FileObject;

/**
 * A {@link BaseSyntaxDecorator} for CSS sources. All the work — syntax highlighting,
 * brace matching and mark-occurrences of class/selector names — is done by the
 * lex-based defaults of the base class over a {@link CssLexDecorationProcessor}.
 */
public class CssSyntaxDecorator extends BaseSyntaxDecorator {

    public CssSyntaxDecorator(FileObject fo) {
        super(fo);
    }

    @Override
    protected BaseLexDecorationProcessor getLexProcessor(FileObject fo) {
        return new CssLexDecorationProcessor(new SourceContext(fo, null));
    }
}
