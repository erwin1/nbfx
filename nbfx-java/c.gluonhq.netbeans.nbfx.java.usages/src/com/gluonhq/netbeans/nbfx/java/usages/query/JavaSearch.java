package com.gluonhq.netbeans.nbfx.java.usages.query;

import com.gluonhq.netbeans.nbfx.api.usages.SearchTarget;
import com.gluonhq.netbeans.nbfx.api.progress.Cancellation;
import com.gluonhq.netbeans.nbfx.api.usages.Usage;
import com.gluonhq.netbeans.nbfx.api.usages.UsageSources;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.openide.filesystems.FileObject;

/**
 * The search for a Java element: compiles the candidate files root by root and confirms each
 * occurrence with a {@link UsageScanner}.
 */
final class JavaSearch implements SearchTarget.Search {

    private static final Logger LOG = Logger.getLogger(JavaSearch.class.getName());

    private final UsagesTarget target;
    private final UsageSources sources;
    private final boolean searchComments;

    JavaSearch(UsagesTarget target, UsageSources sources, boolean searchComments) {
        this.target = target;
        this.sources = sources;
        this.searchComments = searchComments;
    }

    @Override
    public boolean targetExists() throws IOException {
        JavaSource javaSource = JavaSources.javaSourceOf(sources, target.getFile());
        if (javaSource == null) {
            return false;
        }
        AtomicBoolean exists = new AtomicBoolean();
        javaSource.runUserActionTask(info -> {
            if (info.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
                return;
            }
            exists.set(target.resolveIn(info, target.getFile()) != null);
        }, true);
        return exists.get();
    }

    @Override
    public void run(Cancellation cancellation, Consumer<List<Usage>> sink, DoubleConsumer progress) throws IOException {
        List<FileObject> candidates = new ArrayList<>(candidates(cancellation));
        if (cancellation.isCancelled()) {
            return;
        }
        // The declaring file is always compiled - the element resolves through it - but only
        // scanned when the sources accept it.
        if (!candidates.contains(target.getFile())) {
            candidates.add(target.getFile());
        }
        JavaSources.Compilation compilation = JavaSources.compile(sources, candidates);
        int[] scanned = {0};
        for (JavaSource javaSource : compilation.javaSources()) {
            if (cancellation.isCancelled()) {
                return;
            }
            javaSource.runUserActionTask(info -> {
                if (cancellation.isCancelled()) {
                    return;
                }
                scanUnit(info, compilation, cancellation, sink);
                scanned[0]++;
                progress.accept(candidates.isEmpty() ? 1 : (double) scanned[0] / candidates.size());
            }, true);
        }
    }

    /**
     * The files worth compiling for {@code target}: its own file, and - unless the element is only
     * visible there - every Java file under the roots whose text mentions the element's name; all
     * of them only if {@link UsageSources#accepts accepted}.
     */
    private List<FileObject> candidates(Cancellation cancellation) {
        LinkedHashSet<FileObject> result = new LinkedHashSet<>();
        if (sources.accepts(target.getFile())) {
            result.add(target.getFile());
        }
        if (target.isFileLocal() || !target.isSearchableByName()) {
            return new ArrayList<>(result);
        }
        String name = target.getSimpleName();
        for (FileObject root : sources.roots()) {
            Enumeration<? extends FileObject> children = root.getChildren(true);
            while (children.hasMoreElements()) {
                if (cancellation.isCancelled()) {
                    return new ArrayList<>(result);
                }
                FileObject file = children.nextElement();
                if (file.isData() && "java".equals(file.getExt()) && sources.accepts(file) && !result.contains(file)
                        && mentions(file, name)) {
                    result.add(file);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private boolean mentions(FileObject file, String name) {
        try {
            return sources.textOf(file).contains(name);
        } catch (IOException ex) {
            LOG.log(Level.FINE, "Could not read " + file, ex);
            return false;
        }
    }

    private void scanUnit(CompilationController info, JavaSources.Compilation compilation, Cancellation cancellation,
            Consumer<List<Usage>> sink) throws IOException {
        if (info.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }
        FileObject original = compilation.originalOf(info.getFileObject());
        if (!sources.accepts(original)) {
            return;
        }
        Element element = target.resolveIn(info, original);
        if (element == null) {
            return;
        }
        List<Usage> found = new UsageScanner(info, element, sources, original, searchComments, cancellation).scan();
        if (!found.isEmpty() && !cancellation.isCancelled()) {
            sink.accept(found);
        }
    }
}
