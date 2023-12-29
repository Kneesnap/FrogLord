package net.highwayfrogs.editor.utils.fx.wrapper;

import javafx.collections.ObservableFloatArray;
import lombok.Getter;
import net.highwayfrogs.editor.utils.IndexBitArray;
import net.highwayfrogs.editor.utils.IntegerCounter;
import sun.plugin.dom.exception.InvalidStateException;

/**
 * Batches insert/remove array operations for a FXFloatArray into single operations to increase performance.
 * ObservableFloatArray documentation says that optimal performance is obtained with the fewest number of method calls possible.
 * So, we use a cached array that we write to, allowing our changes to be handled once they are all complete, instead of once after every single change.
 * Additionally, we allow bulking changes together to allow for more efficient array operations.
 * Created by Kneesnap on 12/28/2023.
 */
public class FXFloatArrayBatcher {
    @Getter private final FXFloatArray array;
    @Getter private final ObservableFloatArray fxArray;
    private final IntegerCounter batchedInsertion;
    private final FXFloatArray queuedInsertionValues;
    private final FXIntArray queuedInsertionIndices;
    private final IntegerCounter batchedRemovals;
    private final IndexBitArray queuedIndexRemovals;

    public FXFloatArrayBatcher(FXFloatArray array, ObservableFloatArray fxArray) {
        this.array = array;
        this.fxArray = fxArray;
        this.batchedInsertion = new IntegerCounter();
        this.queuedInsertionValues = new FXFloatArray();
        this.queuedInsertionIndices = new FXIntArray();
        this.batchedRemovals = new IntegerCounter();
        this.queuedIndexRemovals = new IndexBitArray();
    }

    /**
     * Test if any batch mode is enabled.
     */
    public boolean isAnyBatchModeEnabled() {
        return isBatchRemovalActive() || isBatchInsertionActive();
    }

    /**
     * Apply to the wrapped JavaFX array.
     */
    public void applyToFxArrayIfReady() {
        if (!isAnyBatchModeEnabled()) {
            System.out.println("Updating FLOAT " + this.fxArray.size() + " -> " + this.array.size()); // TODO: TOSS
            this.array.apply(this.fxArray);
        }
    }

