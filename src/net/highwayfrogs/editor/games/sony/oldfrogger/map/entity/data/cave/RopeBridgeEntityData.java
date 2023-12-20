package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerNullDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared.MatrixEntityData;

/**
 * Represents entity data for the cave rope bridge entity.
 * Created by Kneesnap on 12/17/2023.
 */
@Getter
public class RopeBridgeEntityData extends MatrixEntityData<OldFroggerNullDifficultyData> {
    public RopeBridgeEntityData(OldFroggerMapEntity entity) {
        super(entity, null);
    }

    @Override
    public void loadMainEntityData(DataReader reader) {
        super.loadMainEntityData(reader);
        reader.skipBytesRequireEmpty(4); // Padding.
    }

    @Override
    public void saveMainEntityData(DataWriter writer) {
        super.saveMainEntityData(writer);
        writer.writeInt(0); // Padding
    }
}