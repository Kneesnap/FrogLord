package net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.shared;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerDifficultyWrapper.OldFroggerDifficultyData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapPathPacket.OldFroggerMapPath;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerPathData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path.OldFroggerSpline;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

import java.util.function.Function;

/**
 * Represents entity data which follows a path.
 * Created by Kneesnap on 12/14/2023.
 */
@Getter
public class PathEntityData<TDifficultyData extends OldFroggerDifficultyData> extends OldFroggerEntityData<TDifficultyData> {
    private final OldFroggerPathData pathData;

    private static final IVector GAME_Y_AXIS_POS = new IVector(0, 0x1000, 0);

    public PathEntityData(OldFroggerMapEntity entity, Function<OldFroggerMapEntity, TDifficultyData> difficultyDataMaker) {
        super(entity, difficultyDataMaker);
        this.pathData = new OldFroggerPathData(entity.getGameInstance());
    }

    @Override
    public void loadMainEntityData(DataReader reader) {
        this.pathData.load(reader);
    }

    @Override
    public void saveMainEntityData(DataWriter writer) {
        this.pathData.save(writer);
    }

    @Override
    public void setupMainEntityDataEditor(OldFroggerEntityManager controller, GUIEditorGrid editor) {
        this.pathData.setupEditor(controller, getEntity(), editor);
    }

    @Override
    public float[] getPosition(float[] position) {
        OldFroggerMapFile map = getEntity().getMap();
        if (this.pathData.getPathId() < 0 || this.pathData.getPathId() >= map.getPathPacket().getPaths().size())
            return null; // Invalid path.

        OldFroggerMapPath path = this.pathData.getPath(map);
        if (path == null || this.pathData.getSplineId() < 0 || this.pathData.getSplineId() >= path.getSplines().size())
            return null;

        OldFroggerSpline spline = path.getSplines().get(this.pathData.getSplineId());

        IVector vec_x = new IVector();
        IVector vec_y = new IVector();
        IVector vec_z = spline.calculateSplineTangent(this.pathData.getSplinePosition());
        IVector.MROuterProduct12(GAME_Y_AXIS_POS, vec_z, vec_x);
        vec_x.normalise();
        IVector.MROuterProduct12(vec_z, vec_x, vec_y);
        PSXMatrix matrix = PSXMatrix.WriteAxesAsMatrix(new PSXMatrix(), vec_x, vec_y, vec_z);

        Vector endVec = spline.calculateSplinePoint(this.pathData.getSplinePosition());
        position[0] = endVec.getFloatX();
        position[1] = endVec.getFloatY();
        position[2] = endVec.getFloatZ();
        position[3] = (float) matrix.getRollAngle();
        position[4] = (float) matrix.getPitchAngle();
        position[5] = (float) matrix.getYawAngle();
        return position;
    }
}