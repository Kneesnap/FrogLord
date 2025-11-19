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
        switch (this.operator) {
            case NOT:
                if (primitive.isBoolean()) {
                    thread.getStack().pushBoolean(!primitive.getBoolean());
                } else if (primitive.isIntegerNumber()) {
                    thread.getStack().pushNumber(primitive.getPrimitiveType(), ~primitive.getWholeNumber());
                } else {
                    throw new NoodleRuntimeException("Cannot apply the unary logical NOT operation to %s.", primitive);
                }
                break;
            case INVERT:
                if (primitive.isNull()) {
                    thread.getStack().pushBoolean(true);
                } else if (primitive.isObjectReference()) {
                    thread.getStack().pushBoolean(false); // Object is not null.
                } else if (primitive.isBoolean()) {
                    thread.getStack().pushBoolean(!primitive.getBoolean());
                } else if (primitive.isIntegerNumber()) {
                    thread.getStack().pushBoolean(!primitive.isTrueValue());
                } else {
                    throw new NoodleRuntimeException("Cannot apply the unary invert operation to %s.", primitive);
                }
                break;
            case NEGATE:
                if (primitive.isDecimalNumber()) {
                    thread.getStack().pushNumber(primitive.getPrimitiveType(), -primitive.getDecimal());
                } else if (primitive.isIntegerNumber()) {
                    thread.getStack().pushNumber(primitive.getPrimitiveType(), -primitive.getWholeNumber());
                } else {
                    throw new NoodleRuntimeException("Cannot apply the unary negate operation to %s.", primitive);
                }
                break;
            default:
                throw new NoodleRuntimeException("Unsupported UnaryOp: '%s'.", this.operator);
        }
    }
}