package net.highwayfrogs.editor.gui.extra.hash.tree;

import lombok.Getter;
import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;

import java.util.*;
import java.util.Map.Entry;

/**
 * Represents a tree which tracks hash sums.
 * Created by Kneesnap on 2/25/2022.
 */
public class HashSumLookupTree {
    private final List<HashSum> sums = new ArrayList<>();
    @Getter private final List<HashSum> allSums = new ArrayList<>();

    private static final int PASS_COUNT = 5; // 4 is enough for 512.
    public static final char MAX_ALLOWED_CHARACTER;
    public static final char[] ALLOWED_NAME_CHARACTERS = { // This table must be sorted.
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '_', // 48 -> 57, 95.
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    };

    static {
        char maxChar = 0;
        for (char allowedNameCharacter : ALLOWED_NAME_CHARACTERS)
            if (allowedNameCharacter > maxChar)
                maxChar = allowedNameCharacter;

        MAX_ALLOWED_CHARACTER = maxChar;
    }

    /**
     * Gets the maximum sum currently tracked by this tree.
     * @return maxSum
     */
    public int getMaxSum() {
        return this.sums.size() > 0 ? this.sums.get(this.sums.size() - 1).getSum() : -1;
    }

    /**
     * Gets the sum which represents the numerical value supplied.
     * @param sum The numerical value of the HashSum to get.
     * @return The corresponding HashSum, or null, if none exist.
     */
    public HashSum get(int sum) {
        if (sum < 0 || sum >= this.sums.size())
            return null;
        return this.sums.get(sum);
    }

    /**
     * Gets or creates the sum which represents the numerical value supplied.
     * @param sum The numerical value of the HashSum to get.
     * @return The corresponding HashSum.
     */
    public HashSum getOrCreate(int sum) {
        if (sum < 0)
            throw new RuntimeException("Cannot create HashSum less than zero.");
        while (sum >= this.sums.size())
            this.sums.add(null);

        HashSum hashSum = this.sums.get(sum);
        if (hashSum == null) {
            this.sums.set(sum, hashSum = new HashSum(sum));
            this.allSums.add(hashSum);
        }
        return hashSum;
    }

    public static HashSumLookupTree buildTree() {
        HashSumLookupTree newTree = new HashSumLookupTree();

        // Create the basic characters used to generate the rest. (Pass #1)
        for (char character : ALLOWED_NAME_CHARACTERS) {
            HashSum newNode = newTree.getOrCreate(character + 1);
            newNode.getStringLengths().set(1, true); // Set the length as only able to be 1.
        }

        Map<Integer, Set<HashSumPair>> additionsThisPass = new HashMap<>();
        for (int pass = 1; pass < PASS_COUNT; pass++) {
            long passStart = System.currentTimeMillis();

            // Setup additions for this pass.
            for (HashSum sumOne : newTree.allSums) {
                for (HashSum sumTwo : newTree.allSums) {
                    if (sumTwo.getSum() > sumOne.getSum())
                        continue; // Prevent obvious permutations.

                    int newSum = sumOne.getSum() + sumTwo.getSum();
                    additionsThisPass.computeIfAbsent(newSum, key -> new HashSet<>()).add(new HashSumPair(sumOne, sumTwo));
                }
            }

            // Apply additions from this pass.
            int[] firstLength = new int[FroggerHashUtil.PSYQ_MAX_SYMBOL_NAME_LENGTH];
            int[] secondLength = new int[FroggerHashUtil.PSYQ_MAX_SYMBOL_NAME_LENGTH];
            for (Entry<Integer, Set<HashSumPair>> entry : additionsThisPass.entrySet()) {
                HashSum newNode = newTree.getOrCreate(entry.getKey());

                for (HashSumPair pair : entry.getValue()) {
                    if (!newNode.getPairs().add(pair))
                        continue; // Already present somehow? (Perhaps this was created in a previous round, or first == second, and it's the second one added.)

                    int firstCount = pair.getFirst().getStringLengths().getFlags(firstLength);
                    int secondCount = pair.getSecond().getStringLengths().getFlags(secondLength);

                    for (int i = 0; i < firstCount; i++)
                        for (int j = 0; j < secondCount; j++)
                            newNode.getStringLengths().set(firstLength[i] + secondLength[j], true);
                }

                entry.getValue().clear();
            }

            long passEnd = System.currentTimeMillis();
            System.out.println("Pass #" + (pass + 1) + " took " + (passEnd - passStart) + " ms. (" + newTree.allSums.size() + ")");
        }

        return newTree;
    }
}
