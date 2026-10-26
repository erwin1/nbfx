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
package com.gluonhq.netbeans.nbfx.structure;

import com.gluonhq.netbeans.nbfx.api.actions.Command;
import com.gluonhq.netbeans.nbfx.api.actions.CommandsProvider;
import java.util.Collection;
import java.util.List;
import org.openide.util.lookup.ServiceProvider;

/** Contributes the Window &gt; Navigator command. */
@ServiceProvider(service = CommandsProvider.class)
public final class NavigatorCommandsProvider implements CommandsProvider {

    @Override
    public Collection<Command> createCommands() {
        return List.of(new SelectNavigatorCommand());
    }
}