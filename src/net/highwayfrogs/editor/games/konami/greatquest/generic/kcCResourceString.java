package net.highwayfrogs.editor.games.konami.greatquest.generic;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Represents a string.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
public class kcCResourceString extends GameData<GreatQuestInstance> implements IPropertyListCreator {
    private String value;

    public kcCResourceString(GreatQuestInstance instance) {
        this(instance, "");
    }

    public kcCResourceString(GreatQuestInstance instance, String value) {
        super(instance);
        this.value = value;
    }

    @Override
    public void load(DataReader reader) {
        this.value = reader.readNullTerminatedString();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeTerminatorString(this.value);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Value", this.value,
                () -> InputMenu.promptInputBlocking(getGameInstance(), "Please enter the new string.", this.value, this::setValue));

        return propertyList;
    }

    /**
     * Sets the string resource value.
     * @param newValue The new value to apply
     */
    public void setValue(String newValue) {
        if (newValue == null)
            throw new NullPointerException("newValue");

        this.value = newValue;
    }
}