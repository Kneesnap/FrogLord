package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.struct.types.RwStructInt32;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an rpGeometryList in baclump.c/GeometryListStreamRead
 * Created by Kneesnap on 8/25/2024.
 */
@Getter
public class RwGeometryListChunk extends RwStreamChunk {
    private final List<RwGeometryChunk> geometries = new ArrayList<>();

    public RwGeometryListChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.GEOMETRY_LIST, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        int geometryCount = readStruct(reader, RwStructInt32.class, false).getValue();
        this.geometries.clear();
        for (int i = 0; i < geometryCount; i++)
            this.geometries.add(readChunk(reader, RwGeometryChunk.class));
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeStruct(writer, new RwStructInt32(getGameInstance(), this.geometries.size()), false);
        for (int i = 0; i < this.geometries.size(); i++)
            writeChunk(writer, this.geometries.get(i));
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Geometries", this.geometries.size());
        return propertyList;
    }

    @Override
    protected String getLoggerInfo() {
        return super.getLoggerInfo() + ",geometryCount=" + this.geometries.size();
    }
}