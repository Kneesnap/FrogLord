package net.highwayfrogs.editor.gui.extra.hash.tree;

import lombok.Getter;
import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;
import net.highwayfrogs.editor.gui.extra.hash.HashPlaygroundController;
import net.highwayfrogs.editor.gui.extra.hash.IHashStringGenerator;

import java.util.*;

/**
 * Uses a tree and dynamic generation
 * Created by Kneesnap on 2/25/2022.
 */
public class HashTreeStringGenerator implements IHashStringGenerator {
    @Getter private static HashSumLookupTree lookupTree;
    @Getter
    private static Map<HashSum, Set<HashStringCountMap>> sumCountMaps;

    @Override
    public List<String> generateStrings(int targetLinkerHash, String searchQuery) {
        Set<HashStringCountMap> allResults = new HashSet<>();


        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            int currHash = targetLinkerHash + (FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE * i);
            if (currHash > lookupTree.getMaxSum())
                break; // Reached end.

            HashSum sum = lookupTree.get(currHash);
            if (sum != null && sumCountMaps.containsKey(sum))
                allResults.addAll(sumCountMaps.get(sum));
        }

        List<String> results = new ArrayList<>();
        HashStringCountMap queryMap = searchQuery != null && searchQuery.length() > 0 ? HashStringCountMap.createCountMap(searchQuery) : null;
        for (HashStringCountMap countMap : allResults)
            if (countMap.contains(queryMap))
                results.add(countMap.generateString());

