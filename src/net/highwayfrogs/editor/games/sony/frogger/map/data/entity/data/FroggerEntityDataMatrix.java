package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Entity data which involves a matrix.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
public class FroggerEntityDataMatrix extends FroggerEntityData {
    private final PSXMatrix matrix = new PSXMatrix();

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
        position[0] = Utils.fixedPointIntToFloat4Bit(pos[0]);
        position[1] = Utils.fixedPointIntToFloat4Bit(pos[1]);
        position[2] = Utils.fixedPointIntToFloat4Bit(pos[2]);
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
}