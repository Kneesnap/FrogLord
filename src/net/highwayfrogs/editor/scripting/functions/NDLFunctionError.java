package net.highwayfrogs.editor.scripting.functions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

/**
 * Throws a Noodle runtime error, terminating execution of the script.
 * Created by Kneesnap on 11/17/2025.
 */
public class NDLFunctionError extends NoodleFunction {
    public static final NDLFunctionError INSTANCE = new NDLFunctionError();

    private NDLFunctionError() {
        super("error", "<messageTemplate> [arguments...]");
    }

    @Override
    public NoodlePrimitive execute(NoodleThread<? extends NoodleScript> thread, NoodlePrimitive[] args) {
        String message = args[0].getAsString();
        if (args.length == 1)
            throw new NoodleRuntimeException(message);

        // Throw with arguments.
        Object[] arguments = new Object[args.length - 1];
        for (int i = 1; i < args.length; i++)
            arguments[i - 1] = args[i] != null ? args[i].getAsJavaObject() : null;

        throw new NoodleRuntimeException(message, arguments);
    }
}
