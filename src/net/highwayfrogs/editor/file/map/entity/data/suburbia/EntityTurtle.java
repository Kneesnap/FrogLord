package net.highwayfrogs.editor.file.map.entity.data.suburbia;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the "SUBURBIA_TURTLE" struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityTurtle extends GameObject {
    private PathInfo pathInfo = new PathInfo();
    private int diveDelay;
    private int riseDelay;
    private int turtleType;

    public static final int TYPE_DIVING = 0;
    public static final int TYPE_NOT_DIVING = 1;

    @Override
    public void load(DataReader reader) {
        this.pathInfo.load(reader);
        this.diveDelay = reader.readInt();
        this.riseDelay = reader.readInt();
        this.turtleType = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.pathInfo.save(writer);
        writer.writeInt(this.diveDelay);
        writer.writeInt(this.riseDelay);
        writer.writeInt(this.turtleType);
    }
}
