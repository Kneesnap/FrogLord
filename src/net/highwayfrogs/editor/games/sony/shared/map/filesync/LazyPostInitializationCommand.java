package net.highwayfrogs.editor.games.sony.shared.map.filesync;

import net.highwayfrogs.editor.utils.commandparser.CommandListException;
import net.highwayfrogs.editor.utils.commandparser.ILazyTextCommandHandler;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents a command which is implemented lazily, using lambda functions/interface definitions.
 * Created by Kneesnap on 2/9/2026.
 */
public class LazyPostInitializationCommand<TContext extends MapFileSyncLoadContext<?>> extends PostInitializationCommand<TContext> {
    private final ILazyTextCommandHandler<TContext> lazyHandler;

    public LazyPostInitializationCommand(String name, int minimumArguments, ILazyTextCommandHandler<TContext> lazyHandler) {
        super(name, minimumArguments);
        this.lazyHandler = lazyHandler;
    }

    @Override
    public void execute(TContext context, OptionalArguments arguments) throws CommandListException {
        this.lazyHandler.handle(context, arguments);
    }
}
