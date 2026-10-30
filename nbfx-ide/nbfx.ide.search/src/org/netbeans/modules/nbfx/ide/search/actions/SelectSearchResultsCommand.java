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
package org.netbeans.modules.nbfx.ide.search.actions;

import org.netbeans.modules.nbfx.api.actions.AbstractCommand;
import org.netbeans.modules.nbfx.api.actions.ActionIds;
import org.netbeans.modules.nbfx.ide.search.ui.SearchViewProvider;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.openide.util.NbBundle;

/** Window ▸ Search Results (Shortcut+4, next to Usages' Shortcut+3): opens or fronts the Search view. Always enabled. */
final class SelectSearchResultsCommand extends AbstractCommand {

    /** The view's 16 px icon, the Find icon of the tool bar, as on its tab. */
    private static final Image ICON = new Image(Objects.requireNonNull(
            SelectSearchResultsCommand.class.getResource("/org/netbeans/modules/nbfx/ide/search/ui/icons/find-16.png")).toExternalForm());

    @Override
    public Node getIcon() {
        return new ImageView(ICON);
    }

    SelectSearchResultsCommand() {
        super(ActionIds.SELECT_SEARCH_RESULTS,
                NbBundle.getMessage(SelectSearchResultsCommand.class, "CTL_SelectSearchResultsCommand"),
                new KeyCodeCombination(KeyCode.DIGIT4, KeyCombination.SHORTCUT_DOWN), false);
    }

    @Override
    public void run() {
        SearchViewProvider view = SearchViewProvider.instance();
        if (view != null) {
            view.show(null);
        }
    }
}
