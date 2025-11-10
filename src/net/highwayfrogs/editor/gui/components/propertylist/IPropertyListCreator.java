package net.highwayfrogs.editor.gui.components.propertylist;

import net.highwayfrogs.editor.games.generic.data.IGameObject;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents an object which can populate a property list.
 * Created by Kneesnap on 11/8/2025.
 */
public interface IPropertyListCreator {
    /**
     * Creates and populates the property list.
     */
    default PropertyList createPropertyList() {
        return createPropertyList((this instanceof IGameObject) ? ((IGameObject) this).getLogger() : null);
    }

    /**
     * Creates and populates the property list.
     */
    default PropertyList createPropertyList(ILogger logger) {
        return new PropertyList(this, logger);
    }

    /**
     * Adds properties to the property list.
     * @param propertyList the property list to add properties to
     */
    void addToPropertyList(PropertyListNode propertyList);
}