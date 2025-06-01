package net.highwayfrogs.editor.utils.fx.wrapper;

import javafx.collections.ObservableIntegerArray;
import lombok.Getter;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;
import net.highwayfrogs.editor.utils.objects.IntegerCounter;

/**
 * Batches insert/remove array operations for a FXIntArray into single operations to increase performance.
 * ObservableIntegerArray documentation says that optimal performance is obtained with the fewest number of method calls possible.
 * So, we use a cached array that we write to, allowing our changes to be handled once they are all complete, instead of once after every single change.
 * Additionally, we allow bulking changes together to allow for more efficient array operations.
 * Created by Kneesnap on 12/28/2023.
 */
public class FXIntArrayBatcher {
    @Getter private final FXIntArray array;
    @Getter private final ObservableIntegerArray fxArray;
    private final IntegerCounter batchedUpdates;
    private final IntegerCounter batchedInsertion;
    private final FXIntArray queuedInsertionIndices; // The destination indices to paste each of the queued insertions
    private final FXIntArray queuedInsertionSourceSizes; // The size of each queued insertion
    private final FXIntArray queuedInsertionSourceIndices; // The indices into the data buffer where the insertion data can be found.
    private final FXIntArray queuedInsertionSourceDataBuffer; // The buffer containing the insertion data.
    private final IntegerCounter batchedRemovals;
    private final IndexBitArray queuedIndexRemovals;
    private boolean updateOnBatchCompletion;

    public FXIntArrayBatcher(FXIntArray array, ObservableIntegerArray fxArray) {
        this.array = array;
        this.fxArray = fxArray;
        this.batchedUpdates = new IntegerCounter();
        this.batchedInsertion = new IntegerCounter();
        this.queuedInsertionIndices = new FXIntArray();
        this.queuedInsertionSourceSizes = new FXIntArray();
        this.queuedInsertionSourceIndices = new FXIntArray();
        this.queuedInsertionSourceDataBuffer = new FXIntArray();
        this.batchedRemovals = new IntegerCounter();
        this.queuedIndexRemovals = new IndexBitArray();
    }

    /**
     * Test if any batch mode is enabled.
     */
    public boolean isAnyBatchModeEnabled() {
        return isBatchRemovalActive() || isBatchInsertionActive() || isBatchingUpdatesActive();
    }

    /**
     * Apply to the wrapped JavaFX array.
     * If operations are currently batched, the update will occur once batched operations are finished.
     * @return true if the array was actually updated (as opposed to batched updates still pending)
     */
    public boolean applyToFxArray() {
        if (isAnyBatchModeEnabled()) {
            this.updateOnBatchCompletion = true;
            return false;
        } else {
            forceApplyToFxArray();
            return true;
        }
    }

    /**
     * Force-applies to the wrapped JavaFX array, regardless of if batch updating is enabled or not.
     */
    public void forceApplyToFxArray() {
        applyBatchInsertions();
        applyBatchRemovals();

        this.array.apply(this.fxArray);
        this.updateOnBatchCompletion = false;
    }

    private boolean applyToFxArrayIfNecessary() {
        return this.updateOnBatchCompletion && applyToFxArray();
    }

    /**
     * Clear all batched data.
     */
    public void clear() {
        if (this.batchedUpdates.isActive())
            throw new IllegalStateException("Cleared the array while batch update mode was enabled! (" + this.batchedUpdates.getCounter() + ")");
        if (this.batchedInsertion.isActive())
            throw new IllegalStateException("Cleared the array while batch insertion mode was enabled! (" + this.batchedInsertion.getCounter() + ")");
        if (this.batchedRemovals.isActive())
            throw new IllegalStateException("Cleared the array while batch removal mode was enabled! (" + this.batchedRemovals.getCounter() + ")");

        this.queuedInsertionIndices.clear();
        this.queuedInsertionSourceSizes.clear();
        this.queuedInsertionSourceIndices.clear();
        this.queuedInsertionSourceDataBuffer.clear();
        this.queuedIndexRemovals.clear();
        this.updateOnBatchCompletion = false;
    }

    /**
     * Check if the batching of updates is currently enabled.
     * This will not delay any operations applying to the staging array, but it will delay updates to the underlying FX array.
     */
    public boolean isBatchingUpdatesActive() {
        return this.batchedUpdates.isActive();
    }

