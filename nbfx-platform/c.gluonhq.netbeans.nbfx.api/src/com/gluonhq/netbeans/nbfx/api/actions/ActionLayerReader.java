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
package com.gluonhq.netbeans.nbfx.api.actions;

import com.gluonhq.netbeans.nbfx.annotations.FxLayer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Reads the action references contributed to a UI surface under the {@code NbFx} layer, the way
 * NetBeans menus are assembled from {@code Menu/} folders.
 * <p>
 * References are written by the nbfx annotation processor as {@code <id>.ref} files carrying
 * {@code actionId}, {@code position} and {@code separatorBefore} attributes.
 *
 * @since 1.0
 */
public final class ActionLayerReader {

    private static final String REF_EXT = FxLayer.REF_SUFFIX.startsWith(".")
            ? FxLayer.REF_SUFFIX.substring(1) : FxLayer.REF_SUFFIX;

    private ActionLayerReader() {
    }

    /**
     * Reads the action references directly under {@code folder}, ordered by position then id.
     *
     * @param folder the surface folder (for example {@code NbFx/Menus/File}); may be {@code null}
     * @return the ordered references, never {@code null}
     */
    public static List<FxActionRef> read(FileObject folder) {
        if (folder == null) {
            return List.of();
        }
        List<FxActionRef> refs = new ArrayList<>();
        for (FileObject child : folder.getChildren()) {
            if (!child.isData() || !child.hasExt(REF_EXT)) {
                continue;
            }
            if (child.getAttribute("actionId") instanceof String actionId && !actionId.isBlank()) {
                int position = child.getAttribute("position") instanceof Integer p ? p : Integer.MAX_VALUE;
                boolean separatorBefore = Boolean.TRUE.equals(child.getAttribute("separatorBefore"));
                refs.add(new FxActionRef(actionId, position, separatorBefore));
            }
        }
        refs.sort(Comparator.comparingInt(FxActionRef::position).thenComparing(FxActionRef::actionId));
        return List.copyOf(refs);
    }

    /**
     * Reads the action references directly under {@code folder} that apply to {@code mimeType}: a
     * reference with no {@code mimeType} attribute applies to every type, one with a type applies
     * only when it matches. Ordered by position then id.
     *
     * @param folder   the surface folder (for example {@code NbFx/ContextMenus/Navigator}); may be
     *                 {@code null}
     * @param mimeType the content type of the node the surface is being built for, or {@code null}
     * @return the ordered references, never {@code null}
     */
    public static List<FxActionRef> readForMime(FileObject folder, String mimeType) {
        if (folder == null) {
            return List.of();
        }
        List<FxActionRef> refs = new ArrayList<>();
        for (FileObject child : folder.getChildren()) {
            if (!child.isData() || !child.hasExt(REF_EXT)) {
                continue;
            }
            if (!(child.getAttribute("actionId") instanceof String actionId) || actionId.isBlank()) {
                continue;
            }
            if (!mimeMatches(child.getAttribute("mimeType"), mimeType)) {
                continue;
            }
            int position = child.getAttribute("position") instanceof Integer p ? p : Integer.MAX_VALUE;
            boolean separatorBefore = Boolean.TRUE.equals(child.getAttribute("separatorBefore"));
            refs.add(new FxActionRef(actionId, position, separatorBefore));
        }
        refs.sort(Comparator.comparingInt(FxActionRef::position).thenComparing(FxActionRef::actionId));
        return List.copyOf(refs);
    }

    /**
     * Reads the mime-filtered action references from the layer folder at {@code layerPath}; see
     * {@link #readForMime(FileObject, String)}.
     *
     * @param layerPath the layer path (for example {@code "NbFx/ContextMenus/Navigator"}); may be null
     * @param mimeType  the content type of the node, or {@code null}
     * @return the ordered references, never {@code null}
     */
    public static List<FxActionRef> readForMime(String layerPath, String mimeType) {
        return readForMime(layerPath == null ? null : FileUtil.getConfigFile(layerPath), mimeType);
    }

    /** A reference with no declared mime applies everywhere; otherwise it must equal the node's mime. */
    private static boolean mimeMatches(Object declared, String mimeType) {
        if (!(declared instanceof String declaredMime) || declaredMime.isBlank()) {
            return true;
        }
        return declaredMime.equals(mimeType);
    }

    /**
     * A nested menu folder: its {@link #displayName()}, {@link #position()} and {@link #folder()}.
     *
     * @param position       the position among the siblings, from the folder's {@code position} attribute
     * @param separatorBefore whether the submenu asks for a separator before it
     * @param displayName    the submenu's title, from its {@code displayName} attribute or folder name
     * @param folder         the layer folder holding the submenu's contents
     */
    public record FxSubmenu(int position, boolean separatorBefore, String displayName, FileObject folder) {

        static FxSubmenu of(FileObject folder) {
            String displayName = folder.getAttribute("displayName") instanceof String name && !name.isBlank()
                    ? name : folder.getName();
            int position = folder.getAttribute("position") instanceof Integer p ? p : Integer.MAX_VALUE;
            boolean separatorBefore = Boolean.TRUE.equals(folder.getAttribute("separatorBefore"));
            return new FxSubmenu(position, separatorBefore, displayName, folder);
        }
    }

    /**
     * Reads the nested folders directly under {@code folder} that hold menu content (a data {@code .ref}
     * file or another non-empty folder), ordered by position then display name. The parent folders are
     * the shell's submenus.
     *
     * @param folder the surface folder (for example {@code NbFx/Menus/Window}); may be {@code null}
     * @return the ordered submenu folders, never {@code null}
     */
    public static List<FxSubmenu> readSubmenus(FileObject folder) {
        if (folder == null) {
            return List.of();
        }
        List<FxSubmenu> submenus = new ArrayList<>();
        for (FileObject child : folder.getChildren()) {
            if (child.isFolder() && hasContent(child)) {
                submenus.add(FxSubmenu.of(child));
            }
        }
        submenus.sort(Comparator.comparingInt(FxSubmenu::position).thenComparing(FxSubmenu::displayName));
        return List.copyOf(submenus);
    }

    /** Whether {@code folder} contains an action reference, directly or nested in a child folder. */
    private static boolean hasContent(FileObject folder) {
        for (FileObject child : folder.getChildren()) {
            if (child.isData() && child.hasExt(REF_EXT)) {
                return true;
            }
            if (child.isFolder() && hasContent(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads the action references from the layer folder at {@code layerPath} (for example
     * {@code "NbFx/Menus/File"}).
     *
     * @param layerPath the layer path; may be {@code null}
     * @return the ordered references, never {@code null}
     */
    public static List<FxActionRef> read(String layerPath) {
        return read(layerPath == null ? null : FileUtil.getConfigFile(layerPath));
    }
}
