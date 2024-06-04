package net.highwayfrogs.editor.games.konami.greatquest.animation;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.key.kcTrackKey;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceTrack;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents an animation track.
 * Created by Kneesnap on 4/16/2024.
 */
public class kcTrack extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
    private final kcCResourceTrack parentResource;
    private int value;
    private int tag; // This directly corresponds with the 'tag' value in a kcCSkeleton.
    private final List<kcTrackKey> keyList = new ArrayList<>();

    // room for 4 more flags before bleeding into mode data.
    public static final int FLAG_HAS_NEXT = Constants.BIT_FLAG_21;
    public static final int FLAG_IS_FIXED = Constants.BIT_FLAG_22;
    public static final int FLAG_IS_PACKED = Constants.BIT_FLAG_23;

    public kcTrack(kcCResourceTrack parentResource) {
        super(parentResource.getGameInstance());
        this.parentResource = parentResource;
    }

    @Override
    public Logger getLogger() {
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
        this.value = reader.readInt();
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
        int lastEndPosition = -1;
        for (int i = 0; i < keyListSize; i++) {
            int dataOffset = reader.readInt();

            int readStartIndex = keyListDataStartIndex + dataOffset;
            reader.jumpTemp(readStartIndex);
            if (lastEndPosition >= 0 && readStartIndex != lastEndPosition)
                getLogger().severe("The ending position of " + this.keyList.get(this.keyList.size() - 1) + " was expected to be " + Utils.toHexString(readStartIndex) + ", but was actually " + Utils.toHexString(lastEndPosition) + ".");

            kcTrackKey newKey = controlType.createKey(getGameInstance());
            newKey.load(reader);
            this.keyList.add(newKey);
            lastEndPosition = reader.getIndex();
            reader.jumpReturn();
        }

        if (lastEndPosition >= 0 && lastEndPosition != keyListDataEndIndex)
            getLogger().severe("The ending position of the keyList data (" + this.keyList.get(this.keyList.size() - 1) + ") was expected to be " + Utils.toHexString(keyListDataEndIndex) + ", but we actually stopped reading at " + Utils.toHexString(lastEndPosition) + ".");
        reader.setIndex(keyListDataEndIndex);
    }

    @Override
    public void save(DataWriter writer) {
        save(writer, 0);
    }

    /**
     * Saves the kcTrack data.
     * @param writer The writer to save information into.
     * @param baseOffset The offset to the pointer origin.
     */
    public void save(DataWriter writer, int baseOffset) {
        writer.writeInt(this.value);
        writer.writeInt(this.tag);
        writer.writeInt(this.keyList.size());
        writer.writeNullPointer(); // Runtime pointer.
        int nextTrackAddress = writer.writeNullPointer();

        // Write key list.
        int keyListOffsetStartIndex = writer.getIndex();
        for (int i = 0; i < this.keyList.size(); i++)
            writer.writeNullPointer();

        int keyListDataStartIndex = writer.getIndex();
        for (int i = 0; i < this.keyList.size(); i++) {
            // Write offset in table.
            writer.writeAddressAt(keyListOffsetStartIndex + (i * Constants.POINTER_SIZE), writer.getIndex() - keyListDataStartIndex);

            // Write key data.
            this.keyList.get(i).save(writer);
        }

        // Write the pointer of the next track unless this is the last track.
        if (this.parentResource.getTracks().size() > 0 && this.parentResource.getTracks().get(this.parentResource.getTracks().size() - 1) != this)
            writer.writeAddressAt(nextTrackAddress, writer.getIndex() - baseOffset);
    }

    /**
     * Gets the track type.
     */
    public kcControlType getTrackControlType() {
        return kcControlType.values()[this.value >>> 24]; // 8 bits
    }

    /**
     * Gets the track mode.
     */
    public int getTrackMode() {
        return this.value & 0x1ffff; // 17 bits
    }

    /**
     * Gets the track flags.
     */
    public int getTrackFlags() {
        return (this.value & 0x00FE0000); // 7 bits.
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding);
        builder.append("Track{Type=").append(getTrackControlType()).append(",Flags=").append(Utils.toHexString(getTrackFlags())).append(",Mode=").append(getTrackMode()).append(",Tag=").append(this.tag).append(",Keys=").append(this.keyList.size()).append("}");
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