    /**
     * Indicate the start of batching updates, increasing performance by updating fewer times.
     */
    public void startBatchingUpdates() {
        this.batchedUpdates.increment();
    }

    /**
     * Indicate the end of batching array updates, updating the mesh array if values have changed.
     * @return true if the array was actually updated (as opposed to batched updates still pending)
     */
    public boolean endBatchingUpdates() {
        // Check if batch updates are still active.
        if (!this.batchedUpdates.decrement())
            return false;

        // Don't update the mesh array unless an update was queued.
        return applyToFxArrayIfNecessary();
    }

    /**
     * Check if batch array insertion is currently enabled.
     */
    public boolean isBatchInsertionActive() {
        return this.batchedInsertion.isActive();
    }

    /**
     * Indicate the start of batch value insertion, increasing performance by chunking array insertions together.
     */
    public void startBatchInsertion() {
        this.batchedInsertion.increment();
    }

    /**
     * Indicate the end of batch value insertion, updating the mesh array if values have changed.
     * @return true if the array was actually updated (as opposed to batched updates still pending)
     */
    public boolean endBatchInsertion() {
        // Check if batch insertion is still active.
        if (!this.batchedInsertion.decrement())
            return false;

        if (applyBatchInsertions()) {
            return applyToFxArray(); // Ensure the array is updated, whether now or later.
        } else {
            return applyToFxArrayIfNecessary(); // We've exited a batch mode, so ensure the array gets updated if necessary.
        }
    }

    private boolean applyBatchInsertions() {
        // Ensure the number of values matches the number of indices.
        if (this.queuedInsertionIndices.size() != this.queuedInsertionSourceIndices.size())
            throw new IllegalStateException("There were " + this.queuedInsertionIndices.size() + " indices corresponding to " + this.queuedInsertionSourceIndices.size() + " buffer index values.");
        if (this.queuedInsertionIndices.size() != this.queuedInsertionSourceSizes.size())
            throw new IllegalStateException("There were " + this.queuedInsertionIndices.size() + " indices corresponding to " + this.queuedInsertionSourceSizes.size() + " size values.");

        // Abort if there aren't any values to insert.
        if (this.queuedInsertionIndices.size() == 0)
            return false;

        // Shift the array elements and insert in the new values to their slots.
        // This relies upon the array being sorted.
        this.array.insertValues(this.queuedInsertionIndices, this.queuedInsertionSourceIndices, this.queuedInsertionSourceSizes, this.queuedInsertionSourceDataBuffer);

        // Execute hook
        if (this.queuedInsertionIndices.size() == 1) {
            // Range insertions are more efficient to handle, so we'd like to call them when possible.
            onRangeInsertionComplete(this.queuedInsertionIndices.get(0), this.queuedInsertionSourceSizes.get(0));
        } else {
            onBatchInsertionComplete(this.queuedInsertionIndices, this.queuedInsertionSourceSizes);
        }

        // Clear for future.
        this.queuedInsertionIndices.clear();
        this.queuedInsertionSourceIndices.clear();
        this.queuedInsertionSourceSizes.clear();
        this.queuedInsertionSourceDataBuffer.clear();
        return true;
    }

    /**
     * Called when batch insertion is completed.
     * @param insertionIndices The indices of the values inserted.
     * @param insertionLengths An array of indices representing the amount of data to insert from the data buffer. Each array element must correspond to the corresponding destination index in the insertionIndices array.
     */
    protected void onBatchInsertionComplete(FXIntArray insertionIndices, FXIntArray insertionLengths) {
        int totalInsertedElementCount = insertionLengths.sum();

        // Update the queued removals.
        int remainingInsertions = insertionLengths.size();
        int remainingInsertionElements = totalInsertedElementCount;
        int removeIndex = this.queuedIndexRemovals.getLastBitIndex();
        for (int i = this.queuedIndexRemovals.getBitCount(); i > 0; i--) {
            // Stop accounting for insertions which we've passed.
            while (remainingInsertions > 0 && insertionIndices.get(remainingInsertions - 1) > removeIndex) {
                remainingInsertionElements -= insertionLengths.get(remainingInsertions - 1);
                remainingInsertions--;
            }

            if (remainingInsertions == 0)
                break; // All remaining indices are not impacted by the insertions, so we can stop.

            // Update the removed indices, moving the indices forward that need to be moved forward.
            // Careful, this code modifies the array we're iterating through, it should only edit parts of the array we're done iterating through.
            this.queuedIndexRemovals.setBit(removeIndex + remainingInsertionElements, true);
            this.queuedIndexRemovals.setBit(removeIndex, false);

            // Find the next removed index we plan to edit. (Which happens to come earlier in the array)
            removeIndex = this.queuedIndexRemovals.getPreviousBitIndex(removeIndex);
        }
    }

