package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This jump instruction changes the instruction which code executes from, if a value popped from the stack is true.
 */
@Getter
@Setter
public class NoodleInstructionJumpIf extends NoodleInstruction {
    private int jumpPosition;

    public NoodleInstructionJumpIf() {
        super(NoodleInstructionType.JUMP_IF);
    }

    public NoodleInstructionJumpIf(NoodleCodeLocation codeLocation, int jumpPosition) {
        super(NoodleInstructionType.JUMP_IF, codeLocation);
        this.jumpPosition = jumpPosition;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(getLabelName(script, this.jumpPosition));
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        if (thread.getStack().popWithGC().isTrueValue())
            thread.setPosition(this.jumpPosition);
    }
}
