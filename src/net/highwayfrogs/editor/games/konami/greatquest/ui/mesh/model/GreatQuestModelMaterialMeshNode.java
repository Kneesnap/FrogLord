package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcMatrix;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelNode;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelPrim;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcVertex;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.system.math.Matrix4x4f;
import net.highwayfrogs.editor.system.math.Vector3f;

import java.util.List;

/**
 * Manages mesh data for a model in the Great Quest.
 * Created by Kneesnap on 4/15/2024.
 */
public class GreatQuestModelMaterialMeshNode extends DynamicMeshAdapterNode<kcModelPrim> {
    @NonNull private final List<kcModelPrim> modelPrims;
    private final Vector3f tempVertex = new Vector3f();
    private final Vector3f tempWeighedVertex = new Vector3f();
    private final Vector3f tempTransformedVertex = new Vector3f();

    public GreatQuestModelMaterialMeshNode(GreatQuestModelMaterialMesh mesh, List<kcModelPrim> modelPrims) {
        super(mesh);
        this.modelPrims = modelPrims;
    }

    @Override
    public GreatQuestModelMaterialMesh getMesh() {
        return (GreatQuestModelMaterialMesh) super.getMesh();
    }

    /**
     * Gets the skeleton used to draw this model.
     */
    public kcCResourceSkeleton getSkeleton() {
        return getMesh().getFullMesh().getSkeleton();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertex buffers matching this material.
        for (int i = 0; i < this.modelPrims.size(); i++)
            this.add(this.modelPrims.get(i));
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcModelPrim modelPrim) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), modelPrim);

        // Write vertices and uvs.
        for (int i = 0; i < modelPrim.getVertices().size(); i++) {
            kcVertex vertex = modelPrim.getVertices().get(i);
            entry.addVertexValue(calculateVertexPos(modelPrim, i));
            entry.addTexCoordValue(vertex.getU0(), -vertex.getV0());
        }

        // Write face data.
        int uvStartIndex = entry.getPendingTexCoordStartIndex();
        int vtxStartIndex = entry.getPendingVertexStartIndex();
        switch (modelPrim.getPrimitiveType()) {
            case TRIANGLE_LIST:
                writeTriangleList(entry, uvStartIndex, vtxStartIndex, (int) modelPrim.getVertexCount());
                break;
            case TRIANGLE_STRIP:
                writeTriangleStrip(entry, uvStartIndex, vtxStartIndex, (int) modelPrim.getVertexCount());
                break;
            default:
                getLogger().severe("kcModel had a primitive of type '%s', which was supposed because it was unsupported.", modelPrim.getPrimitiveType());
        }

        return entry;
    }

    private void writeTriangleList(DynamicMeshTypedDataEntry entry, int uvStartIndex, int vtxStartIndex, int vertexCount) {
        for (int i = 0; i < vertexCount; i += 3)
            entry.addFace(vtxStartIndex + i, uvStartIndex + i, vtxStartIndex + i + 1, uvStartIndex + i + 1, vtxStartIndex + i + 2, uvStartIndex + i + 2);
    }

    private  void writeTriangleStrip(DynamicMeshTypedDataEntry entry, int uvStartIndex, int vtxStartIndex, int vertexCount) {
        for (int i = 0; i < vertexCount - 2; i++) {
            int vtx1 = vtxStartIndex + i;
            int uv1 = uvStartIndex + i;
            int vtx2 = vtxStartIndex + i + 1;
            int uv2 = uvStartIndex + i + 1;

            if (i % 2 > 0) { // Alternate the indices so faces always orient consistently
                int temp = vtx1;
                vtx1 = vtx2;
                vtx2 = temp;
                temp = uv1;
                uv1 = uv2;
                uv2 = temp;
            }

            entry.addFace(vtx1, uv1, vtx2, uv2, vtxStartIndex + i + 2, uvStartIndex + i + 2);
        }
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        entry.writeVertexXYZ(localVertexIndex, calculateVertexPos(entry.getDataSource(), localVertexIndex));
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        if (localTexCoordIndex < 0 || localTexCoordIndex >= entry.getDataSource().getVertexCount())
            throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);

        kcVertex vertex = entry.getDataSource().getVertices().get(localTexCoordIndex);
        entry.writeTexCoordValue(localTexCoordIndex, vertex.getU0(), -vertex.getV0());
    }

    private Vector3f calculateVertexPos(kcModelPrim modelPrim, int localVertexIndex) {
        kcVertex vertex = modelPrim.getVertices().get(localVertexIndex);
        Vector3f localPos = this.tempVertex.setXYZ(vertex.getX(), vertex.getY(), vertex.getZ()); // Scaling must happen first for animations to apply at the right pivot points.

        Vector3f result = this.tempTransformedVertex.setXYZ(localPos);
        kcCResourceSkeleton skeleton = getSkeleton();
        if (skeleton != null) {
            GreatQuestModelMesh fullMesh = getMesh().getFullMesh();
            result = result.setXYZ(0, 0, 0); // The result is a sum of weighed bones.

            // Add bone.
            float weight0 = vertex.getWeight() != null && vertex.getWeight().length > 0 ? vertex.getWeight()[0] : 1F;
            float weight1 = (1F - weight0);

            Vector3f tmpWeightedVtx = this.tempWeighedVertex;
            kcModelNode parentNode = modelPrim.getParentNode();
            if (parentNode != null && parentNode.getNodeId() >= 0) {
                Matrix4x4f tempMatrix = fullMesh.getFinalBoneTransform(parentNode.getNodeId());
                result.add(kcMatrix.kcMatrixMulVector(tempMatrix, localPos, tmpWeightedVtx).multiplyScalar(weight0));
            }

            if (modelPrim.getBoneIds() != null && modelPrim.getBoneIds().length > 0) {
                short boneId = modelPrim.getBoneIds()[0];
                if (boneId != 0) { // kcModelTransformRender()
                    Matrix4x4f tempMatrix = fullMesh.getFinalBoneTransform(boneId);
                    result.add(kcMatrix.kcMatrixMulVector(tempMatrix, localPos, tmpWeightedVtx).multiplyScalar(weight1));
                }
            }
        }

        return result;
    }
}