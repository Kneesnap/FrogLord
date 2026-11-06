package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.system.Config;

import java.util.List;

/**
 * A replaced image of a Frogger mod manifest: an [Images] grandchild named by image name under its bank. Absent flags and sizes are null.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
@Setter
public class FroggerModImageEntry {
    @NonNull private String bank;
    @NonNull private String name;
    private String file;
    private Boolean blackIsTransparent;
    private Boolean translucent;
    private Integer inGameWidth;
    private Integer inGameHeight;
    private Integer bitDepth;

    public static final String KEY_FILE = "file";
    public static final String KEY_BLACK_IS_TRANSPARENT = "blackIsTransparent";
    public static final String KEY_TRANSLUCENT = "translucent";
    public static final String KEY_IN_GAME_WIDTH = "inGameWidth";
    public static final String KEY_IN_GAME_HEIGHT = "inGameHeight";
    public static final String KEY_BIT_DEPTH = "bitDepth";

    public FroggerModImageEntry(@NonNull String bank, @NonNull String name) {
        this.bank = bank;
        this.name = name;
    }

    /**
     * Reads an image entry from its section.
     * @param bank the bank name the image belongs to
     * @param section the [[[name]]] section
     * @param sectionPath the section path used in problems
     * @param problems the list to add value problems to
     * @return imageEntry
     */
    public static FroggerModImageEntry fromConfig(@NonNull String bank, @NonNull Config section, String sectionPath, @NonNull List<ModProblem> problems) {
        FroggerModImageEntry entry = new FroggerModImageEntry(bank, section.getSectionName());
        entry.file = ModManifest.getString(section, KEY_FILE);
        entry.blackIsTransparent = FroggerModManifest.readBoolean(section, KEY_BLACK_IS_TRANSPARENT, sectionPath, problems);
        entry.translucent = FroggerModManifest.readBoolean(section, KEY_TRANSLUCENT, sectionPath, problems);
        entry.inGameWidth = FroggerModManifest.readInteger(section, KEY_IN_GAME_WIDTH, sectionPath, problems);
        entry.inGameHeight = FroggerModManifest.readInteger(section, KEY_IN_GAME_HEIGHT, sectionPath, problems);
        entry.bitDepth = FroggerModManifest.readInteger(section, KEY_BIT_DEPTH, sectionPath, problems);
        if (entry.bitDepth != null && entry.bitDepth != 4 && entry.bitDepth != 8 && entry.bitDepth != 16)
            problems.add(ModProblem.error(sectionPath, KEY_BIT_DEPTH, "bitDepth must be 4, 8 or 16, not " + entry.bitDepth + "."));
        if (entry.inGameWidth != null && (entry.inGameWidth < 1 || entry.inGameWidth > 256))
            problems.add(ModProblem.error(sectionPath, KEY_IN_GAME_WIDTH, "inGameWidth must be between 1 and 256."));
        if (entry.inGameHeight != null && (entry.inGameHeight < 1 || entry.inGameHeight > 256))
            problems.add(ModProblem.error(sectionPath, KEY_IN_GAME_HEIGHT, "inGameHeight must be between 1 and 256."));

        return entry;
    }

    /**
     * Writes this entry as a section named by the image name, leaving out absent fields.
     * @return section
     */
    public Config toConfig() {
        Config section = new Config(this.name);
        ModManifest.setString(section, KEY_FILE, this.file);
        FroggerModManifest.writeBoolean(section, KEY_BLACK_IS_TRANSPARENT, this.blackIsTransparent);
        FroggerModManifest.writeBoolean(section, KEY_TRANSLUCENT, this.translucent);
        FroggerModManifest.writeInteger(section, KEY_IN_GAME_WIDTH, this.inGameWidth);
        FroggerModManifest.writeInteger(section, KEY_IN_GAME_HEIGHT, this.inGameHeight);
        FroggerModManifest.writeInteger(section, KEY_BIT_DEPTH, this.bitDepth);
        return section;
    }
}
