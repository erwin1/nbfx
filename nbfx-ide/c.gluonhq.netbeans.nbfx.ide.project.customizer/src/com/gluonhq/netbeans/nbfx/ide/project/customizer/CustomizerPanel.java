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
package com.gluonhq.netbeans.nbfx.ide.project.customizer;

import javafx.scene.Node;
import org.netbeans.api.project.Project;

/**
 * Service provider interface for a project-properties category, mirroring the original
 * {@code CustomizerPanel} / {@code Projects/<type>/Customizer/<category>} registration but rendering
 * in JavaFX.
 *
 * <p>Register implementations with {@code @ServiceProvider(service = CustomizerPanel.class)}.
 * A panel with a {@code null} {@link #projectTypeId() type id} applies to every project; otherwise it
 * is only shown for projects whose {@link com.gluonhq.netbeans.nbfx.project.ui.api.ProjectKindProvider}
 * id matches.</p>
 */
public interface CustomizerPanel {

    /** The project-kind id this panel applies to, or {@code null} for any project. */
    default String projectTypeId() {
        return null;
    }

    /**
     * Whether this panel applies to {@code project}, whose kind id is {@code kindId}. Defaults to
     * matching {@link #projectTypeId()} against {@code kindId}; a panel refines this to distinguish
     * two project types that share a kind (e.g. apisupport vs J2SE, both Ant-based).
     */
    default boolean appliesTo(Project project, String kindId) {
        String type = projectTypeId();
        return type == null || type.equals(kindId);
    }

    /** A stable id for the category (e.g. {@code "sources"}). */
    String id();

    /** The id of the parent category, or {@code null} for a top-level category. */
    default String parentId() {
        return null;
    }

    /** The category's display name, shown in the category list. */
    String displayName();

    /** Ordering among categories, ascending. */
    default int position() {
        return 100;
    }

    /**
     * Builds the JavaFX panel for {@code project}, or {@code null} when this category is only a
     * folder holding sub-categories. Called on the JavaFX thread.
     */
    Node createPanel(Project project);

    /** Whether the panel has changes to apply. Drives the Apply button's enablement. */
    default boolean isChanged() {
        return false;
    }

    /** Whether the panel's current input is valid. When {@code false}, Apply and OK are disabled. */
    default boolean isValid() {
        return true;
    }

    /** Persists the panel's changes. */
    default void apply(Project project) {
    }

    /** Discards the panel's changes. */
    default void cancel() {
    }
}
