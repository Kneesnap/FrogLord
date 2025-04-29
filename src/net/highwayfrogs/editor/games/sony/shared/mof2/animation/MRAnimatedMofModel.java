package net.highwayfrogs.editor.games.sony.shared.mof2.animation;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofBoundingBox;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofBoundingBoxSet;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.List;
import java.util.Map;

/**
 * Represents a MOF animation model. Struct "MR_ANIM_MODEL"
 * Eventually, support PERCEL_BBOXES_INCLUDED.
 * Created by Kneesnap on 8/25/2018.
 */
public class MRAnimatedMofModel extends SCSharedGameData {
    @Getter @NonNull private final MRAnimatedMofModelSet parentModelSet;
    @Getter @Setter private MRMofBoundingBox boundingBox; // TODO: Auto-generate.
    // This is almost never seen, I think I've only seen it in the MediEvil ECTS prototype. So we can ignore auto-generation of this.
    @Getter @Setter private MRMofBoundingBoxSet boundingBoxSet; // TODO: Values in celNumbers should be feasible to resolve here (with application of start at frame zero question)
    // Constraint is unused.

    private transient int tempPartCount = -1;
    private transient int tempBBoxPointerAddress = -1;
    private transient int tempBBoxSetPointerAddress = -1;
    private transient int tempCelSetPointerAddress = -1;

    private static final int EXPECTED_ANIMATION_TYPE = 1;
    private static final int FLAG_INCLUDE_GLOBAL_BBOXES = Constants.BIT_FLAG_0;
    private static final int FLAG_INCLUDE_PER_CEL_BBOXES = Constants.BIT_FLAG_1;
    private static final int FLAG_VALIDATION_MASK = FLAG_INCLUDE_PER_CEL_BBOXES | FLAG_INCLUDE_GLOBAL_BBOXES;

