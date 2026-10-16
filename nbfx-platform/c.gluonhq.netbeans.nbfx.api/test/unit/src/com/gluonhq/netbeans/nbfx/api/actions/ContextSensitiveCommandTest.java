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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.test.MockLookup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that a {@link ContextSensitiveCommand} is re-evaluated from the global action context: it
 * starts disabled, enables when an observed context type appears, and disables again when it is
 * removed.
 */
public class ContextSensitiveCommandTest {

    private final InstanceContent content = new InstanceContent();
    private final FxActionContext context = new DelegatingContext(new AbstractLookup(content));

    @Before
    public void setUp() {
        MockLookup.setInstances(context);
    }

    @After
    public void tearDown() {
        MockLookup.setInstances();
    }

    @Test
    public void followsObservedContextTypes() {
        TestCommand command = new TestCommand();
        assertTrue("no selection yet", command.isDisabled());

        content.add("selected");
        assertFalse("selection present", command.isDisabled());

        content.remove("selected");
        assertTrue("selection cleared", command.isDisabled());
    }

    @Test
    public void evaluatesTheContextDirectly() {
        content.add(42);
        TestCommand command = new TestCommand();
        command.refreshContext();
        assertFalse(command.isDisabled());
    }

    private static final class TestCommand extends ContextSensitiveCommand {
        TestCommand() {
            super("test", "Test", String.class, Integer.class);
        }

        @Override
        protected boolean isEnabled(FxActionContext context) {
            return context.lookup(String.class) != null || context.lookup(Integer.class) != null;
        }

        @Override
        public void run() {
            // no-op
        }
    }

    private static final class DelegatingContext extends FxActionContext {
        private final Lookup delegate;

        DelegatingContext(Lookup delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> T lookup(Class<T> clazz) {
            return delegate.lookup(clazz);
        }

        @Override
        public <T> Result<T> lookup(Template<T> template) {
            return delegate.lookup(template);
        }
    }
}
