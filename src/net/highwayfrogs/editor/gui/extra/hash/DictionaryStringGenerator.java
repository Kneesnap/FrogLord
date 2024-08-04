package net.highwayfrogs.editor.gui.extra.hash;

import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Generates strings based on a word dictionary.
 * Created by Kneesnap on 2/27/2022.
 */
public class DictionaryStringGenerator implements IHashStringGenerator {
    private final List<String> allLoadedWords = new ArrayList<>();
    private final Map<Integer, List<String>> wordsByHash = new HashMap<>();
    private final List<String> cachedGeneratedList = new ArrayList<>();

    @Override
    public List<String> generateStrings(int targetLinkerHash, String searchQuery) {
        if (allLoadedWords.isEmpty())
            return Collections.singletonList("Please load a dictionary first.");

        List<String> words = wordsByHash.get(targetLinkerHash);
        if (words == null)
            return Collections.emptyList();

        cachedGeneratedList.clear();
        cachedGeneratedList.addAll(words);

        if (searchQuery != null && searchQuery.length() > 0) {
            Iterator<String> iterator = cachedGeneratedList.iterator();
            while (iterator.hasNext()) {
                String word = iterator.next();
                if (!word.contains(searchQuery))
                    iterator.remove();
            }
        }

        return cachedGeneratedList;
    }

    @Override
    public void onSetup(HashPlaygroundController controller) {

    }

    /**
     * Loads a dictionary from the file.
     * @param file The file to load the dictionary from.
     */
    @SneakyThrows
    public void loadDictionaryFromFile(File file) {
        if (file == null || !file.exists() || !file.isFile())
            throw new RuntimeException("File not found: '" + file + "'.");

        allLoadedWords.clear();
        allLoadedWords.addAll(Files.readAllLines(file.toPath()));

        char[] characters = "_0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
        for (char i = 0; i < characters.length; i++) {
            String temp = "" + characters[i];
            if (!allLoadedWords.contains(temp))
                allLoadedWords.add(temp);

            for (char j = 0; j < characters.length; j++) {
                String temp2 = temp + characters[j];
                if (!allLoadedWords.contains(temp2))
                    allLoadedWords.add(temp2);
            }
        }

        wordsByHash.values().forEach(List::clear);
        for (String word : allLoadedWords)
            wordsByHash.computeIfAbsent(FroggerHashUtil.getLinkerHash(word), key -> new ArrayList<>()).add(word);


    }
}