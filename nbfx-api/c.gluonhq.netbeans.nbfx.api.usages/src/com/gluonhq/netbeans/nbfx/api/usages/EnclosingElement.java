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
package com.gluonhq.netbeans.nbfx.api.usages;

import com.gluonhq.netbeans.nbfx.api.elements.SourceElementKind;
import com.gluonhq.netbeans.nbfx.api.elements.SourceTypeKind;

/**
 * One declaration enclosing a usage, outermost first in {@link UsageContext#enclosing()}: a type,
 * a method or constructor, a field or an initializer.
 *
 * @param kind       the declaration's kind
 * @param typeKind   the type's sub-kind for {@link SourceElementKind#TYPE} (and constructors), else
 *                   {@link SourceTypeKind#OTHER}
 * @param name       the display name: the simple type name, {@code method(ParamType, ...)} for
 *                   executables, the field name
 * @param modifiers  the declaration's modifiers as {@link java.lang.reflect.Modifier} bits, for its icon
 */
public record EnclosingElement(SourceElementKind kind, SourceTypeKind typeKind, String name, int modifiers) {
}
