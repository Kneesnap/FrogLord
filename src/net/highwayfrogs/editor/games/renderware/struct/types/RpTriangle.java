package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.generic.data.IGameObject;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXTextureShader;
import net.highwayfrogs.editor.games.renderware.RwVersion;
import net.highwayfrogs.editor.games.renderware.chunks.RwImageChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwMaterialChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwTextureChunk;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents the RpTriangle struct defined in bageomet.h
 * Created by Kneesnap on 8/17/2024.
 */
@Getter
public class RpTriangle extends RwStruct {
    private final IRwGeometryMesh parentMesh;
    private final int[] vertexIndices = new int[VERTEX_COUNT];
    private int materialIndex = -1;

    public static final int VERTEX_COUNT = 3;
    public static final int SIZE_IN_BYTES = 4 * Constants.SHORT_SIZE;

    public RpTriangle(IRwGeometryMesh parentMesh) {
        super(parentMesh.getGameInstance(), RwStructType.TRIANGLE);
        this.parentMesh = parentMesh;
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        if (RwVersion.isAtLeast(version, RwVersion.VERSION_3602)) {
            for (int i = 0; i < this.vertexIndices.length; i++)
                this.vertexIndices[i] = reader.readUnsignedShortAsInt();
            this.materialIndex = reader.readUnsignedShortAsInt();
        } else {
            this.materialIndex = reader.readUnsignedShortAsInt();
            for (int i = 0; i < this.vertexIndices.length; i++)
                this.vertexIndices[i] = reader.readUnsignedShortAsInt();
        }
    }

    /**
     * Loads the triangle using the RpGeometry format seen in bageomet.c
     * @param reader the reader to read the data from
     */
    public void loadGeometryFormat(DataReader reader) {
        this.vertexIndices[1] = reader.readUnsignedShortAsInt();
        this.vertexIndices[0] = reader.readUnsignedShortAsInt();
        this.materialIndex = reader.readUnsignedShortAsInt();
        this.vertexIndices[2] = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer, int version) {
        if (!RwVersion.isAtLeast(version, RwVersion.VERSION_3602))
            writer.writeUnsignedShort(this.materialIndex);

        for (int i = 0; i < this.vertexIndices.length; i++)
            writer.writeUnsignedShort(this.vertexIndices[i]);
        if (RwVersion.isAtLeast(version, RwVersion.VERSION_3602))
            writer.writeUnsignedShort(this.materialIndex);
    }

    /**
     * Saves the triangle using the RpGeometry format seen in bageomet.c
     * @param writer the writer to write the data to
     */
    public void saveGeometryFormat(DataWriter writer) {
        writer.writeUnsignedShort(this.vertexIndices[1]);
        writer.writeUnsignedShort(this.vertexIndices[0]);
        writer.writeUnsignedShort(this.materialIndex);
        writer.writeUnsignedShort(this.vertexIndices[2]);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Vertex Indices", Arrays.toString(this.vertexIndices));
        propertyList.add("Material Index", this.materialIndex);
        return propertyList;
    }

    @Override
    public String toString() {
        return "RpTriangle{vertices=" + Arrays.toString(this.vertexIndices) + ",materialIndex=" + this.materialIndex + "}";
    }

    /**
     * Creates a texture shade definition for this triangle.
     * @param worldMesh the mesh to create the shade definition for
     * @param enableGouraudShading Whether to enable gouraud shading.
     */
    public PSXShadeTextureDefinition createPolygonShadeDefinition(PSXShadedDynamicMesh<RpTriangle, ?> worldMesh, boolean enableGouraudShading) {
        PSXPolygonType polygonType;
        CVector[] colors;
        if (!enableGouraudShading || this.parentMesh.getPreLitColors().isEmpty()) {
            polygonType = PSXPolygonType.POLY_FT3;
            colors = new CVector[1];
            colors[0] = PSXTextureShader.UNSHADED_COLOR;
        } else {
            // Check if the colors appear flat.

            RwColorRGBA useFlatColor = this.parentMesh.getPreLitColors().get(this.vertexIndices[0]);
            for (int i = 1; i < this.vertexIndices.length; i++) {
                if (!Objects.equals(useFlatColor, this.parentMesh.getPreLitColors().get(this.vertexIndices[i]))) {
                    useFlatColor = null;
                    break;
                }
            }

            // Check if the colors appear flat.
            if (useFlatColor != null) {
                polygonType = PSXPolygonType.POLY_FT3;
                colors = new CVector[1];
                colors[0] = CVector.makeColorFromRGB(useFlatColor.getColorARGB());
            } else {
                polygonType = PSXPolygonType.POLY_GT3;
                colors = new CVector[VERTEX_COUNT];
                for (int i = 0; i < colors.length; i++)
                    colors[i] = CVector.makeColorFromRGB(this.parentMesh.getPreLitColors().get(this.vertexIndices[i]).getColorARGB());
            }
        }

        SCByteTextureUV[] uvs = new SCByteTextureUV[VERTEX_COUNT];
        for (int i = 0; i < uvs.length; i++) {
            RwTexCoords texCoords = this.parentMesh.getTexCoordSets().get(0).get(this.vertexIndices[i]);
            uvs[i] = new SCByteTextureUV(texCoords.getU(), texCoords.getV());
        }

        // Create definition.
        RwMaterialChunk material = this.parentMesh.getMaterial(this);
        RwTextureChunk texture = material != null ? material.getTexture() : null;
        RwImageChunk image = texture != null ? texture.getImage() : null;
        return new PSXShadeTextureDefinition(worldMesh.getShadedTextureManager(), polygonType, image, colors, uvs, false, true);
    }

    public interface IRwGeometryMesh extends IGameObject, IBinarySerializable {
        /**
         * Gets the mesh vertices, if vertices are present.
         */
        List<RwV3d> getVertices();

        /**
         * Gets the mesh vertex normals, if present.
         */
        List<RpVertexNormal> getNormals();

        /**
         * Gets the mesh pre-lit vertex colors, if present.
         */
        List<RwColorRGBA> getPreLitColors();

        /**
         * Gets the mesh triangles, if present.
         */
        List<RpTriangle> getTriangles();

        /**
         * Gets the mesh texCoord sets, if present.
         */
        List<List<RwTexCoords>> getTexCoordSets();

        /**
         * Gets the material used by the triangle.
         * @param triangle the triangle to get the material for
         * @return material, if there is one
         */
        RwMaterialChunk getMaterial(RpTriangle triangle);
    }
}