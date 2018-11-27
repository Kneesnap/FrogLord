package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the PATH_INFO struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
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
        reader.readShort();
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
}
