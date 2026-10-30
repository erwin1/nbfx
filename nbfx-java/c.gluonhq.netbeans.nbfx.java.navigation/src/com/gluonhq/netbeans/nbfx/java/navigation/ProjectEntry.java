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
package com.gluonhq.netbeans.nbfx.java.navigation;

import org.openide.filesystems.FileObject;

/**
 *
 * @author johan
 */
public class ProjectEntry {
    public enum Type {
        NAME,
        MODULES,
        GROUP,
        PACKAGE,
        FILES,
        FILE,
        LIBRARIES,
        LIBRARY,
        DIRECTORY
    }

    public enum BADGE {
        PACKAGE_BADGE,
        MODULES_BADGE,
        LIBRARIES_BADGE,
        OTHERS_BADGE,
        PROJECTFILES_BADGE,
        NO_BADGE
    }

    private final FileObject fileObject;
    private final String name;
    private String iconName;

    private final Type type;
    private final BADGE badge;
    
    public ProjectEntry(FileObject fileObject, Type type) {
        this.fileObject = fileObject;
        this.name = fileObject.getNameExt();
        this.type = type;
        this.badge = BADGE.NO_BADGE;
    }

    public ProjectEntry(FileObject fileObject, String name, Type type) {
        this(fileObject, name, type, BADGE.NO_BADGE);
    }

    public ProjectEntry(FileObject fileObject, String name, Type type, BADGE badge) {
        this(fileObject, name, type, badge, null);
    }

    public ProjectEntry(FileObject fileObject, String name, Type type, BADGE badge, String iconName) {
        this.fileObject = fileObject;
        this.name = name;
        this.type = type;
        this.iconName = iconName;
        this.badge = badge;
    }

    public boolean isFolder() {
        if (fileObject != null) return fileObject.isFolder();
        return true;
    }

    public boolean isFileObject() {
        return (this.fileObject != null);
    }

    public FileObject getFileObject() {
        return this.fileObject;
    }

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return this.type;
    }

    public String getIconName() {
        return this.iconName;
    }

    public BADGE getBadge() {
        return badge;
    }
}
