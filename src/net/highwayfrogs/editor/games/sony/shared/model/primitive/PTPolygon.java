package net.highwayfrogs.editor.games.sony.shared.model.primitive;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.model.meshview.PTModelMesh;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.util.Arrays;

/**
 * Represents a model polygon in a later SC game.
 * The polygons use consistent ordering between colors, uvs, and vertices.
 * Assuming viewing from the default perspective, a quad is ordered like this:
 * 1---2
 * |  /|
 * | / |
 * |/  |
 * 3---4
 * This order matches how the PSX GPU processes a quad, first using vertices 1-2-3, then 2-3-4, according to <a href="https://psx-spx.consoledev.net/graphicsprocessingunitgpu/">this link</a>.
 * Created by Kneesnap on 5/17/2024.
 */
@Getter
public class PTPolygon extends SCGameData<SCGameInstance> implements IPTPrimitive {
    private int[] vertices = EMPTY_INT_ARRAY;
    private SCByteTextureUV[] textureUvs = EMPTY_UV_ARRAY;
    private CVector[] colors = EMPTY_COLOR_ARRAY;
    private int[] normals = EMPTY_INT_ARRAY;
    private int[] environmentNormals = EMPTY_INT_ARRAY;
    private PTPrimitiveType polygonType;
    private int clutIndex;
    private int texturePage;
    private int imageId;

    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final CVector[] EMPTY_COLOR_ARRAY = new CVector[0];
    private static final SCByteTextureUV[] EMPTY_UV_ARRAY = new SCByteTextureUV[0];

    private static final CVector UNSHADED_COLOR = CVector.makeColorFromRGB(0x80808080);

    public PTPolygon(SCGameInstance instance, PTPrimitiveType polygonType) {
        super(instance);
        setPolygonType(polygonType);
    }

    @Override
    public void load(DataReader reader) {
        int combinedVertex = reader.readInt();
        for (int i = 0; i < getVertexCount(); i++) {
            this.vertices[i] = (combinedVertex & 0xFF);
            combinedVertex >>>= 8;
        }

        // Read colors.
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].load(reader);

        // Read texture data.
        if (getPolygonType().isTextured()) {
            this.textureUvs[0].load(reader);
            this.clutIndex = reader.readUnsignedShortAsInt();
            this.textureUvs[1].load(reader);
            this.texturePage = reader.readUnsignedShortAsInt();
            this.textureUvs[2].load(reader);
            this.imageId = reader.readUnsignedShortAsInt();
            if (this.textureUvs.length > 3)
                this.textureUvs[3].load(reader);
        }

        // Read normals.
        for (int i = 0; i < this.normals.length; i++)
            this.normals[i] = reader.readUnsignedShortAsInt();

        // Read environment normals.
        for (int i = 0; i < this.environmentNormals.length; i++)
            this.environmentNormals[i] = reader.readUnsignedShortAsInt();

        // Align to end.
        reader.alignRequireEmpty(Constants.INTEGER_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        int combinedVertex = 0;
        for (int i = this.vertices.length - 1; i >= 0; i--) {
            if (this.vertices[i] > 0xFF)
                throw new RuntimeException("Too many vertices in primitive block!");

            combinedVertex <<= 8;
            combinedVertex |= this.vertices[i];
        }
        writer.writeInt(combinedVertex);

        // Write colors.
        for (int i = 0; i < this.colors.length; i++)
            this.colors[i].save(writer);

        // Write texture data.
        if (getPolygonType().isTextured()) {
            this.textureUvs[0].save(writer);
            writer.writeUnsignedShort(this.clutIndex);
            this.textureUvs[1].save(writer);
            writer.writeUnsignedShort(this.texturePage);
            this.textureUvs[2].save(writer);
            writer.writeUnsignedShort(this.imageId);
            if (this.textureUvs.length > 3)
                this.textureUvs[3].save(writer);
        }

        // Write normals.
        for (int i = 0; i < this.normals.length; i++)
            writer.writeUnsignedShort(this.normals[i]);

        // Write environment normals.
        for (int i = 0; i < this.environmentNormals.length; i++)
            writer.writeUnsignedShort(this.environmentNormals[i]);

        // Align to end.
        writer.align(Constants.INTEGER_SIZE);
    }

    @Override
    public PTPrimitiveType getPrimitiveType() {
        return this.polygonType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName()).append("{").append(getPolygonType())
                .append(", Texture: ").append(this.imageId).append('/').append(this.clutIndex).append('/').append(this.texturePage)
                .append(", vertices=[");
        for (int i = 0; i < this.vertices.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(this.vertices[i]);
        }

        builder.append(", colors=[");
        for (int i = 0; i < this.colors.length; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(this.colors[i]);
        }

