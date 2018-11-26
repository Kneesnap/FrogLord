package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityHedgehog extends GameObject {
    private PathInfo pathInfo = new PathInfo();
    private int runTime;
    private int rollTime;
    private int runSpeed;
    private int rollSpeed;

    @Override
    public void load(DataReader reader) {
        pathInfo.load(reader);
        this.runTime = reader.readUnsignedShortAsInt();
        this.rollTime = reader.readUnsignedShortAsInt();
        this.runSpeed = reader.readUnsignedShortAsInt();
        this.rollSpeed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        pathInfo.save(writer);
        writer.writeUnsignedShort(this.runTime);
        writer.writeUnsignedShort(this.rollTime);
        writer.writeUnsignedShort(this.runSpeed);
        writer.writeUnsignedShort(this.rollSpeed);
    }
}
