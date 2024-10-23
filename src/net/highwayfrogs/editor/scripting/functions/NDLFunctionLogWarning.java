package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

/**
 * Prints a warning to the logger.
 */
public class NDLFunctionLogWarning extends NoodleFunction {
    public static final NDLFunctionLogWarning INSTANCE = new NDLFunctionLogWarning();

    public NDLFunctionLogWarning() {
        super("logWarning", "<message>");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        thread.getLogger().warning(args[0].getAsString());
        return null;
    }
}
