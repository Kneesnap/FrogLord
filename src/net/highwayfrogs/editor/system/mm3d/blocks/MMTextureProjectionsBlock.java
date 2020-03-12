package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents the texture projections block.
 * Version: 1.6+
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
public class MMTextureProjectionsBlock extends MMDataBlockBody {
    @Setter private short flags;
    private byte[] name = new byte[NAME_BYTE_LENGTH];
    @Setter private MMProjectionType projectionType;
    @Setter private float centerPosX; // Projection Center.
    @Setter private float centerPosY;
    @Setter private float centerPosZ;
    @Setter private float upVectorX;
    @Setter private float upVectorY;
    @Setter private float upVectorZ;
    @Setter private float seamVectorX;
    @Setter private float seamVectorY;
    @Setter private float seamVectorZ;
    @Setter private float minU;
    @Setter private float minV;
    @Setter private float maxU;
    @Setter private float maxV;

    private static final int NAME_BYTE_LENGTH = 40;

    public MMTextureProjectionsBlock(MisfitModel3DObject parent) {
        super(OffsetType.TEXTURE_PROJECTIONS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        reader.readBytes(this.name);
        this.projectionType = MMProjectionType.values()[reader.readInt()];
        this.centerPosX = reader.readFloat();
        this.centerPosY = reader.readFloat();
        this.centerPosZ = reader.readFloat();
        this.upVectorX = reader.readFloat();
        this.upVectorY = reader.readFloat();
        this.upVectorZ = reader.readFloat();
        this.seamVectorX = reader.readFloat();
        this.seamVectorY = reader.readFloat();
        this.seamVectorZ = reader.readFloat();
        this.minU = reader.readFloat();
        this.minV = reader.readFloat();
        this.maxU = reader.readFloat();
        this.maxV = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeBytes(this.name);
        writer.writeInt(this.projectionType.ordinal());
        writer.writeFloat(this.centerPosX);
        writer.writeFloat(this.centerPosY);
        writer.writeFloat(this.centerPosZ);
        writer.writeFloat(this.upVectorX);
        writer.writeFloat(this.upVectorY);
        writer.writeFloat(this.upVectorZ);
        writer.writeFloat(this.seamVectorX);
        writer.writeFloat(this.seamVectorY);
        writer.writeFloat(this.seamVectorZ);
        writer.writeFloat(this.minU);
        writer.writeFloat(this.minV);
        writer.writeFloat(this.maxU);
        writer.writeFloat(this.maxV);
    }

    /**
     * Sets the name of this joint.
     * If a byte length of more than 40 is specified, an error will be thrown.
     * @param name The new name for this joint.
     */
    public void setName(String name) {
        byte[] newBytes = name.getBytes(StandardCharsets.US_ASCII);
        if (newBytes.length > NAME_BYTE_LENGTH)
            throw new RuntimeException("Texture Projection names cannot exceed a length of " + NAME_BYTE_LENGTH + " bytes.");

        Arrays.fill(this.name, Constants.NULL_BYTE);
        System.arraycopy(newBytes, 0, this.name, 0, newBytes.length);
    }

    /**
     * Gets the name of this joint.
     */
    public String getName() {
        int findIndex = -1;
        for (int i = 0; i < this.name.length; i++) {
            if (this.name[i] == Constants.NULL_BYTE) {
                findIndex = i;
                break;
            }
        }

        return findIndex != -1 ? new String(Arrays.copyOfRange(this.name, 0, findIndex)) : new String(this.name);
    }

    public enum MMProjectionType {
        CYLINDER, SPHERE
    }
}
