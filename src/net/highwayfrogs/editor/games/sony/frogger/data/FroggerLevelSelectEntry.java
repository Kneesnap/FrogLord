package net.highwayfrogs.editor.games.sony.frogger.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents an entry on the level stack.
 * The game calls this a "SEL_LEVEL_INFO" struct, defined in SELECT.H
 * Created by Kneesnap on 2/1/2019.
 */
public class FroggerLevelSelectEntry extends SCGameData<FroggerGameInstance> {
    @Setter private int level; // This ID is used to identify the level. (ID into map library to get map book, ID of level high scores to lookup, etc)

    // The world ID is used to:
    // - Check against the next level in the level stack to determine if the level completion screen should say "Zone Complete" or "Next Level: <level name>"
    // - Load the correct level preview screenshot.
    // - Ensure golden frog textures cover the correct areas.
    // - Immediately activate the "end of game" (temple unlock) sequence if the level is JUNGLE2.
    // As such, levels with the same world ID should be grouped together.
    @Getter @Setter private FroggerLevelSelectWorldID world;
    @Getter @Setter private int stackPosition; // Appears unused. Seems to be which cube in the level stack this should be placed within.
    private int theme; // Seems just to be used by golden frogs for unlocking the first level of the next theme. (If a level is unlocked, skip all levels with the same theme)
    @Getter @Setter private int localLevelId; // 0 -> 4 (Level 1 -> 5)
    // These are editable in Noodle scripts.
    @Getter @Setter private int levelsInWorld; // Number of levels in the world. (Used to calculate size of each level slab.)
    @Getter @Setter private long worldImageSelectablePointer; // Displayed on the level stack for the given world once unlocked.
    @Getter @Setter private long worldImageVisitedPointer; // Unused.
    @Getter @Setter private long worldImageNotTriedPointer; // Displayed on the level stack for the given world prior to unlock
    @Getter @Setter private long levelTexturePointer; // The screenshot of the level.
    @Getter @Setter private long levelNameTexturePointer; // The level name shown on the level stack.
    @Getter @Setter private long levelNameTextureInGamePointer; // The level name shown in-game.
    // TODO: We can unlock levels by default if we give access to flags. This is overridden for a few levels which are hardcoded, but this is probably the simplest way to ensure it works.

    private static final int RUNTIME_DATA_SIZE = 44;
    private static final int TERMINATOR_LEVEL_ID = -1;

    public FroggerLevelSelectEntry(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.level = reader.readInt();
        this.world = FroggerLevelSelectWorldID.values()[reader.readInt()];
        if (!getConfig().isAtOrBeforeBuild20())
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
        reader.skipBytesRequireEmpty(RUNTIME_DATA_SIZE);
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
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    public String toString() {
        return "[" + getLevel() + "/" + getWorld() + "] " + getTheme() + " (" + (getLocalLevelId() + 1) + "/" + getLevelsInWorld() + ") [" + getStackPosition() + "]";
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