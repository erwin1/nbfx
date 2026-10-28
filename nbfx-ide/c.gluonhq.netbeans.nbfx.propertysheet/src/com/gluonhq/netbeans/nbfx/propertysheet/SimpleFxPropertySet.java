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
