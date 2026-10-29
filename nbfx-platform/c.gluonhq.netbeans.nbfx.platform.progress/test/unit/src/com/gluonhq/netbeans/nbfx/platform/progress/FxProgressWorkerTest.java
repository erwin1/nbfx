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
package com.gluonhq.netbeans.nbfx.platform.progress;

import com.gluonhq.netbeans.nbfx.platform.progress.FxProgressWorker.Task;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.modules.progress.spi.InternalHandle;
import org.netbeans.modules.progress.spi.ProgressEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the event-to-observable mapping of {@link FxProgressWorker}. The JavaFX toolkit runs on the
 * headless glass platform (see {@code test-unit-sys-prop.glass.platform}); the bodies run on the
 * JavaFX Application Thread so the worker applies the events inline and the state can be asserted
 * synchronously.
 */
public class FxProgressWorkerTest {

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean();

    @BeforeClass
    public static void startFx() {
        if (FX_STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            await(latch);
        }
    }

    @Test
    public void tracksTaskLifecycle() {
        onFx(() -> {
            FxProgressWorker worker = new FxProgressWorker();
            InternalHandle handle = new InternalHandle("Scanning project", null, true) {
            };

            worker.processProgressEvent(new ProgressEvent(handle, ProgressEvent.TYPE_START, false));
            assertEquals(1, worker.taskCountProperty().get());
            Task task = worker.activeTaskProperty().get();
            assertNotNull(task);
            assertFalse(task.isSystemTask());
            assertEquals("Scanning project", task.nameProperty().get());

            worker.processProgressEvent(new ProgressEvent(handle, "half way", 100, 0.5, -1, false));
            assertEquals(0.5, worker.progressProperty().get(), 1e-9);
            assertEquals("Scanning project", task.nameProperty().get());
            assertEquals("half way", task.detailProperty().get());

            worker.processProgressEvent(new ProgressEvent(handle, ProgressEvent.TYPE_FINISH, false));
            assertEquals(0, worker.taskCountProperty().get());
            assertNull(worker.activeTaskProperty().get());
            assertEquals(-1, worker.progressProperty().get(), 1e-9);
            return null;
        });
    }

    @Test
    public void coalescesABurstIntoOneTask() {
        onFx(() -> {
            FxProgressWorker worker = new FxProgressWorker();
            InternalHandle handle = new InternalHandle("Build", null, true) {
            };

            worker.processProgressEvent(new ProgressEvent(handle, ProgressEvent.TYPE_START, false));
            for (int unit = 1; unit <= 5; unit++) {
                worker.processProgressEvent(new ProgressEvent(handle, "step " + unit, 10, unit / 10.0, -1, false));
            }

            assertEquals(1, worker.taskCountProperty().get());
            assertEquals(0.5, worker.progressProperty().get(), 1e-9);
            assertEquals("step 5", worker.activeTaskProperty().get().detailProperty().get());
            return null;
        });
    }

    @Test
    public void marksSystemTasksAndFallsBackToTheLastUserTask() {
        onFx(() -> {
            FxProgressWorker worker = new FxProgressWorker();
            InternalHandle user = new InternalHandle("Build", null, true) {
            };
            InternalHandle system = new InternalHandle("Background scanning", null, false) {
            };

            worker.processProgressEvent(new ProgressEvent(user, ProgressEvent.TYPE_START, false));
            worker.processProgressEvent(new ProgressEvent(system, ProgressEvent.TYPE_START, false));
            assertEquals(2, worker.taskCountProperty().get());

            Task systemTask = worker.getTasks().stream().filter(Task::isSystemTask).findFirst().orElse(null);
            assertNotNull(systemTask);
            assertEquals("Background scanning", systemTask.nameProperty().get());
            assertTrue(user.isUserInitialized());

            // Finishing the user task leaves only the system task as the fallback.
            worker.processProgressEvent(new ProgressEvent(user, ProgressEvent.TYPE_FINISH, false));
            assertEquals(1, worker.taskCountProperty().get());
            assertEquals(systemTask, worker.activeTaskProperty().get());
            return null;
        });
    }

    @Test
    public void progressWithoutStartCreatesTheTask() {
        onFx(() -> {
            FxProgressWorker worker = new FxProgressWorker();
            InternalHandle handle = new InternalHandle("Indexing", null, true) {
            };

            // NetBeans sends Progress/Switch/Silent events for handles that never sent a Start
            // (progress.ui StatusLineComponent materialises the task); the update must not be lost.
            worker.processProgressEvent(new ProgressEvent(handle, "n files", 50, 0.25, -1, false));
            assertEquals(1, worker.taskCountProperty().get());
            Task task = worker.activeTaskProperty().get();
            assertNotNull(task);
            assertEquals("n files", task.detailProperty().get());
            assertEquals(0.25, worker.progressProperty().get(), 1e-9);
            return null;
        });
    }

    private static <T> T onFx(Callable<T> action) {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        await(latch);
        Throwable failure = error.get();
        if (failure != null) {
            throw new AssertionError(failure);
        }
        return result.get();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new AssertionError("timed out waiting for the JavaFX thread");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }
}
