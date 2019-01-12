package net.highwayfrogs.editor.file.mof.animation.transform;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the "MR_MAT34B" transformation struct.
 * Created by Kneesnap on 1/5/2019.
 */
@Getter
public class MR_MAT34B extends TransformObject {
    private byte[][] matrix = new byte[3][3];
    private short[] transform = new short[3];

    @Override
    public void load(DataReader reader) {
        for (int i = 0; i < this.matrix.length; i++)
            for (int j = 0; j < this.matrix[i].length; j++)
                this.matrix[i][j] = reader.readByte();

        reader.readByte(); // Padding.
        for (int i = 0; i < this.transform.length; i++)
            this.transform[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        for (byte[] aMatrix : this.matrix)
            for (byte aByte : aMatrix)
                writer.writeByte(aByte);
        writer.writeByte(Constants.NULL_BYTE); // Padding

        for (short aTransfer : this.transform)
            writer.writeShort(aTransfer);
    }
}
