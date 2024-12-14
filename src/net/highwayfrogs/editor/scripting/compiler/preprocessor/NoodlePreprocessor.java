package net.highwayfrogs.editor.scripting.compiler.preprocessor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompileContext;
import net.highwayfrogs.editor.scripting.compiler.NoodleOperator;
import net.highwayfrogs.editor.scripting.compiler.NoodleSyntaxException;
import net.highwayfrogs.editor.scripting.compiler.NoodleUnaryOperator;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.builtins.NoodleBuiltin;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.INoodleManualPreprocessor;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.NoodlePreprocessorDirective;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.NoodlePreprocessorDirectiveType;
import net.highwayfrogs.editor.scripting.compiler.tokens.*;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstructionBinaryOperation;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Implements the noodle preprocessor.
 */
@Getter
@RequiredArgsConstructor
public class NoodlePreprocessor {
    @NonNull private final NoodleCompileContext compileContext;
    private final Map<File, NoodleCachedInclude> cachedIncludes = new ConcurrentHashMap<>();
    private final Function<File, NoodleCachedInclude> cachedIncludeMaker = file -> new NoodleCachedInclude(this, file);

    /**
     * Gets the cached include for a given file.
     * @param file The file to get the include for.
     */
    public NoodleCachedInclude getCachedInclude(File file) {
         return this.cachedIncludes.computeIfAbsent(file, this.cachedIncludeMaker);
    }

    /**
     * Clears cached includes.
     */
    public void clearCachedIncludes() {
        this.cachedIncludes.clear();
    }

    /**
     * Runs the preprocessor on the tokenized symbols.
     * @param context The context to build with.
     * @param tokens The tokens to preprocess.
     */
    public void runPreprocessor(NoodleCompileContext context, List<NoodleToken> tokens) {
        NoodlePreprocessorContext newContext = new NoodlePreprocessorContext(context, tokens);
        while (newContext.hasMoreTokens()) {
            NoodleToken tempToken = newContext.getCurrentToken();
            if (tempToken.getTokenType() == NoodleTokenType.POUND) {
                newContext.incrementToken();
                NoodleToken nextToken = newContext.getCurrentToken();

                NoodlePreprocessorDirective newDirective = readPreprocessorDirective(newContext);
                if (!newDirective.getDirectiveType().isAllowTopLevel())
                    throw new NoodleSyntaxException("Cannot use #%s here, it must be inside another directive, such as a condition.", nextToken, newDirective.getDirectiveType().getMnemonic());

                processDirective(newContext, newDirective);
            } else if (tempToken.getTokenType() == NoodleTokenType.IDENTIFIER) { // Attempt to evaluate macros and constants.
                int startToken = newContext.getCurrentTokenIndex();
                List<NoodleToken> evaluatedTokens = evaluateIdentifier(newContext, true);
                int endToken = newContext.getCurrentTokenIndex();

                if (evaluatedTokens != null) {
                    // Remove macro tokens.
                    for (int i = endToken - 1; i >= startToken; i--)
                        newContext.getTokens().remove(i);

                    // Add new tokens.
                    newContext.getTokens().addAll(startToken, evaluatedTokens);
                    newContext.setCurrentTokenIndex(startToken); // Reset to beginning of evaluated macro result.
                } else {
                    newContext.incrementToken();
                    // Couldn't be evaluated so continue.
                }
            } else {
                newContext.incrementToken();
            }
        }
    }

    /**
     * Gets the noodle preprocessor directive type from the supplied token.
     * @param token The token to get the type from.
     * @return directiveType
     */
    public static NoodlePreprocessorDirectiveType getDirectiveTypeFromToken(NoodleToken token) {
        NoodleTokenType tokenType = token.getTokenType();
        if (tokenType == NoodleTokenType.IDENTIFIER) {
            String directiveName = ((NoodleTokenString) token).getStringData();
            NoodlePreprocessorDirectiveType directiveType = NoodlePreprocessorDirectiveType.getByName(directiveName);
            if (directiveType == null)
                throw new NoodleSyntaxException("Invalid preprocessor directive '%s'.", token, directiveName);

            return directiveType;
        } else if (tokenType == NoodleTokenType.IF) {
            return NoodlePreprocessorDirectiveType.IF;
        } else if (tokenType == NoodleTokenType.ELSE) {
            return NoodlePreprocessorDirectiveType.ELSE;
        } else {
            throw new NoodleSyntaxException("Expected a preprocessor directive after '#', but got '%s' instead.", token, token);
        }
    }

