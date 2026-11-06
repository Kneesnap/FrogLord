package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.shared.mods.ModFormatConstants;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModManifestValidator;
import net.highwayfrogs.editor.games.shared.mods.ModManifestValidator.SectionRule;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.system.Config;

import java.util.*;

/**
 * The Frogger view of a mod manifest: the Frogger sections of one target's effective configuration as typed records.
 * Level codes, image, bank, model, theme and world names stay strings; resolving them against a game is done elsewhere.
 * Absent sections and keys are null (or empty lists of records), meaning "unchanged from the base build".
 * Created by jimbolina on 11/6/2026.
 */
@Getter
@Setter
public class FroggerModManifest {
    private List<String> initialLevels;
    private List<String> visibleLevels;
    private Boolean jungleSwap;
    private Integer frameRate;
    private List<String> arcadeOrder;
    private List<String> raceOrder;
    private final List<FroggerModLevelEntry> levelEntries = new ArrayList<>();
    private final List<FroggerModMapEntry> mapEntries = new ArrayList<>();
    private final List<FroggerModModelEntry> modelEntries = new ArrayList<>();
    private final List<FroggerModImageEntry> imageEntries = new ArrayList<>();
    private final List<FroggerModThemeEntry> themeEntries = new ArrayList<>();
    private List<String> demoFiles;
    private final List<FroggerModDemoRow> demoRows = new ArrayList<>();
    private final List<FroggerModPatchRef> patches = new ArrayList<>();
    private final List<ModProblem> problems = new ArrayList<>();

    public static final String GAME_IDENTIFIER = "frogger";

    public static final String SECTION_GAMEPLAY = "Gameplay";
    public static final String SECTION_LEVEL_SELECT = "LevelSelect";
    public static final String SECTION_MAPS = "Maps";
    public static final String SECTION_MODELS = "Models";
    public static final String SECTION_IMAGES = "Images";
    public static final String SECTION_THEMES = "Themes";
    public static final String SECTION_DEMOS = "Demos";
    public static final String SECTION_PATCHES = "Patches";
    public static final String CHILD_DEMO_TABLE = "Table";

    public static final String KEY_INITIAL_LEVELS = "initialLevels";
    public static final String KEY_VISIBLE_LEVELS = "visibleLevels";
    public static final String KEY_JUNGLE_SWAP = "jungleSwap";
    public static final String KEY_FRAME_RATE = "frameRate";
    public static final String KEY_ARCADE = "arcade";
    public static final String KEY_RACE = "race";
    public static final String KEY_DEMO_FILES = "files";

    private static final Set<String> GAMEPLAY_KEYS = new HashSet<>(Arrays.asList(KEY_INITIAL_LEVELS, KEY_VISIBLE_LEVELS, KEY_JUNGLE_SWAP, KEY_FRAME_RATE));
    private static final List<String> SECTION_ORDER = Arrays.asList(SECTION_GAMEPLAY, SECTION_LEVEL_SELECT, SECTION_MAPS,
            SECTION_MODELS, SECTION_IMAGES, SECTION_THEMES, SECTION_DEMOS, SECTION_PATCHES);

    /**
     * Creates an empty Frogger manifest view, for building a manifest.
     */
    public FroggerModManifest() {
    }

    /**
     * Reads the Frogger sections that apply to one target.
     * @param manifest the manifest to read
     * @param target the target name or alias, or null for the root without overrides
     */
    public FroggerModManifest(@NonNull ModManifest manifest, String target) {
        readFrom(manifest.getEffectiveConfig(target));
    }

    /**
     * Reads the Frogger sections from an effective configuration root.
     * @param effectiveRoot the root returned by ModManifest.getEffectiveConfig
     * @return froggerManifest
     */
    public static FroggerModManifest fromEffectiveConfig(@NonNull Config effectiveRoot) {
        FroggerModManifest manifest = new FroggerModManifest();
        manifest.readFrom(effectiveRoot);
        return manifest;
    }

