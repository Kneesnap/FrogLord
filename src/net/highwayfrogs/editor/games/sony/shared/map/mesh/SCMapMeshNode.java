package net.highwayfrogs.editor.games.sony.shared.map.mesh;

import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.map.ISCLevelTableEntry;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.map.packet.SCMapPolygonPacket;
import net.highwayfrogs.editor.games.sony.shared.mesh.SCPolygonAdapterNode;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Represents a node in a map mesh for MediEvil.
 * Created by Kneesnap on 5/8/2024.
 */
public class SCMapMeshNode extends SCPolygonAdapterNode<SCMapPolygon> {
    private final SCByteTextureUV tempByteTextureUv = new SCByteTextureUV();

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


        // Setup polygons.
        // First, setup the non-transparent polygons.
        SCMapPolygonPacket<?> polygonPacket = getMap().getPolygonPacket();
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
    protected boolean getPolygonTextureCoordinate(SCMapPolygon polygon, int index, Vector2f result) {
        if (!polygon.getPolygonType().isTextured())
            return false;

        polygon.getTextureUv(index, this.tempByteTextureUv).toVector(result);
        return true;
    }

    @Override
    public List<SVector> getAllVertices() {
        SCMapPolygonPacket<?> polygonPacket = getMap().getPolygonPacket();
        return polygonPacket.getVertices();
    }

    @Override
    protected int[] getVertices(SCMapPolygon polygon) {
        return polygon.getVertices();
    }

    @Override
    protected int getVertexCount(SCMapPolygon polygon) {
        return polygon.getVertexCount();
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public SCMapFile<?> getMap() {
        return getMesh().getMap();
    }
}