package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction pushes a constant (read: encoded in the instruction) number to the stack.
 */
@Getter
public class NoodleInstructionPushPrimitive extends NoodleInstruction {
    private NoodlePrimitive primitive;

    public NoodleInstructionPushPrimitive() {
        super(NoodleInstructionType.PUSHPRIM);
    }

    public NoodleInstructionPushPrimitive(NoodleCodeLocation codeLocation, NoodlePrimitive primitive) {
        super(NoodleInstructionType.PUSHPRIM, codeLocation);
        this.primitive = primitive;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.primitive.getAsString());
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        if (this.primitive != null) {
            if (this.primitive.isObjectReference() && !this.primitive.isNull())
                throw new NoodleRuntimeException("The PushPrimitive instruction cannot push non-null objects reference primitives! [%s]", this.primitive);

            thread.getStack().pushPrimitive(this.primitive);
        } else {
            thread.getStack().pushNull();
        }
    }
}
