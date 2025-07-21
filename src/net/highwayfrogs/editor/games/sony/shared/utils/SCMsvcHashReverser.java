package net.highwayfrogs.editor.games.sony.shared.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHashReverser;
import net.highwayfrogs.editor.gui.extra.hash.DictionaryStringGenerator;
import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;
import net.highwayfrogs.editor.gui.extra.hash.HashRange;
import net.highwayfrogs.editor.gui.extra.hash.HashRange.HashRangeType;
import net.highwayfrogs.editor.gui.extra.hash.PermutationStringGenerator;
import net.highwayfrogs.editor.system.IntList;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.io.File;
import java.util.*;

/**
 * Allows solving/reversing MSVC hashes (combined with PsyQ hashes) back to a string form.
 * Further documentation explaining how this works will come separately as part of markdown at a later date, due to the complexity of this process.
 * TODO:
 *  - Implement the ability to remove a suffix from MSVC hash, and toy around with how useful that is.
 *  - Run in parallel for each 32-bit hash.
 *  - Rules for invalid character sequences like:
 *   - two underscores next to each other.
 *   - q, v, w, x, y, z should have characters which cannot follow it.
 *   - numbers should not occur if there are >= 3 characters left, unless one is an underscore.
 *   - Allow running the program without a restart for lookup table reuse.
 *   - There should be a function to handle these invalid characters both for suffix lookup table creation and hash brute forcing.
 * Created by Kneesnap on 7/17/2025.
 */
public class SCMsvcHashReverser {
    private static final int MAXIMUM_LOOKUP_TABLE_SUFFIX_LENGTH = 6; // 6 takes up 30-40GB, use 4-5 if this is too much. Make sure to add JVM argument -Xmx50G when running it with 6.
    private static final char[] DEFAULT_ALLOWED_CHARACTERS = PermutationStringGenerator.ALLOWED_CHARACTERS_ALPHABET; // This can be upgraded to something with numbers if at 4-5 characters (and on a machine with that much memory available)

    @Getter
    @RequiredArgsConstructor
    public static class MsvcHashTarget {
        @NonNull private final String suffix;
        @NonNull private final HashRange psyqRange;
        @NonNull private final HashRange msvcRange;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MsvcSuffixLookupTable {
        @NonNull private final char[] availableCharacters;
        @NonNull private final MsvcSuffixLookupTableEntry[] entries;
        private final int suffixLength;

        private static final int LOOKUP_TABLE_SIZE = FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE * FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE;
    }

    @Getter
    @RequiredArgsConstructor
    public static class MsvcSuffixLookupTableEntry {
        @NonNull private final String[] suffixes;
        @NonNull private final int[] suffixGroupInfo; // upper 16 bits = index of first suffix in group, lower 16 bits = number of suffixes in group.
        private final int startingHashGroupId; // the ID of the first group in the array.

        /**
         * Gets the number of hash groups found within this lookup table entry.
         */
        public int getHashGroupCount() {
            return this.suffixGroupInfo.length;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Please enter the prefix. Example 'im_sub_': ");
        String prefix = scanner.nextLine();
        System.out.print("Please enter the length of the substrings to generate. ");
        String substringLengthRangeStr = scanner.nextLine();
        System.out.println("Please enter a comma-separated list of entries of the form 'suffix:psyqHashRange:msvcHashRange': ");
        String hashTargetText = scanner.nextLine();

        HashRange substringLengthRange = HashRange.parseRange(substringLengthRangeStr, HashRangeType.PSYQ);
        MsvcHashTarget[] hashTargets = parseHashTargets(hashTargetText);
        Set<String> dictionarySet = new HashSet<>(getDefaultDictionaryStringGenerator().getAllLoadedWords());

        int tempLength = -1;
        while ((tempLength = substringLengthRange.getNextValue(tempLength)) > 0) {
            System.out.println();
            System.out.println("Generating for length=" + tempLength + "...");

            int suffixTableLength = tempLength >= 3 ? Math.min(MAXIMUM_LOOKUP_TABLE_SUFFIX_LENGTH, tempLength) : -1;

            long generationStart = System.currentTimeMillis();
            MsvcSuffixLookupTable suffixLookupTable = generateMsvcSuffixTable(suffixTableLength, DEFAULT_ALLOWED_CHARACTERS);
            System.out.println("Suffix table generated in " + (System.currentTimeMillis() - generationStart) + " ms.");
            System.out.println();

            IntList fullHashes = calculateFullMsvcHashes(prefix, tempLength, hashTargets);
            if (fullHashes != null) {
                System.out.println();
                System.out.println("Found " + fullHashes.size() + " MSVC 32-bit hash" + (fullHashes.size() == 1 ? "" : "es") + ".");
                long fullGenerationTimeStart = System.currentTimeMillis();
                long resultCount = 0;
                for (int i = 0; i < fullHashes.size(); i++) {
                    long generationTimeStart = System.currentTimeMillis();
                    List<String> substrings = generateMiddleSubstrings(prefix, tempLength, hashTargets, fullHashes.get(i), suffixLookupTable);
                    long generationTimeEnd = System.currentTimeMillis();
                    if (substrings.size() > 0) {
                        substrings = sortResultsForDisplay(prefix, substrings, dictionarySet);
                        System.out.println();
                        System.out.println("- Hash: " + fullHashes.get(i) + "/" + NumberUtils.to0PrefixedHexString(fullHashes.get(i)) + " (" + substrings.size() + " results in " + (generationTimeEnd - generationTimeStart) + " ms) [" + (i + 1) + "/" + fullHashes.size() + "]:");
                        for (int j = 0; j < substrings.size(); j++)
                            System.out.println("  - " + prefix + substrings.get(j) + " (" + substrings.get(j) + ")");
                        resultCount += substrings.size();
                    } else {
                        if (generationTimeEnd - generationTimeStart >= 15)
                            System.out.println("- Finished Hash " + fullHashes.get(i) + "/" + NumberUtils.to0PrefixedHexString(fullHashes.get(i)) + " in " + (generationTimeEnd - generationTimeStart) + " ms [" + (i + 1) + "/" + fullHashes.size() + "].");
                    }
                }
                long fullGenerationTimeEnd = System.currentTimeMillis();
                System.out.println();
                System.out.println("Generation of " + resultCount + " hashes complete in " + (fullGenerationTimeEnd - fullGenerationTimeStart) + " ms.");
            }
        }
    }

