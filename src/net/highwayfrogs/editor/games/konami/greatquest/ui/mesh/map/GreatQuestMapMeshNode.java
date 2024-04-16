package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcVertex;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResOctTreeSceneMgr.kcVtxBufFileStruct;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Manages map terrain for a model in the Great Quest.
 * WARNING: THIS IS DISABLED. IT GIVES A ROUGH DISPLAY OF THE LEVEL, BUT CAN'T ACCOUNT FOR WRAPPING TEXTURES DUE TO THE USE OF A TEXTURE SHEET.
 * USE THE OTHER MESH/MESH NODE INSTEAD.
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
        // This has been disabled because it isn't possible to easily show the texture vertices properly without splitting it into several meshes by material.
        for (kcVtxBufFileStruct vertexBuffer : getMap().getSceneManager().getVertexBuffers())
            this.add(vertexBuffer);
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(kcVtxBufFileStruct vtxBuf) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), vtxBuf);
        Texture texture = getTexture(entry.getDataSource());

        // Write vertices and uvs.
        for (int i = 0; i < vtxBuf.getVertices().size(); i++) {
            kcVertex vertex = vtxBuf.getVertices().get(i);
            entry.addVertexValue(vertex.getX(), vertex.getY(), vertex.getZ());
            entry.addTexCoordValue(getTextureCoordinate(vertex, texture));
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
        List<kcMaterial> materials = getMap().getSceneManager().getMaterials();
        kcMaterial material = materialId >= 0 && materials.size() > materialId ? materials.get(materialId) : null;
        return getMesh().getTextureAtlas().getTextureFromSourceOrFallback(material != null ? material.getTexture() : null);
    }

    private Vector2f getTextureCoordinate(kcVertex vertex, Texture texture) {
        this.tempVector.setXY(clamp(vertex.getU0(), false), clamp(vertex.getV0(), true));

        // Get the UVs local to the texture.
        return getMesh().getTextureAtlas().getUV(texture, this.tempVector);
    }

    private float clamp(float value, boolean flip) {
        if (value >= 0) {
            return flip ? (1F - (value % 1F)) : (value % 1F);
        } else {
            float result = (value % 1F) + 1F;
            return flip ? result : 1F - result;
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
        Texture texture = getTexture(entry.getDataSource());
        entry.writeTexCoordValue(localTexCoordIndex, getTextureCoordinate(vertex, texture));
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public GreatQuestChunkedFile getMap() {
        return getMesh().getMap();
    }
}