package net.highwayfrogs.editor.games.konami.greatquest;

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
 * </p>
 * Each nibble is controlled by up to two string characters
 * We can look at which sets of characters could have generated the nibble value, and try all the different combinations.
 * But, we don't need to attempt all possible character permutations, we can use a lookup table to ensure we only try valid character combinations.
 * Once all the strings have been found, they are sorted by how likely they are to be valid filenames.
 * </p>
 * The limit of 8 characters is a difficult one. Once a nibble has more than one unknown character pair, we reach an issue.
 * For any character pair, there is another valid character pair that will create any Xor nibble value.
 * In other words, any character pair becomes valid, making it no better than a bruteforce attempt of all permutations.
 * The only way forward from this point is using patterns & knowledge about the original string to fill in missing data.
 * </p>
 * Rainbow tables were considered as a solution, but it became clear pretty quickly collisions were the real challenge.
 * It is trivially easy to take a hash and create a random string that generates that hash here.
 * The challenge is actually in the amount of strings which can be generated which match the hash.
 * Because the amount of potential strings are high, instead an algorithm was designed to let the user tell the algorithm certain things about the string.
 * These things include any known characters, length of the string, if any characters are duplicated, etc.
 * Using this information, the algorithm will reduce the number of possible strings as much as possible.
 * This means instead of generating an exponential number of strings, more than can be kept in memory, let alone sifted through by the user,
 * we can restrict the strings displayed to ones that fit the criteria supplied by the user.
 * TODO: Consider building the tree as a nibble xor value instead of a character pair. Would save a massive amount of memory, potentially allowing for eeking just a few more characters out.
 * Created by Kneesnap on 7/6/2023.
 */
public class GreatQuestHashReverser {
    // Characters outside of this set are not known to be used in Frogger TGQ hashes, even if they would technically work.
    // This set has been limited to reduce the number of garbage strings that the reverse hashing algorithm generates.
    public static final String VALID_HASH_CHARACTER_STRING = "\0 -0123456789[\\]_abcdefghijklmnopqrstuvwxyz{}";
    public static final char[] VALID_HASH_CHARACTERS = VALID_HASH_CHARACTER_STRING.toCharArray();
    private static CharacterPairTreeBase[][] XOR_LOOKUP_TREES; // [height][xorNibble]
    private static short[][] XOR_LOOKUP_TABLE;
    private static short[] CHARACTER_PAIRS;
    private static char[][] CHAR_NIBBLE_LOOKUP_TABLE;
    private static final int NIBBLE_COUNT = 8; // The number of nibbles in a hash. (32 bits / 4 bits per nibble = 8 nibbles)
    private static boolean UPPER_CASE_SUPPORTED;
    private static final int MAXIMUM_UNKNOWN_CHARACTERS_PER_NIBBLE = 2; // 3 is probably feasible but will probably use > 10GB of RAM, and take several minutes to calculate.

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

    public static void main(String[] args) {
        runHashPlayground();
    }

