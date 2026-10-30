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
package com.gluonhq.netbeans.nbfx.api.view;

import java.util.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies the shell area descriptors: their constants mirror the Swing window modes, and the
 * registry resolves them by id and location.
 */
public class ViewAreaTest {

    @Test
    public void defaultsMirrorTheSwingModes() {
        assertEquals("editor", ViewArea.EDITOR.id());
        assertEquals(DockLocation.CENTER, ViewArea.EDITOR.location());

        assertEquals("explorer", ViewArea.EXPLORER.id());
        assertEquals(DockLocation.LEFT, ViewArea.EXPLORER.location());

        assertEquals("navigator", ViewArea.NAVIGATOR.id());
        assertEquals(DockLocation.LEFT_BOTTOM, ViewArea.NAVIGATOR.location());

        assertEquals("properties", ViewArea.PROPERTIES.id());
        assertEquals(DockLocation.RIGHT, ViewArea.PROPERTIES.location());

        assertEquals("output", ViewArea.OUTPUT.id());
        assertEquals(DockLocation.CENTER_BOTTOM, ViewArea.OUTPUT.location());
    }

    @Test
    public void looksUpByIdCaseInsensitively() {
        assertEquals(ViewArea.EDITOR, ViewArea.lookUp("Editor").orElseThrow());
        assertEquals(ViewArea.OUTPUT, ViewArea.lookUp("output").orElseThrow());
        assertEquals(Optional.empty(), ViewArea.lookUp("console"));
    }

    @Test
    public void resolvesTheDefaultForALocation() {
        assertEquals(Optional.of(ViewArea.EXPLORER), ViewArea.defaultFor(DockLocation.LEFT));
        assertEquals(Optional.of(ViewArea.NAVIGATOR), ViewArea.defaultFor(DockLocation.LEFT_BOTTOM));
        assertEquals(Optional.of(ViewArea.OUTPUT), ViewArea.defaultFor(DockLocation.CENTER_BOTTOM));
        assertEquals(Optional.of(ViewArea.PROPERTIES), ViewArea.defaultFor(DockLocation.RIGHT));
    }

    @Test
    public void keepsWeightsInBounds() {
        assertTrue(ViewArea.EXPLORER.weight() > 0 && ViewArea.EXPLORER.weight() < 1);
        assertTrue(ViewArea.EDITOR.weight() == 1.0d);
        try {
            new ViewArea("bad", "Bad", DockLocation.CENTER, 1.5);
            fail("expected an out-of-range weight to be rejected");
        } catch (IllegalArgumentException expected) {
            // good
        }
    }
}