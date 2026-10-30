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
package org.netbeans.modules.nbfx.propertysheet;

import org.netbeans.modules.nbfx.api.actions.AbstractCommand;
import org.netbeans.modules.nbfx.api.actions.ActionIds;
import org.openide.util.NbBundle;

/** Window &gt; IDE Tools &gt; Properties: opens or fronts the Properties window. Always enabled. */
final class SelectPropertiesCommand extends AbstractCommand {

    SelectPropertiesCommand() {
        super(ActionIds.SELECT_PROPERTIES,
                NbBundle.getMessage(SelectPropertiesCommand.class, "CTL_SelectPropertiesCommand"),
                null, false);
    }

    @Override
    public void run() {
        PropertiesView view = PropertiesView.instance();
        if (view != null) {
            view.show();
        }
    }
}
