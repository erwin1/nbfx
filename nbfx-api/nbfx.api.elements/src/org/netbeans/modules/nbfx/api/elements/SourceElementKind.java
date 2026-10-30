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
package org.netbeans.modules.nbfx.api.elements;

/**
 * Primary semantic category of a source element, shared by the completion and
 * breadcrumbs APIs.
 *
 * <p>Renderers use this kind to choose icons and row treatment.
 */
public enum SourceElementKind {
    /** Callable member, including ordinary methods and constructors. */
    METHOD,
    /** Field-like member (field or enum constant). */
    FIELD,
    /** Type such as class, interface, enum, or record. Its subcategories are defined in {@link SourceTypeKind} */
    TYPE,
    /** Java package segment. */
    PACKAGE,
    /** Java module name (module-descriptor {@code requires}/{@code to} operands). */
    MODULE,
    /** Java keyword or literal keyword. */
    KEYWORD,
    /** Local variable/parameter-like symbol. */
    VARIABLE,
    /** Markup tag (XML/FXML element). */
    TAG,
    /** Stylesheet rule (CSS). */
    RULE,
    /** Element with no specific semantic category. */
    OTHER,

    /** Visual separator item between groups. Not an actual Java semantic type */
    SEPARATOR
}
