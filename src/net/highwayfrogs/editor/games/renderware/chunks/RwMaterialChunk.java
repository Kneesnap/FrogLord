package net.highwayfrogs.editor.games.renderware.chunks;

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
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Represents an RwMaterial.
 * Created by Kneesnap on 8/16/2024.
 */
@Getter
public class RwMaterialChunk extends RwStreamChunk {
    private RpMaterialChunkInfo materialInfo;
    private RwTextureChunk texture;

    public RwMaterialChunk(RwStreamFile streamFile, int version, RwStreamChunk parentChunk) {
        super(streamFile, RwStreamChunkType.MATERIAL, version, parentChunk);
    }

    @Override
    protected void loadChunkData(DataReader reader, int dataLength, int version) {
        this.materialInfo = readStruct(reader, RpMaterialChunkInfo.class);
        this.texture = this.materialInfo.isTextured() ? readChunk(reader, RwTextureChunk.class) : null;
        readOptionalExtensionData(reader);
    }

    @Override
    protected void saveChunkData(DataWriter writer) {
        writeStruct(writer, this.materialInfo);
        if (this.materialInfo.isTextured())
            writeChunk(writer, this.texture);
        writeOptionalExtensionData(writer);
    }

    @Override
    public String getCollectionViewDisplayName() {
        String materialName = this.texture != null ? this.texture.getName() : null;
        return super.getCollectionViewDisplayName() + (materialName != null && materialName.trim().length() > 0 ? " [" + materialName + "]" : "");
    }

    @Getter
    public static class RpMaterialChunkInfo extends RwStruct {
        private int flags; // Unused?
        private int color;
        private int unused;
        private boolean textured;
        private final RwSurfaceProperties surfaceProperties;

        public RpMaterialChunkInfo(GameInstance instance) {
            super(instance, RwStructType.MATERIAL_CHUNK_INFO);
            this.surfaceProperties = new RwSurfaceProperties(instance);
        }

        @Override
        public void load(DataReader reader, int byteLength, int version) {
            this.flags = reader.readInt();
            this.color = reader.readInt();
            this.unused = reader.readInt();
            this.textured = RwUtils.readRwBool(reader);
            this.surfaceProperties.load(reader); // Omitted in older versions.
        }

        @Override
        public void save(DataWriter writer, int version) {
            writer.writeInt(this.flags);
            writer.writeInt(this.color);
            writer.writeInt(this.unused);
            RwUtils.writeRwBool(writer, this.textured);
            this.surfaceProperties.save(writer);
        }

        @Override
        public PropertyList addToPropertyList(PropertyList propertyList) {
            propertyList = super.addToPropertyList(propertyList);
            if (this.flags != 0)
                propertyList.add("Flags", NumberUtils.toHexString(this.flags));
            propertyList.add("Color", this.color);
            if (this.unused != 0)
                propertyList.add("Unused (Garbage?)", this.unused);
            propertyList.add("Textured", this.textured);
            propertyList.add("Surface Properties", this.surfaceProperties);
            return propertyList;
        }
    }

    @Getter
    public static class RwSurfaceProperties extends SharedGameData {
        private float ambient = 1F;
        private float specular = 1F;
        private float diffuse = 1F;

        public RwSurfaceProperties(GameInstance instance) {
            super(instance);
        }

        @Override
        public void load(DataReader reader) {
            this.ambient = reader.readFloat();
            this.specular = reader.readFloat();
            this.diffuse = reader.readFloat();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeFloat(this.ambient);
            writer.writeFloat(this.specular);
            writer.writeFloat(this.diffuse);
        }

        @Override
        public String toString() {
            return "RwSurfaceProperties{ambient=" + this.ambient + ",specular=" + this.specular + ",diffuse=" + this.diffuse + "}";
        }
    }
}