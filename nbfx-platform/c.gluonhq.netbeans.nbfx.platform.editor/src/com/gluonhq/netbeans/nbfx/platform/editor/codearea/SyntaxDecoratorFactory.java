package com.gluonhq.netbeans.nbfx.platform.editor.codearea;

import org.openide.filesystems.FileObject;

/** Creates the syntax decorator for one editor language. */
public interface SyntaxDecoratorFactory {

    boolean supports(String extension);

    BaseSyntaxDecorator create(FileObject file);
}
