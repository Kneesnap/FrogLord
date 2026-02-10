package net.highwayfrogs.editor.games.sony.shared.map.filesync;

import net.highwayfrogs.editor.utils.commandparser.CommandListException;
import net.highwayfrogs.editor.utils.commandparser.TextCommand;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the "version_format" command, specifying the version of the format.
 * Created by Kneesnap on 2/9/2026.
 */
public class CommandFormatVersion<TContext extends MapFileSyncLoadContext<?>> extends TextCommand<TContext> {
    public static final String LABEL = "version_format";
    public static final CommandFormatVersion<?> INSTANCE = new CommandFormatVersion<>();

    public CommandFormatVersion() {
        super(LABEL, 1);
    }

    @Override
    public void execute(TContext context, OptionalArguments arguments) throws CommandListException {
        int newVersion = arguments.useNext().getAsInteger();
        if (newVersion <= 0 || newVersion > context.getMaxSupportedFileFormatVersion())
            context.getLogger().warning("The imported file (%s) reports a file version (%d) not supported by this version of FrogLord! This may lead to errors!", context.getImportedFileName(), newVersion);

        context.fileFormatVersion = newVersion;
    }
}
