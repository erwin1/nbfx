package com.gluonhq.netbeans.nbfx.api.project;

import java.util.Objects;
import org.openide.filesystems.FileObject;

/**
 * One source root of a project, described in language-neutral terms.
 *
 * @param root        the root folder
 * @param displayName the label the project UI uses, for example "Source Packages"
 * @param test        whether the root holds tests
 */
public record SourceRoot(FileObject root, String displayName, boolean test) {

    public SourceRoot {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(displayName, "displayName");
    }
}
