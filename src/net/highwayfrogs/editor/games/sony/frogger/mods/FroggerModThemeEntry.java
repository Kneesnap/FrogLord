package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.system.Config;

import java.util.List;

/**
 * Theme-book changes of a Frogger mod manifest: a [Themes] child named by theme name.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
@Setter
public class FroggerModThemeEntry {
    @NonNull private String theme;
    private Double deathHeight;

    public static final String KEY_DEATH_HEIGHT = "deathHeight";

    public FroggerModThemeEntry(@NonNull String theme) {
        this.theme = theme;
    }

    /**
     * Reads a theme entry from its child section.
     * @param section the [[THEME]] section
     * @param sectionPath the section path used in problems
     * @param problems the list to add value problems to
     * @return themeEntry
     */
    public static FroggerModThemeEntry fromConfig(@NonNull Config section, String sectionPath, @NonNull List<ModProblem> problems) {
        FroggerModThemeEntry entry = new FroggerModThemeEntry(section.getSectionName());
        String value = ModManifest.getString(section, KEY_DEATH_HEIGHT);
        if (value != null) {
            try {
                entry.deathHeight = Double.parseDouble(value.trim());
            } catch (NumberFormatException ex) {
                problems.add(ModProblem.error(sectionPath, KEY_DEATH_HEIGHT, "'" + value + "' is not a number."));
            }
        }

        return entry;
    }

    /**
     * Writes this entry as a child section, leaving out absent fields.
     * @return section
     */
    public Config toConfig() {
        Config section = new Config(this.theme);
        if (this.deathHeight != null)
            ModManifest.setString(section, KEY_DEATH_HEIGHT, Double.toString(this.deathHeight));
        return section;
    }
}
