package net.highwayfrogs.editor.games.greatquest.script;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Map;

/**
 * The settings used to display human-readable scripts.
 * Created by Kneesnap on 6/27/2023.
 */
@Getter
@AllArgsConstructor
public class kcScriptDisplaySettings {
    private Map<Integer, String> namesByHash;
    private boolean showLabels;
    private boolean showUnusedValues;

    public static final kcScriptDisplaySettings DEFAULT_SETTINGS = new kcScriptDisplaySettings(null, true, true);

    /**
     * Get the hash number provided displayed either as its unhashed string or as a hex number.
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

        if (prefixHexNumber) {
            return "0x" + Utils.to0PrefixedHexString(hash);
        } else {
            return "0x" + Integer.toHexString(hash).toUpperCase();
        }
    }

    /**
     * Get the hash number provided displayed either as its unhashed string or as a hex number.
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
}