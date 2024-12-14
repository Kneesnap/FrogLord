package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction duplicates the top value on the stack.
 */
public class NoodleInstructionDuplicate extends NoodleInstruction {
    public NoodleInstructionDuplicate() {
        super(NoodleInstructionType.DUP);
    }

    public NoodleInstructionDuplicate(NoodleCodeLocation codeLocation) {
        super(NoodleInstructionType.DUP, codeLocation);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.getStack().pushPrimitive(thread.getStack().peek());
    }
}
