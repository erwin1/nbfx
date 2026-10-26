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
import javafx.scene.control.CheckBox;
import org.openide.util.lookup.ServiceProvider;

/** The editor for {@link Boolean} values. */
@ServiceProvider(service = FxPropertyEditor.class)
public final class BooleanPropertyEditor implements FxPropertyEditor {

    @Override
    public Class<?> valueType() {
        return Boolean.class;
    }

    @Override
    public Node createEditor(FxProperty property) {
        CheckBox box = new CheckBox();
        box.setSelected(Boolean.TRUE.equals(property.getValue()));
        box.setDisable(!property.isWritable());
        box.selectedProperty().addListener((observable, old, now) -> {
            if (property.isWritable()) {
                property.setValue(now);
            }
        });
        return box;
    }
}
