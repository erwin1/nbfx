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
package org.netbeans.modules.nbfx.ide.search.query;

/**
 * The scope options of NetBeans' search form.
 *
 * @param searchInArchives  descend into jar / zip files
 * @param searchInGenerated also search the folders the project marks as not sharable - the
 *                          build output, where generated sources live (NetBeans drops its
 *                          sharability filter for this)
 * @param useIgnoreList     skip the folders and path patterns of the {@link IgnoreList}
 */
public record ScopeOptions(boolean searchInArchives, boolean searchInGenerated, boolean useIgnoreList) {

    /** NetBeans' defaults: no archives, no generated sources, ignore list on. */
    public static final ScopeOptions DEFAULT = new ScopeOptions(false, false, true);

    public ScopeOptions withSearchInArchives(boolean on) {
        return new ScopeOptions(on, searchInGenerated, useIgnoreList);
    }

    public ScopeOptions withSearchInGenerated(boolean on) {
        return new ScopeOptions(searchInArchives, on, useIgnoreList);
    }

    public ScopeOptions withUseIgnoreList(boolean on) {
        return new ScopeOptions(searchInArchives, searchInGenerated, on);
    }
}
