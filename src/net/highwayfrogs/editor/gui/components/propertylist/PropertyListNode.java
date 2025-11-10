package net.highwayfrogs.editor.gui.components.propertylist;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Represents the basis of a property list node which is capable of holding child property list entries.
 * Created by Kneesnap on 11/8/2025.
 */
public abstract class PropertyListNode {
    private final IPropertyListCreator propertyListCreator;
    @Getter protected final ObservableList<PropertyListEntry> childEntries;
    private boolean pendingChildPopulation;

    public PropertyListNode(IPropertyListCreator propertyListCreator) {
        this.propertyListCreator = propertyListCreator;
        this.childEntries = propertyListCreator != null ? FXCollections.observableArrayList() : null;
        this.pendingChildPopulation = (this.childEntries != null);
    }

    /**
     * Returns true iff this node is capable of having properties.
     */
    public boolean canHaveProperties() {
        return this.childEntries != null;
    }

    /**
     * Populates the list of child entries if it has not been populated yet.
     */
    public void populateChildEntriesIfNecessary() {
        if (this.pendingChildPopulation) {
            this.pendingChildPopulation = false;
            updateChildEntries();
        }
    }

    /**
     * Clear all child entries and make new ones to replace the old ones.
     */
    public void updateChildEntries() {
        if (this.childEntries == null)
            return;

        clearChildEntries();
        this.propertyListCreator.addToPropertyList(this);
    }

    /**
     * Adds a property list entry to the child entry list.
     * @param entry the entry to add
     * @return entry
     */
    public <TEntry extends PropertyListEntry> TEntry add(TEntry entry) {
        if (entry == null)
            throw new NullPointerException("entry");
        if (!canHaveProperties())
            throw new IllegalStateException("This " + Utils.getSimpleName(this) + " is not capable of holding properties!");
        if (entry.getParentNode() != null || entry.getPropertyList() != null)
            throw new IllegalArgumentException("The provided entry is already registered to a PropertyListEntry.");

        entry.onAddedToPropertyList(getPropertyList(), this);
        this.childEntries.add(entry);
        return entry;
    }

    /**
     * Adds a new property to the property list.
     * @param name the name of the property to add
     * @param value the value to add
     * @return newEntry
     */
    public <TData> PropertyListDataEntry<TData> add(String name, TData value) {
        return add(new PropertyListDataEntry<>(null, name, value));
    }

    /**
     * Adds a new property to the property list.
     * @param name the name of the property to add
     * @param value the value to add
     * @return newEntry
     */
    public <TData> PropertyListDataEntry<TData> add(String name, TData value, Supplier<TData> newValue) {
        // TODO: Implement properly.
        return add(new PropertyListDataEntry<>(null, name, value));
    }

    /**
     * Adds a property list creator to the property list.
     * @param name the name of the property list
     * @param propertyListCreator the property list creator to create the property list
     * @return newEntry
     */
    public PropertyListEntry add(String name, IPropertyListCreator propertyListCreator) {
        return add(new PropertyListEntry(propertyListCreator, name));
    }

    /**
     * Adds a property list creator to the property list.
     * @param name the name of the property list
     * @param value the value display text for the property list
     * @param propertyListCreator the property list creator to create the property list
     * @return newEntry
     */
    public PropertyListEntry add(String name, String value, IPropertyListCreator propertyListCreator) {
        return add(new PropertyListEntry(propertyListCreator, name).setValue(value));
    }


    private static final Function<String, String> STRING_CONVERTER = str -> str;

    /**
     * Adds a String value to the property list, without allowing the user to change it.
     * @param name the name of the value to add
     * @param value the value to add
     * @return newEntry
     */
    public PropertyListEntry addString(String name, String value) {
        return add(new PropertyListDataEntry<>(null, name, value));
    }

    /**
     * Adds a String value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<String> addString(String name, String value, Consumer<String> handler) {
        // TODO: IMPLEMENT
        return null;
    }

    /**
     * Adds a String value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param validator the behavior for confirming whether a value can be applied
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<String> addString(String name, String value, Predicate<String> validator, Consumer<String> handler) {
        // TODO: IMPLEMENT
        return null;
    }

    /**
     * Adds a boolean value to the property list, without allowing the user to change it.
     * @param name the name of the value to add
     * @param value the value to add
     * @return newEntry
     */
    public PropertyListEntry addBoolean(String name, boolean value) {
        return add(new PropertyListDataEntry<>(null, name, value));
    }

    /**
     * Adds a boolean value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Boolean> addBoolean(String name, boolean value, Consumer<Boolean> handler) {
        // TODO: IMPLEMENT
        return null;
    }

    /**
     * Adds a short value to the property list, without allowing the user to change it.
     * @param name the name of the value to add
     * @param value the value to add
     * @return newEntry
     */
    public PropertyListDataEntry<Short> addShort(String name, short value) {
        return add(new PropertyListDataEntry<>(null, name, value));
    }

