package net.highwayfrogs.editor.gui.components.propertylist;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Represents the user interface for a specific UI.
 * Created by Kneesnap on 11/9/2025.
 */
public interface IPropertyListEntryUI {
    /**
     * Gets the entry which the UI is displayed for.
     */
    PropertyListEntry getEntry();

    /**
     * Refreshes the data entry.
     * @param node the node to refresh the data entry for
     */
    void updateEntry(PropertyListNode node);

    /**
     * Updates the property list entry by updating the parent entry.
     */
    default void updateSelfAndParent() {
        PropertyListEntry entry = getEntry();
        if (entry == null)
            return;

        if (entry.getParentNode() != null) {
            updateEntry(entry.getParentNode());
        } else {
            updateEntry(entry);
        }
    }

    /**
     * Updates the entire property list.
     */
    default void updateFullPropertyList() {
        PropertyListEntry entry = getEntry();
        if (entry != null && entry.getPropertyList() != null)
            updateEntry(entry.getPropertyList());
    }

    /**
     * Initiates an edit operation on the property value.
     * @param startValue the start value to use
     * @param validator the validator which ensures the value is okay
     * @param newValueHandler the handler which accepts a value. This will only run if a new (valid) value is accepted by the user.
     */
    void edit(String startValue, Predicate<String> validator, BiConsumer<IPropertyListEntryUI, String> newValueHandler);
}
