package com.gluonhq.netbeans.nbfx.ide.editor.css.usages;

import com.gluonhq.netbeans.nbfx.api.usages.SearchTarget;
import com.gluonhq.netbeans.nbfx.api.usages.UsageSources;
import com.gluonhq.netbeans.nbfx.api.usages.UsagesProvider;
import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = UsagesProvider.class)
public final class CssUsagesProvider implements UsagesProvider {

    @Override
    public boolean handles(FileObject file) {
        return CssTarget.isStyleSheet(file);
    }

    @Override
    public SearchTarget targetAt(FileObject file, int offset, UsageSources sources) throws IOException {
        return CssTarget.at(file, offset, sources);
    }

    @Override
    public SearchTarget topLevelTarget(FileObject file, UsageSources sources) {
        return null;
    }
}
