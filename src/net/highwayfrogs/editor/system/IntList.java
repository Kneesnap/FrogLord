package net.highwayfrogs.editor.system;

import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.util.Arrays;

/**
 * A replacement for List<Integer> which avoids auto-boxing.
 * Created by Kneesnap on 10/5/2018.
 */
public class IntList {
    private int[] array;
    private int size;

    public IntList() {
        this(8);
    }

    public IntList(int startSize) {
        this.array = new int[startSize];
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public boolean contains(int value) {
        return indexOf(value) != -1;
    }

    public boolean isArrayFull() {
        return this.size >= this.array.length;
    }

    public boolean add(int value) {
        if (isArrayFull())
            resize(this.array.length * 2);

        this.array[this.size++] = value;
        return false;
    }

    public boolean add(int index, int value) {
        if (isArrayFull())
            resize(this.array.length * 2);

        if (this.size == index) {
            add(value);
        } else {
            System.arraycopy(this.array, index, this.array, index + 1, this.size - index);
            this.array[index] = value;
            this.size++;
        }

        return false;
    }

    private void resize(int newSize) {
        this.array = Arrays.copyOf(array, newSize);
    }

    public int remove(int index) {
        int val = get(index);
        System.arraycopy(array, index + 1, array, index, array.length - (index + 1));
        this.size--;
        return val;
    }

    // Copied from IndexBitArray.removeValuesFromList()
    public void removeIndices(IndexBitArray indices) {
        if (indices == null)
            throw new NullPointerException("indices");

        // Remove Elements.
        int totalIndexCount = indices.getBitCount();
        int removedGroups = 0;
        int currentIndex = indices.getFirstBitIndex();
        for (int i = 0; i < totalIndexCount; i++) {
            int nextIndex = ((totalIndexCount > i + 1) ? indices.getNextBitIndex(currentIndex) : size() - 1);
            int copyLength = (nextIndex - currentIndex);

            // If copy length is 0, that means there is a duplicate index (impossible), and we can just safely skip it.
            if (copyLength > 0) {
                int removeIndex = currentIndex - removedGroups; // This works because TestRemovals is sorted.
                removedGroups++;

                // System.arraycopy(src: list, srcPos: removeIndex + removedGroups, dst: list, dstPos: removeIndex, copyLength);
                for (int j = 0; j < copyLength; j++)
                    set(removeIndex + j, get(removeIndex + removedGroups + j));
            } else if (i == totalIndexCount - 1) { // Edge-case, if we're at the end of the array, copyLength can be zero because there would be nothing to copy. This should still be counted as a removal.
                removedGroups++;
            }

            currentIndex = nextIndex;
        }

        // Remove invalid elements from the end.
        while (totalIndexCount-- > 0)
            remove(size() - 1);
    }

    public void clear() {
        this.size = 0;
    }

    public int get(int index) {
        if (index < 0 || index >= this.size) // Too high use to call Utils.verify.
            throw new ArrayIndexOutOfBoundsException(index);
        return array[index];
    }

    public int set(int index, int newValue) {
        int val = get(index);
        array[index] = newValue;
        return val;
    }

    public int indexOf(int value) {
        for (int i = 0; i < this.size; i++)
            if (this.array[i] == value)
                return i;
        return -1;
    }

    public int[] getArray() {
        return Arrays.copyOf(array, this.size);
    }

    public int[] getInternalArray() {
        return this.array;
    }
}
