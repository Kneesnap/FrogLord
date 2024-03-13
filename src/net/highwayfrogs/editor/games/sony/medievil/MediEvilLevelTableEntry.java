package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameRegion;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;

/**
 * Represents an entry in the MediEvil level table.
 * TODO: Consider making this an interface or an abstract class, so we can support different versions.
 * Created by Kneesnap on 3/8/2024.
 */
@Getter
public class MediEvilLevelTableEntry extends SCGameData<MediEvilGameInstance> {
    private int wadResourceId;
    private int vloResourceId;
    private int overlayId;

    @Setter private long textureRemapPointer;

    @Setter private transient TextureRemapArray remap;

    public MediEvilLevelTableEntry(MediEvilGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int byteSize = getGameInstance().getConfig().getLevelTableEntryByteSize();
        int endIndex = reader.getIndex() + byteSize;
        this.wadResourceId = reader.readInt();
        this.vloResourceId = reader.readInt();

        // Japanese versions have an extra resource ID
        if (byteSize > 100 && getConfig().getRegion() == SCGameRegion.JAPAN) {
            reader.skipBytes(8);
        } else {
            reader.skipBytes(4);
        }

        this.textureRemapPointer = reader.readUnsignedIntAsLong();

        // Retail to 0.37
        if (byteSize > 92 && getGameInstance().getConfig().getEntityTableSize() > 294) { // TODO: Read all this (and make into separate classes as per above if necessary)
            reader.skipBytes(60);
        }
        // 0.36 and below
        else if (byteSize > 92)
        {
            reader.skipBytes(56);
        }
        // ECTS Pre-Alpha build
        else if (getGameInstance().getConfig().getLevelTableSize() > 25 && byteSize > 76) {
            reader.skipBytes(68);
        }
        else {
            reader.skipBytes(52);
        }

        this.overlayId = reader.readInt();
        reader.setIndex(endIndex);
    }

    @Override
    public void save(DataWriter writer) {
        // TODO: IMPLEMENT
    }

    /**
     * Gets the .MAP file containing data for this level.
     */
    public MediEvilMapFile getMapFile() {
        for (WADEntry wadEntry : getWadFile().getFiles())
            if (wadEntry.getFile() instanceof MediEvilMapFile)
                return (MediEvilMapFile) wadEntry.getFile();
        return null;
    }

    /**
     * Gets the .WAD file used by this level.
     */
    public WADFile getWadFile() {
        return this.wadResourceId >= 0 ? getGameInstance().getGameFile(this.wadResourceId) : null;
    }

    /**
     * Gets the .VLO file used by this level.
     */
    public VLOArchive getVloFile() {
        return this.vloResourceId >= 0 ? getGameInstance().getGameFile(this.vloResourceId) : null;
    }
}