    /**
     * Reads a preprocessor directive from tokens.
     * @param context The context to read the preprocessor directive from.
     * @return parsedDirective
     */
    public static NoodlePreprocessorDirective readPreprocessorDirective(NoodlePreprocessorContext context) {
        int currentTokenStartIndex = context.getCurrentTokenIndex() - 1; // Subtract one to include the '#'.
        NoodleToken directiveNameToken = context.getCurrentTokenIncrement();
        NoodlePreprocessorDirectiveType directiveType = getDirectiveTypeFromToken(directiveNameToken);
        NoodlePreprocessorDirective newDirective = directiveType.makeDirective(directiveNameToken.getCodeLocation());

        // Load directive from tokens.
        context.setLastTokenStartIndex(currentTokenStartIndex);
        newDirective.parseTokens(context);
        context.setLastTokenStartIndex(currentTokenStartIndex); // Set again if it's been changed by reading.
        context.setLastTokenEndIndex(context.getCurrentTokenIndex());
        return newDirective;
    }

    /**
     * Processes a newly-read directive.
     * This is private because it relies on the state of lastTokenStart / lastTokenEnd in the NoodlePreprocessorContext.
     * This is updated every single call to readPreprocessorDirective.
     * @param context The context to process the directive under.
     * @param directive The directive to process.
     */
    private static void processDirective(NoodlePreprocessorContext context, NoodlePreprocessorDirective directive) {
        if (directive.shouldStayForAST())
            return;

        // Remove old tokens.
        int startTokenIndex = context.getLastTokenStartIndex();
        int endTokenIndex = context.getLastTokenEndIndex();
        for (int i = endTokenIndex - 1; i >= startTokenIndex; i--)
            context.getTokens().remove(i);

        context.setCurrentTokenIndex(startTokenIndex); // Read again from after the spot we removed stuff from!

        // Write new tokens.
        List<NoodleToken> results = directive.processAndWriteTokens(context);
        if (results != null && results.size() > 0) {
            if (directive instanceof INoodleManualPreprocessor) { // Manual preprocessing is enabled.
                List<NoodleToken> safeResults = new ArrayList<>(results); // Don't modify original results since this might otherwise end up overwriting the original list.
                ((INoodleManualPreprocessor) directive).runPreprocessor(context, safeResults);

                // Add results.
                if (safeResults.size() > 0) {
                    context.getTokens().addAll(startTokenIndex, safeResults);
                    context.setCurrentTokenIndex(context.getCurrentTokenIndex() + safeResults.size());
                }
            } else {
                // Normal preprocessing logic is just to add the resulting tokens, and resume the preprocessor at the spot which the directive was removed from.
                context.getTokens().addAll(startTokenIndex, results);
                context.setCurrentTokenIndex(startTokenIndex); // Read again from after this point!
            }
        }
    }

    /**
     * Reads a list of tokens which make up a parameter passed to a macro.
     * @param context The context to read the parameters from.
     * @return parameters
     */
    private static List<NoodleToken> readParameterExpression(NoodlePreprocessorContext context) {
        NoodleToken startToken = context.getCurrentToken();
        List<NoodleToken> results = new ArrayList<>();

        int parenLevel = 0;
        while (context.hasMoreTokens()) {
            NoodleToken token = context.getCurrentToken();
            NoodleTokenType tokenType = token.getTokenType();

            // hit a closing `)` yet?
            if (parenLevel <= 0 && (tokenType == NoodleTokenType.COMMA || tokenType == NoodleTokenType.PAR_CLOSE)) {
                return results;
            } else if (tokenType == NoodleTokenType.PAR_CLOSE) {
                parenLevel++;
            } else if (tokenType == NoodleTokenType.PAR_OPEN) {
                parenLevel--;
            }

            context.incrementToken();
            results.add(token);
        }

        throw new NoodleSyntaxException("Macro/built-in did not terminate.", startToken);
    }

