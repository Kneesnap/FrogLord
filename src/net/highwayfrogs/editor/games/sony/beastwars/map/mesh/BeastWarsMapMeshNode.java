package net.highwayfrogs.editor.games.sony.beastwars.map.mesh;

import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

/**
 * A dynamic mesh node which can build the mesh for beast wars map tiles / vertices.
 * Created by Kneesnap on 9/25/2023.
 */
public class BeastWarsMapMeshNode extends DynamicMeshAdapterNode<BeastWarsMapVertex> {
    public BeastWarsMapMeshNode(BeastWarsMapMesh mesh) {
        super(mesh);
    }

    @Override
    public BeastWarsMapMesh getMesh() {
        return (BeastWarsMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        for (int z = 0; z < getMap().getHeightMapZLength(); z++)
            for (int x = 0; x < getMap().getHeightMapXLength(); x++)
                this.add(getMap().getVertex(x, z));

        for (int z = 0; z < getMap().getHeightMapZLength(); z++)
            for (int x = 0; x < getMap().getHeightMapXLength(); x++)
                this.setupFaces(getMap().getVertex(x, z));

        // Do last to use the recently configured mesh data.
        super.onAddedToMesh();
    }

    @Override
    protected void onRemovedFromMesh() {
        super.onRemovedFromMesh();
        // TODO: Let's automatically remove all data in the super method, just by having a method for it to get all of the entries.
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(BeastWarsMapVertex data) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), data);
        entry.addVertexValue(data.getWorldX(), data.getWorldY(), data.getWorldZ());

        if (data.hasTile()) {
            ITextureSource source = data.getTextureSource();
            Texture texture = getMesh().getTextureAtlas().getTextureFromSource(source);

            if (texture != null) {
                // The names of each are correct.
                Vector2f uvBottomLeft = getMesh().getTextureAtlas().getUV(texture, Vector2f.UNIT_Y); // 0F, 1F
                Vector2f uvBottomRight = getMesh().getTextureAtlas().getUV(texture, Vector2f.ONE); // 1F, 1F
                Vector2f uvTopLeft = getMesh().getTextureAtlas().getUV(texture, Vector2f.ZERO); // 0F, 0F
                Vector2f uvTopRight = getMesh().getTextureAtlas().getUV(texture, Vector2f.UNIT_X); // 1F, 0F
                entry.addTexCoordValue(uvBottomLeft.getX(), uvBottomLeft.getY());
                entry.addTexCoordValue(uvBottomRight.getX(), uvBottomRight.getY());
                entry.addTexCoordValue(uvTopLeft.getX(), uvTopLeft.getY());
                entry.addTexCoordValue(uvTopRight.getX(), uvTopRight.getY());
            }
        }

        return entry;
    }

    private void setupFaces(BeastWarsMapVertex vertex) {
        if (!vertex.hasTile())
            return;

        DynamicMeshTypedDataEntry entry = getDataEntry(vertex);

        // Determine UV Indices.
        int uvBottomLeftIndex = entry.getTexCoordStartIndex();
        int uvBottomRightIndex = entry.getTexCoordStartIndex() + 1;
        int uvTopLeftIndex = entry.getTexCoordStartIndex() + 2;
        int uvTopRightIndex = entry.getTexCoordStartIndex() + 3;

        // Calculate vertices.
        int vtxBottomLeftIndex = entry.getVertexStartIndex();
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
        if (localTexCoordIndex == 0) {
            // TODO: FINISH
        } else if (localTexCoordIndex == 1) {
            // TODO: FINISH
        }
        // TODO: !
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public BeastWarsMapFile getMap() {
        return getMesh().getMap();
    }
}