package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerFlyScoreType;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Includes the identifier which identifies a fly.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class BonusFlyEntity extends MatrixData {
    private int flyTypeId;

    public BonusFlyEntity(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.flyTypeId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.flyTypeId);
    }

    public FroggerFlyScoreType getFlyType() {
        return (this.flyTypeId >= 0 && this.flyTypeId < FroggerFlyScoreType.values().length)
                ? FroggerFlyScoreType.values()[this.flyTypeId] : null;
    }
}