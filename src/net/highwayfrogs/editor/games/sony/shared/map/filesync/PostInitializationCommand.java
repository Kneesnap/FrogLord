package net.highwayfrogs.editor.games.sony.shared.map.filesync;

import net.highwayfrogs.editor.utils.commandparser.CommandListException;
import net.highwayfrogs.editor.utils.commandparser.CommandListException.CommandListExecutionError;
import net.highwayfrogs.editor.utils.commandparser.CommandLocation;
import net.highwayfrogs.editor.utils.commandparser.TextCommand;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents a command which runs after initialization
 * Created by Kneesnap on 2/9/2026.
 */
public abstract class PostInitializationCommand<TContext extends MapFileSyncLoadContext<?>> extends TextCommand<TContext> {
    public PostInitializationCommand(String label, int minimumArguments) {
        super(label, minimumArguments);
    }

    @Override
    public void validateBeforeExecution(TContext context, OptionalArguments arguments, CommandLocation location) throws CommandListException {
        super.validateBeforeExecution(context, arguments, location);
        if (context.getFileFormatVersion() == 0 || context.getGameConfig() == null)
            throw new CommandListExecutionError(location, "The command '%s' may only be used after initialization (when the game/format versions are specified.)", getName());
    }
}