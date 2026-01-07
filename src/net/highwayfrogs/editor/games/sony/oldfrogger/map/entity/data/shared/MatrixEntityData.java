package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.function.Function;

/**
 * Represents old frogger entity data with a static matrix holding position/rotation.
 * The tunnel in ORIGINAL.MAP is not rotated correctly in editor, but it shows up correctly in-game.
 * After looking at the raw matrix rotation data, the math all seems correct.
 * Most likely there is code in-game which forces the rotation to be correct.
 * Created by Kneesnap on 12/11/2023.
 */
@Getter
public class MatrixEntityData<TDifficultyData extends OldFroggerDifficultyData> extends OldFroggerEntityData<TDifficultyData> {
    private final PSXMatrix matrix = new PSXMatrix();

    public MatrixEntityData(OldFroggerMapEntity entity, Function<OldFroggerMapEntity, TDifficultyData> difficultyDataMaker) {
        super(entity, difficultyDataMaker);
    }

    @Override
    protected void loadMainEntityData(DataReader reader) {
        this.matrix.load(reader);
    }

    @Override
    protected void saveMainEntityData(DataWriter writer) {
        this.matrix.save(writer);
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager manager, GUIEditorGrid editor) {
        editor.addMeshMatrix(this.matrix, manager.getController(), () -> manager.updateEntityPositionRotation(getEntity()), true, null);
    }

    @Override
    public float[] getPosition(float[] position) {
        int[] pos = this.matrix.getTransform();
        position[0] = DataUtils.fixedPointIntToFloat4Bit(pos[0]);
        position[1] = DataUtils.fixedPointIntToFloat4Bit(pos[1]);
        position[2] = DataUtils.fixedPointIntToFloat4Bit(pos[2]);
        position[3] = (float) this.matrix.getPitchAngle();
        position[4] = (float) this.matrix.getYawAngle();
        position[5] = (float) this.matrix.getRollAngle();
        return position;
    }
}