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
package com.gluonhq.netbeans.nbfx.api.actions;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.scene.input.KeyCombination;

/**
 * A {@link ContextSensitiveCommand} built from functions: enablement is a {@link Predicate} over the
 * global {@link FxActionContext} and behaviour is a {@link Consumer} of it. This covers the many
 * commands that only need to read the current selection, so they do not each need a dedicated class.
 */
public class ContextAction extends ContextSensitiveCommand {

    private final Predicate<FxActionContext> enabled;
    private final Consumer<FxActionContext> action;

    /**
     * Creates a context-sensitive command from functions.
     *
     * @param id            the stable command id, never {@code null}
     * @param text          the display text, never {@code null}
     * @param accelerator   the preferred keyboard accelerator, or {@code null} if none
     * @param enabled       whether the command is enabled for a context, never {@code null}
     * @param action        what the command does with a context, never {@code null}
     * @param observedTypes the context types whose changes re-evaluate this command
     */
    @SuppressWarnings("this-escape")
    public ContextAction(String id, String text, KeyCombination accelerator,
            Predicate<FxActionContext> enabled, Consumer<FxActionContext> action,
            Class<?>... observedTypes) {
        super(id, text, accelerator, observedTypes);
        this.enabled = Objects.requireNonNull(enabled);
        this.action = Objects.requireNonNull(action);
    }

    @Override
    protected boolean isEnabled(FxActionContext context) {
        return enabled.test(context);
    }

    @Override
    public void run() {
        FxActionContext context = context();
        if (context != null) {
            action.accept(context);
        }
    }
}
