package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction operates as follows:
 *  - If the value on the stack is true, the value is popped.
 *  - If the value on the stack is false, jump to a stored position.
 */
@Getter
@Setter
public class NoodleInstructionBinaryAnd extends NoodleInstruction {
    private int failJump;

    public NoodleInstructionBinaryAnd() {
        super(NoodleInstructionType.BAND);
    }

    public NoodleInstructionBinaryAnd(NoodleCodeLocation codeLocation, int failJump) {
        super(NoodleInstructionType.BAND, codeLocation);
        this.failJump = failJump;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(getLabelName(script, this.failJump));
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive prim = thread.getStack().peek();
        if (prim.isTrueValue()) {
            thread.getStack().popWithGC();
        } else {
            thread.setPosition(this.failJump);
        }
    }
}
