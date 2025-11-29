package net.highwayfrogs.editor.games.sony.frogger.data.demo;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.DemoFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a Frogger demo table entry editable by the user.
 * Created by Kneesnap on 11/24/2025.
 */
@Getter
public class FroggerPCDemoTableEntry extends FroggerDemoTableEntry {
    // The PC version has two demo tables with virtually identical entries, and bad things happen if those tables aren't kept in sync.
    // Because this would be very easy to mess up, and to keep PSX/PC editing as similar as possible, we combine them into a single entry while loaded.
    private int lowPolyDemoResourceId = -1;


    public FroggerPCDemoTableEntry(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void setup(@NonNull FroggerMapLevelID level, @NonNull DemoFile demoFile, FroggerMapLevelID unlockLevel) {
        super.setup(level, demoFile, unlockLevel);
        this.lowPolyDemoResourceId = demoFile.getFileResourceId();
    }

    /**
     * Saves the low-poly demo table entry to the writer
     * @param writer the writer to write the entry to
     */
    public void saveLowPolyDemoTableEntry(DataWriter writer) {
        if (this.lowPolyDemoResourceId == -1 || this.lowPolyDemoResourceId == getDemoResourceId()) {
            this.save(writer);
            return;
        }

        FroggerDemoTableEntry newTableEntry = new FroggerDemoTableEntry(getGameInstance());
        if (isSkipped()) {
            newTableEntry.setSkipped();
        } else {
            newTableEntry.setup(getLevel(), this.lowPolyDemoResourceId, getUnlockLevel());
        }

        newTableEntry.save(writer);
    }

    /**
     * Sets the low-poly demo resource ID.
     * @param demoResourceID the ID of the demo resource to apply
     */
    public void setLowPolyDemoResourceId(int demoResourceID) {
        this.lowPolyDemoResourceId = (demoResourceID >= 0 && demoResourceID != getDemoResourceId()) ? demoResourceID : -1;
    }
}
