package com.gluonhq.netbeans.nbfx.java.editor.completion.support;

import com.gluonhq.netbeans.nbfx.platform.editor.completion.CompletionImportSupport;
import java.util.Optional;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = CompletionImportSupport.class)
public final class JavaImportSupport implements CompletionImportSupport {

    @Override
    public Optional<ImportEdit> computeImport(String source, String fullyQualifiedName) {
        return JavaImportUtils.computeImport(source, fullyQualifiedName)
                .map(edit -> new ImportEdit(edit.offset(), edit.text()));
    }
}