    /**
     * Calculates a list of potential 32-bit full MSVC hashes which could have resulted in the provided targets.
     * The purpose of this is to get a more detailed version of the MSVC hash, since 32-bit numbers contain significantly more information for reversing a hash than 10-bit numbers. Nearly 420,000x more detail.
     * This method is able to take a known prefix for the full string, and calculate some of the highest bits of the 32-bit MSVC hash which meet the criteria of the provided prefixes and hash suffixes.
     * Then, it will take the highest bits (which are confirmed to be part of all the possible 32-bit hashes), and iterate through all the lower (unknown) bits to find all possible 32-bit MSVC hashes.
     * IMPORTANT: Not all the resulting hashes will be potentially valid. To perform a more expensive check to clear out many of the unusable full hashes, call {@code cleanFullMsvcHashes}.
     * @param prefix a prefix representing the beginning of the symbol to reverse. Usually "im_cav_" or something to that effect.
     * @param middleSubstringLength The length of the substring of unknown contents sitting between the prefix and any suffixes provided as part of the targets.
     * @param targets the hash targets which are to be reversed back into strings.
     * @return dirtyFullMsvcHashes (Note: Many of the returned hashes will not correspond to any valid substrings)
     */
    public static IntList calculateFullMsvcHashes(String prefix, int middleSubstringLength, MsvcHashTarget[] targets) {
        if (prefix == null || prefix.isEmpty() || targets == null || targets.length == 0)
            return null; // There's nothing to test the hashes against.
        if (middleSubstringLength < 0)
            throw new IllegalArgumentException("middleSubstringLength must be greater than or equal to zero! (Was: " + middleSubstringLength + ")");

        // TODO: Can we calculate a proper bit amount, and account for the offset?
        int shiftBits = getPrefixShiftAmount(middleSubstringLength);
        if (shiftBits <= 0 || shiftBits > 30)
            return null; // There are too many options, probably around 524288 (Depending on how many targets are used to narrow it down). It will be very slow to calculate this, and it will create unusable results.

        int shiftedHash = getFixedSizeHash(prefix, middleSubstringLength) >>> shiftBits;

        // Now that we've narrowed down some amount of the highest bits in the 32-bit hash, we can test every single possible 32-bit hash which had those higher bits.
        // If the hash appears to be valid at a glance, it is added to the list.
        IntList hashes = new IntList(16); // 16 is arbitrary.
        for (int i = shiftedHash << shiftBits; i < (shiftedHash + 1) << shiftBits; i++)
            if (doesMsvcHashMatchTargets(i, targets))
                hashes.add(i);

        return hashes;
    }

    // The exact number of bits we check vary.
    // This is because there is a complex overflow from the lower bits in the hash.
    // This value here is a rough pick that works for most hashes.
    // 8 and 10 seemed fine until I ran the 'im_swp_ripple' test, which required bit 11.
    // Then, im_cav_sekpool showed it needs to be at least 12.
    // We want to keep this value as low as possible, because every increment roughly doubles the number of results.
    private static final int LEEWAY_BITS = 12;

    /**
     * Calculates and returns the number of bits to shift in a full 32-bit MSVC hash to
     * @param substringLength the length of the substring to generate
     * @return prefixShiftAmount
     */
    private static int getPrefixShiftAmount(int substringLength) {
        if (substringLength == 0)
            return 0;

        return LEEWAY_BITS + (substringLength * 2);
    }

    /**
     * In order to generate the correct upper-bits of a hash, a known prefix must be padded with null characters to reach the substring length
     * @param input the string (including both the user-specified prefix and partial substring) to calculate the hash upper bits for
     * @param missingCharCount the number of characters to pad to make the input reach the substring length
     * @return paddedHash
     */
    private static int getFixedSizeHash(String input, int missingCharCount) {
        // Calculate the bits of the hash we'd like to use, based on the given number of unknown characters (middle length).
        int msvcPrefixHash = FroggerHashUtil.getMsvcC1FullHash(input);
        for (int i = 0; i < missingCharCount; i++)
            msvcPrefixHash = FroggerHashUtil.getMsvcC1FullHash("\0", msvcPrefixHash);

        return msvcPrefixHash;
    }

    /**
     * Test if the given 32-bit MSVC partial hash produces the correct final MSVC hash table key when combined with the given hash target suffixes.
     * If the answer is no/false, then the 32-bit partial MSVC hash isn't valid and can be skipped.
     * @param hash the 32-bit MSVC partial hash to test
     * @param targets the hash targets which the 32-bit hash must be capable of becoming via suffixes.
     * @return isMsvcHashValid
     */
    private static boolean doesMsvcHashMatchTargets(int hash, MsvcHashTarget[] targets) {
        for (int i = 0; i < targets.length; i++) {
            MsvcHashTarget target = targets[i];
            int fullMsvcHash = FroggerHashUtil.getMsvcC1FullHash(target.suffix, hash);
            int msvcHash = FroggerHashUtil.getMsvcC1HashTableKey(FroggerHashUtil.getMsvcC116BitHash(fullMsvcHash));
            if (!target.msvcRange.isInRange(msvcHash))
                return false;
        }

        return true;
    }

