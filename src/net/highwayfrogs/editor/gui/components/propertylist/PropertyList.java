package net.highwayfrogs.editor.gui.components.propertylist;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Contains a list of properties to display in a UI.
 */
@Getter
public class PropertyList extends PropertyListNode {
    @NonNull private final ILogger logger;

    public PropertyList(IPropertyListCreator propertyListCreator, ILogger logger) {
        super(propertyListCreator);
        this.logger = logger != null ? logger : Utils.getInstanceLogger();
    }

    @Override
    public PropertyList getPropertyList() {
        return this;
    }

    @Override
    public void toString(StringBuilder builder) {
        toString(builder, " ", -1);
    }
}
