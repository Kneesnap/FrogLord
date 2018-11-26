package net.highwayfrogs.editor.file.map.entity.data.general;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
public class CheckpointEntity extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private int id;

    @Override
    public void load(DataReader reader) {
        matrix.load(reader);
        this.id = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        matrix.save(writer);
        writer.writeInt(this.id);
    }
}
