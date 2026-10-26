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
package com.gluonhq.netbeans.nbfx.editor.codearea;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.stage.Window;
import jfx.incubator.scene.control.richtext.CodeArea;

/**
 * Moves keyboard focus to a {@link CodeArea} as soon as it is ready for it: waits, in turn, for
 * its skin, its scene, its window, and finally for that window to be focused.
 */
final class EditorFocusSupport {

    private EditorFocusSupport() {
    }

    /** Focuses the code area (so its caret starts blinking) once it is ready. */
    static void requestWhenReady(CodeArea codeArea) {
        if (codeArea.getSkin() == null) {
            onceReady(codeArea, codeArea.skinProperty());
            return;
        }
        Scene scene = codeArea.getScene();
        if (scene == null) {
            onceReady(codeArea, codeArea.sceneProperty());
            return;
        }
        Window window = scene.getWindow();
        if (window == null) {
            onceReady(codeArea, scene.windowProperty());
            return;
        }
        if (!window.isFocused()) {
            // Focus the editor as soon as its window gains focus.
            window.focusedProperty().addListener(new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> obs, Boolean ov, Boolean focused) {
                    if (Boolean.TRUE.equals(focused)) {
                        window.focusedProperty().removeListener(this);
                        Platform.runLater(codeArea::requestFocus);
                    }
                }
            });
            return;
        }
        Platform.runLater(codeArea::requestFocus);
    }

    /** Re-runs {@link #requestWhenReady(CodeArea)} once {@code property} becomes non-null. */
    private static void onceReady(CodeArea codeArea, ObservableValue<?> property) {
        property.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (property.getValue() != null) {
                    property.removeListener(this);
                    Platform.runLater(() -> requestWhenReady(codeArea));
                }
            }
        });
    }
}
