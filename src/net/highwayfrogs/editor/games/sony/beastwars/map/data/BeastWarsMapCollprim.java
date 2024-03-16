package net.highwayfrogs.editor.games.sony.beastwars.map.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.shared.collprim.CollprimShapeAdapter;
import net.highwayfrogs.editor.games.sony.shared.collprim.ICollprimEditorUI;
import net.highwayfrogs.editor.games.sony.shared.collprim.MRCollprim;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MOFController;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a collprim used in a beast wars map.
 * Different Beast Wars map collprims never appear to share the same matrix pointer, so we treat each matrix as if it belongs exclusively to the collprim.
 * Created by Kneesnap on 9/16/2023.
 */
@Getter
public class BeastWarsMapCollprim extends MRCollprim {
    private final BeastWarsMapFile mapFile;
    private PSXMatrix matrix;

    public BeastWarsMapCollprim(BeastWarsMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public BeastWarsInstance getGameInstance() {
        return (BeastWarsInstance) super.getGameInstance();
    }

    @Override
    public int updateFlags() {
        // No updates need occur.
        return getFlags();
    }

    @Override
    public int getRawMatrixValue() {
        int matrixPointer = 0;

        boolean includeMatrixData = true;
        for (int i = 0; i < this.mapFile.getCollprims().size(); i++) {
            BeastWarsMapCollprim otherCollprim = this.mapFile.getCollprims().get(i);
            matrixPointer += SIZE_IN_BYTES;

            if (otherCollprim == this) // Matrix data comes after ALL collprims, so once we reach ourselves, stop including matrices sizes.
                includeMatrixData = false;

            if (includeMatrixData && otherCollprim.hasMatrix())
                matrixPointer += PSXMatrix.BYTE_SIZE;
        }

        return matrixPointer;
    }

    @Override
    public void setRawMatrixValue(DataReader reader, int rawMatrixValue) {
        if (rawMatrixValue == 0) {
            this.matrix = null;
            return;
        }

        if (reader == null)
            throw new RuntimeException("Cannot read matrix from " + Utils.toHexString(rawMatrixValue) + " without a DataReader.");

        reader.jumpTemp(rawMatrixValue);
        this.matrix = new PSXMatrix();
        this.matrix.load(reader);
        reader.jumpReturn();
    }

    @Override
    public void removeMatrix() {
        this.matrix = null;
    }

    @Override
    protected void setupMatrixCreator(MOFController controller, CollprimShapeAdapter<?> adapter, GUIEditorGrid grid) {
        grid.addButton("Create Matrix", () -> {
            this.matrix = new PSXMatrix();
            controller.updateCollprimBoxes(true, this); // Update the model display and UI.
        });
    }

    @Override
    protected <TManager extends MeshUIManager<?> & ICollprimEditorUI> void setupMatrixCreator(TManager manager, CollprimShapeAdapter<?> adapter, GUIEditorGrid grid) {
        grid.addButton("Create Matrix", () -> {
            this.matrix = new PSXMatrix();
            manager.updateCollprimPosition(this, adapter); // Update the model display and UI.
            manager.updateEditor(); // Refresh UI.
        });
    }
}