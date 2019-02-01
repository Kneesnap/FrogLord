package net.highwayfrogs.editor.file.map.view;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.mesh.MeshManager;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds Map mesh information.
 * Created by Kneesnap on 11/25/2018.
 */
@Getter
public class MapMesh extends TriangleMesh {
    private MAPFile map;
    private TextureMap textureMap;
    private Map<Integer, MAPPolygon> facePolyMap = new HashMap<>();
    private Map<MAPPolygon, Integer> polyFaceMap = new HashMap<>();
    private MeshManager manager;

    private int faceCount;
    private int textureCount;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor ANIMATION_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor INVISIBLE_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);
    public static final CursorVertexColor GRID_COLOR = new CursorVertexColor(Color.BLUE, Color.BLACK);

    public MapMesh(MAPFile file, TextureMap texMap) {
        super(VertexFormat.POINT_TEXCOORD);
        this.map = file;
        this.textureMap = texMap;
        this.manager = new MeshManager(this);
        updateData();
    }

    /**
     * Load mesh data from the map.
     * TODO: Keep everything added after polygon data.
     */
    public void updateData() {
        updateVertices();
        updatePolygonData();
    }

    /**
     * Load polygon data.
     */
    public void updatePolygonData() {
        facePolyMap.clear();
        polyFaceMap.clear();
        getFaces().clear();
        getTexCoords().clear();

        AtomicInteger texId = new AtomicInteger();
        map.forEachPrimitive(prim -> {
            if (!(prim instanceof MAPPolygon))
                return;

            MAPPolygon poly = (MAPPolygon) prim;
            int vertCount = poly.getVerticeCount();

            if (vertCount == MAPPolygon.TRI_SIZE) {
                addTriangle(poly, texId);
            } else if (vertCount == MAPPolygon.QUAD_SIZE) {
                addRectangle(poly, texId);
            } else {
                throw new RuntimeException("Cannot handle " + vertCount + " vertices");
            }
        });

        this.faceCount = getFaces().size();
        this.textureCount = getTexCoords().size();
    }

    /**
     * Add a rectangle polygon.
     * @param poly The rectangle polygon.
     */
    public void addRectangle(MAPPolygon poly, AtomicInteger texCoord) {
        Utils.verify(poly.getVerticeCount() == MAPPolygon.QUAD_SIZE, "This polygon has %d vertices!", poly.getVerticeCount());

        int[] verts = poly.getVertices();
        int face = getFaces().size() / getFaceElementSize();
        polyFaceMap.put(poly, face);
        facePolyMap.put(face, poly);
        facePolyMap.put(face + 1, poly);

        // Add Face + Textures.
        int texId = addTexCoords(poly, texCoord);
        getFaces().addAll(verts[0], texId, verts[3], texId + 2, verts[1], texId + 1);
        getFaces().addAll(verts[1], texId + 1, verts[3], texId + 2, verts[2], texId + 3);
    }

    /**
     * Add a rectangle polygon.
     */
    public void addRectangle(TextureEntry entry, int v1, int v2, int v3, int v4, int v5, int v6) {
        int texId = getTexCoords().size() / getTexCoordElementSize();
        entry.applyMesh(this, MAPPolygon.QUAD_SIZE);
        getFaces().addAll(v1, texId, v2, texId + 1, v3, texId + 2);
        getFaces().addAll(v4, texId + 1, v5, texId + 2, v6, texId + 3);
    }

    /**
     * Add a triangle polygon.
     * @param poly The triangle polygon.
     */
    public void addTriangle(MAPPolygon poly, AtomicInteger texCoord) {
        Utils.verify(poly.getVerticeCount() == MAPPolygon.TRI_SIZE, "This polygon has %d vertices!", poly.getVerticeCount());

        int[] verts = poly.getVertices();
        int face = getFaces().size() / getFaceElementSize();
        facePolyMap.put(face, poly);
        polyFaceMap.put(poly, face);

        int texId = addTexCoords(poly, texCoord);
        getFaces().addAll(verts[2], texId + 2, verts[1], texId + 1, verts[0], texId);
    }

    /**
     * Add a triangle.
     * @param entry The uvs of the texture to add.
     */
    public void addTriangle(TextureEntry entry, int v1, int v2, int v3) {
        int texId = getTexCoords().size() / getTexCoordElementSize();
        entry.applyMesh(this, MAPPolygon.TRI_SIZE);
        getFaces().addAll(v1, texId, v2, texId + 1, v3, texId + 2);
    }

    private int addTexCoords(MAPPolygon poly, AtomicInteger texCoord) {
        int texId = texCoord.get();
        int texCount = poly.getVerticeCount();

        texCoord.addAndGet(texCount);
        TextureEntry entry = poly.getEntry(textureMap);
        Utils.verify(entry != null, "Failed to get TextureEntry for polygon.");

        float uSize = (entry.getMaxU() - entry.getMinU());
        float vSize = (entry.getMaxV() - entry.getMinV());

        if (poly instanceof MAPPolyTexture) {
            ByteUV[] uvs = ((MAPPolyTexture) poly).getUvs();
            for (ByteUV uv : uvs)
                getTexCoords().addAll(entry.getMinU() + (uSize * uv.getFloatU()), entry.getMinV() + (vSize * uv.getFloatV()));
        } else {
            entry.applyMesh(this, texCount);
        }

        return texId;
    }

    /**
     * Load vertex data.
     */
    public void updateVertices() {
        getPoints().clear();
        for (SVector vertex : map.getVertexes()) {
            getPoints().addAll(Utils.fixedPointShortToFloatNBits(vertex.getX(), 4), Utils.fixedPointShortToFloatNBits(vertex.getY(), 4), Utils.fixedPointShortToFloatNBits(vertex.getZ(), 4));
        }
    }
}