        return builder.append("]}").toString();
    }

    /**
     * Get the number of vertices supported by this polygon.
     */
    public int getVertexCount() {
        return getPolygonType().getVertexCount();
    }

    /**
     * If the polygon renders as partially transparent, this will return true.
     */
    public boolean isSemiTransparent() {
        return this.colors.length > 0 && this.colors[0].isCodeValid() && (this.colors[0].getCode() & CVector.FLAG_SEMI_TRANSPARENT) == CVector.FLAG_SEMI_TRANSPARENT;
    }

    /**
     * Set the type of the polygon.
     */
    public void setPolygonType(PTPrimitiveType newType) {
        if (newType == null || newType == PTPrimitiveType.CONTROL)
            throw new IllegalArgumentException("PTPolygon cannot be set to type " + newType + ".");

        // Change vertices.
        if (newType.getVertexCount() != this.vertices.length) {
            if (newType.getVertexCount() > 0) {
                int[] newVertices = new int[newType.getVertexCount()];
                Arrays.fill(newVertices, -1);
                System.arraycopy(this.vertices, 0, newVertices, 0, Math.min(this.vertices.length, newVertices.length));
                this.vertices = newVertices;
            } else {
                this.vertices = EMPTY_INT_ARRAY;
            }
        }

        // Change texture uvs.
        if (newType.getTextureUvCount() != this.textureUvs.length) {
            if (newType.getTextureUvCount() > 0) {
                SCByteTextureUV[] newTextureUvs = new SCByteTextureUV[newType.getTextureUvCount()];
                System.arraycopy(this.textureUvs, 0, newTextureUvs, 0, Math.min(this.textureUvs.length, newTextureUvs.length));
                for (int i = this.textureUvs.length; i < newTextureUvs.length; i++)
                    newTextureUvs[i] = new SCByteTextureUV();

                this.textureUvs = newTextureUvs;
            } else {
                this.textureUvs = EMPTY_UV_ARRAY;
            }
        }

        // Change colors.
        if (newType.getColorCount() != this.colors.length) {
            if (newType.getColorCount() > 0) {
                CVector[] newColors = new CVector[newType.getColorCount()];
                System.arraycopy(this.colors, 0, newColors, 0, Math.min(this.colors.length, newColors.length));
                for (int i = this.colors.length; i < newColors.length; i++)
                    newColors[i] = UNSHADED_COLOR.clone();

                this.colors = newColors;
            } else {
                this.colors = EMPTY_COLOR_ARRAY;
            }
        }

        // Change normals.
        if (newType.getNormalCount() != this.normals.length) {
            if (newType.getNormalCount() > 0) {
                int[] newNormals = new int[newType.getNormalCount()];
                Arrays.fill(newNormals, -1);
                System.arraycopy(this.normals, 0, newNormals, 0, Math.min(this.normals.length, newNormals.length));
                this.normals = newNormals;
            } else {
                this.normals = EMPTY_INT_ARRAY;
            }
        }

        // Change environment normals.
        if (newType.getEnvironmentNormalCount() != this.environmentNormals.length) {
            if (newType.getEnvironmentNormalCount() > 0) {
                int[] newEnvironmentNormals = new int[newType.getEnvironmentNormalCount()];
                Arrays.fill(newEnvironmentNormals, -1);
                System.arraycopy(this.environmentNormals, 0, newEnvironmentNormals, 0, Math.min(this.environmentNormals.length, newEnvironmentNormals.length));
                this.environmentNormals = newEnvironmentNormals;
            } else {
                this.environmentNormals = EMPTY_INT_ARRAY;
            }
        }

        this.polygonType = newType;
    }

    /**
     * Gets the texture held by this polygon.
     * @return texture
     */
    public GameImage getTexture() {
        return getGameInstance().getMainArchive().getImageByTextureId(this.imageId);
    }

    /**
     * Creates a texture shade definition for this polygon.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(PTModelMesh modelMesh, boolean enableGouraudShading) {
        PTPrimitiveType polygonType = getPolygonType();
        boolean isSemiTransparent = isSemiTransparent();

        SCByteTextureUV[] uvs = null;
        if (polygonType.isTextured()) {
            uvs = new SCByteTextureUV[getVertexCount()];
            for (int i = 0; i < uvs.length; i++)
                uvs[i] = this.textureUvs[i].clone();
        }

        CVector[] colors = new CVector[polygonType.getColorCount()];
        if (enableGouraudShading || !polygonType.isTextured()) {
            for (int i = 0; i < colors.length; i++)
                colors[i] = this.colors[i].clone();
        } else {
            Arrays.fill(colors, UNSHADED_COLOR);
        }

        // Create definition.
        ITextureSource textureSource = polygonType.isTextured() ? getTexture() : null;
        return new PSXShadeTextureDefinition(modelMesh.getShadedTextureManager(), polygonType.getUnderlyingType(), textureSource, colors, uvs, isSemiTransparent);
    }

    /**
     * Creates a texture shade definition for this polygon.
     * @param shadeTexture The shaded texture to load from.
     */
    public void loadDataFromShadeDefinition(PSXShadeTextureDefinition shadeTexture, boolean isShadingEnabled) {
        // Update polygon type.
        PTPrimitiveType newPolygonType = PTPrimitiveType.getPrimitiveType(shadeTexture.getPolygonType());
        setPolygonType(newPolygonType);

        // Apply texture UVs.
        if (newPolygonType.isTextured() && shadeTexture.getTextureUVs() != null) {
            for (int i = 0; i < newPolygonType.getVertexCount(); i++) {
                SCByteTextureUV modifiedUv = shadeTexture.getTextureUVs()[i];
                if (modifiedUv != null)
                    this.textureUvs[i].copyFrom(modifiedUv);
            }
        }

        // Apply colors.
        if (isShadingEnabled && newPolygonType.getColorCount() > 0 && shadeTexture.getColors() != null)
            for (int i = 0; i < Math.min(this.colors.length, shadeTexture.getColors().length); i++)
                this.colors[i].copyFrom(shadeTexture.getColors()[i]);

        // Load texture.
        if (shadeTexture.getTextureSource() instanceof GameImage) {
            GameImage gameImage = (GameImage) shadeTexture.getTextureSource();
            this.imageId = gameImage.getTextureId();
        }
    }
}