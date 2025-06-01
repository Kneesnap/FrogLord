package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.generic.GameConfig;

/**
 * Contains configuration data for a release of The Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestConfig extends GameConfig {
    private String hostRootPath = "";

    public GreatQuestConfig(String internalName) {
        super(internalName);
    }

    @Override
    protected void readConfigData(Config config) {
        super.readConfigData(config);
        this.hostRootPath = config.getString("hostRootPath", this.hostRootPath);
    }

    /**
     * Gets the host root path with the protocol (eg: 'host:') part of the path stripped out.
     * @return strippedHostRootPath
     */
    public String getStrippedHostRootPath() {
        String strippedPath = this.hostRootPath;
        int colonIndex = strippedPath.indexOf(':');
        if (colonIndex >= 0 && colonIndex < 10)
            strippedPath = strippedPath.substring(colonIndex + 1);

        return strippedPath;
    }
}