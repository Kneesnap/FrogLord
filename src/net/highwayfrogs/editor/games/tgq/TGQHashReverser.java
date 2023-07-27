package net.highwayfrogs.editor.games.tgq;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.Utils;

import java.util.*;

/**
 * Contains algorithm for generating strings that reduce to an arbitrary hash.
 * The original hashing algorithm works by starting with the input string's length.
 * For each character (byte) in the string, the number is shifted 4 bits left (The highest 4 bits are moved to become the lowest 4 bits),
 * then the number's lowest 8 bits are XOR'd with the character byte.
 * On a small scale, raw bruteforce is feasible, but I've designed a more efficient algorithm.
 * The algorithm centers around the idea of a "template". A template is a guide for creating the string.
 * It contains two kinds of characters, known characters and unknown characters.
 * By using data known about the known characters (including ones found by the algorithm), the algorithm will look to find all remaining unknown characters.
 * The first step is to create a special hash of the template string called the template hash.
 * By XORing this template hash against the hash we're trying to crack, we remove the operations of all of the known characters.
 * Thus, the number we are left with can be thought of as 8 nibbles (a nibble being 4 bits, and a 32 bit number / 4 bits = 8 nibbles).
 * Each nibble is controlled by up to two string characters
 * We can look at which sets of characters could have generated the nibble value, and try all the different combinations.
 * But, we don't need to attempt all possible character permutations, we can use a lookup table to ensure we only try valid character combinations.
 * Once all the strings have been found, they are sorted by how likely they are to be valid filenames.
 * TODO: Let's upgrade the algorithm to support more than 8 wildcards.
 * Created by Kneesnap on 7/6/2023.
 */
public class TGQHashReverser {
    // Characters outside of this set are not known to be used in Frogger TGQ hashes, even if they would technically work.
    // This set has been limited to reduce the number of garbage strings that the reverse hashing algorithm generates.
    public static final String VALID_HASH_CHARACTER_STRING = " -0123456789[\\]_abcdefghijklmnopqrstuvwxyz{}";
    public static final char[] VALID_HASH_CHARACTERS = VALID_HASH_CHARACTER_STRING.toCharArray();
    private static short[][] XOR_LOOKUP_TABLE;
    private static char[][] CHAR_NIBBLE_LOOKUP_TABLE;
    private static final int NIBBLE_COUNT = 8; // The number of nibbles in a hash. (32 bits / 4 bits per nibble = 8 nibbles)
    private static boolean UPPER_CASE_SUPPORTED;
    private static final Map<Character, String> LIKELIHOOD_MAP = new HashMap<Character, String>() { // Used for finding what strings look like english text.
        {
            put('a', "ajqoxhzwfykvueigdpmbscrnlt");
            put('b', "xqzkwgvfnhpmjcdtysburoaeil");
            put('c', "xvgfwbpzmdqnscylkurtiehao");
            put('d', "xqzktpcjfbvhwmgnydsluroaie");
            put('e', "jzkqhywbvfgiuxoepmcatldnsr");
            put('f', "vxzjkgpcdhwnmbsytrfauleoi");
            put('g', "qvjzckpfdbwtmsygnhuolraie");
            put('h', "qjzvkghcdpfbwsmnlturyaioe");
            put('i', "yjwhqixukbfpgrmvzedlaotcsn");
            put('j', "vbgwlpstkmcjyhdrnioeau");
            put('k', "qxzjvgcdkpfmbtwrhuynolsaie");
            put('l', "xqjzrhwbgkfnvcmpdstuloyaie");
            put('m', "xqzjkgvhdwctrflnsymubpoiea");
            put('n', "xqjzwhbmylvrkpufncsdaoitge");
            put('o', "jqzhykxfeawbivodgctpsmlurn");
            put('p', "xzqvjkgdcfwbmnypustliaorhe");
            put('q', "fgmydhlnpvwoqsertiau");
            put('r', "xzqjwfvkhlbgpndcrumytsoaie");
            put('s', "zjvdgrbfqwknylmpoacuhiest");
            put('t', "xqjkvdgzpbnfmwclstuyhraoei");
            put('u', "wqujhyzxvkfogedaicpbmtlrsn");
            put('v', "bhwmpzkcgtdnlsvryuoaie");
            put('w', "qvjzgcwpufmytkbdlsrnhoeia");
            put('x', "zkvxgnqrdmwbflshucyopaeti");
            put('y', "qyjvkxhuzfwbgidoeratcnmspl");
            put('z', "jfqgvrnkthmscpwbdulyzoiae");
        }
    };

