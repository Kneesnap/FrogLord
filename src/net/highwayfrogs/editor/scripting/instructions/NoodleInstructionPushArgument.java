package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Pushes the argument passed into the 'main' function onto the stack.
 */
public class NoodleInstructionPushArgument extends NoodleInstruction {
    private short argumentIndex;

    public NoodleInstructionPushArgument() {
        super(NoodleInstructionType.PUSHARG);
    }

    public NoodleInstructionPushArgument(NoodleCodeLocation codeLocation, short argument) {
        super(NoodleInstructionType.PUSHARG, codeLocation);
        this.argumentIndex = argument;
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        if (this.argumentIndex >= thread.getArguments().size())
            throw new NoodleRuntimeException("Tried to use thread argument %d, but no such argument was actually provided to the thread.", this.argumentIndex);

        thread.getStack().pushPrimitive(thread.getArguments().get(this.argumentIndex));
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.argumentIndex);
    }
}