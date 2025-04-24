package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.IRwStreamChunkWithEmbeddedStruct;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.struct.types.RwV2d;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Implements the RwCamera chunk as defined in babincam.h
 * Created by Kneesnap on 8/26/2024.
 */
@Getter
public class RwCameraChunk extends RwStreamChunk implements IRwStreamChunkWithEmbeddedStruct {
    private final RwV2d viewWindow;
    private final RwV2d viewOffset;
    private float nearPlane;
    private float farPlane;
    private float fogPlane;
    private int projection;

    public RwCameraChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.CAMERA, version, parentChunk);
        this.viewWindow = new RwV2d(streamFile.getGameInstance());
        this.viewOffset = new RwV2d(streamFile.getGameInstance());
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        readEmbeddedStruct(reader);
        readOptionalExtensionData(reader);
    }

    @Override
    public void loadEmbeddedStructData(DataReader reader, int version, int dataLength) {
        // Implements rwStreamCamera as defined in babincam.h
        this.viewWindow.load(reader, version, dataLength);
        this.viewOffset.load(reader, version, dataLength);
        this.nearPlane = reader.readFloat();
        this.farPlane = reader.readFloat();
        this.fogPlane = reader.readFloat();
        this.projection = reader.readInt();
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeEmbeddedStruct(writer);
        writeOptionalExtensionData(writer);
    }

    @Override
    public void saveEmbeddedStructData(DataWriter writer, int version) {
        this.viewWindow.save(writer, version);
        this.viewOffset.save(writer, version);
        writer.writeFloat(this.nearPlane);
        writer.writeFloat(this.farPlane);
        writer.writeFloat(this.fogPlane);
        writer.writeInt(this.projection);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("View Window", this.viewWindow);
        propertyList.add("View Offset", this.viewOffset);
        propertyList.add("Near Plane", this.nearPlane);
        propertyList.add("Far Plane", this.farPlane);
        propertyList.add("Fog Plane", this.fogPlane);
        propertyList.add("Projection", this.projection);
        return propertyList;
    }
}