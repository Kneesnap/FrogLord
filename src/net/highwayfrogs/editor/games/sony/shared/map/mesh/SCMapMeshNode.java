package net.highwayfrogs.editor.games.sony.shared.map.mesh;

import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Represents a node in a map mesh for MediEvil.
 * Created by Kneesnap on 5/8/2024.
 */
public class SCMapMeshNode extends DynamicMeshAdapterNode<SCMapPolygon> {
    private final Vector2f tempVector = new Vector2f();
    private final SCByteTextureUV tempByteTextureUv = new SCByteTextureUV();
    private DynamicMeshDataEntry vertexEntry;

    public SCMapMeshNode(SCMapMesh mesh) {
        super(mesh);
    }

    @Override
    public SCMapMesh getMesh() {
        return (SCMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertices.
        this.vertexEntry = new DynamicMeshDataEntry(getMesh());
        SCMapPolygonPacket<?> polygonPacket = getMap().getPolygonPacket();
        List<SVector> vertices = polygonPacket.getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            SVector vertex = vertices.get(i);
            this.vertexEntry.addVertexValue(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ());
        }
        addUnlinkedEntry(this.vertexEntry);

        // Setup polygons.
        // First, setup the non-transparent polygons.
        ISCLevelTableEntry levelTableEntry = getMap().getLevelTableEntry();
        for (SCMapPolygon polygon : polygonPacket.getPolygons())
            if (!polygon.isSemiTransparent(levelTableEntry))
                this.add(polygon);

        // Second, add the transparent polygons.
        for (SCMapPolygon polygon : polygonPacket.getPolygons())
            if (polygon.isSemiTransparent(levelTableEntry))
                this.add(polygon);
    }

    @Override
    public void clear() {
        super.clear();
        this.vertexEntry = null;
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(SCMapPolygon data) {
        DynamicMeshTypedDataEntry entry = new DynamicMeshTypedDataEntry(getMesh(), data);

        // Resolve texture.
        ITextureSource textureSource = getMesh().getShadedTextureManager().getShadedTexture(data);
        Texture texture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureSource);

        // Add texture UVs.
        if (data.getVertexCount() == 4) {
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
        } else {
            int uvIndex1 = entry.addTexCoordValue(getTextureCoordinate(data, textureSource, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
            int uvIndex2 = entry.addTexCoordValue(getTextureCoordinate(data, textureSource, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
            int uvIndex3 = entry.addTexCoordValue(getTextureCoordinate(data, textureSource, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F

            // Vertice IDs are the same IDs seen in the map data.
            int vtxIndex1 = this.vertexEntry.getVertexStartIndex() + data.getVertices()[0];
            int vtxIndex2 = this.vertexEntry.getVertexStartIndex() + data.getVertices()[1];
            int vtxIndex3 = this.vertexEntry.getVertexStartIndex() + data.getVertices()[2];

            // JavaFX uses counter-clockwise winding order.
            entry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
        }

        return entry;
    }

    private Vector2f getTextureCoordinate(SCMapPolygon polygon, ITextureSource textureSource, Texture texture, int index, Vector2f fallback) {
        Vector2f localUv;
        if (polygon.getPolygonType().isTextured()) {
            localUv = polygon.getTextureUv(index, this.tempByteTextureUv).toVector(this.tempVector);
        } else {
            localUv = this.tempVector.setXY(fallback);
        }

        PSXShadeTextureDefinition.tryApplyUntexturedShadingPadding(texture, textureSource, localUv);

        // Get the UVs local to the texture.
        return getMesh().getTextureAtlas().getUV(texture, localUv);
    }

    /**
     * Updates a map vertex index.
     * @param mapVertexIndex index of the map vertex to update
     */
    public void updateMapVertex(int mapVertexIndex) {
        updateVertex(this.vertexEntry, mapVertexIndex);
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        if (this.vertexEntry == entry) {
            SVector vertexPos = getMap().getPolygonPacket().getVertices().get(localVertexIndex);
            entry.writeVertexXYZ(localVertexIndex, vertexPos.getFloatX(), vertexPos.getFloatY(), vertexPos.getFloatZ());
        }

        // Do nothing else, no other entries are given vertices.
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        SCMapPolygon polygon = entry.getDataSource();
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
    public SCMapFile<?> getMap() {
        return getMesh().getMap();
    }
}