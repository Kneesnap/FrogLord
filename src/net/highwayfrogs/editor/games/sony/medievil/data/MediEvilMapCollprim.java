package net.highwayfrogs.editor.games.sony.medievil.data;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.collprim.*;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MOFController;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.RenderManager;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Consumer;

/**
 * Represents a collprim used in a MediEvil map.
 * Different MediEvil map collprims never appear to share the same matrix pointer, so we treat each matrix as if it belongs exclusively to the collprim.
 * Created by Kneesnap on 9/16/2023.
 */
@Getter
public class MediEvilMapCollprim extends MRCollprim {
    private final MediEvilMapFile mapFile;
    @Setter
    private PSXMatrix matrix;

    public MediEvilMapCollprim(MediEvilMapFile mapFile) {
        super(mapFile.getGameInstance());
        this.mapFile = mapFile;
    }

    @Override
    public MediEvilGameInstance getGameInstance() {
        return (MediEvilGameInstance) super.getGameInstance();
    }

    @Override
    public void load(DataReader reader) {
        setRawMatrixValue(reader, 0);
        this.setFlags(reader.readUnsignedShortAsInt());
        this.setType(resolveType(this.getFlags() >> 14));
        this.setXLength(Utils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4));
        this.setYLength(Utils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4));
        this.setZLength(Utils.fixedPointIntToFloatNBits(reader.readUnsignedShortAsInt(), 4));
        this.setRadiusSquared(Utils.fixedPointIntToFloatNBits(reader.readInt(), 8)); // 8 bits are used because multiplying two fixed point numbers together increases the position of the radius.
    }

    private CollprimType resolveType(int type)
    {
        switch(type) {
            default:
            case 3:
                return CollprimType.CUBOID;
            case 2:
                return CollprimType.CYLINDER_X; // TODO: Confirm these are correct. I'm pretty sure about cuboid and CYLINDER_Y.
            case 1:
                return CollprimType.CYLINDER_Y;
            case 0:
                return CollprimType.CYLINDER_Z;
        }
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
        for (int i = 0; i < this.mapFile.getCollprimsPacket().getCollprims().size(); i++) {
            MediEvilMapCollprim otherCollprim = this.mapFile.getCollprimsPacket().getCollprims().get(i);
            matrixPointer += SIZE_IN_BYTES;

            if (otherCollprim == this) // Matrix data comes after ALL collprims, so once we reach ourselves, stop including matrices sizes.
                includeMatrixData = false;

            if (includeMatrixData && otherCollprim.hasMatrix())
                matrixPointer += PSXMatrix.BYTE_SIZE;
        }

        return matrixPointer;
    }

    /**
     * Add this collprim to the 3D display.
     * @param manager  The render manager to add the display to.
     * @param listID   The list of collprims to add to.
     * @param material The collprim material.
     * @return display
     */
    @Override
    public CollprimShapeAdapter<?> addDisplay(MOFController controller, RenderManager manager, String listID, PhongMaterial material, Consumer<MRCollprim> removeHandler) {
        this.setOffset(this.matrix.toVector());
        SVector position = this.getOffset();
        float x = Utils.fixedPointIntToFloat4Bit(position.getX());
        float y = Utils.fixedPointIntToFloat4Bit(position.getY());
        float z = Utils.fixedPointIntToFloat4Bit(position.getZ());

        CollprimShapeAdapter<?> adapter;
        switch (this.getType()) {
            case CUBOID:
                Box box = manager.addBoundingBoxCenteredWithDimensions(listID, x, y, z, this.getXLength() * 2, this.getYLength() * 2, this.getZLength() * 2, material, true);
                adapter = new CuboidCollprimShapeAdapter(this, box);
                break;
            case SPHERE:
                Sphere sphere = manager.addSphere(listID, x, y, z, 1, material, true);
                adapter = new SphereCollprimShapeAdapter(this, sphere);
                break;
            case CYLINDER_X:
            case CYLINDER_Y:
            case CYLINDER_Z:
                // CYLINDER_Z indicates that the cylinder is aligned to the Z axis, conceptualized as an infinitely large line.
                // zLength thus indicates the height.
                Cylinder cylinder = manager.addCylinder(listID, x, y, z, 1, 2, material, true);
                adapter = new CylinderCollprimShapeAdapter(this, cylinder);
                break;
            default:
                throw new RuntimeException("Unsupported Collprim type " + this.getType() + ", cannot create display for " + toString() + ".");
        }

        adapter.getShape().setOnMouseClicked(evt -> setupEditor(controller, adapter, removeHandler));
        adapter.getShape().setMouseTransparent(false);
        adapter.onLengthXUpdate(this.getXLength());
        adapter.onLengthYUpdate(this.getYLength());
        adapter.onLengthZUpdate(this.getZLength());
        adapter.onRadiusSquaredUpdate(Utils.fixedPointIntToFloat4Bit((int)this.getRadiusSquared()));

        return adapter;
    }

    @Override
    public <TManager extends MeshUIManager<?> & ICollprimEditorUI> CollprimShapeAdapter<?> addDisplay(TManager manager, DisplayList displayList, PhongMaterial material) {
        this.setOffset(this.matrix.toVector());
        SVector position = this.getOffset();
        float x = Utils.fixedPointIntToFloat4Bit(position.getX());
        float y = Utils.fixedPointIntToFloat4Bit(position.getY());
        float z = Utils.fixedPointIntToFloat4Bit(position.getZ());

        CollprimShapeAdapter<?> adapter;
        switch (this.getType()) {
            case CUBOID:
                Box box = displayList.addBoundingBoxCenteredWithDimensions(x, y, z, this.getXLength() * 2, this.getYLength() * 2, this.getZLength() * 2, material, true);
                adapter = new CuboidCollprimShapeAdapter(this, box);
                break;
            case SPHERE:
                Sphere sphere = displayList.addSphere(x, y, z, 1, material, true);
                adapter = new SphereCollprimShapeAdapter(this, sphere);
                break;
            case CYLINDER_X:
            case CYLINDER_Y:
            case CYLINDER_Z:
                // CYLINDER_Z indicates that the cylinder is aligned to the Z axis, conceptualized as an infinitely large line.
                // zLength thus indicates the height.
                Cylinder cylinder = displayList.addCylinder(x, y, z, 1, 2, material, true);
                adapter = new CylinderCollprimShapeAdapter(this, cylinder);
                break;
            default:
                throw new RuntimeException("Unsupported Collprim type " + this.getType() + ", cannot create display for " + toString() + ".");
        }

        adapter.getShape().setOnMouseClicked(evt -> setupEditor(manager, adapter));
        adapter.getShape().setMouseTransparent(false);
        adapter.onLengthXUpdate(this.getXLength());
        adapter.onLengthYUpdate(this.getYLength());
        adapter.onLengthZUpdate(this.getZLength());
        adapter.onRadiusSquaredUpdate(Utils.fixedPointIntToFloat4Bit((int)this.getRadiusSquared()));

        return adapter;
    }

    @Override
    public void setRawMatrixValue(DataReader reader, int rawMatrixValue) {
        this.matrix = new PSXMatrix();
        this.matrix.load(reader);
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