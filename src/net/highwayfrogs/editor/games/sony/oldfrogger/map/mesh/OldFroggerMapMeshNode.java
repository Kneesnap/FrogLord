package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Represents a node in a map mesh for pre-recode frogger.
 * Created by Kneesnap on 12/8/2023.
 */
public class OldFroggerMapMeshNode extends DynamicMeshAdapterNode<OldFroggerMapPolygon> {
    private final Vector2f tempVector = new Vector2f();
    private DynamicMeshDataEntry vertexEntry;

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

        // Setup vertices.
        this.vertexEntry = new DynamicMeshDataEntry(getMesh());
        List<SVector> vertices = getMap().getVertexPacket().getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            SVector vertex = vertices.get(i);
            this.vertexEntry.addVertexValue(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ());
        }
        addUnlinkedEntry(this.vertexEntry);

        // Setup polygons.
        for (OldFroggerMapGrid grid : getMap().getGridPacket().getGrids())
            for (OldFroggerMapPolygon polygon : grid.getPolygons())
                this.add(polygon);
    }

    @Override
    public void clear() {
        super.clear();
        this.vertexEntry = null;
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(OldFroggerMapPolygon data) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), data);

        // Resolve texture.
        ITextureSource textureSource = getMesh().getShadedTextureManager().getShadedTexture(data);
        Texture texture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureSource);

        // Add texture UVs.
        int uvIndex1 = entry.addTexCoordValue(getTextureCoordinate(data, textureSource, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
        int uvIndex2 = entry.addTexCoordValue(getTextureCoordinate(data, textureSource, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
        int uvIndex3 = entry.addTexCoordValue(getTextureCoordinate(data, textureSource, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F
        int uvIndex4 = entry.addTexCoordValue(getTextureCoordinate(data, textureSource, texture, 3, Vector2f.ONE)); // uvBottomRight, 1F, 1F

        // Vertice IDs are the same IDs seen in the map data.
        int vtxIndex1 = this.vertexEntry.getVertexStartIndex() + data.getVertices()[0];
        int vtxIndex2 = this.vertexEntry.getVertexStartIndex() + data.getVertices()[1];
        int vtxIndex3 = this.vertexEntry.getVertexStartIndex() + data.getVertices()[2];
        int vtxIndex4 = this.vertexEntry.getVertexStartIndex() + data.getVertices()[3];

        // JavaFX uses counter-clockwise winding order.
        entry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
        entry.addFace(vtxIndex3, uvIndex3, vtxIndex4, uvIndex4, vtxIndex2, uvIndex2);

        return entry;
    }

    private Vector2f getTextureCoordinate(OldFroggerMapPolygon polygon, ITextureSource textureSource, Texture texture, int index, Vector2f fallback) {
        Vector2f localUv;
        if (polygon.getPolygonType().isTextured()) {
            localUv = polygon.getTextureUvs()[index].toVector(this.tempVector);
        } else {
            localUv = this.tempVector.setXY(fallback);
        }

        // Shaded untextured gouraud images generated by FrogLord include a small amount of padding.
        // This padding prevents bleeding colors from nearby textures.
        // Frogger does this normally for its textured polygons, but because untextured polygons don't have UVs,
        // it's our job to handle it here.
        boolean shadedTextureNeedsFlipping = false;
        if (textureSource instanceof PSXShadeTextureDefinition) {
            PSXShadeTextureDefinition shadeTexture = (PSXShadeTextureDefinition) textureSource;
            if (!shadeTexture.getPolygonType().isTextured()) {
                float minValue = (float) texture.getLeftPadding() / texture.getPaddedWidth();
                float maxValue = (float) (texture.getLeftPadding() + texture.getWidth()) / texture.getPaddedWidth();
                if (localUv.getX() == 0F)
                    localUv.setX(minValue);
                if (localUv.getX() == 1F)
                    localUv.setX(maxValue);
                if (localUv.getY() == 0F)
                    localUv.setY(minValue);
                if (localUv.getY() == 1F)
                    localUv.setY(maxValue);
            } else {
                shadedTextureNeedsFlipping = true;
            }
        }

        // Map textures seem to be flipped vertically.
        // Shaded textures only need flipping if they are using real textures (real UVs).
        // The reason is that the UVs in the map file are inverted, but when we use our own UVs (necessary for shaded un-texturedp polygons which don't have UVs),
        // since we choose the UVs, we provide the correct ones.
        if (shadedTextureNeedsFlipping || textureSource instanceof GameImage)
            localUv.setY(1F - localUv.getY()); // UVs are flipped for generated shader textures too, in order to stay consistent.

        // Get the UVs local to the texture.
        return getMesh().getTextureAtlas().getUV(texture, localUv);
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        // Do nothing, the typed entries don't contain any vertices.
    }

    @Override
    public boolean updateVertex(DynamicMeshDataEntry entry, int localVertexIndex) {
        if (entry == this.vertexEntry) {
            SVector vertexPos = getMap().getVertexPacket().getVertices().get(localVertexIndex);
            this.vertexEntry.writeVertexXYZ(localVertexIndex, vertexPos.getFloatX(), vertexPos.getFloatY(), vertexPos.getFloatZ());
            return true;
        }

        return super.updateVertex(entry, localVertexIndex);
    }

    /**
     * Updates a map vertex index.
     * @param mapVertexIndex index of the map vertex to update
     */
    public void updateMapVertex(int mapVertexIndex) {
        updateVertex(this.vertexEntry, mapVertexIndex);
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        OldFroggerMapPolygon polygon = entry.getDataSource();
        ITextureSource textureSource = getMesh().getShadedTextureManager().getShadedTexture(polygon);
        Texture texture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureSource);

        Vector2f uv;
        switch (localTexCoordIndex) {
            case 0:
                uv = getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO); // uvTopLeft, 0F, 0F
                break;
            case 1:
                uv = getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X); // uvTopRight, 1F, 0F
                break;
            case 2:
                uv = getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y); // uvBottomLeft, 0F, 1F
                break;
            case 3:
                uv = getTextureCoordinate(polygon, textureSource, texture, 3, Vector2f.ONE); // uvBottomRight, 1F, 1F
                break;
            default:
                throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);
        }

        entry.writeTexCoordValue(localTexCoordIndex, uv);
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public OldFroggerMapFile getMap() {
        return getMesh().getMap();
    }
}