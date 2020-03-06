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
 * Represents the joints block.
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
public class MMJointsBlock extends MMDataBlockBody {
    @Setter private short flags;
    private byte[] name = new byte[40];
    @Setter private long parentJointIndex;
    @Setter private float xRotation;
    @Setter private float yRotation;
    @Setter private float zRotation;
    @Setter private float xTranslation;
    @Setter private float yTranslation;
    @Setter private float zTranslation;

    public static final int FLAG_HIDDEN = Constants.BIT_FLAG_0;
    public static final int FLAG_SELECTED = Constants.BIT_FLAG_1;

    public MMJointsBlock(MisfitModel3DObject parent) {
        super(OffsetType.JOINTS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.name = reader.readBytes(40);
        this.parentJointIndex = reader.readUnsignedIntAsLong();
        this.xRotation = reader.readFloat();
        this.yRotation = reader.readFloat();
        this.zRotation = reader.readFloat();
        this.xTranslation = reader.readFloat();
        this.yTranslation = reader.readFloat();
        this.zTranslation = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeBytes(this.name);
        writer.writeUnsignedInt(this.parentJointIndex);
        writer.writeFloat(this.xRotation);
        writer.writeFloat(this.yRotation);
        writer.writeFloat(this.zRotation);
        writer.writeFloat(this.xTranslation);
        writer.writeFloat(this.yTranslation);
        writer.writeFloat(this.zTranslation);
    }

    /**
     * Sets the name of this joint.
     * If a byte length of more than 40 is specified, an error will be thrown.
     * @param name The new name for this joint.
     */
    public void setName(String name) {
        byte[] newBytes = name.getBytes(StandardCharsets.US_ASCII);
        if (newBytes.length > this.name.length)
            throw new RuntimeException("Joint names cannot exceed a length of 40 bytes.");

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
}
