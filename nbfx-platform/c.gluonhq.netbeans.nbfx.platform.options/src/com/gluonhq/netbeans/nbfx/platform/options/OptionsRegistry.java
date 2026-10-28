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
package com.gluonhq.netbeans.nbfx.platform.options;

import com.gluonhq.netbeans.nbfx.api.layer.LayerPaths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * Discovers the {@link OptionsPanelHost}s registered under {@code NbFx/Options} in the module layer.
 *
 * @since 1.0
 */
public final class OptionsRegistry {

    private OptionsRegistry() {
    }

    /**
     * Discovers and orders all registered options panels. Reads the configuration filesystem, so it
     * must be called once the platform is initialized.
     *
     * @return the panel registrations, ordered by position then id
     */
    public static List<OptionsPanelEntry> discover() {
        Lookup lookup = Lookups.forPath(LayerPaths.OPTIONS);
        FileObject folder = FileUtil.getConfigFile(LayerPaths.OPTIONS);
        return read(folder, lookup.lookupAll(OptionsPanelHost.class));
    }

    /** Reads the registrations of {@code panels}, taking metadata from {@code folder}'s attributes. */
    static List<OptionsPanelEntry> read(FileObject folder, Iterable<? extends OptionsPanelHost> panels) {
        List<OptionsPanelEntry> registrations = new ArrayList<>();
        for (OptionsPanelHost panel : panels) {
            String id = panel.getId();
            FileObject file = folder == null ? null : folder.getFileObject(id, "instance");
            String displayName = stringAttribute(file, "displayName", panel.getDisplayName());
            int position = intAttribute(file, "position", panel.getPosition());
            registrations.add(new OptionsPanelEntry(id, displayName, panel.getCategory(), position, panel));
        }
        return sort(registrations);
    }

    /** Orders panels by position, then id. */
    static List<OptionsPanelEntry> sort(List<OptionsPanelEntry> registrations) {
        List<OptionsPanelEntry> sorted = new ArrayList<>(registrations);
        sorted.sort(Comparator
                .comparingInt(OptionsPanelEntry::position)
                .thenComparing(OptionsPanelEntry::id));
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
}
