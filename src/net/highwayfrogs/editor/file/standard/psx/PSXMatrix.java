package net.highwayfrogs.editor.file.standard.psx;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a PSX "MATRIX" struct.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PSXMatrix extends GameObject {
    private short[][] matrix; // Rotation Matrix.
    private int[] transfer; // Transfer vector.

    private static final int DIMENSION = 3;
    public static final int BYTE_SIZE = (DIMENSION * DIMENSION * Constants.SHORT_SIZE) + (DIMENSION * Constants.INTEGER_SIZE);

    @Override
    public void load(DataReader reader) {
        this.matrix = new short[DIMENSION][DIMENSION];
        this.transfer = new int[DIMENSION];

        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < DIMENSION; j++)
                this.matrix[i][j] = reader.readShort();

        for (int i = 0; i < this.transfer.length; i++)
            this.transfer[i] = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (short[] aMatrix : this.matrix)
            for (short aShort : aMatrix)
                writer.writeShort(aShort);

        for (int aTransfer : this.transfer)
            writer.writeInt(aTransfer);
    }
}
