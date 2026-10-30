package com.gluonhq.netbeans.nbfx.api.usages;

import java.io.IOException;
import org.openide.filesystems.FileObject;

/** Resolves a language-specific Find Usages target. */
public interface UsagesProvider {

    boolean handles(FileObject file);

    SearchTarget targetAt(FileObject file, int offset, UsageSources sources) throws IOException;

    /**
     * Whether {@link #topLevelTarget} can resolve a target for {@code file}. A provider that has no
     * top-level element to search, such as a style sheet, leaves this false.
     */
    default boolean handlesTopLevel(FileObject file) {
        return false;
    }

    SearchTarget topLevelTarget(FileObject file, UsageSources sources) throws IOException;
}
