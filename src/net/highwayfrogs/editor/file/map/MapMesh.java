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
     * An example mesh that works.
     */
    public void exampleWorks() {
        getPoints().addAll(0F, 0F, 0F);
        getPoints().addAll(0F, 10F, 0F);
        getPoints().addAll(10F, 0F, 0F);
        getPoints().addAll(10F, 10F, 0F);

        loadTextureCoords();

        getFaces().addAll(0, 0, 1, 0, 3, 0);
        getFaces().addAll(0, 0, 2, 0, 3, 0);
    }

    /**
     * Load mesh data from the map.
     */
    public void loadData() {
        loadVertices();
        loadTextureCoords();
        loadPolygonData();
        //getFaceSmoothingGroups();
        //getNormals();
    }

    /**
     * Load texture coordinates.
     */
    public void loadTextureCoords() {
        getTexCoords().addAll(1, 1);
        getTexCoords().addAll(1, 0);
        getTexCoords().addAll(0, 1);
        getTexCoords().addAll(0, 0);
    }

    /**
     * Load polygon data.
     */
    public void loadPolygonData() {
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
        getFaces().addAll(verts[0], 0, verts[1], 0, verts[3], 0);
        getFaces().addAll(verts[0], 0, verts[2], 0, verts[3], 0);
    }

    /**
     * Add a triangle polygon.
     * @param poly The triangle polygon.
     */
    public void addTriangle(PSXPolygon poly) {
        Utils.verify(poly.getVertices().length == PSXPolygon.TRI_SIZE, "This polygon has %d vertices!", poly.getVertices().length);
        for (short vertice : poly.getVertices())
            getFaces().addAll(vertice, 0);
    }

    /**
     * Load vertex data.
     */
    public void loadVertices() {
        for (SVector vertex : map.getVertexes())
            getPoints().addAll(Utils.unsignedShortToFloat(vertex.getX()) * 100, Utils.unsignedShortToFloat(vertex.getY()) * 100, -Utils.unsignedShortToFloat(vertex.getZ()) * 100);
    }
}
