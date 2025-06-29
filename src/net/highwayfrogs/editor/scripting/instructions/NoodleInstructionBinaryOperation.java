package net.highwayfrogs.editor.scripting.instructions;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.compiler.NoodleOperator;
import net.highwayfrogs.editor.scripting.runtime.*;
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
        } else if (a.isObjectReference() || b.isObjectReference()) {
            if (operator == NoodleOperator.ADD) {
                return stack.pushObject(a.getAsString() + b.getAsString());
            } else {
                throw new NoodleRuntimeException("Can't apply %s operator to `%s` and `%s`.", operator, a, b);
            }
        } else {
            switch (operator) {
                case ADD:
                    return stack.pushPrimitive(addPrimitives(a, b));
                case SUB:
                    return stack.pushPrimitive(subtractPrimitives(a, b));
                case MUL:
                    return stack.pushPrimitive(multiplyPrimitives(a, b));
                case DIV:
                    return stack.pushPrimitive(dividePrimitives(a, b));
                case MOD:
                    return stack.pushPrimitive(moduloPrimitives(a, b));
                case SHL: // Bitwise operations can only be done on integers. This is a java limitation, however I can't think of any use-cases where we'd possibly need to bit-shift floating point numbers.
                    if (!a.isIntegerNumber() || !b.isIntegerNumber())
                        throw new NoodleRuntimeException("Cannot shift %s left by %s bits.", a, b);
                    return stack.pushNumber(a.getPrimitiveType(), a.getWholeNumber() << b.getInteger());
                case SHR:
                    if (!a.isIntegerNumber() || !b.isIntegerNumber())
                        throw new NoodleRuntimeException("Cannot shift %s right by %s bits.", a, b);
                    return stack.pushNumber(a.getPrimitiveType(), a.getWholeNumber() >> b.getInteger());
                case LAND:
                    if (!a.isIntegerNumber() || !b.isIntegerNumber())
                        throw new NoodleRuntimeException("Cannot bitwise-AND %s against %s.", a, b);
                    return stack.pushNumber(a.getPrimitiveType(), a.getWholeNumber() & b.getWholeNumber());
                case LOR:
                    if (!a.isIntegerNumber() || !b.isIntegerNumber())
                        throw new NoodleRuntimeException("Cannot bitwise-OR %s against %s.", a, b);
                    return stack.pushNumber(a.getPrimitiveType(), a.getWholeNumber() | b.getWholeNumber());
                case LXOR:
                    if (!a.isIntegerNumber() || !b.isIntegerNumber())
                        throw new NoodleRuntimeException("Cannot bitwise-XOR %s against: %s.", a, b);
                    return stack.pushNumber(a.getPrimitiveType(), a.getWholeNumber() ^ b.getWholeNumber());
                case LT:
                    return stack.pushBoolean(compareValues(a, b, ComparisonResult.A_LESS_THAN_B, false));
                case LTE:
                    return stack.pushBoolean(compareValues(a, b, ComparisonResult.A_LESS_THAN_B, true));
                case GT:
                    return stack.pushBoolean(compareValues(a, b, ComparisonResult.A_GREATER_THAN_B, false));
                case GTE:
                    return stack.pushBoolean(compareValues(a, b, ComparisonResult.A_GREATER_THAN_B, true));
                default:
                    throw new NoodleRuntimeException("Can't apply %s operator to `%s` and `%s`.", operator, a, b);
            }
        }
    }

    private enum ComparisonResult {
        A_EQUALS_B,
        A_GREATER_THAN_B,
        A_LESS_THAN_B
    }

    private static boolean compareValues(NoodlePrimitive a, NoodlePrimitive b, ComparisonResult result, boolean allowEquals) {
        ComparisonResult compareResult = compareValues(a, b);
        return (allowEquals && compareResult == ComparisonResult.A_EQUALS_B) || compareResult == result;
    }

    private static ComparisonResult compareValues(NoodlePrimitive a, NoodlePrimitive b) {
        if (a.isDecimalNumber()) {
            double valueA = a.getDecimal();
            if (b.isDecimalNumber()) {
                double valueB = b.getDecimal();
                if (valueA == valueB) {
                    return ComparisonResult.A_EQUALS_B;
                } else if (valueA > valueB) {
                    return ComparisonResult.A_GREATER_THAN_B;
                } else {
                    return ComparisonResult.A_LESS_THAN_B;
                }
            } else {
                long valueB = b.getWholeNumber();
                if (valueA == valueB) {
                    return ComparisonResult.A_EQUALS_B;
                } else if (valueA > valueB) {
                    return ComparisonResult.A_GREATER_THAN_B;
                } else {
                    return ComparisonResult.A_LESS_THAN_B;
                }
            }
        } else {
            long valueA = a.getWholeNumber();
            if (b.isDecimalNumber()) {
                double valueB = b.getDecimal();
                if (valueA == valueB) {
                    return ComparisonResult.A_EQUALS_B;
                } else if (valueA > valueB) {
                    return ComparisonResult.A_GREATER_THAN_B;
                } else {
                    return ComparisonResult.A_LESS_THAN_B;
                }
            } else {
                long valueB = b.getWholeNumber();
                if (valueA == valueB) {
                    return ComparisonResult.A_EQUALS_B;
                } else if (valueA > valueB) {
                    return ComparisonResult.A_GREATER_THAN_B;
                } else {
                    return ComparisonResult.A_LESS_THAN_B;
                }
            }
        }
    }

    private static NoodlePrimitive addPrimitives(NoodlePrimitive a, NoodlePrimitive b) {
        NoodlePrimitiveType newType = NoodlePrimitiveType.getPreferredType(a.getPrimitiveType(), b.getPrimitiveType());

        if (a.isDecimalNumber()) {
            if (b.isIntegerNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() + b.getWholeNumber());
            } else if (b.isDecimalNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() + b.getDecimal());
            }
        } else if (b.isDecimalNumber()) {
            if (a.isIntegerNumber())
                return new NoodlePrimitive(newType, a.getWholeNumber() + b.getDouble());
        } else if (a.isIntegerNumber() && b.isIntegerNumber()) {
            return new NoodlePrimitive(newType, a.getWholeNumber() + b.getWholeNumber());
        }

        throw new NoodleRuntimeException("Cannot add %s and %s together.", a, b);
    }

    private static NoodlePrimitive subtractPrimitives(NoodlePrimitive a, NoodlePrimitive b) {
        NoodlePrimitiveType newType = NoodlePrimitiveType.getPreferredType(a.getPrimitiveType(), b.getPrimitiveType());

        if (a.isDecimalNumber()) {
            if (b.isIntegerNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() - b.getWholeNumber());
            } else if (b.isDecimalNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() - b.getDecimal());
            }
        } else if (b.isDecimalNumber()) {
            if (a.isIntegerNumber())
                return new NoodlePrimitive(newType, a.getWholeNumber() - b.getDouble());
        } else if (a.isIntegerNumber() && b.isIntegerNumber()) {
            return new NoodlePrimitive(newType, a.getWholeNumber() - b.getWholeNumber());
        }

        throw new NoodleRuntimeException("Cannot subtract %s from %s.", b, a);
    }

    private static NoodlePrimitive multiplyPrimitives(NoodlePrimitive a, NoodlePrimitive b) {
        NoodlePrimitiveType newType = NoodlePrimitiveType.getPreferredType(a.getPrimitiveType(), b.getPrimitiveType());
        if (a.isDecimalNumber()) {
            if (b.isIntegerNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() * b.getWholeNumber());
            } else if (b.isDecimalNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() * b.getDecimal());
            }
        } else if (b.isDecimalNumber()) {
            if (a.isIntegerNumber())
                return new NoodlePrimitive(newType, a.getWholeNumber() * b.getDouble());
        } else if (a.isIntegerNumber() && b.isIntegerNumber()) {
            return new NoodlePrimitive(newType, a.getWholeNumber() * b.getWholeNumber());
        }

        throw new NoodleRuntimeException("Cannot multiply %s by %s.", b, a);
    }

    private static NoodlePrimitive dividePrimitives(NoodlePrimitive a, NoodlePrimitive b) {
        if ((b.isIntegerNumber() && b.getWholeNumber() == 0) || (b.isDecimalNumber() && b.getDecimal() == 0))
            throw new NoodleRuntimeException("Tried to divide %f by zero!", a);

        NoodlePrimitiveType newType = NoodlePrimitiveType.getPreferredType(a.getPrimitiveType(), b.getPrimitiveType());
        if (a.isDecimalNumber()) {
            if (b.isIntegerNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() / b.getWholeNumber());
            } else if (b.isDecimalNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() / b.getDecimal());
            }
        } else if (b.isDecimalNumber()) {
            if (a.isIntegerNumber())
                return new NoodlePrimitive(newType, a.getWholeNumber() / b.getDouble());
        } else if (a.isIntegerNumber() && b.isIntegerNumber()) {
            return new NoodlePrimitive(newType, a.getWholeNumber() / b.getWholeNumber());
        }

        throw new NoodleRuntimeException("Cannot divide %s by %s.", b, a);
    }

    private static NoodlePrimitive moduloPrimitives(NoodlePrimitive a, NoodlePrimitive b) {
        if ((b.isIntegerNumber() && b.getWholeNumber() == 0) || (b.isDecimalNumber() && b.getDecimal() == 0))
            throw new NoodleRuntimeException("Tried to modulo %f by zero!", a);

        NoodlePrimitiveType newType = NoodlePrimitiveType.getPreferredType(a.getPrimitiveType(), b.getPrimitiveType());
        if (a.isDecimalNumber()) {
            if (b.isIntegerNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() % b.getWholeNumber());
            } else if (b.isDecimalNumber()) {
                return new NoodlePrimitive(newType, a.getDecimal() % b.getDecimal());
            }
        } else if (b.isDecimalNumber()) {
            if (a.isIntegerNumber())
                return new NoodlePrimitive(newType, a.getWholeNumber() % b.getDouble());
        } else if (a.isIntegerNumber() && b.isIntegerNumber()) {
            return new NoodlePrimitive(newType, a.getWholeNumber() % b.getWholeNumber());
        }

        throw new NoodleRuntimeException("Cannot modulo %s by %s.", b, a);
    }
}