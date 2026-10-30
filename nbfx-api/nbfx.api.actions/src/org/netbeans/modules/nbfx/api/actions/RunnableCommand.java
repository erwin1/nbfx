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
package org.netbeans.modules.nbfx.api.actions;

import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCombination;

/**
 * A {@link Command} whose behaviour is a fixed {@link Runnable}, either always enabled or with its
 * enablement driven by an observable given at construction. Covers every command that does not need
 * to resolve a target when it runs - project and window actions, file actions - leaving a dedicated
 * class only to those that do.
 * <p>
 * The action never runs while the command is disabled, so an accelerator that stays installed while
 * its command is unavailable cannot trigger it.
 */
public class RunnableCommand extends AbstractCommand {

    private final Runnable action;

    /**
     * Creates a command that is always enabled.
     *
     * @param id          the stable command id, never {@code null}
     * @param text        the display text, never {@code null}
     * @param accelerator the preferred keyboard accelerator, or {@code null} if none
     * @param action      what the command does, never {@code null}
     */
    public RunnableCommand(String id, String text, KeyCombination accelerator, Runnable action) {
        super(id, text, accelerator, false);
        this.action = Objects.requireNonNull(action);
    }

    /**
     * Creates a command whose {@code disabled} state follows {@code disabled}.
     *
     * @param disabled the observable driving the disabled state
     * @see #enabledWhen(String, String, KeyCombination, ObservableBooleanValue, Runnable)
     */
    public RunnableCommand(String id, String text, KeyCombination accelerator, Runnable action,
            ObservableValue<Boolean> disabled) {
        this(id, text, accelerator, action);
        bindDisabled(disabled);
    }

    /** As the constructor, for callers whose observable expresses when the command is <em>enabled</em>. */
    public static RunnableCommand enabledWhen(String id, String text, KeyCombination accelerator,
            ObservableBooleanValue enabled, Runnable action) {
        return new RunnableCommand(id, text, accelerator, action, Bindings.not(enabled));
    }

    @Override
    public final void run() {
        if (!isDisabled()) {
            action.run();
        }
    }
}
