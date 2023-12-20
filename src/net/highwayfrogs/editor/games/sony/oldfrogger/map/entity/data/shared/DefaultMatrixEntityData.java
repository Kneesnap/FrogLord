package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared;

import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerNullDifficultyData;

/**
 * Represents old frogger entity data which contains a matrix position, and no difficulty data.
 * Created by Kneesnap on 12/15/2023.
 */
public class DefaultMatrixEntityData extends MatrixEntityData<OldFroggerNullDifficultyData> {
    public DefaultMatrixEntityData(OldFroggerMapEntity entity) {
        super(entity, null);
    }
}