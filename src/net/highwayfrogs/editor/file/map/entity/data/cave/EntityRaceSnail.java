package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.PathEntity;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityRaceSnail extends GameObject implements PathEntity {
    private PathInfo pathInfo = new PathInfo();
    private int forwardDistance;
    private int backwardDistance;

    @Override
    public void load(DataReader reader) {
        this.pathInfo.load(reader);
        this.forwardDistance = reader.readUnsignedShortAsInt();
        this.backwardDistance = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.pathInfo.save(writer);
        writer.writeUnsignedShort(this.forwardDistance);
        writer.writeUnsignedShort(this.backwardDistance);
    }
}
