package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwStreamSectionType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.nio.charset.StandardCharsets;

/**
 * Represents the unicode string section defined in rwstring.c
 * Created by Kneesnap on 8/14/2024.
 */
@Getter
public class RwUnicodeStringChunk extends RwStreamChunk {
    private String value = "";

    public RwUnicodeStringChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamSectionType.UNICODE_STRING, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        this.value = reader.readString(dataLength, StandardCharsets.UTF_8);
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