package net.highwayfrogs.editor.games.sony.shared.collprim;

import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim.CollprimType;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents a mesh UI Manager which has collprim editing capabilities.
 * Created by Kneesnap on 9/26/2023.
 */
public interface ICollprimEditorUI {
    /**
     * Gets the GUIEditorGrid used to edit Collprim data.
     */
    public GUIEditorGrid getCollprimEditorGrid();

    /**
     * This method is run when the collprim changes its type in an editor.
     * @param collprim The collprim which changed types.
     */
    public void onCollprimChangeType(MRCollprim collprim, CollprimShapeAdapter<?> adapter, CollprimType oldType, CollprimType newType); // Update the 3D shape display and UI.

    /**
     * Called when the position and/or rotation of the collprim changes.
     * @param collprim The collprim whose position changed.
     * @param adapter  The adapter whose position changed.
     */
    public void updateCollprimPosition(MRCollprim collprim, CollprimShapeAdapter<?> adapter);

    /**
     * Called when a collprim should be removed.
     * @param collprim The collprim to remove.
     * @param adapter  The adapter representing the collprim.
     */
    public void onCollprimRemove(MRCollprim collprim, CollprimShapeAdapter<?> adapter);
}