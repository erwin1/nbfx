/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gluonhq.netbeans.nbfx.progress;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.progress.spi.Controller;
import org.netbeans.modules.progress.spi.ProgressEnvironment;
import org.openide.util.Cancellable;
import org.openide.util.lookup.ServiceProvider;

/**
 * The nbfx {@link ProgressEnvironment}: every {@code ProgressHandle} created by NetBeans/ nbfx code
 * is backed by an {@link FxInternalHandle} and rendered by the FX progress worker. Registered as the
 * single environment (the Swing {@code progress.ui} module is excluded from the nbfx clusters).
 */
@ServiceProvider(service = ProgressEnvironment.class)
public final class FxProgressEnvironment implements ProgressEnvironment {

    @Override
    public ProgressHandle createHandle(String displayName, Cancellable cancel, boolean userInitiated) {
        return new FxInternalHandle(displayName, cancel, userInitiated).createProgressHandle();
    }

    @Override
    public Controller getController() {
        return FxProgressController.getInstance();
    }
}
