package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameRegion;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.polygrid.MediEvilPolygonGridFile;
import net.highwayfrogs.editor.games.sony.medievil.map.quadtree.MediEvilMapQuadTree;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.List;

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
        int byteSize = getGameInstance().getVersionConfig().getLevelTableEntryByteSize();
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

        // TODO: Read all this (and make into separate classes as per above if necessary)
        if (byteSize > 92 && getGameInstance().getVersionConfig().getEntityTableSize() > 294) { // Retail to 0.37
            reader.skipBytes(60);
        } else if (byteSize > 92) { // 0.36 and below
            reader.skipBytes(56);
        } else if (getGameInstance().getVersionConfig().getLevelTableSize() > 25 && byteSize > 76) { // ECTS Pre-Alpha build
            reader.skipBytes(68);
        } else { // Version ???
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
        WADFile wadFile = getWadFile();
        if (wadFile == null)
            return null;

        List<WADEntry> wadEntries = wadFile.getFiles();
        for (int i = 0; i < wadEntries.size(); i++) {
            WADEntry wadEntry = wadEntries.get(i);
            SCGameFile<?> wadEntryFile = wadEntry.getFile();
            if (wadEntryFile instanceof MediEvilMapFile)
                return (MediEvilMapFile) wadEntryFile;
        }

        return null;
    }

    /**
     * Gets the .QTR file containing data for this level.
     */
    public MediEvilMapQuadTree getQuadTreeFile() {
        WADFile wadFile = getWadFile();
        if (wadFile == null)
            return null;

        List<WADEntry> wadEntries = wadFile.getFiles();
        for (int i = 0; i < wadEntries.size(); i++) {
            WADEntry wadEntry = wadEntries.get(i);
            SCGameFile<?> wadEntryFile = wadEntry.getFile();
            if (wadEntryFile instanceof MediEvilMapQuadTree)
                return (MediEvilMapQuadTree) wadEntryFile;
        }

        return null;
    }

    /**
     * Gets the .PGD file containing data for this level.
     */
    public MediEvilPolygonGridFile getPolygonGridFile() {
        WADFile wadFile = getWadFile();
        if (wadFile == null)
            return null;

        List<WADEntry> wadEntries = wadFile.getFiles();
        for (int i = 0; i < wadEntries.size(); i++) {
            WADEntry wadEntry = wadEntries.get(i);
            SCGameFile<?> wadEntryFile = wadEntry.getFile();
            if (wadEntryFile instanceof MediEvilPolygonGridFile)
                return (MediEvilPolygonGridFile) wadEntryFile;
        }

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
    public VloFile getVloFile() {
        return this.vloResourceId >= 0 ? getGameInstance().getGameFile(this.vloResourceId) : null;
    }

    /**
     * Gets the .VLO file used by this level.
     */
    public MWIResourceEntry getVloMwiEntry() {
        return this.vloResourceId >= 0 ? getGameInstance().getResourceEntryByID(this.vloResourceId) : null;
    }

    /**
     * Gets the map code associated with this table entry.
     * @return mapCode
     */
    public String getMapCode() {
        // Search MWI, since this function is used before game files are loaded.
        MWIResourceEntry wadEntry = this.wadResourceId >= 0 ? getGameInstance().getResourceEntryByID(this.wadResourceId) : null;
        if (wadEntry != null) {
            String displayName = wadEntry.getDisplayName();
            if (displayName != null)
                return displayName.split("_")[0];
        }

        wadEntry = this.vloResourceId >= 0 ? getGameInstance().getResourceEntryByID(this.vloResourceId) : null;
        if (wadEntry != null) {
            String displayName = wadEntry.getDisplayName();
            if (displayName != null)
                return displayName.split("_")[0];
        }

        return null;
    }
}