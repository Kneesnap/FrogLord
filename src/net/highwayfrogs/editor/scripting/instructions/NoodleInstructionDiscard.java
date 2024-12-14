package net.highwayfrogs.editor.scripting.instructions;

import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * The "DISCARD" instruction will pop (& discard) a value from the stack.
 */
public class NoodleInstructionDiscard extends NoodleInstruction {
    public NoodleInstructionDiscard() {
        super(NoodleInstructionType.DISCARD);
    }

    public NoodleInstructionDiscard(NoodleCodeLocation codeLocation) {
        super(NoodleInstructionType.DISCARD, codeLocation);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.getStack().popWithGC();
    }
}
