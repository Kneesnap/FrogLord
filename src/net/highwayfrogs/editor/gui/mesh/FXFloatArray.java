package net.highwayfrogs.editor.gui.mesh;

import javafx.collections.ObservableFloatArray;
import net.highwayfrogs.editor.utils.IndexBitArray;

import java.util.Arrays;

/**
 * Represents an array of floating point values wrapped around a JavaFX ObservableFloatArray.
 * The purpose is to allow array operations that are not possible in the original.
 * It also enables bulking changes together before updating JavaFX.
 * ObservableFloatArray documentation says that optimal performance is obtained with the fewest number of method calls possible, so this also yields better performance.
 * This class is mostly a copy of ObservableFloatArrayImpl (Licensed under GPL) enhanced with techniques developed for ModToolFramework.
 * This file's code is likewise licensed under GPLv2, however I was unable to find the GPLv2 license in the original JavaFX code. The JavaFX website states that it is licensed under GPLv2.
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * Created by Kneesnap on 12/26/2023.
 */
public class FXFloatArray {
    private int length;
    private float[] array;
    private IndexBitArray bulkRemovedIndices;
    private int bulkRemovalStackCount;

    private static final float[] EMPTY_ARRAY = new float[0];

    public FXFloatArray() {
        this.length = 0;
        this.array = EMPTY_ARRAY;
    }

    public FXFloatArray(ObservableFloatArray array) {
        this.length = array != null ? array.size() : 0;
        this.array = this.length > 0 ? new float[this.length] : EMPTY_ARRAY;
        if (array != null)
            this.array = array.toArray(this.array);
    }

    /**
     * Applies the array contents to an ObservableFXArray
     * @param array The array to apply the contents to.
     */
    public void apply(ObservableFloatArray array) {
        if (isBulkRemovalEnabled())
            throw new IllegalStateException("Cannot apply to ObservableArray while bulk removal mode is enabled.");

        array.setAll(this.array, 0, this.length);
    }

    /**
     * Get the number of floats currently tracked in the array.
     */
    public int getLength() {
        return this.length;
    }

