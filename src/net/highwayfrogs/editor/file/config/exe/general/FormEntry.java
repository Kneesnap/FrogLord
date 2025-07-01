package net.highwayfrogs.editor.file.config.exe.general;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.exe.MapBook;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.IFroggerFormEntry;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents an entry in a form book.
 * Created by Kneesnap on 3/4/2019.
 */
@Getter
@Setter
public class FormEntry extends SCGameData<FroggerGameInstance> implements IFroggerFormEntry {
    private int entityType; // Index into global entity book.
    private int id; // Index into theme wad.
    private int scriptId;
    private int flags;
    private long collisionReactFunction; // Form collision callback.
    private int radiusSquared; // Squared radius of bounding box. Only set by GEN_BONUS_FLY_GRE when not running.
    private FormDeathType deathType;
    private long bonusCallbackFunction; // Eaten.

    private transient final FroggerMapTheme theme;
    private transient final int globalFormId;
    private transient final int localFormId;
    private transient FroggerFormGrid formGrid; // Null by default, gets setup as part of load process.

    public static final int PROJECT_MAX_THEME_MOFS = 128;
    public static final int FLAG_GENERAL = 0x8000;
    public static final int BYTE_SIZE = (8 * Constants.INTEGER_SIZE);
    public static final int OLD_BYTE_SIZE = (7 * Constants.INTEGER_SIZE);

    public FormEntry(FroggerGameInstance instance, FroggerMapTheme theme, int formId, int globalFormId) {
        super(instance);
        this.theme = theme;
        this.localFormId = formId;
        this.id = formId;
        this.globalFormId = globalFormId;
    }

    /**
     * Gets the frogger game config.
     */
    public FroggerConfig getConfig() {
        return getGameInstance().getVersionConfig();
    }

