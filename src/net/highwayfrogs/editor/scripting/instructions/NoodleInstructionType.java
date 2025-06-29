package net.highwayfrogs.editor.scripting.instructions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

/**
 * A registry of script instruction types.
 */
@Getter
@AllArgsConstructor
public enum NoodleInstructionType {
    PUSHPRIM(NoodleInstructionPushPrimitive::new), // (value): push(value)
    PUSHVAR(NoodleInstructionPushIdentifier::new), // (name): push(self[name])
    PUSHARG(NoodleInstructionPushArgument::new), // push(args[arg_num])
    PUSHFIELD(NoodleInstructionPushField::new), // push(pop().name)
    PUSHNULL(NoodleInstructionPushNull::new), // push(null)
    UNARY_OP(NoodleInstructionUnaryOperation::new), // (unop): push(-pop())
    BINARY_OP(NoodleInstructionBinaryOperation::new), // (op): a = pop(); b = pop(); push(binop(op, a, b))
    CALL(NoodleInstructionCall::new), // (script, argc):
    CALL_INST(NoodleInstructionCallInstance::new), // (script, argc):
    CALL_STATIC(NoodleInstructionCallStatic::new), // (script, argc):
    RET(NoodleInstructionReturn::new), // (): return pop()
    DISCARD(NoodleInstructionDiscard::new), // (): pop() - for when we don't care for output
    JUMP(NoodleInstructionJump::new), // (pos): pc = pos
    JUMP_UNLESS(NoodleInstructionJumpUnless::new), // (pos): if (!pop()) pc = pos
    PUSHSTR(NoodleInstructionPushConstantString::new), // (value:string): push(value)
    SETVAR(NoodleInstructionSetIdentifier::new), // (name:string): self[name] = pop()
    SETFIELD(NoodleInstructionSetField::new), // (name:string): pop().name = pop()
    BAND(NoodleInstructionBinaryAnd::new), // (pos): if (peek()) pop(); else pc = pos
    BOR(NoodleInstructionBinaryOr::new), // (pos): if (peek()) pc = pos; else pop()
    JUMP_IF(NoodleInstructionJumpIf::new), // (pos): if (pop()) pc = pos
    JUMP_PUSH(NoodleInstructionJumpPush::new), // (pos): js.push(pc); pc = pos
    JUMP_POP(NoodleInstructionJumpPop::new), // (): pc = js.pop()
    DUP(NoodleInstructionDuplicate::new), // push(top())
    SWITCH_JUMP(NoodleInstructionSwitchJump::new), // (pos): if (pop() == peek()) { pop(); pc = pos; }
    CREATE_ARRAY(NoodleInstructionCreateArray::new);

    private final Supplier<NoodleInstruction> instructionMaker;
}