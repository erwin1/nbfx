package com.gluonhq.netbeans.nbfx.ide.editor.css.codearea;

import com.gluonhq.netbeans.nbfx.platform.editor.codearea.BaseSyntaxDecorator;
import com.gluonhq.netbeans.nbfx.platform.editor.codearea.SyntaxDecoratorFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SyntaxDecoratorFactory.class)
public final class CssSyntaxDecoratorFactory implements SyntaxDecoratorFactory {

    @Override
    public boolean supports(String extension) {
        return "css".equals(extension);
    }

    @Override
    public BaseSyntaxDecorator create(FileObject file) {
        return new CssSyntaxDecorator(file);
    }
}
