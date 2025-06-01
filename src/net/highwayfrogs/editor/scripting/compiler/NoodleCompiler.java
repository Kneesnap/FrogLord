package net.highwayfrogs.editor.scripting.compiler;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.scripting.NoodleScriptFunction;
import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.compiler.nodes.*;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodleMacro;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessor;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.builtins.NoodleBuiltin;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.NoodlePreprocessorDirective;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.NoodlePreprocessorDirectiveType;
import net.highwayfrogs.editor.scripting.compiler.tokens.*;
import net.highwayfrogs.editor.scripting.functions.NoodleFunction;
import net.highwayfrogs.editor.scripting.instructions.*;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeSource;
import net.highwayfrogs.editor.scripting.tracking.NoodleFileCodeSource;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Compiles noodle code.
 */
public class NoodleCompiler {
    public static final int FLAG_NO_OPERATORS = 1;
    private static final int SPECIAL_JUMP_CODE_BREAK = -10;
    private static final int SPECIAL_JUMP_CODE_CONTINUE = -11;

    /**
     * Compile a script into a script which can be run.
     * @param engine the engine to compile the script with
     * @param file The file to compile into a script.
     * @return compiledScript
     */
    public static <T extends NoodleScript> T compileScript(NoodleScriptEngine engine, File file, T scriptToLoad) {
        if (!file.exists())
            throw new NoodleCompilerException("Tried to load compile file '%s', because it cannot be found!", file.getName());

        return compileScript(engine, FileUtils.readFileText(file), scriptToLoad);
    }

    /**
     * Compile a script into a script which can be run.
     * @param engine the engine to compile the script with
     * @param fileText     The text to compile.
     * @param scriptToLoad The script to compile into.
     * @return compiledScript
     */
    public static <T extends NoodleScript> T compileScript(NoodleScriptEngine engine, String fileText, T scriptToLoad) {
        Config codeConfig = Config.loadConfigFromString(fileText, scriptToLoad.getName());
        return compileScript(engine, codeConfig, scriptToLoad);
    }

    /**
     * Compile a script into a script which can be run.
     * @param engine the engine to compile the script with
     * @param codeConfig   The text to compile, loaded as a config.
     * @param scriptToLoad The script to compile into.
     * @return compiledScript
     */
    public static <T extends NoodleScript> T compileScript(NoodleScriptEngine engine, Config codeConfig, T scriptToLoad) {
        int startLineNumber = codeConfig.getOriginalLineNumber();
        String codeText = String.join(Constants.NEWLINE, codeConfig.getTextWithComments()) + Constants.NEWLINE; // Adding the extra newline makes it easier on the token parser, aka if we don't it'll error.
        scriptToLoad.clearScript(); // Reset the script object before compiling anything into it.
        scriptToLoad.setConfig(codeConfig);

        // Compile the script.
        NoodleCompileContext context = new NoodleCompileContext(engine, codeText, scriptToLoad);
        context.reset();

        // 1.) Parses the raw text into tokens.
        NoodleFileCodeSource mainSource = context.getCodeSource(scriptToLoad.getSourceFile());
        parseIntoTokens(mainSource, codeText, context.getTokens(), startLineNumber);

        // 2.) Runs the preprocessor on the tokens.
        context.getPreprocessor().runPreprocessor(context, context.getTokens());

        // 3.) Builds an AST from the tokens.
        buildAST(context, mainSource, startLineNumber);

        // 4.) Compiles the AST into Noodle assembly instructions / bytecode.
        compileExpression(context, context.getNode());

        // 5.) Write the compiled data (such as instructions, labels, etc) into the script object.
        context.applyToScript();

        // 6.) Final verifications.
        validateAndSetupScript(scriptToLoad);

        // Done.
        return scriptToLoad;
    }

    /**
     * Validate the validity of a script, and perform any setup.
     * @param script The script to validate and setup.
     */
    public static void validateAndSetupScript(NoodleScript script) {
        for (int i = 0; i < script.getInstructions().size(); i++) {
            NoodleInstruction instruction = script.getInstructions().get(i);
            if (!(instruction instanceof NoodleInstructionCall))
                continue;

            NoodleInstructionCall instructionCall = (NoodleInstructionCall) instruction;
            if (!instructionCall.resolveName(script))
                throw new NoodleSyntaxException("Cannot resolve function '%s(%d args)'.", instructionCall, instructionCall.getFunctionLabel(), instructionCall.getArgumentCount());
        }
    }

