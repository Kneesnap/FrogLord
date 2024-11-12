package net.highwayfrogs.editor.games.konami.greatquest.generic;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestCharsetProvider;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.gui.InputMenu;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.FXUtils;

/**
 * Represents a string.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
public class kcCResourceString extends GameData<GreatQuestInstance> implements kcIGenericResourceData, IPropertyListCreator {
    private final kcCResourceGeneric resource;
    private String value;

    public kcCResourceString(@NonNull kcCResourceGeneric resource) {
        this(resource, "");
    }

    public kcCResourceString(@NonNull kcCResourceGeneric resource, @NonNull String value) {
        super(resource.getGameInstance());
        this.resource = resource;
        this.value = value;
    }

    @Override
    public void load(DataReader reader) {
        this.value = reader.readNullTerminatedString(GreatQuestCharsetProvider.getCharset());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeNullTerminatedString(this.value, GreatQuestCharsetProvider.getCharset());
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.STRING_RESOURCE;
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

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        MenuItem newItem = new MenuItem("Copy Text to Clipboard");
        contextMenu.getItems().add(newItem);
        newItem.setOnAction(event -> FXUtils.setClipboardText(this.value));
    }
}