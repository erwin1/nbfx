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
