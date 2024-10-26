package net.highwayfrogs.editor.utils.fx.wrapper;

import javafx.collections.ObservableIntegerArray;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.util.Arrays;

/**
 * Represents an array of integer values wrapped around a JavaFX ObservableIntegerArray.
 * The purpose is to allow array operations that are not possible in the original.
 * This class is mostly a copy of ObservableIntegerArrayImpl (Licensed under GPL) enhanced with techniques developed for ModToolFramework.
 * This file's code is likewise licensed under GPLv2, however I was unable to find the GPLv2 license in the original JavaFX code. The JavaFX website states that it is licensed under GPLv2.
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Created by Kneesnap on 12/26/2023.
 */
public class FXIntArray {
    private int length;
    private int[] array;

    private static final int[] EMPTY_ARRAY = new int[0];

    public FXIntArray() {
        this.length = 0;
        this.array = EMPTY_ARRAY;
    }

    public FXIntArray(ObservableIntegerArray array) {
        this.length = array != null ? array.size() : 0;
        this.array = this.length > 0 ? new int[this.length] : EMPTY_ARRAY;
        if (array != null)
            this.array = array.toArray(this.array);
    }

    /**
     * Applies the array contents to an ObservableFXArray
     * @param array The array to apply the contents to.
     */
    public void apply(ObservableIntegerArray array) {
        array.setAll(this.array, 0, this.length);
    }

    /**
     * Get the number of integers currently tracked in the array.
     */
    public int size() {
        return this.length;
    }

    /**
     * Get the capacity (number of element slots) available in the currently allocated array.
     */
    public int getCapacity() {
        return this.array != null ? this.array.length : 0;
    }

    /**
     * Sets new length of data in this array. This method grows capacity
     * if necessary but never shrinks it. Resulting array will contain existing
     * data for indexes that are less than the current size and zeroes for
     * indexes that are greater than the current size.
     * @param size new length of data in this array
     * @throws NegativeArraySizeException if size is negative
     */
    public void resize(int size) {
        if (size < 0)
            throw new NegativeArraySizeException("The provided size was negative (" + size + ")");

        ensureCapacity(size);
        if (this.length > size)
            this.length = size;
    }

    /**
     * Grows the capacity of this array if the currently unused capacity is less than
     * given {@code numberOfElements}, otherwise doing nothing.
     * @param numberOfElements The number of additional elements to ensure can be held.
     */
    public void growCapacityIfNecessary(int numberOfElements) {
        int currentCapacity = getCapacity();
        int neededCapacity = size() + numberOfElements;
        while (neededCapacity > currentCapacity) {
            if (currentCapacity > 0) {
                currentCapacity *= 2;
            } else {
                currentCapacity = 1;
            }
        }

        ensureCapacity(currentCapacity);
    }

    /**
     * Grows the capacity of this array if the current capacity is less than
     * given {@code capacity}, does nothing if it already exceeds
     * the {@code capacity}.
     * @param capacity The array size to expand to.
     */
    public void ensureCapacity(int capacity) {
        if (this.array.length < capacity)
            this.array = Arrays.copyOf(this.array, capacity);
    }

    /**
     * Shrinks the capacity to the current size of data in the array.
     */
    public void trimToSize() {
        if (this.array.length == this.length)
            return; // Capacity matches usage.

        int[] newArray = new int[this.length];
        System.arraycopy(this.array, 0, newArray, 0, this.length);
        this.array = newArray;
    }

    /**
     * Empties the array by resizing it to 0. Capacity is not changed.
     * @see #trimToSize()
     */
    public void clear() {
        this.length = 0;
    }

    /**
     * Copies specified portion of array into {@code dest} array. Throws
     * the same exceptions as {@link System#arraycopy(java.lang.Object,
     * int, java.lang.Object, int, int) System.arraycopy()} method.
     * @param srcIndex  starting position in the observable array
     * @param dest      destination array
     * @param destIndex starting position in destination array
     * @param length    length of portion to copy
     */
    public void copyTo(int srcIndex, int[] dest, int destIndex, int length) {
        rangeCheck(srcIndex + length); // While the array may contain these elements, it should not be possible to access them to avoid bugs.
        System.arraycopy(this.array, srcIndex, dest, destIndex, length);
    }

