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
     * Resize the underlying array.
     * @param newSize The underlying array's new size.
     */
    public void resize(int newSize) {
        byte[] newArray = new byte[newSize];
        System.arraycopy(this.array, 0, newArray, 0, arraySize());
        this.array = newArray;
    }

    /**
     * Add a byte to this array.
     * @param value The value to add.
     */
    public void add(byte value) {
        if (this.growthFactor > 0 && this.length >= arraySize()) // We've reached the end of our array, it needs to expand.
            resize(arraySize() + this.growthFactor);

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

    /**
     * Gets the size of the underlying array.
     * @return arraySize
     */
    public int arraySize() {
        return this.array.length;
    }
}