package net.highwayfrogs.editor.scripting.compiler.tokens;

/**
 * Represents all the possible tokens.
 */
public enum NoodleTokenType {
    EOF, // <end of file>
    OPERATOR, // + - * / %
    PAR_OPEN, // (
    PAR_CLOSE, // )
    NUMBER,  // 37
    IDENTIFIER, // some
    COMMA, // ,
    RETURN, // return
    IF,
    ELSE,
    STRING, // "hi!"
    CUB_OPEN, // {
    CUB_CLOSE, // }
    POUND, // #
    BACKSLASH, // \
    SET, // = += -= etc
    UNARY_OPERATOR, // ! -
    WHILE,
    DO,
    FOR,
    SEMICOLON, // ;
    BREAK,
    CONTINUE,
    LABEL,
    JUMP,
    JUMP_PUSH,
    JUMP_POP,
    COLON, // :
    SELECT,
    DEFAULT,
    ADJFIX, // (delta) ++/--
    SWITCH,
    CASE,
    FUNCTION,
    NULL,
    PERIOD,
    NEW
}
