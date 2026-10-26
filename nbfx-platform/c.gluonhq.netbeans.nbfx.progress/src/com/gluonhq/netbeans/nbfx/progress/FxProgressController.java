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

import org.netbeans.modules.progress.spi.Controller;
import org.netbeans.modules.progress.spi.ProgressUIWorkerWithModel;

/**
 * The nbfx progress controller: it reuses the headless task/event bookkeeping of
 * {@link Controller} and renders through the {@link FxProgressWorker}.
 */
final class FxProgressController extends Controller {

    private static final FxProgressController INSTANCE = new FxProgressController();

    private FxProgressController() {
        super(null);
    }

    static FxProgressController getInstance() {
        return INSTANCE;
    }

    @Override
    protected ProgressUIWorkerWithModel createWorker() {
        return FxProgressWorker.getInstance();
    }
}
