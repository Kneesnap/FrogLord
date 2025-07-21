package net.highwayfrogs.editor.gui.extra.hash;

import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.sony.shared.utils.SCMsvcHashReverser.MsvcHashTarget;
import net.highwayfrogs.editor.gui.extra.hash.HashRange.HashRangeType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Generates permutations of dictionary words.
 * Created by Kneesnap on 7/7/2025.
 */
@RequiredArgsConstructor
public class DictionaryPermutationStringGenerator implements IHashStringGenerator {
    private final DictionaryStringGenerator dictionaryGenerator;
    private final List<List<String>> wordsByLength = new ArrayList<>();
    private final List<String> cachedGeneratedList = new ArrayList<>();

    // If the amount of word would exceed this amount, we skip them. It's just too expensive
    private static final long CUTOFF_THRESHOLD = 160000_000_000L;

    @Override
    public List<String> generateStrings(HashPlaygroundController controller) {
        this.cachedGeneratedList.clear();
        HashRange psyqRange = controller.getPsyqTargetHashRange();
        HashRange msvcRange = controller.getMsvcTargetHashRange();
        if (psyqRange == null || msvcRange == null)
            return this.cachedGeneratedList; // The only way this is remotely reasonable is if both are available.

        int maxWordLength = controller.getMaxWordLength();
        if (maxWordLength <= 0)
            return this.cachedGeneratedList;

        String prefix = controller.getPrefix();
        String suffix = controller.getSuffix();
        return generateStrings(prefix, maxWordLength, new MsvcHashTarget(suffix, psyqRange, msvcRange));
    }

    @Override
    public void onSetup(HashPlaygroundController controller) {
        generateWordLookupTable();
    }

    /**
     * Generates strings.
     * NOTE: I like keeping the duplicate strings here. Makes it harder to miss an obvious answer by accident.
     * @param prefix the prefix to generate the strings with
     * @param wordLength the length of the word to generate
     * @param suffixTargets the suffix targets the valid words must validate
     * @return words
     */
    private List<String> generateStrings(String prefix, int wordLength, MsvcHashTarget... suffixTargets) {
        if (prefix == null)
            throw new NullPointerException("prefix");
        if (wordLength <= 0)
            throw new IllegalArgumentException("wordLength must be a positive non-zero number!");
        if (suffixTargets == null || suffixTargets.length == 0)
            throw new IllegalArgumentException("suffixTargets must have at least one target in it!");

        this.cachedGeneratedList.clear();

        for (int length = 0; length < wordLength; length++) {
            List<String> words1 = length > 0 ? getWordsForLength(length) : Collections.singletonList("");
            List<String> words2 = getWordsForLength(wordLength - length);
            if ((long) words1.size() * words2.size() > CUTOFF_THRESHOLD)
                continue; // Too many combinations to try.

            for (int i = 0; i < words1.size(); i++) {
                String compoundPrefix = words1.get(i);
                for (int j = 0; j < words2.size(); j++) {
                    String compound = compoundPrefix + words2.get(j);

                    boolean allMatch = true;
                    for (int k = 0; k < suffixTargets.length; k++) {
                        MsvcHashTarget target = suffixTargets[k];
                        String testWord = prefix + compound + target.getSuffix();
                        if (!target.getMsvcRange().isInRange(FroggerHashUtil.getMsvcC1HashTableKey(testWord))
                                || !target.getPsyqRange().isInRange(FroggerHashUtil.getPsyQLinkerHash(testWord))) {
                            allMatch = false;
                            break;
                        }
                    }

                    if (allMatch)
                        this.cachedGeneratedList.add(compound);
                }
            }
        }

        return this.cachedGeneratedList;
    }

    /**
     * Gets words from the dictionary of a particular length
     * @param length the length to obtain
     * @return words
     */
    public List<String> getWordsForLength(int length) {
        if (this.wordsByLength.isEmpty())
            generateWordLookupTable();

        if (length < 0 || length >= this.wordsByLength.size())
            return Collections.emptyList();

        return this.wordsByLength.get(length);
    }

    private void generateWordLookupTable() {
        this.wordsByLength.clear();

        List<String> allWords = this.dictionaryGenerator.getAllLoadedWords();
        for (int i = 0; i < allWords.size(); i++) {
            String word = allWords.get(i);
            while (word.length() >= this.wordsByLength.size())
                this.wordsByLength.add(new ArrayList<>());
            this.wordsByLength.get(word.length()).add(word);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Please enter the prefix. Example 'im_sub_': ");
        String prefix = scanner.nextLine();
        System.out.print("Please enter the number of characters to target. ");
        HashRange charRange = HashRange.parseRange(scanner.nextLine(), HashRangeType.PSYQ);

        System.out.println("Please enter a comma-separated list of entries of the form 'suffix:psyqHashRange:msvcHashRange': ");
        String entryText = scanner.nextLine();
        String[] split = entryText.split(",");
        MsvcHashTarget[] targets = new MsvcHashTarget[split.length];
        for (int i = 0; i < split.length; i++) {
            String[] split2 = split[i].split(":");
            targets[i] = new MsvcHashTarget(split2[0], HashRange.parseRange(split2[1], HashRangeType.PSYQ), HashRange.parseRange(split2[2], HashRangeType.MSVC));
        }

        DictionaryStringGenerator dictionaryGenerator = new DictionaryStringGenerator();
        File dictionaryFile = new File("dictionary.txt");
        dictionaryGenerator.loadDictionaryFromFile(null, dictionaryFile);

        DictionaryPermutationStringGenerator generator = new DictionaryPermutationStringGenerator(dictionaryGenerator);

        int tempLength = -1;
        while ((tempLength = charRange.getNextValue(tempLength)) > 0) {
            System.out.println();
            System.out.println("Generating for " + tempLength + "...");
            System.out.println();
            List<String> results = generator.generateStrings(prefix, tempLength, targets);

            System.out.println("Results (" + results.size() + "):");
            for (int i = 0; i < results.size(); i++)
                System.out.println(" - " + results.get(i));
        }
    }
}
