package net.highwayfrogs.editor.games.sony.shared.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Utils;

import java.util.List;

/**
 * This represents a polygon seen in a Sony Cambridge game.
 * These games seem to usually use consistent vertex ordering.
 * Created by Kneesnap on 6/17/2024.
 */
public abstract class SCPolygonAdapterNode<TPolygon> extends DynamicMeshAdapterNode<TPolygon> {
    private final Vector2f tempVector = new Vector2f();
    @Getter private DynamicMeshDataEntry vertexEntry;

    public SCPolygonAdapterNode(PSXShadedDynamicMesh<TPolygon, ?> mesh) {
        super(mesh);
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();

        // Setup vertices.
        this.vertexEntry = new DynamicMeshDataEntry(getMesh());
        List<SVector> vertices = getAllVertices();
        for (int i = 0; i < vertices.size(); i++) {
            SVector vertex = vertices.get(i);
            this.vertexEntry.addVertexValue(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ());
        }
        addUnlinkedEntry(this.vertexEntry);
    }

    @Override
    public void clear() {
        super.clear();
        this.vertexEntry = null;
    }

    @Override
    protected DynamicMeshTypedDataEntry writeValuesToArrayAndCreateEntry(TPolygon polygon) {
        DynamicMeshTypedDataEntry newEntry = new DynamicMeshTypedDataEntry(getMesh(), polygon);
        addPolygonDataToEntries(polygon, newEntry);
        return newEntry;
    }

    // TODO: Consider shading with https://github.com/Teragam/JFXShader

    @Override
    public void updateTexCoord(DynamicMeshTypedDataEntry entry, int localTexCoordIndex) {
        TPolygon polygon = entry.getDataSource();

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
            List<SVector> vertices = getAllVertices();
            if (vertices == null || localVertexIndex >= vertices.size())
                throw new RuntimeException("Invalid vertex ID: " + localVertexIndex);

            SVector vertexPos = vertices.get(localVertexIndex);
            entry.writeVertexXYZ(localVertexIndex, vertexPos.getFloatX(), vertexPos.getFloatY(), vertexPos.getFloatZ());
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
    protected final Vector2f getTextureCoordinate(TPolygon polygon, ITextureSource textureSource, Texture texture, int index, Vector2f fallback) {
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
     * @param entry the entry to write texCoord and face data to
     */
    protected void addPolygonDataToEntries(TPolygon polygon, DynamicMeshDataEntry entry) {
        addPolygonDataToEntries(polygon, entry, entry);
    }

    /**
     * Adds the polygon data to the given entries.
     * @param polygon the polygon which has the data to write
     * @param texCoordEntry the entry to write texCoordEntries to
     * @param faceEntry the entry to write face data to
     */
    protected void addPolygonDataToEntries(TPolygon polygon, DynamicMeshDataEntry texCoordEntry, DynamicMeshDataEntry faceEntry) {
        if (texCoordEntry == null)
            throw new NullPointerException("texCoordEntry");
        if (faceEntry == null)
            throw new NullPointerException("faceEntry");

        // Resolve texture.
        ITextureSource textureSource = getTextureSource(polygon);
        Texture texture = getMesh().getTextureAtlas().getTextureFromSourceOrFallback(textureSource);

        // Add texture UVs.
        int[] polygonVertices = getVertices(polygon);
        if (getVertexCount(polygon) == 4) {
            int uvIndex1 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
            int uvIndex2 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
            int uvIndex3 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F
            int uvIndex4 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 3, Vector2f.ONE)); // uvBottomRight, 1F, 1F

            // Vertice IDs are the same IDs seen in the map data.
            int vtxIndex1 = this.vertexEntry.getVertexStartIndex() + polygonVertices[0];
            int vtxIndex2 = this.vertexEntry.getVertexStartIndex() + polygonVertices[1];
            int vtxIndex3 = this.vertexEntry.getVertexStartIndex() + polygonVertices[2];
            int vtxIndex4 = this.vertexEntry.getVertexStartIndex() + polygonVertices[3];

            // JavaFX uses counter-clockwise winding order.
            faceEntry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
            faceEntry.addFace(vtxIndex3, uvIndex3, vtxIndex4, uvIndex4, vtxIndex2, uvIndex2);
        } else {
            int uvIndex1 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 0, Vector2f.ZERO)); // uvTopLeft, 0F, 0F
            int uvIndex2 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 1, Vector2f.UNIT_X)); // uvTopRight, 1F, 0F
            int uvIndex3 = texCoordEntry.addTexCoordValue(getTextureCoordinate(polygon, textureSource, texture, 2, Vector2f.UNIT_Y)); // uvBottomLeft, 0F, 1F

            // Vertice IDs are the same IDs seen in the map data.
            int vtxIndex1 = this.vertexEntry.getVertexStartIndex() + polygonVertices[0];
            int vtxIndex2 = this.vertexEntry.getVertexStartIndex() + polygonVertices[1];
            int vtxIndex3 = this.vertexEntry.getVertexStartIndex() + polygonVertices[2];

            // JavaFX uses counter-clockwise winding order.
            faceEntry.addFace(vtxIndex3, uvIndex3, vtxIndex2, uvIndex2, vtxIndex1, uvIndex1);
        }
    }

    /**
     * Gets the shaded texture manager for the mesh.
     */
    @SuppressWarnings("unchecked")
    public PSXShadedTextureManager<TPolygon> getShadedTextureManager() {
        DynamicMesh mesh = getMesh();
        if (!(mesh instanceof IPSXShadedMesh))
            return null;

        return ((PSXShadedTextureManager<TPolygon>) ((IPSXShadedMesh) mesh).getShadedTextureManager());
    }

    /**
     * Gets the texture coordinates used to render the provided polygon
     * @param polygon the polygon to get the texture coordinate data from
     * @param index the index of the local vertex to evaluate
     * @param result the vector to store the results in
     * @return if the data was successfully obtained from the polygon
     */
    protected abstract boolean getPolygonTextureCoordinate(TPolygon polygon, int index, Vector2f result);

    /**
     * Gets a list of all the vertices available in the map.
     */
    public abstract List<SVector> getAllVertices();

    /**
     * Gets the vertex ids in the polygon.
     * This array may differ in size from the actual count of vertices, so use getVertexCount() instead.
     */
    protected abstract int[] getVertices(TPolygon polygon);

    /**
     * Gets the number of vertices in the polygon.
     */
    protected abstract int getVertexCount(TPolygon polygon);

    /**
     * Gets the texture source for the given polygon.
     * @param polygon the polygon to get the texture source from
     * @return textureSourceOrNull
     */
    protected ITextureSource getTextureSource(TPolygon polygon) {
        PSXShadedTextureManager<TPolygon> textureManager = getShadedTextureManager();
        if (textureManager == null)
            throw new RuntimeException("Cannot resolve TextureSource from " + Utils.getSimpleName(polygon) + " in " + Utils.getSimpleName(this) + ", because there is no shaded texture manager! Override the method if necessary.");

        return textureManager.getShadedTexture(polygon);
    }
}