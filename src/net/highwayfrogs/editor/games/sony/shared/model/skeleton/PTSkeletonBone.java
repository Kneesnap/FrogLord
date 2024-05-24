package net.highwayfrogs.editor.games.sony.shared.model.skeleton;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.utils.Utils;

import java.util.logging.Logger;

/**
 * Represents a skeleton bone.
 * Created by Kneesnap on 5/15/2024.
 */
@Getter
public class PTSkeletonBone extends SCSharedGameData {
    private final PTSkeletonFile skeleton;
    private final SVector jointOffset = new SVector();

    public static final int FLAG_HAS_PREVIOUS_BONE = Constants.BIT_FLAG_0;
    public static final int FLAG_USE_LOCAL_TRANSLATION = Constants.BIT_FLAG_1;
    public static final int FLAG_USE_OFFSET_AS_SCALE = Constants.BIT_FLAG_2;
    public static final int FLAG_VALIDATION_MASK = 0b111;

    public PTSkeletonBone(PTSkeletonFile skeleton) {
        super(skeleton != null ? skeleton.getGameInstance() : null);
        this.skeleton = skeleton;
    }

    @Override
    public void load(DataReader reader) {
        this.jointOffset.loadWithPadding(reader);
        warnAboutInvalidBitFlags(getFlags(), FLAG_VALIDATION_MASK);
    }

    @Override
    public void save(DataWriter writer) {
        this.jointOffset.saveWithPadding(writer);
    }

    /**
     * Gets the previous bone in the skeleton hierarchy. (Only valid when flag is set)
     */
    public short getPreviousBoneID() {
        if (!testFlagMask(FLAG_HAS_PREVIOUS_BONE))
            throw new RuntimeException("Cannot get previous bone ID, since it does not exist.");

        return (short) (this.jointOffset.getPadding() & 0xFF);
    }

    /**
     * Sets the previous bone in the skeleton hierarchy. (Only valid when flag is set)
     */
    public void setPreviousBoneID(int previousBoneID) {
        if (previousBoneID < 0 || previousBoneID > 0xFF)
            throw new IllegalArgumentException("Invalid Bone ID: " + previousBoneID);

        this.jointOffset.setPadding((short) ((this.jointOffset.getPadding() & 0xFF00) | previousBoneID));
    }

    /**
     * Gets the previous bone in the skeleton hierarchy. (Only valid when flag is set)
     */
    public byte getFlags() {
        return (byte) ((this.jointOffset.getPadding() >>> 8) & 0xFF);
    }

    /**
     * Test if the given bits are set in the flags value.
     * @param flagMask the bits to test.
     * @return if all the bits were set.
     */
    public boolean testFlagMask(int flagMask) {
        return ((getFlags() & flagMask) == flagMask);
    }

    /**
     * Set the given bits in the flag value.
     * @param flagMask the bits to apply.
     * @param state if the value is true, the bits will be set to 1, otherwise 0.
     */
    public void setFlagMask(int flagMask, boolean state) {
        int oldFlags = getFlags();

        int newFlags;
        if (state) {
            newFlags = oldFlags | flagMask;
        } else {
            newFlags = oldFlags & ~flagMask;
        }

        this.jointOffset.setPadding((short) ((this.jointOffset.getPadding() & 0xFF) | (newFlags << 8)));
    }

    /**
     * Gets information used to identify the logger.
     */
    public String getLoggerInfo() {
        return this.skeleton != null ? this.skeleton.getFileDisplayName() + "|Bone=" + this.skeleton.getBones().indexOf(this) : Utils.getSimpleName(this);
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger(getLoggerInfo());
    }
}