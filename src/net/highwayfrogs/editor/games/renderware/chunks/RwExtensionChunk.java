package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the rwEXTENSION data.
 * Created by Kneesnap on 8/15/2024.
 */
@Getter
public class RwExtensionChunk extends RwStreamChunk {
    private final List<RwStreamChunk> extensionChunks = new ArrayList<>();

    public RwExtensionChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.EXTENSION, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        this.extensionChunks.clear();
        while (dataLength > 0) {
            int dataStartIndex = reader.getIndex();
            this.extensionChunks.add(readChunk(reader));
            int dataEndIndex = reader.getIndex();
            dataLength -= (dataEndIndex - dataStartIndex);
        }
    }

    @Override
    public void saveChunkData(DataWriter writer) {
        for (int i = 0; i < this.extensionChunks.size(); i++)
            writeChunk(writer, this.extensionChunks.get(i));
    }

    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo() + ",chunkCount=" + this.extensionChunks.size();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Extension Chunk Count", this.extensionChunks.size());
        return propertyList;
    }

    /**
     * Returns the first extension chunk which is an instance of the provided class, if one is found.
     * @param chunkClass the class of the extension chunk to obtain
     * @return extensionChunk, or null if none are found of the class provided
     * @param <TStreamChunk> the type of extension chunk to obtain
     */
    public <TStreamChunk extends RwStreamChunk> TStreamChunk getExtension(Class<TStreamChunk> chunkClass) {
        if (chunkClass == null)
            throw new NullPointerException("chunkClass");

        for (int i = 0; i < this.extensionChunks.size(); i++) {
            RwStreamChunk testChunk = this.extensionChunks.get(i);
            if (chunkClass.isInstance(testChunk))
                return chunkClass.cast(testChunk);
        }

        return null;
    }
}