package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_CELS" struct.
 * This is believed to be a single animation 'action'. Ie: 1 full animation.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimationCels extends GameObject {
    private List<Integer> celNumbers = new ArrayList<>(); // Entry for each frame. Starts at 0, counts up for each frame, unless there is a duplicate frame, where it won't count. Index into indice list.
    private List<Short> indices = new ArrayList<>(); // All of the transform ids used. Each frame has indices for each part, seemingly in order from start to end of animation.
    @Setter private boolean interpolationEnabled;

    private transient MOFAnimation parent;
    private transient int tempCelNumberPointer;
    private transient int tempIndicePointer;

    public static final int FLAG_VIRTUAL_STANDARD = Constants.BIT_FLAG_0;
    public static final int FLAG_VIRTUAL_INTERPOLATION = Constants.BIT_FLAG_1;

    public MOFAnimationCels(MOFAnimation parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        int celCount = reader.readUnsignedShortAsInt();
        int partCount = reader.readUnsignedShortAsInt();
        int virtualCelCount = reader.readUnsignedShortAsInt();
        this.interpolationEnabled = (reader.readUnsignedShortAsInt() == FLAG_VIRTUAL_INTERPOLATION);

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
    }

    @Override
    public void save(DataWriter writer) {
        int partCount = getParent().getStaticMOF().getParts().size();
        writer.writeUnsignedShort(this.celNumbers.size()); // This may need to have 1 subtracted if 1 is added while loading.
        writer.writeUnsignedShort(partCount);
        writer.writeUnsignedShort(this.indices.size() / partCount);
        writer.writeUnsignedShort(this.interpolationEnabled ? FLAG_VIRTUAL_INTERPOLATION : FLAG_VIRTUAL_STANDARD);
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
        int actualCel = celNumbers.get(frame % celNumbers.size()) - (getParent().isStartAtFrameZero() ? 0 : 1);
        int partCount = getParent().getStaticMOF().getParts().size();
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