    /**
     * Enters a forever loop which checks the CLI for input on hashes.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void runHashPlayground() {
        System.out.println("Welcome to the Frogger Great Quest hash playground.");
        System.out.println("By default, what you type in will be hashed and you'll be shown the hash of the text.");
        System.out.println("Starting your input with '$<hash>,<template>' will find all strings which matches the hash by filling in the '*' characters in the template.");
        System.out.println("Using more than 8 wildcard characters in a template is not currently supported.");
        // Example Command: '$6AFA9D47,D00lILog*t' will find the letter 'o' for '*' making D00lILog*t.

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String value = scanner.nextLine();
            if (value.startsWith("$") || value.startsWith("!")) {
                String searchQuery = value.substring(1);

                int hash;
                String template;
                if (value.contains(",")) {
                    String[] split = searchQuery.split(",", 2);
                    hash = Integer.parseUnsignedInt(split[0], 16);
                    template = split[1];
                    System.out.println("Brute-forcing '" + template + "' to find strings that hash to '" + Utils.to0PrefixedHexString(hash) + "'.");
                } else {
                    System.out.println("There is no wildcard to search.");
                    continue;
                }

                List<String> reverseHashes = TGQHashReverser.reverseHash(template, hash, value.startsWith("!"));
                System.out.println("Results [" + reverseHashes.size() + "]:");
                for (String str : reverseHashes)
                    System.out.println(" - " + str);
            } else if (value.startsWith("\\")) {
                String hashFilePath = TGQUtils.getFileIdFromPath(value);
                System.out.println("Full File Path: '" + value + "'");
                System.out.println("Hash File Path: '" + hashFilePath + "'");
                System.out.println("Hash: " + Utils.to0PrefixedHexString(TGQUtils.hash(hashFilePath)));
            } else {
                System.out.println("Hash: " + Utils.to0PrefixedHexString(TGQUtils.hash(value)));
            }
        }
    }

    /**
     * Calculate a score for a string which ranks how likely the string is to "be a valid file name".
     * The general approach is to reward things which look good, but not penalize things which look bad unless we're certain it's bad.
     * This means the more "good" traits we see, the better it gets. We try to weight this by string length, so strings of different lengths are weighed the same.
     * @param str The string to calculate score from.
     * @return score
     */
    @SuppressWarnings({"StatementWithEmptyBody"})
    private static double calculateScore(String str) {
        int readStart = 0;

        // Skip the directory prefix Eg: "S00lI" standing for "\GameSource\Level00Global\Interface\".
        if (str.length() >= 5 && Character.isLetter(str.charAt(0)) && Character.isDigit(str.charAt(1)) &&
                Character.isDigit(str.charAt(2)) && Character.isLetter(str.charAt(3)) && Character.isLetter(str.charAt(4))) {
            readStart = 5;
        }

        boolean wasDigit = true;
        boolean squareBraceOpen = false;
        boolean curlyBraceOpen = false;
        int digitsSeen = 0;

        char lastChar = '\0';
        double score = 0;
        for (int i = readStart; i < str.length() - 1; i++) {
            char temp = str.charAt(i);
            char nextChar = str.charAt(i + 1);
            boolean isDigit = Character.isDigit(temp);
            if ((temp >= 'a' && temp <= 'z') || (temp >= 'A' && temp <= 'Z')) {
                if (isUpperCaseLetter(temp))
                    temp = Character.toLowerCase(temp);

                if (i > 0) {
                    String probabilities = LIKELIHOOD_MAP.get(str.charAt(i - 1));
                    if (probabilities != null) {
                        int pos = probabilities.indexOf(temp);
                        score += (double) (pos + 1) / probabilities.length();
                    }
                }
            } else if (isDigit && digitsSeen++ > 0 && !wasDigit) {
                // Skip, this doesn't fit how we think about numbers.
            } else if ((temp == '_' || temp == '-' || temp == '\\') && (lastChar == temp || nextChar == temp)) {
                // Do nothing / don't increase score.
            } else if (temp == '\\' && (i <= readStart + 2 || i >= str.length() - 3)) {
                // Do nothing.
            } else if (temp == '[') {
                if (squareBraceOpen) {
                    // Definitely not supposed to happen.
                    score -= str.length();
                } else {
                    score += .5;
                    squareBraceOpen = true;
                }
            } else if (temp == ']') {
                if (squareBraceOpen) {
                    score += .5;
                    squareBraceOpen = false;
                } else {
                    // Definitely not good.
                    score -= str.length();
                }
            } else if (temp == '{') {
                if (curlyBraceOpen) {
                    // Definitely not supposed to happen.
                    score -= str.length();
                } else {
                    score += .5;
                    curlyBraceOpen = true;
                }
            } else if (temp == '}') {
                if (curlyBraceOpen) {
                    score += .5;
                    curlyBraceOpen = false;
                } else {
                    // Definitely not good.
                    score -= str.length();
                }
            } else {
                score += .5;
            }

            wasDigit = isDigit;
            lastChar = temp;
        }

        // Bad!
        if (squareBraceOpen)
            score -= str.length();
        if (curlyBraceOpen)
            score -= str.length();

        return score / str.length(); // Use the full length, not the abridged one.
    }

