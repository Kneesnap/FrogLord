package net.highwayfrogs.editor.file.map.path;

import lombok.AllArgsConstructor;
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
    private int motionType;
    private int speed;

    @Override
    public void load(DataReader reader) {
        this.pathId = reader.readUnsignedShortAsInt();
        this.segmentId = reader.readUnsignedShortAsInt();
        this.segmentDistance = reader.readUnsignedShortAsInt();
        this.motionType = reader.readUnsignedShortAsInt();
        this.speed = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.pathId);
        writer.writeUnsignedShort(this.segmentId);
        writer.writeUnsignedShort(this.segmentDistance);
        writer.writeUnsignedShort(this.motionType);
        writer.writeUnsignedShort(this.speed);
        writer.writeUnsignedShort(0);
    }

    /**
     * Test if a flag is present.
     * @param type The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(PathMotionType type) {
        return (this.motionType & type.getFlag()) == type.getFlag();
    }

    /**
     * Set the flag state.
     * @param flag     The flag type.
     * @param newState The new state of the flag.
     */
    public void setFlag(PathMotionType flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.motionType |= flag.getFlag();
        } else {
            this.motionType ^= flag.getFlag();
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PathMotionType {
        ACTIVE(Constants.BIT_FLAG_0),
        BACKWARDS(Constants.BIT_FLAG_1),
        ONE_SHOT(Constants.BIT_FLAG_2),
        REPEAT(Constants.BIT_FLAG_3),
        FINISHED(Constants.BIT_FLAG_4);

        private final int flag;
    }
}