    /**
     * Creates a validator which knows the shared and the Frogger sections.
     * @return validator
     */
    public static ModManifestValidator createValidator() {
        ModManifestValidator validator = new ModManifestValidator(GAME_IDENTIFIER);
        validator.addSection(new SectionRule(SECTION_GAMEPLAY)
                .allowKeys(KEY_INITIAL_LEVELS, KEY_VISIBLE_LEVELS, KEY_JUNGLE_SWAP, KEY_FRAME_RATE).strict().noChildren());
        validator.addSection(new SectionRule(SECTION_LEVEL_SELECT)
                .allowKeys(KEY_ARCADE, KEY_RACE)
                .allowDescendantKeys(FroggerModLevelEntry.KEY_WORLD, FroggerModLevelEntry.KEY_STACK_POSITION,
                        FroggerModLevelEntry.KEY_LOCAL_LEVEL_ID, FroggerModLevelEntry.KEY_LEVELS_IN_WORLD,
                        FroggerModLevelEntry.KEY_NAME, FroggerModLevelEntry.KEY_NAME_IMAGE,
                        FroggerModLevelEntry.KEY_PICTURE, FroggerModLevelEntry.KEY_TIME_LIMIT)
                .allowDescendantKeyPattern("name\\.[A-Za-z0-9_-]+"));
        validator.addSection(new SectionRule(SECTION_MAPS)
                .allowDescendantKeys(FroggerModMapEntry.KEY_FILE, FroggerModMapEntry.KEY_REMAP, FroggerModMapEntry.KEY_LOW_POLY_FILE, FroggerModMapEntry.KEY_MODELS)
                .pathKeys(FroggerModMapEntry.KEY_FILE, FroggerModMapEntry.KEY_REMAP, FroggerModMapEntry.KEY_LOW_POLY_FILE));
        validator.addSection(new SectionRule(SECTION_MODELS)
                .allowDescendantKeys(FroggerModModelEntry.KEY_FILE).pathKeys(FroggerModModelEntry.KEY_FILE));
        validator.addSection(new SectionRule(SECTION_IMAGES)
                .allowDescendantKeys(FroggerModImageEntry.KEY_FILE, FroggerModImageEntry.KEY_BLACK_IS_TRANSPARENT,
                        FroggerModImageEntry.KEY_TRANSLUCENT, FroggerModImageEntry.KEY_IN_GAME_WIDTH,
                        FroggerModImageEntry.KEY_IN_GAME_HEIGHT, FroggerModImageEntry.KEY_BIT_DEPTH)
                .pathKeys(FroggerModImageEntry.KEY_FILE));
        validator.addSection(new SectionRule(SECTION_THEMES)
                .allowDescendantKeys(FroggerModThemeEntry.KEY_DEATH_HEIGHT));
        validator.addSection(new SectionRule(SECTION_DEMOS)
                .allowKeys(KEY_DEMO_FILES).allowChildren(CHILD_DEMO_TABLE).allowDescendantKeyPattern("[0-9]+"));
        validator.addSection(new SectionRule(SECTION_PATCHES)
                .allowAnyDescendantKey().pathKeys(FroggerModPatchRef.KEY_FILE));
        return validator;
    }