    /**
     * Generates potential strings for the hash.
     * @param prefix The prefix to create strings with. Example: "D00lI" or "D00l" or "S00lIquit".
     * @param hash   The hash value to reverse.
     */
    public static List<String> reverseHash(String prefix, int hash, boolean debugMode) {
        if (!prefix.contains("*")) {
            if (TGQUtils.hash(prefix) == hash)
                return Collections.singletonList(prefix);

            HashSet<String> results = new HashSet<>();
            for (int i = 0; i < NIBBLE_COUNT; i++) {
                prefix += "*";
                results.addAll(reverseHashForTemplate(prefix, hash, debugMode));
            }

            List<String> sortedResults = new ArrayList<>(results);
            sortedResults.sort(Comparator.comparingDouble(TGQHashReverser::calculateScore).reversed());
            return sortedResults;
        }

        return reverseHashForTemplate(prefix, hash, debugMode);
    }

    /**
     * Generates potential strings for the hash using the aforementioned algorithm.
     * @param template The template string. All characters are treated as literal except '*' which indicates the character should be tested.
     * @param hash     The hash value to reverse.
     */
    public static List<String> reverseHashForTemplate(String template, int hash, boolean debugMode) {
        initGlobalData();

        char[] stringChars = template.toCharArray();
        int[] charsToReplace = getReplacementPositions(stringChars);
        if (charsToReplace.length == 0)
            return (TGQUtils.hash(template) == hash) ? Collections.singletonList(template) : Collections.emptyList();

        // Verify there aren't too many nibbles used.
        if (charsToReplace.length > NIBBLE_COUNT)
            throw new RuntimeException("The hash " + Utils.to0PrefixedHexString(hash) + " could not be found using template string '" + template + "', because only " + NIBBLE_COUNT + " wildcard characters are supported.");

        // Verify there aren't two wildcard characters impacting the same nibble.
        boolean[] nibblesAlreadySeen = new boolean[NIBBLE_COUNT];
        for (int i = 0; i < charsToReplace.length; i++) {
            int nibble = calculateNibbleId(template.length(), charsToReplace[i]);
            if (nibblesAlreadySeen[nibble])
                throw new RuntimeException("Wildcard character at index " + charsToReplace[i] + " impacts nibble " + nibble + ", which is already impacted by an earlier wildcard character.");

            nibblesAlreadySeen[nibble] = true;
        }

        int templateHash = TGQUtils.hash(template.replace('*', '\0')); // '\0' is a character that will not modify the string when Xor'd.
        if (debugMode)
            System.out.println("Partial hash from template '" + template + "' is " + Utils.to0PrefixedHexString(templateHash) + ", XOR is " + Utils.to0PrefixedHexString(hash ^ templateHash) + ".");

        // Generate strings from the pairs.
        TGQHashContext context = new TGQHashContext(template, stringChars, charsToReplace, templateHash, hash, debugMode);
        return generateStringsFromNibblePairs(context);
    }

