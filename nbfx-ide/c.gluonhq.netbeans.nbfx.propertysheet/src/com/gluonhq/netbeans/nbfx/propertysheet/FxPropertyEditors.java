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

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.openide.util.Lookup;

/**
 * Resolves the {@link FxPropertyEditor} for a property: the first registered editor whose value type
 * can hold the property's value type, or a read-only label when none matches.
 */
public final class FxPropertyEditors {

    private FxPropertyEditors() {
    }

    public static Node createEditor(FxProperty property) {
        if (property.isWritable()) {
            for (FxPropertyEditor editor : Lookup.getDefault().lookupAll(FxPropertyEditor.class)) {
                if (editor.valueType().isAssignableFrom(property.getValueType())) {
                    return editor.createEditor(property);
                }
            }
        }
        return new Label(display(property.getValue()));
    }

    static String display(Object value) {
        return value == null ? "" : value.toString();
    }
}
