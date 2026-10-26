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
package com.gluonhq.netbeans.nbfx.api.actions;

import java.util.Map;
import javafx.scene.input.KeyCombination;

/**
 * The user-configurable keyboard map: it maps a command id to the accelerator the user chose,
 * overriding the command's built-in default. Resolved through the global
 * {@link org.openide.util.Lookup}; when no implementation is registered, commands keep their
 * defaults.
 *
 * @since 1.0
 */
public interface KeyMap {

    /**
     * The accelerator to use for {@code actionId}: the user's override when one is set (an empty
     * override means the user removed the shortcut), otherwise {@code defaultAccelerator}.
     */
    KeyCombination accelerator(String actionId, KeyCombination defaultAccelerator);

    /**
     * Sets the user's accelerator for {@code actionId}. A {@code null} accelerator records that the
     * user removed the shortcut (as opposed to having no override at all).
     */
    void setAccelerator(String actionId, KeyCombination accelerator);

    /** Removes any user override for {@code actionId}, restoring the command's default shortcut. */
    void reset(String actionId);

    /** The user's overrides, action id to accelerator (a {@code null} value means "removed"). */
    Map<String, KeyCombination> overrides();
}