    private static List<String> generateStringsFromNibblePairs(TGQHashContext context) {
        // Generate possible strings.
        boolean debugMode = context.isDebugMode();
        List<String> results = new ArrayList<>();
        List<TGQHashString> queue = new ArrayList<>(); // LIFO.
        queue.add(new TGQHashString(context));
        while (queue.size() > 0) {
            TGQHashString temp = queue.remove(queue.size() - 1);

            // The string is complete.
            if (temp.isDone()) {
                if (temp.getHash() == context.getTargetHash() && (!debugMode || TGQUtils.hash(temp.toString()) == context.getTargetHash())) {
                    String tempStr = temp.toString();
                    if (!results.contains(tempStr)) {
                        results.add(tempStr);
                    } else if (debugMode) {
                        System.out.println("Attempted to add duplicate string '" + tempStr + "'.");
                    }
                } else if (debugMode) {
                    System.out.println("Finished string to a non-matching hash: '" + temp + "', Tracked: " + Utils.to0PrefixedHexString(temp.getHash()) + ", String: " + Utils.to0PrefixedHexString(TGQUtils.hash(temp.toString())) + ", Target: " + Utils.to0PrefixedHexString(context.getTargetHash()));
                }

                continue;
            }

            temp.queueGuessesAtNextCharacter(queue);
        }

        results.sort(Comparator.comparingDouble(TGQHashReverser::calculateScore).reversed());
        return results;
    }

    @Getter
    private static class TGQHashString {
        private final TGQHashContext context;
        private final char[] characters;
        private final int position;
        private final int hash;

        public TGQHashString(TGQHashContext context) {
            this(context, new char[context.getIndicesToReplace().length], 0, context.getTemplateHash());
        }

        private TGQHashString(TGQHashContext context, char[] characters, int position, int hash) {
            this.context = context;
            this.characters = characters;
            this.position = position;
            this.hash = hash;
        }

        /**
         * Test if the string has been completed.
         */
        public boolean isDone() {
            return this.position >= this.characters.length;
        }

        /**
         * Gets the last applied character.
         */
        public char getLastCharacter() {
            if (this.position <= 0)
                throw new RuntimeException("Cannot get last character when no characters have been set.");
            return this.characters[this.position - 1];
        }

        /**
         * Test if the previous nibble slot has been completed.
         */
        public boolean isPreviousNibbleComplete() {
            int prevNibbleId = getPreviousNibbleId();
            // Since a nibble is impacted by two characters, we should be checking for two different characters right?
            // Well no, because we are guaranteed that the current object is one of those, and so the only other relevant nibble ID is the exact match.
            for (int i = this.position + 1; i < this.characters.length; i++)
                if (prevNibbleId == calculateNibbleId(this.context.getTargetLength(), this.context.getIndicesToReplace()[i]))
                    return false;
            return true;
        }

        /**
         * Gets the ID of the nibble the previous character slot takes up.
         */
        public int getPreviousNibbleId() {
            return calculateNibbleId(this.context.getTargetLength(), this.context.getIndicesToReplace()[this.position] - 1);
        }

        /**
         * Gets the ID of the nibble the current character slot takes up.
         */
        public int getNibbleId() {
            return calculateNibbleId(this.context.getTargetLength(), this.context.getIndicesToReplace()[this.position]);
        }

        /**
         * Gets the nibble Xor value for the given nibble ID.
         * @param nibbleId The nibble ID to get the xor value for.
         * @return nibbleXorValue
         */
        public byte getNibbleXor(int nibbleId) {
            return getNibble(this.hash ^ this.context.getTargetHash(), nibbleId);
        }

        /**
         * Choose the next character in the sequence.
         * @param chosen The character to choose.
         * @return A new object with the slightly more completed string.
         */
        public TGQHashString chooseNext(char chosen) {
            if (isDone())
                throw new RuntimeException("Cannot add more characters to a completed string.");

            // Setup new string array.
            char[] newCharacters = Arrays.copyOf(this.characters, this.characters.length);
            newCharacters[this.position] = chosen;

            // For hash calculating purposes, treat it as lower case.
            if (!UPPER_CASE_SUPPORTED && isUpperCaseLetter(chosen))
                chosen = Character.toLowerCase(chosen);

            // Calculate new hash.
            int nibbleId = getNibbleId();
            int newHash = this.hash ^ (((byte) chosen) << (getNibbleId() << 2));
            if (nibbleId == NIBBLE_COUNT - 1) // If we're at the highest bit, make sure to XOR the lowest one too.
                newHash ^= getNibble(chosen, 1);

            return new TGQHashString(this.context, newCharacters, this.position + 1, newHash);
        }

