package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.NoodleScriptFunction;
import net.highwayfrogs.editor.scripting.functions.NoodleFunction;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.NoodleThreadStatus;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction will call a static function.
 * Names are checked in this order:
 * 1) Functions defined in the script.
 *  - If there are multiple functions with the same name, use the one with the correct argument count.
 * 2) Embedded scripts in the script.
 * 3) Global noodle functions.
 */
public class NoodleInstructionCall extends NoodleInstruction {
    @Getter private String functionLabel;
    @Getter private int argumentCount;
    @Getter private NoodleFunction function;
    private NoodleScriptFunction scriptFunction;

    private static final NoodlePrimitive[] EMPTY_ARGS = new NoodlePrimitive[0];

    public NoodleInstructionCall() {
        super(NoodleInstructionType.CALL);
    }

    public NoodleInstructionCall(NoodleCodeLocation codeLocation, String functionLabel, int argumentCount) {
        super(NoodleInstructionType.CALL, codeLocation);
        this.functionLabel = functionLabel;
        this.argumentCount = argumentCount;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.functionLabel).append("(").append(this.argumentCount).append(" args)");
    }

    /**
     * Resolves the function name into a function object.
     */
    public boolean resolveName(NoodleScript script) {
        // 1) Functions defined in the script.
        this.scriptFunction = script.getFunctionByName(this.functionLabel, this.argumentCount);
        if (this.scriptFunction != null)
            return true;

        // 2) Global functions.
        this.function = script.getEngine().getGlobalFunctionByName(this.functionLabel);
        return this.function != null;
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive[] arguments = this.argumentCount > 0 ? new NoodlePrimitive[this.argumentCount] : EMPTY_ARGS;
        for (int i = arguments.length - 1; i >= 0; i--)
            arguments[i] = thread.getStack().popWithoutGC();

        try {
            if (this.scriptFunction != null) {
                thread.callFunction(this.scriptFunction, arguments);
            } else if (this.function != null) {
                if (this.function.getMinArgs() > arguments.length)
                    throw new NoodleRuntimeException("Tried to call the function '%s' with only %d arguments when at least %d were required!", this.function.getLabel(), arguments.length, this.function.getMinArgs());

                int oldStackSize = thread.getStack().size();
                NoodlePrimitive resultValue = this.function.execute(thread, arguments);

                // Pushes the result value to the stack if the thread is running, and the function didn't push the value to the stack itself.
                if (thread.getStatus() == NoodleThreadStatus.RUNNING && oldStackSize == thread.getStack().size())
                    thread.getStack().pushPrimitive(resultValue);
            } else {
                throw new NoodleRuntimeException("Could not resolve a noodle function with the name '%s'.", getFunctionLabel());
            }
        } catch (Throwable ex) {
            StringBuilder builder = new StringBuilder("Error executing script function: '");
            writeSignature(builder, arguments);
            builder.append("'.");

            throw new NoodleRuntimeException(ex, builder.toString());
        }

        // Apply garbage collection to the values removed from the stack.
        decreaseObjectRefs(arguments); // TODO: HRMM PROBLEM. This won't work right if the thread is yielded. We should go over this here and for the other call instructions, and update it. I think we should try to make it so when the thread resumes is when the refcounts are lowered.
    }

    private void writeSignature(StringBuilder builder, NoodlePrimitive[] arguments) {
        builder.append(getFunctionLabel()).append("(");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(arguments[i]);
        }

        builder.append(')');
    }
}