    /**
     * Test if the given 32-bit MSVC partial hash produces both the correct final MSVC hash table key and PsyQ hashes when combined with the given hash target suffixes.
     * If the answer is no/false, then the provided string isn't a valid substring and can be skipped.
     * @param input the string to test
     * @param targets the hash targets which the string's hashes must match to be valid.
     * @return areHashesMatchedByString
     */
    private static boolean doesStringMatchBothMsvcAndPsyqTargets(String input, MsvcHashTarget[] targets) {
        int fullMsvcHash = FroggerHashUtil.getMsvcC1FullHash(input);
        if (!doesMsvcHashMatchTargets(fullMsvcHash, targets))
            return false;

        for (int i = 0; i < targets.length; i++) {
            MsvcHashTarget target = targets[i];
            int psyqHash = FroggerHashUtil.getPsyQLinkerHash(input + target.suffix);
            if (!target.psyqRange.isInRange(psyqHash))
                return false;
        }

        return true;
    }

    // NOTE: I'm thinking that as long as there are less than 512 hashes returned, this may allow for some serious brute-forcing.
    // We could hand a program like hashcat a 41 bit hash (9-bit psyq hash combined with the 32-bit msvc hash), and take advantage of gpu acceleration.
    // This would make it reasonable to calculate strings of up to 9 characters long (8 if including 0-9 and _) without a dictionary.
    // TODO:
    //  - At 17GH/s (a conservative estimate, I think we could go as high as 200 GH/s judging by other benchmarks), it seems like it would take 30 seconds to bruteforce all dictionary words against the four patterns:
    //   - [word][word]
    //   - [word]_[word]
    //   - [word][word]_
    //   - [word]_[word]_
    //  - At 17GH/s, it seems like it would take 7.5 minutes to exhaust 9 characters from [a-z_]
    //  - At 17GH/s, it seems like it would take 2 hours to exhaust 9 characters from [a-z0-9_]

    /**
     * Accepts a list of full 32-bit MSVC hashes, and removes any which do not appear to be valid after further calculations.
     * The purpose of this function is to prepare the smallest viable list of hashes for the given string situation.
     * This would be used if we wanted to pass those large hashes to hashcat, and use GPU accelerated brute-forcing to find the missing substrings.
     * Having as few hashes as possible to bruteforce will make a significant difference for how fast hashcat can churn through them all.
     * Note that hashcat support is not currently supported technically, if it gets to a point where I'd like to use hashcat, FrogLord will be responsible for creating a template to instantiate hashcat (proper command-line arguments, generate a text file with the hashes, etc.)
     * @param msvcHashes the list of full MSVC hashes to reduce
     * @param prefix the prefix used to generate the MSVC hashes list
     * @param middleSubstringLength the substring length used to generate the MSVC hashes list
     * @param targets the hash targets used to generate the MSVC hashes list
     * @param suffixLookupTable the suffix lookup table used to generate the MSVC hashes list
     * @param timeoutMs the amount of time to wait before considering a hash to be valid
     * @return updatedMsvcHashes
     */
    @SuppressWarnings("unused")
    public static IntList cleanFullMsvcHashes(IntList msvcHashes, String prefix, int middleSubstringLength, MsvcHashTarget[] targets, MsvcSuffixLookupTable suffixLookupTable, long timeoutMs) {
        if (msvcHashes == null)
            throw new NullPointerException("msvcHashes");
        if (msvcHashes.isEmpty())
            return msvcHashes;
        if (prefix == null || prefix.isEmpty() || targets == null || targets.length == 0)
            return null; // There's nothing to test the hashes against.
        if (middleSubstringLength < 0)
            throw new IllegalArgumentException("middleSubstringLength must be greater than or equal to zero! (Was: " + middleSubstringLength + ")");
        if (suffixLookupTable == null)
            throw new NullPointerException("suffixLookupTable");
        if (timeoutMs <= 0)
            throw new IllegalArgumentException("Cannot check for removal without a positive non-zero timeout interval. (Provided: " + timeoutMs + ")");

        List<String> tempList = new ArrayList<>();
        IndexBitArray indicesToRemove = new IndexBitArray();
        for (int i = 0; i < msvcHashes.size(); i++) {
            int targetFullMsvcHash = msvcHashes.get(i);
            tempList.clear();
            // There are a lot of full MSVC hashes which die/get removed very quickly during the generation process, so just re-use the generation process.
            if (!generateMiddleSubstrings(tempList, prefix, middleSubstringLength, targets, targetFullMsvcHash, suffixLookupTable, timeoutMs))
                indicesToRemove.setBit(i, true);
        }

        msvcHashes.removeIndices(indicesToRemove);
        return msvcHashes;
    }

    /**
     * Generates all middle substrings between the prefix and target suffixes.
     * This takes around a fraction of the number of permutations that a bruteforce approach would take.
     * However, it still will struggle to generate strings depending on how small the suffix lookup table is.
     * @param prefix the prefix to generate the substrings for
     * @param targetLength the size (in characters) of the middle substrings to generate
     * @param targets the targets containing the hash information which must be matched
     * @param targetFullMsvcHash the full 32-bit msvc hash to generate the substrings for
     * @param suffixLookupTable the lookup table to perform the suffix lookup speed increase for
     * @return middleSubstrings
     */
    public static List<String> generateMiddleSubstrings(String prefix, int targetLength, MsvcHashTarget[] targets, int targetFullMsvcHash, MsvcSuffixLookupTable suffixLookupTable) {
        if (prefix == null || prefix.isEmpty() || targets == null || targets.length == 0)
            return Collections.emptyList(); // There's nothing to test the hashes against.
        if (targetLength < 0)
            throw new IllegalArgumentException("targetLength must be greater than or equal to zero! (Was: " + targetLength + ")");
        if (suffixLookupTable == null)
            throw new NullPointerException("suffixLookupTable");

        List<String> results = new ArrayList<>();
        generateMiddleSubstrings(results, prefix, targetLength, targets, targetFullMsvcHash, suffixLookupTable, -1);
        return results;
    }

