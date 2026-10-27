package com.gluonhq.netbeans.nbfx.progress;

import com.gluonhq.netbeans.nbfx.annotations.StatusAlignment;
import com.gluonhq.netbeans.nbfx.annotations.StatusRegistration;
import com.gluonhq.netbeans.nbfx.statusbar.FxStatusElement;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import org.openide.util.NbBundle;

/**
 * The status-line progress element: an aggregate bar, the active task's name and a button that opens
 * the all-tasks popup (plus a cancel button for a cancellable active task). Driven by the
 * {@link FxProgressWorker} singleton, which receives events from the reused progress controller.
 */
@StatusRegistration(id = "progress", alignment = StatusAlignment.LEFT, position = 100)
public final class FxProgressElement implements FxStatusElement {

    private final FxProgressWorker worker = FxProgressWorker.getInstance();
    private final ProgressBar bar = new ProgressBar();
    private final Label label = new Label();
    private final Button taskCount = new Button();
    private final Button cancel = new Button();
    private final HBox node;

    private static final StringProperty NO_TASK = new SimpleStringProperty("");

    /** Creates the element. Must be called on the JavaFX Application Thread. */
    public FxProgressElement() {
        bar.setPrefWidth(120);
        bar.getStyleClass().add("status-bar-progress");
        taskCount.setOnAction(event -> worker.showPopup());
        taskCount.getStyleClass().add("status-bar-count");
        cancel.getStyleClass().add("status-bar-cancel");
        cancel.setOnAction(event -> {
            FxProgressWorker.Task active = worker.activeTaskProperty().get();
            if (active != null) {
                worker.cancel(active);
            }
        });

        label.textProperty().bind(worker.activeTaskProperty().flatMap(
                task -> task == null ? NO_TASK : task.nameProperty()));
        cancel.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    FxProgressWorker.Task task = worker.activeTaskProperty().get();
                    return task != null && task.isCancellable();
                },
                worker.activeTaskProperty()));
        cancel.managedProperty().bind(cancel.visibleProperty());

        bar.progressProperty().bind(worker.progressProperty());
        taskCount.textProperty().bind(worker.taskCountProperty().asString());
        taskCount.visibleProperty().bind(worker.taskCountProperty().greaterThan(1));
        taskCount.managedProperty().bind(taskCount.visibleProperty());

        node = new HBox(6, bar, label, taskCount, cancel);
        node.getStyleClass().add("status-bar-progress-group");
        node.setAlignment(Pos.CENTER_LEFT);
        node.visibleProperty().bind(worker.taskCountProperty().greaterThan(0));
        node.managedProperty().bind(node.visibleProperty());
        worker.setAnchor(node);
    }

    @Override
    public String getId() {
        return "progress";
    }

    @Override
    public Node getNode() {
        return node;
    }
}
