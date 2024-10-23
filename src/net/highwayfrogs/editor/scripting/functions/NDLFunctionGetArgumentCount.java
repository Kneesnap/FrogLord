package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

/**
 * Gets the amount of arguments passed to this script.
 */
public class NDLFunctionGetArgumentCount extends NoodleFunction {
    public NDLFunctionGetArgumentCount() {
        super("argumentCount", "");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        return thread.getStack().pushNumber(thread.getArguments().size());
    }
}