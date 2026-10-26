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
package com.gluonhq.netbeans.nbfx.propertysheet;

import javafx.beans.value.ObservableValue;

/**
 * One row in a {@link FxPropertySheet}, mirroring the shape of an {@code org.openide.nodes.Node.Property}
 * but for JavaFX. Implementations are usually simple value holders; the sheet edits them through a
 * registered {@link FxPropertyEditor}.
 */
public interface FxProperty {

    /** A stable name, unique within its property set. */
    String getName();

    /** The display name shown in the sheet. */
    String getDisplayName();

    /** A short help text, or {@code null}. */
    String getShortDescription();

    /** The value's type, used to pick an editor. */
    Class<?> getValueType();

    /** The current value, or {@code null}. */
    Object getValue();

    /** Sets the value; ignored when {@link #isWritable()} is {@code false}. */
    void setValue(Object value);

    /** Whether the value can be edited. */
    boolean isWritable();

    /** Whether the property is only shown with the expert toggle on. */
    boolean isExpert();

    /** Whether the value currently equals the property's default. */
    boolean isDefaultValue();

    /** The value as an observable, so the sheet can react to changes. */
    ObservableValue<Object> valueObservable();
}
