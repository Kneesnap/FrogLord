package net.highwayfrogs.editor.file.map.entity.data.retro;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.PathEntity;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityBeaver extends GameObject implements PathEntity {
    private PathInfo pathInfo = new PathInfo();
    private short delay;

    @Override
    public void load(DataReader reader) {
        this.pathInfo.load(reader);
        this.delay = reader.readShort();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.pathInfo.save(writer);
        writer.writeShort(this.delay);
        writer.writeUnsignedShort(0);
    }
}
