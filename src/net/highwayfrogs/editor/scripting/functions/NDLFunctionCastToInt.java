package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

/**
 * Equivalent to casting to int in java.
 */
public class NDLFunctionCastToInt extends NoodleFunction {
    public static final NDLFunctionCastToInt INSTANCE = new NDLFunctionCastToInt();

    public NDLFunctionCastToInt() {
        super("int", "<decimal>");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        NoodlePrimitive num = args[0];
        return thread.getStack().pushNumber((int) num.getNumberValue());
    }
}
