package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.Objects;

/**
 * This instruction chooses a code-path based on a compile-time value matching the value on the stack.
 * Unlike a select statement, switch statements can execute multiple cases.
 */
@Getter
@Setter
public class NoodleInstructionSwitchJump extends NoodleInstruction {
    private int jumpPosition;

    public NoodleInstructionSwitchJump() {
        super(NoodleInstructionType.SWITCH_JUMP);
    }

    public NoodleInstructionSwitchJump(NoodleCodeLocation codeLocation, int jumpPosition) {
        super(NoodleInstructionType.SWITCH_JUMP, codeLocation);
        this.jumpPosition = jumpPosition;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(getLabelName(script, this.jumpPosition));
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive testValue = thread.getStack().popWithGC();
        if (Objects.equals(testValue, thread.getStack().peek())) {
            thread.getStack().popWithGC();
            thread.setPosition(this.jumpPosition);
        }
    }
}
