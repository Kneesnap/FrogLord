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
 * Points are objects that have a position and orientation.
 * They can be attached to bone joints for animation purposes.
 * Points do not affect model geometry in any way.
 * They are simply reference objects for specifying a location in the model.
 * One potential use for this is bolt points for attaching one model to another (such as tags in MD3 models).
 * Version: 1.6+
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMPointsBlock extends MMDataBlockBody {
    @Setter private short flags;
    private byte[] name = new byte[NAME_BYTE_LENGTH];
    @Setter private int type; // Should be zero.
    @Setter private int jointIndex; // Index of parent joint.
    @Setter private float rotX; // NOTE: In radians!
    @Setter private float rotY; // NOTE: In radians!
    @Setter private float rotZ; // NOTE: In radians!
    @Setter private float translationX;
    @Setter private float translationY;
    @Setter private float translationZ;

    private static final int NAME_BYTE_LENGTH = 40;
    public static final short FLAG_HIDDEN = Constants.BIT_FLAG_0; // Set if hidden, clear if visible
    public static final short FLAG_SELECTED = Constants.BIT_FLAG_1; // Set if selected, clear if unselected

    public MMPointsBlock(MisfitModel3DObject parent) {
        super(OffsetType.POINTS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        reader.readBytes(this.name);
        this.type = reader.readInt();
        this.jointIndex = reader.readInt();
        this.rotX = reader.readFloat();
        this.rotY = reader.readFloat();
        this.rotZ = reader.readFloat();
        this.translationX = reader.readFloat();
        this.translationY = reader.readFloat();
        this.translationZ = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeBytes(this.name);
        writer.writeInt(this.type);
        writer.writeInt(this.jointIndex);
        writer.writeFloat(this.rotX);
        writer.writeFloat(this.rotY);
        writer.writeFloat(this.rotZ);
        writer.writeFloat(this.translationX);
        writer.writeFloat(this.translationY);
        writer.writeFloat(this.translationZ);
    }

    /**
     * Sets the name of this joint.
     * If a byte length of more than 40 is specified, an error will be thrown.
     * @param name The new name for this point.
     */
    public void setName(String name) {
        byte[] newBytes = name.getBytes(StandardCharsets.US_ASCII);
        if (newBytes.length > NAME_BYTE_LENGTH)
            throw new RuntimeException("Point names cannot exceed a length of " + NAME_BYTE_LENGTH + " bytes.");

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