    /**
     * Generates all middle substrings between the prefix and target suffixes.
     * @param results the list to store any generated substrings within
     * @param prefix the prefix to generate the substrings for
     * @param targetLength the size (in characters) of the middle substrings to generate
     * @param hashTargets the targets containing the hash information which must be matched
     * @param targetFullMsvcHash the full 32-bit msvc hash to generate the substrings for
     * @param suffixLookupTable the lookup table to perform the suffix lookup speed increase for
     * @param timeoutMs if this is greater than zero, this function will exit after the given number of milliseconds. This is primarily used for testing if an MSVC 32-bit hash can easily be eliminated or not
     * @return true if the msvc hash seems valid (see {@code timeoutMs} for more info)
     */
    private static boolean generateMiddleSubstrings(List<String> results, String prefix, int targetLength, MsvcHashTarget[] hashTargets, int targetFullMsvcHash, MsvcSuffixLookupTable suffixLookupTable, long timeoutMs) {
        if (results == null)
            throw new NullPointerException("results");
        if (prefix == null || prefix.isEmpty() || hashTargets == null || hashTargets.length == 0)
            return false; // There's nothing to test the hashes against.
        if (targetLength < 0)
            throw new IllegalArgumentException("targetLength must be greater than or equal to zero! (Was: " + targetLength + ")");
        if (suffixLookupTable == null)
            throw new NullPointerException("suffixLookupTable");

        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add("");
        long startTime = System.currentTimeMillis();
        int startSize = results.size();
        boolean testingIfFullHashAppearsValid = timeoutMs > 0;
        while (queue.size() > 0 && (!testingIfFullHashAppearsValid || (results.size() == startSize && System.currentTimeMillis() > startTime + timeoutMs))) {
            String lastSubstring = testingIfFullHashAppearsValid ? queue.removeLast() : queue.removeFirst(); // If testing the msvc hash validity, we want to use an order which will get us as close to the end as possible.
            int lastFullHash = FroggerHashUtil.getMsvcC1FullHash(prefix + lastSubstring);
            int lastPaddedHash = getFixedSizeHash(prefix + lastSubstring, targetLength - lastSubstring.length());
            int lastMatchingBitCount = getMatchingBitLength(targetFullMsvcHash, lastPaddedHash);

            // Try to use the suffix table to quickly solve the ways to end the string.
            int suffixTableLength = suffixLookupTable.getSuffixLength();
            if (suffixLookupTable.getEntries().length > 0 && suffixTableLength > 2 && lastSubstring.length() + suffixTableLength == targetLength) {
                // Find the first target hash which we can solve.
                int targetPsyqHash = -1;
                for (int i = 0; i < hashTargets.length; i++) {
                    MsvcHashTarget suffixTarget = hashTargets[i];
                    if (!suffixTarget.getPsyqRange().isMinValueSameAsMaxValue())
                        continue;

                    int partialPsyqHash = suffixTarget.getPsyqRange().getSingleValue();
                    targetPsyqHash = FroggerHashUtil.getPsyQLinkerHashWithoutPrefixSuffix(partialPsyqHash, prefix + lastSubstring, suffixTarget.suffix);
                    break;
                }

                // Scan each of the lookup table entries for any that solve our hash.
                if (targetPsyqHash >= 0) {
                    int baseIndex = targetPsyqHash * FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE;
                    for (int localIndex = 0; localIndex < FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE; localIndex++) {
                        MsvcSuffixLookupTableEntry lookupTableEntry = suffixLookupTable.getEntries()[baseIndex + localIndex];
                        List<String> usableSuffixes = findSuffixes(lookupTableEntry, lastFullHash, lastPaddedHash, targetFullMsvcHash);
                        for (int i = 0; i < usableSuffixes.size(); i++)
                            results.add(lastSubstring.length() > 0 ? lastSubstring + usableSuffixes.get(i) : usableSuffixes.get(i));
                    }
                }

                continue; // No need to continue, there's no solution as long as the suffix table lookup is reliable.
            }

            // Take a more bruteforce approach to find all characters.
            final char[] availableChars = suffixLookupTable.getAvailableCharacters();
            for (int i = 0; i < availableChars.length; i++) {
                String nextSubstring = lastSubstring + availableChars[i];
                String fullString = prefix + nextSubstring;

                // Determine if the string looks improper.
                int remainingCharCount = targetLength - nextSubstring.length();
                int newHash = getFixedSizeHash(fullString, remainingCharCount);
                int newMatchingBitCount = getMatchingBitLength(targetFullMsvcHash, newHash);
                if (lastMatchingBitCount > newMatchingBitCount)
                    continue; // This is theoretically impossible to happen without selecting an incorrect letter.

                // But, lastMatchingBitCount can equal newMatchingBitCount.
                // In this case, there can be exactly two possible scenarios:
                //  - #1) The bit in newHash is 1, but 0 in targetFullMsvcHash.
                //   - This indicates an invalid substring, as turning newHash's 1 to a 0 bit would require overflowing to higher bits, breaking the correctness of the higher bits.
                //  - #2) The bit in newHash is 0, but 1 in targetFullMsvcHash.
                //   - This is recoverable if and only if there's a trail of 1s up to the bit, so we can overflow them to set the bit equal to 1.
                //  - The bits in targetFullMsvcHash and newHash cannot match (both be 0 or 1) because that would mean lastMatchingBitCount > newMatchingBitCount.
                if (lastMatchingBitCount == newMatchingBitCount) {
                    if ((newHash & (1 << (31 - newMatchingBitCount))) != 0)
                        continue; // Situation #1 described above -- invalid!

                    // Situation #2
                    int nextZeroBit = 30 - newMatchingBitCount;
                    while (nextZeroBit > 0 && (newHash & (1 << nextZeroBit)) != 0)
                        nextZeroBit--;

                    int nextCharacterHighBitIndex = getNextCharacterLowBitIndex(remainingCharCount) + 1;
                    if (nextZeroBit > nextCharacterHighBitIndex)
                        continue; // Even if the nextZeroBit could be set to 1 via overflow, it would be impossible to overflow it a second time, as required to ultimately flip situation #2's bit 0 to bit 1.
                }

                if (targetFullMsvcHash == newHash && doesStringMatchBothMsvcAndPsyqTargets(fullString, hashTargets)) {
                    results.add(nextSubstring);
                } else if (targetLength > nextSubstring.length()) {
                    queue.add(nextSubstring);
                }
            }
        }

        return results.size() > startSize || !queue.isEmpty();
    }

