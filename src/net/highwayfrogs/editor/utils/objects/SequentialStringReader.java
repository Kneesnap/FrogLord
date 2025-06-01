package net.highwayfrogs.editor.utils.objects;

import lombok.Getter;
import lombok.Setter;

/**
 * Allows reading a string.
 * Created by Kneesnap on 10/29/2024.
 */
@Getter
public class SequentialStringReader {
    private final String input;
    @Setter private int index;

    /**
     * Creates a new string reader.
     * @param input The String to read.
     */
    public SequentialStringReader(String input) {
        this.input = input;
    }

    /**
     * Returns true iff there are remaining characters in the string.
     */
    public boolean hasNext() {
        return this.input != null && this.input.length() > this.index;
    }

    /**
     * Reads the next character from the string, incrementing the positional counter.
     * @return nextChar
     */
    public char read() {
        if (this.input == null || this.index >= this.input.length())
            throw new RuntimeException("The end of the string has been reached.");

        return this.input.charAt(this.index++);
    }

    /**
     * Peeks the next character from the string.
     * @return nextChar
     */
    public char peek() {
        if (this.input == null || this.index >= this.input.length())
            throw new RuntimeException("The end of the string has been reached.");

        return this.input.charAt(this.index);
    }

    /**
     * Change the offset into the string by the given amount.
     * @param offset the number of characters to offset by
     */
    public void skip(int offset) {
        this.index += offset;
    }

    /**
     * Gets the part of the string which had been previously read.
     */
    public String getPreviouslyReadPartOfString() {
        if (this.input == null)
            return null;

        return this.input.substring(0, this.index);
    }
}