package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

import java.nio.charset.StandardCharsets;

/**
 * Represents the joints block.
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
public class MMJointsBlock extends MMDataBlockBody {
    @Setter private short flags;
    private String name = "";
    @Setter private int parentJointIndex = -1; // -1 = Not attached.
    @Setter private float xRotation;
    @Setter private float yRotation;
    @Setter private float zRotation;
    @Setter private float xTranslation;
    @Setter private float yTranslation;
    @Setter private float zTranslation;

    private static final int NAME_BYTE_LENGTH = 40;
    public static final short FLAG_HIDDEN = Constants.BIT_FLAG_0;
    public static final short FLAG_SELECTED = Constants.BIT_FLAG_1;

    public MMJointsBlock(MisfitModel3DObject parent) {
        super(OffsetType.JOINTS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.name = reader.readNullTerminatedFixedSizeString(NAME_BYTE_LENGTH);
        this.parentJointIndex = reader.readInt();
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
        writer.writeNullTerminatedFixedSizeString(this.name, NAME_BYTE_LENGTH);
        writer.writeInt(this.parentJointIndex);
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
     * @param newName The new name for this joint.
     */
    public void setName(String newName) {
        byte[] newBytes = newName.getBytes(StandardCharsets.US_ASCII);
        if (newBytes.length > NAME_BYTE_LENGTH)
            throw new RuntimeException("Joint names cannot exceed a length of " + NAME_BYTE_LENGTH + " bytes.");

        this.name = newName;
    }
}