    /**
     * Gets the index of the next low-bit influenced exclusively by the next character
     * @param remainingChars how many unsolved characters remain in the substring
     * @return nextCharacterLowBitIndex
     */
    private static int getNextCharacterLowBitIndex(int remainingChars) {
        // 6 represents the number of bits which are still going to change from future characters.
        // It's 6 because the 7th bit (represented as the number 6) is the highest bit written by any 7-bit ASCII character.
        // It seems highly improbable that any non-7bit ASCII would be used in this, as these are symbol names written in source code.
        // 2 is the number of bits shifted left every new character causes, and thus, the number of bits which will no longer change.
        return 6 + (2 * remainingChars);
    }

    /**
     * Gets the number of bits matched between the two numbers from the highest bit to the lowest bit.
     * @param hash1 the first number
     * @param hash2 the second number
     * @return numberOfMatchingBits
     */
    private static int getMatchingBitLength(int hash1, int hash2) {
        int count = 0;
        for (int i = 31; i >= 0; i--, count++) {
            int mask = (1 << i);
            if ((hash1 & mask) != (hash2 & mask))
                break;
        }

        return count;
    }

    /**
     * Sorts a list of resulting strings in a way which
     * @param prefix the prefix to put in front of all substrings (optional)
     * @param substrings the substrings to sort
     * @param dictionary the dictionary to check substrings against to prioritize them earlier in the list.
     * @return sortedSubstrings
     */
    @SuppressWarnings("ExtractMethodRecommender")
    public static List<String> sortResultsForDisplay(String prefix, List<String> substrings, Set<String> dictionary) {
        if (substrings == null)
            throw new NullPointerException("substrings");

        substrings.sort(GreatQuestHashReverser.HEURISTIC_COMPARISON);
        if (dictionary == null || dictionary.isEmpty())
            return substrings;

        // #1) Get just the part after the last underscore, since no word in the dictionary will be resolved if it has an underscore in it.
        String wordPrefix = null;
        if (prefix != null && prefix.length() > 0) {
            int lastUnderscoreIndex = prefix.lastIndexOf('_');
            if (lastUnderscoreIndex >= 0 && Character.isLetter(prefix.charAt(prefix.length() - 1)))
                wordPrefix = prefix.substring(lastUnderscoreIndex + 1);
        }

        List<String> newResults = new ArrayList<>(substrings.size());

        // #2) Add results found in the dictionary.
        IndexBitArray wordIndices = new IndexBitArray();
        for (int i = 0; i < substrings.size(); i++) {
            String result = substrings.get(i);
            if (dictionary.contains(result) || (wordPrefix != null && wordPrefix.length() > 0 && dictionary.contains(wordPrefix + result))) {
                wordIndices.setBit(i, true);
                newResults.add(result);
            }
        }

        // #3) Add results not found in the dictionary.
        for (int i = 0; i < substrings.size(); i++)
            if (!wordIndices.getBit(i))
                newResults.add(substrings.get(i));

        return newResults;
    }

