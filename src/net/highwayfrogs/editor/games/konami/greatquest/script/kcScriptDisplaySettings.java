package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.objects.StringNode;

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
        kcCResource resource = GreatQuestUtils.findResourceByHashGlobal(this.chunkedFile, getGameInstance(), hash);
        if (resource != null && resource.getName() != null)
            return "\"" + resource.getName().replace("\"", "\\\"") + "\"";

        // Fallback to number.
        if (prefixHexNumber) {
            return "0x" + NumberUtils.to0PrefixedHexString(hash);
        } else {
            return "0x" + Integer.toHexString(hash).toUpperCase();
        }
    }

    /**
     * Get the hash number provided displayed either as its un-hashed string or as a hex number.
     * This attempts to return values in the GQS syntax.
     * @param hash The hash to get a display string from.
     * @return displayString
     */
    public String getGqsHashDisplay(int hash) {
        if (this.namesByHash != null) {
            String name = this.namesByHash.get(hash);
            if (name != null)
                return name;
        }

        // Search main game file.
        kcCResource resource = GreatQuestUtils.findResourceByHashGlobal(this.chunkedFile, getGameInstance(), hash);
        if (resource != null && resource.getName() != null)
            return resource.getName();

        // Fallback to number.
        return "0x" + NumberUtils.to0PrefixedHexString(hash);
    }

    /**
     * Applies the hash number provided displayed either as its un-hashed string or as a hex number.
     * This attempts to return values in the GQS syntax.
     * @param hash The hash to get a display string from.
     */
    public void applyGqsHashDisplay(StringNode node, int hash) {
        if (this.namesByHash != null) {
            String name = this.namesByHash.get(hash);
            if (name != null) {
                node.setAsString(name, true);
                return;
            }
        }

        // Search main game file.
        kcCResource resource = GreatQuestUtils.findResourceByHashGlobal(this.chunkedFile, getGameInstance(), hash);
        if (resource != null && resource.getName() != null) {
            node.setAsString(resource.getName(), true);
        } else {
            // Fallback to number.
            node.setAsString("0x" + NumberUtils.to0PrefixedHexString(hash), false);
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
            return "0x" + NumberUtils.to0PrefixedHexString(hash);
        } else {
            return "0x" + Integer.toHexString(hash).toUpperCase();
        }
    }

    /**
     * Apply the hash number provided in the FrogLord GreatQuest script syntax.
     * @param hash The hash to get a display string from.
     */
    public static void applyGqsSyntaxHashDisplay(StringNode node, kcScriptDisplaySettings settings, int hash, int nullValue) {
        if (hash == nullValue) {
            node.setNull();
        } else if (settings != null) {
            settings.applyGqsHashDisplay(node, hash);
        } else {
            node.setAsString("0x" + NumberUtils.to0PrefixedHexString(hash), false);
        }
    }

    /**
     * Apply the hash number provided in the FrogLord GreatQuest script syntax.
     * @param hashObj The hash to get a display string from.
     */
    public static void applyGqsSyntaxHashDisplay(StringNode node, kcScriptDisplaySettings settings, GreatQuestHash<?> hashObj) {
        if (hashObj == null || hashObj.getHashNumber() == 0 || hashObj.getHashNumber() == -1) {
            node.setNull();
        } else if (hashObj.getOriginalString() != null) {
            node.setAsString(hashObj.getAsString(false), true);
        } else if (settings != null) {
            settings.applyGqsHashDisplay(node, hashObj.getHashNumber());
        } else {
            node.setAsString("0x" + NumberUtils.to0PrefixedHexString(hashObj.getHashNumber()), false);
        }
    }

    /**
     * Gets the default script settings for the given scenario
     * @param gameInstance the game instance to lookup resources from
     * @param parentFile the parent file to lookup resources from
     * @return defaultSettings
     */
    public static kcScriptDisplaySettings getDefaultSettings(GreatQuestInstance gameInstance, GreatQuestChunkedFile parentFile) {
        if (parentFile != null) {
            return parentFile.createScriptDisplaySettings();
        } else {
            return new kcScriptDisplaySettings(gameInstance, null, null, true, true);
        }
    }
}