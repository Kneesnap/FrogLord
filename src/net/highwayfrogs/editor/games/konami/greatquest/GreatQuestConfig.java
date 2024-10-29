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
}