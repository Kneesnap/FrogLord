package net.highwayfrogs.editor.games.renderware.chunks.sector;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.RwUtils;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Implements the RwPlaneSector chunk, PlaneSectorStreamRead/babinwor.c
 * Created by Kneesnap on 8/16/2024.
 */
@Getter
public class RwPlaneSectorChunk extends RwSectorBase {
    private float value;
    private float leftValue; // Left node contains less of the plane. (leftValue >= value >= rightValue). If either is atomic, it is equal to value.
    private float rightValue; // Right node contains more of the plane. (leftValue >= value >= rightValue). If either is atomic, it is equal to value.
    private RwSectorBase leftSector;
    private RwSectorBase rightSector;

    public RwPlaneSectorChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.PLANE_SECTOR, version, parentChunk);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        if (!getWorld().getPlaneSectors().contains(this))
            getWorld().getPlaneSectors().add(this);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        RpPlaneSectorChunkInfo chunkInfo = readStruct(reader, RpPlaneSectorChunkInfo.class);
        this.value = chunkInfo.getValue();
        this.leftValue = chunkInfo.getLeftValue();
        this.rightValue = chunkInfo.getRightValue();

        if (chunkInfo.isLeftWorldSector()) {
            this.leftSector = readChunk(reader, RwAtomicSectorChunk.class);
        } else {
            this.leftSector = readChunk(reader, RwPlaneSectorChunk.class);
        }

        if (chunkInfo.isRightWorldSector()) {
            this.rightSector = readChunk(reader, RwAtomicSectorChunk.class);
        } else {
            this.rightSector = readChunk(reader, RwPlaneSectorChunk.class);
        }
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeStruct(writer, new RpPlaneSectorChunkInfo(this));
        writeChunk(writer, this.leftSector);
        writeChunk(writer, this.rightSector);
    }

    @Override
    public boolean isWorldSector() {
        return false;
    }

    // TODO: Display info.

    @Getter
    public static class RpPlaneSectorChunkInfo extends RwStruct {
        private int sectorType;
        private float value;
        private boolean leftWorldSector; // Left node contains less of the plane.
        private boolean rightWorldSector; // Right node contains more of the plane.
        private float leftValue;
        private float rightValue;

        public RpPlaneSectorChunkInfo(GameInstance instance) {
            super(instance, RwStructType.PLANE_SECTOR_CHUNK_INFO);
        }

        public RpPlaneSectorChunkInfo(RwPlaneSectorChunk chunk) {
            this(chunk.getGameInstance());
            // TODO: ? this.sectorType = ?;
            this.value = chunk.getValue();
            this.leftValue = chunk.getLeftValue();
            this.rightValue = chunk.getRightValue();
            this.leftWorldSector = chunk.getLeftSector().isWorldSector();
            this.rightWorldSector = chunk.getRightSector().isWorldSector();
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.sectorType = reader.readInt(); // 8? 4? 0?
            this.value = reader.readFloat(); // MIDDLE NUMBER? What is this?
            this.leftWorldSector = RwUtils.readRwBool(reader);
            this.rightWorldSector = RwUtils.readRwBool(reader);
            this.leftValue = reader.readFloat(); // LARGER NUMBER. IF EITHER IS ATOMIC, IT IS EQUAL TO THE CURRENT VALUE
            this.rightValue = reader.readFloat(); // SMALLER NUMBER. IF EITHER IS ATOMIC, IT IS EQUAL TO THE CURRENT VALUE
        }

        @Override
        public void save(DataWriter writer, int version) {
            writer.writeInt(this.sectorType);
            writer.writeFloat(this.value);
            RwUtils.writeRwBool(writer, this.leftWorldSector);
            RwUtils.writeRwBool(writer, this.rightWorldSector);
            writer.writeFloat(this.leftValue);
            writer.writeFloat(this.rightValue);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Sector Type", this.sectorType);
            propertyList.add("Value", this.value);
            propertyList.add("IsLeftWorldSector?", this.leftWorldSector);
            propertyList.add("Left Value", this.leftValue);
            propertyList.add("IsRightWorldSector?", this.rightWorldSector);
            propertyList.add("Right Value", this.rightValue);
            return propertyList;
        }

        @Override
        public String toString() {
            return "RpPlaneSectorChunkInfo{sectorType=" + this.sectorType + ",value=" + this.value
                    + "IsLeftWorldSector=" + this.leftWorldSector + ",leftValue=" + this.leftValue
                    + "IsRightWorldSector=" + this.rightWorldSector + ",rightValue=" + this.rightValue + "}";
        }
    }
}