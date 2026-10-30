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
package org.netbeans.modules.nbfx.platform.progress;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.netbeans.modules.progress.spi.InternalHandle;
import org.netbeans.modules.progress.spi.ProgressEvent;
import org.netbeans.modules.progress.spi.ProgressUIWorkerWithModel;
import org.netbeans.modules.progress.spi.TaskModel;
import org.openide.util.NbBundle;

/**
 * The FX progress worker: it consumes the headless progress events and keeps a JavaFX-observable
 * model of the running tasks, rendered by {@link FxProgressElement} in the status line and by an
 * all-tasks popup. Reuses the NetBeans {@link org.netbeans.modules.progress.spi.Controller}/
 * {@link TaskModel} for the task bookkeeping.
 * <p>
 * Events arrive on the controller's own thread; they are queued and applied in a single
 * {@code Platform.runLater} batch, so a burst of progress updates coalesces into one JavaFX pass.
 */
public final class FxProgressWorker implements ProgressUIWorkerWithModel {

    private static final FxProgressWorker INSTANCE = new FxProgressWorker();

    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final Map<InternalHandle, Task> byHandle = new HashMap<>();
    private final ReadOnlyObjectWrapper<Task> active = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(-1);
    private final ReadOnlyIntegerWrapper count = new ReadOnlyIntegerWrapper(0);

    private final Queue<ProgressEvent> pending = new ConcurrentLinkedQueue<>();
    private final AtomicReference<InternalHandle> pendingSelection = new AtomicReference<>();
    private final AtomicBoolean drainScheduled = new AtomicBoolean();

    private Popup popup;
    private Node anchor;

    /** Package-private for tests; production code uses {@link #getInstance()}. */
    FxProgressWorker() {
    }

    public static FxProgressWorker getInstance() {
        return INSTANCE;
    }

    /** The running tasks, in start order. Read on the JavaFX thread. */
    public ObservableList<Task> getTasks() {
        return FXCollections.unmodifiableObservableList(tasks);
    }

    /** The task shown in the status line (the watched task, else the most recent one). */
    public ReadOnlyObjectProperty<Task> activeTaskProperty() {
        return active.getReadOnlyProperty();
    }

