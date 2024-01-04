package net.highwayfrogs.editor.gui.mesh;

import javafx.collections.ObservableFloatArray;
import lombok.Getter;
import net.highwayfrogs.editor.utils.IndexBitArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFloatArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFloatArrayBatcher;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArrayBatcher;

/**
 * Represents an array of mesh data.
 * Created by Kneesnap on 12/28/2023.
 */
@Getter
public class DynamicMeshFloatArray extends FXFloatArrayBatcher {
    private final DynamicMesh mesh;
    private final int vertexSize;
    private final int vertexOffset;
    private final int elementSize;

    public DynamicMeshFloatArray(DynamicMesh mesh, ObservableFloatArray meshArray, int vertexOffset, int elementSize) {
        super(new FXFloatArray(), meshArray);
        this.mesh = mesh;
        this.vertexSize = mesh.getVertexFormat().getVertexIndexSize();
        this.vertexOffset = vertexOffset;
        this.elementSize = elementSize;
    }

    @Override
    protected void onRangeInsertionComplete(int startIndex, int amount) {
        super.onRangeInsertionComplete(startIndex, amount);

        // Update faces to use updated indices.
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldElement = faceData.get(i);

            // Increase the index value, to offset the newly inserted values.
            if (oldElement >= startIndex)
                faceData.set(i, oldElement + amount);
        }

        getMesh().updateEntryStartIndices();
    }

    @Override
    protected void onBatchInsertionComplete(FXIntArray indices, FXFloatArray values) {
        super.onBatchInsertionComplete(indices, values);

        // Update faces to use updated indices.
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldElement = faceData.get(i);
            int insertedValues = indices.getInsertionPoint(oldElement);

            // Save new value.
            if (insertedValues > 0)
                faceData.set(i, oldElement + insertedValues);
        }

        getMesh().updateEntryStartIndices();
    }

    @Override
    protected void onRangeRemovalComplete(int startIndex, int amount) {
        super.onRangeRemovalComplete(startIndex, amount);

        // Update faces to use updated indices.
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldElement = faceData.get(i);

            // Reduce the element index value by the amount which was removed, assuming it is impacted by the removed values.
            if (oldElement >= startIndex)
                faceData.set(i, Math.max(0, oldElement - amount));
        }

        getMesh().updateEntryStartIndices();
    }

    @Override
    protected void onBatchRemovalComplete(IndexBitArray indices) {
        super.onBatchRemovalComplete(indices);

        // Update faces to use updated indices.
        FXIntArrayBatcher faceData = this.mesh.getEditableFaces();
        int errorCount = 0;
        for (int i = this.vertexOffset; i < faceData.size(); i += this.vertexSize) {
            int oldElement = faceData.get(i);

            // Show warnings if the face array is seen to be using data that was just removed.
            if (indices.getBit(oldElement * this.elementSize)) {
                errorCount++;
                if (errorCount == 10) {
                    System.out.println("[Warning] Omitting remaining batch removal warnings.");
                } else if (errorCount < 10) {
                    System.out.println("[Warning] Face Element " + i + " referenced index " + oldElement + ", which was just removed. This will probably create visual corruption.");
                }
            }

            // Calculate the number of indices removed at/before the current index.
            int removedElements = 0;
            int previousBit = oldElement;
            while ((previousBit = indices.getPreviousBitIndex(previousBit)) >= 0)
                removedElements++;

            // Save new value.
            if (removedElements > 0)
                faceData.set(i, oldElement - removedElements);
        }

        if (errorCount > 10)
            System.out.println("[Warning] " + errorCount + " face elements referenced newly removed indices. This will probably create visual corruption.");

        getMesh().updateEntryStartIndices();
    }
}