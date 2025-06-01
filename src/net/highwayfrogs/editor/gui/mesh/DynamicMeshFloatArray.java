package net.highwayfrogs.editor.gui.mesh;

import javafx.collections.ObservableFloatArray;
import lombok.Getter;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFloatArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFloatArrayBatcher;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArrayBatcher;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

/**
 * Represents an array of mesh data.
 * Meshes will group multiple elements together, into what I'm calling here as a unit.
 * So, an individual element could be an X position, Y position, etc.
 * Group multiple elements together, and that becomes a single unit for the purposes of this class.
 * Created by Kneesnap on 12/28/2023.
 */
@Getter
public class DynamicMeshFloatArray extends FXFloatArrayBatcher {
    private final DynamicMesh mesh;
    private final String unitName;
    private final int vertexSize;
    private final int vertexOffset;
    private final int elementsPerUnit;
    private final Runnable indexUpdateCallback;
    private static final int FACE_ELEMENT_BATCH_REMOVAL_WARNING_LIMIT = 3;

    public DynamicMeshFloatArray(DynamicMesh mesh, String unitName, ObservableFloatArray meshArray, int vertexOffset, int elementsPerUnit, Runnable indexUpdateCallback) {
        super(new FXFloatArray(), meshArray);
        this.mesh = mesh;
        this.unitName = unitName;
        this.vertexSize = mesh.getVertexFormat().getVertexIndexSize();
        this.vertexOffset = vertexOffset;
        this.elementsPerUnit = elementsPerUnit;
        this.indexUpdateCallback = indexUpdateCallback;
    }

    /**
     * Gets the logger used by this class.
     */
    public ILogger getLogger() {
        return this.mesh.getLogger();
    }

    @Override
    @SuppressWarnings("ExtractMethodRecommender")
    protected void onRangeInsertionComplete(int startIndex, int insertedDataAmount) {
        super.onRangeInsertionComplete(startIndex, insertedDataAmount);
        if ((insertedDataAmount % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A range insertion occurred which inserted a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ". (Amount: " + insertedDataAmount + ")");

        // Update indices to reflect the data has been written.
        if (this.indexUpdateCallback != null)
            this.indexUpdateCallback.run();

        int newSize = size();
        int insertionEnd = startIndex + insertedDataAmount;
        if (insertionEnd >= newSize)
            return; // The elements were inserted at the end, so there shouldn't have been any faces accessing the data. (Or if there were, they were likely pre-emptively referencing the inserted values.)

        // Update faces to use updated indices.
        int insertedElementAmount = (insertedDataAmount / this.elementsPerUnit);
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        faceData.startBatchingUpdates();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldDataIndex = faceData.get(i);
            int oldElementIndex = (oldDataIndex * this.elementsPerUnit);

            // Increase the index value, to offset the newly inserted values.
            if (oldElementIndex >= startIndex)
                faceData.set(i, oldDataIndex + insertedElementAmount);
        }

        faceData.endBatchingUpdates();
    }

    @Override
    protected void onBatchInsertionComplete(FXIntArray indices, FXIntArray insertionLengths) {
        super.onBatchInsertionComplete(indices, insertionLengths);

        int totalInsertionCount = insertionLengths.sum();
        if ((totalInsertionCount % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A batch insertion occurred which inserted a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ". (Inserted Elements: " + totalInsertionCount + ")");

        // Update indices to reflect the data has been written.
        if (this.indexUpdateCallback != null)
            this.indexUpdateCallback.run();

        // Update insertion lengths array to instead represent the full amount of insertions for a given position.
        // The array is cleared immediately after this function runs, so it's okay to modify the input.
        int insertedValueSum = 0;
        for (int i = 0; i < insertionLengths.size(); i++) {
            int temp = insertionLengths.get(i);
            insertionLengths.set(i, insertedValueSum / this.elementsPerUnit);
            insertedValueSum += temp;
        }
        insertionLengths.add(insertedValueSum);

        // Update faces to use updated indices. O(n log(n))
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        faceData.startBatchingUpdates();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldDataIndex = faceData.get(i);
            int oldElementIndex = oldDataIndex * this.elementsPerUnit;
            int lookupIndex = indices.getInsertionPoint(oldElementIndex);
            int insertedValueCount = insertionLengths.get(lookupIndex);

            // Save new value.
            if (insertedValueCount > 0)
                faceData.set(i, oldDataIndex + insertedValueCount);
        }

        faceData.endBatchingUpdates();
    }

