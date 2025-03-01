package net.highwayfrogs.editor.gui.mesh;

import javafx.collections.ObservableIntegerArray;
import lombok.Getter;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArray;
import net.highwayfrogs.editor.utils.fx.wrapper.FXIntArrayBatcher;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

/**
 * Represents an array of mesh data.
 * Meshes will group multiple elements together, into what I'm calling here as a unit.
 * So, an individual element could be an X position, Y position, etc.
 * Group multiple elements together, and that becomes a single unit for the purposes of this class.
 * Created by Kneesnap on 2/28/2025.
 */
@Getter
public class DynamicMeshIntArray extends FXIntArrayBatcher  {
    private final DynamicMesh mesh;
    private final String unitName;
    private final int elementsPerUnit;
    private final Runnable indexUpdateCallback;
    private static final int FACE_ELEMENT_BATCH_REMOVAL_WARNING_LIMIT = 3;

    public DynamicMeshIntArray(DynamicMesh mesh, String unitName, ObservableIntegerArray meshArray, int elementsPerUnit, Runnable indexUpdateCallback) {
        super(new FXIntArray(), meshArray);
        this.mesh = mesh;
        this.unitName = unitName;
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
    protected void onRangeInsertionComplete(int startIndex, int insertedDataAmount) {
        super.onRangeInsertionComplete(startIndex, insertedDataAmount);
        if ((insertedDataAmount % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A range insertion occurred which inserted a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ". (Amount: " + insertedDataAmount + ")");

        // Update indices to reflect the data has been written.
        if (this.indexUpdateCallback != null)
            this.indexUpdateCallback.run();
    }

    @Override
    protected void onBatchInsertionComplete(FXIntArray indices, FXIntArray insertionLengths) {
        super.onBatchInsertionComplete(indices, insertionLengths);

        // Validate amount.
        int totalInsertionCount = insertionLengths.sum();
        if ((totalInsertionCount % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A batch insertion occurred which inserted a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ". (Inserted Elements: " + totalInsertionCount + ")");

        // Update indices to reflect the data has been written.
        if (this.indexUpdateCallback != null)
            this.indexUpdateCallback.run();
    }

    @Override
    protected void onRangeRemovalComplete(int startIndex, int removedDataAmount) {
        super.onRangeRemovalComplete(startIndex, removedDataAmount);
        if ((removedDataAmount % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A range removal occurred which removed a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ". (Amount: " + removedDataAmount + ")");

        // Update indices to reflect the data has been written.
        if (this.indexUpdateCallback != null)
            this.indexUpdateCallback.run();
    }

    @Override
    protected void onBatchRemovalComplete(IndexBitArray indices) {
        super.onBatchRemovalComplete(indices);
        if ((indices.getBitCount() % this.elementsPerUnit) != 0)
            throw new IllegalStateException("A batch removal occurred which removed a number of elements which was not divisible by " + this.elementsPerUnit + ", a requirement for a single " + this.unitName + ". (Indices: " + indices.getBitCount() + ")");

        // Update indices to reflect the data has been written.
        if (this.indexUpdateCallback != null)
            this.indexUpdateCallback.run();
    }
}
