package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

/**
 * Prints an error to the logger.
 */
public class NDLFunctionLogError extends NoodleFunction {
    public static final NDLFunctionLogError INSTANCE = new NDLFunctionLogError();

    public NDLFunctionLogError() {
        super("logError", "<message>");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        thread.getLogger().severe(args[0].getAsString());
        return null;
    }
}
