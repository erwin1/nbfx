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
package com.gluonhq.netbeans.nbfx.api.usages;

import java.io.IOException;
import org.openide.filesystems.FileObject;

/** Resolves a language-specific Find Usages target. */
public interface UsagesProvider {

    boolean handles(FileObject file);

    SearchTarget targetAt(FileObject file, int offset, UsageSources sources) throws IOException;

    /**
     * Whether {@link #topLevelTarget} can resolve a target for {@code file}. A provider that has no
     * top-level element to search, such as a style sheet, leaves this false.
     */
    default boolean handlesTopLevel(FileObject file) {
        return false;
    }

    SearchTarget topLevelTarget(FileObject file, UsageSources sources) throws IOException;
}