    /**
     * Copies specified portion of array into {@code dest} observable array.
     * Throws the same exceptions as {@link System#arraycopy(java.lang.Object,
     * int, java.lang.Object, int, int) System.arraycopy()} method.
     * @param srcIndex  starting position in the observable array
     * @param dest      destination observable array
     * @param destIndex starting position in destination observable array
     * @param length    length of portion to copy
     */
    public void copyTo(int srcIndex, ObservableIntegerArray dest, int destIndex, int length) {
        rangeCheck(srcIndex + length);
        dest.set(destIndex, this.array, srcIndex, length);
    }

    /**
     * Gets a single value of array. This is generally as fast as direct access
     * to an array and eliminates necessity to make a copy of array.
     * @param index index of element to get
     * @return value at the given index
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside array bounds
     */
    public int get(int index) {
        rangeCheck(index + 1);
        return this.array[index];
    }

    /**
     * Sets a single value in the array. Avoid using this method if many values
     * are updated, use {@linkplain #set(int, int[], int, int)} update method
     * instead with as minimum number of invocations as possible.
     * @param index index of the value to set
     * @param value new value for the given index
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside
     *                                        array bounds
     */
    public void set(int index, int value) {
        if (index == this.length) {
            this.add(value);
        } else {
            rangeCheck(index + 1);
            this.array[index] = value;
        }
    }

    /**
     * Adds a single value to the end of the array
     * Functions regardless of batching state.
     * @param value new value to append to the array
     */
    public void add(int value) {
        growCapacityIfNecessary(1);
        this.array[this.length++] = value;
    }

    /**
     * Adds a single value to the provided index
     * @param index index of the value to insert
     * @param value new value to append to the array
     */
    public void add(int index, int value) {
        growCapacityIfNecessary(1);
        rangeCheck(index);

        // Shift all elements to make room for the inserted ones.
        int shiftedElements = Math.max(0, this.length - index - 1);
        if (shiftedElements > 0)
            System.arraycopy(this.array, index, this.array, index + 1, shiftedElements);

        this.array[this.length++] = value;
    }

    /**
     * Removes a single value from the provided index.
     * @param index index of the value to remove
     * @return removed value
     */
    public int remove(int index) {
        if (index < 0 || index >= this.length)
            throw new ArrayIndexOutOfBoundsException("Cannot remove invalid index " + index + ", valid indices are within the range [0, " + this.length + ").");

        int removedValue = this.array[index];

        // Shift all elements.
        int shiftedElements = Math.max(0, this.length - index - 1);
        if (shiftedElements > 0)
            System.arraycopy(this.array, index + 1, this.array, index, shiftedElements);

        this.length--;
        return removedValue;
    }

    /**
     * Remove a number of values starting at the provided index.
     * @param startIndex index to remove values from
     * @param amount     amount of elements to remove
     */
    public void remove(int startIndex, int amount) {
        ensureCapacity(startIndex + amount);
        if (amount < 0) {
            throw new IllegalArgumentException("The amount of values to remove was negative (" + amount + ")");
        } else if (amount == 0) {
            return; // Nothing to remove.
        }

        // Shift all elements to make room for the inserted ones.
        int shiftedElements = Math.max(0, this.length - amount);
        if (shiftedElements > 0)
            System.arraycopy(this.array, startIndex + amount, this.array, startIndex, shiftedElements);

        // Reduce the length of the array.
        this.length -= amount;
    }

