package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

/**
 * Represents a node in a map mesh for pre-recode frogger.
 * TODO: Vertices should be shared. UVs can probably be per-face.
 * Created by Kneesnap on 12/8/2023.
 */
public class OldFroggerMapMeshNode extends DynamicMeshAdapterNode<OldFroggerMapPolygon> {
    private final Vector2f tempVector = new Vector2f();

    public OldFroggerMapMeshNode(OldFroggerMapMesh mesh) {
        super(mesh);
    }

    @Override
    public OldFroggerMapMesh getMesh() {
        return (OldFroggerMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        for (OldFroggerMapGrid grid : getMap().getGridPacket().getGrids())
            for (OldFroggerMapPolygon polygon : grid.getPolygons())
                this.add(polygon);

        for (OldFroggerMapGrid grid : getMap().getGridPacket().getGrids())
            for (OldFroggerMapPolygon polygon : grid.getPolygons())
                this.setupFaces(polygon);

        /*for (OldFroggerMapPolygon polygon : getMap().getQuadPacket().getPolygons())
            this.add(polygon);

        for (OldFroggerMapPolygon polygon : getMap().getQuadPacket().getPolygons())
            this.setupFaces(polygon);*/

    }

    @Override
    protected void onRemovedFromMesh() {
        super.onRemovedFromMesh();
        // TODO: Let's automatically remove all data in the super method, just by having a method for it to get all of the entries.
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(OldFroggerMapPolygon data) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), data);

        // TODO: Vertices should be shared across all. (Debate different options ranging from putting all vertices in the first entry to making there always be one entry owned by the class, and not a specific node, to just having free-form management)
        for (int i = 0; i < data.getVertices().length; i++) {
            SVector vertexPos = getMap().getVertexPacket().getVertices().get(data.getVertices()[i]);
            entry.addVertexValue(vertexPos.getFloatX(), vertexPos.getFloatY(), vertexPos.getFloatZ());
        }

        ITextureSource textureSource = getMesh().getShadedTextureManager().getShadedTexture(data);
        Texture texture = getMesh().getTextureAtlas().getNullTextureFromSource(textureSource);

        /*// TODO: Once this is deemed reliable, we can just check the shaded texture manager.
        ITextureSource textureSource;
        Texture texture;
        if (data.getPolygonType().isTextured()) {
            GameImage imageSource = data.getTexture(getMap().getLevelTableEntry());

            // Lookup texture from image source.
            texture = imageSource != null ? getMesh().getTextureAtlas().getNullTextureFromSource(imageSource) : null;
        } else if ((textureSource = getMesh().getShadedTextureManager().getShadedTexture(data)) != null) {
            texture = getMesh().getTextureAtlas().getNullTextureFromSource(textureSource);
        } else if (data.getPolygonType().isGouraud()) {
            texture = getMesh().getGouarudPlaceholderTexture();
        } else {
            texture = getMesh().getFlatPlaceholderTexture();
        }*/

        // Use fallback texture if none found.
        if (texture == null)
            texture = getMesh().getTextureAtlas().getFallbackTexture();

        // Add texture UVs.
        entry.addTexCoordValue(getTextureCoordinate(data, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
        entry.addTexCoordValue(getTextureCoordinate(data, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
        entry.addTexCoordValue(getTextureCoordinate(data, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F
        entry.addTexCoordValue(getTextureCoordinate(data, texture, 3, Vector2f.ONE)); // uvBottomRight, 1F, 1F

        return entry;
    }

    private Vector2f getTextureCoordinate(OldFroggerMapPolygon polygon, Texture texture, int index, Vector2f fallback) {
        if (polygon.getPolygonType().isTextured()) {
            Vector2f localUv = polygon.getTextureUvs()[index].toVector(this.tempVector);
            localUv.setY(1F - localUv.getY()); // Flip texture vertically.

            return getMesh().getTextureAtlas().getUV(texture, localUv);
        } else {
            Vector2f localUv = this.tempVector.setXY(fallback);
            localUv.setY(1F - localUv.getY()); // UVs are flipped for generated shader textures too, in order to stay consistent.

            return getMesh().getTextureAtlas().getUV(texture, localUv);
        }
    }

    private void setupFaces(OldFroggerMapPolygon polygon) {
        DynamicMeshTypedDataEntry entry = getDataEntry(polygon);

        // Determine UV Indices.
        int uvIndex1 = entry.getTexCoordStartIndex();
        int uvIndex2 = entry.getTexCoordStartIndex() + 1;
        int uvIndex3 = entry.getTexCoordStartIndex() + 2;
        int uvIndex4 = entry.getTexCoordStartIndex() + 3;

        // Calculate vertices.
        int vtxIndex1 = entry.getVertexStartIndex();
        int vtxIndex2 = vtxIndex1 + 1;
        int vtxIndex3 = vtxIndex1 + 2;
        int vtxIndex4 = vtxIndex1 + 3;

        // JavaFX uses counter-clockwise winding order.
        entry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
        entry.addFace(vtxIndex3, uvIndex3, vtxIndex4, uvIndex4, vtxIndex2, uvIndex2);
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        OldFroggerMapPolygon vertex = entry.getDataSource();
        int vertexIndex = vertex.getVertices()[localVertexIndex];
        SVector vertexPos = getMap().getVertexPacket().getVertices().get(vertexIndex);
        entry.writeVertexXYZ(localVertexIndex, vertexPos.getFloatX(), vertexPos.getFloatY(), vertexPos.getFloatZ());
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        OldFroggerMapPolygon vertex = entry.getDataSource();
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
    public OldFroggerMapFile getMap() {
        return getMesh().getMap();
    }
}