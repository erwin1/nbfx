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
package org.netbeans.modules.nbfx.api.file;

import java.util.Locale;
import java.util.Set;

import org.openide.filesystems.FileObject;

/**
 * File classification shared by the navigator, the launcher and the editor: whether a file can be
 * opened in the code editor.
 */
public final class FileTypes {

    /** Binary/archive file extensions that cannot be opened in the code editor. */
    private static final Set<String> NON_OPENABLE_EXTENSIONS = Set.of(
            "jar", "nbm", "zip", "war", "ear", "tar", "gz", "tgz", "bz2", "xz", "rar", "7z",
            "class", "so", "dll", "dylib", "exe", "bin", "o", "a", "lib",
            "jpg", "jpeg", "png", "gif", "bmp", "ico", "icns",
            "pdf", "keystore", "jks");

    private FileTypes() {}

    /**
     * Whether {@code fo} is a file that can be opened in the code editor. Folders and known
     * binary/archive files (see {@link #NON_OPENABLE_EXTENSIONS}) are not openable.
     */
    public static boolean isOpenable(FileObject fo) {
        if (fo == null || !fo.isData()) {
            return false;
        }
        String ext = fo.getExt();
        return ext == null || !NON_OPENABLE_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }
}
