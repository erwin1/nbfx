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
package org.netbeans.modules.nbfx.ide.editor.css.usages;

import org.netbeans.modules.nbfx.api.usages.SearchTarget;
import org.netbeans.modules.nbfx.api.usages.UsageSources;
import org.netbeans.modules.nbfx.api.usages.UsagesProvider;
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