    /**
     * Evaluate the next IDENTIFIER token as either a macro or a built-in.
     * @param context The context to execute.
     * @return evaluatedTokens
     */
    public List<NoodleToken> evaluateIdentifier(NoodlePreprocessorContext context, boolean allowNull) {
        int identifierTokenIndex = context.getCurrentTokenIndex();
        NoodleToken nameToken = context.getCurrentTokenIncrement();
        if (nameToken.getTokenType() != NoodleTokenType.IDENTIFIER)
            throw new NoodleSyntaxException("Cannot evaluate identifier because the token wasn't an identifier! (Was: '%s')", nameToken, nameToken);

        String functionName = ((NoodleTokenString) nameToken).getStringData();

        boolean hasParameterParenthesis = false;
        NoodleToken possibleParOpenTkn = context.getCurrentToken();
        List<List<NoodleToken>> replacements = Collections.emptyList();
        if (possibleParOpenTkn.getTokenType() == NoodleTokenType.PAR_OPEN) { // `ident(`
            hasParameterParenthesis = true;
            context.incrementToken();
            replacements = new ArrayList<>();

            // read the arguments and the closing `)`:
            boolean closed = false;
            while (context.hasMoreTokens()) {
                replacements.add(readParameterExpression(context));

                // hit a closing `)` yet?
                NoodleToken tempToken = context.getCurrentTokenIncrement();
                if (tempToken.getTokenType() == NoodleTokenType.PAR_CLOSE) {
                    closed = true;
                    break;
                } else if (tempToken.getTokenType() != NoodleTokenType.COMMA) {
                    throw new NoodleSyntaxException("Preprocessor macro evaluation expected a `,` or `)` for function arguments, but got '%s' instead", tempToken, tempToken);
                }
            }

            if (!closed)
                throw new NoodleSyntaxException("Preprocessor macro evaluation expected a `,` or `)` for function arguments, but never finished.", nameToken, nameToken);
        }

        // 1) Find built-in constant.
        if (!hasParameterParenthesis) {
            NoodlePrimitive constant = this.compileContext.getEngine().getConstantByName(functionName);
            if (constant != null) {
                if (constant.isNumber()) {
                    return Collections.singletonList(new NoodleTokenNumber(NoodleTokenType.NUMBER, nameToken.getCodeLocation(), constant.getNumberValue()));
                } else if (constant.isString()) {
                    return Collections.singletonList(new NoodleTokenString(NoodleTokenType.STRING, nameToken.getCodeLocation(), constant.getStringValue()));
                } else {
                    throw new NoodleSyntaxException("Cannot use constant %s/%s, its type is unsupported.", nameToken, functionName, constant);
                }
            }
        }

        // 2) Execute built-in functions.
        NoodleBuiltin builtin = compileContext.getEngine().getBuiltinManager().getBuiltIn(functionName, replacements.size());
        if (builtin != null) {
            try {
                return builtin.execute(context, replacements);
            } catch (NoodleSyntaxException nse) {
                throw nse;
            } catch (Throwable th) {
                throw new NoodleSyntaxException(th, "Preprocessor: '" + builtin.getSignature() + "' encountered an error.", nameToken);
            }
        }

        // 3) Evaluate the expression as a macro.
        NoodleMacro macro = context.getCompileContext().getMacros().getByNameAndArgumentCount(functionName, replacements.size());
        if (macro != null)
            return macro.evaluateMacro(context, nameToken, replacements);

        // 4) Throw an error because the preprocessor couldn't find anything matching this.
        if (!allowNull)
            throw new NoodleSyntaxException("The preprocessor did not find anything it could evaluate named '%s' with %d parameters.", nameToken, functionName, replacements.size());

        // Reading failed, so we reset to the identifier token.
        context.setCurrentTokenIndex(identifierTokenIndex);
        return null;
    }

    /**
     * Evaluate a node to whether or not it is true or false.
     * @param context The context to evaluate under.
     * @param tokens The tokens to evaluate.
     * @return Whether the node is true or false.
     */
    public boolean evaluateExpressionToBoolean(NoodlePreprocessorContext context, List<NoodleToken> tokens) {
        return tokens != null && tokens.size() > 0 && evaluateExpression(context, tokens).getBooleanValue();
    }

    /**
     * Builds an expression AST node.
     * @param context The context to build from.
     */
    public NoodlePrimitive evaluateExpression(NoodlePreprocessorContext context, List<NoodleToken> tokens) {
        NoodlePreprocessorContext pContext = new NoodlePreprocessorContext(context.getCompileContext(), tokens);
        NoodlePreprocessorMathNode node = buildExpression(pContext, true);
        return node != null ? node.evaluate() : null;
    }

    /**
     * Builds an expression AST node.
     * @param context The context to build from.
     */
    public NoodlePrimitive evaluateExpression(NoodleCompileContext context, List<NoodleToken> tokens) {
        NoodlePreprocessorContext pContext = new NoodlePreprocessorContext(context, tokens);
        NoodlePreprocessorMathNode node = buildExpression(pContext, true);
        return node != null ? node.evaluate() : null;
    }

    /**
     * Builds an expression AST node.
     * @param context The context to build from.
     */
    public NoodlePrimitive evaluateExpression(NoodlePreprocessorContext context) {
        NoodlePreprocessorMathNode node = buildExpression(context, true);
        return node != null ? node.evaluate() : null;
    }

