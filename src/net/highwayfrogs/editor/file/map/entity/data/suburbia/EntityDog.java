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
public class EntityDog extends GameObject {
    private PathInfo pathInfo = new PathInfo();
    private int waitDelay;

    @Override
    public void load(DataReader reader) {
        this.pathInfo.load(reader);
        this.waitDelay = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.pathInfo.save(writer);
        writer.writeInt(this.waitDelay);
    }
}
