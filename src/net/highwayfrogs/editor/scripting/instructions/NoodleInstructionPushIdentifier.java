package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction pushes a named identifier to the stack.
 */
@Getter
public class NoodleInstructionPushIdentifier extends NoodleInstruction {
    private String variableName;

    public NoodleInstructionPushIdentifier() {
        super(NoodleInstructionType.PUSHVAR);
    }

    public NoodleInstructionPushIdentifier(NoodleCodeLocation codeLocation, String variableName) {
        super(NoodleInstructionType.PUSHVAR, codeLocation);
        this.variableName = variableName;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.variableName);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        thread.getStack().pushPrimitive(thread.getHeap().getVariable(this.variableName));
    }
}