    /**
     * Compiles an expression into Noodle bytecode instructions.
     * @param context The compilation context.
     * @param node The node to create an expression from.
     */
    public static void compileExpression(NoodleCompileContext context, NoodleNode node) {
        List<NoodleInstruction> out = context.getInstructions();

        NoodleCodeLocation pos = context.getRuntimeCodeLocation(node.getCodeLocation());
        switch (node.getNodeType()) {
            case NULL:
                out.add(new NoodleInstructionPushNull(pos));
                break;
            case NUMBER:
                out.add(new NoodleInstructionPushConstantNumber(pos, ((NoodleNodeNumber) node).getNumberValue()));
                break;
            case STRING:
                out.add(new NoodleInstructionPushConstantString(pos, ((NoodleNodeString) node).getStringValue()));
                break;
            case EVALUATION_CHAIN:
            case IDENTIFIER:
                compileGetter(context, node);
                break;
            case CALL_STATIC:
                NoodleNodeFunctionCallStatic staticCallNode = ((NoodleNodeFunctionCallStatic) node);

                // Write arguments.
                for (int i = 0; i < staticCallNode.getArguments().size(); i++)
                    compileExpression(context, staticCallNode.getArguments().get(i));

                NoodleObjectTemplate<?> objTemplate = staticCallNode.getObjectTemplate();
                String functionName = staticCallNode.getFunctionName();
                int argumentCount = staticCallNode.getArguments().size();

                // Verify function exists.
                if (objTemplate.getStaticFunction(functionName, argumentCount) == null)
                    throw new NoodleSyntaxException("Static function '%s.%s(%d args)' does not exist.", node, objTemplate.getName(), functionName, argumentCount);

                out.add(new NoodleInstructionCallStatic(pos, objTemplate, functionName, argumentCount));
                break;
            case UNARY_OPERATOR:
                NoodleNodeUnary unary = (NoodleNodeUnary) node;
                compileExpression(context, unary.getNode());
                out.add(new NoodleInstructionUnaryOperation(pos, unary.getUnaryOperator()));
                break;
            case BINARY_OPERATOR:
                NoodleNodeBinaryOp binary = (NoodleNodeBinaryOp) node;
                switch (binary.getOperator()) {
                    case BAND:
                        compileExpression(context, binary.getFirst());
                        NoodleInstructionBinaryAnd instructionAnd = new NoodleInstructionBinaryAnd(pos, 0);
                        out.add(instructionAnd);
                        compileExpression(context, binary.getSecond());
                        instructionAnd.setFailJump(out.size());
                        break;
                    case BOR:
                        compileExpression(context, binary.getFirst());
                        NoodleInstructionBinaryOr instructionOr = new NoodleInstructionBinaryOr(pos, 0);
                        out.add(instructionOr);
                        compileExpression(context, binary.getSecond());
                        instructionOr.setJumpTo(out.size());
                        break;
                    default:
                        compileExpression(context, binary.getFirst());
                        compileExpression(context, binary.getSecond());
                        out.add(new NoodleInstructionBinaryOperation(pos, binary.getOperator()));
                }
                break;
            case CALL:
                NoodleNodeFunctionCall func = (NoodleNodeFunctionCall) node;
                int argCount = func.getArgs().size();
                for (int i = 0; i < argCount; i++)
                    compileExpression(context, func.getArgs().get(i));

                out.add(new NoodleInstructionCall(pos, func.getFunctionName(), argCount));
                break;
            case BLOCK:
                NoodleNodeBlock block = (NoodleNodeBlock) node;
                for (int i = 0; i < block.getNodes().size(); i++)
                    compileExpression(context, block.getNodes().get(i));
                break;
            case RETURN:
                NoodleNode returnValueNode = ((NoodleNodeReturn) node).getReturnValue();
                if (returnValueNode != null) {
                    // There's a return value, so let's include it.
                    compileExpression(context, returnValueNode);
                } else {
                    // No return value, so push null.
                    out.add(new NoodleInstructionPushNull(pos));
                }

                out.add(new NoodleInstructionReturn(pos));
                break;
            case DISCARD:
                compileExpression(context, ((NoodleNodeSingle) node).getNode());
                out.add(new NoodleInstructionDiscard(pos));
                break;
            case IF_THEN: // -> <cond>; jump_unless(l1); <then>; l1:
                NoodleNodeCondition ifThen = (NoodleNodeCondition) node;
                compileExpression(context, ifThen.getCondition());
                NoodleInstructionJumpUnless jump = new NoodleInstructionJumpUnless(pos, 0);
                out.add(jump);
                compileExpression(context, ifThen.getTrueLogic());
                jump.setJumpPosition(out.size());
                break;
            case IF_THEN_ELSE: // -> <cond>; jump_unless(l1); <then>; goto l2; l1: <else>; l2:
                NoodleNodeCondition ifThenElse = (NoodleNodeCondition) node;

                // Compile condition statement.
                compileExpression(context, ifThenElse.getCondition());

                // Jumps to the else block if the condition is false. Otherwise, run the true block.
                NoodleInstructionJumpUnless jumpElse = new NoodleInstructionJumpUnless(pos, 0);
                out.add(jumpElse);
                compileExpression(context, ifThenElse.getTrueLogic());

                // Skips the else block if the true block is executed.
                NoodleInstructionJump jumpThen = new NoodleInstructionJump(pos, 0);
                out.add(jumpThen);

                // Updates jump positions, then writes the false block.
                jumpElse.setJumpPosition(out.size());
                compileExpression(context, ifThenElse.getFalseLogic());
                jumpThen.setJumpPosition(out.size());
                break;
            case SELECT:
            case SWITCH:
                boolean isSelect = (node.getNodeType() == NoodleNodeType.SELECT);
                NoodleNodeSwitch switchNode = (NoodleNodeSwitch) node;
                List<NoodleNode> caseStatements = switchNode.getCaseStatements();
                List<NoodleNode> caseBlocks = switchNode.getCaseBlocks();
                NoodleInstructionSwitchJump[] switchJumps = new NoodleInstructionSwitchJump[caseStatements.size()];

                // Header:
                compileExpression(context, switchNode.getExpression());

                // Add case statements:
                for (int i = 0; i < caseStatements.size(); i++) { // value; switchjump <case label>
                    compileExpression(context, caseStatements.get(i));
                    NoodleInstructionSwitchJump switchInstruction = new NoodleInstructionSwitchJump(pos, 0);
                    switchJumps[i] = switchInstruction;
                    out.add(switchInstruction);
                }

                // Discard if we're hitting the default case. Upon a successful switch_jump instruction jump, the value would be popped, so this is a failsafe.
                out.add(new NoodleInstructionDiscard(pos));

                // Add default jump instruction to jump to the default case.
                NoodleInstructionJump defaultJump = new NoodleInstructionJump(pos, 0);
                out.add(defaultJump);

                // Add case blocks.
                int posStart = out.size();
                for (int i = 0; i < caseStatements.size(); i++) {
                    switchJumps[i].setJumpPosition(out.size());
                    compileExpression(context, caseBlocks.get(i));
                    if (isSelect)
                        out.add(new NoodleInstructionJump(pos, SPECIAL_JUMP_CODE_BREAK));
                }

                // Add default case, if there is one.
                defaultJump.setJumpPosition(out.size());
                if (switchNode.getDefaultCase() != null)
                    compileExpression(context, switchNode.getDefaultCase());

                // Setup break and continuing.
                int posBreak = out.size();
                patchSpecialJumpInstructions(context, posStart, posBreak, posBreak, -1);
                break;
            case SET:
                NoodleNodeSet setNode = (NoodleNodeSet) node;
                if (setNode.getOperator() == NoodleOperator.SET) {
                    NoodleInstruction setter = compileSetter(context, setNode.getDestination());
                    compileExpression(context, setNode.getValue());
                    out.add(setter);
                } else {
                    // a %= b -> get a; get b; fmod; set a
                    NoodleInstruction setter = compileGetterAndSetter(context, setNode.getDestination(), pos);
                    compileExpression(context, setNode.getValue());
                    out.add(new NoodleInstructionBinaryOperation(pos, setNode.getOperator()));
                    out.add(setter);
                }
                break;
            case ADJFIX: // a++; -> get a; push 1; add; set a
                NoodleNodePrePostOperator adjFix = (NoodleNodePrePostOperator) node;
                NoodleInstruction adjfixSetter = compileGetterAndSetter(context, adjFix.getNode(), pos);
                out.add(new NoodleInstructionPushConstantNumber(pos, adjFix.getValue()));
                out.add(new NoodleInstructionBinaryOperation(pos, NoodleOperator.ADD));
                out.add(adjfixSetter);
                break;
            case PREFIX: // ++a; -> get a; push 1; add; dup; set a
                NoodleNodePrePostOperator prefix = (NoodleNodePrePostOperator) node;
                NoodleInstruction prefixSetter = compileGetterAndSetter(context, prefix.getNode(), pos);
                out.add(new NoodleInstructionPushConstantNumber(pos, prefix.getValue()));
                out.add(new NoodleInstructionBinaryOperation(pos, NoodleOperator.ADD));
                out.add(new NoodleInstructionDuplicate(pos));
                out.add(prefixSetter);
                break;
            case POSTFIX: // a++; -> get a; dup; push 1; add; set a
                NoodleNodePrePostOperator postfix = (NoodleNodePrePostOperator) node;
                NoodleInstruction postfixSetter = compileGetterAndSetter(context, postfix.getNode(), pos);
                out.add(new NoodleInstructionDuplicate(pos));
                out.add(new NoodleInstructionPushConstantNumber(pos, postfix.getValue()));
                out.add(new NoodleInstructionBinaryOperation(pos, NoodleOperator.ADD));
                out.add(postfixSetter);
                break;
            case WHILE:
                NoodleNodeWhile whileNode = (NoodleNodeWhile) node;
                // l1: {cont} <condition> jump_unless l2
                int posCont = out.size();
                compileExpression(context, whileNode.getCondition());
                NoodleInstructionJumpUnless whileJump = new NoodleInstructionJumpUnless(pos, 0);
                out.add(whileJump);

                // <loop> jump l1
                posStart = out.size();
                compileExpression(context, whileNode.getLoop());
                out.add(new NoodleInstructionJump(pos, posCont));

                // l2: {break}
                posBreak = out.size();
                whileJump.setJumpPosition(posBreak);
                patchSpecialJumpInstructions(context, posStart, posBreak, posBreak, posCont);
                break;
            case DO_WHILE:
                NoodleNodeWhile doWhileNode = (NoodleNodeWhile) node;
                // l1: <loop>
                posStart = out.size();
                compileExpression(context, doWhileNode.getCondition());

                // l2: {cont} <condition> jump_if l1
                posCont = out.size();
                compileExpression(context, doWhileNode.getLoop());
                out.add(new NoodleInstructionJumpIf(pos, posStart));

                // l3: {break}
                posBreak = out.size();
                patchSpecialJumpInstructions(context, posStart, posBreak, posBreak, posCont);
                break;
            case FOR:
                NoodleNodeFor forNode = (NoodleNodeFor) node;
                compileExpression(context, forNode.getInitial());

                // l1: <condition> jump_unless l3
                int posLoop = out.size();
                compileExpression(context, forNode.getCondition());
                NoodleInstructionJumpUnless jumpFor = new NoodleInstructionJumpUnless(pos, 0);
                out.add(jumpFor);

                // <loop>
                posStart = out.size();
                compileExpression(context, forNode.getBody());

                // l2: {cont} <post> jump l1
                posCont = out.size();
                compileExpression(context, forNode.getPost());
                out.add(new NoodleInstructionJump(pos, posLoop));

                // l3: {break}
                posBreak = out.size();
                jumpFor.setJumpPosition(posBreak);
                patchSpecialJumpInstructions(context, posStart, posBreak, posBreak, posCont);
                break;
            case BREAK:
                out.add(new NoodleInstructionJump(pos, SPECIAL_JUMP_CODE_BREAK));
                break;
            case CONTINUE:
                out.add(new NoodleInstructionJump(pos, SPECIAL_JUMP_CODE_CONTINUE));
                break;
            case LABEL:
                NoodleNodeLabel nodeLabel = (NoodleNodeLabel) node;
                if (context.getLabels().containsKey(nodeLabel.getName()))
                    throw new NoodleSyntaxException("Label '%s' is defined more than once.", node, nodeLabel.getName());

                context.getLabels().put(nodeLabel.getName(), out.size());
                compileExpression(context, nodeLabel.getNode());
                break;
            case JUMP:
            case JUMP_PUSH:
                String labelName = ((NoodleNodeString) node).getStringValue();
                List<NoodleInstruction> labels = context.getLabelJumps().computeIfAbsent(labelName, key -> new ArrayList<>());

                NoodleInstruction newJump = (node.getNodeType() == NoodleNodeType.JUMP ? new NoodleInstructionJump(pos, -1) : new NoodleInstructionJumpPush(pos, -1));
                out.add(newJump);
                labels.add(newJump);
                break;
            case JUMP_POP:
                out.add(new NoodleInstructionJumpPop(pos));
                break;
            case PREPROCESSOR_DIRECTIVE:
                NoodleNodePreprocessorDirective preprocessorDirectiveNode = (NoodleNodePreprocessorDirective) node;
                preprocessorDirectiveNode.getDirective().compile(context, preprocessorDirectiveNode); // Usually does nothing, but this is supported!
                break;
            case DEFINE_FUNCTION:
                NoodleNodeFunctionDefinition functionDefinition = (NoodleNodeFunctionDefinition) node;

                // This instruction here will skip over the function instructions whenever the function is reached during normal execution.
                NoodleInstructionJump functionSkipper = new NoodleInstructionJump(pos, 0);
                out.add(functionSkipper);
                int functionStartAddress = out.size();

                // Write instructions for function body.
                compileExpression(context, functionDefinition.getFunctionBody());

                // If there isn't a return at the end of the function, write one.
                NoodleInstruction lastInstruction = out.get(out.size() - 1);
                if (lastInstruction.getInstructionType() != NoodleInstructionType.RET) {
                    out.add(new NoodleInstructionPushNull(pos)); // Push null onto the stack for the DISCARD call.
                    out.add(new NoodleInstructionReturn(pos));
                }

                // Update the instruction which the function skipper jumps to.
                int functionEndAddress = out.size() - 1;
                functionSkipper.setJumpPosition(functionEndAddress + 1);

                // Update the function registry to point to the right instruction.
                NoodleScriptFunction scriptFunction = context.getFunctionsByDefinition().get(functionDefinition);
                if (scriptFunction == null)
                    throw new NoodleSyntaxException("The definition for '%s' has no NoodleScriptFunction registered.", node, functionDefinition.getFunctionName());

                scriptFunction.setStartAddress(functionStartAddress);
                scriptFunction.setEndAddress(functionEndAddress);
                break;
            default:
                throw new NoodleSyntaxException("Cannot compile '%s' as an expression.", node, node);
        }
    }

