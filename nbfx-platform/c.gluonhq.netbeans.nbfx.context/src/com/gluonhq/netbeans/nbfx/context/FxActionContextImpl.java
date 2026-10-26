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
package com.gluonhq.netbeans.nbfx.context;

import com.gluonhq.netbeans.nbfx.api.actions.FxActionContext;

import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 * Default {@link FxActionContext}: a thin wrapper over the NetBeans global context
 * ({@link Utilities#actionsGlobalContext()}).
 * <p>
 * It carries no domain knowledge and no selection logic of its own: the nbfx
 * {@code ContextGlobalProvider} (in {@code nbfx.ui}) feeds the nbfx selection into the global
 * context, exactly as the Swing {@code GlobalActionContextImpl} feeds it from the activated
 * TopComponent. Domain objects that are not carried directly are resolved on demand by per-domain
 * facades (for example {@code ProjectContext}).
 */
@ServiceProvider(service = FxActionContext.class)
public final class FxActionContextImpl extends FxActionContext {

    private final Lookup delegate;

    /** Creates the context as a wrapper over the NetBeans global context. */
    public FxActionContextImpl() {
        this.delegate = Utilities.actionsGlobalContext();
    }

    @Override
    public <T> T lookup(Class<T> clazz) {
        return delegate.lookup(clazz);
    }

    @Override
    public <T> Result<T> lookup(Template<T> template) {
        return delegate.lookup(template);
    }
}
