package net.highwayfrogs.editor.gui.mesh.fxobject;

import lombok.Getter;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshNode;
import net.highwayfrogs.editor.gui.mesh.wrapper.MeshEntryBox;
import net.highwayfrogs.editor.system.math.Vector2f;

/**
 * Represents a box.
 * Created by Kneesnap on 4/15/2024.
 */
public class BoxMeshNode extends DynamicMeshNode {
    private double startWidth = 1;
    private double startHeight = 1;
    private double startDepth = 1;
    private DynamicMeshDataEntry meshEntry;
    @Getter private MeshEntryBox meshBox;

    public BoxMeshNode(DynamicMesh mesh) {
        this(mesh, 1, 1, 1);
    }

    public BoxMeshNode(DynamicMesh mesh, double startingWidth, double startingHeight, double startingDepth) {
        super(mesh);
        this.startWidth = startingWidth;
        this.startHeight = startingHeight;
        this.startDepth = startingDepth;
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();
        createMeshEntry();
    }

    private void createMeshEntry() {
        DynamicMeshDataEntry meshEntry = new DynamicMeshDataEntry(getMesh());

        int uvTopLeft = meshEntry.addTexCoordValue(Vector2f.ZERO);
        int uvTopRight = meshEntry.addTexCoordValue(Vector2f.UNIT_X);
        int uvBottomLeft = meshEntry.addTexCoordValue(Vector2f.UNIT_Y);
        int uvBottomRight = meshEntry.addTexCoordValue(Vector2f.ONE);

        this.meshBox = MeshEntryBox.createCenteredBoxEntry(meshEntry, 0, 0, 0, this.startWidth, this.startHeight, this.startDepth, uvTopLeft, uvTopRight, uvBottomLeft, uvBottomRight);
        addUnlinkedEntry(meshEntry);
        this.meshEntry = meshEntry;
    }

    @Override
    public boolean updateTexCoord(DynamicMeshDataEntry entry, int localTexCoordIndex) {
        if (entry != this.meshEntry)
            return false;

        return false;
    }

    @Override
    public boolean updateVertex(DynamicMeshDataEntry entry, int localVertexIndex) {
        if (entry != this.meshEntry)
            return false;

        return false;
    }
}