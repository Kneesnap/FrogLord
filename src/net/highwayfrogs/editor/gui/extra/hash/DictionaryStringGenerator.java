package net.highwayfrogs.editor.gui.extra.hash;

import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates strings based on a word dictionary.
 * Created by Kneesnap on 2/27/2022.
 */
public class DictionaryStringGenerator implements IHashStringGenerator {
    @Getter private final List<String> allLoadedWords = new ArrayList<>();
    @SuppressWarnings("unchecked") private final List<String>[] wordsByPsyQHash = new List[FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE];
    private final List<String> cachedGeneratedList = new ArrayList<>();

    @Override
    public List<String> generateStrings(HashPlaygroundController controller) {
        this.cachedGeneratedList.clear();
        HashRange psyqRange = controller.getPsyqTargetHashRange();
        HashRange msvcRange = controller.getMsvcTargetHashRange();
        String prefix = controller.getPrefix();
        String suffix = controller.getSuffix();

        if (psyqRange != null) {
            generateStringsPsyQ(this.cachedGeneratedList, psyqRange, msvcRange, prefix, suffix);
        } else if (msvcRange != null) { // Slowly test all words.
            generateStringsMsvc(this.cachedGeneratedList, msvcRange, prefix, suffix);
        }

        return this.cachedGeneratedList;
    }

    private void generateStringsPsyQ(List<String> output, HashRange psyqRange, HashRange msvcRange, String prefix, String suffix) {
        int hash = -1;
        while ((hash = psyqRange.getNextValue(hash)) >= 0) {
            int targetLinkerHash = FroggerHashUtil.getPsyQLinkerHashWithoutPrefixSuffix(hash, prefix, suffix);
            List<String> words = this.wordsByPsyQHash[targetLinkerHash];
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
        for (int i = 0; i < this.allLoadedWords.size(); i++) {
            String word = this.allLoadedWords.get(i);
            String testStr = prefix + word + suffix;
            if (msvcRange.isInRange(FroggerHashUtil.getMsvcC1HashTableKey(testStr)))
                output.add(word);
        }
    }

    @Override
    public void onSetup(HashPlaygroundController controller) {

    }

    /**
     * Loads a dictionary from the file.
     * @param file The file to load the dictionary from.
     */
    @SneakyThrows
    public void loadDictionaryFromFile(ILogger logger, File file) {
        if (file == null || !file.exists() || !file.isFile())
            throw new RuntimeException("File not found: '" + file + "'.");

        this.allLoadedWords.clear();
        long fileReadStart = System.currentTimeMillis();
        this.allLoadedWords.addAll(Files.readAllLines(file.toPath()));

        long setupDictionaryStart = System.currentTimeMillis();
        // Store words per-hash.
        for (int i = 0; i < this.wordsByPsyQHash.length; i++)
            if (this.wordsByPsyQHash[i] != null)
                this.wordsByPsyQHash[i].clear();

        for (int i = 0; i < this.allLoadedWords.size(); i++) {
            String word = this.allLoadedWords.get(i);
            int psyqHash = FroggerHashUtil.getPsyQLinkerHash(word);
            List<String> list = this.wordsByPsyQHash[psyqHash];
            if (list == null)
                this.wordsByPsyQHash[psyqHash] = list = new ArrayList<>();
            list.add(word);
        }

        long finishedTime = System.currentTimeMillis();
        if (logger != null)
            logger.info("Loaded %d words from %s in %d ms. (File Read: %d ms, Dictionary Setup: %d ms)", this.allLoadedWords.size(), file.getName(), finishedTime - fileReadStart, setupDictionaryStart - fileReadStart, finishedTime - setupDictionaryStart);
    }
}