    /**
     * Patches all jump instructions which represent special jumps such as 'break' or 'continue' to jump to the correct instruction.
     * @param context The context to build for.
     * @param start The first instruction to search which might be a jump.
     * @param till The index after the last instruction which we should check for jump instructions.
     * @param _break The position to make any 'break' instructions should jump to. Values less than zero indicate 'break' is not valid in this section.
     * @param _continue The position to make any 'continue' instructions should jump to. Values less than zero indicate 'continue' is not valid in this section.
     */
    private static void patchSpecialJumpInstructions(NoodleCompileContext context, int start, int till, int _break, int _continue) {
        for (int i = start; i < till; i++) {
            NoodleInstruction instruction = context.getInstructions().get(i);
            if (instruction.getInstructionType() == NoodleInstructionType.JUMP) {
                NoodleInstructionJump jump = (NoodleInstructionJump) instruction;
                switch (jump.getJumpPosition()) {
                    case SPECIAL_JUMP_CODE_BREAK:
                        if (_break >= 0)
                            jump.setJumpPosition(_break);
                        break;
                    case SPECIAL_JUMP_CODE_CONTINUE:
                        if (_continue >= 0)
                            jump.setJumpPosition(_continue);
                        break;
                }
            }
        }
    }

    /**
     * Takes a node, and writes instructions which will put the number
     * Compiles a getter instruction sequence.
     * @param context The context to write to.
     * @param node The node to write a getter for.
     */
    private static void compileGetter(NoodleCompileContext context, NoodleNode node) {
        List<NoodleInstruction> out = context.getInstructions();
        NoodleCodeLocation pos = context.getRuntimeCodeLocation(node.getCodeLocation());

        // Handle chain.
        if (node.getNodeType() == NoodleNodeType.EVALUATION_CHAIN) {
            compileGetterChain(context, (NoodleNodeEvaluationChain) node, pos);
            return;
        }

        // Verify it's an identifier.
        if (node.getNodeType() != NoodleNodeType.IDENTIFIER)
            throw new NoodleSyntaxException("Cannot create getter for node of type '%s'.", node, node.getNodeType());

        // Try to get this as a constant.
        String strValue = ((NoodleNodeString) node).getStringValue();
        NoodlePrimitive constant = context.getEngine().getConstantByName(strValue);
        if (constant != null) {
            if (constant.isString()) {
                out.add(new NoodleInstructionPushConstantString(pos, constant.getStringValue()));
            } else {
                out.add(new NoodleInstructionPushConstantNumber(pos, constant.getNumberValue()));
            }
        } else if (context.getEngine().getBuiltinManager().hasSystemMacroNamed(strValue)) {
            context.getEngine().getBuiltinManager().generateSystemMacroInstructions(strValue, pos, out);
        } else if (context.getMainArgumentNames().contains(strValue)) {
            int argIndex = context.getMainArgumentNames().indexOf(strValue);
            out.add(new NoodleInstructionPushArgument(pos, (short) argIndex));
        } else {
            out.add(new NoodleInstructionPushIdentifier(pos, strValue));
        }
    }

    private static NoodleNode compileChain(NoodleCompileContext context, NoodleNodeEvaluationChain node, NoodleCodeLocation pos, boolean isTopLayer) {
        if (isTopLayer) {
            compileExpression(context, node.getCurrentNode());
        } else {
            compileGetterChainNode(context, node.getCurrentNode(), pos);
        }

        NoodleNode remainingNode = node.getRemainingChainNode();
        if (remainingNode.getNodeType() == NoodleNodeType.EVALUATION_CHAIN) {
            return compileChain(context, (NoodleNodeEvaluationChain) remainingNode, pos, false);
        } else {
            return remainingNode;
        }
    }

    private static void compileGetterChain(NoodleCompileContext context, NoodleNodeEvaluationChain node, NoodleCodeLocation pos) {
        NoodleNode finalNode = compileChain(context, node, pos, true);
        compileGetterChainNode(context, finalNode, pos);
    }

    /**
     * Compiles a single node in a getter evaluation chain.
     * @param context The context to compile under.
     * @param node The node to compile.
     */
    private static void compileGetterChainNode(NoodleCompileContext context, NoodleNode node, NoodleCodeLocation pos) {
        List<NoodleInstruction> out = context.getInstructions();

        if (node.getNodeType() == NoodleNodeType.IDENTIFIER) {
            String fieldName = ((NoodleNodeString) node).getStringValue();
            out.add(new NoodleInstructionPushField(pos, fieldName));
        } else if (node.getNodeType() == NoodleNodeType.CALL) {
            NoodleNodeFunctionCall func = (NoodleNodeFunctionCall) node;
            int argCount = func.getArgs().size();
            for (int i = 0; i < argCount; i++)
                compileExpression(context, func.getArgs().get(i));

            out.add(new NoodleInstructionCallInstance(pos, func.getFunctionName(), argCount));
        } else {
            throw new NoodleSyntaxException("The node type '%s' is not supported during an evaluation-chain.", node, node.getNodeType());
        }
    }

    /**
     * Attempts to turn a setter expression into a sequence of Noodle bytecode instructions.
     * @param context The context to write to.
     * @param node The node which holds the variable that gets written to.
     */
    private static NoodleInstruction compileSetter(NoodleCompileContext context, NoodleNode node) {
        List<NoodleInstruction> out = context.getInstructions();
        NoodleCodeLocation pos = context.getRuntimeCodeLocation(node.getCodeLocation());

        if (node.getNodeType() == NoodleNodeType.EVALUATION_CHAIN)
            return compileSetterChain(context, (NoodleNodeEvaluationChain) node, pos);

        // Add identifier.
        return compileSetterSingle(context, node, pos);
    }

