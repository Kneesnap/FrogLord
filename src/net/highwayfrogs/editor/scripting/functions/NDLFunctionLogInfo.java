package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

/**
 * Prints a message to the logger.
 */
public class NDLFunctionLogInfo extends NoodleFunction {
    public NDLFunctionLogInfo() {
        super("logInfo", "<message>");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        thread.getLogger().info(args[0].getAsString());
        return null;
    }
}
