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
package org.netbeans.modules.nbfx.java.editor.breadcrumbs;
import org.netbeans.modules.nbfx.platform.editor.breadcrumbs.CachingBreadcrumbsProvider;

import org.netbeans.modules.nbfx.api.progress.Cancellation;
import org.netbeans.modules.nbfx.api.breadcrumbs.BreadcrumbElement;
import org.netbeans.modules.nbfx.api.breadcrumbs.BreadcrumbsContext;
import org.netbeans.modules.nbfx.api.breadcrumbs.BreadcrumbsProvider;
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
