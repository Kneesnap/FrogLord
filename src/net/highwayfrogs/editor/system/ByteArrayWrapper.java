package net.highwayfrogs.editor.system;

/**
 * A wrapper around byte[].
 * Created by Kneesnap on 11/6/2018.
 */
public class ByteArrayWrapper {
    private byte[] array;
    private int length;
    private int growthFactor;

    public ByteArrayWrapper(int initialSize) {
        this(initialSize, 0);
    }

    public ByteArrayWrapper(int initialSize, int growthFactor) {
        this.array = new byte[initialSize];
        this.growthFactor = growthFactor;
    }

    /**
     * Add a byte to this array.
     * @param value The value to add.
     */
    public void add(byte value) {
        if (this.growthFactor > 0 && this.length >= this.array.length) { // We've reached the end of our array, it needs to expand.
            byte[] newArray = new byte[this.array.length + this.growthFactor];
            System.arraycopy(this.array, 0, newArray, 0, this.array.length);
            this.array = newArray;
        }

        this.array[this.length++] = value;
    }

    /**
     * Clear all values from this array.
     */
    public void clear() {
        this.length = 0;
    }

    /**
     * Get the value at a given index.
     * @param index The index to get.
     * @return value
     */
    public byte get(int index) {
        return this.array[index];
    }

    /**
     * Gets the amount of elements in this array.
     * @return elementCount
     */
    public int size() {
        return this.length;
    }
}