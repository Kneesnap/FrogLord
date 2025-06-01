package net.highwayfrogs.editor.games.renderware;

import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.generic.data.IGameObject;
import net.highwayfrogs.editor.games.renderware.struct.types.RwStructParentData;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents an RwStreamChunk which has an embedded struct which is not used in any other place.
 * Created by Kneesnap on 8/26/2024.
 */
public interface IRwStreamChunkWithEmbeddedStruct extends IGameObject, IBinarySerializable, IPropertyListCreator, ICollectionViewEntry {
    /**
     * Read embedded struct data from the reader
     * @param reader the reader to read the struct data from
     * @param version the version of the struct data to read
     * @param dataLength the amount of data (in bytes) to read
     */
    void loadEmbeddedStructData(DataReader reader, int version, int dataLength);

    /**
     * Saves embedded struct data to the writer
     * @param writer the writer to write the struct data to
     * @param version the version of the struct data to write
     */
    void saveEmbeddedStructData(DataWriter writer, int version);

    /**
     * Reads the embedded struct for the given chunk
     * @param reader the chunk reader to read an embedded struct from
     */
    default void readEmbeddedStruct(DataReader reader) {
        if (!(this instanceof RwStreamChunk))
            throw new IllegalArgumentException("This object (" + Utils.getSimpleName(this) + ") is not an RwStreamChunk!");

        ((RwStreamChunk) this).readStruct(reader, new RwStructParentData<>((RwStreamChunk & IRwStreamChunkWithEmbeddedStruct) this), false);
    }

    /**
     * Saves the embedded struct for the given chunk
     * @param writer the chunk writer to write an embedded struct to
     */
    default void writeEmbeddedStruct(DataWriter writer) {
        if (!(this instanceof RwStreamChunk))
            throw new IllegalArgumentException("This object (" + Utils.getSimpleName(this) + ") is not an RwStreamChunk!");

        ((RwStreamChunk) this).writeStruct(writer, new RwStructParentData<>((RwStreamChunk & IRwStreamChunkWithEmbeddedStruct) this), false);
    }
}