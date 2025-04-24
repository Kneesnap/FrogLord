package net.highwayfrogs.editor.file.mof.animation;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the MR_ANIM_MODEL_SET struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MOFAnimationModelSet extends SCSharedGameData {
    private final MOFAnimationModel model;
    @Getter private final MOFAnimationCelSet celSet;
    @Getter private final transient MOFAnimation parent;
    // BBOX Set is always empty, so we don't keep it.

    public static final int CEL_SET_COUNT = 1;
    private static final int FORCED_MODEL_COUNT = 1;
    private static final int DEFAULT_TYPE = 0;

    public MOFAnimationModelSet(SCGameInstance instance, MOFAnimation parent) {
        super(instance);
        this.parent = parent;
        this.model = new MOFAnimationModel(this);
        this.celSet = new MOFAnimationCelSet(parent);
    }

    @Override
    public void load(DataReader reader) {
        int type = reader.readInt();
        Utils.verify(type == DEFAULT_TYPE, "Unknown Model-Set Type: %d!", type);

        short modelCount = reader.readUnsignedByteAsShort();
        short celsetCount = reader.readUnsignedByteAsShort();
        short bboxCount = reader.readUnsignedByteAsShort();
        reader.skipByte(); // Padding.

        int modelPointer = reader.readInt();
        int celsetPointer = reader.readInt(); // Right after struct.
        int bboxPointer = reader.readInt();

        //Utils.verify(bboxCount == 0, "The ModelSet has a non-zero BBOX count. (%d, %d)", bboxCount, bboxPointer);
        //if (modelCount != FORCED_MODEL_COUNT && !getParent().getFileDisplayName().contains("-FORCED_MODELS"))
        //    getParent().getFileEntry().setFilePath(getParent().getFileDisplayName() + "-FORCED_MODELS"); // TODO: Address later.

        if (!getGameInstance().isMediEvil())
            Utils.verify(modelCount == FORCED_MODEL_COUNT, "FrogLord does not currently support MOFs with more than one model! (%d)", modelCount);

        Utils.verify(celsetCount == CEL_SET_COUNT, "FrogLord does not support MOFs with more than one cel-set! (%d)", celsetCount);

        // Read Celset.
        reader.jumpTemp(celsetPointer);
        this.celSet.load(reader);
        reader.jumpReturn();

        // Read Models. (After Celset so it can reference cel sets loaded previously.)
        reader.jumpTemp(modelPointer);
        this.model.load(reader);
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(DEFAULT_TYPE);

        writer.writeUnsignedByte((short) FORCED_MODEL_COUNT);
        writer.writeUnsignedByte((short) CEL_SET_COUNT);
        writer.writeNull(2); // BBOX Count + 1 byte of padding.

        int modelSetPointer = writer.writeNullPointer();
        int celSetPointer = writer.writeNullPointer(); // Right after model.
        writer.writeNullPointer(); // BBOX Pointer

        // Write models.
        writer.writeAddressTo(modelSetPointer);
        this.model.save(writer);

        // Write Celset.
        writer.writeAddressTo(celSetPointer);
        this.celSet.save(writer);

        // Writes Cel Set Pointers. MUST BE CALLED AFTER
        this.model.writeCelPointer(writer);
    }
}