    /**
     * Get the number of floats currently tracked in the array.
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
        int neededCapacity = getLength() + numberOfElements;
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

        float[] newArray = new float[this.length];
        System.arraycopy(this.array, 0, newArray, 0, this.length);
        this.array = newArray;
    }

    /**
     * Empties the array by resizing it to 0. Capacity is not changed.
     * @see #trimToSize()
     */
    public void clear() {
        this.length = 0;
        if (this.bulkRemovedIndices != null)
            this.bulkRemovedIndices.clear();

        if (this.bulkRemovalStackCount != 0)
            throw new IllegalStateException("Cleared the array while bulk removal mode was enabled! (" + this.bulkRemovalStackCount + ")");
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
    public void copyTo(int srcIndex, float[] dest, int destIndex, int length) {
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
    public void copyTo(int srcIndex, ObservableFloatArray dest, int destIndex, int length) {
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
    public float get(int index) {
        rangeCheck(index + 1);
        return this.array[index];
    }

    /**
     * Sets a single value in the array. Avoid using this method if many values
     * are updated, use {@linkplain #set(int, float[], int, int)} update method
     * instead with as minimum number of invocations as possible.
     * @param index index of the value to set
     * @param value new value for the given index
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside
     *                                        array bounds
     */
    public void set(int index, float value) {
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
    public void add(float value) {
        growCapacityIfNecessary(1);
        this.array[this.length++] = value;
    }

    /**
     * Adds a single value to the provided index
     * @param index index of the value to insert
     * @param value new value to append to the array
     */
    public void add(int index, float value) {
        growCapacityIfNecessary(1);
        rangeCheck(index);

        // Shift all elements to make room for the inserted ones.
        int shiftedElements = Math.max(0, this.length - 1);
        if (shiftedElements > 0)
            System.arraycopy(this.array, index, this.array, index + 1, shiftedElements);

        this.array[this.length++] = value;
    }

    /**
     * Returns whether bulk removal mode is currently enabled.
     */
    public boolean isBulkRemovalEnabled() {
        return this.bulkRemovalStackCount > 0;
    }

    /**
     * Enables a performance boost to removals by bulking them together in a single remove.
     */
    public void startBulkRemovals() {
        this.bulkRemovalStackCount++;
    }

    /**
     * Completes bulked removal operations
     */
    public void endBulkRemovals() {
        if (--this.bulkRemovalStackCount == 0) {
            // Perform bulk removals.
            if (this.bulkRemovedIndices != null) {
                removeIndices(this.bulkRemovedIndices);
                this.bulkRemovedIndices.clear();
            }
        } else if (this.bulkRemovalStackCount < 0) {
            this.bulkRemovalStackCount = 0;
            throw new IllegalStateException("startBulkRemovals() was called fewer times than endBulkRemovals() was called.");
        }
    }

    /**
     * Removes a single value from the provided index.
     * If bulk removal is enabled, these will be skipped.
     * @param index index of the value to remove
     * @return removed value
     */
    public float remove(int index) {
        if (index < 0 || index >= this.length)
            throw new ArrayIndexOutOfBoundsException("Cannot remove invalid index " + index + ", valid indices are within the range [0, " + this.length + ").");

        float removedValue = this.array[index];

        if (isBulkRemovalEnabled()) {
            if (this.bulkRemovedIndices == null)
                this.bulkRemovedIndices = new IndexBitArray();

            // Mark the index for removal.
            this.bulkRemovedIndices.setBit(index, true);
        } else {
            // Shift all elements.
            int shiftedElements = Math.max(0, this.length - index - 1);
            if (shiftedElements > 0)
                System.arraycopy(this.array, index + 1, this.array, index, shiftedElements);

            this.length--;
        }

        return removedValue;
    }

    /**
     * Remove a number of values starting at the provided index.
     * If bulk removal mode is enabled, the removal will be queued.
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

        // Track bulk removal instead.
        if (isBulkRemovalEnabled()) {
            if (this.bulkRemovedIndices == null)
                this.bulkRemovedIndices = new IndexBitArray();

            this.bulkRemovedIndices.setBits(startIndex, amount, true);
            return;
        }

        // Shift all elements to make room for the inserted ones.
        int shiftedElements = Math.max(0, this.length - amount);
        if (shiftedElements > 0)
            System.arraycopy(this.array, startIndex + amount, this.array, startIndex, shiftedElements);

        // Reduce the length of the array.
        this.length -= amount;
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
            int nextIndex = ((totalIndexCount > i + 1) ? indices.getNextBitIndex(currentIndex) : this.length - 1) - removedGroups;
            int removeIndex = currentIndex - removedGroups; // This works because TestRemovals is sorted.
            int copyLength = (nextIndex - removeIndex);

            // If copy length is 0, that means there is a duplicate index (impossible), and we can just safely skip it.
            if (copyLength > 0) {
                removedGroups++;
                System.arraycopy(this.array, removeIndex + removedGroups, this.array, removeIndex, copyLength);
            } else if (i == totalIndexCount - 1) { // Edge-case, if we're at the end of the array, copyLength can be zero because there would be nothing to copy. This should still be counted as a removal.
                removedGroups++;
            }

            currentIndex = nextIndex;
        }

        this.length -= totalIndexCount;
    }

    private void addAllInternal(int destIndex, float[] src, int srcIndex, int length) {
        growCapacityIfNecessary(length);

        // Shift all elements to make room for the inserted ones.
        int shiftedElements = Math.max(0, this.length - destIndex);
        if (shiftedElements > 0)
            System.arraycopy(this.array, destIndex, this.array, destIndex + length, shiftedElements);

        System.arraycopy(src, srcIndex, this.array, this.length, length);
        this.length += length;
    }

    /**
     * Appends given {@code elements} to the end of this array. Capacity is increased
     * if necessary to match the new size of the data.
     * @param elements elements to append
     */
    public void addAll(float... elements) {
        addAllInternal(this.length, elements, 0, elements.length);
    }

    /**
     * Inserts given {@code elements} to the provided array index.
     * Capacity is increased if necessary to match the new size of the data.
     * @param destIndex index of the elements to insert
     * @param elements  elements to insert
     */
    public void addAll(int destIndex, float... elements) {
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
    public void addAll(float[] src, int srcIndex, int length) {
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
    public void addAll(int destIndex, float[] src, int srcIndex, int length) {
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
    public void set(int destIndex, float[] src, int srcIndex, int length) {
        rangeCheck(destIndex + length);
        System.arraycopy(src, srcIndex, array, destIndex, length);
    }

    /**
     * Returns an array containing copy of the observable array.
     * If the observable array fits in the specified array, it is copied therein.
     * Otherwise, a new array is allocated with the size of the observable array.
     * @param dest the array into which the observable array to be copied,
     *             if it is big enough; otherwise, a new float array is allocated.
     *             Ignored, if null.
     * @return a float array containing the copy of the observable array
     */
    public float[] toArray(float[] dest) {
        if ((dest == null) || (this.length > dest.length))
            dest = new float[this.length];

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
     *                 otherwise, a new float array is allocated.
     *                 Ignored, if null.
     * @param length   length of portion to copy
     * @return a float array containing the copy of specified portion the observable array
     */
    public float[] toArray(int srcIndex, float[] dest, int length) {
        rangeCheck(srcIndex + length);
        if ((dest == null) || (length > dest.length))
            dest = new float[length];

        System.arraycopy(this.array, srcIndex, dest, 0, length);
        return dest;
    }

    private void rangeCheck(int size) {
        if (size > this.length)
            throw new ArrayIndexOutOfBoundsException("Cannot access elements after the end of the FXFloatArray. (Length: " + this.length + ", Accessed: " + size + ")");
    }

    private void rangeCheck(float[] src, int srcIndex, int length) {
        if (src == null)
            throw new NullPointerException("The source array is null");

        if (srcIndex < 0 || srcIndex + length > src.length)
            throw new ArrayIndexOutOfBoundsException(src.length);
        if (length < 0)
            throw new ArrayIndexOutOfBoundsException(-1);
    }
}
