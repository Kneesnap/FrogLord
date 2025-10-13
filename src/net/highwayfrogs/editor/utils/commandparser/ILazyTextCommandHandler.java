package net.highwayfrogs.editor.utils.commandparser;

import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * A generic interface for lazy command handling.
 * Created by Kneesnap on 10/12/2025.
 */
public interface ILazyTextCommandHandler<TContext extends CommandListExecutionContext> {
    /**
     * Handles execution for the given context and arguments.
     * @param context the context to handle.
     * @param arguments the arguments to handle.
     * @throws CommandListException can be thrown
     */
    void handle(TContext context, OptionalArguments arguments) throws CommandListException;
}
