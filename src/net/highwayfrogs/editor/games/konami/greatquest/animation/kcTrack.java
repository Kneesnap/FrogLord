package net.highwayfrogs.editor.games.konami.greatquest.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKey;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTrack;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an animation track.
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
public class kcTrack extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
    private final kcCResourceTrack parentResource;
    private int packedValue; // This value contains the track mode, bit flags, and the kcControlType.
    private int tag; // This directly corresponds with the 'tag' value in a kcCSkeleton.
    private final List<kcTrackKey<?>> keyList = new ArrayList<>();

    private static final int TRACK_MODE_SIZE = 17;
    private static final int TRACK_MODE_MASK = (1 << TRACK_MODE_SIZE) - 1;
    private static final int BIT_FLAG_SIZE = 7;
    private static final int BIT_FLAG_MASK = (1 << BIT_FLAG_SIZE) - 1;
    private static final int TRACK_CONTROL_SIZE = 8;
    private static final int TRACK_CONTROL_MASK = (1 << TRACK_CONTROL_SIZE) - 1;
    private static final int TRACK_CONTROL_SHIFT = TRACK_MODE_SIZE + BIT_FLAG_SIZE;

    // NOTE: TRACK MODE ->
    //  - 0x2 is a bit flag controlling kcTrackTcbPreparePnt3()
    //  - _kcControl->flags |= kcTrackGetMode() in StdControlInit(), not sure what this is used for.
    //  - Only observed as being 0, 1, or 2. But, it seems like often times this value is ignored by the game code, suggesting to me it's some kind of independent flag. They may be bit flags, as the bit 2 is usually tested.

    // kcTrackFixup() can set this flag, but it has always been observed to be set already in the raw data.
    // This flag being set means that there is another track after this one. So, all tracks will contain this flag except the last flag in the file.
    public static final int FLAG_HAS_NEXT = Constants.BIT_FLAG_4; // 0x200000 = 1 << (17 + 4)

    // kcTrackFixup() sets this flag, but only on the first track.
    // Thus, the flag is likely an indicator that it is the first track.
    // It appears to be already set when loaded, even though the game will assign this flag if it is missing.
    public static final int FLAG_IS_FIRST = Constants.BIT_FLAG_5; // 0x400000 = 1 << (17 + 5)

    // This flag means kcTrackPrepare() should call kcTrackFixup() This is automatically applied to ALL tracks by the game.
    // However, as the only call to kcTrackPrepare() is by kcCResourceTrack::Prepare(), I believe all tracks in the game have this flag.
    // Every subsequent track will have this flag applied by kcTrackFixup().
    // It does not appear this flag is ever checked, also, it appears this flag is NOT set in the raw game files, ie: it's probably runtime only now.
    public static final int FLAG_IS_PACKED = Constants.BIT_FLAG_6; // 0x800000 = 1 << (17 + 6)
    private static final int FLAG_VALIDATION_MASK = FLAG_IS_PACKED | FLAG_IS_FIRST | FLAG_HAS_NEXT; // 0b1110000

    public kcTrack(kcCResourceTrack parentResource) {
        super(parentResource.getGameInstance());
        this.parentResource = parentResource;
    }

    @Override
    public ILogger getLogger() {
        return this.parentResource != null ? this.parentResource.getLogger() : super.getLogger();
    }

    @Override
    public void load(DataReader reader) {
        load(reader, 0);
    }

    /**
     * Loads the kcTrack data from the origin.
     * @param reader The reader to read information from.
     * @param baseOffset The offset to the pointer origin.
     */
    public void load(DataReader reader, int baseOffset) {
        this.packedValue = reader.readInt();
        this.tag = reader.readInt();
        int keyListSize = reader.readInt();
        reader.skipPointer(); // Runtime pointer.
        int nextTrackAddress = reader.readInt();

        // Read key list.
        int keyListOffsetStartIndex = reader.getIndex();
        int keyListDataByteLength = nextTrackAddress != 0 ? (nextTrackAddress + baseOffset - keyListOffsetStartIndex) : reader.getRemaining();
        int keyListDataStartIndex = keyListOffsetStartIndex + (Constants.POINTER_SIZE * keyListSize);
        int keyListDataEndIndex = keyListOffsetStartIndex + keyListDataByteLength;

        this.keyList.clear();
        kcControlType controlType = getTrackControlType();
        int lastTick = Integer.MIN_VALUE; // There are actually some less than zero in the PS2 NTSC version.
        int lastEndPosition = -1;
        for (int i = 0; i < keyListSize; i++) {
            int dataOffset = reader.readInt();

            int readStartIndex = keyListDataStartIndex + dataOffset;
            reader.jumpTemp(readStartIndex);
            if (lastEndPosition >= 0 && readStartIndex != lastEndPosition)
                getLogger().severe("The ending position of %s was expected to be 0x%X, but was actually 0x%X.", this.keyList.get(this.keyList.size() - 1), readStartIndex, lastEndPosition);

            kcTrackKey<?> newKey = controlType.createKey(getGameInstance());
            newKey.load(reader);
            this.keyList.add(newKey);
            lastEndPosition = reader.getIndex();
            reader.jumpReturn();

            if (lastTick == newKey.getTick()) {
                throw new RuntimeException("There were two kcTrackKey objects using tick " + lastTick + ".");
            } else if (lastTick > newKey.getTick()) {
                throw new RuntimeException("There was a kcTrackKey at tick " + newKey.getTick() + ", located after a key at tick " + lastTick + ".");
            }

            lastTick = newKey.getTick();
        }

        if (lastEndPosition >= 0 && lastEndPosition != keyListDataEndIndex)
            getLogger().severe("The ending position of the keyList data (%s) was expected to be 0x%X, but we actually stopped reading at 0x%X.", this.keyList.get(this.keyList.size() - 1), keyListDataEndIndex, lastEndPosition);
        reader.setIndex(keyListDataEndIndex);

        // Validate flags.
        // FLAG_HAS_NEXT and FLAG_IS_FIRST are validated in the parent object, as this object is unaware of its ordering.
        warnAboutInvalidBitFlags(getFlags(), FLAG_VALIDATION_MASK);
        if ((getFlags() & FLAG_IS_PACKED) != FLAG_IS_PACKED)
            getLogger().warning("Encountered a kcTrack which had the IS_PACKED flag set! (This has never been observed before)");

        int mode = getTrackMode();
        if (mode != 0 && mode != 1 && mode != 2) // 17 is seen in the PS2 NTSC prototype.
            getLogger().severe("Encountered unexpected track mode of: %s", mode);
    }

    @Override
    public void save(DataWriter writer) {
        save(writer, 0, false, false);
    }

    /**
     * Saves the kcTrack data.
     * @param writer The writer to save information into.
     * @param baseOffset The offset to the pointer origin.
     * @param isFirstTrack Whether this is the first track in the file.
     * @param hasNextTrack Whether there is another track after this one.
     */
    public void save(DataWriter writer, int baseOffset, boolean isFirstTrack, boolean hasNextTrack) {
        updateFlags(isFirstTrack, hasNextTrack); // Ensure the flags are updated.

        writer.writeInt(this.packedValue);
        writer.writeInt(this.tag);
        writer.writeInt(this.keyList.size());
        writer.writeNullPointer(); // Runtime pointer.
        int nextTrackAddress = writer.writeNullPointer();

        // Write key list.
        int keyListOffsetStartIndex = writer.getIndex();
        for (int i = 0; i < this.keyList.size(); i++)
            writer.writeNullPointer();

        int lastTick = Integer.MIN_VALUE;
        int keyListDataStartIndex = writer.getIndex();
        for (int i = 0; i < this.keyList.size(); i++) {
            // Write offset in table.
            writer.writeIntAtPos(keyListOffsetStartIndex + (i * Constants.POINTER_SIZE), writer.getIndex() - keyListDataStartIndex);

            // Write key data.
            kcTrackKey<?> key = this.keyList.get(i);
            key.save(writer);

            if (lastTick > key.getTick())
                throw new RuntimeException("The kcTrack is not sorted by tick!!");

            lastTick = key.getTick();
        }

        // Write the pointer of the next track unless this is the last track.
        if (this.parentResource.getTracks().size() > 0 && this.parentResource.getTracks().get(this.parentResource.getTracks().size() - 1) != this)
            writer.writeIntAtPos(nextTrackAddress, writer.getIndex() - baseOffset);
    }

    /**
     * Gets the track key which covers the given tick
     * @param tick the tick to find a key for
     * @return indexOfKeyCoveringTick, if there is one
     */
    public int getKeyIndexForTick(int tick) {
        int left = 0;
        int right = this.keyList.size() - 1;
        while (left <= right) {
            int mid = (left + right) / 2;
            kcTrackKey<?> midKey = this.keyList.get(mid);
            int nextKeyTick = this.keyList.size() > mid + 1 ? this.keyList.get(mid + 1).getTick() : Integer.MAX_VALUE;

            if (tick >= midKey.getTick() && (tick < nextKeyTick)) {
                return mid;
            } else if (nextKeyTick > tick) {
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return -1;
    }

    /**
     * Gets the track type.
     */
    public kcControlType getTrackControlType() {
        return kcControlType.values()[(this.packedValue >>> TRACK_CONTROL_SHIFT) & TRACK_CONTROL_MASK];
    }

    /**
     * Gets the track mode, which appears to be used during track preparation, or something.
     * I believe this makes it some kind of smoothing behavior, but I am not sure as we didn't need to implement this in FrogLord.
     */
    public int getTrackMode() {
        return this.packedValue & TRACK_MODE_MASK;
    }

    /**
     * Gets the track flags.
     */
    public int getFlags() {
        return (this.packedValue >>> TRACK_MODE_SIZE) & BIT_FLAG_MASK;
    }

    /**
     * Sets the bit flags for this track.
     * NOTE: The user will never need to see/modify these flags, as their behavior has been completely automated by FrogLord.
     * @param newFlags The new flag bits to set
     */
    private void setFlags(int newFlags) {
        if ((newFlags & BIT_FLAG_MASK) != newFlags)
            throw new IllegalArgumentException("The bit flags " + NumberUtils.toHexString(newFlags) + " contained bits outside of the flag mask of " + BIT_FLAG_MASK);

        this.packedValue = (this.packedValue & ~(BIT_FLAG_MASK << TRACK_MODE_SIZE)) | (newFlags << TRACK_MODE_SIZE);
    }

    private void updateFlags(boolean isFirstTrack, boolean hasNextTrack) {
        int newFlags = getFlags();
        if (hasNextTrack) {
            newFlags |= FLAG_HAS_NEXT;
        } else {
            newFlags &= ~FLAG_HAS_NEXT;
        }

        if (isFirstTrack) {
            newFlags |= FLAG_IS_FIRST;
        } else {
            newFlags &= ~FLAG_IS_FIRST;
        }

        setFlags(newFlags);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding);
        builder.append("Track{Type=").append(getTrackControlType()).append(",Flags=").append(NumberUtils.toHexString(getFlags())).append(",Mode=").append(getTrackMode()).append(",Tag=").append(this.tag).append(",Keys=").append(this.keyList.size()).append("}");
        builder.append(Constants.NEWLINE);

        // Write keyList
        String newPadding = padding + " ";
        for (int i = 0; i < this.keyList.size(); i++) {
            builder.append(newPadding);
            this.keyList.get(i).writeInfo(builder);
            builder.append(Constants.NEWLINE);
        }
    }
}
