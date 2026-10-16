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
package com.gluonhq.netbeans.nbfx.docking;

import com.gluonhq.netbeans.nbfx.docking.DockArea.Anchor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies minimizing panes out of a {@link DockArea} and restoring them: a minimized pane leaves
 * the tree and comes back where it was with its share, a docked pane that empties while minimized is
 * disposed of, a pane anchored into another rejoins it, a minimized permanent pane does not come back
 * on its own when it receives content, and {@link DockArea#applyTree} restores everything first.
 * Runs on the headless JavaFX toolkit, as the area defers its clean-up to later pulses.
 */
public class DockAreaTest {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean();

    /** Panes are stack panes; a pane is empty when it has no children. */
    private static final class Host implements DockHost<StackPane> {

        final List<StackPane> disposed = new ArrayList<>();
        /** Panes the host treats as tool windows: the main pane never absorbs them. */
        final List<StackPane> views = new ArrayList<>();

        @Override
        public StackPane createDockedPane() {
            return new StackPane();
        }

        @Override
        public boolean belongsToMainArea(StackPane pane) {
            return !views.contains(pane);
        }

        @Override
        public void disposeDockedPane(StackPane pane) {
            disposed.add(pane);
        }

        @Override
        public ObservableValue<Boolean> isEmpty(StackPane pane) {
            return Bindings.isEmpty(pane.getChildren());
        }

        @Override
        public void moveContent(StackPane from, StackPane into) {
            into.getChildren().addAll(List.copyOf(from.getChildren()));
            from.getChildren().clear();
        }
    }

    /** An area of a navigator pane (holding content) and a main pane. */
    private static final class Fixture {

        final Host host = new Host();
        final StackPane navigator = new StackPane(new Region());
        final StackPane main = new StackPane();
        final DockArea<StackPane> area;

        Fixture() {
            SequencedMap<String, StackPane> primaries = new LinkedHashMap<>();
            primaries.put("NAVIGATOR", navigator);
            primaries.put("MAIN", main);
            area = new DockArea<>(host, primaries, List.of(0.3));
        }
    }

    @Test
    public void minimizedPaneLeavesTheTreeAndComesBackWithItsShare() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            f.area.minimize(f.navigator);
            assertEquals("P:MAIN", DockTrees.format(f.area.tree()));
            assertTrue(f.area.isMinimized(f.navigator));
            assertTrue(f.area.isPrimaryHidden(f.navigator));
            assertEquals(List.of(f.navigator), f.area.minimizedPanes());
            Anchor<StackPane> anchor = f.area.anchorOf(f.navigator);
            assertNotNull(anchor);
            assertSame(f.main, anchor.leaf());
            assertEquals(Side.LEFT, anchor.side());
            assertEquals(0.3, anchor.share(), 0.001);

            f.area.restore(f.navigator);
            assertFalse(f.area.isMinimized(f.navigator));
            assertTrue(f.area.minimizedPanes().isEmpty());
            assertEquals(List.of(f.navigator, f.main), f.area.leaves());
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void dockedPaneMinimizedBelowTheNavigatorComesBackBelowIt() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            StackPane docked = f.area.split(f.navigator, Side.BOTTOM);
            docked.getChildren().add(new Region());
            assertEquals("H[0.3000](V[0.6500](P:NAVIGATOR,L),P:MAIN)", DockTrees.format(f.area.tree()));

            f.area.minimize(docked);
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            Anchor<StackPane> anchor = f.area.anchorOf(docked);
            assertSame(f.navigator, anchor.leaf());
            assertEquals(Side.BOTTOM, anchor.side());
            assertEquals(0.35, anchor.share(), 0.001);

            f.area.restore(docked);
            assertEquals("H[0.3000](V[0.6500](P:NAVIGATOR,L),P:MAIN)", DockTrees.format(f.area.tree()));
            assertSame(docked, f.area.leaves().get(1));
            return null;
        });
    }

    @Test
    public void dockedPaneThatEmptiesWhileMinimizedIsDisposed() throws Exception {
        Fixture f = onFx(Fixture::new);
        StackPane docked = onFx(() -> {
            StackPane pane = f.area.split(f.navigator, Side.BOTTOM);
            pane.getChildren().add(new Region());
            f.area.minimize(pane);
            pane.getChildren().clear();
            return pane;
        });
        pulse();
        onFx(() -> {
            assertFalse(f.area.isMinimized(docked));
            assertEquals(List.of(docked), f.host.disposed);
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void paneAnchoredIntoAnotherRejoinsItOnRestore() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            // A single component minimized out of the navigator pane: a pane of its own, never docked.
            StackPane alone = f.host.createDockedPane();
            Node component = new Region();
            alone.getChildren().add(component);
            f.area.minimize(alone, Anchor.into(f.navigator));
            assertTrue(f.area.isMinimized(alone));
            assertTrue(f.area.anchorOf(alone).joins());
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));

            f.area.restore(alone);
            assertTrue(f.navigator.getChildren().contains(component));
            assertEquals(List.of(alone), f.host.disposed);
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void minimizedPrimaryStaysOutWhenItReceivesContent() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            f.area.minimize(f.navigator);
            f.navigator.getChildren().clear();
            return null;
        });
        pulse();
        onFx(() -> {
            assertTrue("an emptied minimized primary stays minimized", f.area.isMinimized(f.navigator));
            f.navigator.getChildren().add(new Region());
            assertEquals("content does not bring a minimized primary back", "P:MAIN", DockTrees.format(f.area.tree()));
            assertNull(f.area.paneBelow(f.navigator));

            f.area.restore(f.navigator);
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void emptyMinimizedPrimaryRestoredStaysHiddenUntilItFills() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            f.area.minimize(f.navigator);
            f.navigator.getChildren().clear();
            f.area.restore(f.navigator);
            assertFalse(f.area.isMinimized(f.navigator));
            assertEquals("P:MAIN", DockTrees.format(f.area.tree()));
            f.navigator.getChildren().add(new Region());
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void applyTreeRestoresMinimizedPanesFirst() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            f.area.minimize(f.navigator);
            f.area.applyTree(f.area.defaultTree());
            assertTrue(f.area.minimizedPanes().isEmpty());
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void twoPanesThatLeftInTurnComeBackAsTheyWereWhicheverReturnsFirst() throws Exception {
        for (boolean navigatorFirst : new boolean[] {true, false}) {
            for (boolean restoreNavigatorFirst : new boolean[] {true, false}) {
                Fixture f = onFx(Fixture::new);
                String order = "minimize " + (navigatorFirst ? "navigator" : "docked") + " first, restore "
                        + (restoreNavigatorFirst ? "navigator" : "docked") + " first";
                onFx(() -> {
                    StackPane docked = f.area.split(f.navigator, Side.BOTTOM);
                    docked.getChildren().add(new Region());
                    assertEquals("H[0.3000](V[0.6500](P:NAVIGATOR,L),P:MAIN)", DockTrees.format(f.area.tree()));
                    f.area.minimize(navigatorFirst ? f.navigator : docked);
                    f.area.minimize(navigatorFirst ? docked : f.navigator);
                    assertEquals(order, "P:MAIN", DockTrees.format(f.area.tree()));
                    f.area.restore(restoreNavigatorFirst ? f.navigator : docked);
                    f.area.restore(restoreNavigatorFirst ? docked : f.navigator);
                    assertEquals(order, "H[0.3000](V[0.6500](P:NAVIGATOR,L),P:MAIN)", DockTrees.format(f.area.tree()));
                    assertEquals(order, List.of(f.navigator, docked, f.main), f.area.leaves());
                    return null;
                });
            }
        }
    }

    @Test
    public void paneAnchoredIntoAMinimizedPaneRestoresThatPaneAndJoinsIt() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            StackPane alone = f.host.createDockedPane();
            Node component = new Region();
            alone.getChildren().add(component);
            f.area.minimize(alone, Anchor.into(f.navigator));
            f.area.minimize(f.navigator);
            assertEquals("P:MAIN", DockTrees.format(f.area.tree()));

            f.area.restore(alone);
            assertFalse(f.area.isMinimized(f.navigator));
            assertTrue(f.navigator.getChildren().contains(component));
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void maximizedPaneShowsAloneAndTheShapeComesBack() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            StackPane docked = f.area.split(f.navigator, Side.BOTTOM);
            docked.getChildren().add(new Region());
            f.area.maximize(f.main);
            assertTrue(f.area.isMaximized());
            assertSame(f.main, f.area.maximizedPane());
            assertEquals("P:MAIN", DockTrees.format(f.area.tree()));
            assertTrue("set aside, not hidden", f.area.isSetAside(f.navigator));
            assertFalse(f.area.isPrimaryHidden(f.navigator));
            assertFalse(f.area.contains(docked));

            f.area.unmaximize();
            assertFalse(f.area.isMaximized());
            assertEquals("H[0.3000](V[0.6500](P:NAVIGATOR,L),P:MAIN)", DockTrees.format(f.area.tree()));
            assertEquals(List.of(f.navigator, docked, f.main), f.area.leaves());
            return null;
        });
    }

    @Test
    public void aChangeToTheShapeWhileMaximizedRestoresItFirst() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            List<StackPane> ended = new ArrayList<>();
            f.area.maximizedPaneProperty().subscribe((was, now) -> {
                if (was != null && now == null) {
                    ended.add(was);
                }
            });
            f.area.maximize(f.main);
            StackPane editors = f.area.split(f.main, Side.RIGHT);
            editors.getChildren().add(new Region());
            assertFalse("splitting a pane ends the maximized state", f.area.isMaximized());
            assertEquals(List.of(f.main), ended);
            // Split in the full layout: next to the editor pane in the root split, not alone.
            assertEquals("H[0.3000,0.7550](P:NAVIGATOR,P:MAIN,L)", DockTrees.format(f.area.tree()));
            assertEquals(List.of(f.navigator, f.main, editors), f.area.leaves());

            f.area.maximize(editors);
            f.area.splitArea(Side.BOTTOM).getChildren().add(new Region());
            assertFalse("a pane along the area ends it too", f.area.isMaximized());
            assertEquals(4, f.area.leaves().size());
            return null;
        });
    }

    @Test
    public void maximizingAnotherPaneOrApplyingAShapeRestoresFirst() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            f.area.maximize(f.main);
            f.area.maximize(f.navigator);
            assertSame(f.navigator, f.area.maximizedPane());
            assertEquals("P:NAVIGATOR", DockTrees.format(f.area.tree()));

            f.area.applyTree(f.area.defaultTree());
            assertFalse(f.area.isMaximized());
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void maximizingTheOnlyPaneSetsNothingAside() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            f.area.minimize(f.navigator);
            f.area.maximize(f.main);
            assertTrue(f.area.isMaximized());
            f.area.unmaximize();
            assertEquals("P:MAIN", DockTrees.format(f.area.tree()));
            f.area.restore(f.navigator);
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void paneBelowARowOfPanesComesBackBelowTheRow() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            // An output pane below a row of two editor panes, beside the navigator.
            List<StackPane> leaves = f.area.applyTree(DockTrees.parse("H[0.3000](P:NAVIGATOR,V[0.6500](H[0.5000](P:MAIN,L),L))"));
            StackPane editors = leaves.get(2);
            StackPane output = leaves.get(3);
            editors.getChildren().add(new Region());
            output.getChildren().add(new Region());

            f.area.minimize(output);
            Anchor<StackPane> anchor = f.area.anchorOf(output);
            assertSame("nearest leaf of the row", editors, anchor.leaf());
            assertSame("farthest leaf of the row", f.main, anchor.farLeaf());
            assertEquals("H[0.3000](P:NAVIGATOR,H[0.5000](P:MAIN,L))", DockTrees.format(f.area.tree()));

            f.area.restore(output);
            assertEquals("below the whole row, not below one of its panes",
                    "H[0.3000](P:NAVIGATOR,V[0.6500](H[0.5000](P:MAIN,L),L))", DockTrees.format(f.area.tree()));
            assertEquals(List.of(f.navigator, f.main, editors, output), f.area.leaves());

            // With one of the row's panes gone, it comes back below the other.
            f.area.minimize(output);
            editors.getChildren().clear();
            return null;
        });
        pulse();
        onFx(() -> {
            StackPane output = f.area.minimizedPanes().getFirst();
            f.area.restore(output);
            assertEquals("H[0.3000](P:NAVIGATOR,V[0.6500](P:MAIN,L))", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void panesMinimizedInTurnComeBackInReverseOrderAsTheyWere() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            // Projects above the Navigator on the left; two editor panes in a row above Output on the right.
            String shape = "H[0.3000](V[0.6500](P:NAVIGATOR,L),V[0.6500](H[0.5000](P:MAIN,L),L))";
            List<StackPane> leaves = f.area.applyTree(DockTrees.parse(shape));
            StackPane navigator = leaves.get(1);
            StackPane editors = leaves.get(3);
            StackPane output = leaves.get(4);
            for (StackPane pane : List.of(navigator, editors, output)) {
                pane.getChildren().add(new Region());
            }
            // Minimized the way a maximize minimizes the view panes around an editor: in shape order.
            f.area.minimize(f.navigator);
            f.area.minimize(navigator);
            f.area.minimize(output);
            assertEquals("H[0.5000](P:MAIN,L)", DockTrees.format(f.area.tree()));
            f.area.maximize(f.main);
            f.area.unmaximize();

            // Restored last minimized first: each anchor finds the shape it left.
            f.area.restore(output);
            f.area.restore(navigator);
            f.area.restore(f.navigator);
            assertEquals(shape, DockTrees.format(f.area.tree()));
            assertEquals(List.of(f.navigator, navigator, f.main, editors, output), f.area.leaves());
            return null;
        });
    }

    @Test
    public void sideOfAPaneComesFromTheShape() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            List<StackPane> leaves = f.area.applyTree(DockTrees.parse(
                    "V[0.2000](L,H[0.3000,0.7000](V[0.6500](P:NAVIGATOR,L),V[0.6500](P:MAIN,L),L))"));
            for (StackPane leaf : leaves) {
                if (!f.area.isPrimary(leaf)) {
                    leaf.getChildren().add(new Region());
                }
            }
            assertEquals(Side.TOP, f.area.sideOf(leaves.get(0), f.main));
            assertEquals(Side.LEFT, f.area.sideOf(f.navigator, f.main));
            assertEquals("the pane below the navigator is left of the editors too", Side.LEFT, f.area.sideOf(leaves.get(2), f.main));
            assertEquals(Side.BOTTOM, f.area.sideOf(leaves.get(4), f.main));
            assertEquals(Side.RIGHT, f.area.sideOf(leaves.get(5), f.main));
            assertNull(f.area.sideOf(f.main, f.main));
            f.area.minimize(f.navigator);
            assertNull("a minimized pane has no side in the shape", f.area.sideOf(f.navigator, f.main));
            return null;
        });
    }

    @Test
    public void emptiedMainPaneAbsorbsOnlyWhatTheHostAllows() throws Exception {
        Fixture f = onFx(Fixture::new);
        StackPane docked = onFx(() -> {
            StackPane pane = f.area.split(f.navigator, Side.BOTTOM);
            pane.getChildren().add(new Region());
            f.main.getChildren().add(new Region());
            f.host.views.add(pane);
            f.main.getChildren().clear();
            return pane;
        });
        pulse();
        onFx(() -> {
            assertEquals("a tool-window pane is left alone by the emptied editor pane",
                    "H[0.3000](V[0.6500](P:NAVIGATOR,L),P:MAIN)", DockTrees.format(f.area.tree()));
            assertTrue(f.main.getChildren().isEmpty());
            f.host.views.clear();
            f.main.getChildren().add(new Region());
            f.main.getChildren().clear();
            return null;
        });
        pulse();
        onFx(() -> {
            // The editor pane moved into the editor group's slot, and the split it left collapsed.
            assertEquals("an editor group next to it is taken over",
                    "V[0.6500](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            assertFalse(f.main.getChildren().isEmpty());
            assertEquals(List.of(docked), f.host.disposed);
            return null;
        });
    }

    @Test
    public void belowTheMainPaneMeansBelowItsRowOfEditorGroups() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            // A second editor group beside the editor pane, Output below the row.
            List<StackPane> leaves = f.area.applyTree(DockTrees.parse("H[0.3000](P:NAVIGATOR,V[0.6500](H[0.5000](P:MAIN,L),L))"));
            StackPane editors = leaves.get(2);
            StackPane output = leaves.get(3);
            editors.getChildren().add(new Region());
            output.getChildren().add(new Region());
            f.host.views.add(output);

            assertSame("the pane below the row is found", output, f.area.paneBelow(f.main));

            // Without it, a new pane goes below the row, not below the editor pane alone.
            f.area.minimize(output);
            StackPane created = f.area.paneBelow(f.main);
            created.getChildren().add(new Region());
            assertEquals("H[0.3000](P:NAVIGATOR,V[0.6500](H[0.5000](P:MAIN,L),L))", DockTrees.format(f.area.tree()));

            // A tool-window pane beside the editor pane is not part of its area.
            f.host.views.add(editors);
            created.getChildren().clear();
            return null;
        });
        pulse();
        onFx(() -> {
            StackPane below = f.area.paneBelow(f.main);
            below.getChildren().add(new Region());
            assertEquals("H[0.3000](H[0.5000](V[0.6500](P:MAIN,L),L))".replace("H[0.3000](H", "H[0.3000](P:NAVIGATOR,H"),
                    DockTrees.format(f.area.tree()));
            return null;
        });
    }

    @Test
    public void mainPaneCannotBeMinimized() throws Exception {
        Fixture f = onFx(Fixture::new);
        onFx(() -> {
            try {
                f.area.minimize(f.main);
                fail("the main pane was minimized");
            } catch (IllegalArgumentException expected) {
                // as documented
            }
            assertEquals("H[0.3000](P:NAVIGATOR,P:MAIN)", DockTrees.format(f.area.tree()));
            return null;
        });
    }

    /** Lets the pulses the area deferred work to run. */
    private static void pulse() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            onFx(() -> null);
        }
    }

    private static <T> T onFx(Supplier<T> task) throws InterruptedException {
        ensureFxStarted();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(task.get());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(30, TimeUnit.SECONDS)) {
            fail("timed out waiting for the JavaFX thread");
        }
        Throwable failure = error.get();
        if (failure instanceof AssertionError assertion) {
            throw assertion;
        }
        if (failure != null) {
            throw new AssertionError(failure);
        }
        return result.get();
    }

    private static void ensureFxStarted() throws InterruptedException {
        if (FX_STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        }
    }
}
