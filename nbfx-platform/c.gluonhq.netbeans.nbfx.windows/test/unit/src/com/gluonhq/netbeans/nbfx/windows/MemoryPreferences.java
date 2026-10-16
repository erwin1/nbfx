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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A Preferences implementation backed by an in-memory map, so tests never touch the OS preference
 * store. The tree is a plain string-to-string map per node; {@link AbstractPreferences} provides the
 * node/keys API on top.
 */
final class MemoryPreferences extends AbstractPreferences {

    private static final MemoryPreferences ROOT = new MemoryPreferences(null, "");

    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, MemoryPreferences> children = new LinkedHashMap<>();

    private MemoryPreferences(MemoryPreferences parent, String name) {
        super(parent, name);
    }

    static MemoryPreferences root() {
        return ROOT;
    }

    @Override
    protected String getSpi(String key) {
        return values.get(key);
    }

    @Override
    protected void putSpi(String key, String value) {
        values.put(key, value);
    }

    @Override
    protected void removeSpi(String key) {
        values.remove(key);
    }

    @Override
    protected String[] keysSpi() {
        return values.keySet().toArray(new String[0]);
    }

    @Override
    protected String[] childrenNamesSpi() {
        return children.keySet().toArray(new String[0]);
    }

    @Override
    protected MemoryPreferences childSpi(String name) {
        MemoryPreferences child = children.get(name);
        if (child == null) {
            child = new MemoryPreferences(this, name);
            children.put(name, child);
        }
        return child;
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        MemoryPreferences parent = (MemoryPreferences) parent();
        if (parent != null) {
            parent.children.remove(name());
        }
    }

    @Override
    protected void flushSpi() {
    }

    @Override
    protected void syncSpi() {
    }
}