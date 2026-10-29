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
package com.gluonhq.netbeans.nbfx.propertysheet;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import org.openide.util.lookup.ServiceProvider;

/** The editor for {@link Enum} values, shown as a choice box. */
@ServiceProvider(service = PropertyEditor.class)
public final class EnumPropertyEditor implements PropertyEditor {

    @Override
    public Class<?> valueType() {
        return Enum.class;
    }

    @Override
    public Node createEditor(Property property) {
        ComboBox<Object> box = new ComboBox<>(FXCollections.observableArrayList(property.getValueType().getEnumConstants()));
        box.setValue(property.getValue());
        box.setDisable(!property.isWritable());
        box.valueProperty().addListener((observable, old, now) -> {
            if (property.isWritable()) {
                property.setValue(now);
            }
        });
        return box;
    }
}
