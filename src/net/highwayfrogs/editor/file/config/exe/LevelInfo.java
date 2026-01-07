package net.highwayfrogs.editor.file.config.exe;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.config.data.FroggerMapWorldID;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Holds information about each level.
 * Created by Kneesnap on 2/1/2019.
 */
public class LevelInfo extends ExeStruct {
    @Setter private int level;
    @Getter @Setter private FroggerMapWorldID world;
    @Getter @Setter private int stackPosition; // 0 = Top of level stack.
    private int theme;
    @Getter @Setter private int localLevelId; // 0 -> 4 (Level 1 -> 5)
    // These are editable in Noodle scripts.
    @Getter @Setter private int levelsInWorld; // Number of levels in the world. (Used to calculate size.)
    @Getter @Setter private long worldImageSelectablePointer; // Displayed on the level stack for the given world once unlocked.
    @Getter @Setter private long worldImageVisitedPointer; // Unused.
    @Getter @Setter private long worldImageNotTriedPointer; // Displayed on the level stack for the given world prior to unlock
    @Getter @Setter private long levelTexturePointer; // The screenshot of the level.
    @Getter @Setter private long levelNameTexturePointer; // The level name shown on the level stack.
    @Getter @Setter private long levelNameTextureInGamePointer; // The level name shown in-game.

    private static final int RUNTIME_DATA_SIZE = 44;
    private static final int TERMINATOR_LEVEL_ID = -1;

    public LevelInfo(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.level = reader.readInt();
        this.world = FroggerMapWorldID.values()[reader.readInt()];
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
    public boolean isEntry(SCGameFile<?> file) {
        return false;
    }

    /**
     * Gets the MapLevel this info represents.
     * @return mapLevel
     */
    public FroggerMapLevelID getLevel() {
        return isTerminator() ? null : FroggerMapLevelID.values()[this.level];
    }

    /**
     * Gets the MapTheme this info represents.
     * @return mapLevel
     */
    public FroggerMapTheme getTheme() {
        return isTerminator() ? null : FroggerMapTheme.values()[this.theme];
    }

    /**
     * Set the theme of this level.
     * @param theme The new theme.
     */
    public void setTheme(FroggerMapTheme theme) {
        this.theme = theme != null ? theme.ordinal() : -1;
    }

    /**
     * Test if this is a terminator entry.
     * @return isTerminator
     */
    public boolean isTerminator() {
        return this.level == TERMINATOR_LEVEL_ID;
    }


    /**
     * Resolves the image displayed on the level stack for the given world once unlocked.
     */
    public VloImage getWorldLevelStackColoredImage() {
        return getGameInstance().getImageFromPointer(this.worldImageSelectablePointer);
    }

    /**
     * Resolves an image never displayed on the level stack, which theoretically would have displayed once "visited".
     */
    public VloImage getUnusedWorldVisitedImage() {
        return getGameInstance().getImageFromPointer(this.worldImageVisitedPointer);
    }

    /**
     * Resolves the image displayed on the level stack for the given world prior to unlock.
     */
    public VloImage getWorldNotTriedImage() {
        return getGameInstance().getImageFromPointer(this.worldImageNotTriedPointer);
    }

    /**
     * Resolves the preview image screenshot displayed on the level stack.
     */
    public VloImage getLevelPreviewScreenshotImage() {
        return getGameInstance().getImageFromPointer(this.levelTexturePointer);
    }

    /**
     * Resolves the level name image displayed on the level stack.
     */
    public VloImage getLevelNameImage() {
        return getGameInstance().getImageFromPointer(this.levelNameTexturePointer);
    }

    /**
     * Resolves the level name image displayed upon completing a level in-game.
     */
    public VloImage getIngameLevelNameImage() {
        return getGameInstance().getImageFromPointer(this.levelNameTextureInGamePointer);
    }
}