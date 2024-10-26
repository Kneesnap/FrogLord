package net.highwayfrogs.editor.gui.mesh;

import javafx.collections.ObservableFloatArray;
import lombok.Getter;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFloatArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFloatArrayBatcher;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArrayBatcher;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.util.logging.Logger;

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
    private static final int FACE_ELEMENT_BATCH_REMOVAL_WARNING_LIMIT = 3;

    public DynamicMeshFloatArray(DynamicMesh mesh, String unitName, ObservableFloatArray meshArray, int vertexOffset, int elementsPerUnit) {
        super(new FXFloatArray(), meshArray);
        this.mesh = mesh;
        this.unitName = unitName;
        this.vertexSize = mesh.getVertexFormat().getVertexIndexSize();
        this.vertexOffset = vertexOffset;
        this.elementsPerUnit = elementsPerUnit;
    }

    /**
     * Gets the logger used by this class.
     */
    public Logger getLogger() {
        return this.mesh.getLogger();
    }

    @Override
    protected void onRangeInsertionComplete(int startIndex, int insertedDataAmount) {
        super.onRangeInsertionComplete(startIndex, insertedDataAmount);
        if ((insertedDataAmount % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A range insertion occurred which inserted a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ".");

        int newSize = size();
        int insertionEnd = startIndex + insertedDataAmount;
        if (insertionEnd >= newSize)
            return; // The elements were inserted at the end, so there shouldn't have been any faces accessing the data.

        // Update faces to use updated indices.
        int insertedElementAmount = (insertedDataAmount / this.elementsPerUnit);
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldDataIndex = faceData.get(i);
            int oldElementIndex = (oldDataIndex * this.elementsPerUnit);

            // Increase the index value, to offset the newly inserted values.
            if (oldElementIndex >= startIndex)
                faceData.set(i, oldDataIndex + insertedElementAmount);
        }

        getMesh().updateEntryStartIndices();
    }

    @Override
    protected void onBatchInsertionComplete(FXIntArray indices, FXFloatArray values) {
        super.onBatchInsertionComplete(indices, values);
        if ((indices.size() % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A batch insertion occurred which inserted a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ".");

        // Update faces to use updated indices.
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldDataIndex = faceData.get(i);
            int oldElementIndex = oldDataIndex * this.elementsPerUnit;
            int insertedValueCount = indices.getInsertionPoint(oldElementIndex); // The array is sorted, so this should work.

            // Save new value.
            if (insertedValueCount > 0)
                faceData.set(i, oldDataIndex + (insertedValueCount / this.elementsPerUnit));
        }

        getMesh().updateEntryStartIndices();
    }

    @Override
    protected void onRangeRemovalComplete(int startIndex, int removedDataAmount) {
        super.onRangeRemovalComplete(startIndex, removedDataAmount);
        if ((removedDataAmount % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A range removal occurred which removed a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ".");

        if (startIndex >= size())
            return; // The elements were removed from the end, so the behavior for what to do with any faces that used this data is undefined.

        // Update faces to use updated indices.
        int removedElementAmount = (removedDataAmount / this.elementsPerUnit);
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldDataIndex = faceData.get(i);
            int oldElementIndex = oldDataIndex * this.elementsPerUnit;

            // Reduce the element index value by the amount which was removed, assuming it is impacted by the removed values.
            if (oldElementIndex >= startIndex)
                faceData.set(i, Math.max(0, oldDataIndex - removedElementAmount));
        }

        getMesh().updateEntryStartIndices();
    }

    @Override
    protected void onBatchRemovalComplete(IndexBitArray indices) {
        super.onBatchRemovalComplete(indices);
        if ((indices.getBitCount() % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A batch removal occurred which removed a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ".");

        // Update faces to use updated indices.
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        int errorCount = 0;
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldDataIndex = faceData.get(i);
            int oldElementIndex = oldDataIndex * this.elementsPerUnit;

            // Show warnings if the face array is seen to be using data that was just removed.
            if (indices.getBit(oldElementIndex) && ++errorCount <= FACE_ELEMENT_BATCH_REMOVAL_WARNING_LIMIT)
                getLogger().warning("Face Element " + i + " referenced index " + oldDataIndex + ", which was just removed. This will probably create visual corruption.");

            // Calculate the number of indices removed at/before the current index.
            int removedElements = 0;
            int previousBit = oldDataIndex;
            while ((previousBit = indices.getPreviousBitIndex(previousBit)) >= 0)
                removedElements++;

            // Save new value.
            if (removedElements > 0)
                faceData.set(i, oldDataIndex - (removedElements / this.elementsPerUnit));
        }

        if (errorCount > FACE_ELEMENT_BATCH_REMOVAL_WARNING_LIMIT)
            getLogger().warning(errorCount + " total face elements referenced newly removed indices. This will probably create visual corruption.");

        getMesh().updateEntryStartIndices();
    }
}