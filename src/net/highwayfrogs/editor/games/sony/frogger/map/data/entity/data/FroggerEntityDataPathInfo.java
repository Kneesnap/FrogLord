package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Base entity data which holds path data.
 * Created by Kneesnap on 1/20/2019.
 */

public class FroggerEntityDataPathInfo extends FroggerEntityData {
    @Getter private FroggerPathInfo pathInfo;
    private float[] cachedPosition;

    private static final Vector3f TEMP_POSITION = new Vector3f();
    private static final Vector3f TEMP_ROTATION = new Vector3f();

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
        FroggerPathInfo pathInfo = this.pathInfo;
        if (pathInfo == null) {
            if (this.cachedPosition != null) {
                System.arraycopy(this.cachedPosition, 0, position, 0, this.cachedPosition.length);
                return position;
            }

            return null; // No path state.
        }

        // Attempt to evaluate path info.
        if (!evaluatePathInfoToEntityArray(pathInfo, getParentEntity(), position)) {
            if (this.cachedPosition != null) {
                System.arraycopy(this.cachedPosition, 0, position, 0, this.cachedPosition.length);
                return position;
            }

            return null; // Invalid path data.
        }

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

    /**
     * Copies pathing data from the old data.
     * @param oldMatrixData the old matrix data to copy from
     * @param oldPathData the old pathing data to copy from
     */
    public void copyFrom(FroggerEntityDataMatrix oldMatrixData, FroggerEntityDataPathInfo oldPathData) {
        if (oldMatrixData != null)
            this.cachedPosition = oldMatrixData.getPositionAndRotation(this.cachedPosition != null ? this.cachedPosition : new float[6]);

        if (oldPathData != null) {
            this.pathInfo = oldPathData.getPathInfo();
            this.cachedPosition = oldPathData.cachedPosition;
        }
    }

    /**
     * Evaluates the path info to an entity position/rotation array
     * @param pathInfo the path info to evaluate
     * @param entity the entity to evaluate for
     * @param outputArray the output array to store results within
     * @return true iff evaluation was successful
     */
    public static boolean evaluatePathInfoToEntityArray(FroggerPathInfo pathInfo, FroggerMapEntity entity, float[] outputArray) {
        if (pathInfo == null)
            throw new NullPointerException("pathInfo");
        if (outputArray == null)
            throw new NullPointerException("outputArray");
        if (outputArray.length != 6)
            throw new IllegalArgumentException("outputArray.length was expected to be 6, but was actually " + outputArray.length + "!");
        if (!pathInfo.evaluatePositionAndRotation(entity, TEMP_POSITION, TEMP_ROTATION))
            return false; // Invalid path data.

        outputArray[0] = TEMP_POSITION.getX();
        outputArray[1] = TEMP_POSITION.getY();
        outputArray[2] = TEMP_POSITION.getZ();
        outputArray[3] = TEMP_ROTATION.getX();
        outputArray[4] = TEMP_ROTATION.getY();
        outputArray[5] = TEMP_ROTATION.getZ();
        return true;
    }
}