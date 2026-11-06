package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.system.Config;

/**
 * A replaced model of a Frogger mod manifest: a [Models] child named by the model's MWI name.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
@Setter
public class FroggerModModelEntry {
    @NonNull private String modelName;
    private String file;

    public static final String KEY_FILE = "file";

    public FroggerModModelEntry(@NonNull String modelName) {
        this.modelName = modelName;
    }

    /**
     * Reads a model entry from its child section.
     * @param section the [[NAME]] section
     * @return modelEntry
     */
    public static FroggerModModelEntry fromConfig(@NonNull Config section) {
        FroggerModModelEntry entry = new FroggerModModelEntry(section.getSectionName());
        entry.file = ModManifest.getString(section, KEY_FILE);
        return entry;
    }

    /**
     * Writes this entry as a child section.
     * @return section
     */
    public Config toConfig() {
        Config section = new Config(this.modelName);
        ModManifest.setString(section, KEY_FILE, this.file);
        return section;
    }
}
