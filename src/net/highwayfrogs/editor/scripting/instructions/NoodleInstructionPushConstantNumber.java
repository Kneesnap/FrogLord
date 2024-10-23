package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.utils.Utils;

/**
 * This instruction pushes a constant (read: encoded in the instruction) number to the stack.
 */
@Getter
public class NoodleInstructionPushConstantNumber extends NoodleInstruction {
    private double numberValue;

    public NoodleInstructionPushConstantNumber() {
        super(NoodleInstructionType.PUSHNUM);
    }

    public NoodleInstructionPushConstantNumber(NoodleCodeLocation codeLocation, double numberValue) {
        super(NoodleInstructionType.PUSHNUM, codeLocation);
        this.numberValue = numberValue;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(Utils.doubleToCleanString(this.numberValue));
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.getStack().pushNumber(this.numberValue);
    }
}
