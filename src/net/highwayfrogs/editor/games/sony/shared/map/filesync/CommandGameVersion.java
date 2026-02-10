package net.highwayfrogs.editor.games.sony.shared.map.filesync;

import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.utils.commandparser.CommandListException;
import net.highwayfrogs.editor.utils.commandparser.CommandListException.CommandListExecutionError;
import net.highwayfrogs.editor.utils.commandparser.TextCommand;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the "version_game" command, specifying the version of the game.
 * Created by Kneesnap on 2/9/2026.
 */
public class CommandGameVersion<TContext extends MapFileSyncLoadContext<?>> extends TextCommand<TContext> {
    public static final String LABEL = "version_game";
    public static final CommandGameVersion<?> INSTANCE = new CommandGameVersion<>();

    public CommandGameVersion() {
        super(LABEL, 1);
    }

    @Override
    public void execute(TContext context, OptionalArguments arguments) throws CommandListException {
        String versionName = arguments.useNext().getAsString();
        if (context.getGameConfig() != null)
            throw new CommandListExecutionError("Tried to set the game version to '%s', after it was already set to '%s'.", versionName, context.getGameConfig().getInternalName());

        SCGameType gameType = context.getMapFile().getGameInstance().getGameType();
        GameConfig gameConfig = gameType.getVersionConfigByName(versionName);
        if (!(gameConfig instanceof SCGameConfig))
            throw new CommandListExecutionError("Could not resolve a %s game version named '%s'.", gameType.getDisplayName(), versionName);

        context.gameConfig = (SCGameConfig) gameConfig;
    }
}
