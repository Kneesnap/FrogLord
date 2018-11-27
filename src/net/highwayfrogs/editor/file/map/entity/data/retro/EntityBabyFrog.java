package net.highwayfrogs.editor.file.map.entity.data.retro;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents "ORG_BABY_FROG_DATA", which is the data for the pink frog you can pickup on the retro logs.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class EntityBabyFrog extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private short logId; // The id of the log this frog will stand on.
    private short awardedPoints; // The points awarded when collected.

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.logId = reader.readShort();
        this.awardedPoints = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeShort(this.logId);
        writer.writeShort(this.awardedPoints);
    }
}
