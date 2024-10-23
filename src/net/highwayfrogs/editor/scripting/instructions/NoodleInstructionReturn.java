package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction returns from the current function, potentially with a return value.
 */
public class NoodleInstructionReturn extends NoodleInstruction {
    public NoodleInstructionReturn() {
        super(NoodleInstructionType.RET);
    }

    public NoodleInstructionReturn(NoodleCodeLocation codeLocation) {
        super(NoodleInstructionType.RET, codeLocation);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        if (!thread.returnFromFunctionCall()) {
            NoodlePrimitive returnValue = thread.getStack().popWithoutGC();
            thread.complete(returnValue);
            returnValue.tryDecreaseRefCount(); // Decrease refCount from popping it from the stack.
        }
    }
}
