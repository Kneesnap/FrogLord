package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents MR_COLLPRIM
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
public class MOFCollprim extends GameObject {
    private int type;
    private int flags;
    private SVector offset;
    private int radius2;
    private int xLen;
    private int yLen;
    private int zLen;
    private int user;
    private int hasMatrix;

    public static final int FLAG_STATIC = Constants.BIT_FLAG_0;
    public static final int FLAG_LAST_IN_LIST = Constants.BIT_FLAG_1;
    public static final int FLAG_COLLISION_DISABLED = Constants.BIT_FLAG_8;

    public static final int TYPE_CUBOID = 0;
    public static final int TYPE_CYLINDER_X = 1;
    public static final int TYPE_CYLINDER_Y = 2;
    public static final int TYPE_CYLINDER_Z = 3;
    public static final int TYPE_SPHERE = 4;

    @Override
    public void load(DataReader reader) {
        this.type = reader.readUnsignedShortAsInt();
        this.flags = reader.readUnsignedShortAsInt();
        reader.skipInt(); // Run-time.
        reader.skipInt(); // Run-time.
        this.offset = SVector.readWithPadding(reader);
        this.radius2 = reader.readInt();
        this.xLen = reader.readUnsignedShortAsInt();
        this.yLen = reader.readUnsignedShortAsInt();
        this.zLen = reader.readUnsignedShortAsInt();
        this.user = reader.readUnsignedShortAsInt();
        this.hasMatrix = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.type);
        writer.writeUnsignedShort(this.flags);
        writer.writeInt(0); // Run-time.
        writer.writeInt(0); // Run-time.
        this.offset.saveWithPadding(writer);
        writer.writeInt(this.radius2);
        writer.writeUnsignedShort(this.xLen);
        writer.writeUnsignedShort(this.yLen);
        writer.writeUnsignedShort(this.zLen);
        writer.writeUnsignedShort(this.user);
        writer.writeInt(this.hasMatrix);
    }
}
