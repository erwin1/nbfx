package com.gluonhq.netbeans.nbfx.ide.usages.query;

import com.gluonhq.netbeans.nbfx.api.editor.ContentManager;
import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import com.gluonhq.netbeans.nbfx.api.editor.OpenSources;
import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import com.gluonhq.netbeans.nbfx.api.project.ProjectRegistry;
import com.gluonhq.netbeans.nbfx.api.project.ProjectSourceRoots;
import com.gluonhq.netbeans.nbfx.api.project.SourceRoot;
import com.gluonhq.netbeans.nbfx.api.usages.EnclosingElement;
import com.gluonhq.netbeans.nbfx.api.usages.QueryOptions;
import com.gluonhq.netbeans.nbfx.api.usages.UsageContext;
import com.gluonhq.netbeans.nbfx.api.usages.UsageSources;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 * The language-neutral source set of a Find Usages run. Roots, their labels and their test/source
 * nature come from the registered {@link ProjectSourceRoots} service; this class only applies the
 * query scope and reads the files, through {@link OpenSources} when an editor is open.
 */
final class DefaultUsageSources implements UsageSources {

    private final Set<FileObject> roots;
    /** The only files searched under the roots, {@code null} for all of them. */
    private final Set<FileObject> files;
    private final OpenSources openSources;
    private final ProjectSourceRoots sourceRoots;
    private final Map<FileObject, BaseContext> contexts = new ConcurrentHashMap<>();

    private DefaultUsageSources(Collection<FileObject> roots, Set<FileObject> files, OpenSources openSources,
            ProjectSourceRoots sourceRoots) {
        this.roots = new LinkedHashSet<>(roots);
        this.files = files == null ? null : Set.copyOf(files);
        this.openSources = openSources;
        this.sourceRoots = sourceRoots;
    }

    static UsageSources forScope(QueryOptions.Scope scope, FileObject file) {
        ProjectSourceRoots sourceRoots = Lookup.getDefault().lookup(ProjectSourceRoots.class);
        OpenSources openSources = Lookup.getDefault().lookup(OpenSources.class);
        Set<FileObject> allRoots = allRoots(sourceRoots);
        return switch (scope) {
            case ALL_PROJECTS -> new DefaultUsageSources(allRoots, null, openSources, sourceRoots);
            case CURRENT_PROJECT -> new DefaultUsageSources(projectRoots(allRoots, file, sourceRoots), null,
                    openSources, sourceRoots);
            case CURRENT_PACKAGE -> new DefaultUsageSources(allRoots, packageFiles(file), openSources, sourceRoots);
            case CURRENT_FILE -> new DefaultUsageSources(allRoots, Set.of(file), openSources, sourceRoots);
            case OPEN_FILES -> new DefaultUsageSources(allRoots, openFiles(), openSources, sourceRoots);
        };
    }

    private static Set<FileObject> allRoots(ProjectSourceRoots sourceRoots) {
        Set<FileObject> result = new LinkedHashSet<>();
        ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        if (registry == null || sourceRoots == null) {
            return result;
        }
        for (OpenProject project : registry.getOpenProjects()) {
            for (SourceRoot root : sourceRoots.roots(project)) {
                result.add(root.root());
            }
        }
        return result;
    }

    private static Set<FileObject> projectRoots(Set<FileObject> allRoots, FileObject file,
            ProjectSourceRoots sourceRoots) {
        Set<FileObject> result = new LinkedHashSet<>();
        ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        OpenProject project = registry == null ? null : registry.ownerOf(file);
        if (project != null && sourceRoots != null) {
            for (SourceRoot root : sourceRoots.roots(project)) {
                result.add(root.root());
            }
        }
        if (result.isEmpty() && sourceRoots != null) {
            SourceRoot root = sourceRoots.describe(file);
            if (root != null) {
                result.add(root.root());
            }
        }
        return result;
    }

    /** The data files in {@code file}'s folder: its package. */
    private static Set<FileObject> packageFiles(FileObject file) {
        Set<FileObject> result = new LinkedHashSet<>();
        FileObject folder = file.getParent();
        if (folder != null) {
            for (FileObject child : folder.getChildren()) {
                if (child.isData()) {
                    result.add(child);
                }
            }
        }
        result.add(file);
        return result;
    }

