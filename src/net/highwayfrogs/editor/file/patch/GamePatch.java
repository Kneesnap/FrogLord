package net.highwayfrogs.editor.file.patch;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.sony.SCGameType;

import java.util.*;

/**
 * Loads a patch from a config.
 * Created by Kneesnap on 1/15/2020.
 */
@Getter
public class GamePatch {
    private String name;
    private String description;
    private String author;
    private final Set<String> supportedVersions = new HashSet<>();
    private final Map<String, PatchValue> defaultVariables = new HashMap<>();
    private final List<String> argsCode = new ArrayList<>();
    private final List<String> code = new ArrayList<>();
    private final Map<String, Map<String, PatchValue>> versionSpecificVariables = new HashMap<>();
    private final List<PatchArgument> arguments = new ArrayList<>();

    /**
     * Loads patch data from the config.
     * @param config The config to read from.
     */
    public void loadPatchFromConfig(Config config) {
        this.name = config.getString("Name");
        this.description = config.getString("Description", null);
        this.author = config.getString("Author", null);
        Collections.addAll(this.supportedVersions, config.getString("Versions").split(","));

        // Read arguments.
        if (config.hasChild("Args")) {
            Config argChild = config.getChild("Args");

            for (String key : argChild.keySet())
                this.defaultVariables.put(key, PatchValue.parseStringAsPatchValue(argChild.getString(key)));

            for (String line : argChild.getText())
                if (line.length() > 0)
                    this.arguments.add(PatchArgument.parsePatchArgument(line));
        }

        // Read args code.
        if (config.hasChild("ArgsCode"))
            this.argsCode.addAll(config.getChild("ArgsCode").getText());

        // Read code.
        if (config.hasChild("Code"))
            this.code.addAll(config.getChild("Code").getText());

        // Read version-specific variables.
        for (GameConfig gameConfig : SCGameType.FROGGER.getVersionConfigs()) {
            String versionConfigName = gameConfig.getInternalName();
            if (!config.hasChild(versionConfigName))
                continue;

            Config child = config.getChild(versionConfigName);
            Map<String, PatchValue> versionValues = new HashMap<>();
            for (String key : child.keySet())
                versionValues.put(key, PatchValue.parseStringAsPatchValue(child.getString(key)));
            this.versionSpecificVariables.put(versionConfigName, versionValues);
        }
    }

    /**
     * Test if this patch is compatible with a given version.
     * @param version The version to test compatibility with.
     * @return isCompatible
     */
    public boolean isCompatibleWithVersion(String version) {
        return this.supportedVersions.contains(version);
    }
}