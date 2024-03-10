package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.WADFile.WADEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;

/**
 * Represents an entry in the MediEvil level table.
 * Created by Kneesnap on 3/8/2024.
 */
@Getter
public class MediEvilLevelTableEntry extends SCGameData<MediEvilGameInstance> {
    private int wadResourceId;
    private int vloResourceId;
    @Setter private long textureRemapPointer;

    @Setter private transient TextureRemapArray remap;

    public MediEvilLevelTableEntry(MediEvilGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int endIndex = reader.getIndex() + getByteSize();
        this.wadResourceId = reader.readInt();
        this.vloResourceId = reader.readInt();
        reader.skipBytes(4);
        this.textureRemapPointer = reader.readUnsignedIntAsLong();
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

    public int getByteSize() {
        if ("medievil-rolling-demo".equalsIgnoreCase(getConfig().getInternalName())) {
            return 76;
        }
        if ("medievil-ects-pre-alpha".equalsIgnoreCase(getConfig().getInternalName())) {
            return 88;
        } else {
            return 100;
        }
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