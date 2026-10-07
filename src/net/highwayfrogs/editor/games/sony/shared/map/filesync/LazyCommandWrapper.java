package net.highwayfrogs.editor.games.sony.shared.map.filesync;

import net.highwayfrogs.editor.utils.commandparser.CommandListException;
import net.highwayfrogs.editor.utils.commandparser.CommandListExecutionContext;
import net.highwayfrogs.editor.utils.commandparser.CommandLocation;
import net.highwayfrogs.editor.utils.commandparser.TextCommand;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

public class LazyCommandWrapper<TContext extends CommandListExecutionContext> extends TextCommand<TContext> {
    private final TextCommand<TContext> wrappedCommand;

    public LazyCommandWrapper(String label, TextCommand<TContext> command) {
        super(label, command.getMinimumArguments());
        this.wrappedCommand = command;
    }

    @Override
    public void validateBeforeExecution(TContext context, OptionalArguments arguments, CommandLocation location) throws CommandListException {
        this.wrappedCommand.validateBeforeExecution(context, arguments, location);
    }

    @Override
    public void execute(TContext context, OptionalArguments arguments) throws CommandListException {
        this.wrappedCommand.execute(context, arguments);
    }
}
