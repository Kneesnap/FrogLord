package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.compiler.NoodleUnaryOperator;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * This instruction performs a unary operation on the last stack value, placing the result back onto the stack.
 */
@Getter
public class NoodleInstructionUnaryOperation extends NoodleInstruction {
    private NoodleUnaryOperator operator;

    public NoodleInstructionUnaryOperation() {
        super(NoodleInstructionType.UNARY_OP);
    }

    public NoodleInstructionUnaryOperation(NoodleCodeLocation codeLocation, NoodleUnaryOperator operator) {
        super(NoodleInstructionType.UNARY_OP, codeLocation);
        this.operator = operator;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.operator);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive primitive = thread.getStack().popWithGC();
        if (this.operator == NoodleUnaryOperator.INVERT) {
            thread.getStack().pushBoolean(!primitive.isTrueValue());
        } else {
            if (!primitive.isNumber())
                throw new NoodleRuntimeException("Can only apply unary negate to a number.");

            thread.getStack().pushNumber(-primitive.getNumberValue());
        }
    }
}