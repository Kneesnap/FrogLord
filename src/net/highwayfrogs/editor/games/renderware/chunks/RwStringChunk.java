package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwVersion;
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
        if (RwVersion.isAtLeast(version, RwVersion.VERSION_3603)) {
            // Seen in Frogger Ancient Shadow
            this.value = reader.readTerminatedStringOfLength(dataLength);
            reader.align(Constants.INTEGER_SIZE); // There's unallocated data afterward.
        } else {
            // Seen in Frogger Rescue + Beyond
            this.value = reader.readString(dataLength);
        }
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        if (RwVersion.isAtLeast(version, RwVersion.VERSION_3603)) {
            // Seen in Frogger Ancient Shadow
            writer.writeTerminatorString(this.value);
            writer.align(Constants.INTEGER_SIZE); // There's unallocated data afterward.
        } else {
            // Seen in Frogger Rescue + Beyond
            writer.writeStringBytes(this.value);
        }
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