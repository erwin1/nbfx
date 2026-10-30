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
package com.gluonhq.netbeans.nbfx.java.j2seproject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 * Base for J2SE customizer panels that edit a fixed set of {@code nbproject/project.properties}
 * keys, one text field per key.
 */
abstract class J2SEPropertiesCustomizerPanel extends J2SECustomizerPanel {

    /** A property key and the bundle key of its label. */
    protected record Field(String key, String labelKey) {
    }

    private final Map<String, TextField> inputs = new LinkedHashMap<>();
    private final Map<String, String> initial = new LinkedHashMap<>();

    /** The properties this panel edits. */
    protected abstract List<Field> fields();

    @Override
    public Node createPanel(Project project) {
        FileObject dir = project.getProjectDirectory();
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        ColumnConstraints labels = new ColumnConstraints();
        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, values);
        int row = 0;
        for (Field field : fields()) {
            String value = AntProjectFiles.property(dir, field.key());
            initial.put(field.key(), value);
            TextField input = new TextField(value == null ? "" : value);
            inputs.put(field.key(), input);
            grid.addRow(row++, new Label(message(field.labelKey())), input);
        }
        return grid;
    }

    @Override
    public boolean isChanged() {
        for (Field field : fields()) {
            if (!orEmpty(initial.get(field.key())).equals(inputs.get(field.key()).getText().trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply(Project project) {
        for (Field field : fields()) {
            String now = inputs.get(field.key()).getText().trim();
            if (!orEmpty(initial.get(field.key())).equals(now)) {
                AntProjectFiles.setProperty(project.getProjectDirectory(), field.key(), now);
            }
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String message(String key) {
        return NbBundle.getMessage(J2SEPropertiesCustomizerPanel.class, key);
    }

    /** Convenience for subclasses. */
    protected static List<Field> fields(Field... fields) {
        List<Field> list = new ArrayList<>();
        for (Field field : fields) {
            list.add(field);
        }
        return list;
    }
}
