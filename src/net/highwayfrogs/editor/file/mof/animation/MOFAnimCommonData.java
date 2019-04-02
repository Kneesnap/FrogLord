package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_COMMON_DATA" struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimCommonData extends GameObject {
    private List<TransformObject> transforms = new ArrayList<>();
    private transient MOFAnimation parent;

    public static final int FLAG_TRANSFORM_PRESENT = Constants.BIT_FLAG_0;
    public static final int FLAG_BBOX_PRESENT = Constants.BIT_FLAG_3;

    private static final int DEFAULT_FLAGS = FLAG_TRANSFORM_PRESENT | FLAG_BBOX_PRESENT;

    public MOFAnimCommonData(MOFAnimation parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        int flags = reader.readInt();
        Utils.verify(flags == DEFAULT_FLAGS, "Cannot handle AnimCommonData flags: (%s)", Utils.toHexString(flags));

        short transformCount = reader.readShort();
        short rotationCount = reader.readShort();
        short translationCount = reader.readShort();
        short bboxCount = reader.readShort();

        int transformPointer = reader.readInt();
        int rotationPointer = reader.readInt();
        int translationPointer = reader.readInt();
        int bboxPointer = reader.readInt();

        Utils.verify(rotationCount == 0, "There is a non-zero rotation count! (%d, %d)", rotationCount, rotationPointer);
        Utils.verify(translationCount == 0, "There is a non-zero translation count! (%d, %d)", translationCount, translationPointer);
        Utils.verify(bboxCount == 0, "There is a non-zero bounding box count! (%d, %d)", bboxCount, bboxPointer);

        // Read Transforms.
        TransformType transformType = getParent().getTransformType();
        reader.jumpTemp(transformPointer);
        for (int i = 0; i < transformCount; i++) {
            TransformObject transform = transformType.makeTransform();
            transform.load(reader);
            getTransforms().add(transform);
        }

        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(DEFAULT_FLAGS);
        writer.writeShort((short) getTransforms().size());
        writer.writeShort((short) 0);
        writer.writeShort((short) 0);
        writer.writeShort((short) 0);

        int transformPointer = writer.writeNullPointer();
        writer.writeInt(0);
        writer.writeInt(0);
        writer.writeInt(0);

        // Write Transforms.
        writer.writeAddressTo(transformPointer);
        getTransforms().forEach(transform -> transform.save(writer));
        if (writer.getIndex() % 4 > 0) // Padding.
            writer.writeShort((short) 0);
    }
}
