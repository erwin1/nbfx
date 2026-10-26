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
package com.gluonhq.netbeans.nbfx.editor;

import com.gluonhq.netbeans.nbfx.api.editor.EditorSettings;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.openide.util.lookup.ServiceProvider;

/**
 * Default {@link EditorSettings} holding the shared, observable display preferences. Line numbers
 * and breadcrumbs are shown by default; the launcher overrides this from persisted state at startup.
 */
@ServiceProvider(service = EditorSettings.class)
public class EditorSettingsImpl implements EditorSettings {

    private final BooleanProperty showLineNumbers = new SimpleBooleanProperty(this, "showLineNumbers", true);
    private final BooleanProperty showBreadcrumbs = new SimpleBooleanProperty(this, "showBreadcrumbs", true);

    @Override
    public BooleanProperty showLineNumbers() {
        return showLineNumbers;
    }

    @Override
    public BooleanProperty showBreadcrumbs() {
        return showBreadcrumbs;
    }
}
