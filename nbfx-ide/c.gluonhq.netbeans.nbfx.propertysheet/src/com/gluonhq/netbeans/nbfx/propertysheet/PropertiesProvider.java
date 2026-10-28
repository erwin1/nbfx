package com.gluonhq.netbeans.nbfx.propertysheet;

import java.util.List;

/**
 * Contributes the property sets shown for a context object, mirroring {@code Node.getPropertySets()}.
 * Registered via {@code @ServiceProvider}; the {@link PropertySheet} asks every provider for the
 * context it is showing.
 */
public interface PropertiesProvider {

    /** The property sets for {@code context}, or an empty list when the provider has nothing to offer. */
    List<PropertySet> getPropertySets(Object context);
}