    private static NoodleInstruction compileSetterSingle(NoodleCompileContext context, NoodleNode node, NoodleCodeLocation pos) {
        if (node.getNodeType() != NoodleNodeType.IDENTIFIER)
            throw new NoodleSyntaxException("Cannot create setter for node of type '%s'.", node, node.getNodeType());

        String identName = ((NoodleNodeString) node).getStringValue();
        if (!canIdentifierBeAssigned(context, identName))
            throw new NoodleSyntaxException("Cannot modify read-only constant: '%s'.", node, identName);

        // Add identifier.
        return new NoodleInstructionSetIdentifier(pos, identName);
    }

    private static boolean canIdentifierBeAssigned(NoodleCompileContext context, String identifier) {
        return context.getEngine().getConstantByName(identifier) == null
                && !context.getEngine().getBuiltinManager().hasSystemMacroNamed(identifier)
                && !context.getMainArgumentNames().contains(identifier);
    }

    private static NoodleInstruction compileSetterChain(NoodleCompileContext context, NoodleNodeEvaluationChain node, NoodleCodeLocation pos) {
        NoodleNode finalNode = compileChain(context, node, pos, true);

        if (finalNode.getNodeType() == NoodleNodeType.IDENTIFIER) {
            String fieldName = ((NoodleNodeString) finalNode).getStringValue();
            return new NoodleInstructionSetField(pos, fieldName);
        } else {
            throw new NoodleSyntaxException("The node type '%s' can not be set to a value.", finalNode, finalNode.getNodeType());
        }
    }

    private static NoodleInstruction compileGetterAndSetter(NoodleCompileContext context, NoodleNode node, NoodleCodeLocation pos) {
        if (node.getNodeType() == NoodleNodeType.EVALUATION_CHAIN) {
            NoodleNode fieldNode = compileChain(context, (NoodleNodeEvaluationChain) node, pos, true);
            if (fieldNode.getNodeType() != NoodleNodeType.IDENTIFIER)
                throw new NoodleSyntaxException("The node type '%s' can not have its value assigned. (Expected IDENTIFIER)", fieldNode, fieldNode.getNodeType());

            String fieldName = ((NoodleNodeString) fieldNode).getStringValue();
            context.getInstructions().add(new NoodleInstructionDuplicate(pos)); // Duplicate the reference so
            context.getInstructions().add(new NoodleInstructionPushField(pos, fieldName)); // Put the value on the stack.
            return new NoodleInstructionSetField(pos, fieldName);
        } else if (node.getNodeType() == NoodleNodeType.IDENTIFIER) {
            compileGetter(context, node);
            return compileSetterSingle(context, node, pos);
        } else {
            throw new NoodleSyntaxException("The node type '%s' can not have its value assigned.", node, node.getNodeType());
        }
    }

    /**
     * Builds the abstract syntax tree used to represent the source code before compilation.
     * @param context The context to build the AST with.
     */
    private static void buildAST(NoodleCompileContext context, NoodleCodeSource source, int startLineNumber) {
        // Build the tree.
        while (context.hasMoreTokens()) {
            buildStatement(context);
            context.getNodes().add(context.getNode());
        }

        NoodleCodeLocation codeLocation = new NoodleCodeLocation(source, startLineNumber, 1);
        context.setNode(new NoodleNodeBlock(codeLocation, context.getNodes()));
    }

