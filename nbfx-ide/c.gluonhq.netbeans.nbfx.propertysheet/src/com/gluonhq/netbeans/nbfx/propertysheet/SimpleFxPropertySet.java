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

import java.util.List;

/** A simple {@link FxPropertySet} holding a fixed list of properties. */
public final class SimpleFxPropertySet implements FxPropertySet {

    private final String name;
    private final String displayName;
    private final List<FxProperty> properties;

    public SimpleFxPropertySet(String name, String displayName, List<FxProperty> properties) {
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
    public List<FxProperty> getProperties() {
        return properties;
    }
}
