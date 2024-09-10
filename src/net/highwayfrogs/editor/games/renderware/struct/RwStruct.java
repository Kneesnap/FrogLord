package net.highwayfrogs.editor.games.renderware.struct;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GameObject.SharedGameObject;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Represents a RenderWare struct. Generally read from a RenderWare Stream.
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public abstract class RwStruct extends SharedGameObject implements ICollectionViewEntry, IPropertyListCreator {
    @NonNull private final RwStructType structType;

    public RwStruct(GameInstance instance, @NonNull RwStructType structType) {
        super(instance);
        this.structType = structType;
    }

    /**
     * Loads the data for this struct from the reader.
     * @param reader the reader to read the struct data from
     * @param version the RenderWare version used to write the data
     * @param byteLength the number of bytes which the struct should use
     */
    public abstract void load(DataReader reader, int version, int byteLength);

    /**
     * Saves the data for this struct to the writer.
     * @param writer the writer to write the struct data to
     * @param version the RenderWare version we should try to mimic the data format of
     */
    public abstract void save(DataWriter writer, int version);

    /**
     * Returns a UI controller specific to this chunk, if one exists.
     */
    public GameUIController<?> makeEditorUI() {
        return null; // By default, there is no UI.
    }

    @Override
    public String getCollectionViewDisplayName() {
        return this.structType.getDisplayName();
    }

    @Override
    public String getCollectionViewDisplayStyle() {
        return null;
    }

    @Override
    public Image getCollectionViewIcon() {
        return this.structType.getIcon().getFxImage();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Struct Type", this.structType);
        return propertyList;
    }

    /**
     * This method allows struct objects to provide additional info about itself to the logger.
     * @return loggerInfo
     */
    public String getLoggerInfo() {
        return ",type=" + this.structType;
    }
}