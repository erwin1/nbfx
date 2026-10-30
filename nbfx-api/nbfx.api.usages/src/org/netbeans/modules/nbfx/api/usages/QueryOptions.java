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
package org.netbeans.modules.nbfx.api.usages;

/**
 * How a query searches: whether the target's name is also looked for in comments and which files
 * are searched - NetBeans' Find Usages dialog offers these two, among more; the query tab offers
 * them in a small menu instead. Whether comments can name a target is up to its language provider.
 *
 * @param searchComments look for the simple name in comments too
 * @param scope          the files searched
 */
public record QueryOptions(boolean searchComments, Scope scope) {

    /** The files a query searches. */
    public enum Scope {
        /** Every source root of every open project. */
        ALL_PROJECTS,
        /** The source roots of the project owning the target's file. */
        CURRENT_PROJECT,
        /** The files in the target's package - the folder of its file. */
        CURRENT_PACKAGE,
        /** The target's file alone. */
        CURRENT_FILE,
        /** The files open in editors. */
        OPEN_FILES
    }

    public static final QueryOptions DEFAULT = new QueryOptions(true, Scope.ALL_PROJECTS);

    public QueryOptions withSearchComments(boolean value) {
        return new QueryOptions(value, scope);
    }

    public QueryOptions withScope(Scope value) {
        return new QueryOptions(searchComments, value);
    }
}
