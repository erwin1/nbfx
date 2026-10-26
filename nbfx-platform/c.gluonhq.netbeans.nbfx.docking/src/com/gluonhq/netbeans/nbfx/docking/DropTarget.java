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
package com.gluonhq.netbeans.nbfx.docking;

import javafx.geometry.Side;
import javafx.scene.Node;

/**
 * Where an item dragged over a pane of a {@link DockArea} would dock: in a new pane on one
 * {@code side} of the hovered {@code pane}, or - when {@code pane} is {@code null} - in a new pane
 * along one side of the whole area. See {@link DockArea#dropTargetAt}.
 *
 * @param <N> the type of the area's panes
 */
public record DropTarget<N extends Node>(N pane, Side side) {

    /** Whether the target is an edge of the whole area rather than of a pane. */
    public boolean isArea() {
        return pane == null;
    }
}
