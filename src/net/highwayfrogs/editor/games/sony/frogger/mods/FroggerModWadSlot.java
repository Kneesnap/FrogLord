package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;

/**
 * Describes one child slot of a WAD file as seen by mod package name resolution.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class FroggerModWadSlot {
    private final WADFile wad;
    private final int index;
    private final WADEntry entry;
    private final boolean dummy;
    private final MWIResourceEntry fileEntry;
    private final String modelName;
    private final boolean placeholderId;

    public FroggerModWadSlot(@NonNull WADFile wad, int index, @NonNull WADEntry entry, String modelName, boolean placeholderId) {
        this.wad = wad;
        this.index = index;
        this.entry = entry;
        this.dummy = entry.isDummy();
        this.fileEntry = entry.getFileEntry();
        this.modelName = modelName;
        this.placeholderId = placeholderId;
    }

    /**
     * Gets the MWI display name of the WAD holding this slot.
     * @return wadName
     */
    public String getWadName() {
        MWIResourceEntry wadEntry = this.wad.getIndexEntry();
        return wadEntry != null ? wadEntry.getDisplayName() : null;
    }

    @Override
    public String toString() {
        return getWadName() + " slot " + this.index + " ("
                + (this.dummy ? "dummy" : this.modelName) + ", id " + (this.fileEntry != null ? this.fileEntry.getResourceId() : -1)
                + (this.placeholderId ? " placeholder" : "") + ")";
    }
}
