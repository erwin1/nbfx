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
package org.netbeans.modules.nbfx.ide.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Drag-and-drop of files, shared by the navigator trees (drag source and drop target) and the
 * editor tab panes (drop target).
 *
 * <p>An internal drag (from a navigator tree) publishes its files under a private
 * {@link #NBFX_FILES} format holding their absolute paths, rather than under the standard
 * {@link Dragboard#getFiles() files} format, so that dropping onto another application does
 * nothing. External drags (from Finder/Explorer) are read from the standard format, which makes
 * both gestures look the same to a drop target.</p>
 */
public final class FileDragAndDrop {

    /** Private format of an internal file drag: the absolute paths of the dragged files. */
    private static final DataFormat NBFX_FILES = new DataFormat("application/x-nbfx-files");

    private static final String PATH_SEPARATOR = "\n";

    /** Key marking a node that handles file drops itself (so ancestors must not intercept them). */
    private static final Object DROP_TARGET_KEY = new Object();

    /** Key of the per-drop-target resolution cache in {@link Node#getProperties()}. */
    private static final Object FILES_CACHE_KEY = new Object();

    private FileDragAndDrop() {}

    /** Publishes {@code files} on {@code content} as an internal file drag. */
    public static void putFiles(ClipboardContent content, List<FileObject> files) {
        StringBuilder paths = new StringBuilder();
        for (FileObject file : files) {
            File local = FileUtil.toFile(file);
            if (local == null) {
                continue;
            }
            if (!paths.isEmpty()) {
                paths.append(PATH_SEPARATOR);
            }
            paths.append(local.getAbsolutePath());
        }
        content.put(NBFX_FILES, paths.toString());
    }

    /** Whether {@code dragboard} carries files, dragged internally or from another application. */
    public static boolean hasFiles(Dragboard dragboard) {
        return dragboard != null && (dragboard.hasContent(NBFX_FILES) || dragboard.hasFiles());
    }

    /**
     * The existing files carried by {@code dragboard}, from an internal drag ({@link #putFiles})
     * or from an external one (Finder/Explorer), or an empty list if it carries no file.
     */
    public static List<FileObject> filesFrom(Dragboard dragboard) {
        if (dragboard == null) {
            return List.of();
        }
        List<File> files = new ArrayList<>();
        if (dragboard.getContent(NBFX_FILES) instanceof String paths) {
            for (String path : paths.split(PATH_SEPARATOR)) {
                if (!path.isBlank()) {
                    files.add(new File(path));
                }
            }
        } else if (dragboard.hasFiles()) {
            files.addAll(dragboard.getFiles());
        }
        List<FileObject> fileObjects = new ArrayList<>();
        for (File file : files) {
            FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(file));
            if (fo != null) {
                fileObjects.add(fo);
            }
        }
        return List.copyOf(fileObjects);
    }

    /**
     * The existing files carried by {@code dragboard}, caching the resolution on {@code owner}.
     * Drop targets ask for this on every mouse move of a drag, so the tree view or tab pane keeps
     * the last resolution until the dragboard changes or a file becomes invalid.
     */
    public static List<FileObject> filesFrom(Dragboard dragboard, Node owner) {
        if (dragboard == null) {
            return List.of();
        }
        if (owner == null) {
            return filesFrom(dragboard);
        }
        Cache cache = (Cache) owner.getProperties().get(FILES_CACHE_KEY);
        if (cache == null) {
            cache = new Cache();
            owner.getProperties().put(FILES_CACHE_KEY, cache);
        }
        return cache.filesFrom(dragboard);
    }

    /** The last resolution for one drop target; mutable, but owned by that target's node. */
    private static final class Cache {

        private Dragboard dragboard;
        private List<FileObject> files = List.of();

        List<FileObject> filesFrom(Dragboard dragboard) {
            if (dragboard == this.dragboard && files.stream().allMatch(FileObject::isValid)) {
                return files;
            }
            files = FileDragAndDrop.filesFrom(dragboard);
            this.dragboard = dragboard;
            return files;
        }
    }

    /** Whether {@code dragboard} carries files dragged from outside the application. */
    public static boolean isExternal(Dragboard dragboard) {
        return dragboard != null && !dragboard.hasContent(NBFX_FILES) && dragboard.hasFiles();
    }

    /**
     * Marks {@code node} as handling file drops on its own, so that an ancestor intercepting file
     * drags (the editor tab panes) leaves the ones aimed at it alone.
     */
    public static void markDropTarget(Node node) {
        if (node != null) {
            node.getProperties().put(DROP_TARGET_KEY, Boolean.TRUE);
        }
    }

    /** Whether {@code target} is, or sits inside, a node {@link #markDropTarget marked} as a file drop target. */
    public static boolean isWithinDropTarget(EventTarget target) {
        for (Node node = target instanceof Node n ? n : null; node != null; node = node.getParent()) {
            if (node.hasProperties() && node.getProperties().containsKey(DROP_TARGET_KEY)) {
                return true;
            }
        }
        return false;
    }
}
