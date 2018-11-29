package net.highwayfrogs.editor.file.map;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolyTexture;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;
import net.highwayfrogs.editor.file.vlo.TextureMap;
import net.highwayfrogs.editor.file.vlo.TextureMap.TextureEntry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds Map mesh information.
 * TODO: Support colored polygons. [in .obj too]
 * TODO: Support per-polygon uvs.
 * Created by Kneesnap on 11/25/2018.
 */
public class MapMesh extends TriangleMesh {
    private MAPFile map;
    private TextureMap textureMap;

    public MapMesh(MAPFile file, TextureMap texMap) {
        super(VertexFormat.POINT_TEXCOORD);
        this.map = file;
        this.textureMap = texMap;
        updateData();
    }

    /**
     * Load mesh data from the map.
     */
    public void updateData() {
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

        AtomicInteger id = new AtomicInteger();
        for (TextureEntry entry : textureMap.getEntryMap().values()) {
            entry.setCoordinateId(id.getAndAdd(4));

            double uSize = entry.getMaxU() - entry.getMinU();
            double vSize = entry.getMaxV() - entry.getMinV();
            getTexCoords().addAll(entry.getMinU(), entry.getMinV());
            getTexCoords().addAll(entry.getMinU(), entry.getMaxV());
            getTexCoords().addAll(entry.getMaxU(), entry.getMinV());
            getTexCoords().addAll(entry.getMaxU(), entry.getMaxV());
        }
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
        int texId = getTextureId(poly);
        /*getFaces().addAll(verts[0], texId + 3, verts[3], texId + 1, verts[1], texId + 2);
        getFaces().addAll(verts[1], texId + 2, verts[3], texId + 1, verts[2], texId);*/

        // Vertical Ok, Horizontal Not Ok
        /*
        getFaces().addAll(verts[0], texId, verts[3], texId + 2, verts[1], texId + 1);
        getFaces().addAll(verts[1], texId + 1, verts[3], texId + 2, verts[2], texId + 3);
        */

        getFaces().addAll(verts[0], texId, verts[3], texId + 2, verts[1], texId + 1);
        getFaces().addAll(verts[1], texId + 1, verts[3], texId + 2, verts[2], texId + 3);
    }

    /**
     * Add a triangle polygon.
     * @param poly The triangle polygon.
     */
    public void addTriangle(PSXPolygon poly) {
        short[] verts = poly.getVertices();
        Utils.verify(verts.length == PSXPolygon.TRI_SIZE, "This polygon has %d vertices!", poly.getVertices().length);

        int texId = getTextureId(poly);
        getFaces().addAll(verts[2], texId, verts[1], texId + 1, verts[0], texId + 2);
    }

    private int getTextureId(PSXPolygon poly) {
        if (!(poly instanceof PSXPolyTexture))
            return 0; //TODO

        PSXPolyTexture polyTex = (PSXPolyTexture) poly;
        return textureMap.getEntryMap().get(textureMap.getRemapList().get(polyTex.getTextureId())).getCoordinateId();
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
