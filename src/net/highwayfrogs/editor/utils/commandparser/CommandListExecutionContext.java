package net.highwayfrogs.editor.utils.commandparser;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Contains context information while executing commands.
 * Created by Kneesnap on 10/11/2025.
 */
@Getter
@RequiredArgsConstructor
public class CommandListExecutionContext {
    @NonNull private final ILogger logger;
}
