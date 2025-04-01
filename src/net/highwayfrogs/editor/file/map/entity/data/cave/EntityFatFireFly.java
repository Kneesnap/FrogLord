package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;

/**
 * Holds data for the cave bug which completely lights up the cave.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityFatFireFly extends MatrixData {
    private FroggerFlyScoreType type = FroggerFlyScoreType.SUPER_LIGHT; // Unused. Change has no effect.
    private SVector target = new SVector(); // Unused, change has no effect. At one point this was the position to move the camera to upon eating.

    public EntityFatFireFly(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = FroggerFlyScoreType.values()[reader.readUnsignedShortAsInt()];
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