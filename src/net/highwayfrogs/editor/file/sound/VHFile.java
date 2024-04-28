package net.highwayfrogs.editor.file.sound;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses VH files.
 * Created by rdrpenguin04 on 8/22/2018.
 */
@Getter
public class VHFile extends VHAudioHeader {
    private final List<AudioHeader> entries = new ArrayList<>();
    public static final int CHANNEL_COUNT = 1;

    public VHFile(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int numEntries = reader.readInt();
        for (int i = 0; i < numEntries; i++) {
            AudioHeader entry = new AudioHeader();
            entry.load(reader);
            getEntries().add(entry);
        }

        if (getVbFile() != null)
            getVbFile().setHeader(this);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getEntries().size());

        int offset = 0;
        for (AudioHeader entry : getEntries()) {
            entry.setDataStartOffset(offset);
            entry.save(writer);
            if (entry.isAudioPresent())
                offset += entry.getDataSize();
        }
    }

    @Setter
    @Getter
    public static class AudioHeader extends GameObject {
        private boolean audioPresent;
        private int dataStartOffset;
        private int dataSize;
        private int sampleRate;
        private int bitWidth;

        private static final int HAS_AUDIO = 1;
        private static final int UNKNOWN_VALUE = 1;

        @Override
        public void load(DataReader reader) {
            this.audioPresent = (reader.readInt() == HAS_AUDIO);
            this.dataStartOffset = reader.readInt();
            this.dataSize = reader.readInt();

            int unk1 = reader.readInt();
            Utils.verify(unk1 == UNKNOWN_VALUE, "Unknown Value #1 was not correct. (%d)", unk1);
            int unk2 = reader.readInt();
            Utils.verify(unk2 == UNKNOWN_VALUE, "Unknown Value #2 was not correct. (%d)", unk2);
            this.sampleRate = reader.readInt();
            this.bitWidth = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.audioPresent ? HAS_AUDIO : 0);
            writer.writeInt(this.dataStartOffset);
            writer.writeInt(this.dataSize);
            writer.writeInt(UNKNOWN_VALUE);
            writer.writeInt(UNKNOWN_VALUE);
            writer.writeInt(this.sampleRate);
            writer.writeInt(this.bitWidth);
        }

        /**
         * Get the byte-width for this sound.
         * @return byteWidth
         */
        public int getByteWidth() {
            return getBitWidth() / Constants.BITS_PER_BYTE;
        }

        @Override
        public String toString() {
            return "[Data: (" + Utils.toHexString(dataStartOffset) + "->" + Utils.toHexString(dataStartOffset + dataSize)
                    + "), Sample Rate: " + sampleRate + ", Bit-Width: " + this.bitWidth + ", Has Audio: " + audioPresent + "]";
        }

    }
}