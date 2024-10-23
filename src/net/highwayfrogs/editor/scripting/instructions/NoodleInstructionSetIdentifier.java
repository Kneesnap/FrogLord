package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction sets the value of an identifier to a value popped from the stack.
 */
public class NoodleInstructionSetIdentifier extends NoodleInstruction {
    private String variableName;

    public NoodleInstructionSetIdentifier() {
        super(NoodleInstructionType.SETVAR);
    }

    public NoodleInstructionSetIdentifier(NoodleCodeLocation codeLocation, String variableName) {
        super(NoodleInstructionType.SETVAR, codeLocation);
        this.variableName = variableName;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.variableName);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive value = thread.getStack().popWithoutGC();
        thread.getHeap().setVariable(this.variableName, value);
        if (value != null) // Remove stack reference (Must run after we call set, so we don't garbage collect it before setting the variable, which should cause it to not GC.)
            value.tryDecreaseRefCount();
    }
}
