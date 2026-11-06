package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.system.Config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A level-select row of a Frogger mod manifest: a [LevelSelect] child named by level code. Every field is optional; null keeps the base value.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
@Setter
public class FroggerModLevelEntry {
    @NonNull private String levelCode;
    private String world;
    private Integer stackPosition;
    private Integer localLevelId;
    private Integer levelsInWorld;
    private String name;
    private final Map<String, String> localizedNames = new LinkedHashMap<>();
    private String nameImage;
    private String picture;
    private Integer timeLimit;

    public static final String KEY_WORLD = "world";
    public static final String KEY_STACK_POSITION = "stackPosition";
    public static final String KEY_LOCAL_LEVEL_ID = "localLevelId";
    public static final String KEY_LEVELS_IN_WORLD = "levelsInWorld";
    public static final String KEY_NAME = "name";
    public static final String KEY_NAME_PREFIX = "name.";
    public static final String KEY_NAME_IMAGE = "nameImage";
    public static final String KEY_PICTURE = "picture";
    public static final String KEY_TIME_LIMIT = "timeLimit";

    public FroggerModLevelEntry(@NonNull String levelCode) {
        this.levelCode = levelCode;
    }

    /**
     * Reads a level entry from its child section.
     * @param section the [[CODE]] section
     * @param sectionPath the section path used in problems
     * @param problems the list to add value problems to
     * @return levelEntry
     */
    public static FroggerModLevelEntry fromConfig(@NonNull Config section, String sectionPath, @NonNull List<ModProblem> problems) {
        FroggerModLevelEntry entry = new FroggerModLevelEntry(section.getSectionName());
        entry.world = ModManifest.getString(section, KEY_WORLD);
        entry.stackPosition = FroggerModManifest.readInteger(section, KEY_STACK_POSITION, sectionPath, problems);
        entry.localLevelId = FroggerModManifest.readInteger(section, KEY_LOCAL_LEVEL_ID, sectionPath, problems);
        entry.levelsInWorld = FroggerModManifest.readInteger(section, KEY_LEVELS_IN_WORLD, sectionPath, problems);
        entry.name = ModManifest.getString(section, KEY_NAME);
        for (String key : ModManifest.getKeys(section))
            if (key.startsWith(KEY_NAME_PREFIX) && key.length() > KEY_NAME_PREFIX.length())
                entry.localizedNames.put(key.substring(KEY_NAME_PREFIX.length()), ModManifest.getString(section, key));

        entry.nameImage = ModManifest.getString(section, KEY_NAME_IMAGE);
        entry.picture = ModManifest.getString(section, KEY_PICTURE);
        entry.timeLimit = FroggerModManifest.readInteger(section, KEY_TIME_LIMIT, sectionPath, problems);
        return entry;
    }

    /**
     * Writes this entry as a child section, leaving out absent fields.
     * @return section
     */
    public Config toConfig() {
        Config section = new Config(this.levelCode);
        ModManifest.setString(section, KEY_WORLD, this.world);
        FroggerModManifest.writeInteger(section, KEY_STACK_POSITION, this.stackPosition);
        FroggerModManifest.writeInteger(section, KEY_LOCAL_LEVEL_ID, this.localLevelId);
        FroggerModManifest.writeInteger(section, KEY_LEVELS_IN_WORLD, this.levelsInWorld);
        ModManifest.setString(section, KEY_NAME, this.name);
        for (Map.Entry<String, String> localizedName : this.localizedNames.entrySet())
            ModManifest.setString(section, KEY_NAME_PREFIX + localizedName.getKey(), localizedName.getValue());

        ModManifest.setString(section, KEY_NAME_IMAGE, this.nameImage);
        ModManifest.setString(section, KEY_PICTURE, this.picture);
        FroggerModManifest.writeInteger(section, KEY_TIME_LIMIT, this.timeLimit);
        return section;
    }
}
