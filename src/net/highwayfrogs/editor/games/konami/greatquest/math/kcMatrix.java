package net.highwayfrogs.editor.games.konami.greatquest.math;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;

/**
 * Represents a 4x4 matrix.
 * Created by Kneesnap on 4/19/2024.
 */
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
}