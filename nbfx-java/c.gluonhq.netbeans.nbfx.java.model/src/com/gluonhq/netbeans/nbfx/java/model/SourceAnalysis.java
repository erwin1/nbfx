package com.gluonhq.netbeans.nbfx.java.model;

import java.io.IOException;
import org.netbeans.api.java.source.ClasspathInfo;
import org.openide.filesystems.FileObject;

/** The Java semantic bridge: classpath creation and in-memory sources. */
public interface SourceAnalysis {

    /** A memory copy of {@code original} holding {@code text}. */
    FileObject inMemoryFile(FileObject original, String text) throws IOException;

    /** Creates the classpath for {@code owner}. */
    ClasspathInfo classpath(FileObject owner);
}
