package net.highwayfrogs.editor.games.sony.medievil.map.mesh;

import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.mesh.SCPolygonAdapterNode;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Represents a node in a map mesh for MediEvil.
 * Cloned from a file created by Kneesnap on 03/9/2024.
 */
public class MediEvilMapMeshNode extends SCPolygonAdapterNode<MediEvilMapPolygon> {

    public MediEvilMapMeshNode(MediEvilMapMesh mesh) {
        super(mesh);
    }

    @Override
    public MediEvilMapMesh getMesh() {
        return (MediEvilMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup polygons.
        // First, setup the non-transparent polygons.
        MediEvilLevelTableEntry levelTableEntry = getMap().getLevelTableEntry();
        for (MediEvilMapPolygon polygon : getMap().getGraphicsPacket().getPolygons())
            if (polygon.isFullyOpaque(levelTableEntry))
                this.add(polygon);

        // Second, add the transparent polygons.
        for (MediEvilMapPolygon polygon : getMap().getGraphicsPacket().getPolygons())
            if (!polygon.isFullyOpaque(levelTableEntry))
                this.add(polygon);
    }

    @Override
    protected boolean getPolygonTextureCoordinate(MediEvilMapPolygon polygon, int index, Vector2f result) {
        if (!polygon.getPolygonType().isTextured())
            return false;

        polygon.getTextureUvs()[index].toVector(result);
        return true;
    }

    @Override
    public List<SVector> getAllVertices() {
        return getMap().getGraphicsPacket().getVertices();
    }

    @Override
    protected int[] getVertices(MediEvilMapPolygon polygon) {
        return polygon.getVertices();
    }

    @Override
    protected int getVertexCount(MediEvilMapPolygon polygon) {
        return polygon.getVertexCount();
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public MediEvilMapFile getMap() {
        return getMesh().getMap();
    }
}