package net.highwayfrogs.editor.games.sony.oldfrogger.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.data.OldFroggerEntityData.OldFroggerEntityDataFactory;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Holds configuration data about form entries.
 * Created by Kneesnap on 12/12/2023.
 */
@AllArgsConstructor
public class OldFroggerFormConfig {
    @Getter private final String internalName;
    private final Map<Integer, OldFroggerFormConfigEntry> entriesByType;

    /**
     * Gets a form by its type integer id.
     * @param formType The type id to lookup the config entry from.
     * @return The form associated with that id, or null if no such form exists.
     */
    public OldFroggerFormConfigEntry getFormByType(int formType) {
        return this.entriesByType.get(formType);
    }

    /**
     * Parse a full form config from a config.
     * @param config The config to parse form data from.
     * @return The parsed form config
     */
    public static OldFroggerFormConfig parseFromConfig(Config config) {
        Map<Integer, OldFroggerFormConfigEntry> entriesByType = new HashMap<>();

        // Load form config entries.
        for (Entry<String, String> entry : config.getValues().entrySet()) {
            OldFroggerFormConfigEntry newEntry = OldFroggerFormConfigEntry.parseFromText(entry.getKey() + "=" + entry.getValue());
            if (newEntry != null)
                entriesByType.put(newEntry.getFormType(), newEntry);
        }

        return new OldFroggerFormConfig(config.getName(), entriesByType);
    }

    /**
     * Parse a full form config.
     * @param formConfigName The name of the config to load.
     * @return formConfig
     */
    public static OldFroggerFormConfig loadFormConfig(String formConfigName) {
        Config config = new Config(SCGameType.OLD_FROGGER.getEmbeddedResourceStream("forms/" + formConfigName + ".cfg"));
        return parseFromConfig(config);
    }

    @Getter
    @AllArgsConstructor
    public static class OldFroggerFormConfigEntry {
        private final int formType;
        private final String displayName;
        private final OldFroggerEntityDataFactory entityDataFactory;

        /**
         * Parse an OldFroggerFormConfigEntry from a line of text.
         * @param text The line of text to parse.
         * @return The parsed form config entry
         */
        public static OldFroggerFormConfigEntry parseFromText(String text) {
            if (text == null || text.isEmpty())
                return null;

            String[] split = text.split("=");
            if (split.length != 2) {
                getLogger().warning("Missing equals sign in form config entry '" + text + "'.");
                return null;
            }

            int formType;
            try {
                formType = Integer.parseInt(split[0]);
            } catch (NumberFormatException nfe) {
                getLogger().warning("Invalid form type number '" + split[0] + "' in '" + text + "'.");
                return null;
            }

            split = split[1].split(",");
            String displayName;
            OldFroggerEntityDataFactory entityDataFactory;
            if (split.length > 2 || split.length == 0) {
                getLogger().warning("Improperly formatted form config entry '" + text + "'.");
                return null;
            } else if (split.length == 2) {
                displayName = split[0];
                entityDataFactory = OldFroggerEntityData.getEntityDataFactory(split[1]);
                if (entityDataFactory == null)
                    getLogger().warning("Failed to find entity data factory named '" + split[1] + "'.");
            } else {
                displayName = split[0];
                entityDataFactory = null;
            }

            return new OldFroggerFormConfigEntry(formType, displayName, entityDataFactory);
        }

        private static ILogger getLogger() {
            return ClassNameLogger.getLogger(null, OldFroggerFormConfigEntry.class);
        }
    }
}