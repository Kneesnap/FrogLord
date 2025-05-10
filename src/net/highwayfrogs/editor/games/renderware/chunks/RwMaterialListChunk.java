package net.highwayfrogs.editor.games.renderware.chunks;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.RwStreamChunk;
import net.highwayfrogs.editor.games.renderware.RwStreamChunkType;
import net.highwayfrogs.editor.games.renderware.RwStreamFile;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the material list chunk.
 * Created by Kneesnap on 8/16/2024.
 */
@Getter
public class RwMaterialListChunk extends RwStreamChunk {
    private final List<RwMaterialChunk> materials = new ArrayList<>();

    public RwMaterialListChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.MATERIAL_LIST, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        RwMaterialListStruct value = readStruct(reader, RwMaterialListStruct.class);

        this.materials.clear();
        for (int i = 0; i < value.getMaterialCount(); i++) {
            if (value.getMaterialIndices()[i] >= 0) {
                this.materials.add(this.materials.get(value.getMaterialIndices()[i]));
                continue;
            }

            this.materials.add(readChunk(reader, RwMaterialChunk.class));
        }
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        RwMaterialListStruct rwMaterialListStruct = new RwMaterialListStruct(this);
        writeStruct(writer, rwMaterialListStruct);
        for (int i = 0; i < this.materials.size(); i++) {
            if (rwMaterialListStruct.getMaterialIndices()[i] >= 0)
                continue;

            writeChunk(writer, this.materials.get(i));
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Material Count", this.materials.size());
        return propertyList;
    }

    /**
     * There's no name for this struct.
     */
    @Getter
    public static class RwMaterialListStruct extends RwStruct {
        private int materialCount;
        private int[] materialIndices;

        public RwMaterialListStruct(GameInstance instance) {
            super(instance, RwStructType.MATERIAL_LIST_STRUCT);
        }

        public RwMaterialListStruct(RwMaterialListChunk chunk) {
            this(chunk.getGameInstance());
            this.materialCount = chunk.getMaterials().size();
            this.materialIndices = new int[this.materialCount];
            for (int i = 0; i < this.materialIndices.length; i++) {
                int j = i;
                while (j-- > 0)
                    if (chunk.getMaterials().get(j) == chunk.getMaterials().get(i))
                        break;

                this.materialIndices[i] = j;
            }
        }

        @Override
        public void load(DataReader reader, int version, int byteLength) {
            this.materialCount = reader.readInt();
            this.materialIndices = new int[this.materialCount];
            for (int i = 0; i < this.materialIndices.length; i++)
                this.materialIndices[i] = reader.readInt();
        }

        @Override
        public void save(DataWriter writer, int version) {
            writer.writeInt(this.materialCount);
            for (int i = 0; i < this.materialIndices.length; i++)
                writer.writeInt(this.materialIndices[i]);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            propertyList.add("Material Count", this.materialCount);
            for (int i = 0; i < this.materialIndices.length; i++)
                propertyList.add("Material Index " + i, this.materialIndices[i]);
            return propertyList;
        }
    }
}