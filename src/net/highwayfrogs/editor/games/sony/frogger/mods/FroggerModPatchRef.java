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
 * An executable patch named in a Frogger mod manifest: a [Patches] child naming a bundled patch, or a patch file in the package, with argument values.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
@Setter
public class FroggerModPatchRef {
    @NonNull private String id;
    private String file;
    private Boolean raw;
    private final Map<String, String> arguments = new LinkedHashMap<>();

    public static final String KEY_FILE = "file";
    public static final String KEY_RAW = "raw";

    public FroggerModPatchRef(@NonNull String id) {
        this.id = id;
    }

    /**
     * Returns true if the patch is a file shipped in the package rather than a bundled patch.
     */
    public boolean isPackaged() {
        return this.file != null;
    }

    /**
     * Reads a patch reference from its child section. Every key except file and raw is an argument.
     * @param section the [[id]] section
     * @param sectionPath the section path used in problems
     * @param problems the list to add value problems to
     * @return patchRef
     */
    public static FroggerModPatchRef fromConfig(@NonNull Config section, String sectionPath, @NonNull List<ModProblem> problems) {
        FroggerModPatchRef patchRef = new FroggerModPatchRef(section.getSectionName());
        patchRef.file = ModManifest.getString(section, KEY_FILE);
        patchRef.raw = FroggerModManifest.readBoolean(section, KEY_RAW, sectionPath, problems);
        for (String key : ModManifest.getKeys(section))
            if (!KEY_FILE.equals(key) && !KEY_RAW.equals(key))
                patchRef.arguments.put(key, ModManifest.getString(section, key));

        return patchRef;
    }

    /**
     * Writes this reference as a child section.
     * @return section
     */
    public Config toConfig() {
        Config section = new Config(this.id);
        ModManifest.setString(section, KEY_FILE, this.file);
        FroggerModManifest.writeBoolean(section, KEY_RAW, this.raw);
        for (Map.Entry<String, String> argument : this.arguments.entrySet())
            ModManifest.setString(section, argument.getKey(), argument.getValue());
        return section;
    }
}
