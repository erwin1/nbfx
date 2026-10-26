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
package com.gluonhq.netbeans.nbfx.api.file;

import javafx.beans.value.ObservableValue;

/**
 * The shared, observable state of the navigator focus: whether a navigator view owns the keyboard
 * focus. The navigator <em>selection</em> no longer lives here: since the global action context
 * follows the active component, each navigator publishes its own selection through
 * {@link com.gluonhq.netbeans.nbfx.api.view.ViewProvider#getLookup()}, so two navigators never answer for
 * each other.
 * <p>
 * The focus signal stays here because it is shell state, not selection: it drives the dispatch of
 * the shared Cut/Copy/Paste/Undo/Redo ids between their file and editor variants. It is updated by
 * the application shell (which owns the navigator views) and observed by the file action providers.
 * The context is resolved through the global {@link org.openide.util.Lookup}.
 */
public interface FileSelectionContext {

    /**
     * Whether the keyboard focus is currently inside a navigator view. Drives the dispatch of
     * the shared Cut/Copy/Paste/Undo/Redo ids between their file and editor variants.
     *
     * @return the observable navigator-focused state
     */
    ObservableValue<Boolean> navigatorFocused();

    /**
     * Publishes whether the keyboard focus is inside a navigator view.
     *
     * @param focused {@code true} while a navigator view owns the focus
     */
    void setNavigatorFocused(boolean focused);
}