    /** The active task's progress in [0,1], or {@code -1} for an indeterminate task / no task. */
    public ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }

    /** The number of running tasks. */
    public ReadOnlyIntegerProperty taskCountProperty() {
        return count.getReadOnlyProperty();
    }

    /** Anchors the popup to the status element that shows this worker. */
    public void setAnchor(Node anchor) {
        this.anchor = anchor;
    }

    @Override
    public void setModel(TaskModel model) {
        // The worker keeps its own JavaFX model; the controller's TaskModel is not read here.
    }

    @Override
    public void showPopup() {
        runOnFx(this::showPopupFx);
    }

    @Override
    public void processProgressEvent(ProgressEvent event) {
        pending.add(event);
        if (drainScheduled.compareAndSet(false, true)) {
            runOnFx(this::drain);
        }
    }

    @Override
    public void processSelectedProgressEvent(ProgressEvent event) {
        // Apply the selection in the same drain as the events: a Start event that creates the
        // task may still be queued, and selecting immediately would lose the selection.
        pendingSelection.set(event.getSource());
        if (drainScheduled.compareAndSet(false, true)) {
            runOnFx(this::drain);
        }
    }

    /** Makes {@code task} the one shown in the status line. */
    public void select(Task task) {
        if (task == null) {
            return;
        }
        active.set(task);
        progress.set(task.getProgress());
    }

    /** Cancels {@code task} when the underlying handle allows it. */
    public void cancel(Task task) {
        if (task.handle().isAllowCancel()) {
            task.handle().requestCancel();
        }
    }

    /** Opens the task's linked output when the underlying handle provides a view action. */
    public void view(Task task) {
        if (task.handle().isAllowView()) {
            task.handle().requestView();
        }
    }

    private void drain() {
        drainScheduled.set(false);
        ProgressEvent event;
        while ((event = pending.poll()) != null) {
            apply(event);
        }
        InternalHandle selected = pendingSelection.getAndSet(null);
        if (selected != null) {
            select(byHandle.get(selected));
        }
    }

    private void apply(ProgressEvent event) {
        InternalHandle handle = event.getSource();
        switch (event.getType()) {
            case ProgressEvent.TYPE_START -> {
                Task task = new Task(handle, displayName(event, handle), event.getMessage());
                byHandle.put(handle, task);
                tasks.add(task);
                if (event.isWatched() || active.get() == null) {
                    active.set(task);
                }
                refresh();
            }
            case ProgressEvent.TYPE_FINISH -> {
                Task task = byHandle.remove(handle);
                if (task != null) {
                    tasks.remove(task);
                }
                if (active.get() != null && active.get().handle() == handle) {
                    active.set(nextActive());
                }
                refresh();
            }
            default -> {
                Task task = byHandle.get(handle);
                if (task == null) {
                    // NetBeans sends Progress/Switch/Silent events for handles that had no Start
                    // (see progress.ui StatusLineComponent); materialise the task instead of
                    // dropping the update.
                    task = new Task(handle, displayName(event, handle), event.getMessage());
                    byHandle.put(handle, task);
                    tasks.add(task);
                }
                task.update(displayName(event, handle), event.getMessage(), event.getPercentageDone());
                if (active.get() == null) {
                    active.set(task);
                }
                refresh();
            }
        }
    }

    /** The task to fall back to when the active one finishes: the last user task, else the last one. */
    private Task nextActive() {
        Task last = null;
        for (Task task : tasks) {
            if (!task.isSystemTask()) {
                last = task;
            }
        }
        return last != null ? last : (tasks.isEmpty() ? null : tasks.get(tasks.size() - 1));
    }

    private void refresh() {
        Task task = active.get();
        progress.set(task == null ? -1 : task.getProgress());
        count.set(tasks.size());
    }

    private static String displayName(ProgressEvent event, InternalHandle handle) {
        String name = event.getDisplayName();
        return name != null ? name : handle.getDisplayName();
    }

    private void showPopupFx() {
        if (tasks.isEmpty() || anchor == null || anchor.getScene() == null) {
            return;
        }
        if (popup == null) {
            popup = createPopup();
        }
        if (popup.isShowing()) {
            popup.hide();
            return;
        }
        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null || bounds.getWidth() <= 0) {
            return;
        }
        // Popup#getHeight() is 0 before the first show; show at the anchor and move above it once
        // the popup has been laid out.
        popup.show(anchor, bounds.getMinX(), bounds.getMinY());
        popup.setY(bounds.getMinY() - popup.getHeight());
    }

    private Popup createPopup() {
        Label title = new Label(NbBundle.getMessage(FxProgressWorker.class, "Progress.title"));
        title.setStyle("-fx-font-weight: bold;");

        ListView<Task> list = new ListView<>(tasks);
        list.setPrefWidth(380);
        list.setPrefHeight(200);
        list.setCellFactory(view -> new TaskCell(this));

        VBox content = new VBox(6, title, list);
        content.getStyleClass().add("progress-popup");
        content.setStyle("-fx-padding: 8; -fx-background-color: -fx-background;");

        Popup p = new Popup();
        p.setAutoHide(true);
        p.getContent().add(content);
        return p;
    }

    private static void runOnFx(Runnable action) {
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException ex) {
            // The JavaFX toolkit is not running (headless tests): apply inline.
            action.run();
        }
    }

    /** One running task, shown in the status line and in the popup. */
    public static final class Task {

        private final InternalHandle handle;
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty detail = new SimpleStringProperty();
        private final SimpleDoubleProperty progress = new SimpleDoubleProperty(-1);

        Task(InternalHandle handle, String name, String detail) {
            this.handle = handle;
            this.name.set(name);
            this.detail.set(detail);
            this.progress.set(handle.getPercentageDone());
        }

        InternalHandle handle() {
            return handle;
        }

        public StringProperty nameProperty() {
            return name;
        }

        public StringProperty detailProperty() {
            return detail;
        }

        public double getProgress() {
            return progress.get();
        }

        /** The task's progress in [0,1], or {@code -1} for an indeterminate task. */
        public ReadOnlyDoubleProperty progressProperty() {
            return progress;
        }

        public boolean isCancellable() {
            return handle.isAllowCancel();
        }

        public boolean isViewable() {
            return handle.isAllowView();
        }

        /** A task not triggered by an explicit user action (NetBeans' "system" task). */
        public boolean isSystemTask() {
            return !handle.isUserInitialized();
        }

        void update(String name, String detail, double progress) {
            if (name != null) {
                this.name.set(name);
            }
            this.detail.set(detail);
            this.progress.set(progress);
        }
    }

    /** A popup row: name + detail, a progress bar, a View and a Cancel button. */
    private static final class TaskCell extends ListCell<Task> {

        private final FxProgressWorker worker;
        private final Label name = new Label();
        private final Label detail = new Label();
        private final ProgressBar bar = new ProgressBar();
        private final Button view = new Button();
        private final Button cancel = new Button();
        private final HBox root;

        TaskCell(FxProgressWorker worker) {
            this.worker = worker;
            name.setStyle("-fx-font-weight: bold;");
            detail.getStyleClass().add("progress-detail");
            bar.setPrefWidth(110);
            view.setText(NbBundle.getMessage(FxProgressWorker.class, "Progress.view"));
            cancel.setText(NbBundle.getMessage(FxProgressWorker.class, "Progress.cancel"));

            VBox texts = new VBox(name, detail);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            root = new HBox(8, texts, spacer, bar, view, cancel);
            root.setAlignment(Pos.CENTER_LEFT);

            view.setOnAction(event -> {
                if (getItem() != null) {
                    worker.view(getItem());
                }
            });
            cancel.setOnAction(event -> {
                if (getItem() != null) {
                    worker.cancel(getItem());
                }
            });
        }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            bar.progressProperty().unbind();
            if (empty || task == null) {
                setGraphic(null);
                return;
            }
            name.textProperty().unbind();
            detail.textProperty().unbind();
            name.textProperty().bind(task.nameProperty());
            detail.textProperty().bind(task.detailProperty());
            detail.setStyle(task.isSystemTask() ? "-fx-opacity: 0.7;" : "");
            bar.progressProperty().bind(task.progressProperty());
            view.setVisible(task.isViewable());
            view.setManaged(task.isViewable());
            cancel.setVisible(task.isCancellable());
            cancel.setManaged(task.isCancellable());
            setGraphic(root);
        }
    }
}
