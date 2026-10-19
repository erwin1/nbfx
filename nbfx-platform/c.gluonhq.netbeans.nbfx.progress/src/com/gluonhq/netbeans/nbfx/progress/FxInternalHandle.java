package com.gluonhq.netbeans.nbfx.progress;

import org.netbeans.modules.progress.spi.InternalHandle;
import org.openide.util.Cancellable;

/**
 * The headless {@link InternalHandle} the FX progress environment creates for every
 * {@code ProgressHandle}. It carries no UI; the FX worker renders it.
 */
final class FxInternalHandle extends InternalHandle {

    FxInternalHandle(String displayName, Cancellable cancel, boolean userInitiated) {
        super(displayName, cancel, userInitiated);
    }
}
