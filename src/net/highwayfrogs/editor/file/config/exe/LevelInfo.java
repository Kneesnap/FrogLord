package net.highwayfrogs.editor.file.config.exe;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.data.WorldId;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Holds information about each level.
 * Created by Kneesnap on 2/1/2019.
 */
@Getter
public class LevelInfo extends ExeStruct {
    private int level;
    private WorldId worldId;
    private int stackPosition; // 0 = Top of level stack.
    private int theme;
    private int localLevelId; // 0 -> 4 (Level 1 -> 5)
    private int levelsInWorld; // Number of levels in the world. (Used to calculate size.)
    private int worldImageSelectablePointer;
    private int worldImageVisitedPointer;
    private int worldImageNotTriedPointer;
    private int levelTexturePointer;
    private int levelNameTexturePointer;
    private int levelNameTextureInGamePointer;

    private static final int RUNTIME_DATA_SIZE = 44;
    private static final int TERMINATOR_LEVEL_ID = -1;

    @Override
    public void load(DataReader reader) {
        this.level = reader.readInt();
        this.worldId = WorldId.values()[reader.readInt()];
        this.stackPosition = reader.readInt();
        this.theme = reader.readInt();
        this.localLevelId = reader.readInt();
        this.levelsInWorld = reader.readInt();
        this.worldImageSelectablePointer = reader.readInt();
        this.worldImageVisitedPointer = reader.readInt();
        this.worldImageNotTriedPointer = reader.readInt();
        this.levelTexturePointer = reader.readInt();
        this.levelNameTexturePointer = reader.readInt();
        this.levelNameTextureInGamePointer = reader.readInt();
        reader.skipBytes(RUNTIME_DATA_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.level);
        writer.writeInt(this.worldId.ordinal());
        writer.writeInt(this.stackPosition);
        writer.writeInt(this.theme);
        writer.writeInt(this.localLevelId);
        writer.writeInt(this.levelsInWorld);
        writer.writeInt(this.worldImageSelectablePointer);
        writer.writeInt(this.worldImageVisitedPointer);
        writer.writeInt(this.worldImageNotTriedPointer);
        writer.writeInt(this.levelTexturePointer);
        writer.writeInt(this.levelNameTexturePointer);
        writer.writeInt(this.levelNameTextureInGamePointer);
        writer.writeNull(RUNTIME_DATA_SIZE);
    }

    @Override
    public String toString() {
        return "[" + getLevel() + "/" + getWorldId() + "] " + getTheme() + " (" + (getLocalLevelId() + 1) + "/" + getLevelsInWorld() + ") [" + getStackPosition() + "]";
    }

    @Override
    public void handleCorrection(String[] args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEntry(FileEntry test) {
        return false;
    }

    /**
     * Gets the MapLevel this info represents.
     * @return mapLevel
     */
    public MAPLevel getLevel() {
        return isTerminator() ? null : MAPLevel.values()[this.level];
    }

    /**
     * Gets the MapTheme this info represents.
     * @return mapLevel
     */
    public MAPTheme getTheme() {
        return isTerminator() ? null : MAPTheme.values()[this.theme];
    }

    /**
     * Test if this is a terminator entry.
     * @return isTerminator
     */
    public boolean isTerminator() {
        return this.level == TERMINATOR_LEVEL_ID;
    }
}