        /**
         * Generates possible next characters, and adds them to the provided queue.
         * This function is a mess, I don't think it's worth the effort to clean up though...
         * @param queue The queue to add the guesses to.
         */
        public void queueGuessesAtNextCharacter(List<TGQHashString> queue) {
            if (isDone())
                return; // Nothing to add.

            boolean debugMode = this.context.isDebugMode();
            int[] charSlots = this.context.getIndicesToReplace();
            boolean isLastCharacterInString = (this.position >= this.characters.length - 1);
            boolean isLastCharacterInSequence = isLastCharacterInString || (charSlots[this.position] + 1 != charSlots[this.position + 1]);
            boolean isFirstInSequence = (this.position == 0) // If we're beyond the first replacement character,
                    || (charSlots[this.position] != charSlots[this.position - 1] + 1); // Check if the last character was part of the template (and subsequently was XOR'd out already.)

            if (debugMode) {
                System.out.print("Current Chars: '");
                System.out.print(this);
                System.out.print("', isFirstInSequence: ");
                System.out.print(isFirstInSequence);
                System.out.print(", isLastCharacter: ");
                System.out.print(isLastCharacterInString);
            }

            if (!isFirstInSequence) {
                // Because we know we have at least one character came directly before this one (eg: it is not the first in a sequence),
                // we can use it to determine a short list of potential characters which could belong here.

                int lastNibbleId = getPreviousNibbleId();
                int lastNibbleXor = getNibble(getNibbleXor(lastNibbleId) ^ getLastCharacter(), 0); // Undoes the last character so we can find the last character.
                short[] charPairs = binarySearchXorTable(XOR_LOOKUP_TABLE[lastNibbleXor], (byte) getLastCharacter());
                if (debugMode) {
                    System.out.print(", Last Nibble: ");
                    System.out.print(lastNibbleId);
                    System.out.print(", XOR: ");
                    System.out.println(lastNibbleXor);
                }

                if (charPairs == null || charPairs.length == 0)
                    return; // Skip empty.

                if (debugMode) {
                    System.out.print(" - Found ");
                    System.out.print(charPairs.length);
                    System.out.print(" character pairs: ");
                    printShortArray(charPairs);
                    System.out.println();
                }

                byte nibbleXor = getNibbleXor(getNibbleId());
                if (isLastCharacterInString) {
                    // Because this is the last character, it means the only bits to apply to the final nibble are from this character.
                    // Therefore, we can have a check to confirm the validity of these characters.

                    if (debugMode) {
                        System.out.print(" - XOR Target: ");
                        System.out.print(nibbleXor);
                        System.out.println();
                    }

                    for (int i = 0; i < charPairs.length; i++) {
                        short value = charPairs[i];
                        byte nextByte = getSecondaryByteFromShort(value);
                        boolean validChar = getNibble(nextByte, 0) == nibbleXor;

                        if (debugMode) {
                            System.out.print(" - ");
                            System.out.print(validChar ? "Approving" : "Denying");
                            System.out.print(" '");
                            System.out.print((char) nextByte);
                            System.out.print("' with XOR ");
                            System.out.print(getNibble(nextByte, 0));
                            System.out.println();
                        }

                        if (validChar)
                            queue.add(chooseNext((char) nextByte));
                    }
                } else {
                    // We know the bits from whatever character we choose will satisfy the last nibble since we operate under the assumption
                    // that the last character was a valid choice, and on this execution path we chose another character as part of the same sequence as this one.
                    // If we are the last character in the sequence, then we must verify the following nibble/character is compatible with our choice.
                    // We do not need to verify if an upcoming nibble is complete, because we're directly testing if the seen character is compatible with the character we're looking to add, something independent of the xor state.

                    // Add next characters.
                    for (int i = 0; i < charPairs.length; i++) {
                        short value = charPairs[i];
                        byte nextByte = getSecondaryByteFromShort(value);

                        if (isLastCharacterInSequence) {
                            // As stated above, we need to check that the byte we've chosen is compatible.
                            // We can test this by checking if the nibble xor matches the lower nibble of the character we're adding.
                            // We don't need to include the other character because it was XOR'd out by nature of it being in the template.
                            byte testXor = getNibble(nextByte, 0);

                            boolean pass = (testXor == nibbleXor);
                            if (debugMode) {
                                System.out.print(" - ");
                                System.out.print(pass ? "Approving" : "Denying");
                                System.out.print(" '");
                                System.out.print((char) nextByte);
                                System.out.print("' with XOR ");
                                System.out.print(testXor);
                                System.out.println('.');
                            }

                            if (!pass)
                                continue;
                        }

                        queue.add(chooseNext((char) nextByte));
                    }
                }

                return;
            }

            // We have the first character in a sequence.

            // All characters present in the template (& the string length) are cancelled out by XORing them.
            // Due to this cancellation, the current nibble has no influences on it besides the lower nibble of the character at this spot.
            // Therefore, we can just find all characters that have a lower nibble matching the current nibble XOR.
            int xorValue = getNibbleXor(getNibbleId());
            if (debugMode) {
                System.out.print(", Nibble: ");
                System.out.print(getNibbleId());
                System.out.print(", XOR: ");
                System.out.println(xorValue);
            }

            if (isLastCharacterInSequence) {
                // We've reached the last character in the string, so the final character we pick is on its own.
                // We can select it by finding values that XOR to the desired value for the nibble, and make the previous nibble match.
                // We do NOT need to care if another character impacts the previous nibble, because if we're at the end of a sequence, it's been XOR'd out, and if we're at the end of the string, there's no further character.

                char[] validChars = CHAR_NIBBLE_LOOKUP_TABLE[xorValue];
                int targetNibble = getNibbleXor(getPreviousNibbleId());
                boolean prevNibbleComplete = isPreviousNibbleComplete(); // TODO: Right now, validChars[] can lookup with the wrong xorValue if the value can be updated from later characters.
                if (debugMode) {
                    System.out.print(" - Testing characters in '");
                    System.out.print(new String(validChars));
                    if (prevNibbleComplete) {
                        System.out.print("' for those who have an upper nibble matching ");
                        System.out.print(targetNibble);
                        System.out.println(".");
                    } else {
                        System.out.println("'.");
                    }
                }

                for (int i = 0; i < validChars.length; i++) {
                    char nextChar = validChars[i];
                    byte highNibble = getNibble(nextChar, 1);

                    // Verify the data XOR'd into the previous nibble validates it.
                    if (!prevNibbleComplete || targetNibble == highNibble) {
                        if (debugMode)
                            System.out.print(" - Approving '");
                        queue.add(chooseNext(nextChar));
                    } else if (debugMode) {
                        System.out.print(" - Denying '");
                    }

                    if (debugMode) {
                        System.out.print(nextChar);
                        System.out.print("', highNibble: ");
                        System.out.println(highNibble);
                    }
                }
            } else {
                // We're somewhere before the end of the string.
                // Valid characters make the current nibble have the correct value.
                // They also are checked against the next nibble to ensure the next value we select is good.
                // Additionally, if the previous nibble is safe to test against, we will test against it.

                short[] charPairs = XOR_LOOKUP_TABLE[xorValue];
                if (charPairs == null || charPairs.length == 0)
                    return; // Nothing found.

                if (debugMode) {
                    System.out.print(" - Found ");
                    System.out.print(charPairs.length);
                    System.out.print(" character pairs: ");
                    printShortArray(charPairs);
                    System.out.println();
                }

                // Add new characters.
                int prevXorNibble = getNibbleXor(getPreviousNibbleId());
                boolean prevNibbleComplete = isPreviousNibbleComplete();

                byte lastPrimary = (byte) -1;
                for (int i = 0; i < charPairs.length; i++) {
                    byte primary = getPrimaryByteFromShort(charPairs[i]);
                    if (primary == lastPrimary)
                        continue;

                    // If the previous nibble is complete, verify our selected character is compatible with it.
                    if (prevNibbleComplete && prevXorNibble != getNibble(primary, 1)) {
                        if (debugMode) {
                            System.out.print(" - Denying '");
                            System.out.print((char) primary);
                            System.out.print("' from '");
                            System.out.print((char) primary);
                            System.out.print((char) getSecondaryByteFromShort(charPairs[i]));
                            System.out.print("', because the character nibble needed to be ");
                            System.out.print(prevXorNibble);
                            System.out.print(", but was ");
                            System.out.print(getNibble(primary, 1));
                            System.out.println('.');
                        }

                        lastPrimary = primary; // Skip to the next primary value.
                        continue;
                    }

                    // Add character.
                    lastPrimary = primary;
                    queue.add(chooseNext((char) primary));
                    if (debugMode) {
                        System.out.print(" - Approving character '");
                        System.out.print((char) primary);
                        System.out.print("' from '");
                        System.out.print((char) primary);
                        System.out.print((char) getSecondaryByteFromShort(charPairs[i]));
                        System.out.println("'.");
                    }
                }
            }
        }

