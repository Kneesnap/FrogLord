package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.mof.MOFBBox;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformObject;
import net.highwayfrogs.editor.file.mof.animation.transform.TransformType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_COMMON_DATA" struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimCommonData extends GameObject {
    private MOFAnimation parent;
    private int flags;
    private List<TransformObject> transforms = new ArrayList<>();

    public static final int FLAG_TRANSFORM_PRESENT = 1;
    public static final int FLAG_ROTATION_PRESENT = 1 << 1;
    public static final int FLAG_TRANSLATION_PRESENT = 1 << 2;
    public static final int FLAG_BBOX_PRESENT = 1 << 3;

    public MOFAnimCommonData(MOFAnimation parent) {
        this.parent = parent;
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        short transformCount = reader.readShort();
        short rotationCount = reader.readShort();
        short translationCount = reader.readShort();
        short bboxCount = reader.readShort();

        int transformPointer = reader.readInt();
        int rotationPointer = reader.readInt();
        int translationPointer = reader.readInt();
        int bboxPointer = reader.readInt();

        Utils.verify(rotationCount == 0, "There is a non-zero rotation count!");
        Utils.verify(translationCount == 0, "There is a non-zero translation count!");
        Utils.verify(bboxCount == 0, "There is a non-zero bounding box count!");

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
        writer.writeInt(this.flags);
        writer.writeShort((short) getTransforms().size());
        writer.writeShort((short) 0);
        writer.writeShort((short) 0);
        writer.writeShort((short) 0);

        int transformPointer = writer.getIndex() + (4 * Constants.POINTER_SIZE);
        writer.writeInt(transformPointer);
        writer.writeInt(0);
        writer.writeInt(0);
        writer.writeInt(0);

        // Write Transforms.
        Utils.verify(transformPointer == writer.getIndex(), "Transform Pointer calculated wrong! (%d, %d)", transformPointer, writer.getIndex());
        getTransforms().forEach(transform -> transform.save(writer));
        if (writer.getIndex() % 4 > 0) // Padding.
            writer.writeShort((short) 0);
    }
}