    public MRAnimatedMofModel(MRAnimatedMofModelSet set) {
        super(set.getGameInstance());
        this.parentModelSet = set;
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.parentModelSet.getLogger(), "modelId=" + getStaticModelID(), AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    /**
     * Gets the static model ID for the model. (staticMof index)
     */
    public int getStaticModelID() {
        int localIndex = this.parentModelSet.getModels().lastIndexOf(this);
        if (localIndex < 0) // Wasn't found in the model set.
            return localIndex;

        int baseIndex = 0;
        List<MRAnimatedMofModelSet> modelSets = this.parentModelSet.getParentMof().getModelSets();
        for (int i = 0; i < modelSets.size(); i++) {
            MRAnimatedMofModelSet modelSet = modelSets.get(i);
            if (modelSet == this.parentModelSet) {
                return baseIndex + localIndex;
            } else {
                baseIndex += modelSet.getModels().size();
            }
        }

        return -1; // Couldn't find the parent modelSet.
    }

    @Override
    public void load(DataReader reader) {
        int animationType = reader.readUnsignedShortAsInt();
        if (animationType != EXPECTED_ANIMATION_TYPE) // Never accessed by the game, and observed to always be one in game files.
            getLogger().severe("Invalid/unsupported MRMAnimatedMofModel animationType: %d (At %08X)", animationType, reader.getIndex() - Constants.SHORT_SIZE);

        int flags = reader.readUnsignedShortAsInt();
        warnAboutInvalidBitFlags(flags, FLAG_VALIDATION_MASK, "MRAnimatedMofModel");

        this.tempPartCount = reader.readUnsignedShortAsInt(); // Appears to match the real part count.
        int staticModelId = reader.readUnsignedShortAsInt();
        if (staticModelId != getStaticModelID())
            getLogger().severe("Invalid MRAnimatedMofModel Static Model ID! (Got %d, but expected %d!)", staticModelId, getStaticModelID());

        this.tempCelSetPointerAddress = reader.readInt(); // Right after BBOX
        this.tempBBoxPointerAddress = reader.readInt(); // Right after model data.
        this.tempBBoxSetPointerAddress = reader.readInt(); // Almost unused. This points to the modelSet data.
        int constraintPointer = reader.readInt(); // Unused.

        // While there is a struct definition for such a constraint, it has never been seen before.
        // Also, the game code never checks this field/it goes unused.
        if (constraintPointer != 0)
            throw new RuntimeException("MRAnimatedMofModel had a constraint which was not null! (" + NumberUtils.toHexString(constraintPointer) + ")");

        // Setup bounding box.
        this.boundingBox = ((flags & FLAG_INCLUDE_GLOBAL_BBOXES) == FLAG_INCLUDE_GLOBAL_BBOXES) ? new MRMofBoundingBox() : null;

        // This is used iff the flag PERCEL_BBOXES_INCLUDED is set. This has been found in MediEvil ECTS, but pretty much nowhere else.
        // If that flag is set, MRAnimDisplayMeshInstance will check if the bounding box is on-screen, and if it isn't, rendering will be skipped.
        // In other words, this would have been used for culling.
        boolean hasPerCelBoundingBoxFlag = (flags & FLAG_INCLUDE_PER_CEL_BBOXES) == FLAG_INCLUDE_PER_CEL_BBOXES;
        if (this.tempBBoxSetPointerAddress != 0 && hasPerCelBoundingBoxFlag) { // It's good to be aware of rare features, especially when we add new builds.
            getLogger().info("Found usage of a rare feature: MRMofBoundingBoxSet!");
        } else if ((this.tempBBoxSetPointerAddress != 0) ^ hasPerCelBoundingBoxFlag) { // I don't believe this ever happens.
            getLogger().severe("MRAnimatedMofModel disagreed between the bboxSetPointer and the perCel flag configuration! (%08X vs %b)", this.tempBBoxSetPointerAddress, hasPerCelBoundingBoxFlag);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(EXPECTED_ANIMATION_TYPE);
        writer.writeUnsignedShort(generateBitFlags());
        writer.writeUnsignedShort(getStaticMof().getParts().size()); // Appears to match the real part count.
        writer.writeUnsignedShort(getStaticModelID());

        this.tempCelSetPointerAddress = writer.writeNullPointer(); // Pointer goes right after BBOX?
        this.tempBBoxPointerAddress = writer.writeNullPointer();
        this.tempBBoxSetPointerAddress = writer.writeNullPointer(); // bboxSetPointer
        writer.writeNullPointer(); // constraint
    }

    /**
     * Generate an integer with the bit flags of this model.
     */
    private int generateBitFlags() {
        int flags = 0;
        if (this.boundingBox != null)
            flags |= FLAG_INCLUDE_GLOBAL_BBOXES;
        if (this.boundingBoxSet != null)
            flags |= FLAG_INCLUDE_PER_CEL_BBOXES;

        return flags;
    }

    /**
     * Gets the static mof which this model corresponds to.
     * @return staticMof
     */
    public MRStaticMof getStaticMof() {
        int staticMofId = getStaticModelID();
        List<MRStaticMof> staticMofs = this.parentModelSet.getParentMof().getStaticMofs();
        if (staticMofId < 0 || staticMofId >= staticMofs.size())
            throw new RuntimeException("The static model ID was " + staticMofId + ", which did not correspond to any of the available " + staticMofs.size() + " static mof(s).");

        return staticMofs.get(staticMofId);
    }

    /**
     * Returns true iff the active format version uses an empty bounding box pointer.
     * Beast Wars, and MediEvil all use the empty bounding box pointer. (Confirmed)
     * Most likely Frogger had a separate MOF export tool specific to it.
     */
    public boolean doesVersionFormatUseEmptyBoundingBoxPointer() {
        if (getGameInstance().isFrogger()) {
            FroggerConfig config = (FroggerConfig) getConfig();
            return config.isSonyPresentation() || config.isPSXAlpha();
        } else {
            // It is unknown how games before Frogger worked, since Old Frogger is the only game we have, and it has no XAR models.
            return getGameInstance().getGameType().isAfter(SCGameType.FROGGER);
        }
    }

    /**
     * Reads body data from the reader.
     * @param reader the reader to read the body data from
     */
    void readBodyData(DataReader reader) {
        if (this.tempBBoxPointerAddress < 0)
            throw new RuntimeException("load(DataReader) has not been called yet, so there is no need/ability to resolve the BBOX.");

        int bboxPointer = this.tempBBoxPointerAddress;
        this.tempBBoxPointerAddress = -1;

        // Read bounding box data. (The bounding box pointer is only set on the E3 build.)
        if (this.boundingBox != null) {
            // TODO: Better support...!!!
            requireReaderIndex(reader, bboxPointer, "Expected BBOX Data");
            this.boundingBox.load(reader); // Unused.
        } else if (!doesVersionFormatUseEmptyBoundingBoxPointer()) {
            requireReaderIndex(reader, bboxPointer, "Expected BBOX Data");
        }
    }

    /**
     * Writes the celSet pointer, if there is a position saved to save it to.
     * @param writer The DataWriter to write to.
     */
    void writeBodyData(DataWriter writer) {
        if (this.tempBBoxPointerAddress < 0)
            throw new RuntimeException("save(DataWriter) has not been called yet, so there is no need/ability to write the celSetPointer.");

        int bboxPointer = this.tempBBoxPointerAddress;
        this.tempBBoxPointerAddress = -1;

        // Write BBOX
        if (this.boundingBox != null) {
            // TODO: Better support...!!!
            writer.writeAddressTo(bboxPointer);
            this.parentModelSet.getParentMof().makeBoundingBox().save(writer);
        } else if (!doesVersionFormatUseEmptyBoundingBoxPointer()) {
            writer.writeAddressTo(bboxPointer);
        }
    }

    /**
     * Reads/resolves the bounding box set for this model, if there is one.
     * @param boundingBoxSets A map of the index a bounding box set was found to the loaded bounding box set.
     */
    void readBoundingBoxSet(Map<Integer, MRMofBoundingBoxSet> boundingBoxSets) {
        if (this.tempBBoxSetPointerAddress < 0)
            throw new RuntimeException("load(DataReader) has not been called yet, so there is no need/ability to resolve the bboxSetPointer.");

        int bboxSetPointer = this.tempBBoxSetPointerAddress;
        this.tempBBoxSetPointerAddress = -1;

        if (bboxSetPointer > 0) {
            this.boundingBoxSet = boundingBoxSets.get(bboxSetPointer);
            if (this.boundingBoxSet == null)
                throw new RuntimeException("Could not resolve MRMofBoundingBoxSet at 0x" + NumberUtils.toHexString(bboxSetPointer) + ".");
        } else {
            this.boundingBoxSet = null;
        }
    }

    /**
     * Writes the pointer to the bounding box set for this model, if there is one.
     * @param writer The writer to write the data with
     * @param boundingBoxSetPointer The pointer to the bounding box set, or 0 if there is none
     */
    void writeBoundingBoxSet(DataWriter writer, int boundingBoxSetPointer) {
        if (this.tempBBoxSetPointerAddress < 0)
            throw new RuntimeException("save(DataWriter) has not been called yet, so there is no need/ability to write the bboxSetPointer.");

        writer.writeIntAtPos(this.tempBBoxSetPointerAddress, boundingBoxSetPointer);
        this.tempBBoxSetPointerAddress = -1;
    }

    /**
     * Ensures the celSetPointer is shared with the parent model set.
     * @param realCelSetPointer the pointer seen by the modelSet.
     */
    void validateCelSetPointer(int realCelSetPointer) {
        if (this.tempCelSetPointerAddress < 0)
            throw new RuntimeException("load(DataReader) has not been called yet, so there is no need/ability to validate the celSetPointer.");

        int tempCelSetPointer = this.tempCelSetPointerAddress;
        this.tempCelSetPointerAddress = -1;
        if (tempCelSetPointer != realCelSetPointer)
            throw new RuntimeException("The MRAnimatedMofModel thought the celSet was at " + NumberUtils.toHexString(tempCelSetPointer) + ", but the MRAnimatedMofModelSet thought it was at " + NumberUtils.toHexString(realCelSetPointer) + ".");
    }

    /**
     * Writes the celSet pointer, if there is a position saved to save it to.
     * @param writer The DataWriter to write to.
     * @param celSetPointer The pointer to celSet data to write.
     */
    void writeCelSetPointer(DataWriter writer, int celSetPointer) {
        if (this.tempCelSetPointerAddress < 0)
            throw new RuntimeException("save(DataWriter) has not been called yet, so there is no need/ability to write the celSetPointer.");

        writer.writeIntAtPos(this.tempCelSetPointerAddress, celSetPointer);
        this.tempCelSetPointerAddress = -1;
    }

    /**
     * Ensures the partCount is shared with the parent model set.
     */
    void validatePartCount() {
        if (this.tempPartCount < 0)
            throw new RuntimeException("load(DataReader) has not been called yet, so there is no need/ability to validate the partCount.");

        int tempPartCount = this.tempPartCount;
        this.tempPartCount = -1;
        MRStaticMof staticMof = getStaticMof();
        if (tempPartCount != staticMof.getParts().size())
            throw new RuntimeException("The MRAnimatedMofModel thought the partCount was " + tempPartCount+ ", but the static mof actually had a part count of " + staticMof.getParts().size() + ".");
    }
}