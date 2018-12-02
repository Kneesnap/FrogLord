package net.highwayfrogs.editor.file.map.view;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolyTexture;
import net.highwayfrogs.editor.file.standard.psx.prims.polygon.PSXPolygon;
import net.highwayfrogs.editor.gui.GUIMain;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds Map mesh information.
 * Created by Kneesnap on 11/25/2018.
 */
public class MapMesh extends TriangleMesh {
    private MAPFile map;
    private TextureMap textureMap;

    private boolean remapFinder;
    private int remapStart;
    private int maxRemapSize;
    private int currentRemap;

    public MapMesh(MAPFile file, TextureMap texMap) {
        super(VertexFormat.POINT_TEXCOORD);
        this.map = file;
        this.textureMap = texMap;
        updateData();
    }

    public MapMesh(MAPFile file, TextureMap texMap, int remapStart, int maxRemapSize) {
        super(VertexFormat.POINT_TEXCOORD);
        this.map = file;
        this.textureMap = texMap;

        this.remapFinder = true;
        this.remapStart = remapStart - 1;
        this.maxRemapSize = maxRemapSize;
        this.currentRemap = this.remapStart;
    }

    /**
     * Find the next valid remap range.
     */
    public void findNextValidRemap() {
        if (!this.remapFinder)
            return;

        int totalSkips = 0;
        while (true) {
            this.currentRemap++;

            try {
                textureMap.setRemapList(GUIMain.EXE_CONFIG.readRemapTable(this.currentRemap, this.maxRemapSize));
            } catch (Exception ex) {
                System.out.println("Reached end of file. Restarting at beginning.");
                this.currentRemap = this.remapStart;
                continue;
            }

            try {
                updateData();
                System.out.println("[Skipped " + totalSkips + "]: Found Possible Remap at 0x" + Integer.toHexString(this.currentRemap) + ", " + this.maxRemapSize);
                return;
            } catch (Exception ex) { // Failed.
                totalSkips++;
            }
        }
    }

    /**
     * Load mesh data from the map.
     */
    public void updateData() {
        updateVertices();
        updatePolygonData();
    }

    /**
     * Load polygon data.
     */
    public void updatePolygonData() {
        getFaces().clear();
        getTexCoords().clear();

        AtomicInteger texId = new AtomicInteger();
        map.getCachedPolygons().values().forEach(list -> list.forEach(prim -> {
            if (!(prim instanceof PSXPolygon))
                return;

            PSXPolygon poly = (PSXPolygon) prim;
            int vertCount = poly.getVertices().length;

            if (vertCount == PSXPolygon.TRI_SIZE) {
                addTriangle(poly, texId);
            } else if (vertCount == PSXPolygon.QUAD_SIZE) {
                addRectangle(poly, texId);
            } else {
                throw new RuntimeException("Cannot handle " + vertCount + " vertices");
            }
        }));
    }

    /**
     * Add a rectangle polygon.
     * @param poly The rectangle polygon.
     */
    public void addRectangle(PSXPolygon poly, AtomicInteger texCoord) {
        short[] verts = poly.getVertices();
        Utils.verify(verts.length == PSXPolygon.QUAD_SIZE, "This polygon has %d vertices!", verts.length);

        // Alternate Option: [1 0 2] [2 0 3]
        int texId = addTexCoords(poly, texCoord);
        getFaces().addAll(verts[0], texId, verts[3], texId + 2, verts[1], texId + 1);
        getFaces().addAll(verts[1], texId + 1, verts[3], texId + 2, verts[2], texId + 3);
    }

    /**
     * Add a triangle polygon.
     * @param poly The triangle polygon.
     */
    public void addTriangle(PSXPolygon poly, AtomicInteger texCoord) {
        short[] verts = poly.getVertices();
        Utils.verify(verts.length == PSXPolygon.TRI_SIZE, "This polygon has %d vertices!", poly.getVertices().length);

        int texId = addTexCoords(poly, texCoord);
        getFaces().addAll(verts[2], texId + 2, verts[1], texId + 1, verts[0], texId);
    }

    private int addTexCoords(PSXPolygon poly, AtomicInteger texCoord) {
        int texId = texCoord.get();
        int texCount = poly.getVertices().length;

        texCoord.addAndGet(texCount);
        TextureEntry entry = poly.getEntry(textureMap);
        Utils.verify(entry != null, "Failed to get TextureEntry for polygon.");

        float uSize = (entry.getMaxU() - entry.getMinU());
        float vSize = (entry.getMaxV() - entry.getMinV());

        if (poly instanceof PSXPolyTexture) {
            ByteUV[] uvs = ((PSXPolyTexture) poly).getUvs();
            for (ByteUV uv : uvs)
                getTexCoords().addAll(entry.getMinU() + (uSize * uv.getFloatU()), entry.getMinV() + (vSize * uv.getFloatV()));
        } else {
            getTexCoords().addAll(entry.getMinU(), entry.getMinV());
            getTexCoords().addAll(entry.getMinU(), entry.getMaxV());
            getTexCoords().addAll(entry.getMaxU(), entry.getMinV());
            if (texCount == PSXPolygon.QUAD_SIZE)
                getTexCoords().addAll(entry.getMaxU(), entry.getMaxV());
        }

        return texId;
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
