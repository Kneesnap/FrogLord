package net.highwayfrogs.editor.games.sony.beastwars.map.mesh;

import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

/**
 * A dynamic mesh node which can build the mesh for beast wars map tiles / vertices.
 * Created by Kneesnap on 9/25/2023.
 */
public class BeastWarsMapMeshNode extends DynamicMeshAdapterNode<BeastWarsMapVertex> {
    private final Vector2f tempVector = new Vector2f();

    public BeastWarsMapMeshNode(BeastWarsMapMesh mesh) {
        super(mesh);
    }

    @Override
    public BeastWarsMapMesh getMesh() {
        return (BeastWarsMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        for (int z = 0; z < getMap().getHeightMapZLength(); z++)
            for (int x = 0; x < getMap().getHeightMapXLength(); x++)
                this.add(getMap().getVertex(x, z));

        // Ensure the vertex & texCoord ids are accessible to the face data.
        getMesh().getEditableVertices().forceApplyToFxArray();
        getMesh().getEditableTexCoords().forceApplyToFxArray();

        // After the vertices are set, lets try to create the faces.
        for (int z = 0; z < getMap().getHeightMapZLength(); z++)
            for (int x = 0; x < getMap().getHeightMapXLength(); x++)
                this.setupFaces(getMap().getVertex(x, z));
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(BeastWarsMapVertex vertex) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), vertex);
        entry.addVertexValue(vertex.getWorldX(), vertex.getWorldY(), vertex.getWorldZ());

        if (vertex.hasTile()) {
            ITextureSource source = vertex.getTextureSource();
            Texture texture = getMesh().getTextureAtlas().getTextureFromSource(source);

            if (texture != null) {
                PSXShadeTextureDefinition shadeDef = getMesh().getShadedTextureManager().getShadedTexture(vertex);
                Texture shadedTexture = getMesh().getTextureAtlas().getTextureFromSource(shadeDef);
                SCByteTextureUV[] uvs = shadeDef.getTextureUVs();
                for (int i = 0; i < uvs.length; i++)
                    entry.addTexCoordValue(getMesh().getTextureAtlas().getUV(shadedTexture, uvs[i].toVector(this.tempVector)));
            }
        }

        return entry;
    }

    private void setupFaces(BeastWarsMapVertex vertex) {
        if (!vertex.hasTile())
            return;

        DynamicMeshTypedDataEntry entry = getDataEntry(vertex);

        // Determine UV Indices.
        int uvBottomLeftIndex = entry.getPendingTexCoordStartIndex();
        int uvBottomRightIndex = entry.getPendingTexCoordStartIndex() + 1;
        int uvTopLeftIndex = entry.getPendingTexCoordStartIndex() + 2;
        int uvTopRightIndex = entry.getPendingTexCoordStartIndex() + 3;

        // Calculate vertices.
        int vtxBottomLeftIndex = entry.getPendingVertexStartIndex();
        int vtxBottomRightIndex = vtxBottomLeftIndex + 1;
        int vtxTopLeftIndex = vtxBottomLeftIndex + getMap().getHeightMapXLength();
        int vtxTopRightIndex = vtxTopLeftIndex + 1;

        // JavaFX uses counter-clockwise winding order.
        entry.addFace(vtxBottomLeftIndex, uvBottomLeftIndex, vtxTopRightIndex, uvTopRightIndex, vtxTopLeftIndex, uvTopLeftIndex);
        entry.addFace(vtxBottomLeftIndex, uvBottomLeftIndex, vtxBottomRightIndex, uvBottomRightIndex, vtxTopRightIndex, uvTopRightIndex);
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        BeastWarsMapVertex vertex = entry.getDataSource();
        entry.writeVertexXYZ(localVertexIndex, vertex.getWorldX(), vertex.getWorldY(), vertex.getWorldZ());
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        BeastWarsMapVertex vertex = entry.getDataSource();
        PSXShadeTextureDefinition shadeDef = getMesh().getShadedTextureManager().getShadedTexture(vertex);
        Texture shadedTexture = getMesh().getTextureAtlas().getTextureFromSource(shadeDef);
        SCByteTextureUV[] textureUvs = shadeDef.getTextureUVs();
        Vector2f textureUv = getMesh().getTextureAtlas().getUV(shadedTexture, textureUvs[localTexCoordIndex % textureUvs.length].toVector(this.tempVector), this.tempVector);
        entry.writeTexCoordValue(localTexCoordIndex, textureUv);
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public BeastWarsMapFile getMap() {
        return getMesh().getMap();
    }
}