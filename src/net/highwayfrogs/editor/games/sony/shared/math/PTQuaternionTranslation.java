package net.highwayfrogs.editor.games.sony.shared.math;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Arrays;

/**
 * Pretty much an updated MR_QUAT_TRANS.
 * Created by Kneesnap on 5/21/2024.
 */
@Getter
public class PTQuaternionTranslation extends PTQuaternion {
    private final short[] translation = new short[3];

    public PTQuaternionTranslation(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        for (int i = 0; i < this.translation.length; i++)
            this.translation[i] = reader.readShort();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int i = 0; i < this.translation.length; i++)
            writer.writeShort(this.translation[i]);
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ ((this.translation[2] << 8) ^ (this.translation[1] << 16) ^ this.translation[0]);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PTQuaternionTranslation))
            return false;

        return Arrays.equals(this.translation, ((PTQuaternionTranslation) obj).translation) && super.equals(obj);
    }

    @Override
    public void fromMatrix(PSXMatrix matrix) {
        this.translation[0] = (short) matrix.getTransform()[0];
        this.translation[1] = (short) matrix.getTransform()[1];
        this.translation[2] = (short) matrix.getTransform()[2];
        super.fromMatrix(matrix);
    }

    @Override
    public PSXMatrix createMatrix() {
        PSXMatrix matrix = super.createMatrix();
        matrix.getTransform()[0] = this.translation[0];
        matrix.getTransform()[1] = this.translation[1];
        matrix.getTransform()[2] = this.translation[2];
        return matrix;
    }

    @Override
    public String toString() {
        return "PT_QUAT_TRANS<c=" + getC() + ",x=" + getX() + ",y=" + getY() + ",z=" + getZ() + ",tx=" + this.translation[0] + ",ty=" + this.translation[1] + ",tz=" + this.translation[2] + ">";
    }
}