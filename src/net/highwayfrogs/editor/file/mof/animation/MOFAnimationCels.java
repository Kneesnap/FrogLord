package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_CELS" struct.
 * This is believed to be a single animation 'action'. Ie: 1 full animation.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimationCels extends GameObject {
    private int partCount; // In the future maybe this can be calculated. This is the number of parts this animation keeps data for.
    private int flags;
    private List<Integer> celNumbers = new ArrayList<>(); // celNumbers[virtualId] -> actualCel
    private List<Short> indices = new ArrayList<>(); // Transform Ids. [(actualCel * partCount) + part] part is ?.

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

        int totalIndiceCount = virtualCelCount * partCount;
        reader.jumpTemp(celNumberPointer);
        for (int i = 0; i < celCount; i++)
            celNumbers.add(reader.readUnsignedShortAsInt());
        reader.jumpReturn();

        reader.jumpTemp(indicePointer);
        for (int i = 0; i < totalIndiceCount; i++)
            indices.add(reader.readShort());
        reader.jumpReturn();

        Utils.verify((getFlags() & FLAG_VIRTUAL_INTERPOLATION) == 0, "Model cel-set had interpolation enabled!"); // We don't support this mode as of now.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.celNumbers.size()); // This may need to have 1 subtracted if 1 is added while loading.
        writer.writeUnsignedShort(this.partCount);
        writer.writeUnsignedShort(this.indices.size() / getPartCount());
        writer.writeUnsignedShort(this.flags);
        this.tempCelNumberPointer = writer.writeNullPointer();
        this.tempIndicePointer = writer.writeNullPointer();
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

        if (this.indices.size() % 2 > 0)
            writer.writeShort((short) 0); // Writes padding.

        this.tempIndicePointer = 0;
        this.tempCelNumberPointer = 0;
    }

    /**
     * Gets the transform ID for an animation stage.
     * @param frame The frame of this animation.
     * @param part  The mof part to get the transform for.
     * @return transformId
     */
    public int getTransformID(int frame, MOFPart part) {
        int actualCel = celNumbers.get(frame % celNumbers.size());
        return indices.get((actualCel * partCount) + part.getPartID());
    }

    /**
     * Get the total number of frames in this cel.
     * @return frameCount
     */
    public int getFrameCount() {
        return celNumbers.size();
    }
}
