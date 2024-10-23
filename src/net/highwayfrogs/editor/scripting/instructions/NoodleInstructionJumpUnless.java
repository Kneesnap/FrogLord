package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This jump instruction changes the instruction which code executes if the popped value is not true.
 */
@Getter
@Setter
public class NoodleInstructionJumpUnless extends NoodleInstruction {
    private int jumpPosition;

    public NoodleInstructionJumpUnless() {
        super(NoodleInstructionType.JUMP_UNLESS);
    }

    public NoodleInstructionJumpUnless(NoodleCodeLocation codeLocation, int jumpPosition) {
        super(NoodleInstructionType.JUMP_UNLESS, codeLocation);
        this.jumpPosition = jumpPosition;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(getLabelName(script, this.jumpPosition));
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        if (!thread.getStack().popWithGC().isTrueValue())
            thread.setPosition(this.jumpPosition);
    }
}
