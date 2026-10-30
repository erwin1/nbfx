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
package org.netbeans.modules.nbfx.propertysheet;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * A pure-JavaFX property sheet: a two-column tree (Name / Value) whose branches are the
 * {@link PropertySet}s and whose leaves are the {@link Property properties}, with an expert
 * toggle and per-type inline editors from {@link PropertyEditors}.
 *
 * <p>Reused for Project Properties, File Properties and the future FXML component inspector. The
 * caller sets the context object; the sheet asks every registered {@link PropertiesProvider} for
 * its property sets.</p>
 */
public final class PropertySheet extends BorderPane {

    private final TreeTableView<Object> tree = new TreeTableView<>();
    private final CheckBox expert = new CheckBox(message("PropertySheet.expert"));
    private Object context;
    private boolean showExpert;

    public PropertySheet() {
        TreeTableColumn<Object, String> nameColumn = new TreeTableColumn<>(message("PropertySheet.name"));
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(displayName(cell.getValue().getValue())));
        nameColumn.setPrefWidth(180);

        TreeTableColumn<Object, Object> valueColumn = new TreeTableColumn<>(message("PropertySheet.value"));
        valueColumn.setCellValueFactory(cell -> {
            Object value = cell.getValue().getValue();
            return value instanceof Property property
                    ? property.valueObservable()
                    : new ReadOnlyObjectWrapper<>();
        });
        valueColumn.setCellFactory(column -> new TreeTableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                TreeItem<Object> row = getTreeTableRow() == null ? null : getTreeTableRow().getTreeItem();
                Object value = row == null ? null : row.getValue();
                if (value instanceof Property property) {
                    setGraphic(PropertyEditors.createEditor(property));
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(null);
                }
            }
        });

        tree.getColumns().add(nameColumn);
        tree.getColumns().add(valueColumn);
        tree.setShowRoot(false);
        tree.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        expert.selectedProperty().addListener((observable, old, now) -> {
            showExpert = now;
            rebuild();
        });
        setTop(expert);
        setCenter(tree);
    }

    /** Sets the context object whose properties are shown, and rebuilds the sheet. */
    public void setContext(Object context) {
        this.context = context;
        rebuild();
    }

    private void rebuild() {
        TreeItem<Object> root = new TreeItem<>();
        if (context != null) {
            for (PropertiesProvider provider : Lookup.getDefault().lookupAll(PropertiesProvider.class)) {
                for (PropertySet set : provider.getPropertySets(context)) {
                    TreeItem<Object> setItem = new TreeItem<>(set);
                    for (Property property : set.getProperties()) {
                        if (property.isExpert() && !showExpert) {
                            continue;
                        }
                        setItem.getChildren().add(new TreeItem<>(property));
                    }
                    root.getChildren().add(setItem);
                }
            }
        }
        tree.setRoot(root);
    }

    private static String displayName(Object value) {
        if (value instanceof PropertySet set) {
            return set.getDisplayName();
        }
        if (value instanceof Property property) {
            return property.getDisplayName();
        }
        return "";
    }

    private static String message(String key) {
        return NbBundle.getMessage(PropertySheet.class, key);
    }
}
