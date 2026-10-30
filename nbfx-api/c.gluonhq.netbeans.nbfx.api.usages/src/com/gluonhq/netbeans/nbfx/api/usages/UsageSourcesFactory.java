package com.gluonhq.netbeans.nbfx.api.usages;

import org.openide.filesystems.FileObject;

/** Creates the source set for a query scope. */
public interface UsageSourcesFactory {

    UsageSources forScope(QueryOptions.Scope scope, FileObject file);
}
