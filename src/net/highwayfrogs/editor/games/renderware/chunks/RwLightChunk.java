package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.IRwStreamChunkWithEmbeddedStruct;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Implements lighting data types as seen in balight.h
 * Created by Kneesnap on 8/26/2024.
 */
@Getter
public class RwLightChunk extends RwStreamChunk implements IRwStreamChunkWithEmbeddedStruct {
    private float radius = 1F;
    private float red = 1F;
    private float green = 1F;
    private float blue = 1F;
    private float minusCosAngle;
    private int typeAndFlags;

    public RwLightChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.LIGHT, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        readEmbeddedStruct(reader);
        readOptionalExtensionData(reader);
    }

    @Override
    public void loadEmbeddedStructData(DataReader reader, int version, int dataLength) {
        // Implements _rpLight/RpLightChunkInfo as defined in balight.h
        this.radius = reader.readFloat();
        this.red = reader.readFloat();
        this.green = reader.readFloat();
        this.blue = reader.readFloat();
        this.minusCosAngle = reader.readFloat();
        this.typeAndFlags = reader.readInt();
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeEmbeddedStruct(writer);
        writeOptionalExtensionData(writer);
    }

    @Override
    public void saveEmbeddedStructData(DataWriter writer, int version) {
        writer.writeFloat(this.radius);
        writer.writeFloat(this.red);
        writer.writeFloat(this.green);
        writer.writeFloat(this.blue);
        writer.writeFloat(this.minusCosAngle);
        writer.writeInt(this.typeAndFlags);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Radius", this.radius);
        propertyList.add("Red", this.red);
        propertyList.add("Green", this.green);
        propertyList.add("Blue", this.blue);
        propertyList.add("-cosAngle", this.minusCosAngle);
        propertyList.add("Type & Flags", Utils.toHexString(this.typeAndFlags));
        return propertyList;
    }
}