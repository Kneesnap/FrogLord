package net.highwayfrogs.editor.games.renderware.chunks;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.beyond.FroggerBeyondGameType;
import net.highwayfrogs.editor.games.renderware.IRwStreamChunkWithEmbeddedStruct;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.chunks.RwFrameListChunk.RwStreamFrame;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.List;

/**
 * Represents an atomic clump stream chunk.
 * Created by Kneesnap on 8/25/2024.
 */
public class RwAtomicChunk extends RwStreamChunk implements IRwStreamChunkWithEmbeddedStruct {
    private RwGeometryChunk embeddedGeometry;
    private int frameIndex;
    private int geometryIndex;
    private int flags;
    // renderType & blendMode are an added modification, eg: part of AL: Reborn. We do not need to manage them.

    public RwAtomicChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.ATOMIC, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        readEmbeddedStruct(reader);
        this.embeddedGeometry = hasEmbeddedGeometry() ? readChunk(reader, RwGeometryChunk.class) : null;
        readOptionalExtensionData(reader);
    }

    @Override
    public void loadEmbeddedStructData(DataReader reader, int version, int dataLength) {
        // Implements the '_rpAtomicBinary' struct as defined in baclump.c
        this.frameIndex = reader.readInt();
        this.geometryIndex = reader.readInt();
        this.flags = reader.readInt();
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // unused
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeEmbeddedStruct(writer);
        if (hasEmbeddedGeometry())
            writeChunk(writer, this.embeddedGeometry != null ? this.embeddedGeometry : new RwGeometryChunk(getStreamFile(), getVersion(), this));
        writeOptionalExtensionData(writer);
    }

    @Override
    public void saveEmbeddedStructData(DataWriter writer, int version) {
        writer.writeInt(this.frameIndex);
        writer.writeInt(this.geometryIndex);
        writer.writeInt(this.flags);
        writer.writeInt(0); // unused
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Frame Index", this.frameIndex);
        propertyList.add("Geometry Index", this.geometryIndex);
        propertyList.add("Flags", NumberUtils.toHexString(this.flags));
        return propertyList;
    }

    /**
     * Gets the geometry associated with the chunk.
     */
    public RwGeometryChunk getGeometry() {
        if (hasEmbeddedGeometry())
            return this.embeddedGeometry;

        RwGeometryListChunk chunk = findGeometryList();
        if (chunk == null)
            return null;

        List<RwGeometryChunk> geometries = chunk.getGeometries();
        return this.geometryIndex >= 0 && geometries.size() > this.geometryIndex
                ? geometries.get(this.geometryIndex) : null;
    }

    /**
     * Gets the frame associated with the chunk if there is one.
     */
    public RwStreamFrame getFrame() {
        if (!(getParentChunk() instanceof RwClumpChunk))
            return null;

        List<RwStreamFrame> frames = ((RwClumpChunk) getParentChunk()).getFrameList().getFrames();
        return this.frameIndex >= 0 && frames.size() > this.frameIndex
                ? frames.get(this.frameIndex) : null;
    }

    /**
     * Returns true iff we expect embedded geometry.
     */
    public boolean hasEmbeddedGeometry() {
        if (getGameInstance().getGameType() == FroggerBeyondGameType.INSTANCE)
            return false; // Frogger Beyond doesn't seem to ever embed geometry, but the game seems to want to.

        RwGeometryListChunk geometryList = findGeometryList();
        return geometryList == null || geometryList.getGeometries().isEmpty();
    }

    private RwGeometryListChunk findGeometryList() {
        if (getParentChunk() instanceof RwClumpChunk)
            return ((RwClumpChunk) getParentChunk()).getGeometryList();

        for (RwStreamChunk testChunk : getStreamFile().getChunks())
            if (testChunk instanceof RwGeometryListChunk && ((RwGeometryListChunk) testChunk).getGeometries().size() > 0)
                return (RwGeometryListChunk) testChunk;

        return null;
    }
}