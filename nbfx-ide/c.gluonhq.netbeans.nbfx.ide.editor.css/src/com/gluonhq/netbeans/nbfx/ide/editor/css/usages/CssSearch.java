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
package com.gluonhq.netbeans.nbfx.ide.editor.css.usages;

import com.gluonhq.netbeans.nbfx.api.progress.Cancellation;
import com.gluonhq.netbeans.nbfx.api.usages.UsageContext;
import com.gluonhq.netbeans.nbfx.api.usages.UsageKind;
import com.gluonhq.netbeans.nbfx.api.usages.UsageSources;
import com.gluonhq.netbeans.nbfx.api.usages.SearchTarget;
import com.gluonhq.netbeans.nbfx.api.usages.Usage;
import com.gluonhq.netbeans.nbfx.ide.editor.css.usages.CssUsageScanner.Symbol;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import org.openide.filesystems.FileObject;

/**
 * The search for a {@link CssTarget}: every style sheet under the sources' roots (that the sources
 * accept) is scanned, the target's own first, and each symbol matching the target becomes a usage. All the sheets count,
 * as NetBeans does with "Find all occurrences" - there is no CSS dependency graph to narrow them.
 */
final class CssSearch implements SearchTarget.Search {

    private final CssTarget target;
    private final UsageSources sources;

    CssSearch(CssTarget target, UsageSources sources) {
        this.target = target;
        this.sources = sources;
    }

    @Override
    public boolean targetExists() throws IOException {
        return target.getFile().isValid()
                && !target.occurrencesIn(CssUsageScanner.scan(textOf(sources, target.getFile()))).isEmpty();
    }

    @Override
    public void run(Cancellation cancellation, Consumer<List<Usage>> sink, DoubleConsumer progress) throws IOException {
        List<FileObject> candidates = candidates(cancellation);
        if (candidates.isEmpty()) {
            progress.accept(1);
            return;
        }
        int scanned = 0;
        for (FileObject file : candidates) {
            if (cancellation.isCancelled()) {
                return;
            }
            List<Usage> found = scan(file);
            if (!found.isEmpty()) {
                sink.accept(found);
            }
            scanned++;
            progress.accept((double) scanned / candidates.size());
        }
    }

    private List<FileObject> candidates(Cancellation cancellation) {
        LinkedHashSet<FileObject> result = new LinkedHashSet<>();
        if (sources.accepts(target.getFile())) {
            result.add(target.getFile());
        }
        for (FileObject root : sources.roots()) {
            Enumeration<? extends FileObject> children = root.getChildren(true);
            while (children.hasMoreElements()) {
                if (cancellation.isCancelled()) {
                    break;
                }
                FileObject file = children.nextElement();
                if (file.isData() && CssTarget.isStyleSheet(file) && sources.accepts(file)) {
                    result.add(file);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private List<Usage> scan(FileObject file) throws IOException {
        String text = textOf(sources, file);
        List<Symbol> occurrences = target.occurrencesIn(CssUsageScanner.scan(text));
        if (occurrences.isEmpty()) {
            return List.of();
        }
        UsageContext context = sources.contextOf(file, List.of());
        List<Usage> usages = new ArrayList<>(occurrences.size());
        for (Symbol symbol : occurrences) {
            usages.add(usage(text, symbol, context));
        }
        return usages;
    }

    private static Usage usage(String text, Symbol symbol, UsageContext context) {
        int lineStart = text.lastIndexOf('\n', symbol.start() - 1) + 1;
        int lineEnd = text.indexOf('\n', symbol.end());
        lineEnd = lineEnd < 0 ? text.length() : lineEnd;
        int line = 1;
        for (int i = 0; i < lineStart; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        // the text begins at the first non-blank character of the line
        int textStart = lineStart;
        while (textStart < symbol.start() && Character.isWhitespace(text.charAt(textStart))) {
            textStart++;
        }
        String lineText = text.substring(textStart, lineEnd).stripTrailing();
        return new Usage(context.file(), symbol.start(), symbol.end(), line, textStart, lineText,
                UsageKind.OTHER, context.test(), context);
    }

    /**
     * The text of {@code file} in the editor's convention - one {@code '\n'} per line break - so
     * every offset the scan yields is an editor offset, whether the text came from an open editor
     * or from a disk file with {@code \r\n} line breaks.
     */
    static String textOf(UsageSources sources, FileObject file) throws IOException {
        return sources.textOf(file).replace("\r", "");
    }
}
