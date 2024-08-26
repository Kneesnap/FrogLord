package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a struct with arbitrary contents.
 * Created by Kneesnap on 6/9/2020.
 */
@Getter
public class RwStructChunk<TStruct extends RwStruct> extends RwStreamChunk {
    @NonNull private final Class<? extends TStruct> structClass;
    private TStruct value;

    private static final Map<Class<? extends RwStruct>, Constructor<? extends RwStruct>> CONSTRUCTOR_CACHE = new HashMap<>();

    public RwStructChunk(RwStreamFile streamFile, int renderwareVersion, RwStreamChunk parentChunk, Class<? extends TStruct> structClass) {
        super(streamFile, RwStreamChunkType.STRUCT, renderwareVersion, parentChunk);
        this.structClass = structClass;
    }

    @SuppressWarnings("unchecked")
    public RwStructChunk(RwStreamFile streamFile, int renderwareVersion, RwStreamChunk parentChunk, TStruct value) {
        super(streamFile, RwStreamChunkType.STRUCT, renderwareVersion, parentChunk);
        if (value == null)
            throw new NullPointerException("value");
        this.structClass = (Class<? extends TStruct>) value.getClass();
        this.value = value;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        if (this.value == null) {
            Constructor<? extends RwStruct> constructor = CONSTRUCTOR_CACHE.get(this.structClass);
            if (constructor == null) {
                try {
                    CONSTRUCTOR_CACHE.put(this.structClass, constructor = this.structClass.getConstructor(GameInstance.class));
                } catch (NoSuchMethodException ex) {
                    Utils.handleError(getLogger(), ex, false, "Could not find constructor for RwStruct %s.", this.structClass.getSimpleName());
                    return;
                }
            }

            try {
                this.value = (TStruct) constructor.newInstance(getGameInstance());
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "Failed to construct new %s object.", this.structClass.getSimpleName());
                return;
            }
        }

        this.value.load(reader, version, dataLength);
        if (this.value.getStructType().isReadExtensionChunk())
            readOptionalExtensionData(reader);
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        if (this.value == null)
            throw new IllegalStateException("Cannot save struct data, the value is null!");

        this.value.save(writer, getVersion());
        if (this.value.getStructType().isReadExtensionChunk())
            writeOptionalExtensionData(writer);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        return this.value != null ? this.value.addToPropertyList(propertyList) : propertyList;
    }

    @Override
    public GameUIController<?> makeEditorUI() {
        GameUIController<?> editor = this.value != null ? this.value.makeEditorUI() : null;
        return editor != null ? editor : super.makeEditorUI(); // By default, there is no UI.
    }

    @Override
    public String getCollectionViewDisplayName() {
        return super.getCollectionViewDisplayName()
                + (this.value != null ? " [" + this.value.getStructType().getDisplayName() + "]" : "");
    }

    @Override
    public String getLoggerInfo() {
        return super.getLoggerInfo() + (this.value != null ? this.value.getLoggerInfo() : "");
    }
}