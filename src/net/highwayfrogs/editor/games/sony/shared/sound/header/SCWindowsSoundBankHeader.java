package net.highwayfrogs.editor.games.sony.shared.sound.header;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.shared.sound.EditableAudioFormat;
import net.highwayfrogs.editor.games.shared.sound.ISoundSample;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBodyEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeaderEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCWindowsSoundBankHeader.SCWindowsSoundBankHeaderEntry;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Implements the Millennium Interactive sound bank header as seen on Windows.
 * Created by Kneesnap on 5/13/2024.
 */
@Getter
public class SCWindowsSoundBankHeader<TBodyEntry extends SCSplitSoundBankBodyEntry & ISoundSample> extends SCSplitSoundBankHeader<SCWindowsSoundBankHeaderEntry, TBodyEntry> {
    public static final int CHANNEL_COUNT = 1;

    public SCWindowsSoundBankHeader(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public boolean load(DataReader reader, SCSplitSoundBankBody<SCWindowsSoundBankHeaderEntry, TBodyEntry> other) {
        int numEntries = reader.readInt();
        getEntries().clear();
        for (int i = 0; i < numEntries; i++) {
            SCWindowsSoundBankHeaderEntry entry = new SCWindowsSoundBankHeaderEntry(this, i);
            entry.load(reader);
            getEntries().add(entry);
        }

        return true;
    }

    @Override
    public void save(DataWriter writer, SCSplitSoundBankBody<SCWindowsSoundBankHeaderEntry, TBodyEntry> other) {
        writer.writeInt(getEntries().size());

        int offset = 0;
        for (int i = 0; i < getEntries().size(); i++) {
            SCWindowsSoundBankHeaderEntry entry = getEntries().get(i);
            entry.setDataStartOffset(offset);
            entry.save(writer);
            if (entry.isAudioPresent())
                offset += entry.getDataSize();
        }
    }

    @Setter
    @Getter
    public static class SCWindowsSoundBankHeaderEntry extends SCSplitSoundBankHeaderEntry {
        private final EditableAudioFormat audioFormat;
        private boolean audioPresent;
        private int dataStartOffset;
        private int dataSize;

        private static final int HAS_AUDIO = 1;
        private static final int UNKNOWN_VALUE = 1;

        public SCWindowsSoundBankHeaderEntry(SCWindowsSoundBankHeader<?> header, int internalId) {
            super(header, internalId);
            this.audioFormat = new EditableAudioFormat(11025, 16, 1, true, false);
        }

        @Override
        public void load(DataReader reader) {
            this.audioPresent = (reader.readInt() == HAS_AUDIO);
            this.dataStartOffset = reader.readInt();
            this.dataSize = reader.readInt();

            if (!isOldFormat()) {
                int unk1 = reader.readInt();
                Utils.verify(unk1 == UNKNOWN_VALUE, "Unknown Value #1 was not correct. (%d)", unk1);
                int unk2 = reader.readInt();
                Utils.verify(unk2 == UNKNOWN_VALUE, "Unknown Value #2 was not correct. (%d)", unk2);
                this.audioFormat.setSampleRate(reader.readInt());
                this.audioFormat.setSampleSizeInBits(reader.readInt());
            }
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.audioPresent ? HAS_AUDIO : 0);
            writer.writeInt(this.dataStartOffset);
            writer.writeInt(this.dataSize);
            if (!isOldFormat()) {
                writer.writeInt(UNKNOWN_VALUE);
                writer.writeInt(UNKNOWN_VALUE);
                writer.writeInt((int) this.audioFormat.getSampleRate());
                writer.writeInt(this.audioFormat.getSampleSizeInBits());
            }
        }

        @Override
        public String toString() {
            return "[Data: (" + Utils.toHexString(this.dataStartOffset) + "->" + Utils.toHexString(this.dataStartOffset + this.dataSize)
                    + "), Sample Rate: " + (int) this.audioFormat.getSampleRate() + ", Bit-Width: " + this.audioFormat.getSampleSizeInBits() + ", Has Audio: " + this.audioPresent + "]";
        }

        /**
         * Returns true if this is the old format, seen in pre-recode Frogger's Milestone 3 PC build.
         * This format is not seen in the June 1997 PC build of Frogger, so it is likely this format was changed between the two builds.
         */
        public boolean isOldFormat() {
            return getGameInstance().getGameType().isAtOrBefore(SCGameType.OLD_FROGGER);
        }
    }
}