    /**
     * Inserts values into the array.
     * @param indices A sorted array of integer indices.
     * @param values  An array of values, the size should match.
     */
    public void insertValues(FXIntArray indices, FXIntArray values) {
        // Ensure the number of values matches the number of indices.
        if (indices.size() != values.size())
            throw new IllegalStateException("There were " + indices.size() + " indices corresponding to " + values.size() + " values.");

        // Abort if there aren't any values to add.
        int valueCount = values.size();
        if (valueCount == 0)
            return;

        // Ensure the index array is sorted.
        int lastIndex = -1;
        for (int i = 0; i < indices.size(); i++) {
            int currentIndex = indices.get(i);
            if (currentIndex < lastIndex)
                throw new IllegalArgumentException("The indices array was not sorted! [" + Arrays.toString(indices.toArray(null)) + "]");
        }

        // Ensure enough room exists for the inserted values.
        growCapacityIfNecessary(valueCount);

        // Shift the array elements and add in the new values to their slots.
        int lastInsertionIndex = this.length;
        for (int i = indices.size() - 1; i >= 0; i--) {
            int insertionIndex = indices.get(i);

            // Shift old values.
            int copyAmount = (lastInsertionIndex - insertionIndex);
            if (copyAmount > 0)
                System.arraycopy(this.array, insertionIndex, this.array, insertionIndex + i + 1, copyAmount);

            // Write inserted value
            this.array[insertionIndex + i] = values.get(i);

            // Prepare for next.
            lastInsertionIndex = insertionIndex;
        }

        // Increase size of array.
        this.length += valueCount;
    }

    /**
     * Removes all indices set in the bit array.
     * @param indices The indices to remove from this array.
     */
    public void removeIndices(IndexBitArray indices) {
        if (indices == null)
            throw new NullPointerException("indices");

        // Remove Elements.
        int totalIndexCount = indices.getBitCount();
        int removedGroups = 0;
        int currentIndex = indices.getFirstBitIndex();
        for (int i = 0; i < totalIndexCount; i++) {
            int nextIndex = ((totalIndexCount > i + 1) ? indices.getNextBitIndex(currentIndex) : this.length - 1);
            int copyLength = (nextIndex - currentIndex);

            // If copy length is 0, that means there is a duplicate index (impossible), and we can just safely skip it.
            if (copyLength > 0) {
                int removeIndex = currentIndex - removedGroups; // This works because TestRemovals is sorted.
                removedGroups++;
                System.arraycopy(this.array, removeIndex + removedGroups, this.array, removeIndex, copyLength);
            } else if (i == totalIndexCount - 1) { // Edge-case, if we're at the end of the array, copyLength can be zero because there would be nothing to copy. This should still be counted as a removal.
                removedGroups++;
            }

            currentIndex = nextIndex;
        }

        this.length -= totalIndexCount;
    }

    private void addAllInternal(int destIndex, int[] src, int srcIndex, int length) {
        growCapacityIfNecessary(length);

        // Shift all elements to make room for the inserted ones.
        int shiftedElements = Math.max(0, this.length - destIndex);
        if (shiftedElements > 0)
            System.arraycopy(this.array, destIndex, this.array, destIndex + length, shiftedElements);

        System.arraycopy(src, srcIndex, this.array, destIndex, length);
        this.length += length;
    }

    /**
     * Appends given {@code elements} to the end of this array. Capacity is increased
     * if necessary to match the new size of the data.
     * @param elements elements to append
     */
    public void addAll(int... elements) {
        addAllInternal(this.length, elements, 0, elements.length);
    }

    /**
     * Inserts given {@code elements} to the provided array index.
     * Capacity is increased if necessary to match the new size of the data.
     * @param destIndex index of the elements to insert
     * @param elements  elements to insert
     */
    public void addAll(int destIndex, int... elements) {
        rangeCheck(destIndex);
        addAllInternal(destIndex, elements, 0, elements.length);
    }

    /**
     * Appends a portion of given array to the end of this array.
     * Capacity is increased if necessary to match the new size of the data.
     * @param src      source array
     * @param srcIndex starting position in source array
     * @param length   length of portion to append
     */
    public void addAll(int[] src, int srcIndex, int length) {
        rangeCheck(src, srcIndex, length);
        addAllInternal(this.length, src, srcIndex, length);
    }