    @Override
    protected void onRangeRemovalComplete(int startIndex, int removedDataAmount) {
        super.onRangeRemovalComplete(startIndex, removedDataAmount);
        if ((removedDataAmount % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A range removal occurred which removed a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ". (Amount: " + removedDataAmount + ")");

        // Update indices to reflect the data has been removed.
        if (this.indexUpdateCallback != null)
            this.indexUpdateCallback.run();

        // Update faces to use updated indices.
        int facesUsingRemovedIndices = 0;
        int removedElementAmount = (removedDataAmount / this.elementsPerUnit);
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        faceData.startBatchingUpdates();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldDataIndex = faceData.get(i);
            int oldElementIndex = oldDataIndex * this.elementsPerUnit;

            // Reduce the element index value by the amount which was removed, assuming it is impacted by the removed values.
            if (oldElementIndex >= startIndex) {
                if (startIndex + removedDataAmount > oldElementIndex && !getMesh().getEditableFaces().isQueuedForRemoval(i))
                    facesUsingRemovedIndices++;

                faceData.set(i, Math.max(0, oldDataIndex - removedElementAmount));
            }
        }

        faceData.endBatchingUpdates();
        if (facesUsingRemovedIndices > 0) // Won't warn if the faceData in question is queued for removal.
            getLogger().warning("%d total face element(s) referenced newly range-removed %s values. This will probably create visual corruption, so make sure to remove the faces first next time.", facesUsingRemovedIndices, this.unitName);
    }

    @Override
    protected void onBatchRemovalComplete(IndexBitArray indices) {
        super.onBatchRemovalComplete(indices);
        if ((indices.getBitCount() % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A batch removal occurred which removed a number of " + this.unitName + " elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ". (Indices: " + indices.getBitCount() + ")");

        // Update indices to reflect the data has been removed.
        if (this.indexUpdateCallback != null)
            this.indexUpdateCallback.run();

        // Update faces to use updated indices.
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        faceData.startBatchingUpdates();
        int errorCount = 0;
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldDataIndex = faceData.get(i);
            int oldElementIndex = oldDataIndex * this.elementsPerUnit;

            // Show warnings if the face array is seen to be using data that was just removed.
            if (indices.getBit(oldElementIndex) && !getMesh().getEditableFaces().isQueuedForRemoval(i) && ++errorCount <= FACE_ELEMENT_BATCH_REMOVAL_WARNING_LIMIT)
                getLogger().warning("Face Element %d referenced %s index %d, which was just removed. This will probably create visual corruption.", i, this.unitName, oldDataIndex);

            // Calculate the number of indices removed at/before the current index.
            int removedElements = 0;
            int previousBit = oldElementIndex;
            while ((previousBit = indices.getPreviousBitIndex(previousBit)) >= 0)
                removedElements++;

            // Save new value.
            if (removedElements > 0)
                faceData.set(i, oldDataIndex - (removedElements / this.elementsPerUnit));
        }

        faceData.endBatchingUpdates();
        if (errorCount > FACE_ELEMENT_BATCH_REMOVAL_WARNING_LIMIT) // Won't warn if the faceData in question is queued for removal.
            getLogger().warning("%d total face element(s) referenced newly batch-removed %s values. This will probably create visual corruption, so make sure to remove the faces first next time.", errorCount, this.unitName);
    }
}