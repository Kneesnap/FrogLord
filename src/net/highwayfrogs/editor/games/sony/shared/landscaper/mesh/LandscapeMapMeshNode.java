package net.highwayfrogs.editor.games.sony.shared.landscaper.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager;
import net.highwayfrogs.editor.games.sony.shared.landscaper.Landscape;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapePolygon;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapeVertex;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * Manages the terrain in a landscape.
 * Created by Kneesnap on 7/16/2024.
 */
@Getter
public class LandscapeMapMeshNode extends DynamicMeshAdapterNode<LandscapePolygon> {
    private final Vector2f tempVector = new Vector2f();
    private final Vector3f tempVector3 = new Vector3f();
    @Getter private DynamicMeshDataEntry vertexEntry;

    public LandscapeMapMeshNode(LandscapeMapMesh mesh) {
        super(mesh);
    }

    @Override
    public LandscapeMapMesh getMesh() {
        return (LandscapeMapMesh) super.getMesh();
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertices.
        this.vertexEntry = new DynamicMeshDataEntry(getMesh());
        List<LandscapeVertex> vertices = getLandscape().getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            LandscapeVertex vertex = vertices.get(i);
            Vector3f vertexWorldPos = vertex.getWorldPosition(this.tempVector3);
            this.vertexEntry.addVertexValue(vertexWorldPos);
        }
        addUnlinkedEntry(this.vertexEntry);

        // Setup polygons.
        // First, setup the non-transparent polygons.
        for (LandscapePolygon polygon : getLandscape().getPolygons())
            if (!polygon.isSemiTransparent())
                this.add(polygon);

