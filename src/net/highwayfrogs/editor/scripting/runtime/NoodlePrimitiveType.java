package net.highwayfrogs.editor.scripting.runtime;

/**
 * Contains possible primitive types.
 * This is sorted by type-precedence.
 */
public enum NoodlePrimitiveType {
    CHAR,
    BYTE,
    SHORT,
    INTEGER,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    OBJECT_REFERENCE;

    /**
     * Gets the preferred primitive type between the two options
     * @param a the first type
     * @param b the second type
     * @return preferredType
     */
    public static NoodlePrimitiveType getPreferredType(NoodlePrimitiveType a, NoodlePrimitiveType b) {
        if (a == null)
            throw new NullPointerException("a");
        if (b == null)
            throw new NoodleRuntimeException("b");

        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
