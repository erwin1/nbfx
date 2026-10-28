package com.gluonhq.netbeans.nbfx.propertysheet;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.openide.util.lookup.ServiceProvider;

/** The editor for {@link String} values. */
@ServiceProvider(service = PropertyEditor.class)
public final class TextPropertyEditor implements PropertyEditor {

    @Override
    public Class<?> valueType() {
        return String.class;
    }

    @Override
    public Node createEditor(Property property) {
        TextField field = new TextField(PropertyEditors.display(property.getValue()));
        field.setEditable(property.isWritable());
        field.textProperty().addListener((observable, old, now) -> {
            if (property.isWritable()) {
                property.setValue(now);
            }
        });
        return field;
    }
}
