package com.gluonhq.netbeans.nbfx.api.usages;

import java.util.List;
import org.openide.filesystems.FileObject;

/**
 * Where a usage lives, from the project down to the innermost declaration, which the Usages view
 * turns into its two trees: the logical one goes project → source root → package → file → the
 * {@link #enclosing()} declarations → usage, the physical one project → file → usage.
 *
 * @param projectRoot    the root folder of the open project owning the file, or the source root's
 *                       parent when the file belongs to no open project
 * @param projectName    the project's display name
 * @param sourceRoot     the source root the file lives under; {@code null} when it has none
 * @param sourceRootName the root's label, as the project UI shows it
 * @param packageName    the file's package, empty for the unnamed package
 * @param file           the file, always the one on disk (never an in-memory substitute)
 * @param enclosing      the declarations around the usage, outermost first; empty for a usage at
 *                       the top level of the file (an import)
 * @param test           the file lives under a test source root
 */
public record UsageContext(FileObject projectRoot, String projectName, FileObject sourceRoot, String sourceRootName,
        String packageName, FileObject file, List<EnclosingElement> enclosing, boolean test) {

    public UsageContext {
        enclosing = List.copyOf(enclosing);
    }
}
