package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

/**
 * Gets an argument passed to the script.
 */
public class NDLFunctionGetArgument extends NoodleFunction {
    public NDLFunctionGetArgument() {
        super("getArgument", "<argIndex>");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        int index = args[0].getIntegerValue();
        if (index < 0 || index >= thread.getArguments().size())
            throw new NoodleRuntimeException("Tried to get argument %d, which does not exist. (Argument Count: %d)", index, thread.getArguments().size());

        return thread.getArguments().get(index);
    }
}
