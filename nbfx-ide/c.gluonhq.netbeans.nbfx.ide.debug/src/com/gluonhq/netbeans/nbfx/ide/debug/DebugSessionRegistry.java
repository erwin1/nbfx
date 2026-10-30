package com.gluonhq.netbeans.nbfx.ide.debug;

import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/** Holds the current debug session for the view and session commands. */
@ServiceProvider(service = DebugSessionRegistry.class)
public final class DebugSessionRegistry {

    private volatile DebugSession current;

    public static DebugSessionRegistry getDefault() {
        return Lookup.getDefault().lookup(DebugSessionRegistry.class);
    }

    public DebugSession current() {
        return current;
    }

    void setCurrent(DebugSession session) {
        current = session;
    }
}