    /**
     * Builds a statement AST node from the tokens.
     * @param context The context to build from.
     */
    public static void buildStatement(NoodleCompileContext context) {
        NoodleToken tkn;
        NoodleToken tk = context.getCurrentTokenIncrement();
        List<NoodleNode> args;
        List<NoodleNode> options;
        NoodleNode defaultCase = null;
        boolean closed = false;

        switch (tk.getTokenType()) {
            case RETURN: // return [expr]
                NoodleToken token = context.getCurrentToken();

                // If there's a return value, read it.
                NoodleNode returnValue = null;
                if (token.getTokenType() != NoodleTokenType.SEMICOLON && token.getTokenType() != NoodleTokenType.CUB_CLOSE) {
                    buildExpression(context, 0);
                    returnValue = context.getNode();
                }

                // Create return node.
                context.setNode(new NoodleNodeReturn(tk.getCodeLocation(), returnValue));
                break;
            case IF: // if <condition-expr> <then-statement> [else <else-statement>]
                buildExpression(context, 0);
                NoodleNode cond = context.getNode();
                buildStatement(context);
                NoodleNode then = context.getNode();
                tkn = context.getCurrentToken();
                if (tkn.getTokenType() == NoodleTokenType.ELSE) { // else <else-statement>
                    context.incrementToken();
                    buildStatement(context);
                    context.setNode(new NoodleNodeCondition(NoodleNodeType.IF_THEN_ELSE, tk.getCodeLocation(), cond, then, context.getNode()));
                } else {
                    context.setNode(new NoodleNodeCondition(NoodleNodeType.IF_THEN, tk.getCodeLocation(), cond, then, null));
                }
                break;
            case SELECT: // select func(...) { option <v1>: <x1>; option <v2>: <x2> }
            case SWITCH: // switch (expr) { ...cases [default:] }
                boolean isSelect = (tk.getTokenType() == NoodleTokenType.SELECT);
                buildExpression(context, 0);
                NoodleNode expr = context.getNode();
                String tokenName = isSelect ? "select" : "switch";

                // this is pretty much the same as select but without argument packing.
                tkn = context.getCurrentTokenIncrement();
                if (tkn.getTokenType() != NoodleTokenType.CUB_OPEN)
                    throw new NoodleSyntaxException("Expected a `{` for the %s statement, but got '%s' instead.", tkn, tokenName, tkn);

                args = new ArrayList<>();
                options = new ArrayList<>();
                while (context.hasMoreTokens()) {
                    tkn = context.getCurrentTokenIncrement();
                    if (tkn.getTokenType() == NoodleTokenType.CUB_CLOSE) {
                        closed = true;
                        break;
                    } else if (tkn.getTokenType() == NoodleTokenType.CASE || tkn.getTokenType() == NoodleTokenType.DEFAULT) {
                        List<NoodleNode> nodes = new ArrayList<>();
                        if (tkn.getTokenType() == NoodleTokenType.CASE) { // case <value>: ...statements
                            buildExpression(context, 0);
                            args.add(context.getNode());
                            options.add(new NoodleNodeBlock(tk.getCodeLocation(), nodes));
                        } else { // default: ...statements
                            defaultCase = new NoodleNodeBlock(tk.getCodeLocation(), nodes);
                        }

                        // Verify there's a colon.
                        tkn = context.getCurrentTokenIncrement();
                        if (tkn.getTokenType() != NoodleTokenType.COLON)
                            throw new NoodleSyntaxException("Expected a `:` for the %s case, but got '%s' instead.", tkn, tokenName, tkn);

                        // now read statements until we hit `case` or `}`:
                        while (context.hasMoreTokens()) {
                            tkn = context.getCurrentToken();
                            if (tkn.getTokenType() == NoodleTokenType.CUB_CLOSE || tkn.getTokenType() == NoodleTokenType.CASE || tkn.getTokenType() == NoodleTokenType.DEFAULT)
                                break;

                            // Attempt to build.
                            boolean oldBreakState = context.isBuildCanBreak();
                            context.setBuildCanBreak(!isSelect);
                            buildStatement(context);
                            context.setBuildCanBreak(oldBreakState);
                            nodes.add(context.getNode());
                        }
                    } else {
                        throw new NoodleSyntaxException("Expected `case`, `default`, or `}`, but got '%s' instead.", tkn, tkn);
                    }
                }

                if (!closed)
                    throw new NoodleSyntaxException("Expected a `}` to end the %s statement, but got '%s' instead.", tkn, tokenName, tkn);

                context.setNode(new NoodleNodeSwitch(tk.getCodeLocation(), expr, args, options, defaultCase, isSelect));
                break;
            case CUB_OPEN: // { ... statements }
                List<NoodleNode> nodes = new ArrayList<>();
                while (context.hasMoreTokens()) {
                    tkn = context.getCurrentToken();
                    if (tkn.getTokenType() == NoodleTokenType.CUB_CLOSE) {
                        context.incrementToken();
                        closed = true;
                        break;
                    }

                    buildStatement(context);
                    nodes.add(context.getNode());
                }

                if (!closed)
                    throw new NoodleSyntaxException("A block starting with '{' was not closed.", tk);

                context.setNode(new NoodleNodeBlock(tk.getCodeLocation(), nodes));
                break;
            case WHILE: // while <condition-expr> <loop-expr>
                buildExpression(context, 0);
                cond = context.getNode();
                buildLoopBody(context);
                context.setNode(new NoodleNodeWhile(NoodleNodeType.WHILE, tk.getCodeLocation(), cond, context.getNode()));
                break;
            case DO: // do <loop-expr> while <condition-expr>
                buildLoopBody(context);
                NoodleNode loop = context.getNode();

                // expect a `while`:
                tkn = context.getCurrentToken();
                if (tkn.getTokenType() != NoodleTokenType.WHILE)
                    throw new NoodleSyntaxException("Expected a `while` after `do`, but got '%s' instead.", tkn, tkn);
                context.incrementToken();

                // read condition:
                buildExpression(context, 0);
                context.setNode(new NoodleNodeWhile(NoodleNodeType.DO_WHILE, tk.getCodeLocation(), loop, context.getNode()));
                break;
            case FOR: // for (<init>; <cond-expr>; <post>) <loop>
                // See if there's a `(`:
                tkn = context.getCurrentToken();
                boolean firstPass = tkn.getTokenType() == NoodleTokenType.PAR_OPEN;
                if (firstPass)
                    context.incrementToken();

                // Read init:
                buildStatement(context);
                NoodleNode init = context.getNode();

                // Read condition:
                buildExpression(context, 0);
                NoodleNode condition = context.getNode();
                tkn = context.getCurrentToken();
                if (tkn.getTokenType() == NoodleTokenType.SEMICOLON)
                    context.incrementToken();

                // Read post-statement:
                buildStatement(context);
                NoodleNode post = context.getNode();

                // See if there is a matching close parenthesis `)`.
                if (firstPass) {
                    tkn = context.getCurrentToken();
                    if (tkn.getTokenType() != NoodleTokenType.PAR_CLOSE)
                        throw new NoodleSyntaxException("Expected to close the for loop with `)`, but got '%s' instead.", tkn, tkn);
                    context.incrementToken();
                }

                // Read the loop body.
                buildLoopBody(context);
                NoodleNode body = context.getNode();
                context.setNode(new NoodleNodeFor(tk.getCodeLocation(), init, condition, post, body));
                break;
            case BREAK:
                if (context.isBuildCanBreak()) {
                    context.setNode(new NoodleNode(NoodleNodeType.BREAK, tk.getCodeLocation()));
                } else {
                    throw new NoodleSyntaxException("Cannot use `break` here.", tk);
                }
                break;
            case CONTINUE:
                if (context.isBuildCanContinue()) {
                    context.setNode(new NoodleNode(NoodleNodeType.CONTINUE, tk.getCodeLocation()));
                } else {
                    throw new NoodleSyntaxException("Cannot use `continue` here.", tk);
                }
                break;
            case LABEL:
                tkn = context.getCurrentTokenIncrement();
                if (tkn.getTokenType() != NoodleTokenType.IDENTIFIER)
                    throw new NoodleSyntaxException("Expected a label name, but got '%s' instead.", tkn, tkn);

                String name = ((NoodleTokenString) tkn).getStringData();
                tkn = context.getCurrentToken();
                if (tkn.getTokenType() == NoodleTokenType.COLON)
                    context.incrementToken(); // allow `label some:`

                buildStatement(context);
                context.setNode(new NoodleNodeLabel(tk.getCodeLocation(), name, context.getNode()));
                break;
            case JUMP:
                tkn = context.getCurrentTokenIncrement();
                if (tkn.getTokenType() != NoodleTokenType.IDENTIFIER)
                    throw new NoodleSyntaxException("Expected a label name for jump, but got '%s' instead.", tkn, tkn);
                context.setNode(new NoodleNodeString(NoodleNodeType.JUMP, tk.getCodeLocation(), ((NoodleTokenString) tkn).getStringData()));
                break;
            case JUMP_PUSH:
                tkn = context.getCurrentTokenIncrement();
                if (tkn.getTokenType() != NoodleTokenType.IDENTIFIER)
                    throw new NoodleSyntaxException("Expected a label name for jump, but got '%s' instead. ", tkn, tkn);

                context.setNode(new NoodleNodeString(NoodleNodeType.JUMP_PUSH, tk.getCodeLocation(), ((NoodleTokenString) tkn).getStringData()));
                break;
            case JUMP_POP:
                context.setNode(new NoodleNode(NoodleNodeType.JUMP_POP, tk.getCodeLocation()));
                break;
            case FUNCTION:
                // function <function-name> [minimum argument-count] <function-arguments> <function-block-statement>
                buildFunctionDefinition(context);
                break;
            case POUND:
                // Most preprocessor directives will not get it this far, but some exceptions might.
                tkn = context.getCurrentTokenIncrement();
                NoodlePreprocessorDirectiveType directiveType = NoodlePreprocessor.getDirectiveTypeFromToken(tkn);
                NoodlePreprocessorDirective directive = directiveType.makeDirective(tkn.getCodeLocation());
                directive.parseTokens(context);
                context.setNode(new NoodleNodePreprocessorDirective(tk.getCodeLocation(), directive));
                break;
            default:
                context.decrementToken();
                buildExpression(context, FLAG_NO_OPERATORS);
                expr = context.getNode();
                tkn = context.getCurrentToken();

                if (expr.getNodeType() == NoodleNodeType.POSTFIX || expr.getNodeType() == NoodleNodeType.PREFIX) {
                    expr.setNodeType(NoodleNodeType.ADJFIX);
                } else if (expr.getNodeType() == NoodleNodeType.CALL) { // Certain expressions are allowed to be statements, such as function calls.
                    // They need to run the `discard` instruction so that we don't clog the stack.
                    context.setNode(new NoodleNodeSingle(NoodleNodeType.DISCARD, expr.getCodeLocation(), expr));
                } else if (tkn.getTokenType() == NoodleTokenType.SET) { // node = value
                    NoodleTokenOperator tokenOp = (NoodleTokenOperator) tkn;

                    context.incrementToken();
                    buildExpression(context, 0);
                    NoodleNode setValue = context.getNode();
                    context.setNode(new NoodleNodeSet(tkn.getCodeLocation(), tokenOp.getOperator(), expr, setValue));
                } else if (expr.getNodeType() == NoodleNodeType.EVALUATION_CHAIN || expr.getNodeType() == NoodleNodeType.CALL_STATIC) {
                    // They need to run the `discard` instruction so that we don't clog the stack.
                    // As of writing, this is guaranteed to have a return value, since the setter version is handled by the if-statement above this one.
                    context.setNode(new NoodleNodeSingle(NoodleNodeType.DISCARD, expr.getCodeLocation(), expr));
                } else {
                    throw new NoodleSyntaxException("Expected a statement, but got '%s' instead.", tkn, tkn);
                }
        }

        // Allow a semicolon after statements:
        tk = context.getCurrentToken();
        if (tk.getTokenType() == NoodleTokenType.SEMICOLON)
            context.incrementToken();
    }

