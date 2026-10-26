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
package com.gluonhq.netbeans.nbfx.propertysheet;

import com.gluonhq.netbeans.nbfx.api.editor.EditorDocument;
import java.util.ArrayList;
import java.util.List;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Contributes a read-only "General" property set for an {@link EditorDocument}: its title, backing
 * file, modified state and owning project.
 */
@ServiceProvider(service = FxPropertiesProvider.class)
public final class EditorDocumentPropertiesProvider implements FxPropertiesProvider {

    @Override
    public List<FxPropertySet> getPropertySets(Object context) {
        if (!(context instanceof EditorDocument document)) {
            return List.of();
        }
        String project = document.getProjectPath();
        List<FxProperty> properties = new ArrayList<>();
        properties.add(readOnly("title", message("EditorDocumentPropertiesProvider.title"),
                document.getTitle()));
        properties.add(readOnly("file", message("EditorDocumentPropertiesProvider.file"),
                document.getFileObject().getPath()));
        properties.add(readOnly("modified", message("EditorDocumentPropertiesProvider.modified"),
                message(document.isModified()
                        ? "EditorDocumentPropertiesProvider.yes"
                        : "EditorDocumentPropertiesProvider.no")));
        properties.add(readOnly("project", message("EditorDocumentPropertiesProvider.project"),
                project == null ? "" : project));
        return List.of(new SimpleFxPropertySet("editor",
                message("EditorDocumentPropertiesProvider.general"), properties));
    }

    private static FxProperty readOnly(String name, String displayName, Object value) {
        return new SimpleFxProperty(name, displayName, null, String.class, value, false, false, value);
    }

    private static String message(String key) {
        return NbBundle.getMessage(EditorDocumentPropertiesProvider.class, key);
    }
}
