package com.gluonhq.netbeans.nbfx.propertysheet;

import java.util.List;

/**
 * A named group of {@link Property properties}, shown as a branch in the {@link PropertySheet}.
 */
public interface PropertySet {

    /** A stable name, unique among the sets of a context. */
    String getName();

    /** The display name shown for the group. */
    String getDisplayName();

    /** The properties in this group. */
    List<Property> getProperties();
}
