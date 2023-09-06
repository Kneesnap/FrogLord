package net.highwayfrogs.editor.file.config.exe;

import lombok.Getter;
import lombok.Setter;
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
@Setter
public class LevelInfo extends ExeStruct {
    private int level;
    private WorldId world;
    private int stackPosition; // 0 = Top of level stack.
    private int theme;
    private int localLevelId; // 0 -> 4 (Level 1 -> 5)
    private int levelsInWorld; // Number of levels in the world. (Used to calculate size.)
    private long worldImageSelectablePointer;
    private long worldImageVisitedPointer;
    private long worldImageNotTriedPointer;
    private long levelTexturePointer; // The screenshot of the level.
    private long levelNameTexturePointer;
    private long levelNameTextureInGamePointer;

    private static final int RUNTIME_DATA_SIZE = 44;
    private static final int TERMINATOR_LEVEL_ID = -1;

    @Override
    public void load(DataReader reader) {
        this.level = reader.readInt();
        this.world = WorldId.values()[reader.readInt()];
        if (!getConfig().isAtOrBeforeBuild20()) // TODO: Flip these checks so they make more sense. Eg: "isAtLeastBuild21" or "isAfterBuild20".
            this.stackPosition = reader.readInt();
        if (!getConfig().isAtOrBeforeBuild21())
            this.theme = reader.readInt();

        this.localLevelId = reader.readInt();
        this.levelsInWorld = reader.readInt();
        this.worldImageSelectablePointer = reader.readUnsignedIntAsLong();
        this.worldImageVisitedPointer = reader.readUnsignedIntAsLong();
        this.worldImageNotTriedPointer = reader.readUnsignedIntAsLong();
        this.levelTexturePointer = reader.readUnsignedIntAsLong();
        this.levelNameTexturePointer = reader.readUnsignedIntAsLong();
        this.levelNameTextureInGamePointer = reader.readUnsignedIntAsLong();
        reader.skipBytes(RUNTIME_DATA_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.level);
        writer.writeInt(this.world.ordinal());
        if (!getConfig().isAtOrBeforeBuild20())
            writer.writeInt(this.stackPosition);
        if (!getConfig().isAtOrBeforeBuild21())
            writer.writeInt(this.theme);
        writer.writeInt(this.localLevelId);
        writer.writeInt(this.levelsInWorld);
        writer.writeUnsignedInt(this.worldImageSelectablePointer);
        writer.writeUnsignedInt(this.worldImageVisitedPointer);
        writer.writeUnsignedInt(this.worldImageNotTriedPointer);
        writer.writeUnsignedInt(this.levelTexturePointer);
        writer.writeUnsignedInt(this.levelNameTexturePointer);
        writer.writeUnsignedInt(this.levelNameTextureInGamePointer);
        writer.writeNull(RUNTIME_DATA_SIZE);
    }

    @Override
    public String toString() {
        return "[" + getLevel() + "/" + getWorld() + "] " + getTheme() + " (" + (getLocalLevelId() + 1) + "/" + getLevelsInWorld() + ") [" + getStackPosition() + "]";
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
     * Set the theme of this level.
     * @param theme The new theme.
     */
    public void setTheme(MAPTheme theme) {
        this.theme = theme != null ? theme.ordinal() : -1;
    }

    /**
     * Test if this is a terminator entry.
     * @return isTerminator
     */
    public boolean isTerminator() {
        return this.level == TERMINATOR_LEVEL_ID;
    }
}