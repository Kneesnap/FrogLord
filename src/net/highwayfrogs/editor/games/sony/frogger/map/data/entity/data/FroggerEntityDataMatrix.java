package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Entity data which involves a matrix.
 * Created by Kneesnap on 1/20/2019.
 */
public class FroggerEntityDataMatrix extends FroggerEntityData {
    @Getter private final PSXMatrix matrix = new PSXMatrix();
    private final float[] cachedPosition = new float[6];

    public FroggerEntityDataMatrix(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        // Still overwritten by children.
    }

    @Override
    public float[] getPositionAndRotation(float[] position) {
        int[] pos = this.matrix.getTransform();
        position[0] = DataUtils.fixedPointIntToFloat4Bit(pos[0]);
        position[1] = DataUtils.fixedPointIntToFloat4Bit(pos[1]);
        position[2] = DataUtils.fixedPointIntToFloat4Bit(pos[2]);
        position[3] = (float) this.matrix.getPitchAngle();
        position[4] = (float) this.matrix.getYawAngle();
        position[5] = (float) this.matrix.getRollAngle();
        return position;
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addMeshMatrix(this.matrix, manager.getController(), () -> manager.updateEntityPositionRotation(getParentEntity()), true);
        super.setupEditor(editor, manager);
    }

    /**
     * Copies pathing data from the old data.
     * @param oldMatrixData the old matrix data to copy from
     * @param oldPathData the old pathing data to copy from
     */
    public void copyFrom(FroggerEntityDataMatrix oldMatrixData, FroggerEntityDataPathInfo oldPathData) {
        if (oldMatrixData != null) {
            this.matrix.copyFrom(oldMatrixData.getMatrix());
        } else if (oldPathData != null) {
            float[] positionData = oldPathData.getPositionAndRotation(this.cachedPosition);
            if (positionData != null)
                applyPositionData(positionData);
        }
    }

    /**
     * Applies the position data from a position array to the matrix
     * @param positionData the positional data to apply
     */
    public void applyPositionData(float[] positionData) {
        if (positionData == null)
            throw new NullPointerException("positionData");
        if (positionData.length != 6)
            throw new IllegalArgumentException("Invalid position data length: " + positionData.length + "!");

        this.matrix.getTransform()[0] = DataUtils.floatToFixedPointInt4Bit(positionData[0]);
        this.matrix.getTransform()[1] = DataUtils.floatToFixedPointInt4Bit(positionData[1]);
        this.matrix.getTransform()[2] = DataUtils.floatToFixedPointInt4Bit(positionData[2]);
        this.matrix.updateMatrix(positionData[3], positionData[4], positionData[5]);
    }
}