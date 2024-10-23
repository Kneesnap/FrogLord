package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This jump instruction changes the instruction to a specified position, pushing the current instruction to the stack so it can be returned to.
 */
@Getter
@Setter
public class NoodleInstructionJumpPush extends NoodleInstruction {
    private int jumpPosition;

    public NoodleInstructionJumpPush() {
        super(NoodleInstructionType.JUMP_PUSH);
    }

    public NoodleInstructionJumpPush(NoodleCodeLocation codeLocation, int jumpPosition) {
        super(NoodleInstructionType.JUMP_PUSH, codeLocation);
        this.jumpPosition = jumpPosition;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(getLabelName(script, this.jumpPosition));
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.getJumpStack().push(thread.getPosition());
        thread.setPosition(this.jumpPosition);
    }
}