    /**
     * Called when insertion of a range of values is completed.
     * @param startIndex The index which values are inserted starting from.
     * @param amount     The number of elements inserted.
     */
    protected void onRangeInsertionComplete(int startIndex, int amount) {
        int removeIndex = this.queuedIndexRemovals.getLastBitIndex();
        for (int i = this.queuedIndexRemovals.getBitCount(); i > 0; i--) {
            if (removeIndex < startIndex)
                break; // Reached an index which is not affected by the insertion.

            // Update the removed indices, moving the indices forward that need to be moved forward.
            // Careful, this code modifies the array we're iterating through, it should only edit parts of the array we're done iterating through.
            this.queuedIndexRemovals.setBit(removeIndex, false);
            this.queuedIndexRemovals.setBit(removeIndex + amount, true);

            // Find the next removed index we plan to edit. (Which happens to come earlier in the array)
            removeIndex = this.queuedIndexRemovals.getPreviousBitIndex(removeIndex);
        }
    }

    /**
     * Test if the given index is queued for removal.
     * @param index the index to test
     * @return queuedForRemoval
     */
    public boolean isQueuedForRemoval(int index) {
        return this.queuedIndexRemovals.getBit(index);
    }

    /**
     * Check if batch removal of array values is currently enabled.
     */
    public boolean isBatchRemovalActive() {
        return this.batchedRemovals.isActive();
    }

    /**
     * Indicate the start of batch removals, increasing performance by chunking array removals together.
     */
    public void startBatchRemoval() {
        this.batchedRemovals.increment();
    }

    /**
     * Indicate the end of batch value removals, updating the mesh array if values have changed.
     * @return true if the array was actually updated (as opposed to batched updates still pending)
     */
    public boolean endBatchRemoval() {
        // Check if batch removals are still active.
        if (!this.batchedRemovals.decrement())
            return false;

        if (applyBatchRemovals()) {
            return applyToFxArray(); // Ensure the array is updated, whether now or later.
        } else {
            return applyToFxArrayIfNecessary(); // We've exited a batch mode, so ensure the array gets updated if necessary.
        }
    }

    private boolean applyBatchRemovals() {
        // Abort if there aren't any values to remove.
        int removalCount = this.queuedIndexRemovals.getBitCount();
        if (removalCount == 0)
            return false;

        // Remove the values from the array.
        this.array.removeIndices(this.queuedIndexRemovals);

        // Determine if all removed entries are grouped together (and can be treated as a more efficient ranged-removal)
        boolean isRangeRemoval = true;
        int startBit = this.queuedIndexRemovals.getFirstBitIndex();
        for (int i = 0; i < removalCount; i++) {
            if (!this.queuedIndexRemovals.getBit(startBit + i)) {
                isRangeRemoval = false;
                break;
            }
        }

        // Call hook
        if (isRangeRemoval) {
            // Range removals are more efficient to handle, so we'd like to call them when possible.
            this.onRangeRemovalComplete(startBit, removalCount);
        } else {
            this.onBatchRemovalComplete(this.queuedIndexRemovals);
        }

        // Clear future.
        this.queuedIndexRemovals.clear();
        return true;
    }

    /**
     * Called upon successful removal of values.
     * @param indices The indices removed.
     */
    protected void onBatchRemovalComplete(IndexBitArray indices) {
        // Update the queued insertions. (Assumes the insertion indices are sorted lowest to highest)
        int indexOffset = 0;
        int nextRemovalIndex = indices.getFirstBitIndex();
        for (int i = 0; i < this.queuedInsertionIndices.size(); i++) {
            int insertionIndex = this.queuedInsertionIndices.get(i);

            // Any removals that have occurred before this index impact the index change.
            while (insertionIndex > nextRemovalIndex && nextRemovalIndex >= 0) {
                indexOffset++;
                nextRemovalIndex = indices.getNextBitIndex(nextRemovalIndex);
            }

            this.queuedInsertionIndices.set(i, insertionIndex - indexOffset);
        }
    }

