package net.highwayfrogs.editor.gui.extra.hash;

/**
 * Contains utilities for symbol name hashing for Frogger. (PsyQ 4.0's Assembler & Linker)
 * Created by Kneesnap on 2/24/2022.
 */
public class FroggerHashUtil {
    public static final int LINKER_HASH_TABLE_SIZE = 512;
    public static final int ASSEMBLER_HASH_TABLE_SIZE = 256;
    public static final int MAX_SYMBOL_NAME_LENGTH = 255;

    /**
     * Gets the 'full' hash of the supplied string using the linker's algorithm.
     * @param input The string to hash.
     * @return fullLinkerHash
     */
    public static int getFullLinkerHash(String input) {
        int hash = input.length();
        for (int i = 0; i < input.length(); i++)
            hash += input.charAt(i);
        return hash;
    }

    /**
     * Gets the hash of the supplied string using the linker's algorithm.
     * @param input The string to hash.
     * @return linkerHash
     */
    public static int getLinkerHash(String input) {
        return getFullLinkerHash(input) % LINKER_HASH_TABLE_SIZE;
    }

    /**
     * Gets the 'full' hash of the supplied string using the assembler's algorithm.
     * @param input The string to hash.
     * @return fullAssemblerHash
     */
    public static int getFullAssemblerHash(String input) {
        int hash = 0;
        for (int i = 0; i < input.length(); i++)
            hash += input.charAt(i);
        return hash;
    }

    /**
     * Gets the hash of the supplied string using the assembler's algorithm.
     * @param input The string to hash.
     * @return assemblerHash
     */
    public static int getAssemblerHash(String input) {
        return getFullAssemblerHash(input) % ASSEMBLER_HASH_TABLE_SIZE;
    }

    /**
     * Converts a linker hash into an assembler hash.
     * @param linkerHash The linker hash to convert.
     * @param strLength  The length of the string which the linker hash was created from.
     * @return assemblerHash
     */
    public static int convertLinkerHashToAssemblerHash(int linkerHash, int strLength) {
        int assemblerHash = linkerHash - strLength;
        while (assemblerHash < 0)
            assemblerHash += ASSEMBLER_HASH_TABLE_SIZE;
        return assemblerHash % ASSEMBLER_HASH_TABLE_SIZE;
    }

    /**
     * Takes a linker hash, and modifies it to remove the given substring's impact on the hash.
     * @param toRemove   The string to remove.
     * @param linkerHash The linker hash of the full string
     * @return modifiedHash
     */
    public static int getLinkerHashWithoutSubstring(String toRemove, int linkerHash) {
        int result = linkerHash - FroggerHashUtil.getLinkerHash(toRemove);
        while (result < 0)
            result += LINKER_HASH_TABLE_SIZE;
        return result % LINKER_HASH_TABLE_SIZE;
    }
}