    /**
     * Clear all batched data.
     */
    public void clear() {
        this.queuedInsertionIndices.clear();
        this.queuedInsertionValues.clear();
        this.queuedIndexRemovals.clear();

        if (this.batchedInsertion.isActive())
            throw new IllegalStateException("Cleared the array while batch insertion mode was enabled! (" + this.batchedInsertion.getCounter() + ")");
        if (this.batchedRemovals.isActive())
            throw new IllegalStateException("Cleared the array while batch removal mode was enabled! (" + this.batchedRemovals.getCounter() + ")");
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
     */
    public void endBatchInsertion() {
        // Check if batch insertion is still active.
        if (!this.batchedInsertion.decrement())
            return;

        // Ensure the number of values matches the number of indices.
        if (this.queuedInsertionIndices.size() != this.queuedInsertionValues.size())
            throw new InvalidStateException("There were " + this.queuedInsertionIndices.size() + " indices corresponding to " + this.queuedInsertionValues.size() + " values.");

        // Abort if there aren't any values to insert.
        int valueCount = this.queuedInsertionValues.size();
        if (valueCount == 0)
            return;

        // Shift the array elements and insert in the new values to their slots.
        // This relies upon the array being sorted.
        this.array.insertValues(this.queuedInsertionIndices, this.queuedInsertionValues);

        // Execute hook
        onBatchInsertionComplete(this.queuedInsertionIndices, this.queuedInsertionValues);

        // Clear future.
        this.queuedInsertionValues.clear();
        this.queuedInsertionIndices.clear();

        // Don't update the mesh array if batch removals are active.
        // We still update if there aren't any removals queued, since we want to ensure an update occurs.
        if (!this.batchedRemovals.isActive() || this.queuedIndexRemovals.getBitCount() == 0)
            this.array.apply(this.fxArray);
    }

    /**
     * Called when batch insertion is completed.
     * @param indices The indices of the values inserted.
     * @param values  The values inserted.
     */
    protected void onBatchInsertionComplete(FXIntArray indices, FXFloatArray values) {
        // Update the queued removals.
        int remainingInsertions = values.size();
        int removeIndex = this.queuedIndexRemovals.getLastBitIndex();
        for (int i = this.queuedIndexRemovals.getBitCount(); i > 0; i--) {
            // Stop accounting for insertions which we've passed.
            while (remainingInsertions > 0 && this.queuedInsertionIndices.get(remainingInsertions - 1) > removeIndex)
                remainingInsertions--;

            if (remainingInsertions == 0)
                break; // All remaining indices are not impacted by the insertions, so we can stop.

            // Update the removed indices, moving the indices forward that need to be moved forward.
            // Careful, this code modifies the array we're iterating through, it should only edit parts of the array we're done iterating through.
            this.queuedIndexRemovals.setBit(removeIndex, false);
            this.queuedIndexRemovals.setBit(removeIndex + remainingInsertions, true);

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
     */
    public void endBatchRemoval() {
        // Check if batch removals are still active.
        if (!this.batchedRemovals.decrement())
            return;

        // Abort if there aren't any values to remove.
        int removalCount = this.queuedIndexRemovals.getBitCount();
        if (removalCount == 0)
            return;

        // Remove the values from the array.
        this.array.removeIndices(this.queuedIndexRemovals);

        // Call hook
        this.onBatchRemovalComplete(this.queuedIndexRemovals);

        // Clear future.
        this.queuedIndexRemovals.clear();

        // Don't update the mesh array if batch insertion is still active.
        // We still update if there aren't any insertions queued, since we want to ensure an update occurs.
        if (!this.batchedInsertion.isActive() || this.queuedInsertionIndices.size() == 0)
            this.array.apply(this.fxArray);
    }

    /**
     * Called upon successful removal of values.
     * @param indices The indices removed.
     */
    protected void onBatchRemovalComplete(IndexBitArray indices) {
        // Update the queued insertions. (Assumes the insertion indices are sorted lowest to highest)
        int indexOffset = 0;
        int nextRemovalIndex = this.queuedIndexRemovals.getFirstBitIndex();
        for (int i = 0; i < this.queuedInsertionIndices.size(); i++) {
            int insertionIndex = this.queuedInsertionIndices.get(i);

            // Any removals that have occurred before this index impact the index change.
            while (insertionIndex > nextRemovalIndex && nextRemovalIndex >= 0) {
                indexOffset++;
                nextRemovalIndex = this.queuedIndexRemovals.getNextBitIndex(nextRemovalIndex);
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
     * @param index index of element to get
     * @return value at the given index
     * @throws ArrayIndexOutOfBoundsException if {@code index} is outside array bounds
     */
    public float get(int index) {
        return this.array.get(index);
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
        this.array.set(index, value);
    }

    /**
     * Adds a single value to the end of the array
     * Functions regardless of batching state.
     * @param value new value to append to the array
     */
    public void add(float value) {
        // Not batched since adding values to the end of an array doesn't have any performance benefit from batching.
        this.array.add(value);
        onRangeInsertionComplete(this.array.getLength() - 1, 1);
    }

    /**
     * Adds a single value to the provided index
     * @param index index of the value to insert
     * @param value new value to append to the array
     * @return true if the value was added, false if it was batched.
     */
    public boolean insert(int index, float value) {
        if (isBatchInsertionActive()) {
            int insertionIndex = this.queuedInsertionIndices.binarySearch(index);
            this.queuedInsertionIndices.add(insertionIndex, index);
            this.queuedInsertionValues.add(insertionIndex, value);
            return false;
        } else {
            this.array.add(index, value);
            onRangeInsertionComplete(index, 1);
            return true;
        }
    }

    /**
     * Removes a single value from the provided index.
     * If bulk removal is enabled, these will be skipped.
     * @param index index of the value to remove
     * @return removed value
     */
    public float remove(int index) {
        if (isBatchRemovalActive()) {
            this.queuedIndexRemovals.setBit(index, true);
            return this.array.get(index);
        } else {
            float removedValue = this.array.remove(index);
            onRangeRemovalComplete(index, 1);
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
            this.queuedIndexRemovals.setBits(startIndex, amount, true);
            return false;
        } else {
            this.array.remove(startIndex, amount);
            onRangeRemovalComplete(startIndex, amount);
            return true;
        }
    }

    /**
     * Appends given {@code elements} to the end of this array. Capacity is increased
     * if necessary to match the new size of the data.
     * @param elements elements to append
     */
    public void addAll(float... elements) {
        // These values are added to the end of the array.
        // There is no performance benefit to batching values added to the end of the array.
        int insertIndex = this.array.getLength();
        this.array.addAll(elements);
        onRangeInsertionComplete(insertIndex, elements.length);
    }

    /**
     * Inserts given {@code elements} to the provided array index.
     * Capacity is increased if necessary to match the new size of the data.
     * @param destIndex index of the elements to insert
     * @param elements  elements to insert
     * @return true if the values were added, false if they were batched.
     */
    public boolean addAll(int destIndex, float... elements) {
        if (isBatchInsertionActive()) {
            for (int i = 0; i < elements.length; i++)
                insert(destIndex + i, elements[i]);
            return false;
        } else {
            this.array.addAll(destIndex, elements);
            onRangeInsertionComplete(destIndex, elements.length);
            return true;
        }
    }

    /**
     * Appends a portion of given array to the end of this array.
     * Capacity is increased if necessary to match the new size of the data.
     * @param src      source array
     * @param srcIndex starting position in source array
     * @param length   length of portion to append
     */
    public void addAll(float[] src, int srcIndex, int length) {
        // These values are added to the end of the array.
        // There is no performance benefit to batching values added to the end of the array.
        int insertionIndex = this.array.size();
        this.array.addAll(src, srcIndex, length);
        onRangeInsertionComplete(insertionIndex, length);
    }

    /**
     * Appends a portion of given array to the target array index.
     * Capacity is increased if necessary to match the new size of the data.
     * @param destIndex index of the values to insert
     * @param src       source array
     * @param srcIndex  starting position in source array
     * @param length    length of portion to append
     * @return true if the values were added, false if they were batched.
     */
    public boolean addAll(int destIndex, float[] src, int srcIndex, int length) {
        if (isBatchInsertionActive()) {
            for (int i = 0; i < length; i++)
                insert(destIndex + i, src[srcIndex + i]);
            return false;
        } else {
            this.array.addAll(destIndex, src, srcIndex, length);
            onRangeInsertionComplete(destIndex, length);
            return true;
        }
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
        this.array.set(destIndex, src, srcIndex, length);
    }

    /**
     * Returns the size of the array.
     */
    public int size() {
        return this.array.size();
    }
}