    // With 37 characters available, a substring length of 5 characters uses 8.6GB of RAM.
    //  - My 36 bytes per string estimate put memory usable only at 2.2GB, so there's something wrong with my calculation.
    // With 27 characters available, a substring length of 6 characters uses 33-41 GB of RAM.
    //  - My 36 bytes per string estimate put memory usable only at 12.98GB, so there's something wrong with my calculation.
    /**
     * Generates a suffix lookup table with suffixes created by the given characters.
     * @param suffixLength the length of the suffixes to generate. Specify 0 to indicate an empty suffix lookup table.
     * @param availableCharacters the characters available to generate suffix permutations from
     * @return lookupTable
     */
    public static MsvcSuffixLookupTable generateMsvcSuffixTable(int suffixLength, char[] availableCharacters) {
        if (suffixLength < 0)
            throw new IllegalArgumentException("suffixLength must not be less than zero! (suffixLength: " + suffixLength + ")");
        if (availableCharacters == null || availableCharacters.length == 0)
            throw new NullPointerException("charTable");

        long memoryUsage = (suffixLength * 2L) + 24; // Memory used per string: ((character count per string * size of each character) + array length (4) + pointer to array (8) + size of hash integer (4) + pointer to string in parent array (8))
        final long dataSize128MB = 128 * 1024 * 1024; // A buffer of data just to be safe.
        long availableMemory = Runtime.getRuntime().maxMemory() - dataSize128MB;
        for (int i = 0; i < suffixLength; i++) { // pow(charTable.length, size).
            memoryUsage *= availableCharacters.length;
            if (memoryUsage > availableMemory)
                throw new OutOfMemoryError("The suffix table is too large to fit in memory. Try reducing the suffixLength from " + suffixLength + " to something lower.");
        }

        // Create storage for string permutations.
        @SuppressWarnings("unchecked") List<String>[] suffixTable = new List[MsvcSuffixLookupTable.LOOKUP_TABLE_SIZE];
        for (int i = 0; i < suffixTable.length; i++)
            suffixTable[i] = new ArrayList<>();

        // Generate string permutations.
        int[] indices = new int[suffixLength];
        char[] characters = new char[suffixLength];
        Arrays.fill(characters, availableCharacters[0]);
        while (true) {
            String suffix = new String(characters);
            int msvcHash = FroggerHashUtil.getMsvcC1HashTableKey(suffix);
            int psyqHash = FroggerHashUtil.getPsyQLinkerHash(suffix);
            int tableKey = (psyqHash * FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE) + msvcHash;
            suffixTable[tableKey].add(suffix);

            // Increment to next suffix.
            int index = 0;
            int nextCharIndex = -1;
            while (indices.length > index && (nextCharIndex = ++indices[index]) >= availableCharacters.length) {
                characters[index] = availableCharacters[0];
                indices[index++] = 0;
            }

            if (index >= indices.length)
                break;

            characters[index] = availableCharacters[nextCharIndex];
        }

        // Convert list of strings to arrays.
        String[] emptyStringArray = new String[0];
        int[] emptyIntArray = new int[0];

        MsvcSuffixLookupTableEntry[] lookupTableEntries = new MsvcSuffixLookupTableEntry[suffixTable.length];
        Comparator<String> sortLogic = Comparator.comparingInt(FroggerHashUtil::getMsvcC1FullHash);
        for (int i = 0; i < suffixTable.length; i++) {
            String[] suffixes = suffixTable[i].toArray(emptyStringArray);
            suffixTable[i] = null; // Clear just in-case we need this memory to GC before the loop ends.
            Arrays.sort(suffixes, sortLogic); // Refer to findSuffixes() for an explanation of why we sort this.

            // Generate hash group info.
            int minimumHashGroupIndex = -1;
            int[] hashGroupInfo = emptyIntArray;
            if (suffixes.length > 0) {
                minimumHashGroupIndex = Integer.MAX_VALUE;
                int maximumHashGroupIndex = Integer.MIN_VALUE;
                for (int j = 0; j < suffixes.length; j++) {
                    int msvcHash = FroggerHashUtil.getMsvcC1FullHash(suffixes[j]);
                    int hashGroupIndex = getMsvcHashGroupFromFullMsvcHash(msvcHash);
                    if (hashGroupIndex < minimumHashGroupIndex)
                        minimumHashGroupIndex = hashGroupIndex;
                    if (hashGroupIndex > maximumHashGroupIndex)
                        maximumHashGroupIndex = hashGroupIndex;
                }

                hashGroupInfo = new int[(maximumHashGroupIndex - minimumHashGroupIndex) + 1];
            }

            int lastHashGroupIndex = -1;
            Arrays.fill(hashGroupInfo, -1);
            for (int j = 0; j < suffixes.length; j++) {
                int msvcHash = FroggerHashUtil.getMsvcC1FullHash(suffixes[j]);
                int hashGroupIndex = getMsvcHashGroupFromFullMsvcHash(msvcHash);

                // Because the suffix is sorted by msvcHash, and hashGroupIndex is created by shifting right msvcHash, the hashGroupIndex values are guaranteed to be sorted too.
                if (hashGroupIndex > lastHashGroupIndex)
                    hashGroupInfo[hashGroupIndex - minimumHashGroupIndex] = (j << 16); // Store the index the group starts.
                hashGroupInfo[hashGroupIndex - minimumHashGroupIndex]++; // Increment the element count for the group.

                lastHashGroupIndex = hashGroupIndex;
            }

            // Store lookup table entry.
            lookupTableEntries[i] = new MsvcSuffixLookupTableEntry(suffixes, hashGroupInfo, minimumHashGroupIndex);
        }

        return new MsvcSuffixLookupTable(availableCharacters, lookupTableEntries, suffixLength);
    }

    /**
     * Gets the conceptual "hash group" which we use to search suffixes efficiently.
     * Note that the provided hash should always be created with an empty prefix.
     * @param fullHashEmptyPrefix the hash to get the group from
     * @return hashGroup
     */
    private static int getMsvcHashGroupFromFullMsvcHash(int fullHashEmptyPrefix) {
        // Shift left by exactly 10 since we've constructed the lookup table in a way to ensure different groups are always incremented by 1024 (bit 11).
        return fullHashEmptyPrefix >>> 10;
    }

