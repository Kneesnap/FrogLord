package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr.kcVtxBufFileStruct;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcVertex;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;

import java.util.List;

/**
 * Manages map terrain for a model in the Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestMapMaterialMeshNode extends DynamicMeshAdapterNode<kcVtxBufFileStruct> {
    private final kcVtxBufFileStruct highlightedVertexBuffer;

    public GreatQuestMapMaterialMeshNode(GreatQuestMapMaterialMesh mesh) {
        this(mesh, null);
    }

    public GreatQuestMapMaterialMeshNode(GreatQuestMapMaterialMesh mesh, kcVtxBufFileStruct highlightedVertexBuffer) {
        super(mesh);
        this.highlightedVertexBuffer = highlightedVertexBuffer;
    }

    @Override
    public GreatQuestMapMaterialMesh getMesh() {
        return (GreatQuestMapMaterialMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertex buffers matching this material.
        if (this.highlightedVertexBuffer != null) {
            add(this.highlightedVertexBuffer);
        } else {
            kcCResOctTreeSceneMgr sceneMgr = getMap().getSceneManager();
            List<kcVtxBufFileStruct> vertexBuffers = sceneMgr.getVertexBuffersForMaterial(getMesh().getMapMaterial());
            if (vertexBuffers != null)
                vertexBuffers.forEach(this::add);
        }
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcVtxBufFileStruct vtxBuf) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), vtxBuf);

        // Write vertices and uvs.
        for (int i = 0; i < vtxBuf.getVertices().size(); i++) {
            kcVertex vertex = vtxBuf.getVertices().get(i);
            entry.addVertexValue(vertex.getX(), vertex.getY(), vertex.getZ());
            entry.addTexCoordValue(vertex.getU0(), -vertex.getV0());
        }

        // Write face data.
        int uvStartIndex = entry.getTexCoordStartIndex();
        int vtxStartIndex = entry.getVertexStartIndex();
        switch (vtxBuf.getPrimitiveType()) {
            case TRIANGLE_LIST:
                writeTriangleList(entry, uvStartIndex, vtxStartIndex, vtxBuf.getVertexCount());
                break;
            case TRIANGLE_STRIP:
                writeTriangleStrip(entry, uvStartIndex, vtxStartIndex, vtxBuf.getVertexCount());
                break;
            default:
                getLogger().severe("kcCResOctTreeSceneMgr had a prim of type '" + vtxBuf.getPrimitiveType() + "', which was supposed because it was unsupported.");
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
        kcVertex vertex = entry.getDataSource().getVertices().get(localVertexIndex);
        entry.writeVertexXYZ(localVertexIndex, vertex.getX(), vertex.getY(), vertex.getZ());
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
    public GreatQuestChunkedFile getMap() {
        return getMesh().getMap();
    }
}