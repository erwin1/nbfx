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
package org.netbeans.modules.nbfx.project.ui.api;

/**
 * The progress sink for an in-process build (the status bar), abstracted so build executors do not
 * depend on the progress module.
 *
 * @since 1.0
 */
public interface BuildProgress {

    /**
     * Shows cancellable progress while the build runs.
     *
     * @param name   the build's display name
     * @param cancel invoked when the user cancels the build
     */
    void start(String name, Runnable cancel);

    /** Hides the progress. */
    void finish();
}
