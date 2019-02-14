package net.highwayfrogs.editor.file.map.view;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;
import net.highwayfrogs.editor.gui.mesh.MeshManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A mesh which we can write things to.
 * Created by Kneesnap on 2/13/2019.
 */
@Getter
public abstract class FrogMesh<T extends PSXGPUPrimitive> extends TriangleMesh {
    private Map<Integer, T> facePolyMap = new HashMap<>();
    private Map<T, Integer> polyFaceMap = new HashMap<>();
    private TextureMap textureMap;
    private MeshManager manager;
    private int faceCount;
    private int textureCount;

    public FrogMesh(TextureMap map, VertexFormat format) {
        super(format);
        this.textureMap = map;
        this.manager = new MeshManager(this);
    }

    /**
     * Add a polygon of intederminate size!
     * @param prim  The primitive to add.
     * @param texId The texture id.
     */
    public void addPolygon(T prim, AtomicInteger texId) {
        int vertCount = prim.getVerticeCount();

        if (vertCount == MAPPolygon.TRI_SIZE) {
            addTriangle(prim, texId);
        } else if (vertCount == MAPPolygon.QUAD_SIZE) {
            addRectangle(prim, texId);
        } else {
            throw new RuntimeException("Cannot handle " + vertCount + " vertices");
        }
    }


    /**
     * Add a rectangle polygon.
     * @param poly The rectangle polygon.
     */
    public void addRectangle(T poly, AtomicInteger texCoord) {
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
    public void addTriangle(T poly, AtomicInteger texCoord) {
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

    private int addTexCoords(T poly, AtomicInteger texCoord) {
        int texId = texCoord.get();
        int texCount = poly.getVerticeCount();

        texCoord.addAndGet(texCount);
        TextureEntry entry = poly.getEntry(textureMap);
        if (entry == null)
            throw new RuntimeException("Failed to get TextureEntry for " + poly.getClass().getSimpleName() + " polygon.");

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
        for (SVector vertex : getVertices())
            getPoints().addAll(Utils.fixedPointShortToFloat412(vertex.getX()), Utils.fixedPointShortToFloat412(vertex.getY()), Utils.fixedPointShortToFloat412(vertex.getZ()));
    }

    /**
     * Load mesh data from the map.
     * TODO: Keep everything added after polygon data.
     */
    public void updateData() {
        updateVertices();
        updatePolygonData();
        onUpdateData(); // Overwrite this.
        this.faceCount = getFaces().size();
        this.textureCount = getTexCoords().size();
    }

    /**
     * Load polygon data.
     */
    public void updatePolygonData() {
        getFacePolyMap().clear();
        getPolyFaceMap().clear();
        getFaces().clear();
        getTexCoords().clear();
        this.onUpdatePolygonData();
    }

    /**
     * Load data.
     */
    public void onUpdateData() {

    }

    /**
     * Called when its time to setup polygon data.
     */
    public abstract void onUpdatePolygonData();

    /**
     * Gets a list of vertices.
     * @return vertices
     */
    public abstract List<SVector> getVertices();
}
