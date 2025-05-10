package net.highwayfrogs.editor.games.sony.shared.mof2.animation;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform.MRAnimatedMofTransform;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.transform.MRAnimatedMofTransformType;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofBoundingBox;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the "MR_ANIM_COMMON_DATA" struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MRAnimatedMofCommonData extends SCSharedGameData {
    @NonNull private final MRAnimatedMof parentMof;
    private final List<MRAnimatedMofTransform> transforms = new ArrayList<>();
    private final List<MRMofBoundingBox> boundingBoxes = new ArrayList<>();

    public static final int FLAG_TRANSFORMS_PRESENT = Constants.BIT_FLAG_0;
    public static final int FLAG_ROTATIONS_PRESENT = Constants.BIT_FLAG_1; // This would be used if the "break transforms into parts" (MR_MOF_ANIM_INDEXED_TRANSFORMS_IN_PARTS) flag were seen, but the MR API suggests that will never be the case.
    public static final int FLAG_TRANSLATIONS_PRESENT = Constants.BIT_FLAG_2; // This would be used if the "break transforms into parts" (MR_MOF_ANIM_INDEXED_TRANSFORMS_IN_PARTS) flag were seen, but the MR API suggests that will never be the case.
    public static final int FLAG_BBOXES_PRESENT = Constants.BIT_FLAG_3;
    private static final int EXPECTED_FLAGS = FLAG_TRANSFORMS_PRESENT | FLAG_BBOXES_PRESENT;

    public MRAnimatedMofCommonData(MRAnimatedMof parent) {
        super(parent.getGameInstance());
        this.parentMof = parent;
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.parentMof.getLogger(), "CommonData", AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        int flags = reader.readInt();
        if (flags != EXPECTED_FLAGS)
            throw new RuntimeException("Unsupported flags seen for MRAnimatedMofCommonData: " + NumberUtils.toHexString(flags));

        short transformCount = reader.readShort();
        short rotationCount = reader.readShort();
        short translationCount = reader.readShort();
        short bboxCount = reader.readShort();

        int transformPointer = reader.readInt();
        int rotationPointer = reader.readInt();
        int translationPointer = reader.readInt();
        int bboxPointer = reader.readInt(); // bboxPointer is always set, even when the count is zero.

        // The following are explicitly not used by the MR API.
        if (rotationCount != 0 || rotationPointer != 0)
            throw new RuntimeException("There was non-zero rotation data in the MRAnimatedMofCommonData! (Pointer: " + NumberUtils.toHexString(rotationPointer) + ", Count:" + rotationCount + ")");
        if (translationCount != 0 || translationPointer != 0)
            throw new RuntimeException("There was non-zero translation data in the MRAnimatedMofCommonData! (Pointer: " + NumberUtils.toHexString(translationPointer) + ", Count:" + translationCount + ")");

        // Read Transforms.
        this.transforms.clear();
        requireReaderIndex(reader, transformPointer, "Expected MRAnimatedMofTransform data");
        MRAnimatedMofTransformType transformType = this.parentMof.getTransformType();
        for (int i = 0; i < transformCount; i++) {
            MRAnimatedMofTransform newTransform = transformType.makeTransform();
            newTransform.load(reader);
            this.transforms.add(newTransform);
        }

        reader.align(Constants.INTEGER_SIZE); // The data here can be non-zero (garbage)

        // Read bounding box data, if it's there. (Pretty much only been seen for MRORGAN.XAR in MediEvil ECTS onward)
        this.boundingBoxes.clear();
        requireReaderIndex(reader, bboxPointer, "Expected non-existent BBOX data");
        for (int i = 0; i < bboxCount; i++) {
            MRMofBoundingBox newBoundingBox = new MRMofBoundingBox();
            this.boundingBoxes.add(newBoundingBox);
            newBoundingBox.load(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(EXPECTED_FLAGS);
        writer.writeUnsignedShort(this.transforms.size());
        writer.writeUnsignedShort(0); // Rotation Count
        writer.writeUnsignedShort(0); // Translation Count
        writer.writeUnsignedShort(this.boundingBoxes.size()); // Bounding Box Count

        int transformPointer = writer.writeNullPointer();
        writer.writeInt(0); // Rotation Pointer
        writer.writeInt(0); // Translation Pointer
        int bboxPointer = writer.writeNullPointer(); // bboxPointer is always set, even when the count is zero.

        // Write Transforms.
        writer.writeAddressTo(transformPointer);
        for (int i = 0; i < this.transforms.size(); i++)
            this.transforms.get(i).save(writer);
        writer.align(Constants.INTEGER_SIZE);

        // Write bbox pointer even if there aren't any. (Pretty much only been seen for MRORGAN.XAR in MediEvil ECTS onward)
        this.boundingBoxes.clear();
        writer.writeAddressTo(bboxPointer);
        for (int i = 0; i < this.boundingBoxes.size(); i++)
            this.boundingBoxes.get(i).save(writer);
    }
}
