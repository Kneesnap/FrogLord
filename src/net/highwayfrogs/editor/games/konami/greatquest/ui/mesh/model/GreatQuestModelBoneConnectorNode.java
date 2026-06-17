package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.wrapper.MeshEntryBox;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.system.math.Matrix4x4f;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.system.math.Vector3f;

/**
 * Connects various bones together.
 * Created by Kneesnap on 06/16/2026.
 */
public class GreatQuestModelBoneConnectorNode extends DynamicMeshAdapterNode<kcNode> {
    private final Vector2f tempUv = new Vector2f();
    private final Vector3f tempVertex = new Vector3f();

    private final Matrix4x4f lastLookAtMatrix = new Matrix4x4f();
    private final Vector3f lastBonePos = new Vector3f();
    private final Vector3f lastParentBonePos = new Vector3f();
    private float lastConnectorLength;

    private static final float SIZE = .003125f;
    private static final int VERTEX_COUNT = 8;
    private static final Vector3f VIEW_UP_DIRECTION = Vector3f.UNIT_Y;

    public GreatQuestModelBoneConnectorNode(GreatQuestModelSkeletonMesh mesh) {
        super(mesh);
    }

    @Override
    public GreatQuestModelSkeletonMesh getMesh() {
        return (GreatQuestModelSkeletonMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertex buffers matching this material.
        kcCResourceSkeleton skeleton = getMesh().getFullMesh().getSkeleton();
        if (skeleton != null)
            skeleton.getAllNodes().forEach(this::add);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcNode bone) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), bone);
        if (bone.getParent() == null)
            return entry; // The root node has no connector, since there's nothing previous to connect to.

        // Add box vertices.
        int vtxStartIndex = entry.getPendingVertexStartIndex();
        for (int i = 0; i < VERTEX_COUNT; i++)
            entry.addVertexValue(calculateVertexPos(bone, i));

        // Add UV.
        entry.addTexCoordValue(evaluateUvs(bone));

        // Create faces.
        int uvIndex = entry.getPendingTexCoordStartIndex();
        MeshEntryBox.addBoxFaces(entry, vtxStartIndex + 4, vtxStartIndex + 5,
                vtxStartIndex + 6, vtxStartIndex + 7,
                vtxStartIndex, vtxStartIndex + 1, vtxStartIndex + 2,
                vtxStartIndex + 3, uvIndex);
        return entry;
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        entry.writeVertexXYZ(localVertexIndex, calculateVertexPos(entry.getDataSource(), localVertexIndex));
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        entry.writeTexCoordValue(localTexCoordIndex, evaluateUvs(entry.getDataSource()));
    }

    private void setActiveBone(kcNode bone) {
        kcNode parent = bone.getParent();
        if (parent == null)
            return;

        Matrix4x4f boneTransform = getMesh().getFullMesh().getBoneTransform(bone.getTag());
        Matrix4x4f parentBoneTransform = getMesh().getFullMesh().getBoneTransform(parent.getTag());

        Vector3f bonePos = boneTransform.multiply(Vector3f.ZERO, this.lastBonePos);
        Vector3f parentBonePos = parentBoneTransform.multiply(Vector3f.ZERO, this.lastParentBonePos);
        Matrix4x4f.initialiseLookAtMatrix(parentBonePos, bonePos, Vector3f.UNIT_Y, Vector3f.UNIT_Z, this.lastLookAtMatrix);
        this.lastConnectorLength = (float) this.tempVertex.setXYZ(bonePos).subtract(parentBonePos).calculateLength();
    }

    private Vector3f calculateVertexPos(kcNode bone, int vertexId) {
        kcNode parent = bone.getParent();
        if (parent == null)
            return this.tempVertex.setXYZ(Vector3f.ZERO);

        setActiveBone(bone);
        switch (vertexId) {
            case 0: // Z- Y- X-
                this.tempVertex.setXYZ(0, -SIZE, -SIZE);
                break;
            case 1: // Z- Y- X+
                this.tempVertex.setXYZ(this.lastConnectorLength, -SIZE, -SIZE);
                break;
            case 2: // Z- Y+ X-
                this.tempVertex.setXYZ(0, SIZE, -SIZE);
                break;
            case 3: // Z- Y+ X+
                this.tempVertex.setXYZ(this.lastConnectorLength, SIZE, -SIZE);
                break;
            case 4: // Z+ Y- X-
                this.tempVertex.setXYZ(0, -SIZE, SIZE);
                break;
            case 5: // Z+ Y- X+
                this.tempVertex.setXYZ(this.lastConnectorLength, -SIZE, SIZE);
                break;
            case 6: // Z+ Y+ X-
                this.tempVertex.setXYZ(0, SIZE, SIZE);
                break;
            case 7: // Z+ Y+ X+
                this.tempVertex.setXYZ(this.lastConnectorLength, SIZE, SIZE);
                break;
        }

        return this.lastLookAtMatrix.multiply(this.tempVertex, this.tempVertex);
    }

    private Vector2f evaluateUvs(kcNode bone) {
        kcNode selectedBone = getMesh().getSelectedBone();

        AtlasTexture texture = getMesh().getDefaultConnectorTexture();
        if (selectedBone != null && selectedBone == bone)
            texture = getMesh().getSelectedBoneTexture();

        return getMesh().getTextureAtlas().getUV(texture, this.tempUv.setXY(.5F, .5F), this.tempUv);
    }
}
