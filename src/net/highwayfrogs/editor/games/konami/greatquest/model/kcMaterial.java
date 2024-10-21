package net.highwayfrogs.editor.games.konami.greatquest.model;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.utils.IBinarySerializable;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a material.
 * Created by Kneesnap on 6/22/2023.
 */
@Getter
public class kcMaterial implements IMultiLineInfoWriter, IBinarySerializable {
    private String materialName;
    private String textureFileName;
    private int flags; // TODO: Let's document the different flags.
    private float xpVal = 0F;
    private float diffuseRed = 1F;
    private float diffuseGreen = 1F;
    private float diffuseBlue = 1F;
    private float diffuseAlpha = 1F;
    private float ambientRed = 1F;
    private float ambientGreen = 1F;
    private float ambientBlue = 1F;
    private float ambientAlpha = 1F;
    private float specularRed;
    private float specularGreen;
    private float specularBlue;
    private float specularAlpha = 1F;
    private float emissiveRed;
    private float emissiveGreen;
    private float emissiveBlue;
    private float emissiveAlpha = 1F;
    private float power = 1F;
    @Setter private transient GreatQuestImageFile texture;

    private static final int NAME_SIZE = 32;
    private static final int FILENAME_SIZE = 32;

    private static final int MATERIAL_FLAG_TEXTURED = Constants.BIT_FLAG_0; // 0x01: kcImportMaterialTexture(_kcMaterial*) will remove this flag if the texture is not found.
    private static final int MATERIAL_FLAG_ENABLE_ALPHA_BLEND = Constants.BIT_FLAG_3; // 0x08, Confirmed via SetMaterial(_kcMaterial*). Ignored in maps, and also in kcModel if blendMode != KCBLEND_DISABLE.

    /**
     * Tests if there is a texture assigned to this material.
     */
    public boolean hasTexture() {
        return (this.flags & MATERIAL_FLAG_TEXTURED) == MATERIAL_FLAG_TEXTURED;
    }

    @Override
    public void load(DataReader reader) {
        this.materialName = reader.readNullTerminatedFixedSizeString(NAME_SIZE, Constants.NULL_BYTE);
        this.textureFileName = reader.readNullTerminatedFixedSizeString(FILENAME_SIZE, Constants.NULL_BYTE);
        this.flags = reader.readInt();
        this.xpVal = reader.readFloat();
        this.diffuseRed = reader.readFloat();
        this.diffuseGreen = reader.readFloat();
        this.diffuseBlue = reader.readFloat();
        this.diffuseAlpha = reader.readFloat();
        this.ambientRed = reader.readFloat();
        this.ambientGreen = reader.readFloat();
        this.ambientBlue = reader.readFloat();
        this.ambientAlpha = reader.readFloat();
        this.specularRed = reader.readFloat();
        this.specularGreen = reader.readFloat();
        this.specularBlue = reader.readFloat();
        this.specularAlpha = reader.readFloat();
        this.emissiveRed = reader.readFloat();
        this.emissiveGreen = reader.readFloat();
        this.emissiveBlue = reader.readFloat();
        this.emissiveAlpha = reader.readFloat();
        this.power = reader.readFloat();

        // TODO: kcImportTextures(kcModel*) overwrites the material colors & power.

        // The last value is a 32-bit integer: pTexture.
        // I suspect that this value is a pointer into malloc'd data for the texture.
        // In other words, this value only meant something to the program which wrote it.
        // Neither FrogLord nor the actual game itself is capable of using this value for anything useful.
        // kcCResourceModel::Load(char*) sets the texture path to be the folder containing the .vtx file, enabling textures to be resolved by their filename instead of this.
        // The value is overwritten when kcImportMaterialTexture is called (Specifically, the call to kcImportTexture will overwrite the existing value, if it exists), which occurs when loading maps + models.
        reader.skipInt();
    }

