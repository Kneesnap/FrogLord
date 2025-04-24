package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Represents the string chunk defined in rwstring.c
 * Created by Kneesnap on 8/14/2024.
 */
@Getter
public class RwStringChunk extends RwStreamChunk {
    private String value;

    public RwStringChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        this(streamFile, version, parentChunk, null);
    }

    public RwStringChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk, String value) {
        super(streamFile, RwStreamChunkType.STRING, version, parentChunk);
        this.value = value != null ? value : "";
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        // Seen in Frogger Ancient Shadow + Rescue + Beyond
        this.value = reader.readNullTerminatedFixedSizeString(dataLength);
        reader.align(Constants.INTEGER_SIZE); // There's unallocated data after the null terminator.
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        // Seen in Frogger Ancient Shadow + Rescue + Beyond
        writer.writeNullTerminatedString(this.value);
        writer.align(Constants.INTEGER_SIZE); // There's unallocated data after the null terminator.
    }

    @Override
    public String getCollectionViewDisplayName() {
        return super.getCollectionViewDisplayName() + " ['" + this.value + "']";
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Value", this.value);
        return propertyList;
    }

    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo() + ",value='" + this.value + "'";
    }
}