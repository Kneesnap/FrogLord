package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;

/**
 * Points are objects that have a position and orientation.
 * They can be attached to bone joints for animation purposes.
 * Points do not affect model geometry in any way.
 * They are simply reference objects for specifying a location in the model.
 * One potential use for this is bolt points for attaching one model to another (such as tags in MD3 models).
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMPointsBlock extends MMDataBlockBody {
    private int flags;
    private byte[] pointName = new byte[40];
    private int type; // Should be zero.
    private int jointIndex; // Index of parent joint.
    private float rotX; // NOTE: In radians!
    private float rotY; // NOTE: In radians!
    private float rotZ; // NOTE: In radians!
    private float translationX;
    private float translationY;
    private float translationZ;

    public static final int FLAG_HIDDEN = Constants.BIT_FLAG_0; // Set if hidden, clear if visible
    public static final int FLAG_SELECTED = Constants.BIT_FLAG_1; // Set if selected, clear if unselected

    public MMPointsBlock(MisfitModel3DObject parent) {
        super(parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        reader.readBytes(this.pointName);
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
        writer.writeUnsignedShort(this.flags);
        writer.writeBytes(this.pointName);
        writer.writeInt(this.type);
        writer.writeInt(this.jointIndex);
        writer.writeFloat(this.rotX);
        writer.writeFloat(this.rotY);
        writer.writeFloat(this.rotZ);
        writer.writeFloat(this.translationX);
        writer.writeFloat(this.translationY);
        writer.writeFloat(this.translationZ);
    }
}
