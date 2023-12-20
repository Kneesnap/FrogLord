package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
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

        Texture texture;
        if (data.getPolygonType().isTextured()) {
            OldFroggerLevelTableEntry levelTableEntry = getMap().getLevelTableEntry();
            VLOArchive mainArchive = levelTableEntry != null ? levelTableEntry.getMainVLOArchive() : null;
            TextureRemapArray textureRemap = levelTableEntry != null ? levelTableEntry.getTextureRemap() : null;
            Short globalTextureId = textureRemap != null ? textureRemap.getRemappedTextureId((int) data.getTextureId()) : null;

            // Lookup image source.
            GameImage imageSource = null;
            if (globalTextureId != null) {
                imageSource = mainArchive != null ? mainArchive.getImageByTextureId(globalTextureId) : null;
                if (imageSource == null)
                    imageSource = getMap().getArchive().getImageByTextureId(globalTextureId);
            }

            // Lookup texture from image source.
            texture = imageSource != null ? getMesh().getTextureAtlas().getNullTextureFromSource(imageSource) : null;
        } else if (data.getPolygonType().isGouraud()) {
            texture = getMesh().getGouarudPlaceholderTexture();
        } else {
            texture = getMesh().getFlatPlaceholderTexture();
        }

        // Use fallback texture if none found.
        if (texture == null)
            texture = getMesh().getTextureAtlas().getFallbackTexture();

        // Add texture UV.
        /*Vector2f uvBottomLeft = getMesh().getTextureAtlas().getUV(texture, Vector2f.UNIT_Y); // 0F, 1F
        Vector2f uvBottomRight = getMesh().getTextureAtlas().getUV(texture, Vector2f.ONE); // 1F, 1F
        Vector2f uvTopLeft = getMesh().getTextureAtlas().getUV(texture, Vector2f.ZERO); // 0F, 0F
        Vector2f uvTopRight = getMesh().getTextureAtlas().getUV(texture, Vector2f.UNIT_X); // 1F, 0F
        entry.addTexCoordValue(uvBottomLeft);
        entry.addTexCoordValue(uvBottomRight);
        entry.addTexCoordValue(uvTopLeft);
        entry.addTexCoordValue(uvTopRight);
         */

        // TODO: These names are incorrect, let's get them right.
        entry.addTexCoordValue(getTextureCoordinate(data, texture, 0, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F
        entry.addTexCoordValue(getTextureCoordinate(data, texture, 1, Vector2f.ONE)); // uvBottomRight, 1F, 1F
        entry.addTexCoordValue(getTextureCoordinate(data, texture, 2, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
        entry.addTexCoordValue(getTextureCoordinate(data, texture, 3, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F

        return entry;
    }

    private Vector2f getTextureCoordinate(OldFroggerMapPolygon polygon, Texture texture, int index, Vector2f fallback) {
        if (polygon.getPolygonType().isTextured()) {
            Vector2f localUv = polygon.getTextureUvs()[index].toVector(this.tempVector);
            localUv.setY(1F - localUv.getY()); // Flip texture vertically.

            return getMesh().getTextureAtlas().getUV(texture, localUv);
        } else {
            return getMesh().getTextureAtlas().getUV(texture, fallback);
        }
    }

    private void setupFaces(OldFroggerMapPolygon polygon) {
        DynamicMeshTypedDataEntry entry = getDataEntry(polygon);

        // Determine UV Indices.
        int uvIndex0 = entry.getTexCoordStartIndex();
        int uvIndex1 = entry.getTexCoordStartIndex() + 1;
        int uvIndex2 = entry.getTexCoordStartIndex() + 2;
        int uvIndex3 = entry.getTexCoordStartIndex() + 3;

        // Calculate vertices.
        int vtxIndex0 = entry.getVertexStartIndex();
        int vtxIndex1 = vtxIndex0 + 1;
        int vtxIndex2 = vtxIndex0 + 2;
        int vtxIndex3 = vtxIndex0 + 3;

        // JavaFX uses counter-clockwise winding order.
        //entry.addFace(vtxIndex2, uvIndex2, vtxIndex3, uvIndex3, vtxIndex0, uvIndex0);
        entry.addFace(vtxIndex2, uvIndex2, vtxIndex3, uvIndex3, vtxIndex0, uvIndex0);
        entry.addFace(vtxIndex3, uvIndex3, vtxIndex1, uvIndex1, vtxIndex0, uvIndex0);
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