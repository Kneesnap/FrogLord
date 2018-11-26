package net.highwayfrogs.editor.file.map.entity.data.suburbia;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityTurtle extends GameObject {
    private PathInfo pathInfo = new PathInfo();
    private short diveDelay;
    private short riseDelay;
    private short diveSpeed;
    private short riseSpeed;

    @Override
    public void load(DataReader reader) {
        this.pathInfo.load(reader);
        this.diveDelay = reader.readShort();
        this.riseDelay = reader.readShort();
        this.diveSpeed = reader.readShort();
        this.riseSpeed = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.pathInfo.save(writer);
        writer.writeShort(this.diveDelay);
        writer.writeShort(this.riseDelay);
        writer.writeShort(this.diveSpeed);
        writer.writeShort(this.riseSpeed);
    }
}