        // Second, add the transparent polygons.
        for (LandscapePolygon polygon : getLandscape().getPolygons())
            if (polygon.isSemiTransparent())
                this.add(polygon);
    }

    @Override
    public void clear() {
        super.clear();
        this.vertexEntry = null;
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(LandscapePolygon polygon) {
        DynamicMeshTypedDataEntry newEntry = new DynamicMeshTypedDataEntry(getMesh(), polygon);
        addPolygonDataToEntries(polygon, newEntry, newEntry);
        return newEntry;
    }

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        LandscapePolygon polygon = entry.getDataSource();

        // Resolve texture in texture atlas.
        ITextureSource textureSource = getTextureSource(polygon);
        AtlasTexture texture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureSource);

        Vector2f uv;
        switch (localTexCoordIndex) {
            case 0:
                uv = getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO); // uvTopLeft, 0F, 0F
                break;
            case 1:
                uv = getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X); // uvTopRight, 1F, 0F
                break;
            case 2:
                uv = getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y); // uvBottomLeft, 0F, 1F
                break;
            case 3:
                uv = getTextureCoordinate(polygon, textureSource, texture, 3, Vector2f.ONE); // uvBottomRight, 1F, 1F
                break;
            default:
                throw new IllegalArgumentException("Unsupported local texCoordIndex " + localTexCoordIndex);
        }


        entry.writeTexCoordValue(localTexCoordIndex, uv);
    }

    /**
     * Updates a shared vertex by its index.
     * @param vertexIndex index of the shared vertex to update
     */
    public void updateVertex(int vertexIndex) {
        updateVertex(this.vertexEntry, vertexIndex);
    }

    @Override
    public void updateVertex(DynamicMeshTypedDataEntry entry, int localVertexIndex) {
        if (this.vertexEntry == entry) {
            List<LandscapeVertex> vertices = getLandscape().getVertices();
            if (vertices == null || localVertexIndex >= vertices.size())
                throw new RuntimeException("Invalid vertex ID: " + localVertexIndex);

            LandscapeVertex vertex = vertices.get(localVertexIndex);
            Vector3f vertexPos = vertex.getWorldPosition(this.tempVector3);
            entry.writeVertexXYZ(localVertexIndex, vertexPos.getX(), vertexPos.getY(), vertexPos.getZ());
        } else {
            // Do nothing else, no other entries are given vertices. If we do this in a subclass, override this method.
            throw new RuntimeException("Cannot update vertex for vertex entry: " + entry);
        }
    }

    /**
     * Evaluates the texture coordinates given the provided data.
     * @param polygon the polygon to evaluate the texture coordinate from
     * @param textureSource the source of the texture. when this is PSX shading, the padding will automatically be applied
     * @param texture the texture registered in the texture atlas
     * @param index the index of the vertex to evaluate
     * @param fallback the fallback value to use when we cannot get the data from the polygon
     * @return textureCoordinateVector
     */
    protected final Vector2f getTextureCoordinate(LandscapePolygon polygon, ITextureSource textureSource, Texture texture, int index, Vector2f fallback) {
        Vector2f localUv;
        if (getPolygonTextureCoordinate(polygon, index, this.tempVector)) {
            localUv = this.tempVector;
        } else {
            localUv = this.tempVector.setXY(fallback);
        }

        PSXShadeTextureDefinition.tryApplyUntexturedShadingPadding(texture, textureSource, localUv);

        // Get the UVs local to the texture.
        return getMesh().getTextureAtlas().getUV(texture, localUv, localUv);
    }

    /**
     * Adds the polygon data to the given entries.
     * @param polygon the polygon which has the data to write
     * @param texCoordEntry the entry to write texCoordEntries to
     * @param faceEntry the entry to write face data to
     */
    protected void addPolygonDataToEntries(LandscapePolygon polygon, DynamicMeshDataEntry texCoordEntry, DynamicMeshDataEntry faceEntry) {
        if (texCoordEntry == null)
            throw new NullPointerException("texCoordEntry");
        if (faceEntry == null)
            throw new NullPointerException("faceEntry");

        // Resolve texture.
        ITextureSource textureSource = getTextureSource(polygon);
        Texture texture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureSource);

        // Add texture UVs.
        LandscapeVertex polygonVertex0 = polygon.getVertex(0);
        LandscapeVertex polygonVertex1 = polygon.getVertex(1);
        LandscapeVertex polygonVertex2 = polygon.getVertex(2);
        if (polygon.isQuad()) {
            LandscapeVertex polygonVertex3 = polygon.getVertex(3);
            int uvIndex1 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
            int uvIndex2 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
            int uvIndex3 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F
            int uvIndex4 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 3, Vector2f.ONE)); // uvBottomRight, 1F, 1F

            // Vertice IDs are the same IDs seen in the map data.
            int vtxIndex1 = this.vertexEntry.getPendingVertexStartIndex() + polygonVertex0.getVertexId();
            int vtxIndex2 = this.vertexEntry.getPendingVertexStartIndex() + polygonVertex1.getVertexId();
            int vtxIndex3 = this.vertexEntry.getPendingVertexStartIndex() + polygonVertex2.getVertexId();
            int vtxIndex4 = this.vertexEntry.getPendingVertexStartIndex() + polygonVertex3.getVertexId();

            // JavaFX uses counter-clockwise winding order.
            faceEntry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
            faceEntry.addFace(vtxIndex3, uvIndex3, vtxIndex4, uvIndex4, vtxIndex2, uvIndex2);
        } else if (polygon.isTri()) {
            int uvIndex1 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
            int uvIndex2 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
            int uvIndex3 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F

            // Vertice IDs are the same IDs seen in the map data.
            int vtxIndex1 = this.vertexEntry.getPendingVertexStartIndex() + polygonVertex0.getVertexId();
            int vtxIndex2 = this.vertexEntry.getPendingVertexStartIndex() + polygonVertex1.getVertexId();
            int vtxIndex3 = this.vertexEntry.getPendingVertexStartIndex() + polygonVertex2.getVertexId();

            // JavaFX uses counter-clockwise winding order.
            faceEntry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
        } else {
            throw new RuntimeException("Cannot add polygon which isn't either a quad or a tri!");
        }
    }

    /**
     * Gets the shaded texture manager for the mesh.
     */
    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    public PSXShadedTextureManager<LandscapePolygon> getShadedTextureManager() {
        DynamicMesh mesh = getMesh();
        if (!(mesh instanceof IPSXShadedMesh))
            return null;

        return ((PSXShadedTextureManager<LandscapePolygon>) ((IPSXShadedMesh) mesh).getShadedTextureManager());
    }

    /**
     * Gets the landscape which mesh data comes from.
     */
    public Landscape getLandscape() {
        return getMesh().getLandscape();
    }

    /**
     * Gets the texture coordinates used to render the provided polygon
     * @param polygon the polygon to get the texture coordinate data from
     * @param index the index of the local vertex to evaluate
     * @param result the vector to store the results in
     * @return if the data was successfully obtained from the polygon
     */
    protected boolean getPolygonTextureCoordinate(LandscapePolygon polygon, int index, Vector2f result) {
        if (!polygon.getPolygonType().isTextured())
            return false; // No textured -> Can't get the UVs from here.

        polygon.getTextureUvs()[index].toVector(result);
        return true;
    }

    /**
     * Gets the texture source for the given polygon.
     * @param polygon the polygon to get the texture source from
     * @return textureSourceOrNull
     */
    protected ITextureSource getTextureSource(LandscapePolygon polygon) {
        PSXShadedTextureManager<LandscapePolygon> textureManager = getShadedTextureManager();
        if (textureManager == null)
            throw new RuntimeException("Cannot resolve TextureSource from " + Utils.getSimpleName(polygon) + " in " + Utils.getSimpleName(this) + ", because there is no shaded texture manager! Override the method if necessary.");

        return textureManager.getShadedTexture(polygon);
    }
}
