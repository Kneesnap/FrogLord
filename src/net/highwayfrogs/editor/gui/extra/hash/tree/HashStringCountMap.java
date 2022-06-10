package net.highwayfrogs.editor.gui.extra.hash.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tracks how many of each character is present, as a string blueprint.
 * Created by Kneesnap on 2/25/2022.
 */
public class HashStringCountMap {
    private final byte[] counts = new byte[HashSumLookupTree.ALLOWED_NAME_CHARACTERS.length + 1];
    private static final int[] INDEX_BY_CHARACTER_MAP;
    private static final StringBuilder TEMP_BUILDER = new StringBuilder();
    private static final List<HashStringCountMap> POOL = new ArrayList<>();

    /**
     * Test whether this count map contains all of the characters another count map contains.
     * @param other The other count map to check.
     * @return contains
     */
    public boolean contains(HashStringCountMap other) {
        if (other == null)
            return true;

        for (int i = 0; i < this.counts.length; i++)
            if ((this.counts[i] & 0xFF) < (other.counts[i] & 0xFF))
                return false;

        return true;
    }

    /**
     * Gets the number of times a certain character is used.
     * @param character The character to check.
     */
    public int getCount(char character) {
        int index = INDEX_BY_CHARACTER_MAP[character];
        return (index != -1) ? (this.counts[index] & 0xFF) : 0;
    }

    /**
     * Increments the number of times a given character is used. The character must be valid.
     * @param character The character to increment usages for.
     * @return Times used.
     */
    public int incrementCount(char character) {
        int index = INDEX_BY_CHARACTER_MAP[character];
        if (index == -1)
            throw new RuntimeException("Cannot increment count for character '" + character + "', it is not a valid string character.");
        return ++this.counts[index];
    }

    /**
     * Creates a copy of this count map.
     * @return countMapCopy
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public HashStringCountMap clone() {
        HashStringCountMap clone = getFree();
        System.arraycopy(this.counts, 0, clone.counts, 0, this.counts.length);
        return clone;
    }

    /**
     * Adds another count map to this one, resulting in a new sum count map.
     * @param other The count map to sum with this one.
     * @return sumCountMap
     */
    public HashStringCountMap add(HashStringCountMap other) {
        HashStringCountMap result = this.clone();
        for (int i = 0; i < this.counts.length; i++)
            result.counts[i] += other.counts[i];
        return result;
    }

    /**
     * Generates a string which matches the criteria of the count map.
     * @return generatedString
     */
    public String generateString() {
        for (char c = 0; c < INDEX_BY_CHARACTER_MAP.length; c++) {
            int index = INDEX_BY_CHARACTER_MAP[c];
            if (index == -1)
                continue;

            for (int j = 0; j < this.counts[index]; j++)
                TEMP_BUILDER.append(c);
        }

        String result = TEMP_BUILDER.toString();
        TEMP_BUILDER.setLength(0);
        return result;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        for (int i = 0; i < this.counts.length; i++)
            for (int j = 0; j < this.counts[i]; j++)
                hashCode = (31 * hashCode) + i;
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof HashStringCountMap) && Arrays.equals(this.counts, ((HashStringCountMap) other).counts);
    }

    public void free() {
        Arrays.fill(this.counts, (byte) 0);
        POOL.add(this);
    }

    public static HashStringCountMap getFree() {
        return POOL.size() > 0 ? POOL.remove(POOL.size() - 1) : new HashStringCountMap();
    }

    static {
        INDEX_BY_CHARACTER_MAP = new int[HashSumLookupTree.MAX_ALLOWED_CHARACTER + 1];
        Arrays.fill(INDEX_BY_CHARACTER_MAP, -1);
        for (int i = 0; i < HashSumLookupTree.ALLOWED_NAME_CHARACTERS.length; i++)
            INDEX_BY_CHARACTER_MAP[HashSumLookupTree.ALLOWED_NAME_CHARACTERS[i]] = i;
    }

    /**
     * Creates a count map by parsing a string.
     * @param inputStr The string to create a count map from. If it contains any invalid characters, an exception will be thrown.
     * @return newCountMap
     */
    public static HashStringCountMap createCountMap(String inputStr) {
        HashStringCountMap newCountMap = getFree();
        if (inputStr == null || inputStr.isEmpty())
            return newCountMap;

        for (int i = 0; i < inputStr.length(); i++)
            newCountMap.incrementCount(inputStr.charAt(i));
        return newCountMap;
    }
}
