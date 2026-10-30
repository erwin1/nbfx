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

import javafx.scene.Node;

/**
 * Creates the JavaFX editor for a {@link Property} of a given value type. Registered via
 * {@code @ServiceProvider}; the first editor whose {@link #valueType() type} can hold the property's
 * value type wins.
 */
public interface PropertyEditor {

    /** The value type this editor handles (e.g. {@code String.class}). */
    Class<?> valueType();

    /** Builds the editor for {@code property}. Called on the JavaFX thread. */
    Node createEditor(Property property);
}
