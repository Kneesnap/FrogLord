package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MR_ANIM_MODEL_SET struct.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MOFAnimationModelSet extends GameObject {
    private int type;
    private List<MOFAnimationModel> models = new ArrayList<>();
    private List<MOFAnimationCelSet> celSets = new ArrayList<>();
    // BBOX Set is always empty, so we don't keep it.

    public static final int FLAG_HIERARCHICAL = Constants.BIT_FLAG_0;

    @Override
    public void load(DataReader reader) {
        this.type = reader.readInt();

        short modelCount = reader.readUnsignedByteAsShort();
        short celsetCount = reader.readUnsignedByteAsShort();
        short bboxCount = reader.readUnsignedByteAsShort();
        reader.readByte(); // Padding.

        int modelPointer = reader.readInt();
        int celsetPointer = reader.readInt(); // Right after struct.
        int bboxPointer = reader.readInt();

        Utils.verify(bboxCount == 0, "The ModelSet has a non-zero BBOX count. (%d, %d)", bboxCount, bboxPointer);

        // Read Models.
        reader.jumpTemp(modelPointer);
        for (int i = 0; i < modelCount; i++) {
            MOFAnimationModel model = new MOFAnimationModel();
            model.load(reader);
        }
        reader.jumpReturn();

        // Read Celset.
        reader.jumpTemp(celsetPointer);
        for (int i = 0; i < celsetCount; i++) {
            MOFAnimationCelSet celSet = new MOFAnimationCelSet();
            celSet.load(reader);
            celSets.add(celSet);
        }
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.type);

        writer.writeUnsignedByte((short) models.size());
        writer.writeUnsignedByte((short) celSets.size());
        writer.writeNull(2); // BBOX Count + 1 byte of padding.

        int modelPointerAddress = writer.getIndex();
        writer.writeInt(0);

        int calculatedCellPointer = writer.getIndex() + (2 * Constants.POINTER_SIZE);
        writer.writeInt(calculatedCellPointer); // Right after struct.
        writer.writeInt(0); // BBOX Pointer

        // Write models.
        Utils.verify(calculatedCellPointer == writer.getIndex(), "Calculated wrong cell pointer index: (%d, %d)", calculatedCellPointer, writer.getIndex());
        getModels().forEach(model -> model.save(writer));

        // Write Celset.
        int tempAddress = writer.getIndex();
        writer.jumpTemp(modelPointerAddress);
        writer.writeInt(tempAddress);
        writer.jumpReturn();

        getCelSets().forEach(celSet -> celSet.save(writer));
    }
}