        @Override
        public String toString() {
            char[] template = this.context.getTemplateCharacters();
            int[] charsToReplace = this.context.getIndicesToReplace();
            if (charsToReplace.length != this.characters.length)
                throw new RuntimeException("The amount of characters to replace did not match the act");

            // Replace characters with replacements.
            for (int i = 0; i < charsToReplace.length; i++) {
                char temp = template[charsToReplace[i]];
                template[charsToReplace[i]] = this.characters[i];
                this.characters[i] = temp;
            }

            // Create result string.
            String result = new String(template).replace('\0', '*');

            // Restore template.
            for (int i = 0; i < charsToReplace.length; i++) {
                char temp = template[charsToReplace[i]];
                template[charsToReplace[i]] = this.characters[i];
                this.characters[i] = temp;
            }

            return result;
        }
    }

    @Getter
    @AllArgsConstructor
    private static class TGQHashContext {
        private final String template;
        private final char[] templateCharacters;
        private final int[] indicesToReplace;
        private final int templateHash;
        private final int targetHash;
        private final boolean debugMode;

        /**
         * Gets the length of the strings which can be created from this context.
         */
        public int getTargetLength() {
            return this.templateCharacters.length;
        }
    }

    private static int calculateNibbleId(int strLength, int strPos) {
        // We can ignore the nibbles impacted by the initial value of string length.
        // This is because we've moving from the end of the string (the lowest nibble in a hash) to an offset to the start of the string.
        // We never need to go back to the string length, so we can exclude it from the calculation.
        return (strLength - strPos - 1) % NIBBLE_COUNT;
    }