    /**
     * Called upon successful removal of values.
     * @param startIndex The first index removed.
     * @param amount     The number of elements removed.
     */
    protected void onRangeRemovalComplete(int startIndex, int amount) {
        // Update the queued insertions.
        for (int i = 0; i < this.queuedInsertionIndices.size(); i++) {
            int insertionIndex = this.queuedInsertionIndices.get(i);
            if (insertionIndex > startIndex)
                this.queuedInsertionIndices.set(i, insertionIndex - Math.min(amount, insertionIndex - startIndex));
        }
    }

    /**
     * Gets a single value of array. This is generally as fast as direct access
     * to an array and eliminates necessity to make a copy of array.
     * If a pending value is waiting to be written (but not inserted), the pending value is returned.
     * @param index index of element to get
     * @return value at the given index
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside array bounds
     */
    public int get(int index) {
        return this.array.get(index);
    }

    /**
     * Gets a single value of array. If a pending value is waiting to be written, the original unchanged value is returned.
     * This is generally as fast as direct access to an array.
     * @param index index of element to get
     * @return value at the given index
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside array bounds
     */
    public int getOld(int index) {
        return this.fxArray.get(index);
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
        this.array.set(index, value);
        applyToFxArray();
    }

    /**
     * Adds a single value to the end of the array
     * Functions regardless of batching state.
     * @param value new value to append to the array
     */
    public void add(int value) {
        insert(this.array.size(), value);
    }

    /**
     * Adds a single value to the provided index
     * @param index index of the value to insert
     * @param value new value to append to the array
     * @return true if the value was added, false if it was batched.
     */
    public boolean insert(int index, int value) {
        if (isBatchInsertionActive()) {
            if (index > size() || index < 0)
                throw new IndexOutOfBoundsException("The provided insertion index (" + index + ") is not valid when the array has a length of " + size() + ".");

            int insertionIndex = this.queuedInsertionIndices.getInsertionPoint(index);
            int previousIndex = insertionIndex - 1;
            boolean hasPreviousBuffer = previousIndex >= 0 && this.queuedInsertionIndices.get(previousIndex) == index;
            int previousBufferSize = hasPreviousBuffer ? this.queuedInsertionSourceSizes.get(previousIndex) : 0;
            if (hasPreviousBuffer && (this.queuedInsertionSourceIndices.get(previousIndex) + previousBufferSize) == this.queuedInsertionSourceDataBuffer.size()) {
                // Expand the previous buffer segment instead of adding a new one.
                this.queuedInsertionSourceSizes.set(previousIndex, previousBufferSize + 1);
                this.queuedInsertionSourceDataBuffer.add(value);
            } else {
                // Add a new buffer segment.
                this.queuedInsertionIndices.add(insertionIndex, index);
                this.queuedInsertionSourceSizes.add(insertionIndex, 1);
                this.queuedInsertionSourceIndices.add(insertionIndex, this.queuedInsertionSourceDataBuffer.size());
                this.queuedInsertionSourceDataBuffer.add(value);
            }

            return false;
        } else {
            this.array.add(index, value);
            onRangeInsertionComplete(index, 1);
            applyToFxArray();
            return true;
        }
    }

    /**
     * Removes a single value from the provided index.
     * If bulk removal mode is enabled, the removal will be queued.
     * @param index index of the value to remove
     * @return removed value
     */
    public int remove(int index) {
        if (isBatchRemovalActive()) {
            if (index >= size() || index < 0)
                throw new IndexOutOfBoundsException("The provided removal index (" + index + ") is not valid when the array has a length of " + size() + ".");

            this.queuedIndexRemovals.setBit(index, true);
            return this.array.get(index);
        } else {
            int removedValue = this.array.remove(index);
            onRangeRemovalComplete(index, 1);
            applyToFxArray();
            return removedValue;
        }
    }

    /**
     * Remove a number of values starting at the provided index.
     * If bulk removal mode is enabled, the removal will be queued.
     * @param startIndex index to remove values from
     * @param amount     amount of elements to remove
     * @return true if the values were removed, false if they were batched.
     */
    public boolean remove(int startIndex, int amount) {
        if (isBatchRemovalActive()) {
            if (startIndex + amount > size() || startIndex < 0) {
                throw new IndexOutOfBoundsException("The provided removal index (" + startIndex + " + " + amount + ") is not valid when the array has a length of " + size() + ".");
            } else {
                this.queuedIndexRemovals.setBits(startIndex, amount, true);
            }

            return false;
        } else {
            this.array.remove(startIndex, amount);
            onRangeRemovalComplete(startIndex, amount);
            applyToFxArray();
            return true;
        }
    }

