package net.highwayfrogs.editor.file.map.entity.data.jungle;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityOutroPlinth extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private int id;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.id = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeInt(this.id);
    }
}
