package com.gluonhq.netbeans.nbfx.api.usages;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.openide.filesystems.FileObject;

/** The files a Find Usages run searches, their text, and where each file lives. */
public interface UsageSources {

    Set<FileObject> roots();

    Set<FileObject> files();

    boolean accepts(FileObject file);

    FileObject rootOf(FileObject file);

    String packageNameOf(FileObject file);

    String textOf(FileObject file) throws IOException;

    /**
     * The context of {@code file}, with {@code enclosing} declarations around one of its usages.
     * The implementation knows the project and source-root metadata for any language.
     */
    UsageContext contextOf(FileObject file, List<EnclosingElement> enclosing);
}
