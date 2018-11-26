package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntitySpider extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private int speed;

    @Override
    public void load(DataReader reader) {
        matrix.load(reader);
        speed = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        matrix.save(writer);
        writer.writeUnsignedShort(speed);
        writer.writeUnsignedShort(0);
    }
}
