package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction operates as follows:
 *  - If the value on the stack is true, jump to a stored position.
 *  - If the value on the stack is false, the value is popped.
 */
@Getter
@Setter
public class NoodleInstructionBinaryOr extends NoodleInstruction {
    private int jumpTo;

    public NoodleInstructionBinaryOr() {
        super(NoodleInstructionType.BOR);
    }

    public NoodleInstructionBinaryOr(NoodleCodeLocation codeLocation, int jumpTo) {
        super(NoodleInstructionType.BOR, codeLocation);
        this.jumpTo = jumpTo;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(getLabelName(script, this.jumpTo));
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive prim = thread.getStack().peek();
        if (prim.isTrueValue()) {
            thread.setPosition(this.jumpTo);
        } else {
            thread.getStack().popWithGC();
        }
    }
}
