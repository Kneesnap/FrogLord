package net.highwayfrogs.editor.games.sony.shared.landscaper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager;
import net.highwayfrogs.editor.games.psx.shading.PSXTextureShader;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.landscaper.mesh.LandscapeMapMesh;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a polygon usable in the Landscape.
 * Created by Kneesnap on 7/16/2024.
 */
public abstract class LandscapePolygon extends SCSharedGameData implements ILandscapeComponent {
    @Getter private final LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> owner;
    @Getter @Setter(AccessLevel.PACKAGE) private int polygonId = -1;
    private LandscapeVertex[] vertices; // Array is inaccessible to prevent accidental changes to the array when setVertex() should be used instead.
    @Getter private SCByteTextureUV[] textureUvs;
    @Getter private LandscapeTexture texture;
    @Getter private LandscapeMaterial material;

    // TODO: Consider if we can apply the UV system seen in Med2 onward.
    //   - There are four default UV values. Other UV values can be added to a list shared across all map polygons, accessible via index.
    //   - Results in extremely compact UV storage with full flexibility. Downside: splitting large textures across multiple polygons might be a little annoying? Think about it though.
    //   - This can be done either indexed at the time of use OR we could just do this at the time of conversion / editing. (Editing shows a checkbox if it matches one of the default ones.)

    public LandscapePolygon(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> owner) {
        this(owner, null);
    }

    public LandscapePolygon(LandscapeNode<? extends LandscapeVertex, ? extends LandscapePolygon> owner, PSXPolygonType polygonType) {
        super(owner.getGameInstance());
        this.owner = owner;
        if (polygonType != null)
            invalidateArrays(polygonType);
    }

    /**
     * Invalidates arrays and generates new ones.
     * @param polygonType the polygon type to generate arrays from
     */
    protected void invalidateArrays(PSXPolygonType polygonType) {
        // Stop tracking previous vertices.
        if (this.vertices != null) {
            for (int i = 0; i < this.vertices.length; i++) {
                LandscapeVertex vertex = this.vertices[i];
                if (vertex != null)
                    vertex.getInternalConnectedPolygonList().remove(this);
            }
        }

        // Allocate new vertex array if the old one isn't a good size.
        int newVertexCount = polygonType.getVerticeCount();
        if (this.vertices == null || this.vertices.length != newVertexCount)
            this.vertices = new LandscapeVertex[newVertexCount];

        // Allocate a new UV array if the old one isn't a good size.
        if (this.textureUvs == null || this.textureUvs.length != newVertexCount)
            this.textureUvs = new SCByteTextureUV[newVertexCount];
    }

    @Override
    public Landscape getLandscape() {
        return this.owner != null ? this.owner.getLandscape() : null;
    }

    @Override
    public boolean isRegistered() {
        return getLandscape() != null && this.polygonId >= 0;
    }

