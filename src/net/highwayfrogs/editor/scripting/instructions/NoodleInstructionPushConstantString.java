package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction pushes a constant (read: encoded in the instruction) string to the stack.
 */
@Getter
public class NoodleInstructionPushConstantString extends NoodleInstruction {
    private String stringValue;

    public NoodleInstructionPushConstantString() {
        super(NoodleInstructionType.PUSHSTR);
    }

    public NoodleInstructionPushConstantString(NoodleCodeLocation codeLocation, String stringValue) {
        super(NoodleInstructionType.PUSHSTR, codeLocation);
        this.stringValue = stringValue;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" \"").append(NoodleUtils.compiledStringToCodeString(this.stringValue)).append("\"");
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.getStack().pushObject(this.stringValue);
    }
}
