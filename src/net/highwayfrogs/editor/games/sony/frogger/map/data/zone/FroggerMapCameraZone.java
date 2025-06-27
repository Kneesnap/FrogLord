package net.highwayfrogs.editor.games.sony.frogger.map.data.zone;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.FroggerOffsetVectorType;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.function.Predicate;

/**
 * Holds data for a FroggerMapCameraZone.
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public class FroggerMapCameraZone extends FroggerMapZone {
    private short flags;
    @Setter private FroggerCameraRotation forcedCameraDirection; // Null is allowed.
    private final SVector northSourceOffset = new SVector();
    private final SVector northTargetOffset = new SVector();
    private final SVector eastSourceOffset = new SVector();
    private final SVector eastTargetOffset = new SVector();
    private final SVector southSourceOffset = new SVector();
    private final SVector southTargetOffset = new SVector();
    private final SVector westSourceOffset = new SVector();
    private final SVector westTargetOffset = new SVector();

    public static final int BYTE_SIZE = (2 * Constants.SHORT_SIZE) + (8 * SVector.PADDED_BYTE_SIZE);

    public FroggerMapCameraZone(FroggerMapFile mapFile) {
        super(mapFile, FroggerMapZoneType.CAMERA);
    }

    @Override
    protected void loadExtensionData(DataReader reader) {
        this.flags = reader.readShort();
        warnAboutInvalidBitFlags(this.flags, FroggerMapCameraZoneFlag.FLAG_VALIDATION_MASK);
        this.forcedCameraDirection = FroggerCameraRotation.getCameraRotationFromID(reader.readShort());
        this.northSourceOffset.loadWithPadding(reader);
        this.northTargetOffset.loadWithPadding(reader);
        this.eastSourceOffset.loadWithPadding(reader);
        this.eastTargetOffset.loadWithPadding(reader);
        this.southSourceOffset.loadWithPadding(reader);
        this.southTargetOffset.loadWithPadding(reader);
        this.westSourceOffset.loadWithPadding(reader);
        this.westTargetOffset.loadWithPadding(reader);

        if (!testFlag(FroggerMapCameraZoneFlag.ENABLE_PER_DIRECTION_OFFSETS) && !areCameraOffsetsEqual())
            getLogger().warning("The camera zone flags are behaving unexpectedly!");
        if (testFlag(FroggerMapCameraZoneFlag.OUTRO))
            getLogger().warning("The camera zone has the OUTRO flag set!");

        // This is rare but does happen.
        /*if (testFlag(FroggerMapCameraZoneFlag.SEMI_FORCED) && this.forcedCameraDirection == null)
            getLogger().warning("The camera zone has the SEMI_FORCED flag set, but there is no forced camera direction!");*/
    }

    @Override
    protected void saveExtensionData(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeShort(FroggerCameraRotation.getCameraRotationID(this.forcedCameraDirection));
        this.northSourceOffset.saveWithPadding(writer);
        this.northTargetOffset.saveWithPadding(writer);
        this.eastSourceOffset.saveWithPadding(writer);
        this.eastTargetOffset.saveWithPadding(writer);
        this.southSourceOffset.saveWithPadding(writer);
        this.southTargetOffset.saveWithPadding(writer);
        this.westSourceOffset.saveWithPadding(writer);
        this.westTargetOffset.saveWithPadding(writer);
    }

    @Override
    public FroggerMapZone clone(FroggerMapFile mapFile) {
        return DataUtils.cloneSerializableObject(this, new FroggerMapCameraZone(mapFile));
    }

    /**
     * Gets the camera offset for the given camera rotation / vector type combination.
     * @param rotation the desired vector's camera rotation
     * @param vectorType the desired vector's offset type
     * @return cameraOffset
     */
    public SVector getCameraOffset(FroggerCameraRotation rotation, FroggerOffsetVectorType vectorType) {
        if (rotation == null)
            throw new NullPointerException("rotation");
        if (vectorType == null)
            throw new NullPointerException("vectorType");

        // Reduce the vector type down to a boolean.
        boolean sourceOffset;
        switch (vectorType) {
            case SOURCE:
                sourceOffset = true;
                break;
            case TARGET:
                sourceOffset = false;
                break;
            default:
                throw new RuntimeException("Unsupported FroggerOffsetVectorType: " + vectorType);
        }

        // Return the target camera offset.
        switch (rotation) {
            case NORTH:
                return sourceOffset ? this.northSourceOffset : this.northTargetOffset;
            case EAST:
                return sourceOffset ? this.eastSourceOffset : this.eastTargetOffset;
            case SOUTH:
                return sourceOffset ? this.southSourceOffset : this.southTargetOffset;
            case WEST:
                return sourceOffset ? this.westSourceOffset : this.westTargetOffset;
            default:
                throw new RuntimeException("Unsupported FroggerCameraRotation: " + vectorType);
        }
    }

    /**
     * Test if the offsets for a particular camera rotation are disabled (match the single enabled offset direction)
     * @param rotation the rotation to test
     * @return true iff the camera rotation offset is disabled
     */
    public boolean isCameraRotationOffsetDisabled(FroggerCameraRotation rotation) {
        if (rotation == null)
            throw new NullPointerException("rotation");

        if (testFlag(FroggerMapCameraZoneFlag.ENABLE_PER_DIRECTION_OFFSETS) && (this.forcedCameraDirection == null || testFlag(FroggerMapCameraZoneFlag.SEMI_FORCED)))
            return false; // The flags are setup in a way that no offsets are disabled.

        FroggerCameraRotation enabledDirection = this.forcedCameraDirection != null ? this.forcedCameraDirection : FroggerCameraRotation.NORTH;
        return rotation != enabledDirection;
    }

    /**
     * Test if this has a particular flag enabled.
     * @param flag The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(FroggerMapCameraZoneFlag flag) {
        return (this.flags & flag.getBitFlagMask()) == flag.getBitFlagMask();
    }

    /**
     * Set the flag state.
     * @param flag     The flag type.
     * @param newState The new state of the flag.
     * @return true iff the zone flags have changed in a way which changes which camera offsets are active
     */
    public boolean setFlag(FroggerMapCameraZoneFlag flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return false; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.flags |= flag.getBitFlagMask();
        } else {
            this.flags ^= flag.getBitFlagMask();
        }

        return flag == FroggerMapCameraZoneFlag.ENABLE_PER_DIRECTION_OFFSETS || (this.forcedCameraDirection != null && flag == FroggerMapCameraZoneFlag.SEMI_FORCED);
    }

    /**
     * Test if all camera offsets are equal.
     */
    public boolean areCameraOffsetsEqual() {
        return this.northSourceOffset.equals(this.southSourceOffset) && this.northTargetOffset.equals(this.southTargetOffset)
                && this.southSourceOffset.equals(this.eastSourceOffset) && this.southTargetOffset.equals(this.eastTargetOffset)
                && this.eastSourceOffset.equals(this.westSourceOffset) && this.eastTargetOffset.equals(this.westTargetOffset);
    }

    @Getter
    public enum FroggerMapCameraZoneFlag {
        ENABLE_PER_DIRECTION_OFFSETS(Constants.BIT_FLAG_0, "Multiple Offsets", "Enables unique camera settings for each camera direction."), // This flag has been guessed to be from mappy, this flag is seen in retail version maps.
        OUTRO(Constants.BIT_FLAG_1, "Outro", "[UNUSED] Special outro camera zone, doesn't seem to be used."),
        SEMI_FORCED(Constants.BIT_FLAG_2, "Lock Camera", "Prevents camera rotation", zone -> zone.getForcedCameraDirection() == null),
        ABSOLUTE_Y(Constants.BIT_FLAG_3, "Absolute Y", "Treat camera Y positions as absolute world coordinates instead of relative to the player.\nIn the original game, this is only used in Uncanny Crusher."),
        CHECKPOINT(Constants.BIT_FLAG_4, "No Chckpnt Zoom", "Skips camera zoom-in when the player collects a checkpoint.");

        private final short bitFlagMask;
        private final String displayName;
        private final String description;
        private final Predicate<FroggerMapCameraZone> isDisabledTester;
        public static final int FLAG_VALIDATION_MASK = 0b11111;

        FroggerMapCameraZoneFlag(int bitFlagMask, String displayName, String description) {
            this(bitFlagMask, displayName, description, null);
        }

        FroggerMapCameraZoneFlag(int bitFlagMask, String displayName, String description, Predicate<FroggerMapCameraZone> isDisabledTester) {
            this.bitFlagMask = (short) bitFlagMask;
            this.displayName = displayName;
            this.description = description;
            this.isDisabledTester = isDisabledTester;
        }

        /**
         * Tests if the flag is disabled for a particular zone.
         * @param zone the zone to test
         */
        public boolean isFlagDisabled(FroggerMapCameraZone zone) {
            return this.isDisabledTester != null && this.isDisabledTester.test(zone);
        }

        /**
         * Returns whether any display of the flag state should be inverted (due to using inverse language)
         */
        public boolean isDisplayInverted() {
            return this == SEMI_FORCED;
        }

        /**
         * Returns whether this camera zone flag is hidden from the user-interface when not set.
         */
        public boolean isDisplayHidden() {
            return this == OUTRO;
        }
    }
}