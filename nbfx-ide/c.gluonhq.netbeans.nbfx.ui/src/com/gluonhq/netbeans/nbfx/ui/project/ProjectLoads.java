/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gluonhq.netbeans.nbfx.ui.project;

import com.gluonhq.netbeans.nbfx.api.project.OpenProject;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The projects whose trees are currently being loaded, in the order the loads were started.
 * <p>
 * Several projects can be opened at once, each loading on its own background thread, so loading is
 * tracked per project rather than as a single application-wide flag: the status bar reports the
 * latest load and its cancel button only cancels that one, and closing a project only has to cancel
 * its own load.
 * <p>
 * Not thread-safe: it is only touched from the JavaFX Application Thread.
 */
public final class ProjectLoads {

    /** Project path (as in {@link OpenProject#getPath()}) to project directory. */
    private final Map<String, File> loading = new LinkedHashMap<>();

    /** Records that {@code dir}, whose project path is {@code path}, has started loading. */
    public void begin(String path, File dir) {
        if (path != null) {
            loading.put(path, dir);
        }
    }

    /** Records that the project at {@code path} is no longer loading. Unknown paths are ignored. */
    public void end(String path) {
        loading.remove(path);
    }

    /** Forgets every ongoing load. */
    void clear() {
        loading.clear();
    }

    public boolean isLoading(String path) {
        return path != null && loading.containsKey(path);
    }

    /** The directory of the project loading at {@code path}, or {@code null} if it is not loading. */
    public File dirOf(String path) {
        return path == null ? null : loading.get(path);
    }

    boolean isEmpty() {
        return loading.isEmpty();
    }

    /** How many projects are loading right now. */
    public int size() {
        return loading.size();
    }

    /** The paths of the projects still loading, in the order their loads were started. */
    public List<String> paths() {
        return List.copyOf(loading.keySet());
    }

    /**
     * The directory of the most recently started load that is still running, or {@code null} when
     * nothing is loading. That is the one the status bar reports on.
     */
    public File current() {
        File last = null;
        for (File dir : loading.values()) {
            last = dir;
        }
        return last;
    }

    /** The path of the most recently started load that is still running, or {@code null}. */
    public String currentPath() {
        String last = null;
        for (String path : loading.keySet()) {
            last = path;
        }
        return last;
    }
}
