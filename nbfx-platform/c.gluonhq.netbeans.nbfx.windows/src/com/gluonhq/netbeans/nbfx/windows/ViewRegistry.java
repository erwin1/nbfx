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
package com.gluonhq.netbeans.nbfx.windows;

import com.gluonhq.netbeans.nbfx.annotations.FxLayer;
import com.gluonhq.netbeans.nbfx.annotations.FxViewLocation;
import com.gluonhq.netbeans.nbfx.annotations.FxViewRegistration.FxPersistenceType;
import com.gluonhq.netbeans.nbfx.api.view.FxArea;
import com.gluonhq.netbeans.nbfx.api.view.DockLocation;
import com.gluonhq.netbeans.nbfx.api.view.ViewProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * Discovers the {@link ViewProvider}s registered under {@code NbFx/Views} in the module layer.
 * <p>
 * Views are declared with {@code @FxViewRegistration}, which writes an
 * {@code NbFx/Views/<id>.instance} entry carrying the display name, icon, default location,
 * position and navigator flag. The registered attributes take precedence over the values reported
 * by the view itself, so placement is declared rather than coded.
 *
 * @since 1.0
 */
public final class ViewRegistry {

    private static final Logger LOG = Logger.getLogger(ViewRegistry.class.getName());

    private ViewRegistry() {
    }

    /**
     * Discovers and orders all registered views. Reads the configuration filesystem, so it must be
     * called once the platform is initialized.
     *
     * @return the view registrations, ordered by location, position and id
     */
    public static List<ViewRegistration> discover() {
        Lookup lookup = Lookups.forPath(FxLayer.VIEWS);
        FileObject folder = FileUtil.getConfigFile(FxLayer.VIEWS);
        return read(folder, lookup.lookupAll(ViewProvider.class));
    }

    /**
     * The registration of the view with {@code id}, or {@code null} when no view declares it. Reads
     * the configuration filesystem like {@link #discover()}.
     *
     * @param id the view id
     * @return the registration, or {@code null}
     */
    public static ViewRegistration find(String id) {
        for (ViewRegistration registration : discover()) {
            if (registration.id().equals(id)) {
                return registration;
            }
        }
        return null;
    }

    /** Reads the registrations of {@code views}, taking placement from {@code folder}'s attributes. */
    static List<ViewRegistration> read(FileObject folder, Iterable<? extends ViewProvider> views) {
        List<ViewRegistration> registrations = new ArrayList<>();
        for (ViewProvider view : views) {
            String id = view.getId();
            FileObject file = folder == null ? null : folder.getFileObject(id, "instance");
            String displayName = stringAttribute(file, "displayName", view.getTitle());
            String iconName = stringAttribute(file, "iconName", "");
            FxViewLocation location = locationAttribute(file, view.getDefaultLocation());
            int position = intAttribute(file, "position", Integer.MAX_VALUE);
            boolean navigator = booleanAttribute(file, "navigator");
            FxArea area = areaAttribute(file, location);
            boolean openAtStartup = booleanAttribute(file, "openAtStartup");
            FxPersistenceType persistenceType = persistenceAttribute(file);
            registrations.add(new ViewRegistration(id, displayName, iconName, location, position,
                    navigator, area, openAtStartup, persistenceType, view));
        }
        return sort(registrations);
    }

    /** Orders views by location, then position, then id, so the layout is stable. */
    static List<ViewRegistration> sort(List<ViewRegistration> registrations) {
        List<ViewRegistration> sorted = new ArrayList<>(registrations);
        sorted.sort(Comparator
                .comparingInt((ViewRegistration registration) -> registration.location().ordinal())
                .thenComparingInt(ViewRegistration::position)
                .thenComparing(ViewRegistration::id));
        return List.copyOf(sorted);
    }

    private static String stringAttribute(FileObject file, String name, String fallback) {
        Object value = file == null ? null : file.getAttribute(name);
        return value instanceof String text ? text : fallback;
    }

    private static int intAttribute(FileObject file, String name, int fallback) {
        Object value = file == null ? null : file.getAttribute(name);
        return value instanceof Integer number ? number : fallback;
    }

    private static boolean booleanAttribute(FileObject file, String name) {
        Object value = file == null ? null : file.getAttribute(name);
        return Boolean.TRUE.equals(value);
    }

    private static FxViewLocation locationAttribute(FileObject file, DockLocation fallback) {
        Object value = file == null ? null : file.getAttribute("location");
        if (value instanceof String name) {
            try {
                return FxViewLocation.valueOf(name);
            } catch (IllegalArgumentException ex) {
                LOG.warning("Unknown view location: " + name);
            }
        }
        return FxViewLocation.valueOf(fallback.name());
    }

    /**
     * The window area of the view: its declared {@code area} attribute resolved as an
     * {@link FxArea}, or the area the {@code location} docks into by default. Empty areas (never
     * declared) and unknown area ids fall back to the location's default area.
     */
    static FxArea areaAttribute(FileObject file, FxViewLocation location) {
        Object value = file == null ? null : file.getAttribute("area");
        if (value instanceof String id && !id.isBlank()) {
            return FxArea.lookUp(id).orElseGet(() -> defaultArea(location));
        }
        return defaultArea(location);
    }

    private static FxPersistenceType persistenceAttribute(FileObject file) {
        Object value = file == null ? null : file.getAttribute("persistenceType");
        if (value instanceof String name) {
            try {
                return FxPersistenceType.valueOf(name);
            } catch (IllegalArgumentException ex) {
                LOG.warning("Unknown persistence type: " + name);
            }
        }
        return FxPersistenceType.ALWAYS;
    }

    /** The default window area a location is docked into, mirroring the Swing modes. */
    static FxArea defaultArea(FxViewLocation location) {
        return switch (location) {
            case LEFT -> FxArea.EXPLORER;
            case LEFT_BOTTOM -> FxArea.NAVIGATOR;
            case CENTER -> FxArea.EDITOR;
            case CENTER_BOTTOM, BOTTOM -> FxArea.OUTPUT;
            case RIGHT -> FxArea.PROPERTIES;
            case DETACHED -> throw new IllegalArgumentException("No default area for " + location);
        };
    }
}
