package net.highwayfrogs.editor.scripting.compiler;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registry of all operators.
 */
@Getter
@AllArgsConstructor
public enum NoodleOperator {
    SET(-1, "="),
    MUL(0x01, "*"),
    DIV(0x02, "/"),
    MOD(0x03, "%"),
    ADD(0x10, "+"),
    SUB(0x11, "-"),
    SHL(0x20, "<<"),
    SHR(0x21, ">>"),
    LAND(0x30, "&"),
    LOR(0x31, "|"),
    LXOR(0x32, "^"),
    EQ(0x40, "=="),
    NEQ(0x41, "!="),
    LT(0x42, "<"),
    LTE(0x43, "<="),
    GT(0x44, ">"),
    GTE(0x45, ">="),
    BAND(0x50, "&&"),
    BOR(0x60, "||"),
    MAXP(0x70, null); // Maximum priority.

    private final int hexCode;
    private final String symbol;

    /**
     * Shift the hex-code right four, getting a category of such.
     * @return category
     */
    public int getCategory() {
        return this.hexCode >> 4;
    }
}
