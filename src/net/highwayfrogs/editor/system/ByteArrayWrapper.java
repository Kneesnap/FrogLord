package net.highwayfrogs.editor.system;

/**
 * A wrapper around byte[].
 * Created by Kneesnap on 11/6/2018.
 */
public class ByteArrayWrapper {
    private final boolean allowGrowth;
    private byte[] array;
    private int length;

    public ByteArrayWrapper() {
        this(0, true);
    }

    public ByteArrayWrapper(int initialSize) {
        this(initialSize, false);
    }

    public ByteArrayWrapper(int initialSize, boolean allowGrowth) {
        this.array = new byte[initialSize];
        this.allowGrowth = allowGrowth;
    }

    /**
     * Copy the contents of this wrapper to a byte array.
     * @param copyArray The array to copy contents into.
     * @return array
     */
    public byte[] toArray(byte[] copyArray, int destIndex) {
        System.arraycopy(this.array, 0, copyArray, destIndex, Math.min(copyArray.length - destIndex, this.array.length));
        return copyArray;
    }

    /**
     * Copy the contents of this wrapper to a byte array.
     * @return array
     */
    public byte[] toNewArray() {
        return toArray(new byte[size()], 0);
    }

    /**
     * Resize the underlying array.
     * @param newSize The underlying array's new size.
     */
    public void resize(int newSize) {
        this.array = toArray(new byte[newSize], 0);
    }

    /**
     * Sets a byte value at an array index
     * @param index the index into the array
     * @param value the value to apply
     */
    public void set(int index, byte value) {
        if (index < 0 || index >= this.length)
            throw new IndexOutOfBoundsException("Array Length: " + this.length + ", Index: " + index);

        this.array[index] = value;
    }

    /**
     * Copies values from a byte array.
     * @param source the source array to copy bytes from
     * @param sourceOffset the offset into the source array to copy from
     * @param amount the amount of bytes to copy
     */
    public void set(int index, byte[] source, int sourceOffset, int amount) {
        if (amount == 0)
            return;

        ensureCapacity(amount);
        System.arraycopy(source, sourceOffset, this.array, index, amount);
        if (index + amount > this.length)
            this.length = index + amount;
    }

    /**
     * Add a byte to this array.
     * @param value The value to add.
     */
    public void add(byte value) {
        ensureCapacity(1);
        this.array[this.length++] = value;
    }

    private void ensureCapacity(int amount) {
        if (!this.allowGrowth)
            return;

        int newSize = this.array.length;
        while (this.length + amount > newSize)
            newSize = newSize > 0 ? newSize * 2 : 1;

        if (newSize > this.array.length)
            resize(newSize);
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