    private void readFrom(Config root) {
        Config gameplay = ModManifest.getFirstChild(root, SECTION_GAMEPLAY);
        if (gameplay != null) {
            this.initialLevels = ModManifest.parseList(ModManifest.getString(gameplay, KEY_INITIAL_LEVELS));
            this.visibleLevels = ModManifest.parseList(ModManifest.getString(gameplay, KEY_VISIBLE_LEVELS));
            this.jungleSwap = readBoolean(gameplay, KEY_JUNGLE_SWAP, SECTION_GAMEPLAY, this.problems);
            this.frameRate = readInteger(gameplay, KEY_FRAME_RATE, SECTION_GAMEPLAY, this.problems);
            if (this.frameRate != null && this.frameRate <= 0)
                this.problems.add(ModProblem.error(SECTION_GAMEPLAY, KEY_FRAME_RATE, "frameRate must be positive."));
            for (String key : ModManifest.getKeys(gameplay))
                if (!GAMEPLAY_KEYS.contains(key))
                    this.problems.add(ModProblem.error(SECTION_GAMEPLAY, key, "Unknown gameplay setting '" + key + "'; it cannot be honoured."));
        }

        Config levelSelect = ModManifest.getFirstChild(root, SECTION_LEVEL_SELECT);
        if (levelSelect != null) {
            this.arcadeOrder = ModManifest.parseList(ModManifest.getString(levelSelect, KEY_ARCADE));
            this.raceOrder = ModManifest.parseList(ModManifest.getString(levelSelect, KEY_RACE));
            for (Config child : levelSelect.getChildConfigNodes())
                this.levelEntries.add(FroggerModLevelEntry.fromConfig(child, SECTION_LEVEL_SELECT + "/" + child.getSectionName(), this.problems));
        }

        Config maps = ModManifest.getFirstChild(root, SECTION_MAPS);
        if (maps != null)
            for (Config child : maps.getChildConfigNodes())
                this.mapEntries.add(FroggerModMapEntry.fromConfig(child));

        Config models = ModManifest.getFirstChild(root, SECTION_MODELS);
        if (models != null)
            for (Config child : models.getChildConfigNodes())
                this.modelEntries.add(FroggerModModelEntry.fromConfig(child));

        Config images = ModManifest.getFirstChild(root, SECTION_IMAGES);
        if (images != null)
            for (Config bank : images.getChildConfigNodes())
                for (Config image : bank.getChildConfigNodes())
                    this.imageEntries.add(FroggerModImageEntry.fromConfig(bank.getSectionName(), image,
                            SECTION_IMAGES + "/" + bank.getSectionName() + "/" + image.getSectionName(), this.problems));

        Config themes = ModManifest.getFirstChild(root, SECTION_THEMES);
        if (themes != null)
            for (Config child : themes.getChildConfigNodes())
                this.themeEntries.add(FroggerModThemeEntry.fromConfig(child, SECTION_THEMES + "/" + child.getSectionName(), this.problems));

        Config demos = ModManifest.getFirstChild(root, SECTION_DEMOS);
        if (demos != null)
            readDemos(demos);

        Config patchSection = ModManifest.getFirstChild(root, SECTION_PATCHES);
        if (patchSection != null)
            for (Config child : patchSection.getChildConfigNodes())
                this.patches.add(FroggerModPatchRef.fromConfig(child, SECTION_PATCHES + "/" + child.getSectionName(), this.problems));
    }

    private void readDemos(Config demos) {
        this.demoFiles = ModManifest.parseList(ModManifest.getString(demos, KEY_DEMO_FILES));
        if (this.demoFiles != null)
            for (String demoFile : this.demoFiles)
                if (!ModFormatConstants.isValidPath(demoFile) || demoFile.indexOf('/') >= 0)
                    this.problems.add(ModProblem.error(SECTION_DEMOS, KEY_DEMO_FILES, "'" + demoFile + "' is not a demo file name."));

        Config table = ModManifest.getFirstChild(demos, CHILD_DEMO_TABLE);
        if (table == null)
            return;

        String tablePath = SECTION_DEMOS + "/" + CHILD_DEMO_TABLE;
        SortedMap<Integer, String> rowsByIndex = new TreeMap<>();
        for (String key : ModManifest.getKeys(table)) {
            Integer index = ModManifest.parseInteger(key);
            if (index == null || index < 0 || !key.matches("[0-9]+")) {
                this.problems.add(ModProblem.error(tablePath, key, "Demo table keys are row indices."));
                continue;
            }

            rowsByIndex.put(index, ModManifest.getString(table, key));
        }

        for (Map.Entry<Integer, String> row : rowsByIndex.entrySet()) {
            FroggerModDemoRow demoRow = FroggerModDemoRow.parse(row.getKey(), row.getValue());
            if (demoRow == null) {
                this.problems.add(ModProblem.error(tablePath, Integer.toString(row.getKey()), "'" + row.getValue() + "' is neither 'level,demo file,unlock level' nor 'skip'."));
            } else {
                this.demoRows.add(demoRow);
            }
        }
    }

