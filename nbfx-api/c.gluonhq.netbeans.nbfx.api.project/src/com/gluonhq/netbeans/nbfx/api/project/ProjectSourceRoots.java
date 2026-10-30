package com.gluonhq.netbeans.nbfx.api.project;

import java.util.List;
import org.openide.filesystems.FileObject;

/**
 * The source roots of projects, described without a language. A language or project-type module
 * implements this service with its own source-group model, so generic code never has to know the
 * Java (or any other language's) source-group type strings.
 */
public interface ProjectSourceRoots {

    /** The source roots of {@code project}; empty when the project has none or is not known. */
    List<SourceRoot> roots(OpenProject project);

    /** The source root containing {@code file}, or {@code null} when it is not in one. */
    SourceRoot describe(FileObject file);
}
