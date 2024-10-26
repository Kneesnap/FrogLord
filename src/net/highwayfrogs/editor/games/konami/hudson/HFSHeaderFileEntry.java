package net.highwayfrogs.editor.games.konami.hudson;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.GameData;

/**
 * Represents an HFS header file entry.
 * Created by Kneesnap on 8/8/2024.
 */
@Getter
public class HFSHeaderFileEntry extends GameData<HudsonGameInstance> {
    public int cdSectorWithFlags = -1;
    public int fileDataLength = -1;

    public static final int CD_SECTOR_FLAG_MASK = 0xFFFFFF;
    public static final int FLAG_IS_COMPRESSED = Constants.BIT_FLAG_24;
    public static final int VALIDATION_FLAG_MASK = FLAG_IS_COMPRESSED | CD_SECTOR_FLAG_MASK; // Lower 24 bits are valid.

    public HFSHeaderFileEntry(HudsonGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.cdSectorWithFlags = reader.readInt();
        this.fileDataLength = reader.readInt();
        warnAboutInvalidBitFlags(this.cdSectorWithFlags, VALIDATION_FLAG_MASK, "HFSHeaderFileEntry");
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.cdSectorWithFlags);
        writer.writeInt(this.fileDataLength);
    }

    /**
     * Returns the CD sector with the flags removed.
     */
    public int getCdSector() {
        return this.cdSectorWithFlags & CD_SECTOR_FLAG_MASK;
    }

    /**
     * Returns true iff compression is enabled for this entry.
     */
    public boolean isCompressed() {
        return (this.cdSectorWithFlags & FLAG_IS_COMPRESSED) == FLAG_IS_COMPRESSED;
    }
}