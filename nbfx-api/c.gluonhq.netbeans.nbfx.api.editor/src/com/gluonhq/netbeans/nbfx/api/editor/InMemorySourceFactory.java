package com.gluonhq.netbeans.nbfx.api.editor;

import java.io.IOException;
import org.openide.filesystems.FileObject;

/**
 * Provides in-memory copies of source files carrying unsaved editor text, so Java semantic
 * analyses see edits that have not yet been written to disk.
 */
public interface InMemorySourceFactory {

    /**
     * A memory {@link FileObject} named and placed like {@code original}, holding {@code text}.
     */
    FileObject copy(FileObject original, String text) throws IOException;
}
