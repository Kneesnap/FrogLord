package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.system.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * A replaced map of a Frogger mod manifest: a [Maps] child named by level code.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
@Setter
public class FroggerModMapEntry {
    @NonNull private String levelCode;
    private String file;
    private String remap;
    private String lowPolyFile;
    private List<String> models;

    public static final String KEY_FILE = "file";
    public static final String KEY_REMAP = "remap";
    public static final String KEY_LOW_POLY_FILE = "lowPolyFile";
    public static final String KEY_MODELS = "models";

    public FroggerModMapEntry(@NonNull String levelCode) {
        this.levelCode = levelCode;
    }

    /**
     * Reads a map entry from its child section.
     * @param section the [[CODE]] section
     * @return mapEntry
     */
    public static FroggerModMapEntry fromConfig(@NonNull Config section) {
        FroggerModMapEntry entry = new FroggerModMapEntry(section.getSectionName());
        entry.file = ModManifest.getString(section, KEY_FILE);
        entry.remap = ModManifest.getString(section, KEY_REMAP);
        entry.lowPolyFile = ModManifest.getString(section, KEY_LOW_POLY_FILE);
        List<String> models = ModManifest.parseList(ModManifest.getString(section, KEY_MODELS));
        entry.models = models != null ? new ArrayList<>(models) : null;
        return entry;
    }

    /**
     * Writes this entry as a child section, leaving out absent fields.
     * @return section
     */
    public Config toConfig() {
        Config section = new Config(this.levelCode);
        ModManifest.setString(section, KEY_FILE, this.file);
        ModManifest.setString(section, KEY_REMAP, this.remap);
        ModManifest.setString(section, KEY_LOW_POLY_FILE, this.lowPolyFile);
        ModManifest.setString(section, KEY_MODELS, ModManifest.joinList(this.models));
        return section;
    }
}
