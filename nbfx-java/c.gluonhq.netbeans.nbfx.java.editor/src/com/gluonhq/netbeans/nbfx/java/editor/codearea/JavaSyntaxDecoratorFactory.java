package com.gluonhq.netbeans.nbfx.java.editor.codearea;

import com.gluonhq.netbeans.nbfx.platform.editor.codearea.BaseSyntaxDecorator;
import com.gluonhq.netbeans.nbfx.platform.editor.codearea.SyntaxDecoratorFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SyntaxDecoratorFactory.class)
public final class JavaSyntaxDecoratorFactory implements SyntaxDecoratorFactory {

    @Override
    public boolean supports(String extension) {
        return "java".equals(extension);
    }

    @Override
    public BaseSyntaxDecorator create(FileObject file) {
        return new JavaSyntaxDecorator(file);
    }
}
