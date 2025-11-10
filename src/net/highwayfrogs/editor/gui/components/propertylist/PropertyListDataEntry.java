package net.highwayfrogs.editor.gui.components.propertylist;

import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a PropertyListEntry which stores an object.
 * Created by Kneesnap on 11/9/2025.
 */
public class PropertyListDataEntry<TData> extends PropertyListEntry {
    private TData dataObject;
    private Function<TData, String> objectDataToStringConverter;
    private Function<String, TData> objectDataFromStringConverter; // TODO: Convert this and the following one into a single pass when passing off to the UI?
    private Predicate<TData> dataValidator;
    private Consumer<TData> dataHandler; // TODO: !
    // TODO: Missing data handler.

    public PropertyListDataEntry(IPropertyListCreator propertyListCreator, String name, TData dataObject) {
        super(propertyListCreator, name);
        setDataObject(dataObject);
    }

    /**
     * Sets the data object.
     * @param dataObject the data object to apply
     */
    public PropertyListDataEntry<TData> setDataObject(TData dataObject) {
        if (this.dataObject == dataObject)
            return this;

        this.dataObject = dataObject;
        updateValue();
        return this;
    }

    /**
     * Sets the object data to string converter.
     * @param dataToStringConverter the converter to apply
     */
    public PropertyListDataEntry<TData> setDataToStringConverter(Function<TData, String> dataToStringConverter) {
        this.objectDataToStringConverter = dataToStringConverter;
        updateValue();
        return this;
    }

    /**
     * Sets the object data from string converter.
     * @param dataFromStringConverter the converter to apply
     */
    public PropertyListDataEntry<TData> setDataFromStringConverter(Function<String, TData> dataFromStringConverter) {
        this.objectDataFromStringConverter = dataFromStringConverter;
        return this;
    }

    /**
     * Sets the object data validity check.
     * @param newDataValidator the data validator
     */
    public PropertyListDataEntry<TData> setDataValidator(Predicate<TData> newDataValidator) {
        this.dataValidator = newDataValidator;
        return this;
    }

    /**
     * Sets the object data handler which will accept the value after other values.
     * @param newDataHandler the new data handler
     */
    public PropertyListDataEntry<TData> setDataHandler(Consumer<TData> newDataHandler) {
        this.dataHandler = newDataHandler;
        return this;
    }

    private void updateValue() {
        setValue(getDataObjectAsString());
    }

    /**
     * Gets the data object as a display string
     * @return displayString
     */
    public String getDataObjectAsString() {
        try {
            return this.objectDataToStringConverter != null ? this.objectDataToStringConverter.apply(this.dataObject) : Objects.toString(this.dataObject);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "Encountered error while getting the dataObject (%s) as a string!", this.dataObject);
            return "<ERROR>";
        }
    }
}
