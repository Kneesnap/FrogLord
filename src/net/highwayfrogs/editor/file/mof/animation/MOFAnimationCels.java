package net.highwayfrogs.editor.file.mof.animation;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_CELS" struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MOFAnimationCels extends GameObject {
    private int partCount; // In the future maybe this can be calculated.
    private int flags;
    private List<Integer> celNumbers = new ArrayList<>();
    private List<Short> indices = new ArrayList<>();

    private transient int tempCelNumberPointer;
    private transient int tempIndicePointer;

    public static final int FLAG_VIRTUAL_STANDARD = Constants.BIT_FLAG_0;
    public static final int FLAG_VIRTUAL_INTERPOLATION = Constants.BIT_FLAG_1; // Virtual cel indices are read as prev, next, interp value.

    @Override
    public void load(DataReader reader) {
        int celCount = reader.readUnsignedShortAsInt();
        this.partCount = reader.readUnsignedShortAsInt();
        int virtualCelCount = reader.readUnsignedShortAsInt();
        this.flags = reader.readUnsignedShortAsInt();

        int celNumberPointer = reader.readInt();
        int indicePointer = reader.readInt();

        reader.jumpTemp(celNumberPointer);
        for (int i = 0; i < celCount; i++)
            celNumbers.add(reader.readUnsignedShortAsInt());
        reader.jumpReturn();

        reader.jumpTemp(indicePointer);
        for (int i = 0; i < virtualCelCount; i++)
            indices.add(reader.readShort());
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.celNumbers.size());
        writer.writeUnsignedShort(this.partCount);
        writer.writeUnsignedShort(this.indices.size());
        writer.writeUnsignedShort(this.flags);

        this.tempCelNumberPointer = writer.getIndex();
        writer.writeInt(0);

        this.tempIndicePointer = writer.getIndex();
        writer.writeInt(0);
    }

    /**
     * Write extra Cel data.
     * @param writer The writer to write data to.
     */
    public void writeExtraData(DataWriter writer) {
        Utils.verify(tempCelNumberPointer > 0 && tempIndicePointer > 0, "Normal write was not called.");

        writer.writeAddressTo(this.tempCelNumberPointer);
        for (int anInt : this.celNumbers)
            writer.writeUnsignedShort(anInt);

        writer.writeAddressTo(this.tempIndicePointer);
        for (short aShort : this.indices)
            writer.writeShort(aShort);

        this.tempIndicePointer = 0;
        this.tempCelNumberPointer = 0;
    }
}
