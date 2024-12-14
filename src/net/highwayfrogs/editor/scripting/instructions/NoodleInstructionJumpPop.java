package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This jump instruction changes the instruction which code executes to the value popped from the stack.
 */
public class NoodleInstructionJumpPop extends NoodleInstruction {
    public NoodleInstructionJumpPop() {
        super(NoodleInstructionType.JUMP_POP);
    }

    public NoodleInstructionJumpPop(NoodleCodeLocation codeLocation) {
        super(NoodleInstructionType.JUMP_POP, codeLocation);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.setPosition(thread.getJumpStack().pop());
    }
}