    /**
     * Builds an expression AST node.
     * @param context The context to build from.
     * @param allowOperators Whether operators are allowed.
     */
    private NoodlePreprocessorMathNode buildExpression(NoodlePreprocessorContext context, boolean allowOperators) {
        NoodleToken tk = context.getCurrentTokenIncrement();

        NoodlePreprocessorMathNode result;
        switch (tk.getTokenType()) {
            case NUMBER:
                result = new NoodlePreprocessorMathValueNode(new NoodlePrimitive(((NoodleTokenNumber) tk).getNumber()));
                break;
            case STRING:
                result = new NoodlePreprocessorMathValueNode(new NoodlePrimitive(((NoodleTokenString) tk).getStringData()));
                break;
            case IDENTIFIER:
                context.decrementToken(); // Go back to the identifier.
                List<NoodleToken> evaluatedTokens = evaluateIdentifier(context, false);
                result = buildExpression(new NoodlePreprocessorContext(context.getCompileContext(), evaluatedTokens), true);
                break;
            case PAR_OPEN: // (value)
                result = buildExpression(context, true);
                tk = context.getCurrentTokenIncrement();
                if (tk.getTokenType() != NoodleTokenType.PAR_CLOSE)
                    throw new NoodleSyntaxException("Expected a `)`, but got '%s' instead.", tk, tk);

                break;
            case OPERATOR: // -value, +value
                switch (((NoodleTokenOperator) tk).getOperator()) {
                    case SUB:
                        result = buildExpression(context, false);
                        result = new NoodlePreprocessorMathUnaryOpNode(tk.getCodeLocation(), NoodleUnaryOperator.NEGATE, result);
                        break;
                    default:
                        throw new NoodleSyntaxException("Unsupported unary operator '%s'.", tk, tk);
                }
                break;
            case UNARY_OPERATOR: // !value
                NoodleUnaryOperator unaryOp = ((NoodleTokenUnary) tk).getUnaryOperator();
                if (unaryOp != NoodleUnaryOperator.INVERT)
                    throw new NoodleSyntaxException("Preprocessor does not support the unary operator '%s'.", tk, unaryOp);

                result = buildExpression(context, false);
                result = new NoodlePreprocessorMathUnaryOpNode(tk.getCodeLocation(), NoodleUnaryOperator.INVERT, result);
                break;
            default:
                throw new NoodleSyntaxException("Expected an expression token, but got '%s' instead.", tk, tk);
        }

        // value + ...?
        if (allowOperators) {
            tk = context.getCurrentToken();
            if (tk.getTokenType() == NoodleTokenType.OPERATOR) {
                context.incrementToken();
                return buildOperators(context, tk, result);
            }
        }

        return result;
    }

    /**
     * Builds a node which holds all of the operators and operands at the current parenthesis nesting level.
     * @param context The context to build from.
     * @param firstToken The first token to build from.
     */
    private NoodlePreprocessorMathNode buildOperators(NoodlePreprocessorContext context, NoodleToken firstToken, NoodlePreprocessorMathNode firstNode) {
        List<NoodlePreprocessorMathNode> nodes = new ArrayList<>();
        nodes.add(firstNode);
        List<NoodleToken> ops = new ArrayList<>();
        ops.add(firstToken);

        while (true) {
            // try to read the next expression and add it to the list.
            NoodlePreprocessorMathNode node = buildExpression(context, false);
            nodes.add(node);

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
                nodes.set(i, new NoodlePreprocessorMathOpNode(tk.getCodeLocation(), tk.getOperator(), nodes.get(i), nodes.get(i + 1)));
                nodes.remove(i + 1);
                ops.remove(i);
                n--;
                i--;
            }
            pri++;
        }

        return nodes.get(0);
    }

    private static abstract class NoodlePreprocessorMathNode {

        public abstract NoodlePrimitive evaluate();
    }

    @Getter
    @AllArgsConstructor
    private static class NoodlePreprocessorMathValueNode extends NoodlePreprocessorMathNode {
        private final NoodlePrimitive value;

        @Override
        public NoodlePrimitive evaluate() {
            return this.value;
        }
    }

    @Getter
    @AllArgsConstructor
    private static class NoodlePreprocessorMathOpNode extends NoodlePreprocessorMathNode {
        private final NoodleCodeLocation positionCounter;
        private final NoodleOperator operator;
        private final NoodlePreprocessorMathNode firstNode;
        private final NoodlePreprocessorMathNode secondNode;

        @Override
        public NoodlePrimitive evaluate() {
            NoodlePrimitive firstValue = this.firstNode.evaluate();
            NoodlePrimitive secondValue = this.secondNode.evaluate();

            return NoodleInstructionBinaryOperation.executeBinaryOperation(firstValue, secondValue, this.operator);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class NoodlePreprocessorMathUnaryOpNode extends NoodlePreprocessorMathNode {
        private final NoodleCodeLocation positionCounter;
        private final NoodleUnaryOperator operator;
        private final NoodlePreprocessorMathNode node;

        @Override
        public NoodlePrimitive evaluate() {
            NoodlePrimitive value = this.node.evaluate();

            switch (this.operator) {
                case NEGATE:
                    return new NoodlePrimitive(-value.getNumberValue());
                case INVERT:
                    return new NoodlePrimitive(!value.getBooleanValue());
                default:
                    throw new NoodleSyntaxException("The preprocessor does not support the %s unary operator yet!", this.positionCounter, this.operator);
            }
        }
    }
}