    /**
     * Builds an expression AST node.
     * @param context The context to build from.
     * @param flags The flags to build with.
     */
    public static void buildExpression(NoodleCompileContext context, int flags) {
        NoodleToken tk = context.getCurrentTokenIncrement();

        switch (tk.getTokenType()) {
            case NULL:
                context.setNode(new NoodleNodeNull(tk.getCodeLocation()));
                break;
            case NUMBER:
                context.setNode(new NoodleNodeNumber(tk.getCodeLocation(), ((NoodleTokenNumber) tk).getNumber()));
                break;
            case STRING:
                context.setNode(new NoodleNodeString(NoodleNodeType.STRING, tk.getCodeLocation(), ((NoodleTokenString) tk).getStringData()));
                break;
            case IDENTIFIER:
                String identValue = ((NoodleTokenString) tk).getStringData();
                NoodleToken tkn = context.getCurrentToken();
                NoodleObjectTemplate<?> staticTemplate;
                if (tkn.getTokenType() == NoodleTokenType.PAR_OPEN) { // `ident(`
                    // Look up the function by name.
                    String functionName = identValue;
                    List<NoodleNode> args = buildFunctionArguments(context);

                    // We check if the function exists as the last compilation step.
                    context.setNode(new NoodleNodeFunctionCall(tk.getCodeLocation(), functionName, args));
                } else if (tkn.getTokenType() == NoodleTokenType.PERIOD && (staticTemplate = context.getEngine().getTemplateByName(identValue)) != null) {
                    context.incrementToken();

                    // Get function name and arguments.
                    tkn = context.getCurrentTokenIncrement();
                    if (!(tkn instanceof NoodleTokenString))
                        throw new NoodleSyntaxException("Expected an IDENTIFIER for the static function name under %s. (Instead got: '%s')", tkn, identValue, tkn);

                    String functionName = ((NoodleTokenString) tkn).getStringData();
                    List<NoodleNode> arguments = buildFunctionArguments(context);

                    // Create static function call node.
                    context.setNode(new NoodleNodeFunctionCallStatic(tk.getCodeLocation(), staticTemplate, functionName, arguments));
                } else { // An actual identifier.
                    context.setNode(new NoodleNodeString(NoodleNodeType.IDENTIFIER, tk.getCodeLocation(), ((NoodleTokenString) tk).getStringData()));
                }

                // If there's a period, the expression is a chain of evaluations. Eg: player.location.block.setType("AIR")
                if (context.getCurrentToken().getTokenType() == NoodleTokenType.PERIOD) {
                    context.incrementToken();

                    NoodleToken nextIdentifier = context.getCurrentToken();
                    if (nextIdentifier.getTokenType() != NoodleTokenType.IDENTIFIER)
                        throw new NoodleSyntaxException("Expected an identifier after period, but got %s.", nextIdentifier, nextIdentifier);

                    NoodleNode lastNode = context.getNode();
                    buildExpression(context, FLAG_NO_OPERATORS);
                    context.setNode(new NoodleNodeEvaluationChain(tk.getCodeLocation(), lastNode, context.getNode()));
                }

                break;
            case PAR_OPEN: // (value)
                buildExpression(context, 0);
                tk = context.getCurrentTokenIncrement();
                if (tk.getTokenType() != NoodleTokenType.PAR_CLOSE)
                    throw new NoodleSyntaxException("Expected a `)`, but got '%s' instead.", tk, tk);
                break;
            case OPERATOR: // -value, +value
                switch (((NoodleTokenOperator) tk).getOperator()) {
                    case SUB: // SUB is both a binary operator and a unary operator, so we need to listen for the '-', and in this situation, it's a unary operator.
                        buildExpression(context, FLAG_NO_OPERATORS);
                        context.setNode(new NoodleNodeUnary(tk.getCodeLocation(), NoodleUnaryOperator.NEGATE, context.getNode()));
                        break;
                    default:
                        throw new NoodleSyntaxException("Unsupported unary operator '%s'.", tk, tk);
                }
                break;
            case NEW: // new
                NoodleToken newTypeTkn = context.getCurrentTokenIncrement();
                if (newTypeTkn.getTokenType() != NoodleTokenType.IDENTIFIER)
                    throw new NoodleSyntaxException("Expected a template following 'new' keyword, but got '%s' instead.", newTypeTkn, newTypeTkn);

                String typeName = ((NoodleTokenString) newTypeTkn).getStringData();
                NoodleObjectTemplate<?> template = context.getEngine().getTemplateByName(typeName);
                if (template == null)
                    throw new NoodleSyntaxException("Cannot use constructor of unknown template '%s'.", newTypeTkn, typeName);

                List<NoodleNode> arguments = buildFunctionArguments(context);

                if (template.getStaticFunction(NoodleObjectTemplate.CONSTRUCTOR_FUNCTION_NAME, arguments.size()) == null)
                    throw new NoodleSyntaxException("The object template %s does not have a constructor which accepts %d arguments.", newTypeTkn, typeName, arguments.size());

                context.setNode(new NoodleNodeFunctionCallStatic(tk.getCodeLocation(), template, NoodleObjectTemplate.CONSTRUCTOR_FUNCTION_NAME, arguments));
                break;
            case UNARY_OPERATOR: // !value
                buildExpression(context, FLAG_NO_OPERATORS);
                context.setNode(new NoodleNodeUnary(tk.getCodeLocation(), ((NoodleTokenUnary) tk).getUnaryOperator(), context.getNode()));
                break;
            case ADJFIX: // ++value
                buildExpression(context, FLAG_NO_OPERATORS);
                context.setNode(new NoodleNodePrePostOperator(NoodleNodeType.PREFIX, tk.getCodeLocation(), context.getNode(), ((NoodleTokenNumber) tk).getNumber()));
                break;
            default:
                throw new NoodleSyntaxException("Expected an expression token, but got '%s' instead.", tk, tk);
        }

        // value++? (Yes, this runs even if FLAG_NO_OPERATORS is set.)
        tk = context.getCurrentToken();
        if (tk.getTokenType() == NoodleTokenType.ADJFIX) {
            context.incrementToken();
            context.setNode(new NoodleNodePrePostOperator(NoodleNodeType.POSTFIX, tk.getCodeLocation(), context.getNode(), ((NoodleTokenNumber) tk).getNumber()));
        }

        // value + ...?
        if ((flags & FLAG_NO_OPERATORS) == 0) {
            tk = context.getCurrentToken();
            if (tk.getTokenType() == NoodleTokenType.OPERATOR) {
                context.incrementToken();
                buildOperators(context, tk);
            }
        }
    }

    private static List<NoodleNode> buildFunctionArguments(NoodleCompileContext context) {
        NoodleToken tk = context.getCurrentTokenIncrement();
        if (tk.getTokenType() != NoodleTokenType.PAR_OPEN)
            throw new NoodleSyntaxException("Expected '(' to open the function argument list, but got '%s'.", tk, tk);

        List<NoodleNode> args = new ArrayList<>();

        // read the arguments and the closing `)`:
        boolean closed = false;
        while (context.hasMoreTokens()) {
            // hit a closing `)` yet?
            NoodleToken tkn = context.getCurrentToken();
            if (tkn.getTokenType() == NoodleTokenType.PAR_CLOSE) {
                context.incrementToken();
                closed = true;
                break;
            }

            // read the argument:
            buildExpression(context, 0);
            args.add(context.getNode());
            // skip a `,`:
            tkn = context.getCurrentToken();
            if (tkn.getTokenType() == NoodleTokenType.COMMA) {
                context.incrementToken();
            } else if (tkn.getTokenType() != NoodleTokenType.PAR_CLOSE) {
                throw new NoodleSyntaxException("Expected a `,` or `)` for function arguments, but got '%s' instead", tkn, tkn);
            }
        }

        if (!closed)
            throw new NoodleSyntaxException("Unclosed function arguments `(`.", tk);

        return args;
    }

    /**
     * Builds a node which holds all operators and operands at the current parenthesis nesting level.
     * @param context The context to build from.
     * @param firstToken The first token to build from.
     */
    public static void buildOperators(NoodleCompileContext context, NoodleToken firstToken) {
        List<NoodleNode> nodes = new ArrayList<>();
        nodes.add(context.getNode());
        List<NoodleToken> ops = new ArrayList<>();
        ops.add(firstToken);

        while (true) {
            // try to read the next expression and add it to the list.
            buildExpression(context, FLAG_NO_OPERATORS);
            nodes.add(context.getNode());

            // if followed by an operator, add that too, stop otherwise.
            NoodleToken tk = context.getCurrentToken();
            if (tk.getTokenType() == NoodleTokenType.OPERATOR) {
                context.incrementToken();
                ops.add(tk);
            } else {
                break;
            }
        }

        // Nest operators from top to bottom priority:
        int n = ops.size();
        int pMax = NoodleOperator.MAXP.getCategory();
        int pri = 0;
        while (pri < pMax) {
            for (int i = 0; i < n; i++) {
                NoodleTokenOperator tk = (NoodleTokenOperator) ops.get(i);
                if (tk.getOperator().getCategory() != pri)
                    continue;
                nodes.set(i, new NoodleNodeBinaryOp(tk.getCodeLocation(), tk.getOperator(), nodes.get(i), nodes.get(i + 1)));
                nodes.remove(i + 1);
                ops.remove(i);
                n--;
                i--;
            }
            pri++;
        }

        // There is one resulting node, and it's in nodes[0].
        context.setNode(nodes.get(0));
    }

    /**
     * Builds the block containing the body of a loop.
     * @param context The context to build from.
     */
    private static void buildLoopBody(NoodleCompileContext context) {
        boolean couldBreak = context.isBuildCanBreak();
        boolean couldContinue = context.isBuildCanContinue();
        context.setBuildCanBreak(true);
        context.setBuildCanContinue(true);
        buildStatement(context);
        context.setBuildCanBreak(couldBreak);
        context.setBuildCanContinue(couldContinue);
    }

