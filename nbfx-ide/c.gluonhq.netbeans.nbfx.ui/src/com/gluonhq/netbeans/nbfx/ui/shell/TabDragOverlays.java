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
package com.gluonhq.netbeans.nbfx.ui.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * An overlay is a near-transparent stage per screen that is used to catch drop events outside the
 * current stage, preventing click-through events.
 */
public final class TabDragOverlays {

    private static final List<Stage> overlays = new ArrayList<>();
    private static BiConsumer<Tab, Point2D> dropHandler;

    private TabDragOverlays() {}

    /** Sets the callback fired when a tab is dropped on the overlay */
    static void setDropHandler(BiConsumer<Tab, Point2D> overlayDropHandler) {
        dropHandler = overlayDropHandler;
    }

    /** Shows one overlay per screen, sized to that screen's full bounds and pushed to the back. */
    static void showAll() {
        List<Screen> screens = Screen.getScreens();
        while (overlays.size() > screens.size()) {
            overlays.removeLast().hide();
        }
        while (overlays.size() < screens.size()) {
            overlays.add(createOverlay());
        }
        for (int i = 0; i < screens.size(); i++) {
            Rectangle2D b = screens.get(i).getBounds();
            Stage stage = overlays.get(i);
            stage.setX(b.getMinX());
            stage.setY(b.getMinY());
            stage.setWidth(b.getWidth());
            stage.setHeight(b.getHeight());
            stage.show();
            stage.toBack();
        }
    }

    /** hides all overlays */
    static void hideAll() {
        overlays.stream().filter(Window::isShowing).forEach(Window::hide);
    }

    private static Stage createOverlay() {
        Pane root = new Pane();
        root.setStyle("-fx-background-color: #00000001;");

        root.setOnDragOver(e -> {
            if (e.getDragboard().hasContent(NbfxTabPane.TAB_DATA_FORMAT)) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        root.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            NbfxTabPane.draggedTab(db).ifPresentOrElse(tab -> {
                e.setDropCompleted(true);
                if (dropHandler != null) {
                    dropHandler.accept(tab, new Point2D(e.getScreenX(), e.getScreenY()));
                }
            }, () -> e.setDropCompleted(false));
            e.consume();
        });

        Stage stage = new Stage(StageStyle.TRANSPARENT);
        Scene scene = new Scene(root);
        scene.setFill(Color.web("#00000001"));
        stage.setScene(scene);
        return stage;
    }
}

