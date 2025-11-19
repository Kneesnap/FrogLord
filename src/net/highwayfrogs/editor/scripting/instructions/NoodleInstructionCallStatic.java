package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.NoodleThreadStatus;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.functions.NoodleStaticFunction;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Calls a static template function.
 */
@Getter
public class NoodleInstructionCallStatic extends NoodleInstruction {
    private String functionLabel;
    private NoodleObjectTemplate<?> template;
    private short argumentCount;

    private static final NoodlePrimitive[] EMPTY_ARGS = new NoodlePrimitive[0];

    public NoodleInstructionCallStatic() {
        super(NoodleInstructionType.CALL_STATIC);
    }

    public NoodleInstructionCallStatic(NoodleCodeLocation codeLocation, NoodleObjectTemplate<?> template, String functionLabel, int argumentCount) {
        super(NoodleInstructionType.CALL_STATIC, codeLocation);
        this.template = template;
        this.functionLabel = functionLabel;
        this.argumentCount = (short) argumentCount;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.template.getName()).append('.').append(this.functionLabel).append("(").append(this.argumentCount).append(" args)");
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        // Gather arguments.
        NoodlePrimitive[] arguments = this.argumentCount > 0 ? new NoodlePrimitive[this.argumentCount] : EMPTY_ARGS;
        for (int i = arguments.length - 1; i >= 0; i--)
            arguments[i] = thread.getStack().popWithoutGC();

        // Find template.
        NoodleStaticFunction<?> function = this.template.getStaticFunction(this.functionLabel, this.argumentCount);
        if (function == null)
            throw new NoodleRuntimeException("The template for object '%s' does not have a static function named '%s' taking %d arguments.", template.getName(), this.functionLabel, this.argumentCount);

        // Execute function.
        int stackSize = thread.getStack().size();
        NoodlePrimitive resultValue;
        try {
            resultValue = function.execute(thread, null, arguments);
        } catch (Throwable ex) {
            StringBuilder builder = new StringBuilder("Error executing Noodle static function: '");
            builder.append(this.template.getName()).append(".");
            function.writeSignature(builder);
            builder.append("' with arguments (");
            writeArguments(builder, arguments);
            builder.append(").");

            throw new NoodleRuntimeException(ex, builder.toString());
        }

        // If the thread hasn't been paused or destroyed, add the result to the stack.
        if (stackSize == thread.getStack().size() && thread.getStatus() == NoodleThreadStatus.RUNNING)
            thread.getStack().pushPrimitive(resultValue);

        decreaseObjectRefs(arguments);
    }

    private void writeArguments(StringBuilder builder, NoodlePrimitive[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(arguments[i]);
        }
    }
}