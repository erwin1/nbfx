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
package com.gluonhq.netbeans.nbfx.findusages.actions;

import com.gluonhq.netbeans.nbfx.api.actions.AbstractCommand;
import com.gluonhq.netbeans.nbfx.api.actions.ActionIds;
import com.gluonhq.netbeans.nbfx.findusages.ui.UsagesViewProvider;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.openide.util.NbBundle;

/** Window ▸ Usages (Shortcut+3): opens or fronts the Usages view. Always enabled. */
final class SelectUsagesCommand extends AbstractCommand {

    /** The view's icon, as on its tab and its root tree item. */
    private static final Image ICON = new Image(Objects.requireNonNull(
            SelectUsagesCommand.class.getResource("/com/gluonhq/netbeans/nbfx/findusages/ui/icons/findusages.png")).toExternalForm());

    @Override
    public Node getIcon() {
        return new ImageView(ICON);
    }

    SelectUsagesCommand() {
        super(ActionIds.SELECT_USAGES, NbBundle.getMessage(SelectUsagesCommand.class, "CTL_SelectUsagesCommand"),
                new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN), false);
    }

    @Override
    public void run() {
        UsagesViewProvider view = UsagesViewProvider.instance();
        if (view != null) {
            view.show(null);
        }
    }
}
