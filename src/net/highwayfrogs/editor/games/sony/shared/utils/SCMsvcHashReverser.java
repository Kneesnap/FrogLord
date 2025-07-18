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
 * Created by Kneesnap on 7/17/2025.
 */
public class SCMsvcHashReverser {
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
        @NonNull private final String[][] suffixes;
        private final int suffixLength;
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

            int suffixTableLength = -1;
            if (tempLength >= 6) {
                suffixTableLength = 4;
            } else if (tempLength >= 4) {
                suffixTableLength = 3;
            }

            long generationStart = System.currentTimeMillis();
            MsvcSuffixLookupTable suffixLookupTable = generateMsvcSuffixTable(suffixTableLength, PermutationStringGenerator.ALLOWED_CHARACTERS_ALPHANUMERIC);
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
                    if (substrings != null && substrings.size() > 0) {
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
     * @return dirtyFullMsvcHashes (Note: Many of the returned hashes will not
     */
    public static IntList calculateFullMsvcHashes(String prefix, int middleSubstringLength, MsvcHashTarget[] targets) {
        if (prefix == null || prefix.isEmpty() || targets == null || targets.length == 0)
            return null; // There's nothing to test the hashes against.
        if (middleSubstringLength < 0)
            throw new IllegalArgumentException("middleSubstringLength must be greater than or equal to zero! (Was: " + middleSubstringLength + ")");

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
    // We want to keep this value as low as possible, because every increment roughly doubles the number of results.
    private static final int LEEWAY_BITS = 11;

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
        int msvcPrefixHash = FroggerHashUtil.getMsvcCompilerC1FullHash(input, 0);
        for (int i = 0; i < missingCharCount; i++)
            msvcPrefixHash = FroggerHashUtil.getMsvcCompilerC1FullHash("\0", msvcPrefixHash);

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
            int fullMsvcHash = FroggerHashUtil.getMsvcCompilerC1FullHash(target.suffix, hash);
            int msvcHash = FroggerHashUtil.getMsvcCompilerC1HashTableKey(FroggerHashUtil.getMsvcCompilerC116BitHash(fullMsvcHash));
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
        int fullMsvcHash = FroggerHashUtil.getMsvcCompilerC1FullHash(input);
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
            int lastFullHash = FroggerHashUtil.getMsvcCompilerC1FullHash(prefix + lastSubstring);
            int lastPaddedHash = getFixedSizeHash(prefix + lastSubstring, targetLength - lastSubstring.length());
            int lastMatchingBitCount = getMatchingBitLength(targetFullMsvcHash, lastPaddedHash);

            // Try to use the suffix table to quickly solve the ways to end the string.
            int suffixTableLength = suffixLookupTable.getSuffixLength();
            if (suffixLookupTable.getSuffixes().length > 0 && suffixTableLength > 2 && lastSubstring.length() + suffixTableLength == targetLength) {
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

                // If we've found any possible PsyQ hashes, validate that they all look good.
                if (targetPsyqHash >= 0) {
                    // TODO: IMPLEMENT!
                }

                // Scan each of the lookup table entries for any that solve our hash.
                if (targetPsyqHash >= 0) {
                    int baseIndex = targetPsyqHash * FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE;
                    for (int localIndex = 0; localIndex < FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE; localIndex++) {
                        String[] suffixes = suffixLookupTable.getSuffixes()[baseIndex + localIndex];
                        if (suffixes.length == 0)
                            continue;

                        List<String> usableSuffixes = findSuffixes(suffixes, lastFullHash, targetFullMsvcHash, lastSubstring);
                        if (usableSuffixes != null)
                            results.addAll(usableSuffixes);
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

    // With 37 characters, it might be feasible to do 5 characters in 2-8GB of RAM. (5GB is the estimate if we use 80 bytes per string, instead of the 34 that I think is correct, which would yield 2.2GB)
    // With 27, characters, it might be feasible to do 6 characters on a machine with 64GB RAM?
    //  - 36 bytes per string estimate puts it at 12.98GB, so 64GB is generous.
    //  - Removing the '_' brings it down to 10.35GB
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
        @SuppressWarnings("unchecked") List<String>[] suffixTable
                = new List[FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE * FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE];
        for (int i = 0; i < suffixTable.length; i++)
            suffixTable[i] = new ArrayList<>();

        // Generate string permutations.
        int[] indices = new int[suffixLength];
        char[] characters = new char[suffixLength];
        Arrays.fill(characters, availableCharacters[0]);
        while (true) {
            String suffix = new String(characters);
            int msvcHash = FroggerHashUtil.getMsvcCompilerC1HashTableKey(suffix);
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
        String[] emptyArray = new String[0];
        String[][] bakedSuffixTable = new String[suffixTable.length][];
        for (int i = 0; i < suffixTable.length; i++) {
            String[] suffixes = suffixTable[i].toArray(emptyArray);
            Arrays.sort(suffixes); // Refer to binarySearchSuffixes() for an explanation of why we sort this.
            bakedSuffixTable[i] = suffixes;
        }

        return new MsvcSuffixLookupTable(availableCharacters, bakedSuffixTable, suffixLength);
    }

    private static List<String> findSuffixes(String[] suffixes, int startHash, int targetHash, String prefix) {
        if (suffixes.length == 0)
            return Collections.emptyList();

        if (suffixes.length < 1000) { // Probably faster to search it like this.
            List<String> results = new ArrayList<>(suffixes.length);
            for (int i = 0; i < suffixes.length; i++) {
                String suffix = suffixes[i];
                int suffixTestMsvcHash = FroggerHashUtil.getMsvcCompilerC1FullHash(suffix, startHash);
                if (suffixTestMsvcHash == targetHash)
                    results.add(prefix != null && prefix.length() > 0 ? prefix + suffix : suffix);
            }

            return results;
        }

        // This method is used to optimize finding valid suffixes from the suffix table.
        // Due to how the MSVC hashing algorithm works, sorting the suffixes by their ASCII IDs produces an array of suffixes sorted in the order of their hash, MOST OF THE TIME.
        // When finding the suffixes with a startHash of 0, the suffix array is perfectly sorted by the resulting full MSVC hash number.

        // However, when the startHash is not zero, a small number of suffixes meeting very specific criteria break the sorting.
        // Example:
        // "apin" and "biwf" are two strings which hash to both the same PsyQ hash and the same MSVC hash.
        // However, when treating them as a suffix, such as the full strings "1apin" and "1biwf", the MSVC hash of those two strings differ.
        // The reason for this is a bit complex, but basically the hashing algorithm for the first characters "a" and "b" produce an interim hash which is very close together.
        // Then, because the letter "p" has a much greater ASCII code than "i", "apin" is able to grow larger than "biwf" when "p" and "i" are added to each hash.

        // TODO: Implement something of a binary search.
        // TODO: Find the suffixes which aren't right. Are they ever what we're looking for?
        // TODO: Note - Calculate the last hash in the array, as if it's lower than the current hash, it means the array resets somewhere, which can be binary searched.

        // TODO: Describe a logical flow which will be efficient for larger lookup tables.
        //  - It's okay if it's not 100% perfect, since it will still show most results, and the speed increase is worth it, taking things from impossible to semi-possible.
        // Example:
        // MISMATCH FOUND! 38 (Largest Size: 1090519036)
        // Hashes: [541, 541, 541, 541, 541, 1090519577, 541, 541, 541, 1090519577, 1090519577, 541, 541, 541, 1090519577, 1090519577, 1090519577, 541, 541, 541, 1090519577, 1090519577, 541, 541, 2586, 2586, 2586, 2586, 2586, 3609, 3609, 3609, 3609, 3610, 3609, 3609, 3610, 3609, 3609, 3609, 3609, 3609, 3609, 3609, 3609, 3609, 3609, 3609, 3610, 3609, 3609]

        return null;
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

        char[] availableChars = PermutationStringGenerator.ALLOWED_CHARACTERS_ALPHANUMERIC;
        MsvcSuffixLookupTable suffixLookupTable0 = generateMsvcSuffixTable(0, availableChars);
        MsvcSuffixLookupTable suffixLookupTable3 = generateMsvcSuffixTable(3, availableChars);
        MsvcSuffixLookupTable suffixLookupTable4 = generateMsvcSuffixTable(4, availableChars);

        // im_select -> hash = 10358682
        // im_sele, size: 2 (Suffix Tables Disabled due to small size)
        // im_sel, size: 3 (Suffix Tables Disabled due to small size.)
        // im_se, size: 4 (Suffix Tables Enabled: 26ms, Disabled: 1.4s)
        // im_s, size: 5 (Suffix Tables Enabled: 315ms, Disabled: 20s)
        // im_, size: 6 (Suffix Tables Enabled: 4s, Disabled: 351.88s)
        // im, size: 7 (This works, but it's too slow for me to want to make it into a test actually run.)
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
        // im_cav_, size: 5 (Suffix Tables Enabled: 250ms, Disabled: 24s)
        // 1:282:437,2:283:438,3:284:439,4:285:440,5:286:441
        runTest("im_cav_", 5, "1:282:437,2:283:438,3:284:439,4:285:440,5:286:441", suffixLookupTable3, "flash");
        runTest("im_cav_", 5, "1:282:437,2:283:438,3:284:439,4:285:440,5:286:441", suffixLookupTable4, "flash");

        // im_swp_ripple -> hash = A841D101
        // im_swp_, size: 6 (Suffix Tables Enabled: 1.4s, Disabled: 54s)
        // b_0:123:523,c_0:124:538,b_1:124:522,c_1:125:537,b_2:125:521,c_2:126:536
        runTest("im_swp_", 6, "b_0:123:523,c_0:124:538,b_1:124:522,c_1:125:537,b_2:125:521,c_2:126:536", suffixLookupTable4, "ripple");
        runTest("im_swp_", 6, "b_0:123:523,c_0:124:538,b_1:124:522,c_1:125:537,b_2:125:521,c_2:126:536", suffixLookupTable3, "ripple");

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
