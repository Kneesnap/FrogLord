package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Calls an instance/template function.
 */
@Getter
public class NoodleInstructionCallInstance extends NoodleInstruction {
    private String functionLabel;
    private short argumentCount;

    private static final NoodlePrimitive[] EMPTY_ARGS = new NoodlePrimitive[0];

    public NoodleInstructionCallInstance() {
        super(NoodleInstructionType.CALL_INST);
    }

    public NoodleInstructionCallInstance(NoodleCodeLocation codeLocation, String functionLabel, int argumentCount) {
        super(NoodleInstructionType.CALL_INST, codeLocation);
        this.functionLabel = functionLabel;
        this.argumentCount = (short) argumentCount;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.functionLabel).append("(").append(this.argumentCount).append(" args)");
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        // Gather arguments.
        NoodlePrimitive[] arguments = this.argumentCount > 0 ? new NoodlePrimitive[this.argumentCount] : EMPTY_ARGS;
        for (int i = arguments.length - 1; i >= 0; i--)
            arguments[i] = thread.getStack().popWithoutGC();

        // Find object instance.
        NoodlePrimitive instanceObject = thread.getStack().popWithoutGC(); // The instance object was evaluated before the function parameters, so it's the last object to pop.
        if (instanceObject == null || instanceObject.isNull())
            throw new NoodleRuntimeException("The object is null. Cannot run function '%s' on a null object.", this.functionLabel);
        if (!instanceObject.isObjectReference())
            throw new NoodleRuntimeException("The popped value was not an object. Cannot call function '%s' on [%s].", this.functionLabel, instanceObject);

        Object objectRef = instanceObject.getObjectReference().getObject();
        NoodleObjectTemplate.executeInstanceFunction(objectRef, thread, this.functionLabel, arguments);

        // Free objects if they have no more
        instanceObject.tryDecreaseRefCount();
        decreaseObjectRefs(arguments);
    }
}