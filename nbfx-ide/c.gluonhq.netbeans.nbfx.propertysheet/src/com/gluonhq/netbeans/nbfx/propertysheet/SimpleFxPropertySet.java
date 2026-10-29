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

import java.util.List;

/** A simple {@link PropertySet} holding a fixed list of properties. */
public final class SimpleFxPropertySet implements PropertySet {

    private final String name;
    private final String displayName;
    private final List<Property> properties;

    public SimpleFxPropertySet(String name, String displayName, List<Property> properties) {
        this.name = name;
        this.displayName = displayName;
        this.properties = List.copyOf(properties);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public List<Property> getProperties() {
        return properties;
    }
}
