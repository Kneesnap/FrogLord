package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixEntity;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents "FOREST_BREAKING_BRANCH".
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class BreakingBranchEntity extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private int breakDelay;
    private int fallSpeed;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.breakDelay = reader.readUnsignedShortAsInt();
        this.fallSpeed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeUnsignedShort(this.breakDelay);
        writer.writeUnsignedShort(this.fallSpeed);
    }
}
