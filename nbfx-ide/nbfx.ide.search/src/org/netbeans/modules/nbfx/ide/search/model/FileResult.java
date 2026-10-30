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
package org.netbeans.modules.nbfx.ide.search.model;

import java.util.List;
import java.util.Objects;
import org.openide.filesystems.FileObject;

/**
 * A file that matched a search - NetBeans' {@code MatchingObject}: the file, its size and
 * modification time when it was searched, and its matches (none for a file-name-only search).
 *
 * @param file         the matching file
 * @param size         the file's size in bytes
 * @param lastModified the file's modification time, in milliseconds since the epoch
 * @param matches      the occurrences, in text order; empty when only the name was matched
 */
public record FileResult(FileObject file, long size, long lastModified, List<TextMatch> matches) {

    public FileResult {
        Objects.requireNonNull(file);
        matches = List.copyOf(matches);
    }

    public int matchCount() {
        return matches.size();
    }
}
