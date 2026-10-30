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
package com.gluonhq.netbeans.nbfx.api.elements;

import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Modifier helpers shared by providers that map language-model elements into the
 * {@link Modifier}-compatible bitmask convention used by the completion and breadcrumbs APIs.
 */
public final class SourceModifiers {

    private SourceModifiers() {
    }

    /**
     * Converts a set of {@code javax.lang.model.element.Modifier} constants into the
     * equivalent {@link Modifier} bitmask (visibility, {@code static},
     * {@code final} and {@code abstract} only).
     *
     * <p>Flags are matched by enum <em>name</em> rather than identity: at runtime the set may
     * hold constants of nb-javac's own copy of {@code javax.lang.model.element.Modifier}
     * (bundled by {@code libs.nbjavacapi}), which is a different class than the JDK's that
     * this module sees, so an identity-based {@code contains} would never match.</p>
     *
     * @param modifiers language-model modifiers (any {@code Modifier}-like enum constants),
     *                  may be {@code null}
     * @return the reflect-compatible bitmask
     */
    public static int toModifierBits(Set<?> modifiers) {
        int bits = 0;
        if (modifiers != null) {
            for (Object modifier : modifiers) {
                if (modifier instanceof Enum<?> flag) {
                    bits |= switch (flag.name()) {
                        case "PUBLIC" -> Modifier.PUBLIC;
                        case "PROTECTED" -> Modifier.PROTECTED;
                        case "PRIVATE" -> Modifier.PRIVATE;
                        case "STATIC" -> Modifier.STATIC;
                        case "FINAL" -> Modifier.FINAL;
                        case "ABSTRACT" -> Modifier.ABSTRACT;
                        default -> 0;
                    };
                }
            }
        }
        return bits;
    }
}