    private static byte getNibble(int number, int nibbleId) {
        return (byte) ((number >> (nibbleId << 2)) & 0x0F);
    }

    private static short[] binarySearchXorTable(short[] xorTable, byte targetByte) {
        int left = 0, right = xorTable.length - 1;

        while (left <= right) {
            int midIndex = (left + right) / 2;
            byte primaryByte = getPrimaryByteFromShort(xorTable[midIndex]);

            if (targetByte == primaryByte) {
                // Find all matching bytes to the left.
                int leftMin = left;
                for (left = midIndex; left > leftMin; left--) {
                    if (getPrimaryByteFromShort(xorTable[left]) != targetByte) {
                        left++;
                        break;
                    }
                }

                // Find all matching bytes to the right.
                int rightMax = right;
                for (right = midIndex; right < rightMax; right++) {
                    if (getPrimaryByteFromShort(xorTable[right]) != targetByte) {
                        right--;
                        break;
                    }
                }

                return Arrays.copyOfRange(xorTable, left, right + 1);
            } else if (targetByte > primaryByte) {
                left = midIndex + 1;
            } else {
                right = midIndex - 1;
            }
        }

        return null;
    }

    /**
     * Returns the value applied first to a nibble. (The byte which the low bits are taken from)
     * @param value The value to extract the byte from.
     * @return primaryByte
     */
    private static byte getPrimaryByteFromShort(short value) {
        return (byte) (value >>> Constants.BITS_PER_BYTE);
    }

    /**
     * Returns the value applied second to a nibble. (The byte which the high bits are taken from)
     * @param value The value to extract the byte from.
     * @return secondaryByte
     */
    private static byte getSecondaryByteFromShort(short value) {
        return (byte) (value & 0xFF);
    }

    private static short makeShortFromBytes(byte primary, byte secondary) {
        return (short) ((primary << Constants.BITS_PER_BYTE) | secondary);
    }

    private static int[] getReplacementPositions(char[] stringArray) {
        // Find number of wildcards.
        int count = 0;
        for (int i = 0; i < stringArray.length; i++)
            if (stringArray[i] == '*')
                count++;

        // Create position array.
        int[] posArray = new int[count];
        count = 0;
        for (int i = 0; i < stringArray.length; i++) {
            if (stringArray[i] == '*') {
                posArray[count++] = i;
                stringArray[i] = VALID_HASH_CHARACTERS[0];
            }
        }

        return posArray;
    }

