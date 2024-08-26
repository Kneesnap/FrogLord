package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.games.renderware.struct.types.RwV3d;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Implements a matrix as a stream chunk.
 * Created by Kneesnap on 8/25/2024.
 */
@Getter
public class RwStreamMatrixChunk extends RwStreamChunk {
    private final RwStreamMatrix matrix;

    public RwStreamMatrixChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.MATRIX, version, parentChunk);
        this.matrix = new RwStreamMatrix(getGameInstance());
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        readStruct(reader, this.matrix, false);
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeStruct(writer, this.matrix, false);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList = this.matrix.addToPropertyList(propertyList);
        return propertyList;
    }

    /**
     * Defined in babinmtx.h
     */
    @Getter
    public static class RwStreamMatrix extends RwStruct {
        private final RwV3d right;
        private final RwV3d up;
        private final RwV3d at;
        private final RwV3d pos;
        private int type;

        public RwStreamMatrix(GameInstance instance) {
            super(instance, RwStructType.MATRIX);
            this.right = new RwV3d(instance);
            this.up = new RwV3d(instance);
            this.at = new RwV3d(instance);
            this.pos = new RwV3d(instance);
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.right.load(reader, version, byteLength);
            this.up.load(reader, version, byteLength);
            this.at.load(reader, version, byteLength);
            this.pos.load(reader, version, byteLength);
            this.type = reader.readInt();
        }

        @Override
        public void save(DataWriter writer, int version) {
            this.right.save(writer, version);
            this.up.save(writer, version);
            this.at.save(writer, version);
            this.pos.save(writer, version);
            writer.writeInt(this.type);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Type", this.type);
            propertyList.add("Position", this.at);
            propertyList.add("At", this.pos);
            propertyList.add("Right", this.right);
            propertyList.add("Up", this.up);
            return propertyList;
        }

        @Override
        public String toString() {
            return "RwStreamMatrix{type=" + this.type + ",pos=" + this.pos + ",at=" + this.at
                    + ",right=" + this.right + ",up=" + this.up + "}";
        }
    }
}