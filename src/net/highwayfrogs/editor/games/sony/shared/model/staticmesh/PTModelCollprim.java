package net.highwayfrogs.editor.games.sony.shared.model.staticmesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.collprim.CollprimShapeAdapter;
import net.highwayfrogs.editor.games.sony.shared.collprim.ICollprim;
import net.highwayfrogs.editor.games.sony.shared.collprim.ICollprimEditorUI;
import net.highwayfrogs.editor.games.sony.shared.collprim.PTCollprim;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Represents a collprim with a static pointer.
 * Created by Kneesnap on 5/17/2024.
 */
public class PTModelCollprim extends PTCollprim {
    @Getter private PSXMatrix matrix;
    private transient int matrixAddress;

    public PTModelCollprim(SCGameInstance instance) {
        super(instance);
    }

    /**
     * Reads the collprim matrix from the current position.
     * @param reader the reader to read it from
     */
    public void readMatrix(DataReader reader) {
        if (this.matrixAddress == 0) {
            this.matrix = null;
            this.matrixAddress = -1;
            return; // No matrix available to read.
        }

        if (this.matrixAddress <= 0)
            throw new RuntimeException("Cannot read collprim matrix, the pointer " + NumberUtils.toHexString(this.matrixAddress) + " is invalid.");

        requireReaderIndex(reader, this.matrixAddress, "Expected PTCollprim matrix");
        this.matrix = new PSXMatrix();
        this.matrix.load(reader);

        this.matrixAddress = -1;
    }

    /**
     * Writes the collprim matrix to the current position.
     * @param writer the writer to write it to
     */
    public void writeMatrix(DataWriter writer) {
        if (this.matrixAddress <= 0)
            throw new RuntimeException("Cannot write collprim matrix, the pointer " + NumberUtils.toHexString(this.matrixAddress) + " is invalid.");

        if (this.matrix != null) {
            writer.writeAddressTo(this.matrixAddress);
            this.matrix.save(writer);
        }

        this.matrixAddress = -1;
    }

    @Override
    public short updateFlags() {
        return getFlags();
    }

    @Override
    public int getRawMatrixValue() {
        return this.matrixAddress;
    }

    @Override
    public void setRawMatrixValue(DataReader reader, int rawMatrixValue) {
        this.matrixAddress = rawMatrixValue;
    }

    @Override
    protected void writeRawMatrixValue(DataWriter writer) {
        this.matrixAddress = writer.writeNullPointer();
    }

    @Override
    public void removeMatrix() {
        this.matrix = null;
        this.matrixAddress = -1;
    }

    @Override
    protected <TManager extends MeshUIManager<?> & ICollprimEditorUI<? super ICollprim>> void setupMatrixCreator(TManager manager, CollprimShapeAdapter<?> adapter, GUIEditorGrid grid) {
        grid.addButton("Create Matrix", () -> {
            this.matrix = new PSXMatrix();
            manager.updateCollprimPosition(this, adapter); // Update the model display.
            manager.updateEditor(); // Refresh UI.
        });
    }
}