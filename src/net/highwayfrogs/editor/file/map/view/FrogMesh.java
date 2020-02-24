package net.highwayfrogs.editor.file.map.view;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;
import net.highwayfrogs.editor.gui.mesh.MeshManager;
import net.highwayfrogs.editor.system.TexturedPoly;
import net.highwayfrogs.editor.utils.Utils;

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
    @Setter private int verticeStart;

    public FrogMesh(TextureMap map, VertexFormat format) {
        super(format);
        this.textureMap = map;
        this.manager = new MeshManager(this);
    }

    /**
     * Add a polygon regardless of size!
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
        getFaces().addAll(verts[0] + getVerticeStart(), texId, verts[3] + getVerticeStart(), texId + 2, verts[1] + getVerticeStart(), texId + 1);
        getFaces().addAll(verts[1] + getVerticeStart(), texId + 1, verts[3] + getVerticeStart(), texId + 2, verts[2] + getVerticeStart(), texId + 3);
    }

    /**
     * Add a rectangle polygon.
     */
    public void addRectangle(TextureTreeNode entry, int v1, int v2, int v3, int v4, int v5, int v6) {
        int texId = getTexCoords().size() / getTexCoordElementSize();
        entry.applyMesh(this, MAPPolygon.QUAD_SIZE);
        getFaces().addAll(v1 + getVerticeStart(), texId, v2 + getVerticeStart(), texId + 2, v3 + getVerticeStart(), texId + 1);
        getFaces().addAll(v4 + getVerticeStart(), texId + 1, v5 + getVerticeStart(), texId + 2, v6 + getVerticeStart(), texId + 3);
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
        getFaces().addAll(verts[2] + getVerticeStart(), texId + 2, verts[1] + getVerticeStart(), texId + 1, verts[0] + getVerticeStart(), texId);
    }

    /**
     * Add a triangle.
     * @param entry The uvs of the texture to add.
     */
    public void addTriangle(TextureTreeNode entry, int v1, int v2, int v3) {
        int texId = getTexCoords().size() / getTexCoordElementSize();
        entry.applyMesh(this, MAPPolygon.TRI_SIZE);
        getFaces().addAll(v1 + getVerticeStart(), texId + 2, v2 + getVerticeStart(), texId + 1, v3 + getVerticeStart(), texId);
    }

    protected int addTexCoords(T poly, AtomicInteger texCoord) {
        int texId = texCoord.get();
        int texCount = poly.getVerticeCount();

        texCoord.addAndGet(texCount);
        TextureTreeNode entry = poly.getNode(textureMap);
        if (entry == null) {
            System.out.println("There were issues setting up textures for this " + poly.getClass().getSimpleName());
            entry = new TextureTreeNode(textureMap.getTextureTree());
        }

        float uSize = (entry.getMaxU() - entry.getMinU());
        float vSize = (entry.getMaxV() - entry.getMinV());

        if (poly instanceof TexturedPoly) {
            TexturedPoly texPoly = (TexturedPoly) poly;
            ByteUV[] uvs = texPoly.getUvs();

            texPoly.performSwap();
            for (ByteUV uv : uvs)
                getTexCoords().addAll(entry.getMinU() + (uSize * uv.getFloatU()), entry.getMinV() + (vSize * uv.getFloatV()));
            texPoly.performSwap();
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
        for (Vector vertex : getVertices())
            getPoints().addAll(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ());
    }

    /**
     * Load mesh data from the map.
     * TODO: Keep everything added after polygon data.
     */
    public void updateData() {
        this.verticeStart = 0;
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
        getManager().getMeshData().clear();
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
    public abstract List<? extends Vector> getVertices();
}
