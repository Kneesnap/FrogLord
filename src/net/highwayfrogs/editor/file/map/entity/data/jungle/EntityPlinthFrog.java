package net.highwayfrogs.editor.file.map.entity.data.jungle;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixEntity;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * The frog on the plinth.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class EntityPlinthFrog extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private int plinthId;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.plinthId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeInt(this.plinthId);
    }
}
