package net.highwayfrogs.editor.games.sony.shared.sound.header;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeaderEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationSoundBankBody.SCPlayStationVabSound;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationMinimalSoundBankHeader.SCPlayStationMinimalSoundBankHeaderEntry;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a minimal header seen in later Sony Cambridge games.
 * Created by Kneesnap on 5/13/2024.
 */
public class SCPlayStationMinimalSoundBankHeader extends SCSplitSoundBankHeader<SCPlayStationMinimalSoundBankHeaderEntry, SCPlayStationVabSound> {
    public SCPlayStationMinimalSoundBankHeader(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public boolean load(DataReader reader, SCSplitSoundBankBody<SCPlayStationMinimalSoundBankHeaderEntry, SCPlayStationVabSound> other) {
        int entryCount = reader.readInt();
        this.entries.clear();
        for (int i = 0; i < entryCount; i++) {
            SCPlayStationMinimalSoundBankHeaderEntry headerEntry = new SCPlayStationMinimalSoundBankHeaderEntry(this, i);
            headerEntry.load(reader);
            this.entries.add(headerEntry);
        }

        return true;
    }

    @Override
    public void save(DataWriter writer, SCSplitSoundBankBody<SCPlayStationMinimalSoundBankHeaderEntry, SCPlayStationVabSound> other) {
        writer.writeInt(this.entries.size());
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).save(writer);
    }

    @Getter
    public static class SCPlayStationMinimalSoundBankHeaderEntry extends SCSplitSoundBankHeaderEntry {
        private int flags;
        @Setter private int dataStartAddress;

        public SCPlayStationMinimalSoundBankHeaderEntry(SCSplitSoundBankHeader<?, ?> header, int internalId) {
            super(header, internalId);
        }

        @Override
        public void load(DataReader reader) {
            this.flags = reader.readInt();
            this.dataStartAddress = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            writer.writeInt(this.flags);
            writer.writeInt(this.dataStartAddress);
        }
    }
}