    /**
     * Writes the Frogger sections into a manifest, replacing any it already has and keeping [Overrides] and [Files] last.
     * @param manifest the manifest to write into
     */
    public void applyTo(@NonNull ModManifest manifest) {
        Config root = manifest.getConfig();
        for (String sectionName : SECTION_ORDER)
            ModManifest.removeChildren(root, sectionName);

        List<Config> trailing = new ArrayList<>();
        for (Config child : new ArrayList<>(root.getChildConfigNodes())) {
            String name = child.getSectionName();
            if (ModFormatConstants.SECTION_OVERRIDES.equals(name) || ModFormatConstants.SECTION_FILES.equals(name)) {
                root.removeChildConfig(child);
                trailing.add(child);
            }
        }

        writeSections(root);
        for (Config child : trailing)
            root.addChildConfig(child);
    }

    /**
     * Appends the non-empty Frogger sections to a parent node: the manifest root, or a [[target]] node under [Overrides].
     * @param parent the node to append to
     */
    public void writeSections(@NonNull Config parent) {
        if (this.initialLevels != null || this.visibleLevels != null || this.jungleSwap != null || this.frameRate != null) {
            Config gameplay = new Config(SECTION_GAMEPLAY);
            ModManifest.setString(gameplay, KEY_INITIAL_LEVELS, ModManifest.joinList(this.initialLevels));
            ModManifest.setString(gameplay, KEY_VISIBLE_LEVELS, ModManifest.joinList(this.visibleLevels));
            writeBoolean(gameplay, KEY_JUNGLE_SWAP, this.jungleSwap);
            writeInteger(gameplay, KEY_FRAME_RATE, this.frameRate);
            parent.addChildConfig(gameplay);
        }

        if (this.arcadeOrder != null || this.raceOrder != null || !this.levelEntries.isEmpty()) {
            Config levelSelect = new Config(SECTION_LEVEL_SELECT);
            ModManifest.setString(levelSelect, KEY_ARCADE, ModManifest.joinList(this.arcadeOrder));
            ModManifest.setString(levelSelect, KEY_RACE, ModManifest.joinList(this.raceOrder));
            for (FroggerModLevelEntry entry : this.levelEntries)
                levelSelect.addChildConfig(entry.toConfig());
            parent.addChildConfig(levelSelect);
        }

        if (!this.mapEntries.isEmpty()) {
            Config maps = new Config(SECTION_MAPS);
            for (FroggerModMapEntry entry : this.mapEntries)
                maps.addChildConfig(entry.toConfig());
            parent.addChildConfig(maps);
        }

        if (!this.modelEntries.isEmpty()) {
            Config models = new Config(SECTION_MODELS);
            for (FroggerModModelEntry entry : this.modelEntries)
                models.addChildConfig(entry.toConfig());
            parent.addChildConfig(models);
        }

        if (!this.imageEntries.isEmpty()) {
            Config images = new Config(SECTION_IMAGES);
            for (FroggerModImageEntry entry : this.imageEntries) {
                Config bank = ModManifest.getFirstChild(images, entry.getBank());
                if (bank == null) {
                    bank = new Config(entry.getBank());
                    images.addChildConfig(bank);
                }

                bank.addChildConfig(entry.toConfig());
            }

            parent.addChildConfig(images);
        }

        if (!this.themeEntries.isEmpty()) {
            Config themes = new Config(SECTION_THEMES);
            for (FroggerModThemeEntry entry : this.themeEntries)
                themes.addChildConfig(entry.toConfig());
            parent.addChildConfig(themes);
        }

        if (this.demoFiles != null || !this.demoRows.isEmpty()) {
            Config demos = new Config(SECTION_DEMOS);
            ModManifest.setString(demos, KEY_DEMO_FILES, ModManifest.joinList(this.demoFiles));
            if (!this.demoRows.isEmpty()) {
                Config table = new Config(CHILD_DEMO_TABLE);
                for (FroggerModDemoRow row : this.demoRows)
                    ModManifest.setString(table, Integer.toString(row.getIndex()), row.toValue());
                demos.addChildConfig(table);
            }

            parent.addChildConfig(demos);
        }

        if (!this.patches.isEmpty()) {
            Config patchSection = new Config(SECTION_PATCHES);
            for (FroggerModPatchRef patchRef : this.patches)
                patchSection.addChildConfig(patchRef.toConfig());
            parent.addChildConfig(patchSection);
        }
    }

