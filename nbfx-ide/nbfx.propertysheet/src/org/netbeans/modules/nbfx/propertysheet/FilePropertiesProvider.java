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
package org.netbeans.modules.nbfx.propertysheet;

import java.util.List;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Contributes a read-only "General" property set for a {@link FileObject}: name, path, kind and size.
 */
@ServiceProvider(service = PropertiesProvider.class)
public final class FilePropertiesProvider implements PropertiesProvider {

    @Override
    public List<PropertySet> getPropertySets(Object context) {
        if (!(context instanceof FileObject file)) {
            return List.of();
        }
        List<Property> properties = List.of(
                readOnly("name", message("FilePropertiesProvider.name"), file.getNameExt()),
                readOnly("path", message("FilePropertiesProvider.path"), file.getPath()),
                readOnly("kind", message("FilePropertiesProvider.kind"),
                        message(file.isFolder() ? "FilePropertiesProvider.folder" : "FilePropertiesProvider.file")),
                readOnly("size", message("FilePropertiesProvider.size"), size(file)));
        return List.of(new SimpleFxPropertySet("file", message("FilePropertiesProvider.general"), properties));
    }

    private static String size(FileObject file) {
        if (file.isFolder()) {
            return "-";
        }
        long size = file.getSize();
        return size < 0 ? "?" : String.valueOf(size);
    }

    private static Property readOnly(String name, String displayName, Object value) {
        return new SimpleFxProperty(name, displayName, null, String.class, value, false, false, value);
    }

    private static String message(String key) {
        return NbBundle.getMessage(FilePropertiesProvider.class, key);
    }
}
