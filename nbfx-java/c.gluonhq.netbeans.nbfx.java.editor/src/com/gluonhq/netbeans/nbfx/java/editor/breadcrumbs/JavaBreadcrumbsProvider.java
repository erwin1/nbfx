package com.gluonhq.netbeans.nbfx.java.editor.breadcrumbs;
import com.gluonhq.netbeans.nbfx.platform.editor.breadcrumbs.CachingBreadcrumbsProvider;

import com.gluonhq.netbeans.nbfx.api.progress.Cancellation;
import com.gluonhq.netbeans.nbfx.api.breadcrumbs.BreadcrumbElement;
import com.gluonhq.netbeans.nbfx.api.breadcrumbs.BreadcrumbsContext;
import com.gluonhq.netbeans.nbfx.api.breadcrumbs.BreadcrumbsProvider;
import org.openide.util.lookup.ServiceProvider;

import java.util.List;

/**
 * Breadcrumbs provider for Java source files.
 *
 * <p>Delegates the parse and tree construction to {@link JavaBreadcrumbsScanner};
 * caching and caret descent come from {@link CachingBreadcrumbsProvider}.</p>
 */
@ServiceProvider(service = BreadcrumbsProvider.class)
public final class JavaBreadcrumbsProvider extends CachingBreadcrumbsProvider {

    public JavaBreadcrumbsProvider() {
        super("java");
    }

    @Override
    public List<BreadcrumbElement> scan(BreadcrumbsContext context, Cancellation cancellation) {
        return JavaBreadcrumbsScanner.scan(context, cancellation);
    }
}