    private static Set<FileObject> openFiles() {
        Set<FileObject> result = new LinkedHashSet<>();
        ContentManager contentManager = Lookup.getDefault().lookup(ContentManager.class);
        ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        if (contentManager == null) {
            return result;
        }
        List<String> projectPaths = new ArrayList<>();
        if (registry != null) {
            for (OpenProject project : registry.getOpenProjects()) {
                projectPaths.add(project.getPath());
            }
        }
        projectPaths.add(null);
        for (String path : projectPaths) {
            for (EditorDocument document : contentManager.documentsOf(path)) {
                if (document.getFileObject() != null) {
                    result.add(document.getFileObject());
                }
            }
        }
        return result;
    }

    @Override
    public Set<FileObject> roots() {
        return roots;
    }

    /** The only files searched among the roots' - the open ones, for that scope - or {@code null} when all are. */
    @Override
    public Set<FileObject> files() {
        return files;
    }

    /** Whether {@code file} is among the files this set searches. */
    @Override
    public boolean accepts(FileObject file) {
        return files == null || files.contains(file);
    }

    /** The source root of {@code file}, or the longest known root containing it, or {@code null}. */
    @Override
    public FileObject rootOf(FileObject file) {
        SourceRoot described = sourceRoots == null ? null : sourceRoots.describe(file);
        if (described != null) {
            return described.root();
        }
        FileObject best = null;
        for (FileObject root : roots) {
            if ((root.equals(file) || FileUtil.isParentOf(root, file))
                    && (best == null || root.getPath().length() > best.getPath().length())) {
                best = root;
            }
        }
        return best;
    }

    /** The dotted path of {@code file}'s folder from its source root, empty when at the root or under none. */
    @Override
    public String packageNameOf(FileObject file) {
        FileObject root = rootOf(file);
        String relative = root == null || file.getParent() == null ? null
                : FileUtil.getRelativePath(root, file.getParent());
        return relative == null ? "" : relative.replace('/', '.');
    }

    /** The editor text of {@code file}, or {@code null} when it is not open. */
    private String openTextOf(FileObject file) {
        return openSources == null ? null : openSources.textOf(file);
    }

    /** The text a query reads for {@code file}: the editor's when open, else the disk's. */
    @Override
    public String textOf(FileObject file) throws IOException {
        String open = openTextOf(file);
        return open != null ? open : file.asText();
    }

    @Override
    public UsageContext contextOf(FileObject file, List<EnclosingElement> enclosing) {
        BaseContext base = contexts.computeIfAbsent(file, this::baseContext);
        return new UsageContext(base.projectRoot(), base.projectName(), base.sourceRoot(), base.sourceRootName(),
                base.packageName(), file, enclosing, base.test());
    }

    private BaseContext baseContext(FileObject file) {
        ProjectRegistry registry = Lookup.getDefault().lookup(ProjectRegistry.class);
        OpenProject project = registry == null ? null : registry.ownerOf(file);
        SourceRoot described = sourceRoots == null ? null : sourceRoots.describe(file);
        FileObject sourceRoot = described != null ? described.root()
                : project == null ? null : rootUnder(project.getRoot(), file);
        FileObject projectRoot = project != null ? project.getRoot()
                : sourceRoot != null && sourceRoot.getParent() != null ? sourceRoot.getParent()
                : file.getParent();
        String projectName = project != null ? project.getDisplayName()
                : projectRoot != null ? projectRoot.getNameExt() : "";
        String relative = sourceRoot == null || projectRoot == null ? null
                : FileUtil.getRelativePath(projectRoot, sourceRoot);
        String sourceRootName = described != null ? described.displayName()
                : relative == null || relative.isEmpty() ? sourceRoot == null ? "" : sourceRoot.getNameExt() : relative;
        boolean test = described != null && described.test();
        return new BaseContext(projectRoot, projectName, sourceRoot, sourceRootName, packageNameOf(file), test);
    }

    private FileObject rootUnder(FileObject projectRoot, FileObject file) {
        FileObject best = null;
        for (FileObject root : roots) {
            if (FileUtil.isParentOf(projectRoot, root) && (root.equals(file) || FileUtil.isParentOf(root, file))
                    && (best == null || root.getPath().length() > best.getPath().length())) {
                best = root;
            }
        }
        return best;
    }

    /** Everything in a context except the enclosing declarations, which differ per usage. */
    private record BaseContext(FileObject projectRoot, String projectName, FileObject sourceRoot,
            String sourceRootName, String packageName, boolean test) {
    }
}