    /**
     * Builds a function definition AST node.
     * @param context The context to build from.
     */
    private static void buildFunctionDefinition(NoodleCompileContext context) {
        NoodleToken tk = context.getCurrentTokenIncrement();

        if (tk.getTokenType() != NoodleTokenType.IDENTIFIER)
            throw new NoodleSyntaxException("Expected a function name, but got '%s' instead.", tk, tk);

        String functionName = ((NoodleTokenString) tk).getStringData();

        tk = context.getCurrentTokenIncrement();
        if (tk.getTokenType() != NoodleTokenType.PAR_OPEN) // `ident(`
            throw new NoodleSyntaxException("Expected '(' to open function arguments, but got '%s' instead.", tk, tk);

        NoodleToken functionArgumentsStartToken = tk;
        List<NoodleNodeString> parameterNameNodes = new ArrayList<>();
        List<String> parameterNames = new ArrayList<>();

        // read the arguments and the closing `)`:
        boolean closed = false;
        while (context.hasMoreTokens()) {
            // hit a closing `)` yet?
            tk = context.getCurrentTokenIncrement();
            if (tk.getTokenType() == NoodleTokenType.PAR_CLOSE) {
                closed = true;
                break;
            }

            // read the argument:
            String parameterName = ((NoodleTokenString) tk).getStringData();
            if (parameterNames.contains(parameterName))
                throw new NoodleSyntaxException("Function '%s' had multiple parameters named '%s'.", tk, functionName, parameterName);

            parameterNameNodes.add(new NoodleNodeString(NoodleNodeType.IDENTIFIER, tk.getCodeLocation(), parameterName));
            parameterNames.add(parameterName);

            tk = context.getCurrentToken();
            if (tk.getTokenType() == NoodleTokenType.COMMA) {
                context.incrementToken();
            } else if (tk.getTokenType() != NoodleTokenType.PAR_CLOSE) {
                throw new NoodleSyntaxException("Expected a `,` or `)` in function arguments, but got '%s' instead.", tk, tk);
            }
        }

        if (!closed)
            throw new NoodleSyntaxException("Unclosed function arguments `(`.", functionArgumentsStartToken);

        tk = context.getCurrentToken(); // Don't increment so we can use buildStatement().
        if (tk.getTokenType() != NoodleTokenType.CUB_OPEN)
            throw new NoodleSyntaxException("Expected a '{' after %s's function signature, but got '%s'.", tk, tk, functionName);

        // Reads the function body block.
        buildStatement(context);
        if (!(context.getNode() instanceof NoodleNodeBlock))
            throw new NoodleSyntaxException("Expected a function body for %s, but got '%s' instead.", context.getNode(), functionName, context.getNode().getNodeType());
        NoodleNodeBlock functionBody = (NoodleNodeBlock) context.getNode();

        // Tests if the function is already defined.
        verifyNameIsAvailable(context, tk, "function", functionName, parameterNames.size());

        // Creates and registers the function.
        NoodleScriptFunction newFunction = new NoodleScriptFunction(context.getTargetScript(), functionName, parameterNames);
        NoodleNodeFunctionDefinition functionDefinition = new NoodleNodeFunctionDefinition(tk.getCodeLocation(), functionName, parameterNameNodes, functionBody);
        context.setNode(functionDefinition);
        context.getFunctionsByDefinition().put(functionDefinition, newFunction);
        if (!context.getFunctions().registerCallable(newFunction))
            throw new NoodleSyntaxException("Internal error registering the function definition '%s'.", tk, newFunction.getSignature()); // Shouldn't happen if verifyNameIsAvailable doesn't throw.
    }

