package net.highwayfrogs.editor.games.konami.greatquest.toc;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * An animation "track" (think of timelines in video editing software).
 * Created by Kneesnap on 4/16/2024.
 */
@Getter
public class kcCResourceTrack extends kcCResource implements IMultiLineInfoWriter {
    private final List<kcTrack> tracks = new ArrayList<>();

    public kcCResourceTrack(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.TRACK);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        reader.verifyString(KCResourceID.TRACK.getSignature()); // For some reason this is here again.
        reader.skipInt(); // Skip the size.

        if (reader.getIndex() != kcTrack.BYTE_OFFSET)
            throw new RuntimeException("The reader index (" + reader.getIndex() + ") needs to match the expected byte offset, otherwise the offset calculations will fail!");

        this.tracks.clear();
        while (reader.hasMore()) {
            kcTrack newTrack = new kcTrack(this);
            newTrack.load(reader);
            this.tracks.add(newTrack);
        }
    }

    @Override
    public void save(DataWriter writer) {
        int dataStartAddress = writer.getIndex();
        super.save(writer);
        writer.writeStringBytes(KCResourceID.TRACK.getSignature());
        int dataSizeAddress = writer.writeNullPointer();
        for (int i = 0; i < this.tracks.size(); i++)
            this.tracks.get(i).save(writer);

        // Ensure we get the size right.
        writer.writeAddressAt(dataSizeAddress, writer.getIndex() - dataStartAddress);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append("kcCResourceTrack['").append(getName()).append("'/").append(Utils.to0PrefixedHexString(getHash())).append(", Tracks: ").append(this.tracks.size()).append("]:");
        String newPadding = padding + " ";
        for (int i = 0; i < this.tracks.size(); i++) {
            builder.append(Constants.NEWLINE).append(padding);
            this.tracks.get(i).writeMultiLineInfo(builder, newPadding);
        }
    }

    public static class kcTrack extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter {
        private final kcCResourceTrack resourceTrack;
        private int value;
        private int tag;
        private int size;
        private byte[] unknownData; // TODO: Figure out how to handle this data.

        public static final int FLAG_IS_TRACKED = Constants.BIT_FLAG_23;
        private static final int BYTE_OFFSET = 40; // 32 bytes for name, and 8 for the file ID + size.

        public kcTrack(kcCResourceTrack resourceTrack) {
            super(resourceTrack.getGameInstance());
            this.resourceTrack = resourceTrack;
        }

        @Override
        public void load(DataReader reader) {
            this.value = reader.readInt();
            this.tag = reader.readInt();
            this.size = reader.readInt(); // TODO: Key Count
            reader.skipPointer(); // Runtime pointer.
            int nextTrackAddress = reader.readInt();
            this.unknownData = reader.readBytes(nextTrackAddress != 0 ? (nextTrackAddress + BYTE_OFFSET - reader.getIndex()) : reader.getRemaining());
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.value);
            writer.writeInt(this.tag);
            writer.writeInt(this.size);
            writer.writeNullPointer(); // Runtime pointer.
            int nextTrackAddress = writer.writeNullPointer();
            if (this.unknownData != null)
                writer.writeBytes(this.unknownData);

            // Write the pointer of the next track unless this is the last track.
            if (this.resourceTrack.getTracks().size() > 0 && this.resourceTrack.getTracks().get(this.resourceTrack.getTracks().size() - 1) != this)
                writer.writeAddressAt(nextTrackAddress, writer.getIndex() - BYTE_OFFSET);
        }

        /**
         * Gets the track type.
         */
        public int getTrackType() {
            return this.value >>> 24; // 8 bits
        }

        /**
         * Gets the track mode
         */
        public int getTrackMode() {
            return this.value & 0x1ffff; // 17 bits
        }

        @Override
        public void writeMultiLineInfo(StringBuilder builder, String padding) {
            builder.append("Track{Value=").append(this.value).append(",Tag=").append(this.tag).append(",Size=").append(this.size).append(",UnknownData=").append(this.unknownData.length).append(" Bytes}");
        }
    }
}