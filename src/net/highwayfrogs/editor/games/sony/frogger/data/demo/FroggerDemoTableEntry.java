package net.highwayfrogs.editor.games.sony.frogger.data.demo;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.file.DemoFile;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a raw Frogger demo table entry as tracked by the game.
 * Created by Kneesnap on 11/24/2025.
 */
@Getter
@Setter
public class FroggerDemoTableEntry extends SCGameData<FroggerGameInstance> {
    private FroggerMapLevelID level;
    private int demoResourceId;
    private FroggerMapLevelID unlockLevel;

    public static final int TERMINATOR_VALUE = -1;
    public static final int SKIP_VALUE = -2;

    public FroggerDemoTableEntry(FroggerGameInstance instance) {
        super(instance);
        setSkipped();
    }

    @Override
    public void load(DataReader reader) {
        int value1 = reader.readInt();
        this.demoResourceId = reader.readInt();
        int value3 = reader.readInt();
        this.level = (value1 != SKIP_VALUE) ? FroggerMapLevelID.values()[value1] : null;
        this.unlockLevel = (value3 != SKIP_VALUE) ? FroggerMapLevelID.values()[value3] : null;
    }

    @Override
    public void save(DataWriter writer) {
        if (isSkipped()) {
            writer.writeInt(SKIP_VALUE);
            writer.writeInt(SKIP_VALUE);
            writer.writeInt(SKIP_VALUE);
        } else {
            writer.writeInt(this.level.ordinal());
            writer.writeInt(this.demoResourceId);
            writer.writeInt(this.unlockLevel.ordinal());
        }
    }

    /**
     * Marks the demo table entry as skipped.
     */
    public void setSkipped() {
        this.level = null;
        this.demoResourceId = SKIP_VALUE;
        this.unlockLevel = null;
    }

    /**
     * Returns true iff this entry represents something which gets skipped.
     * @return skipped
     */
    public boolean isSkipped() {
        return this.level == null || this.demoResourceId < 0 || this.unlockLevel == null;
    }

    /**
     * Setup the demo entry.
     * @param level the level to load
     * @param demoFile the demo file to use for this level
     * @param unlockLevel which level must be unlocked for the demo to be usable.
     */
    public void setup(@NonNull FroggerMapLevelID level, @NonNull DemoFile demoFile, FroggerMapLevelID unlockLevel) {
        this.level = level;
        this.demoResourceId = demoFile.getFileResourceId();
        this.unlockLevel = unlockLevel != null ? unlockLevel : level;
    }

    /**
     * Setup the demo entry.
     * @param level the level to load
     * @param demoResourceId the demo file ID to use for this level
     * @param unlockLevel which level must be unlocked for the demo to be usable.
     */
    public void setup(@NonNull FroggerMapLevelID level, int demoResourceId, FroggerMapLevelID unlockLevel) {
        this.level = level;
        this.demoResourceId = demoResourceId;
        this.unlockLevel = unlockLevel != null ? unlockLevel : level;
    }
}