    /**
     * Enters a forever loop which checks the CLI for input on hashes.
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public static void runHashPlayground() {
        initGlobalData();
        System.out.println("Welcome to the Frogger Great Quest hash playground.");
        System.out.println("By default, what you type in will be hashed and you'll be shown the hash of the text.");
        System.out.println("Starting your input with '$<hash>,<template>' will find all strings which matches the hash by filling in the '*' characters in the template.");
        System.out.println("Starting your input with '@<hash>,<template>' will perform searches on the template by replacing '*' with increasing numbers of asterisks.");
        System.out.println("Putting a '!' at the end will force non-repeat mode.");
        // Example Command: '$6AFA9D47,D00lILog*t' will find the letter 'o' for '*' making D00lILog*t.

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String value = scanner.nextLine();

            try {
                handleCommand(value);
            } catch (Throwable th) {
                System.out.println();
                System.out.println("An error occurred.");
                th.printStackTrace();
            }
        }
    }

    private static void handleCommand(String line) {
        if (line.startsWith("$") || line.startsWith("!")) {
            boolean debugMode = line.startsWith("!");
            line = line.substring(1);

            boolean allowDuplicateMode = true;
            if (line.endsWith("!")) {
                line = line.substring(0, line.length() - 1);
                allowDuplicateMode = false;
            }

            if (!line.contains(",")) {
                System.out.println("There is no wildcard to search.");
                return;
            }

            String[] split = line.split(",", 2);
            int hash = Integer.parseUnsignedInt(split[0], 16);
            String template = split[1];
            System.out.println("Brute-forcing '" + template + "' to find strings that hash to '" + Utils.to0PrefixedHexString(hash) + "'.");

            long hashStartTime = System.currentTimeMillis();
            List<String> reverseHashes = GreatQuestHashReverser.reverseHash(template, hash, debugMode, allowDuplicateMode);
            long hashEndTime = System.currentTimeMillis();

            Collections.reverse(reverseHashes); // Show the most likely ones at the bottom to reduce scrolling.
            System.out.println("Results:");
            for (String str : reverseHashes)
                System.out.println(" - " + str);
            System.out.println(reverseHashes.size() + " result(s) in " + (hashEndTime - hashStartTime) + " ms for " + Utils.to0PrefixedHexString(hash) + ".");

        } else if (line.startsWith("@")) {
            line = line.substring(1);

            if (!line.contains(",")) {
                System.out.println("There is no wildcard to search.");
                return;
            }

            String[] split = line.split(",", 2);
            int hash = Integer.parseUnsignedInt(split[0], 16);
            String template = split[1];
            System.out.println("Brute-forcing '" + template + "' to find strings that hash to '" + Utils.to0PrefixedHexString(hash) + "'.");

            long hashStartTime = System.currentTimeMillis();
            List<String> reverseHashes = GreatQuestHashReverser.reverseHashRepeat(template, hash);
            long hashEndTime = System.currentTimeMillis();

            Collections.reverse(reverseHashes); // Show the most likely ones at the bottom to reduce scrolling.
            System.out.println("Results:");
            for (String str : reverseHashes)
                System.out.println(" - " + str);
            System.out.println(reverseHashes.size() + " result(s) in " + (hashEndTime - hashStartTime) + " ms for " + Utils.to0PrefixedHexString(hash) + ".");
        } else if (line.startsWith("\\")) {
            String hashFilePath = GreatQuestUtils.getFileIdFromPath(line);
            System.out.println("Full File Path: '" + line + "'");
            System.out.println("Hash File Path: '" + hashFilePath + "'");
            System.out.println("Hash: " + Utils.to0PrefixedHexString(GreatQuestUtils.hash(hashFilePath)));
        } else {
            System.out.println("Hash: " + Utils.to0PrefixedHexString(GreatQuestUtils.hash(line)));
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
                if (isUpperCaseLetter(temp)) // Must be lower-case to look up in likelihood map.
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
     * Generates potential strings based on a template.
     * @param template The template to search from. Asterisks are replaced with an arbitrary number of characters.
     * @param hash     The hash value to reverse.
     */
    public static List<String> reverseHashRepeat(String template, int hash) {
        int count = 0;
        boolean lastCharAsterisk = false;
        for (int i = 0; i < template.length(); i++) {
            if (template.charAt(i) == '*') {
                if (!lastCharAsterisk) {
                    count++;
                    lastCharAsterisk = true;
                }
            } else {
                lastCharAsterisk = false;
            }
        }

        boolean allowRepeatMode = (count > 1);
        int charactersToAdd = allowRepeatMode ? NIBBLE_COUNT - 1 : NIBBLE_COUNT;
        Set<String> results = new HashSet<>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < charactersToAdd; i++) {
            builder.append('*');
            String testStr = template.replaceAll("(\\*+)", builder.toString());
            System.out.print("Brute-forcing string '" + testStr + "'...");
            List<String> searchResults = reverseHashForTemplate(testStr, hash, false, allowRepeatMode);
            results.addAll(searchResults);
            System.out.print(" Found: ");
            System.out.println(searchResults.size());
        }

