package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * The jump instruction changes the instruction which code executes from.
 */
@Getter
@Setter
public class NoodleInstructionJump extends NoodleInstruction {
    private int jumpPosition;

    public NoodleInstructionJump() {
        super(NoodleInstructionType.JUMP);
    }

    public NoodleInstructionJump(NoodleCodeLocation codeLocation, int value) {
        super(NoodleInstructionType.JUMP, codeLocation);
        this.jumpPosition = value;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(getLabelName(script, this.jumpPosition));
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.setPosition(this.jumpPosition);
    }
}
