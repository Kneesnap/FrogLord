package net.highwayfrogs.editor.utils.logging;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.utils.Utils;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Represents a logger for an individual game instance.
 * Created by Kneesnap on 12/13/2024.
 */
public class MainGameInstanceLogger extends InstanceLogger {
    @Getter private final GameInstance gameInstance;
    private String cachedName;
    private IGameType oldGameType;
    private GameConfig oldVersionConfig;

    public MainGameInstanceLogger(@NonNull GameInstance instance) {
        super(Logger.getLogger(Utils.getSimpleName(instance)));
        this.gameInstance = instance;
    }

    public String getName() {
        if (this.cachedName == null || this.oldGameType != this.gameInstance.getGameType() || this.oldVersionConfig != this.gameInstance.getVersionConfig()) {
            this.oldGameType = this.gameInstance.getGameType();
            this.oldVersionConfig = this.gameInstance.getVersionConfig();
            this.cachedName = (this.oldGameType != null ? this.oldGameType.getIdentifier() : "null") + (this.oldVersionConfig != null ? "{" + this.oldVersionConfig.getInternalName() + "}" : "");
        }

        return this.cachedName;
    }

    @Override
    public String getLoggerInfo() {
        return getName();
    }

    @Override
    public LogRecord createLogRecord(Level level, String message) {
        return new GameInstanceLogRecord(this.gameInstance, level, message);
    }
}