        return results;

    }

    @Override
    public void onSetup(HashPlaygroundController controller) {
        if (lookupTree == null)
            lookupTree = HashSumLookupTree.buildTree();

        if (sumCountMaps == null)
            buildSumCountMaps();
    }

    private static void buildSumCountMaps() {
        List<HashSum> allSums = new ArrayList<>(lookupTree.getAllSums());
        System.out.println("Sums: " + allSums.size() + ", " + lookupTree.getMaxSum());
        allSums.removeIf(sum -> sum.getSum() >= 512); // TODO: TOSS

        // 1. Build hash sum mappings.
        Map<HashSumPair, HashSum> sumUsingPair = mapPairsToSum(allSums);
        Map<HashSum, List<HashSumPair>> pairsPerSum = mapSumToPairsWhichUseIt(allSums);

        // 2. Get the sums in order.
        List<HashSum> remainingSums = orderSumsByPairAvailability(allSums);

        // 3. Resolve count maps. (This is slow.)
        sumCountMaps = resolveCountMaps(sumUsingPair, pairsPerSum, remainingSums);
    }

    private static Map<HashSumPair, HashSum> mapPairsToSum(List<HashSum> allSums) {
        Map<HashSumPair, HashSum> sumUsingPair = new HashMap<>();
        for (HashSum sum : allSums)
            for (HashSumPair pair : sum.getPairs())
                sumUsingPair.put(pair, sum);

        System.out.println("Linked " + sumUsingPair.size() + " pairs to the sums which they are used by.");
        return sumUsingPair;
    }

    private static Map<HashSum, List<HashSumPair>> mapSumToPairsWhichUseIt(List<HashSum> allSums) {
        List<HashSumPair> allPairs = new ArrayList<>();
        for (HashSum sum : allSums)
            allPairs.addAll(sum.getPairs());

        // Populate seenPairs, seenSums, and populate the sum queue for round #1.
        Map<HashSum, List<HashSumPair>> pairsPerSum = new HashMap<>();
        List<HashSumPair> pairQueue = new ArrayList<>(allPairs);

        while (pairQueue.size() > 0) {
            HashSumPair pair = pairQueue.remove(pairQueue.size() - 1);

            pairsPerSum.computeIfAbsent(pair.getFirst(), key -> new ArrayList<>()).add(pair);
            if (pair.getFirst() != pair.getSecond())
                pairsPerSum.computeIfAbsent(pair.getSecond(), key -> new ArrayList<>()).add(pair);
        }

        System.out.println("Found " + allSums.size() + " sums and " + allPairs.size() + " pairs.");
        System.out.println("Linked " + pairsPerSum.size() + " sums to the pairs which they are used by.");
        return pairsPerSum;
    }

    private static List<HashSum> orderSumsByPairAvailability(List<HashSum> allSums) {
        // Determine the round which each sum should be handled in.
        List<HashSum> remainingSums = new ArrayList<>(allSums);
        Map<HashSum, Integer> sumRound = new HashMap<>();
        while (remainingSums.size() > 0) {
            for (int i = 0; i < remainingSums.size(); i++) {
                HashSum temp = remainingSums.get(i);

                int highestRound = 0;
                for (HashSumPair pair : temp.getPairs()) {
                    Integer round1 = sumRound.get(pair.getFirst());
                    Integer round2 = sumRound.get(pair.getSecond());
                    if (round1 != null && round2 != null) {
                        if (round1 > highestRound)
                            highestRound = round1;
                        if (round2 > highestRound)
                            highestRound = round2;
                    } else {
                        highestRound = -1;
                        break;
                    }
                }

                if (highestRound >= 0) {
                    sumRound.put(temp, highestRound + 1);
                    remainingSums.remove(i--);
                }
            }
        }

        System.out.println("Setup sum rounds for " + sumRound.size() + " sums.");

        // Final step.
        remainingSums.addAll(allSums);
        remainingSums.sort(Comparator.comparingInt(sumRound::get));
        return remainingSums;
    }

    private static Map<HashSum, Set<HashStringCountMap>> resolveCountMaps(Map<HashSumPair, HashSum> sumUsingPair, Map<HashSum, List<HashSumPair>> pairsPerSum, List<HashSum> remainingSums) {
        Map<HashSum, Set<HashStringCountMap>> sumCountMaps = new HashMap<>();
        while (remainingSums.size() > 0) {
            for (int i = 0; i < remainingSums.size(); i++) {
                HashSum tempSum = remainingSums.get(i); // Using this order ensure hash sums have all of their prerequisite pairs and sums handled prior.

                if (tempSum.isSingleCharacter()) {
                    // It's a single character, so start the string count map here.
                    HashStringCountMap newMap = HashStringCountMap.getFree();
                    newMap.incrementCount(tempSum.getCharacter());
                    sumCountMaps.put(tempSum, new HashSet<>(Collections.singletonList(newMap)));
                }

                List<HashSumPair> pairs = pairsPerSum.get(tempSum);

                boolean shouldRemoveFromQueue = true;
                if (pairs != null && pairs.size() > 0) {
                    for (HashSumPair tempPair : pairs) {
                        Set<HashStringCountMap> maps1 = sumCountMaps.get(tempPair.getFirst());
                        Set<HashStringCountMap> maps2 = sumCountMaps.get(tempPair.getSecond());
                        if (maps1 == null || maps2 == null) { // Can't be done yet.
                            shouldRemoveFromQueue = false;
                            continue;
                        }

                        // TODO: Maybe also reduce the ratio of underscores to letters, so you can have 2 total, unless your length is > 7 letters, where you can have 3.

                        for (HashStringCountMap countMap1 : maps1) {
                            for (HashStringCountMap countMap2 : maps2) {
                                HashStringCountMap newCountMap = countMap1.add(countMap2);

                                if (newCountMap.getCount('_') > 3)
                                    continue;

                                int numCount = 0;
                                for (char c = '0'; c <= '9' && numCount <= 3; c++)
                                    numCount += newCountMap.getCount(c);

                                if (numCount > 3)
                                    continue;

                                HashSum nextSum = sumUsingPair.get(tempPair);
                                boolean addedSuccessfully = false;
                                if (nextSum != null && sumCountMaps.computeIfAbsent(nextSum, key -> new HashSet<>()).add(newCountMap))
                                    addedSuccessfully = true;

                                if (!addedSuccessfully)
                                    newCountMap.free();
                            }
                        }
                    }
                }

                if (shouldRemoveFromQueue)
                    remainingSums.remove(i--);
            }
        }

        return sumCountMaps;
    }
}
