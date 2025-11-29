package net.highwayfrogs.editor.gui.components.propertylist;

import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;
import java.util.function.*;

/**
 * Represents a PropertyListEntry which stores an object.
 * Created by Kneesnap on 11/9/2025.
 */
public class PropertyListDataEntry<TData> extends PropertyListEntry {
    private TData dataObject;
    private Function<TData, String> objectDataToStringConverter;
    private Function<String, TData> objectDataFromStringConverter;
    private Supplier<TData> simpleDataProvider;
    private Function<IPropertyListEntryUI, TData> advancedDataProvider;
    private Predicate<TData> dataValidator;
    private Consumer<TData> simpleDataHandler;
    private BiConsumer<IPropertyListEntryUI, TData> advancedDataHandler;

    public PropertyListDataEntry(IPropertyListCreator propertyListCreator, String name, TData dataObject) {
        super(propertyListCreator, name);
        setDataObject(dataObject);
    }

    @Override
    public boolean isEditingAllowed() {
        return (this.advancedDataProvider != null || this.simpleDataProvider != null || this.objectDataFromStringConverter != null)
                && (this.simpleDataHandler != null || this.advancedDataHandler != null);
    }

    @Override
    public void setupEditor(IPropertyListEntryUI entryUI) {
        if (this.advancedDataProvider != null) {
            TData newData;
            try {
                newData = this.advancedDataProvider.apply(entryUI);
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }

            if (newData != null)
                handleNewData(entryUI, newData);
        } else if (this.simpleDataProvider != null) {
            TData newData;
            try {
                newData = this.simpleDataProvider.get();
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }

            if (newData != null)
                handleNewData(entryUI, newData);
        } else {
            entryUI.edit(getValue(), this::validateData, this::handleEdit);
        }
    }

    private boolean validateData(String newValue) {
        TData data;
        try {
            data = this.objectDataFromStringConverter.apply(newValue);
        } catch (Throwable th) {
            return false;
        }

        if (this.dataValidator == null)
            return true;

        try {
            return this.dataValidator.test(data);
        } catch (Throwable th) {
            return false;
        }
    }

    private void handleEdit(IPropertyListEntryUI entryUI, String newValue) {
        TData newData = this.objectDataFromStringConverter.apply(newValue);
        handleNewData(entryUI, newData);
    }

    private void handleNewData(IPropertyListEntryUI entryUI, TData newData) {
        if (newData == this.dataObject) {
            setValue(getDataObjectAsString()); // Update the value, as it's possible something in the object changed.
            return;
        }

        if (this.advancedDataHandler != null) {
            this.advancedDataHandler.accept(entryUI, newData);
        } else {
            this.simpleDataHandler.accept(newData);
        }

        setDataObject(newData);
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
     * Sets the object data provider which will provide a new value through arbitrary means, usually other than editing the text field.
     * @param newDataProvider the new data handler
     */
    public PropertyListDataEntry<TData> setDataProvider(Supplier<TData> newDataProvider) {
        this.simpleDataProvider = newDataProvider;
        this.advancedDataProvider = null;
        return this;
    }

    /**
     * Sets the object data provider which will provide a new value through arbitrary means, usually other than editing the text field.
     * @param newDataProvider the new data handler
     */
    public PropertyListDataEntry<TData> setDataProvider(Function<IPropertyListEntryUI, TData> newDataProvider) {
        this.simpleDataProvider = null;
        this.advancedDataProvider = newDataProvider;
        return this;
    }

    /**
     * Sets the object data handler which will handle/apply a new value.
     * @param newDataHandler the new data handler
     */
    public PropertyListDataEntry<TData> setDataHandler(Consumer<TData> newDataHandler) {
        this.simpleDataHandler = newDataHandler;
        this.advancedDataHandler = null;
        return this;
    }

    /**
     * Sets the object data handler which will handle/apply a new value.
     * @param newDataHandler the new data handler
     */
    public PropertyListDataEntry<TData> setDataHandler(BiConsumer<IPropertyListEntryUI, TData> newDataHandler) {
        this.simpleDataHandler = null;
        this.advancedDataHandler = newDataHandler;
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