    @Override
    public void load(DataReader reader) {
        this.entityType = reader.readInt();
        this.id = reader.readInt();
        this.scriptId = reader.readInt();
        this.flags = reader.readInt();
        this.collisionReactFunction = reader.readUnsignedIntAsLong();
        this.radiusSquared = reader.readInt();
        int deathTypeId = reader.readInt();
        this.deathType = deathTypeId >= 0 && deathTypeId < FormDeathType.values().length ? FormDeathType.values()[deathTypeId] : null;
        if (!getConfig().isAtOrBeforeBuild4())
            this.bonusCallbackFunction = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.entityType);
        writer.writeInt(this.id);
        writer.writeInt(this.scriptId);
        writer.writeInt(this.flags);
        writer.writeUnsignedInt(this.collisionReactFunction);
        writer.writeInt(this.radiusSquared);
        writer.writeInt(this.deathType.ordinal());
        if (!getConfig().isAtOrBeforeBuild4())
            writer.writeUnsignedInt(this.bonusCallbackFunction);
    }

    @Override
    public String getEntityTypeName() {
        Config config = getConfig().getEntityBank().getConfig();
        if (config.hasChild("Override")) {
            String formName = getFormTypeName();
            Config overrideConfig = config.getChild("Override");
            String forceEntity = overrideConfig.getString(formName, null);
            if (forceEntity != null) {
                int replaceIndex = getConfig().getEntityBank().getNames().indexOf(forceEntity);
                if (replaceIndex == -1)
                    throw new RuntimeException("Unknown replacement entity-type: '" + forceEntity + "'.");
                return getConfig().getEntityBank().getName(replaceIndex);
            }
        }

        return getConfig().getEntityBank().getName(this.entityType);
    }

    @Override
    public String getFormTypeName() {
        return getConfig().getFormBank().getName(this.globalFormId);
    }

    /**
     * Gets the map form id.
     * @return mapFormId
     */
    public int getMapFormId() {
        int id = this.localFormId;
        if (this.theme == FroggerMapTheme.GENERAL)
            id |= FLAG_GENERAL;
        return id;
    }

    /**
     * Get the index into the wad.
     * @return wadFile
     */
    public int getWadIndex() {
        int wadIndex = this.id;
        if (getTheme() == FroggerMapTheme.GENERAL || wadIndex >= PROJECT_MAX_THEME_MOFS) {
            wadIndex -= PROJECT_MAX_THEME_MOFS;
            if (!getConfig().isAtLeastRetailWindows() && !getConfig().isAtOrBeforeBuild21())
                wadIndex++; // Some builds have GEN_VRAM.VLO in THEME_GEN.WAD, which requires this offset.
        }

        return wadIndex;
    }

    /**
     * Test if a flag is present.
     * @param flag The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(FormLibFlag flag) {
        return (this.flags & flag.getFlag()) == flag.getFlag();
    }

    /**
     * Set the flag state.
     * @param flag     The flag type.
     * @param newState The new state of the flag.
     */
    public void setFlag(FormLibFlag flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.flags |= flag.getFlag();
        } else {
            this.flags ^= flag.getFlag();
        }
    }

    private WADFile resolveWadFile(FroggerMapFile mapFile, boolean checkThemeBooks) {
        boolean isGeneralTheme = getTheme() == FroggerMapTheme.GENERAL;

        // The game will not use the map book if general is the theme.
        if (!isGeneralTheme && mapFile.getIndexEntry() != null) { // There is an MWI entry, so try to do what the game does.
            for (MapBook mapBook : getGameInstance().getMapLibrary()) {
                if (mapBook != null && mapBook.isEntry(mapFile)) {
                    WADFile wadFile;
                    if (checkThemeBooks) {
                        wadFile = mapBook.getThemeWad(mapFile);
                    } else {
                        wadFile = mapBook.getLevelWad(mapFile);
                    }
                    if (wadFile != null)
                        return wadFile;
                }
            }
        }

        return null;
    }

    /**
     * Gets the wad file which the form entry points to a WAD within.
     * @param mapFile the map file to resolve the wad file for
     * @return wadFile
     */
    public WADFile getWadFile(FroggerMapFile mapFile) {
        // The game will not use the map book if general is the theme.
        WADFile wadFile = resolveWadFile(mapFile, false);
        if (wadFile != null)
            return wadFile;

        wadFile = resolveWadFile(mapFile, true);
        if (wadFile != null)
            return wadFile;

        // If the theme is GENERAL, the game will use the general theme book.
        // But, we also are using this as a fallback option for if there's no way to find the map book.
        ThemeBook themeBook = getGameInstance().getThemeBook(getTheme());
        wadFile = themeBook != null ? themeBook.getWAD(mapFile) : null;
        if (wadFile != null)
            return wadFile;

        // This here is a failsafe, and is not something the game does.
        // At this point, we're going to just get the theme book for the map theme, and use that.
        // This shouldn't really happen, but it doesn't hurt to include.
        themeBook = getGameInstance().getThemeBook(mapFile.getMapTheme());
        wadFile = themeBook != null ? themeBook.getWAD(mapFile) : null;
        if (wadFile != null)
            return wadFile;

        throw new RuntimeException("Failed to find the WADFile for the form entry!");
    }

    /**
     * Gets the MOF for this particular form.
     */
    public WADEntry getModel(FroggerMapFile mapFile, boolean tryImportIfMissing) {
        if (testFlag(FormLibFlag.NO_MODEL))
            return null;

        // Attempt to resolve GENERAL models first.
        int wadIndex = getWadIndex();
        if (this.theme == FroggerMapTheme.GENERAL || this.id >= PROJECT_MAX_THEME_MOFS) {
            ThemeBook themeBook = getGameInstance().getThemeBook(FroggerMapTheme.GENERAL);
            WADFile wadFile = themeBook != null ? themeBook.getWAD(mapFile) : null;
            if (wadFile != null)
                return wadFile.getFiles().get(wadIndex);
        }

        WADFile wadFile = getWadFile(mapFile);
        if (wadFile != null && wadFile.getFiles().size() > wadIndex && wadIndex >= 0) { // Test if there's an associated WAD.
            WADEntry wadEntry = wadFile.getFiles().get(wadIndex);
            if (wadEntry.getFile() instanceof MRModel) {
                MRModel model = (MRModel) wadEntry.getFile();
                if (!model.isDummy())
                    return wadEntry; // There's already a model here so return it.

                if (tryImportIfMissing) {
                    // Try to import from the main theme WAD file if there is one.
                    WADFile themeWad = resolveWadFile(mapFile, true);
                    if (themeWad != null && themeWad.getFiles().size() > wadIndex) {
                        WADEntry themeWadEntry = themeWad.getFiles().get(wadIndex);
                        if (themeWadEntry.getFile() instanceof MRModel) {
                            byte[] rawData = themeWadEntry.getFile().writeDataToByteArray();
                            if (getArchive().replaceFile(themeWadEntry.getDisplayName(), rawData, themeWadEntry.getFileEntry(), wadEntry.getFile(), false) != null)
                                return wadEntry;
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public WADEntry getEntityModelWadEntry(FroggerMapEntity entity) {
        return getModel(entity.getMapFile(), true);
    }

    @Getter
    @AllArgsConstructor
    public enum FormLibFlag {
        NO_MODEL(Constants.BIT_FLAG_0, "No Model"), // Is a sprite, not subject to collprim collision.
        NO_ROTATION_SNAPPING(Constants.BIT_FLAG_1, "No Snapping"), // Frog rotation is not snapped when landing on this form. (Ie: Lily pads.)
        NO_ENTITY_ANGLE(Constants.BIT_FLAG_2, "No Angle"), // No entity angle is calculated. (IE: Lily pads.)
        DONT_RESET_ON_CHECKPOINT(Constants.BIT_FLAG_3, "No Chkpt Reset"), // Don't reset when a checkpoint is collected.
        DONT_RESET_ON_DEATH(Constants.BIT_FLAG_4, "No Death Reset"), // Don't reset if the frog dies.
        THICK(Constants.BIT_FLAG_5, "Thick"), // Form applies beyond bottom of model to a value.
        DONT_CENTER_X(Constants.BIT_FLAG_6, "No Center X"), // Don't center along entity X axis, unless at end of form.
        DONT_CENTER_Z(Constants.BIT_FLAG_7, "No Center Z"), // Don't center Z axis, unless at end of form. Used in places such as logs.
        DONT_FADE_COLOR(Constants.BIT_FLAG_8, "No Color Fade"), // Turns off color scaling for sprites. (Mainly for cave)
        UNIT_FORM(Constants.BIT_FLAG_9, "Unit Form"); // Force form depth to 256 units.

        private final int flag;
        private final String displayName;
    }
}