package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.shared.collprim.CollprimShapeAdapter;
import net.highwayfrogs.editor.games.sony.shared.collprim.ICollprimEditorUI;
import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim;
import net.highwayfrogs.editor.games.sony.shared.ui.file.MOFController;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;

/**
 * Represents MR_COLLPRIM.
 * The way this works is in Frogger, Frogger has a collision hilite.
 * When testing collision, it will check if any of Frogger's collision hilites are inside the collprim, or bbox. If a collprim exists, it will check the collprim, otherwise it will check the bbox.
 * If the collprim is the last one on the model (the release builds of Frogger only have 1 collprim per model, though it's not necessarily that way always), use the last in list flag.
 * Created by Kneesnap on 1/8/2019.
 */
@Getter
@Setter
public class MOFCollprim extends MRCollprim {
    private MOFPart parentPart;
    private int matrixIndex; // This value is always zero in Frogger. It has been seen as non-zero in Old Frogger and Beast Wars.

    public MOFCollprim(MOFPart parentPart) {
        super(parentPart.getGameInstance());
        this.parentPart = parentPart;
    }

    @Override
    public int updateFlags() {
        // Set the flag based on if this is the last one in the collprim.
        // But only set it in a situation where we can know if it's actually the last collprim or not.
        if (this.parentPart != null && this.parentPart.getCollprims().size() > 0)
            setFlag(FLAG_LAST_IN_LIST, this == this.parentPart.getCollprims().get(this.parentPart.getCollprims().size() - 1));

        return getFlags();
    }

    @Override
    public int getRawMatrixValue() {
        return this.matrixIndex;
    }

    @Override
    public void setRawMatrixValue(DataReader reader, int rawMatrixValue) {
        if (rawMatrixValue < -1)
            throw new RuntimeException("Unexpected rawMatrixValue " + rawMatrixValue);

        this.matrixIndex = rawMatrixValue;
    }

    @Override
    public PSXMatrix getMatrix() {
        return this.matrixIndex >= 0 && this.parentPart != null && this.parentPart.getMatrices().size() > this.matrixIndex
                ? this.parentPart.getMatrices().get(this.matrixIndex) : null;
    }

    @Override
    public void removeMatrix() {
        PSXMatrix oldMatrix = getMatrix();
        if (oldMatrix != null)
            this.parentPart.getMatrices().remove(oldMatrix);

        this.matrixIndex = -1;
    }

    @Override
    protected void setupMatrixCreator(MOFController controller, CollprimShapeAdapter<?> adapter, GUIEditorGrid grid) {
        grid.addButton("Create Matrix", () -> {
            this.matrixIndex = this.parentPart.getMatrices().size();
            this.parentPart.getMatrices().add(new PSXMatrix());
            controller.updateCollprimBoxes(true, this); // Update the model display and UI.
        }).setDisable(this.parentPart == null || this.parentPart.getMatrices() == null);
    }

    @Override
    protected <TManager extends MeshUIManager<?> & ICollprimEditorUI> void setupMatrixCreator(TManager manager, CollprimShapeAdapter<?> adapter, GUIEditorGrid grid) {
        grid.addButton("Create Matrix", () -> {
            this.matrixIndex = this.parentPart.getMatrices().size();
            this.parentPart.getMatrices().add(new PSXMatrix());
            manager.updateCollprimPosition(this, adapter); // Update the model display.
            manager.updateEditor(); // Refresh UI.
        }).setDisable(this.parentPart == null || this.parentPart.getMatrices() == null);
    }
}