package net.highwayfrogs.editor.utils.commandparser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.utils.commandparser.CommandListException.CommandListSyntaxError;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents a type of command supported by a command list parser.
 * Created by Kneesnap on 10/11/2025.
 */
@Getter
@RequiredArgsConstructor
public abstract class TextCommand<TContext extends CommandListExecutionContext> {
    private final String name;
    private final int minimumArguments;

    /**
     * Validate the execution before it happens.
     * @param context the context to execute the command under
     * @param arguments the arguments to execute the command with
     * @param location the location where the command was found
     */
    public void validateBeforeExecution(TContext context, OptionalArguments arguments, CommandLocation location) throws CommandListException {
        if (this.minimumArguments > arguments.getRemainingArgumentCount())
            throw new CommandListSyntaxError(location, "The command '%s' requires at least %d argument(s), but %d %s specified.",
                    arguments, this.minimumArguments, arguments.getRemainingArgumentCount(), (arguments.getRemainingArgumentCount() != 1) ? "were" : "was");
    }

    /**
     * Execute the command.
     * @param context the context to execute the command under
     * @param arguments the arguments to execute the command with
     */
    public abstract void execute(TContext context, OptionalArguments arguments) throws CommandListException;
}