    /**
     * For some reason, any materials on a kcModel* passed to kcImportTextures() will have a lot of their values overwritten.
     * This function replicates that behavior.
     */
    public void applyModelMaterialInfo() {
        // Apply default ambient color data.
        this.ambientRed = 1F;
        this.ambientGreen = 1F;
        this.ambientBlue = 1F;
        this.ambientAlpha = 1F;

        // Apply default diffuse color data.
        this.diffuseRed = 1F;
        this.diffuseGreen = 1F;
        this.diffuseBlue = 1F;
        this.diffuseAlpha = 1F;

        // Apply default specular color data.
        this.specularRed = 0F;
        this.specularGreen = 0F;
        this.specularBlue = 0F;
        this.specularAlpha = 1F;

        // Apply emissive specular color data.
        this.emissiveRed = 0F;
        this.emissiveGreen = 0F;
        this.emissiveBlue = 0F;
        this.emissiveAlpha = 1F;

        // Apply default power.
        this.power = 0F;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeNullTerminatedFixedSizeString(this.materialName, NAME_SIZE);
        writer.writeNullTerminatedFixedSizeString(this.textureFileName, FILENAME_SIZE);
        writer.writeInt(this.flags);
        writer.writeFloat(this.xpVal);
        writer.writeFloat(this.diffuseRed);
        writer.writeFloat(this.diffuseGreen);
        writer.writeFloat(this.diffuseBlue);
        writer.writeFloat(this.diffuseAlpha);
        writer.writeFloat(this.ambientRed);
        writer.writeFloat(this.ambientGreen);
        writer.writeFloat(this.ambientBlue);
        writer.writeFloat(this.ambientAlpha);
        writer.writeFloat(this.specularRed);
        writer.writeFloat(this.specularGreen);
        writer.writeFloat(this.specularBlue);
        writer.writeFloat(this.specularAlpha);
        writer.writeFloat(this.emissiveRed);
        writer.writeFloat(this.emissiveGreen);
        writer.writeFloat(this.emissiveBlue);
        writer.writeFloat(this.emissiveAlpha);
        writer.writeFloat(this.power);
        writer.writeInt(0); // Runtime value (texture pointer)
    }

    /**
     * Writes material information to the string builder.
     * @param builder           The builder to write material information to.
     * @param textureFilePrefix The prefix for the texture path, can be null.
     * @param includeAmbient    Whether ambient information should be included.
     * @param includeSpecular   Whether specular information should be included.
     */
    public void writeWavefrontObjMaterial(StringBuilder builder, String textureFilePrefix, boolean includeAmbient, boolean includeSpecular) {
        builder.append("newmtl ").append(this.materialName).append(Constants.NEWLINE);
        builder.append("Kd ").append(this.diffuseRed).append(' ')
                .append(this.diffuseGreen).append(' ')
                .append(this.diffuseBlue).append(Constants.NEWLINE);
        if (includeAmbient) {
            builder.append("Ka ").append(this.ambientRed).append(' ')
                    .append(this.ambientGreen).append(' ')
                    .append(this.ambientBlue).append(Constants.NEWLINE);
        }

        if (includeSpecular) {
            builder.append("Ks ").append(this.specularRed).append(' ')
                    .append(this.specularGreen).append(' ')
                    .append(this.specularBlue).append(Constants.NEWLINE);
        }

        // Transparency.
        builder.append("d ").append(this.diffuseAlpha).append(Constants.NEWLINE);

        // Diffuse texture map.
        builder.append("map_Kd ");
        if (textureFilePrefix != null)
            builder.append(textureFilePrefix);
        if (this.textureFileName != null)
            builder.append(Utils.stripExtension(this.textureFileName));
        builder.append(".png").append(Constants.NEWLINE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("kcMaterial[Name='").append(this.materialName).append("',Texture='").append(this.textureFileName).append("']:").append(Constants.NEWLINE);
        String newPadding = padding + " ";
        builder.append(newPadding).append("Flags: ").append(Utils.toHexString(this.flags)).append(Constants.NEWLINE);
        builder.append(newPadding).append("xpVal: ").append(this.xpVal).append(Constants.NEWLINE);
        builder.append(newPadding).append("Diffuse: [Red=").append(this.diffuseRed).append(",Green=").append(this.diffuseGreen).append(",Blue=").append(this.diffuseBlue).append(",Alpha=").append(this.diffuseAlpha).append("]").append(Constants.NEWLINE);
        builder.append(newPadding).append("Ambient: [Red=").append(this.ambientRed).append(",Green=").append(this.ambientGreen).append(",Blue=").append(this.ambientBlue).append(",Alpha=").append(this.ambientAlpha).append("]").append(Constants.NEWLINE);
        builder.append(newPadding).append("Specular: [Red=").append(this.specularRed).append(",Green=").append(this.specularGreen).append(",Blue=").append(this.specularBlue).append(",Alpha=").append(this.specularAlpha).append("]").append(Constants.NEWLINE);
        builder.append(newPadding).append("Emissive: [Red=").append(this.emissiveRed).append(",Green=").append(this.emissiveGreen).append(",Blue=").append(this.emissiveBlue).append(",Alpha=").append(this.emissiveAlpha).append("]").append(Constants.NEWLINE);
        builder.append(newPadding).append("Power: ").append(this.power).append(Constants.NEWLINE);
    }
}