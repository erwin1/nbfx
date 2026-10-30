package com.gluonhq.netbeans.nbfx.java.model;

import java.io.IOException;
import org.netbeans.api.java.source.ClasspathInfo;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SourceAnalysis.class)
public final class DefaultSourceAnalysis implements SourceAnalysis {

    @Override
    public FileObject inMemoryFile(FileObject original, String text) throws IOException {
        return InMemorySources.copyOf(original, text);
    }

    @Override
    public ClasspathInfo classpath(FileObject owner) {
        return ClasspathInfo.create(owner);
    }
}
