package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * An instruction which pushes null on the stack.
 */
public class NoodleInstructionPushNull extends NoodleInstruction {
    public NoodleInstructionPushNull() {
        super(NoodleInstructionType.PUSHNULL);
    }

    public NoodleInstructionPushNull(NoodleCodeLocation codeLocation) {
        super(NoodleInstructionType.PUSHNULL, codeLocation);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.getStack().pushNull();
    }
}
