package com.gluonhq.netbeans.nbfx.api.view;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A named area of the shell window - the nbfx analogue of a NetBeans Swing window mode ({@code
 * *.wsmode}): a permanent region with a default {@link #location()} and {@link #weight()} that views
 * are docked into. The {@linkplain #defaults() default areas} mirror the Swing {@code editor},
 * {@code explorer}, {@code navigator}, {@code properties} and {@code output} modes, so views that
 * declare a {@link ViewProvider#getDefaultLocation() location} end up where the desktop expectations
 * put them.
 *
 * <p>Kept deliberately lightweight for the MVP: menus and layout proportions derive from the weights
 * below; the sliding (auto-hide) side areas stay deferred until the window chapters.
 *
 * @since 1.0
 */
public record ViewArea(String id, String displayName, DockLocation location, double weight) {

    public ViewArea {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(location, "location");
        if (Double.isNaN(weight) || weight < 0.0d || weight > 1.0d) {
            throw new IllegalArgumentException("weight must be between 0 and 1, was " + weight);
        }
    }

    /** The editor area: the center of the window, where the editors live. */
    public static final ViewArea EDITOR = new ViewArea("editor", "Editor", DockLocation.CENTER, 1.0d);

    /** The explorer area: the left pane, home of the project and file navigators. */
    public static final ViewArea EXPLORER = new ViewArea("explorer", "Explorer", DockLocation.LEFT, 0.3d);

    /** The navigator area: below the explorer, home of the Structure and context views. */
    public static final ViewArea NAVIGATOR = new ViewArea("navigator", "Navigator", DockLocation.LEFT_BOTTOM, 0.5d);

    /** The properties area: along the right of the window, beside the editors. */
    public static final ViewArea PROPERTIES = new ViewArea("properties", "Properties", DockLocation.RIGHT, 0.3d);

    /** The output area: below the editors, home of Output, Search and Git views. */
    public static final ViewArea OUTPUT = new ViewArea("output", "Output", DockLocation.CENTER_BOTTOM, 0.3d);

    /** The default areas, in the order the Swing modes put them. */
    public static List<ViewArea> defaults() {
        return List.of(EDITOR, EXPLORER, NAVIGATOR, PROPERTIES, OUTPUT);
    }

    /** The default area for a {@code location}, or empty for {@link DockLocation#BOTTOM} (no mode). */
    public static Optional<ViewArea> defaultFor(DockLocation location) {
        return defaults().stream().filter(area -> area.location() == location).findFirst();
    }

    /** Looks the area up by id, case-insensitively. */
    public static Optional<ViewArea> lookUp(String id) {
        return defaults().stream().filter(area -> area.id().equalsIgnoreCase(id)).findFirst();
    }
}