    /**
     * Appends given {@code elements} to the end of this array. Capacity is increased
     * if necessary to match the new size of the data.
     * @param elements elements to append
     */
    public void addAll(int... elements) {
        addAll(this.array.size(), elements, 0, elements.length);
    }

    /**
     * Inserts given {@code elements} to the provided array index.
     * Capacity is increased if necessary to match the new size of the data.
     * @param index     index of the elements to insert
     * @param elements  elements to insert
     * @return true if the values were added, false if they were batched.
     */
    public boolean addAll(int index, int... elements) {
        return addAll(index, elements, 0, elements.length);
    }

    /**
     * Appends a portion of given array to the end of this array.
     * Capacity is increased if necessary to match the new size of the data.
     * @param src      source array
     * @param srcIndex starting position in source array
     * @param length   length of portion to append
     */
    public void addAll(int[] src, int srcIndex, int length) {
        addAll(this.array.size(), src, srcIndex, length);
    }

    /**
     * Appends a portion of given array to the target array index.
     * Capacity is increased if necessary to match the new size of the data.
     * @param index index of the values to insert
     * @param src source array
     * @param srcIndex starting position in source array
     * @param length length of portion to append
     * @return true if the values were added, false if they were batched.
     */
    public boolean addAll(int index, int[] src, int srcIndex, int length) {
        if (src == null)
            throw new NullPointerException("src");
        if (srcIndex < 0)
            throw new IndexOutOfBoundsException("The provided source index (" + srcIndex + ") is not valid.");
        if (srcIndex + length > src.length || length < 0)
            throw new IndexOutOfBoundsException("The provided source length (" + length + ") is not valid at index " + srcIndex + " when the source array has a length of " + src.length + ".");

        if (isBatchInsertionActive()) {
            int arrayLength = size();
            if (index > arrayLength || index < 0)
                throw new IndexOutOfBoundsException("The provided insertion index (" + index + ") is not valid when the array has a length of " + arrayLength + ".");

            int insertionIndex = this.queuedInsertionIndices.getInsertionPoint(index);
            int previousIndex = insertionIndex - 1;
            boolean hasPreviousBuffer = previousIndex >= 0 && this.queuedInsertionIndices.get(previousIndex) == index;
            int previousBufferSize = hasPreviousBuffer ? this.queuedInsertionSourceSizes.get(previousIndex) : 0;
            if (hasPreviousBuffer && (this.queuedInsertionSourceIndices.get(previousIndex) + previousBufferSize) == this.queuedInsertionSourceDataBuffer.size()) {
                // Expand the previous buffer segment instead of adding a new one.
                this.queuedInsertionSourceSizes.set(previousIndex, previousBufferSize + length);
                this.queuedInsertionSourceDataBuffer.addAll(src, srcIndex, length);
            } else {
                // Add a new buffer segment.
                this.queuedInsertionIndices.add(insertionIndex, index);
                this.queuedInsertionSourceSizes.add(insertionIndex, length);
                this.queuedInsertionSourceIndices.add(insertionIndex, this.queuedInsertionSourceDataBuffer.size());
                this.queuedInsertionSourceDataBuffer.addAll(src, srcIndex, length);
            }

            return false;
        } else {
            this.array.addAll(index, src, srcIndex, length);
            onRangeInsertionComplete(index, length);
            applyToFxArray();
            return true;
        }
    }

    /**
     * Copies a portion of specified array into this observable array. Throws
     * the same exceptions as {@link System#arraycopy(java.lang.Object,
     * int, java.lang.Object, int, int) System.arraycopy()} method.
     * @param index     the starting destination position in this observable array
     * @param src       source array to copy
     * @param srcIndex  starting position in source array
     * @param length    length of portion to copy
     */
    public void set(int index, int[] src, int srcIndex, int length) {
        this.array.set(index, src, srcIndex, length);
        applyToFxArray();
    }

    /**
     * Returns the size of the array.
     */
    public int size() {
        return this.array.size();
    }

    /**
     * Get the number of elements that will be in the array after the queued batch operations complete.
     */
    public int pendingSize() {
        return this.array.size() + this.queuedInsertionSourceSizes.sum() - this.queuedIndexRemovals.getBitCount();
    }
}