    /**
     * Adds a short value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Short> addShort(String name, short value, Consumer<Short> handler) {
        return add(new PropertyListDataEntry<>(null, name, value))
                .setDataHandler(handler)
                .setDataFromStringConverter(Short::parseShort);
    }

    /**
     * Adds a short value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param validator the behavior for confirming whether a value can be applied
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Short> addShort(String name, short value, Predicate<Short> validator, Consumer<Short> handler) {
        return addShort(name, value, handler)
                .setDataValidator(validator);
    }

    /**
     * Adds an integer value to the property list, without allowing the user to change it.
     * @param name the name of the value to add
     * @param value the value to add
     * @return newEntry
     */
    public PropertyListDataEntry<Integer> addShort(String name, int value) {
        return add(new PropertyListDataEntry<>(null, name, value));
    }

    /**
     * Adds an integer value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Integer> addInteger(String name, int value, Consumer<Integer> handler) {
        return add(new PropertyListDataEntry<>(null, name, value))
                .setDataHandler(handler)
                .setDataFromStringConverter(Integer::parseInt);
    }

    /**
     * Adds an integer value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param validator the behavior for confirming whether a value can be applied
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Integer> addInteger(String name, int value, Predicate<Integer> validator, Consumer<Integer> handler) {
        return addInteger(name, value, handler)
                .setDataHandler(handler)
                .setDataValidator(validator);
    }

    /**
     * Adds a float value to the property list, without allowing the user to change it.
     * @param name the name of the value to add
     * @param value the value to add
     * @return newEntry
     */
    public PropertyListDataEntry<Float> addFloat(String name, float value) {
        return add(new PropertyListDataEntry<>(null, name, value));
    }

    /**
     * Adds a float value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Float> addFloat(String name, float value, Consumer<Float> handler) {
        return add(new PropertyListDataEntry<>(null, name, value))
                .setDataHandler(handler)
                .setDataFromStringConverter(Float::parseFloat);
    }

    /**
     * Adds a float value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param validator the behavior for confirming whether a value can be applied
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Float> addFloat(String name, float value, Predicate<Float> validator, Consumer<Float> handler) {
        return addFloat(name, value, handler)
                .setDataValidator(validator);
    }

    /**
     * Adds a double value to the property list, without allowing the user to change it.
     * @param name the name of the value to add
     * @param value the value to add
     * @return newEntry
     */
    public PropertyListDataEntry<Double> addDouble(String name, double value) {
        return add(new PropertyListDataEntry<>(null, name, value));
    }

    /**
     * Adds a double value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Double> addDouble(String name, double value, Consumer<Double> handler) {
        return add(new PropertyListDataEntry<>(null, name, value))
                .setDataHandler(handler)
                .setDataFromStringConverter(Double::parseDouble);
    }

    /**
     * Adds a double value to the property list, allowing the user to edit the value.
     * @param name the name of the value to add
     * @param value the value to add
     * @param validator the behavior for confirming whether a value can be applied
     * @param handler the handling behavior for a new value
     * @return newEntry
     */
    public PropertyListDataEntry<Double> addDouble(String name, double value, Predicate<Double> validator, Consumer<Double> handler) {
        return addDouble(name, value, handler)
                .setDataValidator(validator);
    }

    /**
     * Clear all child entries.
     */
    public void clearChildEntries() {
        if (!canHaveProperties())
            return;

        this.childEntries.clear();
        this.pendingChildPopulation = true;
    }

    /**
     * Gets the logger used for writing messages relating to this property list.
     * @return logger
     */
    public ILogger getLogger() {
        return getPropertyList().getLogger();
    }

    /**
     * Gets the property list.
     */
    public abstract PropertyList getPropertyList();

    /**
     * Write the property list entry (in isolation) to a StringBuilder.
     * This will not write any newline characters or child entries.
     * @param builder the StringBuilder to append to
     */
    public abstract void toString(StringBuilder builder);

    /**
     * Writes the property list base, and all of its child property list entries to a StringBuilder
     * @param builder the StringBuilder to append to
     * @param padding the string representing padding
     * @param paddingAmount how many times the padding string should be written when writing padding
     */
    public void toString(StringBuilder builder, String padding, int paddingAmount) {
        populateChildEntriesIfNecessary();

        if (canHaveProperties()) {
            int newPaddingAmount = paddingAmount + 1;
            for (int i = 0; i < this.childEntries.size(); i++)
                this.childEntries.get(i).toString(builder, padding, newPaddingAmount);
        }
    }
}
