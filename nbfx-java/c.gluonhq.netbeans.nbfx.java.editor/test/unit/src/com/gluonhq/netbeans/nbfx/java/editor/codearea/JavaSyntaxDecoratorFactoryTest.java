package com.gluonhq.netbeans.nbfx.java.editor.codearea;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JavaSyntaxDecoratorFactoryTest {

    private final JavaSyntaxDecoratorFactory factory = new JavaSyntaxDecoratorFactory();

    @Test
    public void supportsOnlyJava() {
        assertTrue(factory.supports("java"));
        assertFalse(factory.supports("txt"));
        assertFalse(factory.supports("css"));
        assertFalse(factory.supports("xml"));
        assertFalse(factory.supports("fxml"));
    }
}
