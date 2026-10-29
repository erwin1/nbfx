/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gluonhq.netbeans.nbfx.findusages.query;

import com.gluonhq.netbeans.nbfx.api.progress.Cancellation;
import com.gluonhq.netbeans.nbfx.findusages.model.Usage;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import org.openide.filesystems.FileObject;

/**
 * What a {@link UsagesQuery} looks for, whatever the language: a Java element ({@link UsagesTarget})
 * or a CSS selector / colour ({@link CssTarget}). A target knows where it was picked - file and
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
     * root (sources / tests), so the view offers its filters; CSS occurrences have no such categories.
     */
    boolean isFilterable();

    /**
     * The search for this target's usages over {@code sources}; {@code searchComments} is honoured
     * where comments can name the target.
     */
    Search newSearch(SourceSet sources, boolean searchComments);

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
