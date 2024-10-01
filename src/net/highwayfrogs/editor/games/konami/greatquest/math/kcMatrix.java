package net.highwayfrogs.editor.games.konami.greatquest.math;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.system.math.Vector3f;

import java.util.Arrays;

/**
 * Represents a 4x4 matrix.
 * Created by Kneesnap on 4/19/2024.
 */
@Getter
public class kcMatrix extends GameData<GreatQuestInstance> {
    private final float[][] matrix = new float[MATRIX_HEIGHT][MATRIX_WIDTH];

    public static final int MATRIX_WIDTH = 4;
    public static final int MATRIX_HEIGHT = 4;
    public static final int BYTE_SIZE = MATRIX_WIDTH * MATRIX_HEIGHT * Constants.FLOAT_SIZE;

    public kcMatrix(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        for (int y = 0; y < MATRIX_HEIGHT; y++)
            for (int x = 0; x < MATRIX_WIDTH; x++)
                this.matrix[y][x] = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        for (int y = 0; y < MATRIX_HEIGHT; y++)
            for (int x = 0; x < MATRIX_WIDTH; x++)
                writer.writeFloat(this.matrix[y][x]);
    }

    @Override
    public String toString() {
        return "kcMatrix[" + Arrays.toString(this.matrix[0]) + "," + Arrays.toString(this.matrix[1]) + "," + Arrays.toString(this.matrix[2]) + "," + Arrays.toString(this.matrix[3]) + "]";
    }

    /**
     * Equivalent to ApplyMatrix. matrix -> vec (http://psxdev.tlrmcknz.com/psyq/ref/libref46/0392.html?sidebar=outlines)
     * @param matrix The matrix to multiply.
     */
    public static Vector3f applyMatrix(kcVector3 position, kcMatrix matrix) {
        Vector3f output = new Vector3f();
        output.setX((((matrix.matrix[0][0] * position.getX()) + (matrix.matrix[0][1] * position.getY()) + (matrix.matrix[0][2] * position.getZ()))) + matrix.matrix[3][0]);
        output.setY((((matrix.matrix[1][0] * position.getX()) + (matrix.matrix[1][1] * position.getY()) + (matrix.matrix[1][2] * position.getZ()))) + matrix.matrix[3][1]);
        output.setZ((((matrix.matrix[2][0] * position.getX()) + (matrix.matrix[2][1] * position.getY()) + (matrix.matrix[2][2] * position.getZ()))) + matrix.matrix[3][2]);
        return output;
    }
}