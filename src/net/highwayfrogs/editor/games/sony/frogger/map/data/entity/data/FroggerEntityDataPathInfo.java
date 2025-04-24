package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCMath;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity.FroggerMapEntityEntityFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack.FroggerGridStackInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Base entity data which holds path data.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
@Setter
public class FroggerEntityDataPathInfo extends FroggerEntityData {
    private FroggerPathInfo pathInfo;
    private static final IVector GAME_Y_AXIS_POS = new IVector(0, SCMath.FIXED_POINT_ONE, 0);

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

        // Reimplements ENTITY.C/ENTSTRUpdateMovingMOF()
        FroggerPathResult result = path.evaluatePosition(pathInfo);

        // Don't rotate if we're aligned to the world.
        SVector pathPosition = result.getPosition();
        position[0] = pathPosition.getFloatX();
        position[1] = pathPosition.getFloatY();
        position[2] = pathPosition.getFloatZ();
        if (getParentEntity().testFlag(FroggerMapEntityEntityFlag.ALIGN_TO_WORLD)) { // Example: The spinners in VOL1.MAP don't rotate to follow the path despite following it. Also the pink balloons.
            position[3] = 0;
            position[4] = 0;
            position[5] = 0;
            return position;
        }

        IVector vecZ = new IVector(result.getRotation());
        if (getParentEntity().testFlag(FroggerMapEntityEntityFlag.PROJECT_ON_LAND)) { // Example: Hedgehogs and many of the jungle entities.
            FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
            FroggerGridStack gridStack = gridPacket.getGridStack(gridPacket.getGridXFromWorldX(pathPosition.getX()), gridPacket.getGridZFromWorldZ(pathPosition.getZ()));
            FroggerGridStackInfo stackInfo = gridStack != null ? gridStack.getGridStackInfo() : null;
            if (stackInfo != null) {
                int dx = stackInfo.getXSlope().dotProduct(vecZ) >> 12;
                int dz = stackInfo.getZSlope().dotProduct(vecZ) >> 12;
                position[1] = stackInfo.getY() >> 4; // Snap to the ground.
                vecZ.setX(((stackInfo.getXSlope().getX() * dx) + (stackInfo.getZSlope().getX() * dz)) >> 12);
                vecZ.setY(((stackInfo.getXSlope().getY() * dx) + (stackInfo.getZSlope().getY() * dz)) >> 12);
                vecZ.setZ(((stackInfo.getXSlope().getZ() * dx) + (stackInfo.getZSlope().getZ() * dz)) >> 12);
            }
        }

        IVector vecX = new IVector();
        if (getParentEntity().testFlag(FroggerMapEntityEntityFlag.LOCAL_ALIGN)) {
            // I don't think this is entirely correct to how it works in-game, but it captures the spirit of what the code in-game is supposed to do.
            // After the player dies or collects a checkpoint, some entities rotate weirdly. (It might take a few tries) For example,
            // There's one slug in SWP4.MAP which absolutely refuses to cooperate. All the other ones appear right.

            vecX.clear();
            if (Math.abs(vecZ.getFloatX()) > .5F) { // In PSXMatrix, matrix[2][0] = vecZ.x
                vecX.setZ(SCMath.FIXED_POINT_ONE);
            } else {
                vecX.setX(SCMath.FIXED_POINT_ONE);
            }
        } else {
            // ENTITY_BOOK_XZ_PARALLEL_TO_CAMERA applies sprite billboarding, but FrogLord doesn't need to show that.
            IVector.MROuterProduct12(GAME_Y_AXIS_POS, vecZ, vecX);
        }


        vecX.normalise();
        IVector vecY = new IVector();
        IVector.MROuterProduct12(vecZ, vecX, vecY);
        PSXMatrix matrix = PSXMatrix.WriteAxesAsMatrix(new PSXMatrix(), vecX, vecY, vecZ);

        position[3] = (float) matrix.getPitchAngle();
        position[4] = (float) matrix.getYawAngle();
        position[5] = (float) matrix.getRollAngle();
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