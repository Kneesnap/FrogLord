package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelPrim;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcVertex;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;

import java.util.List;

/**
 * Manages mesh data for a model in the Great Quest.
 * Created by Kneesnap on 4/15/2024.
 */
public class GreatQuestModelMaterialMeshNode extends DynamicMeshAdapterNode<kcModelPrim> {
    public GreatQuestModelMaterialMeshNode(GreatQuestModelMaterialMesh mesh) {
        super(mesh);
    }

    @Override
    public GreatQuestModelMaterialMesh getMesh() {
        return (GreatQuestModelMaterialMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertex buffers matching this material.
        List<kcMaterial> materials = getModel().getMaterials();
        for (kcModelPrim modelPrim : getModel().getPrimitives()) {
            int materialId = (int) modelPrim.getMaterialId();
            kcMaterial material = materialId >= 0 && materials.size() > materialId ? materials.get(materialId) : null;
            if (material == getMesh().getGameMaterial())
                this.add(modelPrim);
        }
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcModelPrim modelPrim) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), modelPrim);

        // Write vertices and uvs.
        for (int i = 0; i < modelPrim.getVertices().size(); i++) {
            kcVertex vertex = modelPrim.getVertices().get(i);
            entry.addVertexValue(vertex.getX(), vertex.getZ(), vertex.getY());
            entry.addTexCoordValue(vertex.getU0(), -vertex.getV0());
        }

        // Write face data.
        int uvStartIndex = entry.getTexCoordStartIndex();
        int vtxStartIndex = entry.getVertexStartIndex();
        switch (modelPrim.getPrimitiveType()) {
            case TRIANGLE_LIST:
                writeTriangleList(entry, uvStartIndex, vtxStartIndex, (int) modelPrim.getVertexCount());
                break;
            case TRIANGLE_STRIP:
                writeTriangleStrip(entry, uvStartIndex, vtxStartIndex, (int) modelPrim.getVertexCount());
                break;
            default:
                getLogger().severe("kcModel had a primitive of type '" + modelPrim.getPrimitiveType() + "', which was supposed because it was unsupported.");
        }

        return entry;
    }

    private void writeTriangleList(DynamicMeshTypedDataEntry entry, int uvStartIndex, int vtxStartIndex, int vertexCount) {
        for (int i = 0; i < vertexCount; i += 3)
            entry.addFace(vtxStartIndex + i + 2, uvStartIndex + i + 2, vtxStartIndex + i + 1, uvStartIndex + i + 1, vtxStartIndex + i, uvStartIndex + i);
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

            entry.addFace(vtxStartIndex + i + 2, uvStartIndex + i + 2, vtx2, uv2, vtx1, uv1);
        }
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        kcVertex vertex = entry.getDataSource().getVertices().get(localVertexIndex);
        entry.writeVertexXYZ(localVertexIndex, vertex.getX(), vertex.getZ(), vertex.getY());
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        if (localTexCoordIndex < 0 || localTexCoordIndex >= entry.getDataSource().getVertexCount())
            throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);

        kcVertex vertex = entry.getDataSource().getVertices().get(localTexCoordIndex);
        entry.writeTexCoordValue(localTexCoordIndex, vertex.getU0(), -vertex.getV0());
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public kcModel getModel() {
        return getMesh().getModel();
    }
}