package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.FlyScoreType;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Completely lights up the cave.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityFatFireFly extends MatrixData {
    private FlyScoreType type = FlyScoreType.SUPER_LIGHT; // Unused. Change has no effect.
    private SVector target = new SVector(); // Unused, change has no effect. At one point this was the position to move the camera to upon eating.

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = FlyScoreType.values()[reader.readUnsignedShortAsInt()];
        reader.skipShort();
        this.target = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.type.ordinal());
        writer.writeUnsignedShort(0);
        this.target.saveWithPadding(writer);
    }
}
