package net.highwayfrogs.editor.gui.extra.hash;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates permutation-based strings.
 * Created by Kneesnap on 5/28/2025.
 */
@RequiredArgsConstructor
public class PermutationStringGenerator implements IHashStringGenerator {
    private final int maxPermutationLength;
    private final String permutationPrefix;
    private final String permutationSuffix;
    @NonNull private final char[] availableCharacters;
    private final List<String> allPermutations = new ArrayList<>();
    @SuppressWarnings("unchecked") private final List<String>[] permutedStringsByPsyQHash = new List[FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE];

    public static final char[] ALLOWED_CHARACTERS_ALPHANUMERIC = { // 37 entries.
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    };

    public static final char[] ALLOWED_CHARACTERS_ALPHABET = { // 27 entries.
            '_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    };

    public static final char[] ALLOWED_CHARACTERS_HEXADECIMAL = { // 17 entries.
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', '_',
    };

    public PermutationStringGenerator(int maxPermutationLength, char[] availableCharacters) {
        this(maxPermutationLength, null, null, availableCharacters);
    }

    @Override
    public List<String> generateStrings(HashPlaygroundController controller) {
        HashRange psyqRange = controller.getPsyqTargetHashRange();
        HashRange msvcRange = controller.getMsvcTargetHashRange();
        String prefix = controller.getPrefix();
        String suffix = controller.getSuffix();
        if (!StringUtils.isNullOrWhiteSpace(this.permutationPrefix))
            prefix += this.permutationPrefix;
        if (!StringUtils.isNullOrWhiteSpace(this.permutationSuffix))
            suffix = this.permutationSuffix + suffix;

        List<String> results = new ArrayList<>();
        if (psyqRange != null) {
            generateStringsPsyQ(results, psyqRange, msvcRange, prefix, suffix);
        } else if (msvcRange != null) { // Slowly test all words.
            generateStringsMsvc(results, msvcRange, prefix, suffix);
        }

        return results;
    }

    private void generateStringsPsyQ(List<String> output, HashRange psyqRange, HashRange msvcRange, String prefix, String suffix) {
        int hash = -1;
        while ((hash = psyqRange.getNextValue(hash)) >= 0) {
            int targetLinkerHash = FroggerHashUtil.getPsyQLinkerHashWithoutPrefixSuffix(hash, prefix, suffix);
            List<String> words = this.permutedStringsByPsyQHash[targetLinkerHash];
            if (words == null)
                continue;

            if (msvcRange != null) {
                // Ensure they match the msvc range too.
                for (int i = 0; i < words.size(); i++) {
                    String word = words.get(i);
                    String testStr = prefix + word + suffix;
                    if (msvcRange.isInRange(FroggerHashUtil.getMsvcC1HashTableKey(testStr)))
                        output.add(word);
                }
            } else { // No MSVC range, so use them directly.
                output.addAll(words);
            }
        }
    }

    private void generateStringsMsvc(List<String> output, HashRange msvcRange, String prefix, String suffix) {
        for (int i = 0; i < this.allPermutations.size(); i++) {
            String word = this.allPermutations.get(i);
            String testStr = prefix + word + suffix;
            if (msvcRange.isInRange(FroggerHashUtil.getMsvcC1HashTableKey(testStr)))
                output.add(word);
        }
    }

    @Override
    public void onSetup(HashPlaygroundController controller) {
        long startTime = System.currentTimeMillis();
        char[] allowedNameCharacters = this.availableCharacters;

        // Setup first string group.
        List<PermutationStringNode> queue = new ArrayList<>();
        for (int i = 0; i < allowedNameCharacters.length; i++)
            queue.add(new PermutationStringNode(null, allowedNameCharacters[i]));

        int currLength = 1;
        StringBuilder builder = new StringBuilder();
        List<PermutationStringNode> nextQueue = new ArrayList<>();
        while (this.maxPermutationLength > currLength++) {
            while (queue.size() > 0) {
                PermutationStringNode node = queue.remove(queue.size() - 1);
                addString(node, builder);
                for (int i = 0; i < allowedNameCharacters.length; i++)
                    nextQueue.add(new PermutationStringNode(node, allowedNameCharacters[i]));
            }

            // Prepare for next time.
            queue.addAll(nextQueue);
            nextQueue.clear();
        }

        // Add pending strings.
        for (int i = 0; i < queue.size(); i++)
            addString(queue.get(i), builder);

        controller.getLogger().info("Generated %d permutations (length %d) in %d ms.", this.allPermutations.size(), this.maxPermutationLength, System.currentTimeMillis() - startTime);
    }

    private void addString(PermutationStringNode node, StringBuilder builder) {
        String str = node.toString(builder);
        int psyqHash = FroggerHashUtil.getPsyQLinkerHash(str);

        // Add string to list-based hash.
        List<String> list = this.permutedStringsByPsyQHash[psyqHash];
        if (list == null)
            this.permutedStringsByPsyQHash[psyqHash] = list = new ArrayList<>();
        list.add(str);

        this.allPermutations.add(str);
    }

    @RequiredArgsConstructor
    private static final class PermutationStringNode {
        private final PermutationStringNode parentNode;
        private final char currChar;

        private void recursiveWriteChar(StringBuilder builder) {
            if (this.parentNode != null)
                this.parentNode.recursiveWriteChar(builder);

            builder.append(this.currChar);
        }

        /**
         * Gets the node as a string
         * @param builder the builder to write the string with
         * @return string
         */
        public String toString(StringBuilder builder) {
            builder.setLength(0);
            this.recursiveWriteChar(builder);
            return builder.toString();
        }
    }
}
