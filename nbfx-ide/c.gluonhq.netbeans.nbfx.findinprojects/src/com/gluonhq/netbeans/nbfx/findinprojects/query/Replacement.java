/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gluonhq.netbeans.nbfx.findinprojects.query;

import java.util.Objects;

/**
 * The "Replace With" of a Replace in Projects search; {@code null} in a plain search.
 *
 * @param text         the replacement; {@code $1} group references in a regular-expression search
 * @param preserveCase the replacement takes the case shape of the text it replaces (ignore-case
 *                     literal and wildcard searches only)
 */
public record Replacement(String text, boolean preserveCase) {

    public Replacement {
        Objects.requireNonNull(text);
    }
}
