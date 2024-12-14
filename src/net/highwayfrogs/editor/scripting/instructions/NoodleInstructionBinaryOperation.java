package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.compiler.NoodleOperator;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleStack;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.Objects;

/**
 * This instruction performs a binary operation on the two most recent stack values, and stores the result back on the stack.
 */
@Getter
public class NoodleInstructionBinaryOperation extends NoodleInstruction {
    private NoodleOperator operator;

    private static final ThreadLocal<NoodleStack> OPERATION_STACK = ThreadLocal.withInitial(NoodleStack::new);

    public NoodleInstructionBinaryOperation() {
        super(NoodleInstructionType.BINARY_OP);
    }

    public NoodleInstructionBinaryOperation(NoodleCodeLocation codeLocation, NoodleOperator operator) {
        super(NoodleInstructionType.BINARY_OP, codeLocation);
        this.operator = operator;
    }

    @Override
    public void toString(StringBuilder builder, NoodleScript script) {
        super.toString(builder, script);
        builder.append(" ").append(this.operator);
    }

    @Override
    public void execute(NoodleThread<? extends NoodleScript> thread) {
        NoodlePrimitive b = thread.getStack().popWithGC();
        NoodlePrimitive a = thread.getStack().popWithGC();
        executeBinaryOperation(thread.getStack(), a, b, this.operator);
    }

    /**
     * Executes a binary operation on two values, returning the result of the operation.
     * @param a The value on the left side of the operation.
     * @param b The value on the right side of the operation.
     * @param operator The binary operator representing the operation to apply.
     * @return operationResult
     */
    public static NoodlePrimitive executeBinaryOperation(NoodlePrimitive a, NoodlePrimitive b, NoodleOperator operator) {
        NoodleStack stack = OPERATION_STACK.get();
        executeBinaryOperation(stack, a, b, operator);
        return stack.popWithGC();
    }

    /**
     * Executes a binary operation on two values, putting the result on the stack.
     * @param stack The stack which primitives come from.
     * @param a The value on the left side of the operation.
     * @param b The value on the right side of the operation.
     * @param operator The binary operator representing the operation to apply.
     * @return operationResult
     */
    public static NoodlePrimitive executeBinaryOperation(NoodleStack stack, NoodlePrimitive a, NoodlePrimitive b, NoodleOperator operator) {
        if (operator == NoodleOperator.EQ) {
            return stack.pushBoolean(Objects.equals(a, b));
        } else if (operator == NoodleOperator.NEQ) {
            return stack.pushBoolean(!Objects.equals(a, b));
        } else if (a.isString() || b.isString()) {
            if (operator == NoodleOperator.ADD) {
                return stack.pushString(a.getAsString() + b.getAsString());
            } else {
                throw new NoodleRuntimeException("Can't apply %s operator to `%s` and `%s`.", operator, a, b);
            }
        } else if (a.isNumber() && b.isNumber()) {
            double aVal = a.getNumberValue();
            double bVal = b.getNumberValue();

            switch (operator) {
                case ADD:
                    aVal += bVal;
                    break;
                case SUB:
                    aVal -= bVal;
                    break;
                case MUL:
                    aVal *= bVal;
                    break;
                case DIV:
                    if (bVal == 0)
                        throw new NoodleRuntimeException("Tried to divide %f by zero!", aVal);
                    aVal /= bVal;
                    break;
                case MOD:
                    if (bVal == 0)
                        throw new NoodleRuntimeException("Tried to modulo %f by zero!", aVal);
                    aVal %= bVal;
                    break;
                case SHL: // Bitwise operations can only be done on integers. This is a java limitation, however I can't think of any use-cases where we'd possibly need to bit-shift floating point numbers.
                    aVal = (((int) aVal) << (int) bVal);
                    break;
                case SHR:
                    aVal = (((int) aVal) >> (int) bVal);
                    break;
                case LAND:
                    aVal = ((int) aVal) & ((int) bVal);
                    break;
                case LOR:
                    aVal = ((int) aVal) | ((int) bVal);
                    break;
                case LXOR:
                    aVal = ((int) aVal) ^ ((int) bVal);
                    break;
                case LT:
                    aVal = (aVal < bVal) ? 1D : 0D;
                    break;
                case LTE:
                    aVal = (aVal <= bVal) ? 1D : 0D;
                    break;
                case GT:
                    aVal = (aVal > bVal) ? 1D : 0D;
                    break;
                case GTE:
                    aVal = (aVal >= bVal) ? 1D : 0D;
                    break;
                default:
                    throw new NoodleRuntimeException("Can't perform unsupported binary operation `%s`.", operator);
            }

            return stack.pushNumber(aVal);
        } else {
            throw new NoodleRuntimeException("Can't apply %s operator to `%s` and `%s`.", operator, a, b);
        }
    }
}