    /**
     * Gets the level-select entry for a level code.
     * @param levelCode the level code
     * @return levelEntry, or null
     */
    public FroggerModLevelEntry getLevelEntry(String levelCode) {
        for (FroggerModLevelEntry entry : this.levelEntries)
            if (entry.getLevelCode().equals(levelCode))
                return entry;

        return null;
    }

    /**
     * Gets the map entry for a level code.
     * @param levelCode the level code
     * @return mapEntry, or null
     */
    public FroggerModMapEntry getMapEntry(String levelCode) {
        for (FroggerModMapEntry entry : this.mapEntries)
            if (entry.getLevelCode().equals(levelCode))
                return entry;

        return null;
    }

    /**
     * Gets the model entry for a model name.
     * @param modelName the MWI name of the model
     * @return modelEntry, or null
     */
    public FroggerModModelEntry getModelEntry(String modelName) {
        for (FroggerModModelEntry entry : this.modelEntries)
            if (entry.getModelName().equals(modelName))
                return entry;

        return null;
    }

    /**
     * Gets the image entry for a bank and image name.
     * @param bank the bank name
     * @param name the image name
     * @return imageEntry, or null
     */
    public FroggerModImageEntry getImageEntry(String bank, String name) {
        for (FroggerModImageEntry entry : this.imageEntries)
            if (entry.getBank().equals(bank) && entry.getName().equals(name))
                return entry;

        return null;
    }

    /**
     * Gets the theme entry for a theme name.
     * @param theme the theme name
     * @return themeEntry, or null
     */
    public FroggerModThemeEntry getThemeEntry(String theme) {
        for (FroggerModThemeEntry entry : this.themeEntries)
            if (entry.getTheme().equals(theme))
                return entry;

        return null;
    }

    /**
     * Gets the demo table row with an index.
     * @param index the row index
     * @return demoRow, or null
     */
    public FroggerModDemoRow getDemoRow(int index) {
        for (FroggerModDemoRow row : this.demoRows)
            if (row.getIndex() == index)
                return row;

        return null;
    }

    /**
     * Gets the patch reference with an id.
     * @param id the patch id
     * @return patchRef, or null
     */
    public FroggerModPatchRef getPatch(String id) {
        for (FroggerModPatchRef patchRef : this.patches)
            if (patchRef.getId().equals(id))
                return patchRef;

        return null;
    }

    static Integer readInteger(Config section, String key, String sectionPath, List<ModProblem> problems) {
        String value = ModManifest.getString(section, key);
        if (value == null)
            return null;

        Integer result = ModManifest.parseInteger(value);
        if (result == null)
            problems.add(ModProblem.error(sectionPath, key, "'" + value + "' is not an integer."));

        return result;
    }

    static Boolean readBoolean(Config section, String key, String sectionPath, List<ModProblem> problems) {
        String value = ModManifest.getString(section, key);
        if (value == null)
            return null;

        Boolean result = ModManifest.parseBoolean(value);
        if (result == null)
            problems.add(ModProblem.error(sectionPath, key, "'" + value + "' is neither true nor false."));

        return result;
    }

    static void writeInteger(Config section, String key, Integer value) {
        ModManifest.setString(section, key, value != null ? Integer.toString(value) : null);
    }

    static void writeBoolean(Config section, String key, Boolean value) {
        ModManifest.setString(section, key, value != null ? Boolean.toString(value) : null);
    }
}