    private static void printShortArray(short[] array) {
        for (int j = 0; j < array.length; j++) {
            if (j > 0)
                System.out.print(' ');
            System.out.print((char) getPrimaryByteFromShort(array[j]));
            System.out.print((char) getSecondaryByteFromShort(array[j]));
        }
    }

    @SuppressWarnings("unchecked")
    private static void initXorLookupTable() {
        if (XOR_LOOKUP_TABLE != null)
            return;

        // Setup upper case test.
        UPPER_CASE_SUPPORTED = false;
        for (int i = 0; i < VALID_HASH_CHARACTERS.length; i++) {
            if (isUpperCaseLetter(VALID_HASH_CHARACTERS[i])) {
                UPPER_CASE_SUPPORTED = true;
                break;
            }
        }

        // Create table.
        XOR_LOOKUP_TABLE = new short[16][]; // We get an array for each nibble.

        // Verify characters are valid. These only permit characters representable as a byte, because we should never have a situation where that is not the case.
        for (int i = 0; i < VALID_HASH_CHARACTERS.length; i++)
            if (VALID_HASH_CHARACTERS[i] != (VALID_HASH_CHARACTERS[i] & 0x7F))
                throw new RuntimeException("The character '" + VALID_HASH_CHARACTERS[i] + "' cannot be hashed. (Value: " + ((int) VALID_HASH_CHARACTERS[i]) + ")");

        // Create temporary holder.
        List<Short>[] lookupTable = new List[XOR_LOOKUP_TABLE.length];
        for (int i = 0; i < lookupTable.length; i++)
            lookupTable[i] = new ArrayList<>();

        // Map xor nibbles to the character combinations that can create them.
        for (int i = 0; i < VALID_HASH_CHARACTERS.length; i++) {
            char primary = VALID_HASH_CHARACTERS[i];
            for (int j = 0; j < VALID_HASH_CHARACTERS.length; j++) {
                char secondary = VALID_HASH_CHARACTERS[j];
                byte xorValue = (byte) (getNibble(primary, 0) ^ getNibble(secondary, 1));
                lookupTable[xorValue].add(makeShortFromBytes((byte) primary, (byte) secondary));
            }
        }

        // Populate lookup table from list.
        for (int i = 0; i < XOR_LOOKUP_TABLE.length; i++) {
            List<Short> list = lookupTable[i];
            list.sort(Comparator.comparingInt(TGQHashReverser::getPrimaryByteFromShort)
                    .thenComparingInt(TGQHashReverser::getSecondaryByteFromShort));

            // Convert list to array.
            short[] newArray = new short[list.size()];
            for (int j = 0; j < list.size(); j++)
                newArray[j] = list.get(j);

            XOR_LOOKUP_TABLE[i] = newArray;
        }
    }

    @SuppressWarnings("unchecked")
    private static void initNibbleLookupTable() {
        if (CHAR_NIBBLE_LOOKUP_TABLE != null)
            return;

        // Create table.
        CHAR_NIBBLE_LOOKUP_TABLE = new char[16][]; // We get an array for each nibble.

        // Create temporary holder.
        List<Character>[] lookupTable = new List[CHAR_NIBBLE_LOOKUP_TABLE.length];
        for (int i = 0; i < lookupTable.length; i++)
            lookupTable[i] = new ArrayList<>();

        // Map char nibbles to the characters that create them.
        for (int i = 0; i < VALID_HASH_CHARACTERS.length; i++) {
            char temp = VALID_HASH_CHARACTERS[i];
            lookupTable[getNibble(temp, 0)].add(temp);
        }

        // Populate lookup table from list.
        for (int i = 0; i < CHAR_NIBBLE_LOOKUP_TABLE.length; i++) {
            List<Character> list = lookupTable[i];

            // Convert list to array.
            char[] newArray = new char[list.size()];
            for (int j = 0; j < list.size(); j++)
                newArray[j] = list.get(j);

            CHAR_NIBBLE_LOOKUP_TABLE[i] = newArray;
        }
    }

    private static void initGlobalData() {
        initNibbleLookupTable();
        initXorLookupTable();
    }

    private static boolean isUpperCaseLetter(char value) {
        return value >= 'A' && value <= 'Z';
    }
}