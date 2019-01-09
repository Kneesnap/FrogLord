package net.highwayfrogs.editor.file.mof.animation;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the "MR_ANIM_CELS" struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MOFAnimationCels extends GameObject {
    private int flags; //TODO: Port

    @Override
    public void load(DataReader reader) {
        int celCount = reader.readUnsignedShortAsInt();
        int partCount = reader.readUnsignedShortAsInt(); // celCount + 1.
        int virtualCelCount = reader.readUnsignedShortAsInt();
        this.flags = reader.readUnsignedShortAsInt();

        int celNumberPointer = reader.readInt();
        int transformPointer = reader.readInt(); // TODO: Union of two pointers for ac_indices and ac_transforms.
        System.out.println("Number Pointer: " + celNumberPointer + ", Transform Pointer: " + transformPointer + ", Reader: " + reader.getIndex());

    }

    @Override
    public void save(DataWriter writer) {
        //TODO
    }
}
