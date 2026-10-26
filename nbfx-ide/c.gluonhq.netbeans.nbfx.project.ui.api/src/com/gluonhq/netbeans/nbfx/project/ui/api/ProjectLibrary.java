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
package com.gluonhq.netbeans.nbfx.project.ui.api;

/**
 * A library / dependency shown under a project's "libraries" group. Purely descriptive: it has a
 * name, an optional detail (e.g. a version) and an optional icon resource name.
 *
 * @param name     the library's display name
 * @param detail   an optional detail shown in parentheses, or {@code null}
 * @param iconName an optional icon resource name, or {@code null} for the default
 */
public record ProjectLibrary(String name, String detail, String iconName) {
}
