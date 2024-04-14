package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcVertex;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResOctTreeSceneMgr;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResOctTreeSceneMgr.kcVtxBufFileStruct;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Manages map terrain for a model in the Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestMapMeshNode extends DynamicMeshAdapterNode<kcVtxBufFileStruct> {
    private final Vector2f tempVector = new Vector2f();

    public GreatQuestMapMeshNode(GreatQuestMapMesh mesh) {
        super(mesh);
    }

    @Override
    public GreatQuestMapMesh getMesh() {
        return (GreatQuestMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertex buffers.
        for (kcVtxBufFileStruct vertexBuffer : getSceneManager().getVertexBuffers())
            this.add(vertexBuffer);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcVtxBufFileStruct vtxBuf) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), vtxBuf);
        Texture texture = getTexture(entry.getDataSource());

        // Write vertices and uvs.
        for (int i = 0; i < vtxBuf.getVertices().size(); i++) {
            kcVertex vertex = vtxBuf.getVertices().get(i);
            entry.addVertexValue(vertex.getX(), -vertex.getY(), vertex.getZ());
            entry.addTexCoordValue(getTextureCoordinate(vertex, texture, 0));
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
                System.out.println("kcCResOctTreeSceneMgr had a prim of type '" + vtxBuf.getPrimitiveType() + "', which was supposed because it was unsupported.");
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

    private Texture getTexture(kcVtxBufFileStruct vtxBuf) {
        int materialId = (int) vtxBuf.getMaterialId();
        List<kcMaterial> materials = getSceneManager().getMaterials();
        kcMaterial material = materialId >= 0 && materials.size() > materialId ? materials.get(materialId) : null;
        return getMesh().getTextureAtlas().getTextureFromSourceOrFallback(material != null ? material.getTexture() : null);
    }

    private Vector2f getTextureCoordinate(kcVertex vertex, Texture texture, int index) {
        if (index == 0) {
            this.tempVector.setXY(clamp(vertex.getU0()), clamp(vertex.getV0()));
        } else if (index == 1) {
            this.tempVector.setXY(clamp(vertex.getU1()), clamp(vertex.getV1()));
        } else {
            throw new ArrayIndexOutOfBoundsException("Index must either be 0 or 1, but was: " + index);
        }

        // Get the UVs local to the texture.
        return getMesh().getTextureAtlas().getUV(texture, this.tempVector);
    }

    private float clamp(float value) {
        // TODO: Unfortunately I think we need to start subdividing polygons for this to work. We need to emulate texture wrapping witha texture sheet. Fk.
        // TODO: Try ideas like just adding nearby neighbors in the texture sheet, or making a mesh for each texture to prove this theory.
        value %= 1F;
        if (value < 0F)
            value += 1F;

        return value;
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        kcVertex vertex = entry.getDataSource().getVertices().get(localVertexIndex);
        entry.writeVertexXYZ(localVertexIndex, vertex.getX(), -vertex.getY(), vertex.getZ());

        // Do nothing else, no other entries are given vertices.
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        if (localTexCoordIndex < 0 || localTexCoordIndex >= entry.getDataSource().getVertexCount())
            throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);

        kcVertex vertex = entry.getDataSource().getVertices().get(localTexCoordIndex);
        Texture texture = getTexture(entry.getDataSource());
        entry.writeTexCoordValue(localTexCoordIndex, getTextureCoordinate(vertex, texture, 0));
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public GreatQuestChunkedFile getMap() {
        return getMesh().getMap();
    }

    /**
     * Gets the scene manager for the map
     */
    public kcCResOctTreeSceneMgr getSceneManager() {
        return getMap().getResourceByHash(kcCResOctTreeSceneMgr.LEVEL_RESOURCE_HASH);
    }
}