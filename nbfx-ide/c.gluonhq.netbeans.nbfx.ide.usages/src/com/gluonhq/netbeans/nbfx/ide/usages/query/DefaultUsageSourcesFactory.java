package com.gluonhq.netbeans.nbfx.ide.usages.query;

import com.gluonhq.netbeans.nbfx.api.usages.QueryOptions;
import com.gluonhq.netbeans.nbfx.api.usages.UsageSources;
import com.gluonhq.netbeans.nbfx.api.usages.UsageSourcesFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/** The language-neutral source sets of the Usages engine. */
@ServiceProvider(service = UsageSourcesFactory.class)
public final class DefaultUsageSourcesFactory implements UsageSourcesFactory {

    @Override
    public UsageSources forScope(QueryOptions.Scope scope, FileObject file) {
        return DefaultUsageSources.forScope(scope, file);
    }
}