    @Override
    public void load(DataReader reader) {
        PSXPolygonType polygonType = PSXPolygonType.values()[reader.readUnsignedByteAsShort()];
        invalidateArrays(polygonType);

        // 1) Read vertices.
        for (int i = 0; i < this.vertices.length; i++) {
            int landscapeVertexId = reader.readInt();
            LandscapeVertex vertex = landscapeVertexId >= 0 ? getLandscape().getVertices().get(landscapeVertexId) : null;
            setVertex(i, vertex);
        }

        // 2) Read texture uvs.
        for (int i = 0; i < this.vertices.length; i++)
            this.textureUvs[i].load(reader);

        // 3) Read texture & material.
        int textureId = reader.readInt();
        setTexture((textureId >= 0) ? getLandscape().getTextures().get(textureId) : null);
        int materialId = reader.readInt();
        setMaterial((materialId >= 0) ? getLandscape().getMaterials().get(materialId) : null);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) getPolygonType().ordinal());

        // 1) Write vertices.
        for (int i = 0; i < this.vertices.length; i++) {
            LandscapeVertex vertex = this.vertices[i];
            writer.writeInt(vertex != null ? vertex.getVertexId() : -1);
        }

        // 2) Write texture uvs.
        for (int i = 0; i < this.vertices.length; i++)
            this.textureUvs[i].save(writer);

        // 3) Write texture & material.
        writer.writeInt(this.texture != null ? this.texture.getTextureId() : -1);
        writer.writeInt(this.material != null ? this.material.getMaterialId() : -1);
    }

    /**
     * Gets the vertex for the given local vertex id.
     * @param vertexId the vertex id to resolve
     * @return vertex
     */
    public LandscapeVertex getVertex(int vertexId) {
        if (vertexId < 0 || vertexId >= this.vertices.length)
            throw new ArrayIndexOutOfBoundsException("Invalid vertex ID: " + vertexId + ", there are " + this.vertices.length + " vertices.");

        return this.vertices[vertexId];
    }

    /**
     * Sets the vertex for the given local vertex id.
     * @param vertexId the vertex id to apply
     * @param newVertex the vertex object to apply, null is valid
     */
    public void setVertex(int vertexId, LandscapeVertex newVertex) {
        if (vertexId < 0 || vertexId >= this.vertices.length)
            throw new ArrayIndexOutOfBoundsException("Invalid vertex ID: " + vertexId + ", there are " + this.vertices.length + " vertices.");

        LandscapeVertex oldVertex = this.vertices[vertexId];
        if (oldVertex == newVertex)
            return; // No change.
        if (newVertex != null && newVertex.getLandscape() != getLandscape())
            throw new RuntimeException("The vertex belongs to a different Landscape, so it cannot be applied to this polygon!");
        if (newVertex != null && isRegistered() && !newVertex.isRegistered())
            throw new RuntimeException("The vertex is not registered to the Landscape, so the polygon cannot use it!");

        // Check other vertices to ensure we only change the connected polygon list at the right times when the same vertex is used more than once.
        int oldVertexCount = 0;
        int newVertexCount = 0;
        for (int i = 0; i < this.vertices.length; i++) {
            if (i == vertexId)
                continue; // Ignore the vertex we're replacing

            LandscapeVertex testVertex = this.vertices[i];
            if (testVertex == oldVertex)
                oldVertexCount++;
            if (testVertex == newVertex)
                newVertexCount++;
        }


        // Remove old tracking. (If we don't use the vertex anywhere else)
        if (oldVertexCount == 0 && oldVertex != null)
            oldVertex.getInternalConnectedPolygonList().remove(this);

        this.vertices[vertexId] = newVertex;

        // Apply new tracking. (If we don't use the vertex anywhere else)
        if (newVertexCount == 0 && newVertex != null && newVertex.isRegistered() && isRegistered())
            newVertex.getInternalConnectedPolygonList().add(this);
    }

    /**
     * Gets the shading color for the given local vertex id.
     * @param vertexId the vertex id to get the shading from
     * @return vertexColor
     */
    public CVector getVertexColor(int vertexId) {
        if (vertexId < 0 || vertexId >= this.vertices.length)
            throw new ArrayIndexOutOfBoundsException("Invalid vertex ID: " + vertexId + ", there are " + this.vertices.length + " vertices.");

        return getVertexColorImpl(vertexId);
    }

    /**
     * Resolves the currently active texture if there is one.
     */
    public LandscapeTexture resolveTexture() {
        if (this.texture != null) {
            return this.texture;
        } else if (this.material != null && this.material.getTexture() != null) {
            return this.material.getTexture();
        } else {
            return null;
        }
    }

    /**
     * Applies a texture to the polygon.
     * @param newTexture the texture to apply, null is supported
     */
    public void setTexture(LandscapeTexture newTexture) {
        if (this.texture == newTexture)
            return; // No change.
        if (newTexture != null && newTexture.getLandscape() != getLandscape())
            throw new RuntimeException("The texture belongs to a different Landscape, so it cannot be applied to this polygon!");
        if (newTexture != null && !newTexture.isRegistered() && isRegistered())
            throw new RuntimeException("The texture is not registered to the Landscape, so the polygon can't use it!");

        // Remove old tracking.
        LandscapeTexture oldTexture = this.texture;
        if (oldTexture != null)
            oldTexture.getInternalPolygonList().remove(this);

        this.texture = newTexture;

        // Apply new tracking.
        if (newTexture != null && newTexture.isRegistered() && isRegistered())
            newTexture.getInternalPolygonList().add(this);
    }

    /**
     * Applies a material to the polygon.
     * @param newMaterial the material to apply, null is supported
     */
    public void setMaterial(LandscapeMaterial newMaterial) {
        if (this.material == newMaterial)
            return; // No change.
        if (newMaterial != null && newMaterial.getLandscape() != getLandscape())
            throw new RuntimeException("The material belongs to a different Landscape, so it cannot be applied to this polygon!");
        if (newMaterial != null && !newMaterial.isRegistered() && isRegistered())
            throw new RuntimeException("The material is not registered to the Landscape, so the polygon can't use it!");

        // Remove old tracking.
        LandscapeMaterial oldMaterial = this.material;
        if (oldMaterial != null)
            oldMaterial.getInternalPolygonList().remove(this);

        this.material = newMaterial;

        // Apply new tracking.
        if (newMaterial != null && newMaterial.isRegistered() && isRegistered())
            newMaterial.getInternalPolygonList().add(this);
    }

    /**
     * Determines the polygon type based on the polygon properties.
     */
    public PSXPolygonType getPolygonType() {
        if (isQuad()) {
            if (isGouraudShaded()) {
                return isTextured() ? PSXPolygonType.POLY_GT4 : PSXPolygonType.POLY_G4;
            } else {
                return isTextured() ? PSXPolygonType.POLY_FT4 : PSXPolygonType.POLY_F4;
            }
        } else if (isTri()) {
            if (isGouraudShaded()) {
                return isTextured() ? PSXPolygonType.POLY_GT3 : PSXPolygonType.POLY_G3;
            } else {
                return isTextured() ? PSXPolygonType.POLY_FT3 : PSXPolygonType.POLY_F3;
            }
        }

        throw new RuntimeException("Failed to determine polygon type! (Vertices: " + Arrays.toString(this.vertices) + ")");
    }

    /**
     * Get the number of vertices this polygon has.
     */
    public int getVertexCount() {
        return this.vertices.length;
    }

    /**
     * Returns true iff the polygon is a quad (has 4 vertices).
     */
    public boolean isQuad() {
        return this.vertices.length == 4;
    }

    /**
     * Returns true iff the polygon is a tri (has 3 vertices).
     */
    public boolean isTri() {
        return this.vertices.length == 3;
    }

    /**
     * Returns true iff the polygon is textured.
     */
    public boolean isTextured() {
        return resolveTexture() != null;
    }

    /**
     * Returns true if the polygon can be shaded using flat shading.
     */
    public boolean isFlatShaded() {
        int colorCount = this.vertices.length;
        CVector firstColor = getVertexColor(0);
        for (int i = 1; i < colorCount; i++)
            if (!Objects.equals(firstColor, getVertexColorImpl(i)))
                return false;

        // All colors match, so we can use flat shading.
        return true;
    }

    /**
     * Returns true iff the polygon requires gouraud shading to display properly.
     */
    public boolean isGouraudShaded() {
        return !isFlatShaded();
    }

    /**
     * Creates a texture shade definition for this polygon.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(LandscapeMapMesh mesh, boolean shadingEnabled) {
        SCByteTextureUV[] uvs = null;
        if (isTextured()) {
            uvs = new SCByteTextureUV[this.textureUvs.length];
            for (int i = 0; i < uvs.length; i++)
                uvs[i] = this.textureUvs[i].clone();
        }

        // Determine the texture.
        LandscapeTexture texture = resolveTexture();
        ITextureSource textureSource = texture != null ? texture.getTextureSource() : null;

        // Clone colors.
        CVector[] colors = null;
        if (this.vertices.length > 0) {
            colors = new CVector[this.vertices.length];
            for (int i = 0; i < colors.length; i++)
                colors[i] = shadingEnabled || !isTextured() ? getVertexColorImpl(i).clone() : PSXTextureShader.UNSHADED_COLOR;
        }

        PSXShadedTextureManager<LandscapePolygon> shadedTextureManager = mesh != null ? mesh.getShadedTextureManager() : null;
        return new PSXShadeTextureDefinition(shadedTextureManager, getPolygonType(), textureSource, colors, uvs, isSemiTransparent(), true);
    }

    /**
     * Returns true iff the polygon should be rendered with semi-transparency.
     */
    public abstract boolean isSemiTransparent();

    /**
     * Gets the shading color for the given local vertex id.
     * Vertex colors can have different behavior in different games.
     * For example, in Frogger we want to use per-face colors only if they're set, otherwise we'll use the vertex color.
     * For most other games it will always be using exclusively colors defined on the vertices.
     * @param vertexId the local vertex id to obtain shading for
     * @return vertexColor
     */
    protected abstract CVector getVertexColorImpl(int vertexId);
}