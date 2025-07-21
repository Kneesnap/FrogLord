package net.highwayfrogs.editor.gui.extra.hash;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.system.IntList;

/**
 * Contains utilities for symbol name hashing for Frogger. (PsyQ 4.0's Assembler & Linker)
 * Created by Kneesnap on 2/24/2022.
 */
public class FroggerHashUtil {
    public static final int PSYQ_LINKER_HASH_TABLE_SIZE = 512;
    public static final int PSYQ_ASSEMBLER_HASH_TABLE_SIZE = 256;
    public static final int PSYQ_MAX_SYMBOL_NAME_LENGTH = 255;
    public static final int MSVC_SYMBOL_HASH_TABLE_SIZE = 1024;
    public static final int MSVC_SHIFT_LEFT = 2;
    public static final int MSVC_SHIFT_RIGHT = 4;

    /**
     * Gets the 'full' hash of the supplied string using the linker's algorithm.
     * @param input The string to hash.
     * @return fullLinkerHash
     */
    public static int getPsyQFullLinkerHash(String input) {
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
    public static int getPsyQLinkerHash(String input) {
        return getPsyQFullLinkerHash(input) % PSYQ_LINKER_HASH_TABLE_SIZE;
    }

    /**
     * Gets the 'full' hash of the supplied string using the assembler's algorithm.
     * @param input The string to hash.
     * @return fullAssemblerHash
     */
    public static int getPsyQFullAssemblerHash(String input) {
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
    public static int getPsyQAssemblerHash(String input) {
        return getPsyQFullAssemblerHash(input) % PSYQ_ASSEMBLER_HASH_TABLE_SIZE;
    }

    /**
     * Converts a linker hash into an assembler hash.
     * @param linkerHash The linker hash to convert.
     * @param strLength  The length of the string which the linker hash was created from.
     * @return assemblerHash
     */
    public static int convertPsyQLinkerHashToAssemblerHash(int linkerHash, int strLength) {
        int assemblerHash = linkerHash - strLength;
        while (assemblerHash < 0)
            assemblerHash += PSYQ_ASSEMBLER_HASH_TABLE_SIZE;
        return assemblerHash % PSYQ_ASSEMBLER_HASH_TABLE_SIZE;
    }

    /**
     * Takes a linker hash, and modifies it to remove the given substring's impact on the hash.
     * @param toRemove   The string to remove.
     * @param linkerHash The linker hash of the full string
     * @return modifiedHash
     */
    public static int getPsyQLinkerHashWithoutSubstring(String toRemove, int linkerHash) {
        int result = linkerHash - FroggerHashUtil.getPsyQLinkerHash(toRemove);
        while (result < 0)
            result += PSYQ_LINKER_HASH_TABLE_SIZE;
        return result % PSYQ_LINKER_HASH_TABLE_SIZE;
    }

    /**
     * Gets the linker hash without the prefix/suffixes provided.
     * @param targetLinkerHash the target PsyQ linker hash to modify
     * @param prefix the prefix to remove
     * @param suffix the suffix to remove
     * @return updatedLinkerHash
     */
    public static int getPsyQLinkerHashWithoutPrefixSuffix(int targetLinkerHash, String prefix, String suffix) {
        if (prefix != null && !prefix.isEmpty())
            targetLinkerHash = getPsyQLinkerHashWithoutSubstring(prefix, targetLinkerHash);
        if (suffix != null && !suffix.isEmpty())
            targetLinkerHash = getPsyQLinkerHashWithoutSubstring(suffix, targetLinkerHash);

        return targetLinkerHash;
    }

    /**
     * Gets the full hash of a string in accordance with the hashing algorithm seen in Microsoft Visual Studio '97.
     * The executables reverse-engineered were found in the Frogger 2 development backup, specifically C1.DLL which had a last modified date of 4/24/1997.
     * The MSVC 1997 Compilation Overview is as follows:
     *  - MSDEV.EXE (Development Studio / Visual Studio IDE)
     *   -> CL.EXE (Manages code compilation across several programs.)
     *    -> CL1.DLL (Reads C source code, performs preprocessing, and compiles to an intermediary format, saved as a temporary file, which will be passed along to C2.EXE)
     *    -> C2.EXE (Optimizing compiler, creates the resulting .obj and .asm files)
     *
     * The executable reverse engineered was C1.DLL, which is the main compiler. (CL.EXE is the equivalent of GCC in the sense that it just calls compilation steps in other executables.)
     * C2.EXE fully respects the ordering of symbols as they are provided in the temporary files from C1.DLL.
     * The function starting at 0x10603DEC in C1.DLL (in the previously mentioned version) prepares the symbols to put in the global hash table.
     * As part of that, it calculates a hash of each symbol, based on the symbol name.
     * The following function re-implements the reverse-engineered algorithm.
     * @param input the string to calculate the hash from
     * @return fullHash
     */
    public static int getMsvcC1FullHash(String input) {
        return getMsvcC1FullHash(input, 0);
    }

    /**
     * Gets the full hash of a string in accordance with the hashing algorithm seen in Microsoft Visual Studio '97.
     * The executables reverse-engineered were found in the Frogger 2 development backup, specifically C1.DLL which had a last modified date of 4/24/1997.
     * The MSVC 1997 Compilation Overview is as follows:
     *  - MSDEV.EXE (Development Studio / Visual Studio IDE)
     *   -> CL.EXE (Manages code compilation across several programs.)
     *    -> CL1.DLL (Reads C source code, performs preprocessing, and compiles to an intermediary format, saved as a temporary file, which will be passed along to C2.EXE)
     *    -> C2.EXE (Optimizing compiler, creates the resulting .obj and .asm files)
     *
     * The executable reverse engineered was C1.DLL, which is the main compiler. (CL.EXE is the equivalent of GCC in the sense that it just calls compilation steps in other executables.)
     * C2.EXE fully respects the ordering of symbols as they are provided in the temporary files from C1.DLL.
     * The function starting at 0x10603DEC in C1.DLL (in the previously mentioned version) prepares the symbols to put in the global hash table.
     * As part of that, it calculates a hash of each symbol, based on the symbol name.
     * The following function re-implements the reverse-engineered algorithm.
     * @param input the string to calculate the hash from
     * @return fullHash
     */
    public static int getMsvcC1FullHash(String input, int startHash) {
        if (input == null)
            throw new NullPointerException("input");

        int hash = startHash; // >>> is necessary because the original algorithm uses u32 instead of s32.
        for (int i = 0; i < input.length(); i++)
            hash = input.charAt(i) + (hash >>> MSVC_SHIFT_RIGHT) + (hash << MSVC_SHIFT_LEFT);

        return hash;
    }

    /**
     * Takes a full MSVC hash, and generates an earlier iteration of the hash, with the final characters of the hash having been removed.
     * Example: getFullMsvcHashWithoutSuffix(getMsvcC1FullHash("hello"), "llo") == getMsvcC1FullHash("he").
     * This function may or may not end up being useful, it was added for exploratory purposes.
     * The following function re-implements the reverse-engineered algorithm.
     * @param startHash the hash to remove the suffix from
     * @param suffix the suffix to remove from the hash
     * @return fullMsvcHashes which could have turned into the described hash
     */
    public static int[] getFullMsvcHashesWithoutSuffix(int startHash, String suffix) {
        if (suffix == null)
            throw new NullPointerException("suffix");

        IntList hashes = new IntList();
        IntList nextHashes = new IntList();
        nextHashes.add(startHash);
        for (int i = suffix.length() - 1; i >= 0; i--) {
            char suffixChar = suffix.charAt(i);

            IntList temp = hashes;
            hashes = nextHashes;
            nextHashes = temp;
            nextHashes.clear();
            for (int j = 0; j < hashes.size(); j++) {
                int oldHash = hashes.get(j);

                int[] newHashes = getPreviousFullMsvcHashes(oldHash - suffixChar);
                for (int k = 0; k < newHashes.length; k++) {
                    int nextHash = newHashes[k];
                    if (!nextHashes.contains(nextHash))
                        nextHashes.add(nextHash);
                }
            }
        }

        return nextHashes.getArray();
    }

    /**
     * Takes a full MSVC hash which has had its final character ASCII code subtracted, and generates the previous hash iteration.
     * Example: getPreviousFullMsvcHash(getMsvcC1FullHash("hello") - 'o') == getMsvcC1FullHash("hell").
     * This function may or may not end up being useful, it was added for exploratory purposes.
     * Each 32-bit number has somewhere between zero and five possible hashes which could have preceded it.
     * The probabilities of getting each number of predecessors is mapped by the following table:
     *  0 Predecessors: 3035385540 Occurrences (70.673%)
     *  1 Predecessors: 132152840 Occurrences (3.076%)
     *  2 Predecessors: 132152840 Occurrences (3.076%)
     *  3 Predecessors: 132152840 Occurrences (3.076%)
     *  4 Predecessors: 813565924 Occurrences (18.942%)
     *  5 Predecessors: 49557312 Occurrences (1.153%)
     * @param hash the hash to calculate the previous iteration for
     * @return lastIterationFullMsvcHash
     */
    public static int[] getPreviousFullMsvcHashes(int hash) {
        if (hash == 0) // 0 is what we use in finishPreviousHash() as a placeholder value, so we'll hardcode the correct result to avoid it breaking.
            return new int[] {0};

        int lastHashStart = ((hash & ((1 << MSVC_SHIFT_LEFT) - 1)) << MSVC_SHIFT_RIGHT); // The last two bits are directly from the previous hash.
        int maxAmount = (1 << MSVC_SHIFT_RIGHT);
        IntList results = new IntList();
        for (int i = 0; i < maxAmount; i++) {
            int resultHash = finishPreviousHash(hash, lastHashStart | i);
            if (resultHash != 0 && !results.contains(resultHash))
                results.add(resultHash);
        }

        return results.getArray();
    }

    private static int finishPreviousHash(int hash, int previousHash) {
        // Bits 0-5 have been set in previousHash, so it's time to solve for bits 4-31

        for (int i = MSVC_SHIFT_LEFT; i < Constants.BITS_PER_INTEGER - MSVC_SHIFT_RIGHT; i++) {
            int testHash = (previousHash >>> MSVC_SHIFT_RIGHT) + (previousHash << MSVC_SHIFT_LEFT);
            if (((testHash >>> i) & 1) != ((hash >>> i) & 1))
                previousHash |= 1 << (i + MSVC_SHIFT_RIGHT);
        }

        return getMsvcC1FullHash("\0", previousHash) == hash ? previousHash : 0;
    }

    /**
     * After the full 32-bit hash is calculated in MSVC as described above, all symbols have their hashes reduced down to a 16-bit value.
     * The following function re-implements the reverse-engineered algorithm for reducing the hash.
     * @param fullHash the full 32-bit hash to reduce
     * @return 16BitHash
     */
    public static short getMsvcC116BitHash(int fullHash) {
        return (short) ((fullHash & 0xFFFF) ^ (fullHash >>> 16));
    }

    /**
     * After the full 32-bit hash is calculated in MSVC as described above, all symbols have their hashes reduced down to a 16-bit value.
     * The following function calculates the 16-bit hash for the provided string.
     * @param input the string to calculate the hash from
     * @return 16BitHash
     */
    public static short getMsvcC116BitHash(String input) {
        return getMsvcC116BitHash(getMsvcC1FullHash(input));
    }

    /**
     * After the 16-bit hash is calculated in MSVC as described above, the global symbol table only has 1024 symbol slots.
     * So, the number must be reduced further to become a valid hash table key code.
     * The following function re-implements the reverse-engineered algorithm for reducing the 16-bit hash to a hash table key code.
     * @param hash16Bit the 16-bit hash to reduce further
     * @return hashTableKeyCode
     */
    public static int getMsvcC1HashTableKey(short hash16Bit) {
        return (hash16Bit & (MSVC_SYMBOL_HASH_TABLE_SIZE - 1));
    }

    /**
     * After the 16-bit hash is calculated in MSVC as described above, the global symbol table only has 1024 symbol slots.
     * So, the number must be reduced further to become a valid hash table key code.
     * The following function calculates the hash table key code for the provided string.
     * @param input the string to calculate the hash from
     * @return 16BitHash
     */
    public static int getMsvcC1HashTableKey(String input) {
        return getMsvcC1HashTableKey(getMsvcC116BitHash(input));
    }

    private static void validateMsvcC2Hash(String input, int fullHash, int hash16Bit, int hashTableKeyCode) {
        int calculatedFullHash = getMsvcC1FullHash(input);
        short calculated16BitHash = getMsvcC116BitHash(calculatedFullHash);
        int calculatedHashTableKey = getMsvcC1HashTableKey(calculated16BitHash);
        if (calculatedFullHash != fullHash || calculated16BitHash != (short) hash16Bit || hashTableKeyCode != calculatedHashTableKey)
            System.err.printf("MSVC Hash Failure '%s':%n Expected: %08X -> %04X -> %04d%n   Actual: %08X -> %04X -> %04d%n", input, fullHash, hash16Bit, hashTableKeyCode, calculated16BitHash, calculated16BitHash & 0xFFFF, calculatedHashTableKey);
    }

    private static void validateMsvcC2Hashes() {
        // actor2.c from frogger 2 was used for testing the compiler. Such symbols were added there.
        // The values tested here were pulled directly from the memory of the compiler while it was running to ensure perfect algorithm matching.
        validateMsvcC2Hash("globalFadeVal", 0xa6500e01, 0xa851, 81);
        validateMsvcC2Hash("A00", 0x523, 0x523, 291);
        validateMsvcC2Hash("A01", 0x524, 0x524, 292);
        validateMsvcC2Hash("A02", 0x525, 0x525, 293);
        validateMsvcC2Hash("A10", 0x527, 0x527,295);
        validateMsvcC2Hash("A20", 0x52b, 0x52b,299);
        validateMsvcC2Hash("im_img0", 0x99139, 0x9130, 304);
        validateMsvcC2Hash("im_img2", 0x9913b, 0x9132, 306);
        validateMsvcC2Hash("im_img1", 0x9913a, 0x9133, 307);
        validateMsvcC2Hash("B00", 0x533, 0x533, 307);
        validateMsvcC2Hash("C00", 0x544, 0x544, 324);
        validateMsvcC2Hash("A_", 0x167, 0x167, 359);
        validateMsvcC2Hash("B_", 0x16b, 0x16b, 363);
        validateMsvcC2Hash("C_", 0x16f, 0x16f, 367);

        // Here are some more symbols with their real stuff copied from the exe.
        validateMsvcC2Hash("im_opti_ctrl_config", 0x3f758775, 0xb800, 0);
        validateMsvcC2Hash("im_choose_course_s", 0xb70f6f0a, 0xd805, 5);
        validateMsvcC2Hash("im_opts_exit", 0x2964ad61, 0x8405, 5);
        validateMsvcC2Hash("im_total_score_i", 0xee4a0e4f, 0xe005, 5);
        validateMsvcC2Hash("im_go_get_em_g", 0xaa742a71, 0x8005, 5);
        validateMsvcC2Hash("im_fly_25", 0x9de09b, 0xe006, 6);

        // And even more!
        validateMsvcC2Hash("ifndef", 0x256f7, 0x56f5, 757);
        validateMsvcC2Hash("include", 0x99b51, 0x9b58, 856);
        validateMsvcC2Hash("endif", 0x90AA, 0x90AA, 170);
        validateMsvcC2Hash("ifdef", 0x92ba, 0x92ba, 698);
        validateMsvcC2Hash("define", 0x23edf, 0x3edd, 733);
    }

    static {
        validateMsvcC2Hashes();
    }
}