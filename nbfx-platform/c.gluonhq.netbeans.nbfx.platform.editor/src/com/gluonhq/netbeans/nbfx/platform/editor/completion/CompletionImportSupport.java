package com.gluonhq.netbeans.nbfx.platform.editor.completion;

import java.util.Optional;

/** Language support for adding a missing import when a completion item is committed. */
public interface CompletionImportSupport {

    /** A single text insertion: {@code text} goes in at global character {@code offset}. */
    record ImportEdit(int offset, String text) {}

    Optional<ImportEdit> computeImport(String source, String fullyQualifiedName);
}
