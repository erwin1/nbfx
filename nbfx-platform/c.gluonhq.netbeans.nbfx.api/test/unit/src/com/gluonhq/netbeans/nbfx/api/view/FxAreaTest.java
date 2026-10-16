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
public class FxAreaTest {

    @Test
    public void defaultsMirrorTheSwingModes() {
        assertEquals("editor", FxArea.EDITOR.id());
        assertEquals(DockLocation.CENTER, FxArea.EDITOR.location());

        assertEquals("explorer", FxArea.EXPLORER.id());
        assertEquals(DockLocation.LEFT, FxArea.EXPLORER.location());

        assertEquals("navigator", FxArea.NAVIGATOR.id());
        assertEquals(DockLocation.LEFT_BOTTOM, FxArea.NAVIGATOR.location());

        assertEquals("properties", FxArea.PROPERTIES.id());
        assertEquals(DockLocation.RIGHT, FxArea.PROPERTIES.location());

        assertEquals("output", FxArea.OUTPUT.id());
        assertEquals(DockLocation.CENTER_BOTTOM, FxArea.OUTPUT.location());
    }

    @Test
    public void looksUpByIdCaseInsensitively() {
        assertEquals(FxArea.EDITOR, FxArea.lookUp("Editor").orElseThrow());
        assertEquals(FxArea.OUTPUT, FxArea.lookUp("output").orElseThrow());
        assertEquals(Optional.empty(), FxArea.lookUp("console"));
    }

    @Test
    public void resolvesTheDefaultForALocation() {
        assertEquals(Optional.of(FxArea.EXPLORER), FxArea.defaultFor(DockLocation.LEFT));
        assertEquals(Optional.of(FxArea.NAVIGATOR), FxArea.defaultFor(DockLocation.LEFT_BOTTOM));
        assertEquals(Optional.of(FxArea.OUTPUT), FxArea.defaultFor(DockLocation.CENTER_BOTTOM));
        assertEquals(Optional.of(FxArea.PROPERTIES), FxArea.defaultFor(DockLocation.RIGHT));
    }

    @Test
    public void keepsWeightsInBounds() {
        assertTrue(FxArea.EXPLORER.weight() > 0 && FxArea.EXPLORER.weight() < 1);
        assertTrue(FxArea.EDITOR.weight() == 1.0d);
        try {
            new FxArea("bad", "Bad", DockLocation.CENTER, 1.5);
            fail("expected an out-of-range weight to be rejected");
        } catch (IllegalArgumentException expected) {
            // good
        }
    }
}