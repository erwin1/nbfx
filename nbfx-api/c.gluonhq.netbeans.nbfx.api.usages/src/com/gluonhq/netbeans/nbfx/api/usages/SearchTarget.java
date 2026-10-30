package com.gluonhq.netbeans.nbfx.api.usages;

import com.gluonhq.netbeans.nbfx.api.progress.Cancellation;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import org.openide.filesystems.FileObject;

/**
 * What a query looks for, whatever the language. A target knows where it was picked - file and
 * editor offset, enough to resolve it again after a restart - how it is named, and how to
 * {@link #newSearch search} for its usages.
 */
public interface SearchTarget {

    /** The file the query started in. */
    FileObject getFile();

    /** The editor offset in {@link #getFile()} the target was resolved at; with the file, enough to resolve it again. */
    int getOffset();

    /** The name occurrences of the target are written with. */
    String getSimpleName();

    /** The name the query is titled with. */
    String getDisplayName();

    /**
     * Whether the usages found can be told apart by kind (read / write / import / comment) and by
     * root (sources / tests), so the view offers its filters. Targets without such categories
     * return false.
     */
    boolean isFilterable();

    /**
     * The search for this target's usages over {@code sources}; {@code searchComments} is honoured
     * where comments can name the target.
     */
    Search newSearch(UsageSources sources, boolean searchComments);

    /** The language-specific part of a query run; {@link UsagesQuery} drives it and publishes what it hands over. */
    interface Search {

        /** Whether the target still resolves in its own file - the check NetBeans' refresh makes. */
        boolean targetExists() throws IOException;

        /**
         * Runs the search until done or {@code cancellation} says stop, handing every file's usages
         * to {@code sink} as they are confirmed and the fraction of candidate files scanned to
         * {@code progress}.
         */
        void run(Cancellation cancellation, Consumer<List<Usage>> sink, DoubleConsumer progress) throws IOException;
    }
}
