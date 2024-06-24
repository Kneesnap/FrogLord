package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Base entity data which holds path data.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
@Setter
public class FroggerEntityDataPathInfo extends FroggerEntityData {
    private FroggerPathInfo pathInfo;
    private static final IVector GAME_Y_AXIS_POS = new IVector(0, 0x1000, 0);

    public FroggerEntityDataPathInfo(FroggerMapFile mapFile) {
        super(mapFile);
        this.pathInfo = new FroggerPathInfo(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.pathInfo.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.pathInfo.save(writer);
    }

    @Override
    public float[] getPositionAndRotation(float[] position) {
        FroggerPathInfo pathInfo = getPathInfo();
        if (pathInfo == null)
            return null; // No path state.

        FroggerPath path = pathInfo.getPath();
        if (path == null)
            return null; // Invalid path id.

        FroggerPathResult result = path.evaluatePosition(pathInfo);

        IVector vec_x = new IVector();
        IVector vec_y = new IVector();
        IVector vec_z = result.getRotation();
        IVector.MROuterProduct12(GAME_Y_AXIS_POS, vec_z, vec_x);
        vec_x.normalise();
        IVector.MROuterProduct12(vec_z, vec_x, vec_y);
        PSXMatrix matrix = PSXMatrix.WriteAxesAsMatrix(new PSXMatrix(), vec_x, vec_y, vec_z);

        Vector endVec = result.getPosition();
        position[0] = endVec.getFloatX();
        position[1] = endVec.getFloatY();
        position[2] = endVec.getFloatZ();
        position[3] = (float) matrix.getRollAngle();
        position[4] = (float) matrix.getPitchAngle();
        position[5] = (float) matrix.getYawAngle();
        return position;
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        // Do nothing.
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        this.pathInfo.setupEditor(manager, this, editor);
        super.setupEditor(editor, manager); // Path ID comes before the rest.
    }
}