package net.highwayfrogs.editor.scripting.compiler.nodes;

/**
 * A registry of all Noodle AST node types.
 */
public enum NoodleNodeType {
    NULL,
    NUMBER, // (val:number)
    IDENTIFIER, // (name:string)
    PREPROCESSOR_DIRECTIVE,
    UNARY_OPERATOR, // (unop, node)
    BINARY_OPERATOR, // (binop, a, b)
    CALL, // (script, args_array)
    CALL_STATIC, // (script, args_array)
    DEFINE_FUNCTION,
    BLOCK, // (nodes_array) { ...nodes }
    RETURN, // (node) return [node]
    DISCARD, // (node) - when we don't care
    IF_THEN, // (cond_node, then_node)
    IF_THEN_ELSE, // (cond_node, then_node, else_node)
    STRING, // (val:string)
    SET, // (binop, node, value:node)
    WHILE,
    DO_WHILE,
    FOR,
    BREAK,
    CONTINUE,
    LABEL, // (name:string)
    JUMP, // (name:string)
    JUMP_PUSH, // (name:string)
    JUMP_POP, // ()
    SELECT, // (call_node, nodes, ?default_node)
    PREFIX, // (node, delta) ++x/--x
    POSTFIX, // (node, delta) x++/x--
    ADJFIX, // (node, delta) x+=1/x-=1 (statement)
    SWITCH, // (expr, case_values, case_exprs, ?default_node)
    EVALUATION_CHAIN,
    ARRAY_DEFINITION,
}