    /**
     * Parses the raw text into tokens.
     * The resulting tokens are stored in the context.
     * @param source The source of the code.
     * @param startingLineIndex The index of the line which token parsing has begun on.
     */
    public static void parseIntoTokens(NoodleCodeSource source, String scriptText, List<NoodleToken> out, int startingLineIndex) {
        int len = scriptText.length();

        int pos = 1;
        int lineStart = 1;
        int lineNumber = startingLineIndex;
        while (pos < len) {
            int start = pos;
            NoodleCodeLocation codeLoc = new NoodleCodeLocation(source, lineNumber, pos - lineStart + 1);
            char currentChar = scriptText.charAt(pos - 1);

            pos++;
            switch (currentChar) {
                case ' ':
                case '\t':
                case '\r':
                    break;
                case '\n':
                    lineNumber++;
                    lineStart = pos;
                    break;
                case '\\':
                    out.add(new NoodleToken(NoodleTokenType.BACKSLASH, codeLoc));
                    break;
                case ';':
                    out.add(new NoodleToken(NoodleTokenType.SEMICOLON, codeLoc));
                    break;
                case ':':
                    out.add(new NoodleToken(NoodleTokenType.COLON, codeLoc));
                    break;
                case '(':
                    out.add(new NoodleToken(NoodleTokenType.PAR_OPEN, codeLoc));
                    break;
                case ')':
                    out.add(new NoodleToken(NoodleTokenType.PAR_CLOSE, codeLoc));
                    break;
                case '{':
                    out.add(new NoodleToken(NoodleTokenType.CUB_OPEN, codeLoc));
                    break;
                case '}':
                    out.add(new NoodleToken(NoodleTokenType.CUB_CLOSE, codeLoc));
                    break;
                case ',':
                    out.add(new NoodleToken(NoodleTokenType.COMMA, codeLoc));
                    break;
                case '#':
                    out.add(new NoodleToken(NoodleTokenType.POUND, codeLoc));
                    break;
                case '+':
                    switch (scriptText.charAt(pos - 1)) {
                        case '=': // +=
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.ADD));
                            break;
                        case '+': // ++
                            pos++;
                            out.add(new NoodleTokenNumber(NoodleTokenType.ADJFIX, codeLoc, 1));
                            break;
                        default:
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.ADD));
                    }
                    break;
                case '-':
                    switch (scriptText.charAt(pos - 1)) {
                        case '=': // -=
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.SUB));
                            break;
                        case '-': // --
                            pos++;
                            out.add(new NoodleTokenNumber(NoodleTokenType.ADJFIX, codeLoc, -1));
                            break;
                        default:
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.SUB));
                    }
                    break;
                case '*':
                    if (scriptText.charAt(pos - 1) == '=') { // *=
                        pos++;
                        out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.MUL));
                    } else { // Normal *
                        out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.MUL));
                    }
                    break;
                case '/':
                    switch (scriptText.charAt(pos - 1)) {
                        case '=': // /=
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.DIV));
                            break;
                        case '/': // Line comment.
                            while (pos <= len) {
                                currentChar = scriptText.charAt(pos - 1);
                                if (currentChar == '\r' || currentChar == '\n')
                                    break; // Upon hitting the end of the line, stop. This is no longer a comment.
                                pos++;
                            }
                            break;
                        case '*': // Block comment.
                            pos++;

                            boolean endedComment = false;
                            while (pos <= len) {
                                if (scriptText.charAt(pos - 1) == '\n') {
                                    lineStart = pos;
                                    lineNumber++;
                                } else if (scriptText.charAt(pos - 1) == '*' && pos < len && scriptText.charAt(pos) == '/') {
                                    pos += 2;
                                    endedComment = true;
                                    break;
                                }

                                pos++;
                            }

                            // If we have reached the end, bail.
                            if (!endedComment)
                                throw new NoodleSyntaxException("Unclosed multi-line comment starting at " + NoodleUtils.getErrorPositionText(codeLoc) + ".");

                            break;
                        default:
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.DIV));
                    }
                    break;
                case '%':
                    if (scriptText.charAt(pos - 1) == '=') { // %=
                        pos++;
                        out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.MOD));
                    } else { // Normal.
                        out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.MOD));
                    }
                    break;
                case '!':
                    if (scriptText.charAt(pos - 1) == '=') { // !=
                        pos++;
                        out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.NEQ));
                    } else { // Normal.
                        out.add(new NoodleTokenUnary(NoodleTokenType.UNARY_OPERATOR, codeLoc, NoodleUnaryOperator.INVERT));
                    }
                    break;
                case '=':
                    if (scriptText.charAt(pos - 1) == '=') { // ==
                        pos++;
                        out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.EQ));
                    } else { // Normal.
                        out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.SET));
                    }
                    break;
                case '<':
                    switch (scriptText.charAt(pos - 1)) {
                        case '=': // <=
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.LTE));
                            break;
                        case '<': // <<
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.SHL));
                            break;
                        default:
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.LT));
                    }
                    break;
                case '>':
                    switch (scriptText.charAt(pos - 1)) {
                        case '=': // >=
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.GTE));
                            break;
                        case '>': // >>
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.SHR));
                            break;
                        default:
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.GT));
                    }
                    break;
                case '\"':
                    boolean escaped = false;
                    while (pos <= len) {
                        char tempChar = scriptText.charAt(pos - 1);
                        if (tempChar == '\"' && !escaped)
                            break; // Found the end of the string.

                        if (escaped) {
                            escaped = false;
                        } else if (tempChar == '\\') {
                            escaped = true;
                        }

                        pos++;
                    }

                    if (pos <= len) {
                        pos++;
                        String resultStr = scriptText.substring(start, pos - 2);
                        resultStr = NoodleUtils.codeStringToCompiledString(resultStr);
                        out.add(new NoodleTokenString(NoodleTokenType.STRING, codeLoc, resultStr));
                    } else {
                        throw new NoodleSyntaxException("Unclosed string starting at " + NoodleUtils.getErrorPositionText(codeLoc) + ".");
                    }

                    break;
                case '|':
                    switch (scriptText.charAt(pos - 1)) {
                        case '|': // ||
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.BOR));
                            break;
                        case '=': // |=
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.LOR));
                            break;
                        default:
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.LOR));
                    }
                    break;
                case '&':
                    switch (scriptText.charAt(pos - 1)) {
                        case '&': // &&
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.BAND));
                            break;
                        case '=': // &=
                            pos++;
                            out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.LAND));
                            break;
                        default:
                            out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.LAND));
                    }
                    break;
                case '^':
                    if (scriptText.charAt(pos - 1) == '=') { // ^=
                        pos++;
                        out.add(new NoodleTokenOperator(NoodleTokenType.SET, codeLoc, NoodleOperator.LXOR));
                    } else { // Normal.
                        out.add(new NoodleTokenOperator(NoodleTokenType.OPERATOR, codeLoc, NoodleOperator.LXOR));
                    }
                    break;
                default:
                    if (Character.isDigit(currentChar) || currentChar == '.') {
                        boolean preDot = true;
                        while (pos <= len) {
                            char tempChar = scriptText.charAt(pos - 1);
                            if (tempChar == '.') {
                                if (preDot) { // First dot we've found.
                                    preDot = false;
                                    pos++;
                                } else { // Not the first dot we've found. Exit.
                                    break;
                                }
                            } else if (Character.isDigit(tempChar)) {
                                pos++; // This is part of the number, so continue reading.
                            } else { // Found a character which is not contributing to the number. That means we've reached the end of the number.
                                break;
                            }
                        }

                        String readNumber = scriptText.substring(start - 1, pos - 1);
                        if (readNumber.equals(".")) {
                            // It's just a period.
                            out.add(new NoodleToken(NoodleTokenType.PERIOD, codeLoc));
                        } else {
                            // It's a number.
                            out.add(new NoodleTokenNumber(NoodleTokenType.NUMBER, codeLoc, Double.parseDouble(readNumber)));
                        }
                    } else if (currentChar == '_' || Character.isLetter(currentChar)) {
                        while (pos <= len) {
                            char tempChar = scriptText.charAt(pos - 1);
                            if (tempChar == '_' || Character.isLetterOrDigit(tempChar)) {
                                pos++;
                            } else { // We're done reading this.
                                break;
                            }
                        }

                        String name = scriptText.substring(start - 1, pos - 1);
                        switch (name) {
                            case "true":
                                out.add(new NoodleTokenNumber(NoodleTokenType.NUMBER, codeLoc, 1));
                                break;
                            case "false":
                                out.add(new NoodleTokenNumber(NoodleTokenType.NUMBER, codeLoc, 0));
                                break;
                            case "if":
                                out.add(new NoodleToken(NoodleTokenType.IF, codeLoc));
                                break;
                            case "else":
                                out.add(new NoodleToken(NoodleTokenType.ELSE, codeLoc));
                                break;
                            case "return":
                                out.add(new NoodleToken(NoodleTokenType.RETURN, codeLoc));
                                break;
                            case "while":
                                out.add(new NoodleToken(NoodleTokenType.WHILE, codeLoc));
                                break;
                            case "do":
                                out.add(new NoodleToken(NoodleTokenType.DO, codeLoc));
                                break;
                            case "for":
                                out.add(new NoodleToken(NoodleTokenType.FOR, codeLoc));
                                break;
                            case "break":
                                out.add(new NoodleToken(NoodleTokenType.BREAK, codeLoc));
                                break;
                            case "continue":
                                out.add(new NoodleToken(NoodleTokenType.CONTINUE, codeLoc));
                                break;
                            case "label":
                                out.add(new NoodleToken(NoodleTokenType.LABEL, codeLoc));
                                break;
                            case "jump":
                                out.add(new NoodleToken(NoodleTokenType.JUMP, codeLoc));
                                break;
                            case "call":
                                out.add(new NoodleToken(NoodleTokenType.JUMP_PUSH, codeLoc));
                                break;
                            case "back":
                                out.add(new NoodleToken(NoodleTokenType.JUMP_POP, codeLoc));
                                break;
                            case "select":
                                out.add(new NoodleToken(NoodleTokenType.SELECT, codeLoc));
                                break;
                            case "default":
                                out.add(new NoodleToken(NoodleTokenType.DEFAULT, codeLoc));
                                break;
                            case "switch":
                                out.add(new NoodleToken(NoodleTokenType.SWITCH, codeLoc));
                                break;
                            case "case":
                                out.add(new NoodleToken(NoodleTokenType.CASE, codeLoc));
                                break;
                            case "function":
                                out.add(new NoodleToken(NoodleTokenType.FUNCTION, codeLoc));
                                break;
                            case "null":
                                out.add(new NoodleToken(NoodleTokenType.NULL, codeLoc));
                                break;
                            case "new":
                                out.add(new NoodleToken(NoodleTokenType.NEW, codeLoc));
                                break;
                            case "var": // This is allowed, but does nothing.
                                break;
                            default:
                                out.add(new NoodleTokenString(NoodleTokenType.IDENTIFIER, codeLoc, name));
                                break;
                        }
                    } else {
                        out.clear();
                        throw new NoodleSyntaxException("Unexpected character `" + currentChar + "` at " + NoodleUtils.getErrorPositionText(codeLoc) + ".");
                    }

                    break;
            }
        }

        // Add EOF.
        out.add(new NoodleToken(NoodleTokenType.EOF, new NoodleCodeLocation(source, lineNumber + 1, pos - lineStart + 1)));
    }

    /**
     * Verifies the given callable name is valid.
     * @param context The context to verify under.
     * @param token The token which any syntax errors should point to.
     * @param callableType The type of callable, used in error messages.
     * @param name The name to attempt to use.
     * @param argumentCount The number of arguments to use with the name.
     */
    public static void verifyNameIsAvailable(NoodleCompileContext context, NoodleToken token, String callableType, String name, int argumentCount) {
        // Tests if the function is already defined.
        NoodleScriptFunction foundFunction = context.getFunctions().getByNameAndArgumentCount(name, argumentCount);
        if (foundFunction != null)
            throw new NoodleSyntaxException("This %s definition '%s(%d args) conflicts with the function `%s`.", token, callableType, name, argumentCount, foundFunction.getSignature());

        // Test if there's a macro which already exists with the name.
        NoodleMacro macro = context.getMacros().getByNameAndArgumentCount(name, argumentCount);
        if (macro != null)
            throw new NoodleSyntaxException("This %s definition '%s(%d args)' conflicts with the macro `%s`.", token, callableType, name, argumentCount, macro.getSignature());

        // Test if there's a builtin with the name.
        NoodleBuiltin builtin = context.getEngine().getBuiltinManager().getBuiltIn(name, argumentCount);
        if (builtin != null)
            throw new NoodleSyntaxException("This %s definition '%s(%d args)' conflicts with the builtin `%s`.", token, callableType, name, argumentCount, builtin.getSignature());

        // Search for compiler constants.
        if (argumentCount == 0) {
            NoodlePrimitive constant = context.getEngine().getConstantByName(name);
            if (constant != null)
                throw new NoodleSyntaxException("This %s definition '%s' conflicts with the compiler constant named '%s'.", token, callableType, name, name);
        }

        // Test if there is a Java noodle function with the same name.
        NoodleFunction noodleFunction = context.getEngine().getGlobalFunctionByName(name);
        if (noodleFunction != null)
            throw new NoodleSyntaxException("This %s definition '%s(%d args)' conflicts with the Java NoodleFunction '%s'.", token, callableType, name, argumentCount, noodleFunction.getLabel());
    }
}