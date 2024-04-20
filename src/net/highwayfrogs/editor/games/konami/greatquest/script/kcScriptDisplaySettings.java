package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Map;

/**
 * The settings used to display human-readable scripts.
 * Created by Kneesnap on 6/27/2023.
 */
@Getter
public class kcScriptDisplaySettings extends GameObject<GreatQuestInstance> {
    private final GreatQuestChunkedFile chunkedFile;
    private final Map<Integer, String> namesByHash;
    private final boolean showLabels;
    private final boolean showUnusedValues;

    public kcScriptDisplaySettings(GreatQuestInstance instance, GreatQuestChunkedFile chunkedFile, Map<Integer, String> namesByHash, boolean showLabels, boolean showUnusedValues) {
        super(instance);
        this.chunkedFile = chunkedFile;
        this.namesByHash = namesByHash;
        this.showLabels = showLabels;
        this.showUnusedValues = showUnusedValues;
    }

    /**
     * Get the hash number provided displayed either as its un-hashed string or as a hex number.
     * @param hash            The hash to get a display string from.
     * @param prefixHexNumber If the hex number should be padded to 8 characters if it's included directly.
     * @return displayString
     */
    public String getHashDisplay(int hash, boolean prefixHexNumber) {
        if (this.namesByHash != null) {
            String name = this.namesByHash.get(hash);
            if (name != null)
                return "\"" + name.replace("\"", "\\\"") + "\"";
        }

        // Search main game file.
        kcCResource resource = GreatQuestUtils.findResourceByHash(this.chunkedFile, getGameInstance(), hash);
        if (resource != null && resource.getName() != null)
            return "\"" + resource.getName().replace("\"", "\\\"") + "\"";

        // Fallback to number.
        if (prefixHexNumber) {
            return "0x" + Utils.to0PrefixedHexString(hash);
        } else {
            return "0x" + Integer.toHexString(hash).toUpperCase();
        }
    }

    /**
     * Get the hash number provided displayed either as its un-hashed string or as a hex number.
     * @param hash            The hash to get a display string from.
     * @param prefixHexNumber If the hex number should be padded to 8 characters if it's included directly.
     * @return displayString
     */
    public static String getHashDisplay(kcScriptDisplaySettings settings, int hash, boolean prefixHexNumber) {
        if (settings != null)
            return settings.getHashDisplay(hash, prefixHexNumber);

        if (prefixHexNumber) {
            return "0x" + Utils.to0PrefixedHexString(hash);
        } else {
            return "0x" + Integer.toHexString(hash).toUpperCase();
        }
    }

    /**
     * Gets the default script settings for the given scenario
     * @param gameInstance the game instance to lookup resources from
     * @param parentFile the parent file to lookup resources from
     * @return defaultSettings
     */
    public static kcScriptDisplaySettings getDefaultSettings(GreatQuestInstance gameInstance, GreatQuestChunkedFile parentFile) {
        return new kcScriptDisplaySettings(gameInstance, parentFile, null, true, true);
    }
}