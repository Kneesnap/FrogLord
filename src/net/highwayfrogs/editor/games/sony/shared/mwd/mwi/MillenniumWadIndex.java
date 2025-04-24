package net.highwayfrogs.editor.games.sony.shared.mwd.mwi;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Located in game executable (Such as frogger.exe), this is a registry of most game asset files.
 * It started with the PSX ports of Defcon 5 and Silverload, which the Millennium API appears to have been built from.
 * These games appear to have used hardcoded file lists. The MWI was created to centralize these into a single list with more streamlined tools.
 * This should always export exactly the same size as the original MWI, as this gets pasted directly in the executable.
 * Created by Kneesnap on 8/10/2018.
 */
@Getter
public class MillenniumWadIndex extends SCSharedGameData {
    private final List<MWIResourceEntry> entries = new ArrayList<>();

    public MillenniumWadIndex(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.entries.clear();
        int firstFilePathAddress = Integer.MAX_VALUE;
        while (reader.hasMore() && firstFilePathAddress > reader.getIndex()) {
            MWIResourceEntry newEntry = new MWIResourceEntry(getGameInstance(), this.entries.size());
            newEntry.load(reader);
            this.entries.add(newEntry);

            if (newEntry.filePathPointerAddress != MWIResourceEntry.NO_FILE_NAME_MARKER && firstFilePathAddress > newEntry.filePathPointerAddress)
                firstFilePathAddress = newEntry.filePathPointerAddress;
        }

        // Read file paths.
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).readFilePath(reader);
    }

    @Override
    public void save(DataWriter writer) {
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).save(writer);
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).writeFilePath(writer);
    }

    /**
     * Get the MWIResourceEntry for a given resource id.
     * @param resourceId The resource id.
     * @return fileEntry
     */
    public MWIResourceEntry getResourceEntryByID(int resourceId) {
        return resourceId >= 0 && resourceId < this.entries.size() ? this.entries.get(resourceId) : null;
    }

    /**
     * Gets the resource entry from a given name.
     * @param name The name to lookup.
     * @return foundEntry, if any.
     */
    public MWIResourceEntry getResourceEntryByName(String name) {
        if (name == null || name.isEmpty())
            return null;

        for (int i = 0; i < this.entries.size(); i++) {
            MWIResourceEntry entry = this.entries.get(i);
            if (name.equalsIgnoreCase(entry.getDisplayName()))
                return entry;
        }

        return null;
    }
}