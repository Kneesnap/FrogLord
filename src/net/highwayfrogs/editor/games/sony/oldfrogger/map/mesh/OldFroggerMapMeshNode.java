package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh;

import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.games.sony.shared.mesh.SCPolygonAdapterNode;
import net.highwayfrogs.editor.system.math.Vector2f;

import java.util.List;

/**
 * Represents a node in a map mesh for pre-recode frogger.
 * Created by Kneesnap on 12/8/2023.
 */
public class OldFroggerMapMeshNode extends SCPolygonAdapterNode<OldFroggerMapPolygon> {
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

        // Setup polygons.
        for (OldFroggerMapGrid grid : getMap().getGridPacket().getGrids())
            for (OldFroggerMapPolygon polygon : grid.getPolygons())
                this.add(polygon);
    }

    @Override
    protected boolean getPolygonTextureCoordinate(OldFroggerMapPolygon polygon, int index, Vector2f result) {
        if (!polygon.getPolygonType().isTextured())
            return false;

        polygon.getTextureUvs()[index].toVector(result);

        // Map textures seem to be flipped vertically.
        // Shaded textures only need flipping if they are using real textures (real UVs).
        // The reason is that the UVs in the map file are inverted, but when we use our own UVs (necessary for shaded untextured polygons which don't have UVs),
        // since we choose the UVs, we provide the correct ones.
        result.setY(1F - result.getY()); // UVs are flipped for generated shader textures too, in order to stay consistent.
        return true;
    }

    @Override
    public List<SVector> getAllVertices() {
        return getMap().getVertexPacket().getVertices();
    }

    @Override
    protected int[] getVertices(OldFroggerMapPolygon polygon) {
        return polygon.getVertices();
    }

    @Override
    protected int getVertexCount(OldFroggerMapPolygon polygon) {
        return 4; // There are no tris in this.
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public OldFroggerMapFile getMap() {
        return getMesh().getMap();
    }
}