package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the PATH_INFO struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class PathInfo extends GameObject {
    private int pathId;
    private int segmentId;
    private int segmentDistance;
    private boolean repeat;
    private int speed;

    public static final int MOTION_TYPE_REPEAT = Constants.BIT_FLAG_3;

    @Override
    public void load(DataReader reader) {
        this.pathId = reader.readUnsignedShortAsInt();
        this.segmentId = reader.readUnsignedShortAsInt();
        this.segmentDistance = reader.readUnsignedShortAsInt();

        int motionType = reader.readUnsignedShortAsInt();
        if (motionType != MOTION_TYPE_REPEAT && motionType != 0)
            throw new RuntimeException("PathInfo had MotionType: " + motionType + ", which was not understood.");
        this.repeat = (motionType == MOTION_TYPE_REPEAT);

        this.speed = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.pathId);
        writer.writeUnsignedShort(this.segmentId);
        writer.writeUnsignedShort(this.segmentDistance);
        writer.writeUnsignedShort(this.repeat ? MOTION_TYPE_REPEAT : 0);
        writer.writeUnsignedShort(this.speed);
        writer.writeUnsignedShort(0);
    }
}
