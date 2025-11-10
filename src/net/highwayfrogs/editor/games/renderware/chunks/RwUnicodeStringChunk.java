package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.nio.charset.StandardCharsets;

/**
 * Represents the unicode string chunk defined in rwstring.c
 * Created by Kneesnap on 8/14/2024.
 */
@Getter
public class RwUnicodeStringChunk extends RwStreamChunk {
    private String value = "";

    public RwUnicodeStringChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.UNICODE_STRING, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        this.value = reader.readTerminatedString(dataLength, StandardCharsets.UTF_8);
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writer.writeStringBytes(this.value, StandardCharsets.UTF_8);
    }

    @Override
    public String getCollectionViewDisplayName() {
        return super.getCollectionViewDisplayName() + " ['" + this.value + "']";
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Value", this.value);
    }

    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo() + ",value='" + this.value + "'";
    }
}