        List<String> sortedResults = new ArrayList<>(results);
        sortedResults.sort(Comparator.comparingDouble(GreatQuestHashReverser::calculateScore).reversed());
        return sortedResults;
    }

    /**
     * Generates potential strings for the hash.
     * @param prefix          The prefix to create strings with. Example: "D00lI" or "D00l" or "S00lIquit".
     * @param hash            The hash value to reverse.
     * @param debugMode       If debug information should be displayed.
     * @param allowRepeatMode If "repeat mode" should be allowed.
     */
    public static List<String> reverseHash(String prefix, int hash, boolean debugMode, boolean allowRepeatMode) {
        if (!prefix.contains("*")) {
            if (GreatQuestUtils.hash(prefix) == hash)
                return Collections.singletonList(prefix);

            HashSet<String> results = new HashSet<>();
            for (int i = 0; i < NIBBLE_COUNT; i++) {
                prefix += "*";
                results.addAll(reverseHashForTemplate(prefix, hash, debugMode, false));
            }

            List<String> sortedResults = new ArrayList<>(results);
            sortedResults.sort(Comparator.comparingDouble(GreatQuestHashReverser::calculateScore).reversed());
            return sortedResults;
        }

        return reverseHashForTemplate(prefix, hash, debugMode, allowRepeatMode);
    }

    /**
     * Generates potential strings for the hash using the aforementioned algorithm.
     * @param template        The template string. All characters are treated as literal except '*' which indicates the character should be tested.
     * @param hash            The hash value to reverse.
     * @param debugMode       If debug information should be displayed.
     * @param allowRepeatMode If "repeat mode" should be allowed.
     */
    public static List<String> reverseHashForTemplate(String template, int hash, boolean debugMode, boolean allowRepeatMode) {
        initGlobalData();

        char[] stringChars = template.toCharArray();
        int[] charsToReplace = getReplacementPositions(stringChars);

        // Verify there is at least one character to replace.
        if (charsToReplace.length == 0)
            return (GreatQuestUtils.hash(template) == hash) ? Collections.singletonList(template) : Collections.emptyList();

        // XOR out the known characters, limiting the possible resulting characters.
        int templateHash = GreatQuestUtils.hash(template.replace('*', '\0')); // '\0' is a character that will not modify the string when Xor'd.
        if (debugMode)
            System.out.println("Partial hash from template '" + template + "' is " + Utils.to0PrefixedHexString(templateHash) + ", XOR is " + Utils.to0PrefixedHexString(hash ^ templateHash) + ".");

        // Test if it's in duplicate mode.
        int sequenceLength = 0;
        int sequenceCount = 1;
        for (int i = 0; i < charsToReplace.length; i++) {
            if (i > 0 && charsToReplace[i] > charsToReplace[i - 1] + 1) {
                sequenceCount++;
            } else if (sequenceCount == 1) {
                sequenceLength++;
            }
        }

        // Sequence Count > 2 will break canUseNode, and isn't a realistic use-case anyway.
        boolean repeatMode = (allowRepeatMode && sequenceLength > 2 && sequenceCount == 2 && charsToReplace.length == sequenceLength * sequenceCount);
        int repeatStart = repeatMode ? sequenceLength : -1;

        if (debugMode)
            System.out.println("Duplication Mode: " + repeatMode + " (" + repeatStart + ")");

        // Generate strings from the pairs.
        TGQHashContext context = new TGQHashContext(template, stringChars, charsToReplace, templateHash, hash, repeatStart, debugMode);
        return generateStrings(context);
    }

    private static CharacterPairTreeBase[] setupDefaultTreeArray(TGQHashContext context) {
        // Calculate the number of characters per nibble.
        int[] unknownCharactersPerNibble = new int[NIBBLE_COUNT];
        int lastCharacter = Integer.MIN_VALUE;
        for (int i = 0; i < context.getIndicesToReplace().length; i++) {
            int strPos = context.getIndicesToReplace()[i];
            unknownCharactersPerNibble[calculateNibbleId(context.getTargetLength(), strPos)]++;

            // If this is the start of a sequence, the last nibble is impacted by this character in a way that needs to be tracked in the tree.
            // If we didn't count it, the impact wouldn't be tracked.
            if (strPos > lastCharacter + 1)
                unknownCharactersPerNibble[calculateNibbleId(context.getTargetLength(), strPos - 1)]++;

            lastCharacter = strPos;
        }

        // Verify the number of unknown characters per nibble is supported.
        for (int i = 0; i < unknownCharactersPerNibble.length; i++)
            if (unknownCharactersPerNibble[i] > MAXIMUM_UNKNOWN_CHARACTERS_PER_NIBBLE)
                throw new RuntimeException("The replacement template '" + context.getTemplate() + "' has " + unknownCharactersPerNibble[i] + " unknown characters in nibble slot " + i + ", but only " + MAXIMUM_UNKNOWN_CHARACTERS_PER_NIBBLE + " unknown characters per nibble slot are configured. " + Arrays.toString(unknownCharactersPerNibble));

        // Create array.
        CharacterPairTreeBase[] trees = new CharacterPairTreeBase[NIBBLE_COUNT];
        int strippedHash = context.getTargetHash() ^ context.getTemplateHash();
        for (int i = 0; i < trees.length; i++) {
            int targetXorValue = getNibble(strippedHash, i);
            int characterCount = unknownCharactersPerNibble[i];
            if (characterCount > 0)
                trees[i] = XOR_LOOKUP_TREES[characterCount - 1][targetXorValue];
            if (context.isDebugMode())
                System.out.println("Nibble: " + i + " -> " + targetXorValue + "/" + characterCount);
        }

        return trees;
    }

    private static List<String> generateStrings(TGQHashContext context) {
        // TODO: Consider doing this in parallel. In theory this should distribute very easily.
        // Generate possible strings.
        boolean debugMode = context.isDebugMode();
        List<String> results = new ArrayList<>();
        List<PartialHashString> queue = new ArrayList<>(); // LIFO.
        queue.add(new PartialHashString(context));
        while (queue.size() > 0) {
            PartialHashString temp = queue.remove(queue.size() - 1);

            // The string is complete.
            if (temp.isDone()) {
                if (temp.getHash() == context.getTargetHash() && (!debugMode || GreatQuestUtils.hash(temp.toString()) == context.getTargetHash())) {
                    String tempStr = temp.toString();
                    if (!results.contains(tempStr)) {
                        results.add(tempStr);
                    } else if (debugMode) {
                        System.out.println("Attempted to add duplicate string '" + tempStr + "'.");
                    }
                } else if (debugMode) {
                    System.out.println("Finished string to a non-matching hash: '" + temp + "', Tracked: " + Utils.to0PrefixedHexString(temp.getHash()) + ", String: " + Utils.to0PrefixedHexString(GreatQuestUtils.hash(temp.toString())) + ", Target: " + Utils.to0PrefixedHexString(context.getTargetHash()));
                }

                continue;
            }

            temp.guessNextCharacter(queue);
        }

        results.sort(Comparator.comparingDouble(GreatQuestHashReverser::calculateScore).reversed());
        return results;
    }

    @Getter
    private static class CharacterPairTreeBase {
        // Sorted in ascending order with the first character as the primary key, and the second character as a tiebreaker.
        private final List<CharacterPairTreeNode> children = new ArrayList<>();

        /**
         * Test if this is the root node.
         */
        public boolean isRoot() {
            return true;
        }

        /**
         * Test if this is a leaf node.
         */
        public boolean isLeaf() {
            return this.children.isEmpty();
        }

        /**
         * Attempts to add a child node.
         * @param newNode The node to add.
         * @return If the node was added successfully.
         */
        public boolean addChild(CharacterPairTreeNode newNode) {
            if (newNode == null)
                return false;

            int binarySearchIndex = Collections.binarySearch(this.children, newNode);
            if (binarySearchIndex >= 0) // Value was found already.
                return false;

            int insertPos = -(binarySearchIndex + 1);
            this.children.add(insertPos, newNode);
            return true;
        }

        /**
         * Finds all child nodes whose first character matches the provided one.
         * @param characterPair   The pair of characters encoded as a 16-bit number.
         * @param createIfMissing If this is true and there is no child node found, it will be created.
         * @return The child node corresponding to the provided characters, null if not found and not created.
         */
        public CharacterPairTreeNode getChild(short characterPair, boolean createIfMissing) {
            return this.getChild((char) getPrimaryByteFromShort(characterPair), (char) getSecondaryByteFromShort(characterPair), createIfMissing);
        }

        /**
         * Finds all child nodes whose first character matches the provided one.
         * @param firstChar       The first char in the child node to find.
         * @param secondChar      The second char in the child node to find.
         * @param createIfMissing If this is true and there is no child node found, it will be created.
         * @return The child node corresponding to the provided characters, null if not found and not created.
         */
        public CharacterPairTreeNode getChild(char firstChar, char secondChar, boolean createIfMissing) {
            int left = 0, right = this.children.size() - 1;

            while (left <= right) {
                int midIndex = (left + right) / 2;
                CharacterPairTreeNode midNode = this.children.get(midIndex);

                if (firstChar == midNode.first && secondChar == midNode.second) {
                    return midNode;
                } else if (firstChar > midNode.first || (firstChar == midNode.first && secondChar > midNode.second)) {
                    left = midIndex + 1;
                } else {
                    right = midIndex - 1;
                }
            }

            if (createIfMissing) {
                CharacterPairTreeNode newNode = new CharacterPairTreeNode(firstChar, secondChar);
                if (!this.addChild(newNode)) // If this fails, something is wrong with the above search.
                    throw new RuntimeException("Failed to add new node to tree, but it also wasn't found as a child node? [" + firstChar + secondChar + "]");

                return newNode;
            }

            return null;
        }

        /**
         * Finds all child nodes whose first character matches the provided one.
         * @param searchChar The character to search for.
         * @return childNodes, or null.
         */
        public List<CharacterPairTreeNode> getChildren(char searchChar) {
            if (this.children.isEmpty())
                return null;

            int left = 0, right = this.children.size() - 1;

            while (left <= right) {
                int midIndex = (left + right) / 2;
                CharacterPairTreeNode midNode = this.children.get(midIndex);
                char midsFirstChar = midNode.first;

                if (searchChar == midsFirstChar) {
                    // Find all matching bytes to the left.
                    int leftMin = left;
                    for (left = midIndex; left > leftMin; left--) {
                        if (this.children.get(left).first != searchChar) {
                            left++;
                            break;
                        }
                    }

                    // Find all matching bytes to the right.
                    int rightMax = right;
                    for (right = midIndex; right < rightMax; right++) {
                        if (this.children.get(right).first != searchChar) {
                            right--;
                            break;
                        }
                    }

                    return this.children.subList(left, right + 1);
                } else if (searchChar > midsFirstChar) {
                    left = midIndex + 1;
                } else {
                    right = midIndex - 1;
                }
            }

            return null;
        }

        /**
         * Identifies the node in the root.
         * @return identifier
         */
        protected String getIdentifier() {
            return "TreeRoot";
        }

        /**
         * Writes information about this node and any children to a StringBuilder.
         * @param whitespace The whitespace to write with.
         * @param builder    The builder to write information to.
         */
        public void toString(String whitespace, StringBuilder builder) {
            builder.append(whitespace)
                    .append(getIdentifier())
                    .append(" [")
                    .append(this.children.size())
                    .append(" children]\n");

            if (this.children.size() > 0) {
                String newWhitespace = (whitespace + "  ").intern();
                for (int i = 0; i < this.children.size(); i++)
                    this.children.get(i).toString(newWhitespace, builder);
            }
        }
    }

    @Getter
    private static class CharacterPairTreeNode extends CharacterPairTreeBase implements Comparable<CharacterPairTreeNode> {
        private final char first;
        private final char second;

        public CharacterPairTreeNode(char first, char second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean isRoot() {
            return false;
        }

        @Override
        protected String getIdentifier() {
            return String.valueOf(this.first) + this.second;
        }

        @Override
        public int hashCode() {
            return makeShortFromBytes((byte) this.first, (byte) this.second);
        }

        @Override
        public int compareTo(CharacterPairTreeNode other) {
            if (other == null)
                throw new NullPointerException("The other node was null.");

            int firstCompare = Integer.compare(this.first, other.first);
            if (firstCompare != 0)
                return firstCompare;

            return Integer.compare(this.second, other.second);
        }
    }

    @Getter
    private static class PartialHashString {
        private final TGQHashContext context;
        private final char[] characters;
        private final CharacterPairTreeBase[] treeNodes;
        private final int position;
        private final int hash;

        public PartialHashString(TGQHashContext context) {
            this(context, new char[context.getIndicesToReplace().length], setupDefaultTreeArray(context), 0, context.getTemplateHash());
        }

        private PartialHashString(TGQHashContext context, char[] characters, CharacterPairTreeBase[] treeNodes, int position, int hash) {
            this.context = context;
            this.characters = characters;
            this.treeNodes = treeNodes;
            this.position = position;
            this.hash = hash;
        }

        /**
         * Tests if this guess should have debug information printed.
         * This method exists to allow modification when debugging so the specific strings we want to debug are shown.
         * @return If debugging behavior should apply
         */
        public boolean isDebuggable() {
            return this.context.isDebugMode();
        }

        /**
         * Test if the string has been completed.
         */
        public boolean isDone() {
            return this.position >= this.characters.length;
        }

        /**
         * Gets the index in the template string to the character this partial string will fill.
         * @return templateIndex
         */
        public int getTemplateIndex() {
            if (isDone())
                throw new RuntimeException("A completed string has no position in the template.");

            return this.context.getIndicesToReplace()[this.position];
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
         * Choose the next character in the sequence.
         * @param node The node selected in the tree representing the next character chosen.
         * @return A new object with the slightly more completed string.
         */
        public PartialHashString chooseNext(CharacterPairTreeNode node) {
            return this.chooseNext(node, null);
        }

        /**
         * Choose the next character in the sequence.
         * @param node The node selected in the tree representing the next character chosen.
         * @return A new object with the slightly more completed string.
         */
        public PartialHashString chooseNext(CharacterPairTreeNode node, CharacterPairTreeNode prevNibbleNode) {
            if (isDone())
                throw new RuntimeException("Cannot add more characters to a completed string.");

            // Setup new string array.
            char chosen = node.getFirst();
            char[] newCharacters = Arrays.copyOf(this.characters, this.characters.length);
            newCharacters[this.position] = chosen;

            // Setup new node array.
            CharacterPairTreeBase[] newNodes = Arrays.copyOf(this.treeNodes, this.treeNodes.length);
            newNodes[getNibbleId()] = node;
            if (prevNibbleNode != null)
                newNodes[getPreviousNibbleId()] = prevNibbleNode;

            // For hash calculating purposes, treat it as lower case.
            if (!UPPER_CASE_SUPPORTED && isUpperCaseLetter(chosen))
                chosen = Character.toLowerCase(chosen);

            // Calculate new hash.
            int nibbleId = getNibbleId();
            int newHash = this.hash ^ (((byte) chosen) << (getNibbleId() << 2));
            if (nibbleId == NIBBLE_COUNT - 1) // If we're at the highest bit, make sure to XOR the lowest one too.
                newHash ^= getNibble(chosen, 1);

            return new PartialHashString(this.context, newCharacters, newNodes, this.position + 1, newHash);
        }

        private boolean canUseNode(CharacterPairTreeNode node, boolean isLastCharacterInSequence) {
            boolean debugMode = isDebuggable();

            // Perform duplicate mode checks.
            if (this.context.isRepeatMode()) {
                if (this.position >= this.context.getRepeatSequenceLength()) { // Test if we're past the first sequence.
                    // Ensure we're only allowing the same characters as the first sequence.
                    int relativePos = this.position % this.context.getRepeatSequenceLength();
                    if (node.getFirst() != this.characters[relativePos]) {
                        if (debugMode) {
                            System.out.print(" - Denying '");
                            System.out.print(node.getIdentifier());
                            System.out.print("', because the duplicated character was '");
                            System.out.print(this.characters[relativePos]);
                            System.out.println("'.");
                        }

                        return false;
                    }
                } else {
                    // Determine if any already applied characters impact the current nibble.
                    int currentNibble = getNibbleId();
                    for (int i = 0; i <= this.position; i++) {
                        int futureStrIndex = this.context.getIndicesToReplace()[i + this.context.getRepeatSequenceLength()];
                        int targetNibble = calculateNibbleId(this.context.getTargetLength(), futureStrIndex);
                        if (targetNibble != currentNibble)
                            continue;

                        int actualStrIndex = this.context.getIndicesToReplace()[i];
                        int actualNibble = calculateNibbleId(this.context.getTargetLength(), actualStrIndex);
                        CharacterPairTreeBase futureBase = (i == this.position) ? node : this.treeNodes[actualNibble];
                        if (futureBase.isRoot())
                            continue;

                        // Ensure that the node we're testing can use the .
                        // This works even if this is the end of a sequence because end of sequence always has '\0' as the next character.
                        CharacterPairTreeNode futureNode = (CharacterPairTreeNode) futureBase;
                        if (node.getChild(futureNode.getFirst(), futureNode.getSecond(), false) == null) {
                            if (debugMode) {
                                System.out.print(" - Denying '");
                                System.out.print(node.getIdentifier());
                                System.out.println("', because it wasn't duplicable in the future.");
                            }

                            return false;
                        }
                    }
                }
            }

            // Normal checks.
            if (node.getFirst() == '\0') {
                if (debugMode) {
                    System.out.print(" - Denying '");
                    System.out.print(node.getFirst());
                    System.out.println("'.");
                }

                return false;
            } else if (isLastCharacterInSequence) {
                // This runs at the last character in a sequence.
                // The next character by definition of "end of sequence" was given to us in the template.
                // Therefore, it was XOR'd out, and we're looking for a null character (no impact on XOR) as the secondary character.
                if (node.getSecond() != '\0') {
                    if (debugMode) {
                        System.out.print(" - Denying '");
                        System.out.print(node.getIdentifier());
                        System.out.println("' because at the end of a sequence we need a NULL char.");
                    }

                    // Only null characters are allowed for the last character in a string.
                    return false;
                } else if (debugMode) {
                    System.out.print(" - Approving '");
                    System.out.print(node.getFirst());
                    System.out.println("' for end of sequence.");
                }
            } else if (debugMode) {
                System.out.print(" - Approving '");
                System.out.print(node.getIdentifier());
                System.out.println("'...");
            }

            return true;
        }

        /**
         * Generates possible next characters, and adds them to the provided queue.
         * @param queue The queue to add the guesses to.
         */
        public void guessNextCharacter(List<PartialHashString> queue) {
            if (isDone())
                return; // Nothing to add.

            boolean debugMode = isDebuggable();
            int[] charSlots = this.context.getIndicesToReplace();
            int charTemplatePos = getTemplateIndex(); // The position of the character in the template we are guessing.
            CharacterPairTreeBase currNode = this.treeNodes[getNibbleId()];
            CharacterPairTreeBase prevNode = this.treeNodes[getPreviousNibbleId()];
            boolean isLastCharacterInString = (this.position >= this.characters.length - 1);
            boolean isLastCharacterInSequence = isLastCharacterInString || (charTemplatePos + 1 != charSlots[this.position + 1]);
            boolean isFirstInSequence = (this.position == 0) // If we're beyond the first replacement character,
                    || (charTemplatePos != charSlots[this.position - 1] + 1); // Check if the last character was part of the template (and subsequently was XOR'd out already.)

            if (debugMode) {
                System.out.print("Current Chars: '");
                System.out.print(this);
                System.out.print("', Nibble ID: ");
                System.out.print(getNibbleId());
                System.out.print(", isFirstInSequence: ");
                System.out.print(isFirstInSequence);
                System.out.print(", isLastCharacterInSequence: ");
                System.out.print(isLastCharacterInSequence);
                System.out.print(", isLastCharacterInString: ");
                System.out.println(isLastCharacterInString);
            }

            if (!isFirstInSequence) {
                // Because we know we have at least one character came directly before this one (eg: it is not the first in a sequence),
                // we can use it to determine a short list of potential characters which could belong here.

                // We know the previous one is not a root node because this isn't the first character in the sequence.
                char lastCharacter = ((CharacterPairTreeNode) prevNode).getSecond();
                List<CharacterPairTreeNode> newNodes = currNode.getChildren(lastCharacter);
                if (newNodes == null || newNodes.isEmpty())
                    return; // Skip empty.

                if (debugMode) {
                    System.out.print(" - Found ");
                    System.out.print(newNodes.size());
                    System.out.print(" character pair(s): ");
                    printShortArray(newNodes);
                    System.out.println();
                }

                // Add next characters.
                for (int i = 0; i < newNodes.size(); i++) {
                    CharacterPairTreeNode newNode = newNodes.get(i);
                    if (canUseNode(newNode, isLastCharacterInSequence))
                        queue.add(chooseNext(newNode));
                }

                return;
            }

            // As the first character in a sequence, the previous nibble should have a null character.
            // This is because its next impact is from the character we're about to select.
            // We can use that to limit what nodes can come next though.
            List<CharacterPairTreeNode> prevNodes = prevNode.getChildren('\0');
            if (prevNodes == null || prevNodes.isEmpty())
                return;

            // Add new characters.
            for (int i = 0; i < prevNodes.size(); i++) {
                CharacterPairTreeNode keyNode = prevNodes.get(i);

                // Find the possible pairs we can assume
                List<CharacterPairTreeNode> ourNodes = currNode.getChildren(keyNode.getSecond());
                if (ourNodes == null || ourNodes.isEmpty())
                    continue; // None.

                if (debugMode) {
                    System.out.print(" - Found ");
                    System.out.print(ourNodes.size());
                    System.out.print(" character pairs: ");
                    printShortArray(ourNodes);
                    System.out.println();
                }

                for (int j = 0; j < ourNodes.size(); j++) {
                    CharacterPairTreeNode testNode = ourNodes.get(j);
                    if (canUseNode(testNode, isLastCharacterInSequence))
                        queue.add(chooseNext(testNode, keyNode));
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
        private final int repeatSequenceLength;
        private final boolean debugMode;

        /**
         * Tests if repeat mode is active.
         * Repeat mode means all sequences are the same size as each other, and we assume the same characters are used in each.
         * In other words, finding the characters in the first sequence will find the characters in all remaining sequences.
         * This is useful for filenames like "S17ePBanqBnch\BanqBnchx" where the folder name & file name are the same / duplicated.
         * When we search "$EE1DFE1A,S17ePB*******\B*******x", this search would likely take years through a normal approach.
         * But, using repeat mode cuts it down to a few seconds.
         */
        public boolean isRepeatMode() {
            return this.repeatSequenceLength >= 0;
        }

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

    private static void printShortArray(List<CharacterPairTreeNode> nodes) {
        for (int j = 0; j < nodes.size(); j++) {
            if (j > 0)
                System.out.print(' ');
            System.out.print(nodes.get(j).getFirst());
            System.out.print(nodes.get(j).getSecond());
        }
    }

    private static boolean isUpperCaseLetter(char value) {
        return value >= 'A' && value <= 'Z';
    }

    @SuppressWarnings("unchecked")
    private static void initXorLookupTable() {
        if (XOR_LOOKUP_TABLE != null && CHARACTER_PAIRS != null)
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
        int pairCount = 0;
        for (int i = 0; i < VALID_HASH_CHARACTERS.length; i++) {
            char primary = VALID_HASH_CHARACTERS[i];
            for (int j = 0; j < VALID_HASH_CHARACTERS.length; j++) {
                char secondary = VALID_HASH_CHARACTERS[j];
                byte xorValue = (byte) (getNibble(primary, 0) ^ getNibble(secondary, 1));
                lookupTable[xorValue].add(makeShortFromBytes((byte) primary, (byte) secondary));
                pairCount++;
            }
        }

        // Populate lookup table from list.
        CHARACTER_PAIRS = new short[pairCount];
        for (int i = 0, pair = 0; i < XOR_LOOKUP_TABLE.length; i++) {
            List<Short> list = lookupTable[i];
            list.sort(Comparator.comparingInt(GreatQuestHashReverser::getPrimaryByteFromShort)
                    .thenComparingInt(GreatQuestHashReverser::getSecondaryByteFromShort));

            // Convert list to array.
            short[] newArray = new short[list.size()];
            for (int j = 0; j < list.size(); j++) {
                short value = list.get(j);
                newArray[j] = value;
                CHARACTER_PAIRS[pair++] = value;
            }

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
        initLookupTree();
    }

    private static void initLookupTree() {
        if (XOR_LOOKUP_TREES != null)
            return;

        initNibbleLookupTable(); // We need the character pair nibble lookup table setup.
        long startTime = System.currentTimeMillis();
        System.out.println("Setting up XOR lookup trees...");

        // [height][xorNibble]
        XOR_LOOKUP_TREES = new CharacterPairTreeBase[MAXIMUM_UNKNOWN_CHARACTERS_PER_NIBBLE][];
        for (int i = 0; i < XOR_LOOKUP_TREES.length; i++) {
            long treeStart = System.currentTimeMillis();
            XOR_LOOKUP_TREES[i] = generateLookupTrees(i + 1);
            long treeEnd = System.currentTimeMillis();
            System.out.println("Generated XOR lookup tree of height " + (i + 1) + " in " + (treeEnd - treeStart) + " ms.");
        }

        // Done
        long endTime = System.currentTimeMillis();
        System.out.println("XOR lookup trees setup in " + (endTime - startTime) + " ms.");
    }

    private static CharacterPairTreeBase[] generateLookupTrees(int unknownCharacterCount) {
        CharacterPairTreeBase[] results = new CharacterPairTreeBase[16]; // A nibble has 16 possible values.
        for (int i = 0; i < results.length; i++)
            results[i] = new CharacterPairTreeBase();

        // Generate permutations of all the character pairs.
        int[] currentPairs = new int[unknownCharacterCount];
        int[] currentXorValues = new int[unknownCharacterCount];
        do {
            // XOR the current character pairs.
            int xorNibble = 0;
            for (int i = 0; i < unknownCharacterCount; i++)
                xorNibble ^= currentXorValues[i];

            // Add children to tree.
            CharacterPairTreeBase temp = results[xorNibble];
            for (int i = 0; i < unknownCharacterCount; i++) {
                short characterPair = XOR_LOOKUP_TABLE[currentXorValues[i]][currentPairs[i]];
                temp = temp.getChild(characterPair, true);
            }
        } while (nextPermutation(currentPairs, currentXorValues, unknownCharacterCount - 1));

        return results;
    }

    private static boolean nextPermutation(int[] currentPairs, int[] currentXorValues, int index) {
        if (index < 0) // There are no more permutations.
            return false;

        int currentXor = currentXorValues[index];
        int currentPair = currentPairs[index];
        short[] characterPairs = XOR_LOOKUP_TABLE[currentXor];

        // Move to the next character pair, if possible.
        if (characterPairs.length > currentPair + 1) {
            currentPairs[index]++;
            return true;
        }

        // Move to the next character pair array, by increasing the XOR value.
        if (XOR_LOOKUP_TABLE.length > currentXor + 1) {
            currentPairs[index] = 0;
            currentXorValues[index]++;
            return true;
        }

        // We've gone through all combinations for this index, move to the next one.
        currentPairs[index] = 0;
        currentXorValues[index] = 0;
        return nextPermutation(currentPairs, currentXorValues, index - 1);
    }

    // Tests:
    /*

    Basic Tests:
    $4019FB66,S00lIFrogLog*g (Test single character)
    $4019FB66,S00lIFrogLogo* (Test single character at end of string)
    $4019FB66,S00lIFrogL**og (Test two characters)
    $4019FB66,S00lIFrogLog** (Test two characters at end of string)
    $4019FB66,S00lIFr*gL*gog (Test multiple sequences work)
    $4019FB66,S00lIF**g**gog (Test multiple sequences work)
    $4019FB66,S00lI***g**go* (Test multiple sequences work)

    Long Tests:
    $4019FB66,S00lIFr******* [7 Chars, Results: 180, 34 ms]
    $4019FB66,S00lIF*******g [7 Chars, Results: 120, 2 ms]
    $4019FB66,S00lIF******** [8 Chars, Results: 900, 30 ms]
    $4019FB66,S00lI********g [8 Chars, Results: 900, 19 ms]
    $4019FB66,S00lI********* [9 Chars, Results: 46080, 18195 ms]

    Verify that duplicate character searching works:
    $BABB13D8,S17ePB*******\B*******x (8192ms)
    $BABB13D8,S17ePBr******\Br******x (953 ms)
    $BABB13D8,S17ePBri*****\Bri*****x (563 ms)
    $BABB13D8,S17ePBric****\Bric****x (116 ms)
    $BABB13D8,S17ePBrick***\Brick***x (46 ms)
    $BABB13D8,S17ePBrickP**\BrickP**x (11 ms)
    $BABB13D8,S17ePBrickPc*\BrickPc*x (1 ms)

    General Tests:
    $846BF293,S17ePT***Flag\T***Flagx -> S17ePTowrFlag\TowrFlagx (No Repeat: 30 seconds, Repeat: 72 ms)
    $8A47C99F,S17ePJoy***\Joy***x -> S17ePJoyPic\JoyPicx (No Repeat: 30 seconds, Repeat: 112 ms)
    $7ECD534C,S17ePKnight**\Knight**x -> S17ePKnightSt\KnightStx (No Repeat: 70 ms)
    $D69CBF6A,S17ePKit*****\Kit*****x -> S17ePKitShelf\KitShelfx (Repeat: 450 ms, No Repeat: Too Long)
    $8B3C5ADC,S17ePKit***\Kit***x -> S17ePKitTop\KitTopx (No Repeat: 41 seconds, Repeat: 184 ms)
    $EF67CCCA,S17ePBanq****\Banq****x -> S17ePBanqTble\BanqTblex (No Repeat: Too Long, Repeat: 142 ms)
    $EE1DFE1A,S17ePBanq****\Banq****x -> S17ePBanqBnch\BanqBnchx (No Repeat: Too Long, Repeat: 103 ms)

    Expected Failure:
    !BABB13D8,S17ePBrickPc*\BrickPc*x (Only if we force duplicate mode.)
     */
}