    private static List<String> findSuffixes(MsvcSuffixLookupTableEntry lookupTableEntry, int startHash, int paddedStartHash, int targetHash) {
        String[] suffixes = lookupTableEntry.getSuffixes();
        if (suffixes.length == 0)
            return Collections.emptyList();

        // It's faster to search linearly when there aren't many suffixes.
        if (suffixes.length < 100 || lookupTableEntry.getHashGroupCount() <= 3) {
            List<String> results = new ArrayList<>(suffixes.length);
            for (int i = 0; i < suffixes.length; i++) {
                String suffix = suffixes[i];
                int suffixTestMsvcHash = FroggerHashUtil.getMsvcC1FullHash(suffix, startHash);
                if (suffixTestMsvcHash == targetHash)
                    results.add(suffix);
            }

            return results;
        }

        // This method is used to optimize finding valid suffixes from the suffix table.
        // Due to how the MSVC hashing algorithm works, sorting the suffixes by their ASCII IDs produces an array of suffixes sorted in the order of their hash, MOST OF THE TIME.
        // When finding the suffixes with a startHash of 0, the suffix array is perfectly sorted by the resulting full MSVC hash number.

        // Because the array is sorted by hash, and we're looking for suffixes for our target hash, binary search is an obvious pick right?
        // Almost. The sorting occurs for suffixes without any kind of prefix / a startHash of zero.
        // But when we're solving for a hash, we often want to append the suffix to some substring characters we've already chosen.
        // In other words, the startHash is not zero.
        // And when this happens, some suffixes will hash in a way which breaks the sorting order!
        // Example:
        // "apin" and "biwf" are two strings which hash to both the same PsyQ hash and the same MSVC hash.
        // However, when treating them as a suffix, such as the full strings "1apin" and "1biwf", the MSVC hash of those two strings differ.
        // The reason for this is a bit complex, but basically the hashing algorithm for the first characters "a" and "b" produce an interim hash which is very close together.
        // Then, because the letter "p" has a much greater ASCII code than "i", "apin" is able to grow larger than "biwf" when "p" and "i" are added to each hash.
        // Refer to the actual hash algorithms {@code FroggerHashUtil#getMsvcCompilerC1FullHash} to see exactly why the characters might influence the MSVC hash like this.

        // Instead of binary search, I took advantage of some quirks of the MSVC hash.
        // The lookup table is organized in a way so that each lookup table entry is resolved by providing a combination of a psyq hash number and a msvc hash number.
        // This means that the sorted suffix hashes only differ by intervals of 1024 (the number of unique msvc hash table keys) due to how the hashing algorithm works.
        // More specifically, only the first 10 bits and 10 bits after bit 16 are used to calculate the final msvc hash table key from the full msvc hash value.
        // And because the lookup table entries only contains full msvc hashes yielding a specific final msvc hash table key, it means they can only increment in intervals of 1024.
        // Bits 16-25 XOR with bits 0-9 to create the final msvc hash table key from the full MSVC hash, but this doesn't seem to cause any issues with the group-based ordering.
        // This pattern of incrementing in intervals of 1024 remains REGARDLESS OF THE STARTING HASH, although it means comparing only hashes of similar magnitudes together!
        // So we can consider all suffixes that have the same empty-prefix hash value as part of a single group.
        // We can even bake a static secondary lookup table to indicate where each group starts in the suffix array, and how many suffixes are in the group.
        // Therefore, if we can efficiently identify the group ID which contains the suffixes relevant to the target hash with the specified prefix, most suffixes can easily be skipped.
        // That's what this next function (getHashGroupIndex) does, calculates the group ID containing the relevant suffixes.
        // It came as a shock when I discovered this O(1) algorithm, it came as a shock as I was only hoping for an O(log n) algorithm instead.
        int hashGroupIndex = getHashGroupIndex(lookupTableEntry, paddedStartHash, targetHash);

        // Add suffixes from the suffix hash group.
        // Only ONE group is capable of producing the target hash.
        // This is because each hash group is separated by how large the no-prefix hash is.
        // Because the lookup table is separated by the output MSVC hash, the only way to achieve the same MSVC hash from an empty hash prefix is to increase bit 10 onward, not bits 0-9.
        // And there's no way to do that without changing the full MSVC output hash. So therefore, it's impossible to change the hash group without also changing full MSVC hash by 1 << 10 (or however many bits we choose).
        if (hashGroupIndex >= 0 && hashGroupIndex < lookupTableEntry.getSuffixGroupInfo().length) {
            List<String> results = new ArrayList<>();
            applySuffixesFromGroup(results, lookupTableEntry, startHash, targetHash, hashGroupIndex);
            return results;
        }

        return Collections.emptyList();
    }

    private static int getHashGroupIndex(MsvcSuffixLookupTableEntry lookupTableEntry, int startHash, int targetHash) {
        // Why does this work?
        // The short answer is that each group increases the full MSVC hash by a certain number, currently 1024.
        // This is by design/how the suffix array has been sorted to create these "groups".
        // Because each group increases the hash by a constant number, and we're trying to get the hash to a target number, the group we're looking for is the one that makes the hash as close as possible to the target.
        // Normally when we hash(suffix, startHash), we're likely to get something wildly different from the target number.
        // So instead, we'll take 'paddedStartHash + baseSuffixHash', which due to the way the hashes have been sorted, gets closer to targetHash the closer the group ID is to correct.
        // Since the increase with each group is constant, we can just divide the distance between targetHash and the 'paddedStartHash + baseSuffixHash', and that will give us how many groups away we are from the correct group.

        String suffix = lookupTableEntry.getSuffixes()[0]; // Any of the suffixes could be used here as long as we accounted for their group ID.
        int suffixHash = FroggerHashUtil.getMsvcC1FullHash(suffix);
        int hashGroupIndex = getMsvcHashGroupFromFullMsvcHash(suffixHash) - lookupTableEntry.getStartingHashGroupId();

        int hashDistance = targetHash - startHash - suffixHash;
        int groupDistance = hashDistance / FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE; // TODO: Change this if I change the lookup table size specification. (1024)

        int newGroupIndex = hashGroupIndex + groupDistance;
        return newGroupIndex >= 0 && newGroupIndex <= lookupTableEntry.getHashGroupCount() ? newGroupIndex : Integer.MIN_VALUE;
    }

    private static void applySuffixesFromGroup(List<String> results, MsvcSuffixLookupTableEntry lookupTableEntry, int startHash, int targetHash, int hashGroupIndex) {
        int groupInfo = lookupTableEntry.getSuffixGroupInfo()[hashGroupIndex];
        if (groupInfo == -1)
            return;

        int groupStartIndex = groupInfo >>> 16;
        int groupSize = groupInfo & 0xFFFF;
        if (groupSize == 0)
            return;

        String[] suffixes = lookupTableEntry.getSuffixes();
        for (int i = 0; i < groupSize; i++) {
            String suffix = suffixes[groupStartIndex + i];
            int msvcHash = FroggerHashUtil.getMsvcC1FullHash(suffix, startHash);
            if (msvcHash == targetHash)
                results.add(suffix);
        }
    }

    private static DictionaryStringGenerator getDefaultDictionaryStringGenerator() {
        DictionaryStringGenerator dictionaryGenerator = new DictionaryStringGenerator();
        File dictionaryFile = new File("dictionary.txt");
        dictionaryGenerator.loadDictionaryFromFile(null, dictionaryFile);
        return dictionaryGenerator;
    }

    //------------------------------------------------------------------------------------------------
    // TESTING CODE
    //------------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    private static void runTests() {
        // Tests:
        System.out.println("Running MSVC Hash Tests...");

        char[] availableChars = PermutationStringGenerator.ALLOWED_CHARACTERS_ALPHABET;
        MsvcSuffixLookupTable suffixLookupTable0 = generateMsvcSuffixTable(0, availableChars);
        MsvcSuffixLookupTable suffixLookupTable3 = generateMsvcSuffixTable(3, availableChars);
        MsvcSuffixLookupTable suffixLookupTable4 = generateMsvcSuffixTable(4, availableChars);

