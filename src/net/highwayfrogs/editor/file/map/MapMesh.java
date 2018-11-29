package net.highwayfrogs.editor.file.map;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;

/**
 * Holds Map mesh information.
 * Created by Kneesnap on 11/25/2018.
 */
public class MapMesh extends TriangleMesh {
    private MAPFile map;

    public MapMesh(MAPFile file) {
        super(VertexFormat.POINT_TEXCOORD);
        this.map = file;
        loadData();
    }

    /**
     * Load mesh data from the map.
     */
    public void loadData() {
        updateVertices();
        updateTextureCoords();
        updatePolygonData();
        //getFaceSmoothingGroups();
    }

    /**
     * Load texture coordinates.
     */
    public void updateTextureCoords() {
        getTexCoords().clear();
        getTexCoords().addAll(1, 1);
        getTexCoords().addAll(1, 0);
        getTexCoords().addAll(0, 1);
        getTexCoords().addAll(0, 0);
    }

    /**
     * Load polygon data.
     */
    public void updatePolygonData() {
        getFaces().clear();
        map.getCachedPolygons().values().forEach(list -> list.forEach(prim -> {
            if (!(prim instanceof PSXPolygon))
                return;

            PSXPolygon poly = (PSXPolygon) prim;
            int vertCount = poly.getVertices().length;

            if (vertCount == PSXPolygon.TRI_SIZE) {
                addTriangle(poly);
            } else if (vertCount == PSXPolygon.QUAD_SIZE) {
                addRectangle(poly);
            } else {
                throw new RuntimeException("Cannot handle " + vertCount + "vertices");
            }
        }));
    }

    /**
     * Add a rectangle polygon.
     * @param poly The rectangle polygon.
     */
    public void addRectangle(PSXPolygon poly) {
        short[] verts = poly.getVertices();
        Utils.verify(verts.length == PSXPolygon.QUAD_SIZE, "This polygon has %d vertices!", verts.length);

        // Alternate Option: [1 0 2] [2 0 3]
        getFaces().addAll(verts[0], 0, verts[3], 0, verts[1], 0);
        getFaces().addAll(verts[1], 0, verts[3], 0, verts[2], 0);
    }

    /**
     * Add a triangle polygon.
     * @param poly The triangle polygon.
     */
    public void addTriangle(PSXPolygon poly) {
        short[] verts = poly.getVertices();
        Utils.verify(verts.length == PSXPolygon.TRI_SIZE, "This polygon has %d vertices!", poly.getVertices().length);
        getFaces().addAll(verts[2], 0, verts[1], 0, verts[0], 0);
    }

    /**
     * Load vertex data.
     */
    public void updateVertices() {
        getPoints().clear();
        for (SVector vertex : map.getVertexes())
            getPoints().addAll(Utils.unsignedShortToFloat(vertex.getX()), Utils.unsignedShortToFloat(vertex.getY()), Utils.unsignedShortToFloat(vertex.getZ()));
    }
}
