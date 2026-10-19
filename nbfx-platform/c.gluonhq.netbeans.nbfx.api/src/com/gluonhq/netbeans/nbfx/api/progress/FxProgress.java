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
package com.gluonhq.netbeans.nbfx.api.progress;

/**
 * Starts a long-running activity shown by the status bar's progress UI (the status line and the
 * all-tasks popup), the way NetBeans shows a running build or scan.
 * <p>
 * The service is resolved from the global Lookup and driven from any thread; the implementation
 * marshals to the JavaFX Application Thread. **Each {@link #start} is its own task**: concurrent
 * activities (a project load and a build) get one task each, and each {@link Handle} finishes only
 * its own task. This replaces the former single-slot API, which silently merged concurrent
 * activities into one bar.
 *
 * @since 1.0
 */
public interface FxProgress {

    /**
     * Starts a task and returns its handle. The task stays visible until
     * {@link Handle#finish()} is called.
     *
     * @param message  the text shown next to the progress bar
     * @param onCancel the action run when cancel is pressed, or {@code null} to hide cancel
     * @return the handle that owns this task; never {@code null}
     */
    Handle start(String message, Runnable onCancel);

    /** One running task. All methods are safe to call from any thread; later calls are no-ops. */
    interface Handle {

        /** Changes the task's message (for example when a build moves to its next step). */
        void setMessage(String message);

        /** Finishes only this task. Idempotent. */
        void finish();
    }
}