        // im_select -> hash = 10358682
        // im_sele, size: 2 (Suffix Tables Disabled due to small size)
        // im_sel, size: 3 (Suffix Tables Disabled due to small size.)
        // im_se, size: 4 (Suffix Tables Enabled: 26ms, Disabled: 1.4s), 37 hashes
        // im_s, size: 5 (Suffix Tables Enabled: 315ms, Disabled: 20s), 217 hashes, Ryzen 9 7900X: 37ms
        // im_, size: 6 (Suffix Tables Enabled: 4s, Disabled: 351.88s), 1036 hashes, Ryzen 9 7900X: 103ms
        // im, size: 7 (This works, but it's too slow for me to want to make it into a test actually run.), 14788 hashes complete (Ryzen 9 7900X, Disabled: 339s, Enabled: 2s)
        // 1:496:272,2:497:273,3:498:278,4:499:279,5:500:276
        runTest("im_sele", 2, "1:496:272,2:497:273,3:498:278,4:499:279,5:500:276", suffixLookupTable0, "ct");
        runTest("im_sel", 3, "1:496:272,2:497:273,3:498:278,4:499:279,5:500:276", suffixLookupTable3, "ect");
        runTest("im_se", 4, "1:496:272,2:497:273,3:498:278,4:499:279,5:500:276", suffixLookupTable4, "lect");
        runTest("im_se", 4, "1:496:272,2:497:273,3:498:278,4:499:279,5:500:276", suffixLookupTable3, "lect");
        runTest("im_s", 5, "1:496:272,2:497:273,3:498:278,4:499:279,5:500:276", suffixLookupTable4, "elect");
        runTest("im_s", 5, "1:496:272,2:497:273,3:498:278,4:499:279,5:500:276", suffixLookupTable3, "elect");
        runTest("im_", 6, "1:496:272,2:497:273,3:498:278,4:499:279,5:500:276", suffixLookupTable4, "select");
        runTest("im_", 6, "1:496:272,2:497:273,3:498:278,4:499:279,5:500:276", suffixLookupTable3, "select");

        // im_cav_flash -> hash = 693301698
        // im_cav_, size: 5 (Suffix Tables Enabled: 250ms, Disabled: 24s), 194 hashes, Ryzen 9 7900X: 25ms
        // 1:282:437,2:283:438,3:284:439,4:285:440,5:286:441
        runTest("im_cav_", 5, "1:282:437,2:283:438,3:284:439,4:285:440,5:286:441", suffixLookupTable3, "flash");
        runTest("im_cav_", 5, "1:282:437,2:283:438,3:284:439,4:285:440,5:286:441", suffixLookupTable4, "flash");

        // im_swp_ripple -> hash = A841D101
        // im_swp_, size: 6 (Suffix Tables Enabled: 1.4s, Disabled: 54s), 523 hashes, 75ms
        // b_0:123:523,c_0:124:538,b_1:124:522,c_1:125:537,b_2:125:521,c_2:126:536
        runTest("im_swp_", 6, "b_0:123:523,c_0:124:538,b_1:124:522,c_1:125:537,b_2:125:521,c_2:126:536", suffixLookupTable4, "ripple");
        runTest("im_swp_", 6, "b_0:123:523,c_0:124:538,b_1:124:522,c_1:125:537,b_2:125:521,c_2:126:536", suffixLookupTable3, "ripple");

        // im_cav_ -> I'm not sure what the real solution is, but one viable solution is "sekpool". hash = -1442661551
        // im_cav_, size: 7, 31527 hashes
        // 1:11:896-898,2:12:895-897,3:13:900-903,4:14:899-902
        runTest("im_cav_", 7, "1:11:896-898,2:12:895-897,3:13:900-903,4:14:899-902", suffixLookupTable4, "sekpool");

        System.out.println("Tests Complete.");
        System.out.println();
    }

    private static void runTest(String prefix, int substringLength, String hashTargetsStr, MsvcSuffixLookupTable lookupTable, String... expectedResults) {
        List<String> results = runTest(prefix, substringLength, hashTargetsStr, lookupTable);


        for (int i = 0; i < expectedResults.length; i++) {
            String expectedResult = expectedResults[i];
            if (!results.contains(expectedResult))
                throw new RuntimeException("TEST FAILED! Expected to find '" + expectedResult + "' as one of the substrings generated, but it wasn't found!");
        }

        StringBuilder builder = new StringBuilder(prefix);
        for (int i = 0; i < substringLength; i++)
            builder.append('?');

        System.out.println("TEST PASSED! Found substring(s) " + Arrays.toString(expectedResults) + " for " + builder + ".");
    }

    private static List<String> runTest(String prefix, int substringLength, String hashTargetsStr, MsvcSuffixLookupTable lookupTable) {
        MsvcHashTarget[] hashTargets = parseHashTargets(hashTargetsStr);
        IntList msvcHashes = calculateFullMsvcHashes(prefix, substringLength, hashTargets);

        List<String> results = new ArrayList<>();
        for (int i = 0; i < msvcHashes.size(); i++)
            results.addAll(generateMiddleSubstrings(prefix, substringLength, hashTargets, msvcHashes.get(i), lookupTable));
        return results;
    }

    private static MsvcHashTarget[] parseHashTargets(String hashTargetsStr) {
        String[] split = hashTargetsStr.split(",");
        MsvcHashTarget[] hashTargets = new MsvcHashTarget[split.length];
        for (int i = 0; i < split.length; i++) {
            String[] split2 = split[i].split(":");
            hashTargets[i] = new MsvcHashTarget(split2[0], HashRange.parseRange(split2[1], HashRangeType.PSYQ), HashRange.parseRange(split2[2], HashRangeType.MSVC));
        }

        return hashTargets;
    }
}
