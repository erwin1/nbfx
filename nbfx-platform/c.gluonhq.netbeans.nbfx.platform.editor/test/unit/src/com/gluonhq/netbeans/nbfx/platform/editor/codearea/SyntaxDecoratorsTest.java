package com.gluonhq.netbeans.nbfx.platform.editor.codearea;

import java.io.IOException;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import static org.junit.Assert.assertTrue;

public class SyntaxDecoratorsTest {

    @Test
    public void unknownExtensionFallsBackToPlainText() throws IOException {
        FileObject file = FileUtil.createMemoryFileSystem().getRoot().createData("notes.unknown");

        assertTrue(SyntaxDecorators.forFile(file) instanceof PlainTextSyntaxDecorator);
    }
}
