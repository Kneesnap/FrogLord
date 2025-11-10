package net.highwayfrogs.editor.gui.components.propertylist;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Represents an entry in a property list.
 * Created by Kneesnap on 11/8/2025.
 */
public class PropertyListEntry extends PropertyListNode {
    private final StringProperty nameProperty = new SimpleStringProperty();
    private final StringProperty valueProperty = new SimpleStringProperty();
    @Getter private PropertyList propertyList;
    @Getter private PropertyListNode parentNode;
    private BiConsumer<IPropertyListEntryUI, String> newValueHandler; // TODO: Move this to another implementation?
    private Predicate<String> newValueValidator; // TODO: Move this to another implementation?

    public PropertyListEntry(IPropertyListCreator propertyListCreator, String name) {
        super(propertyListCreator);
        setName(name);
    }

    @Override
    public void toString(StringBuilder builder) {
        builder.append(this.nameProperty.get());
        String value = this.valueProperty.get();
        if (!StringUtils.isNullOrWhiteSpace(value))
            builder.append(": ").append(value);
    }

    @Override
    public void toString(StringBuilder builder, String padding, int paddingAmount) {
        if (padding != null && !padding.isEmpty())
            for (int i = 0; i < paddingAmount; i++)
                builder.append(padding);

        toString(builder);
        super.toString(builder, padding, paddingAmount);
    }

    /**
     * Called when the entry is added to a property list.
     * @param propertyList the property list which the entry has been added to
     * @param parentEntry the parent entry which has been added to
     */
    protected void onAddedToPropertyList(@NonNull PropertyList propertyList, PropertyListNode parentEntry) {
        this.propertyList = propertyList;
        this.parentNode = parentEntry;
    }

    /**
     * Gets the property name display text.
     * This value should not generally be null, but null should still be treated as a possible value.
     * @return propertyName
     */
    public String getName() {
        return this.nameProperty.get();
    }

    /**
     * Sets the property name display text, alerting any listeners.
     */
    public PropertyListEntry setName(String newName) {
        if (!Objects.equals(newName, this.nameProperty.get())) // Prevent triggering a name update getting broadcast to listeners.
            this.nameProperty.set(newName);

        return this;
    }

    /**
     * Gets the property value display text.
     * The value is allowed to be null, and should be hidden in this case. If null should be displayed, the literal text "null" will be returned.
     * @return propertyValue
     */
    public String getValue() {
        return this.nameProperty.get();
    }

    /**
     * Sets the property value display text, alerting any listeners.
     */
    public PropertyListEntry setValue(String newValue) {
        if (!Objects.equals(newValue, this.valueProperty.get())) // Prevent triggering a value update getting broadcast to listeners.
            this.valueProperty.set(newValue);

        return this;
    }

    /**
     * Gets a property representing the displayed property name.
     * This property may be updated at any time to reflect a new name.
     */
    public StringProperty nameProperty() {
        return this.nameProperty;
    }

    /**
     * Gets a property representing the displayed property value.
     * This property may be updated at any time to reflect a new value.
     */
    public StringProperty valueProperty() {
        return this.valueProperty;
    }


    // TODO: Here we can have methods for handling the double-click.
    //  -> Ensure access to refreshing the UI in edit/callback mode? Ensure we've got access to the TreeItem or more, like static methods to refresh it. But it should be done via indirection. Eg: If we display a PropertyList in more UI targets, we should have abstraction to prevent us directly touching a TreeItem in the code trying to refresh the UI.
    //  -> Many properties probably shouldn't make a full pop-up, but instead accept editing a textbox in-place. There should be a system which allows both pop-up prompts as well as text field edits, which can use the same validation.
    //  -> Refer to SfxEntrySimpleAttributes.addToPropertyList() to see places where we might want to have convenience method definitions to avoid tons of duplicated code.
    //   -> Ensure we handle things such as exiting the window cleanly.
}
