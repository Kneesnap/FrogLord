package net.highwayfrogs.editor.games.sony.shared.mof2.animation;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofBoundingBoxSet;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the MR_ANIM_MODEL_SET struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MRAnimatedMofModelSet extends SCSharedGameData {
    @Getter @NonNull private final transient MRAnimatedMof parentMof;
    @Getter private final List<MRAnimatedMofModel> models = new ArrayList<>();
    @Getter private final MRAnimatedMofCelSet celSet;
    @Getter private byte unknownPaddingValue; // TODO: Figure out the pattern of this value.

    private short tempModelCount = -1;
    private short tempCelSetCount = -1;
    private short tempBboxSetCount = -1; // Always zero in Frogger. Non-zero in MediEvil.
    private int tempModelPointer = -1; // After previous model set data, or if there isn't one, after the last model set header.
    private int tempCelSetPointer = -1; // Right after model data
    private int tempBboxSetPointer = -1; // Right after cel set data. Bounding boxes (NOT SETS) are here.
    private int tempBboxSetCountPointer = -1;


    public static final int CEL_SET_COUNT = 1;
    private static final int EXPECTED_TYPE = 0;

    public MRAnimatedMofModelSet(MRAnimatedMof parent) {
        super(parent.getGameInstance());
        this.parentMof = parent;
        this.celSet = new MRAnimatedMofCelSet(this);
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.parentMof.getLogger(), "modelSet=" + getModelSetIndex(), AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        // The only type which might exist is 1, aka MR_ANIM_MODEL_SET_HIERARCHICHAL.
        // However, this field is never checked by the code so it most likely is always zero.
        int type = reader.readInt();
        if (type != EXPECTED_TYPE)
            throw new RuntimeException("Unsupported MRAnimatedMofModelSet type ID: " + type);

        this.tempModelCount = reader.readUnsignedByteAsShort();
        this.tempCelSetCount = reader.readUnsignedByteAsShort();
        this.tempBboxSetCount = reader.readUnsignedByteAsShort(); // Always zero in Frogger. Non-zero in MediEvil.
        this.unknownPaddingValue = reader.readByte();

        // Each model set is written together with its model data.
        this.tempModelPointer = reader.readInt(); // Bounding boxes (NOT SETS) are here.
        this.tempCelSetPointer = reader.readInt(); // Right after model(s) & bounding boxes (NOT SETS).
        this.tempBboxSetPointer = reader.readInt(); // After celSet data (usually the same as the common data pointer)

        if (this.tempModelCount != 1) // TODO: TOSS
            getLogger().info("Found modelSet with %d models!", this.tempModelCount);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(EXPECTED_TYPE);
        writer.writeUnsignedByte((short) this.models.size());
        writer.writeUnsignedByte((short) CEL_SET_COUNT);
        this.tempBboxSetCountPointer = writer.getIndex();
        writer.writeNull(2); // BBOX Count (written later) and 1 byte of padding.

        this.tempModelPointer = writer.writeNullPointer();
        this.tempCelSetPointer = writer.writeNullPointer();
        this.tempBboxSetPointer = writer.writeNullPointer();
    }

    /**
     * Reads the modelSet body data from the reader.
     * @param reader the reader to load the data from
     */
    void readModelSetData(DataReader reader) {
        if (this.tempBboxSetCountPointer >= 0)
            throw new RuntimeException("Cannot read MRAnimatedMofModelSet body data, the object appears to be in a corrupted state.");
        if (this.tempCelSetCount < 0 || this.tempBboxSetCount < 0 || this.tempModelCount < 0
                || this.tempModelPointer < 0 || this.tempCelSetPointer < 0 || this.tempBboxSetPointer < 0)
            throw new RuntimeException("Cannot read MRAnimatedMofModelSet body data, as load(DataReader) has not happened first.");

        // Read models.
        this.models.clear();
        requireReaderIndex(reader, this.tempModelPointer, "Expected MRAnimatedMofModel entries");
        for (int i = 0; i < this.tempModelCount; i++) {
            MRAnimatedMofModel newModel = new MRAnimatedMofModel(this);
            this.models.add(newModel); // Add before loading to ensure it can find its own ID.
            newModel.load(reader);
        }

        // Read model body data.
        for (int i = 0; i < this.models.size(); i++)
            this.models.get(i).readBodyData(reader);

        // Read celSets.
        // Because there's a celSetCount, you'd think it'd be possible for multiple celSets to exist, right?
        // Wrong! The game always accesses the celSetPointer directly, not as an array, except during setup where it for some reason uses it as an array.
        // But as this would do nothing, and celSetCount has never been observed to differ from one, this is assumed to always be one.
        if (this.tempCelSetCount != CEL_SET_COUNT)
            throw new RuntimeException("MRAnimatedMofModelSet had " + this.tempCelSetCount + " cel-sets! This was thought impossible!");
        requireReaderIndex(reader, this.tempCelSetPointer, "Expected MRAnimatedMofCelSet");
        this.celSet.load(reader);

        // Validate celSet pointer in models.
        for (int i = 0; i < this.models.size(); i++)
            this.models.get(i).validateCelSetPointer(this.tempCelSetPointer);

        // Read bounding box sets.
        // In most games, such as Frogger, this never happens, but it has been seen in some MediEvil builds (such as ECTS), MRORGAN.XAR uses this.
        requireReaderIndex(reader, this.tempBboxSetPointer, "Expected BboxSet data");
        if (this.tempBboxSetCount > 0) {
            Map<Integer, MRMofBoundingBoxSet> boundingBoxSetsByIndex = new HashMap<>();
            for (int i = 0; i < this.tempBboxSetCount; i++) {
                MRMofBoundingBoxSet boundingBoxSet = new MRMofBoundingBoxSet(getGameInstance());
                boundingBoxSetsByIndex.put(reader.getIndex(), boundingBoxSet);
                boundingBoxSet.load(reader);
            }

            // Read model body data.
            for (int i = 0; i < this.models.size(); i++)
                this.models.get(i).readBoundingBoxSet(boundingBoxSetsByIndex);

            // Warn if not all bounding box sets are used.
            List<MRMofBoundingBoxSet> remainingBoundingBoxSets = new ArrayList<>(boundingBoxSetsByIndex.values());
            for (int i = 0; i < this.models.size(); i++)
                remainingBoundingBoxSets.remove(this.models.get(i).getBoundingBoxSet());

            // Sanity check.
            if (!remainingBoundingBoxSets.isEmpty())
                getLogger().warning("%d MRMofBoundingBoxSet(s) were present but unused!", remainingBoundingBoxSets.size());
        }

        // Clear.
        this.tempModelCount = -1;
        this.tempCelSetCount = -1;
        this.tempBboxSetCount = -1;
        this.tempModelPointer = -1;
        this.tempCelSetPointer = -1;
        this.tempBboxSetPointer = -1;
    }

    /**
     * Writes the modelSet body data to the writer.
     * @param writer the writer to write the body data to
     */
    void writeModelSetData(DataWriter writer) {
        if (this.tempCelSetCount >= 0 || this.tempBboxSetCount >= 0 || this.tempModelCount >= 0)
            throw new RuntimeException("Cannot save MRAnimatedMofModelSet body data, it does not appear to have been loaded correctly.");
        if (this.tempModelPointer < 0 || this.tempCelSetPointer < 0 || this.tempBboxSetPointer < 0 || this.tempBboxSetCountPointer < 0)
            throw new RuntimeException("Cannot save MRAnimatedMofModelSet body data, as save(DataReader) has been called yet.");

        // Write models.
        writer.writeAddressTo(this.tempModelPointer);
        for (int i = 0; i < this.models.size(); i++)
            this.models.get(i).save(writer);

        // Write model body data.
        for (int i = 0; i < this.models.size(); i++)
            this.models.get(i).writeBodyData(writer);

        // Write Celset.
        int celSetDataPointer = writer.getIndex();
        writer.writeAddressTo(this.tempCelSetPointer);
        this.celSet.save(writer);

        // Writes Cel Set Pointers.
        for (int i = 0; i < this.models.size(); i++)
            this.models.get(i).writeCelSetPointer(writer, celSetDataPointer);

        // Write bboxSets.
        writer.writeAddressTo(this.tempBboxSetPointer);
        Map<MRMofBoundingBoxSet, Integer> writtenBoundingBoxSets = null;
        for (int i = 0; i < this.models.size(); i++) {
            MRAnimatedMofModel model = this.models.get(i);
            if (model.getBoundingBoxSet() == null) {
                model.writeBoundingBoxSet(writer, 0);
                continue;
            }

            if (writtenBoundingBoxSets == null)
                writtenBoundingBoxSets = new HashMap<>();

            Integer previousIndex = writtenBoundingBoxSets.get(model.getBoundingBoxSet());
            if (previousIndex != null) {
                model.writeBoundingBoxSet(writer, previousIndex);
            } else {
                writtenBoundingBoxSets.put(model.getBoundingBoxSet(), writer.getIndex());
                model.writeBoundingBoxSet(writer, writer.getIndex());
                model.getBoundingBoxSet().save(writer);
            }
        }

        // Write the number of bounding box sets.
        writer.jumpTemp(this.tempBboxSetCountPointer);
        writer.writeUnsignedByte((short) (writtenBoundingBoxSets != null ? writtenBoundingBoxSets.size() : 0));
        writer.jumpReturn();

        // Clear temp data.
        this.tempModelPointer = -1;
        this.tempCelSetPointer = -1;
        this.tempBboxSetPointer = -1;
        this.tempBboxSetCountPointer = -1;
    }

    /**
     * Gets the index/ID of this model set.
     */
    public int getModelSetIndex() {
        return Utils.getLoadingIndex(this.parentMof.getModelSets(), this);
    }
}