    /**
     * Appends a portion of given array to the target array index.
     * Capacity is increased if necessary to match the new size of the data.
     * @param destIndex index of the values to insert
     * @param src       source array
     * @param srcIndex  starting position in source array
     * @param length    length of portion to append
     */
    public void addAll(int destIndex, int[] src, int srcIndex, int length) {
        rangeCheck(destIndex);
        rangeCheck(src, srcIndex, length);
        addAllInternal(destIndex, src, srcIndex, length);
    }

    /**
     * Copies a portion of specified array into this observable array. Throws
     * the same exceptions as {@link System#arraycopy(java.lang.Object,
     * int, java.lang.Object, int, int) System.arraycopy()} method.
     * @param destIndex the starting destination position in this observable array
     * @param src       source array to copy
     * @param srcIndex  starting position in source array
     * @param length    length of portion to copy
     */
    public void set(int destIndex, int[] src, int srcIndex, int length) {
        rangeCheck(destIndex + length);
        System.arraycopy(src, srcIndex, array, destIndex, length);
    }

    /**
     * Returns an array containing copy of the observable array.
     * @return an int array containing the copy of the observable array
     */
    public int[] toArray() {
        return toArray(null);
    }

    /**
     * Returns an array containing copy of the observable array.
     * If the observable array fits in the specified array, it is copied therein.
     * Otherwise, a new array is allocated with the size of the observable array.
     * @param dest the array into which the observable array to be copied,
     *             if it is big enough; otherwise, a new int array is allocated.
     *             Ignored, if null.
     * @return an int array containing the copy of the observable array
     */
    public int[] toArray(int[] dest) {
        if ((dest == null) || (this.length > dest.length))
            dest = new int[this.length];

        System.arraycopy(this.array, 0, dest, 0, this.length);
        return dest;
    }

    /**
     * Returns an array containing copy of specified portion of the observable array.
     * If specified portion of the observable array fits in the specified array,
     * it is copied therein. Otherwise, a new array of given length is allocated.
     * @param srcIndex starting position in the observable array
     * @param dest     the array into which specified portion of the observable array
     *                 to be copied, if it is big enough;
     *                 otherwise, a new int array is allocated.
     *                 Ignored, if null.
     * @param length   length of portion to copy
     * @return an int array containing the copy of specified portion the observable array
     */
    public int[] toArray(int srcIndex, int[] dest, int length) {
        rangeCheck(srcIndex + length);
        if ((dest == null) || (length > dest.length))
            dest = new int[length];

        System.arraycopy(this.array, srcIndex, dest, 0, length);
        return dest;
    }

    /**
     * Performs a binary search to find the key.
     * This method assumes the array is sorted lowest to highest.
     * @param key The key to search for.
     * @return the index of the value, or -(insertion point + 1) for the index to insert the key.
     */
    public int binarySearch(int key) {
        return Arrays.binarySearch(this.array, 0, this.length, key);
    }

    /**
     * Gets the position which a value should be inserted, if this array is sorted.
     * @param value value to calculate an insertion point from
     * @return insertionIndex
     */
    public int getInsertionPoint(int value) {
        int searchIndex = this.binarySearch(value);

        if (searchIndex >= 0) {
            int insertionIndex = searchIndex + 1;

            // We want to calculate the insertion index that places the element at the end of the sequence of values that share the same index.
            while (this.length > insertionIndex && this.array[insertionIndex] == value)
                insertionIndex++;

            return insertionIndex;
        } else { // Negative (Insertion index can be obtained from math)
            return -(searchIndex + 1);
        }
    }

    private void rangeCheck(int size) {
        if (size < 0)
            throw new ArrayIndexOutOfBoundsException("Cannot access elements before the start of the FXIntArray. (Index: " + size + ")");
        if (size > this.length)
            throw new ArrayIndexOutOfBoundsException("Cannot access elements after the end of the FXIntArray. (Length: " + this.length + ", Accessed: " + size + ")");
    }

    private void rangeCheck(int[] src, int srcIndex, int length) {
        if (src == null)
            throw new NullPointerException("The source array is null");

        if (srcIndex < 0 || srcIndex + length > src.length)
            throw new ArrayIndexOutOfBoundsException(src.length);
        if (length < 0)
            throw new ArrayIndexOutOfBoundsException(-1);
    }
}