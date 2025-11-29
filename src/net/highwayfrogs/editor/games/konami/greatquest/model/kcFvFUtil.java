package net.highwayfrogs.editor.games.konami.greatquest.model;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GamePlatform;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains utilities for working with FVF (Flexible Vertex Format) values.
 * Created by Kneesnap on 10/29/2025.
 */
public class kcFvFUtil {
    // PS2 FVF Flags:
    public static final int FVF_FLAG_PS2_NORMALS_HAVE_W = Constants.BIT_FLAG_4; // 0x10
    public static final int FVF_FLAG_PS2_POSITIONS_HAVE_SIZE = Constants.BIT_FLAG_5; // 0x20
    public static final int FVF_FLAG_PS2_DIFFUSE_RGBA255 = Constants.BIT_FLAG_6; // 0x40 -> Doesn't seem to always be correct.
    public static final int FVF_FLAG_PS2_HAS_MATRIX = Constants.BIT_FLAG_12; // 0x1000, Also disables blend?

    // PC FVF Flags:
    public static final int FVF_FLAG_PC_NORMALS = Constants.BIT_FLAG_4; // 0x10
    public static final int FVF_FLAG_PC_PSIZE = Constants.BIT_FLAG_5; // 0x20
    public static final int FVF_FLAG_PC_DIFFUSE_RGBAI = Constants.BIT_FLAG_6; // 0x40
    public static final int FVF_FLAG_PC_SPECULAR_RGBAI = Constants.BIT_FLAG_7; // 0x80

    // Platform Independent:
    public static final int FVF_FLAG_COMPRESSED = Constants.BIT_FLAG_14; // 0x4000
    public static final int FVF_MASK_WEIGHTS_START = 1;
    public static final int FVF_MASK_WEIGHTS = 0b111 << FVF_MASK_WEIGHTS_START;
    public static final int FVF_MASK_TEXTURE_START = 8;
    public static final int FVF_MASK_TEXTURE = 0b1111 << FVF_MASK_TEXTURE_START;

    /**
     * Calculate the components with correct order for displaying terrain from a fvf value.
     * This is a recreation of the function 'kcFVFVertexGetOrder'.
     * @param fvf      The fvf value to calculate from.
     * @param platform The platform to calculate the order for.
     * @return orderedComponentList
     */
    public static kcVertexFormatComponent[] getTerrainComponents(int fvf, GamePlatform platform) {
        switch (platform) {
            case WINDOWS:
                return getTerrainComponentsPC(fvf);
            case PLAYSTATION_2:
                return getTerrainComponentsPS2(fvf);
            default:
                throw new RuntimeException("Cannot calculate vertex component FVF order for the platform: " + platform);
        }
    }

    /**
     * Calculate the components with correct order for displaying terrain from a fvf value.
     * This is a recreation of the function 'kcFVFVertexGetOrder' as found in the PS2 PAL version.
     * @param fvf The fvf value to calculate from.
     * @return orderedComponentList
     */
    public static kcVertexFormatComponent[] getTerrainComponentsPS2(int fvf) {
        List<kcVertexFormatComponent> components = new ArrayList<>(8);

        if ((fvf & FVF_FLAG_PS2_POSITIONS_HAVE_SIZE) == FVF_FLAG_PS2_POSITIONS_HAVE_SIZE) {
            components.add(kcVertexFormatComponent.POSITION_XYZF);
            components.add(kcVertexFormatComponent.PSIZE);
        } else {
            components.add(kcVertexFormatComponent.POSITION_XYZWF);
        }

        if ((fvf & FVF_FLAG_PS2_NORMALS_HAVE_W) == FVF_FLAG_PS2_NORMALS_HAVE_W)
            components.add(kcVertexFormatComponent.NORMAL_XYZWF);

        if ((fvf & FVF_FLAG_PS2_DIFFUSE_RGBA255) == FVF_FLAG_PS2_DIFFUSE_RGBA255)
            components.add(kcVertexFormatComponent.DIFFUSE_RGBA255F);

        long textureBits = (fvf & FVF_MASK_TEXTURE) >> FVF_MASK_TEXTURE_START;
        if (textureBits == 1) {
            components.add(kcVertexFormatComponent.TEX1_STQP);
        } else if (textureBits == 2) {
            components.add(kcVertexFormatComponent.TEX2F);
        }

        long weightBits = (fvf & FVF_MASK_WEIGHTS) >> FVF_MASK_WEIGHTS_START;
        if (weightBits == 0b110 || weightBits == 0b101 || weightBits == 0b100 || weightBits == 0b011)
            components.add(kcVertexFormatComponent.WEIGHT4F);

        if ((fvf & FVF_FLAG_PS2_HAS_MATRIX) == FVF_FLAG_PS2_HAS_MATRIX)
            components.add(kcVertexFormatComponent.MATRIX_INDICES);

        return components.toArray(new kcVertexFormatComponent[0]);
    }

    /**
     * Calculate the components in order for displaying terrain from the fvf value.
     * This is a recreation of the function 'kcFVFVertexGetOrder' as found in the PC version.
     * @param fvf The fvf value to calculate from.
     * @return orderedComponentList
     */
    public static kcVertexFormatComponent[] getTerrainComponentsPC(int fvf) {
        List<kcVertexFormatComponent> components = new ArrayList<>(8);

        int weightBits = ((fvf & FVF_MASK_WEIGHTS) >>> FVF_MASK_WEIGHTS_START);
        if (weightBits == 2) {
            components.add(kcVertexFormatComponent.POSITION_XYZWF);
        } else if (weightBits != 0) {
            components.add(kcVertexFormatComponent.POSITION_XYZF);
            switch (weightBits - 2) {
                case -1:
                    // Indicates there are no weights.
                    break;
                case 1:
                    components.add(kcVertexFormatComponent.WEIGHT1F);
                    break;
                case 2:
                    components.add(kcVertexFormatComponent.WEIGHT2F);
                    break;
                case 3:
                    components.add(kcVertexFormatComponent.WEIGHT3F);
                    break;
                case 4:
                    components.add(kcVertexFormatComponent.WEIGHT4F);
                    break;
                default:
                    throw new RuntimeException("Unexpected weightBits value: " + weightBits);
            }
        } else {
            // This is technically supported by the engine, but I'd like to be warned if we ever have a build with it, since it's not known how this would be handled.
            throw new RuntimeException("The kcModel had a weightBits value of zero! This may not indicate a problem, but instead warrants investigation!");
        }

        if ((fvf & FVF_FLAG_PC_NORMALS) == FVF_FLAG_PC_NORMALS)
            components.add(kcVertexFormatComponent.NORMAL_XYZF);
        if ((fvf & FVF_FLAG_PC_PSIZE) == FVF_FLAG_PC_PSIZE)
            components.add(kcVertexFormatComponent.PSIZE);
        if ((fvf & FVF_FLAG_PC_DIFFUSE_RGBAI) == FVF_FLAG_PC_DIFFUSE_RGBAI)
            components.add(kcVertexFormatComponent.DIFFUSE_RGBAI);
        if ((fvf & FVF_FLAG_PC_SPECULAR_RGBAI) == FVF_FLAG_PC_SPECULAR_RGBAI)
            components.add(kcVertexFormatComponent.SPECULAR_RGBAI);

        long textureBits = (fvf & FVF_MASK_TEXTURE) >> FVF_MASK_TEXTURE_START;
        if (textureBits == 2) {
            components.add(kcVertexFormatComponent.TEX2F);
        } else if (textureBits == 1) {
            components.add(kcVertexFormatComponent.TEX1F);
        }

        return components.toArray(new kcVertexFormatComponent[0]);
    }

    /**
     * Calculate the fvf value for displaying terrain from an array of components.
     * This is an inverse of the function 'kcFVFVertexGetOrder'.
     * @param components the components to calculate the FvF from.
     * @param platform The platform to calculate the FvF for.
     * @return fvf
     */
    public static int calculateTerrainFvF(kcVertexFormatComponent[] components, GamePlatform platform) {
        switch (platform) {
            case WINDOWS:
                return calculateTerrainFvF_PC(components);
            case PLAYSTATION_2:
                return calculateTerrainFvF_PS2(components);
            default:
                throw new RuntimeException("Cannot calculate vertex FvF for the platform: " + platform);
        }
    }

    /**
     * Calculate the fvf value for displaying terrain from an array of components.
     * This is the inverse of the function 'kcFVFVertexGetOrder' as found in the PS2 version.
     * @param components the components to calculate the FvF from.
     * @return fvf
     */
    public static int calculateTerrainFvF_PS2(kcVertexFormatComponent[] components) {
        int fvf = 0;
        int weightBits = -1;
        int textureBits = -1;

        for (int i = 0; i < components.length; i++) {
            kcVertexFormatComponent component = components[i];
            switch (component) {
                case POSITION_XYZF:
                case PSIZE:
                    fvf |= FVF_FLAG_PS2_POSITIONS_HAVE_SIZE;
                    break;
                case POSITION_XYZWF:
                    // Valid, do nothing.
                    break;
                case NORMAL_XYZWF:
                    fvf |= FVF_FLAG_PS2_NORMALS_HAVE_W;
                    break;
                case DIFFUSE_RGBA255F:
                    fvf |= FVF_FLAG_PS2_DIFFUSE_RGBA255;
                    break;
                case TEX1_STQP:
                    if (textureBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because textureBits is already set to " + textureBits + ".");

                    textureBits = 1;
                    break;
                case TEX2F:
                    if (textureBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because textureBits is already set to " + textureBits + ".");

                    textureBits = 2;
                    break;
                case WEIGHT4F:
                    if (weightBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because weightBits is already set to " + weightBits + ".");

                    // The game code makes it so weightBits could theoretically be 1, 2, 3, or 4, but all four are treated as WEIGHT4F, with no way to distinguish the real weightBits number.
                    // This means there are indeed four values in all 3D models, even if not all four are used.
                    // But, through observing the actual PS2 game data, this has only ever been seen to be one, so we'll treat it as one here.
                    // The PS2 version has at least some code theoretically supporting FvF values for different weight values, after the data is loaded.
                    weightBits = 1;
                    break;
                case MATRIX_INDICES:
                    fvf |= FVF_FLAG_PS2_HAS_MATRIX;
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported kcVertexFormatComponent: " + component);
            }
        }

        fvf |= ((weightBits + 2) << FVF_MASK_WEIGHTS_START) & FVF_MASK_WEIGHTS; // -1 is a valid value for weightBits here, and indicates no weights.
        if (textureBits != -1)
            fvf |= (textureBits << FVF_MASK_TEXTURE_START) & FVF_MASK_TEXTURE;

        return fvf | FVF_FLAG_COMPRESSED;
    }

    /**
     * Calculate the fvf value for displaying terrain from an array of components.
     * This is the inverse of the function 'kcFVFVertexGetOrder' as found in the PC version.
     * @param components the components to calculate the FvF from.
     * @return fvf
     */
    public static int calculateTerrainFvF_PC(kcVertexFormatComponent[] components) {
        int fvf = 0;
        int weightBits = -1; // If the weightBits is -1, that means there are no weights. (Ie: -1 is a valid value)
        int textureBits = -1;
        for (int i = 0; i < components.length; i++) {
            kcVertexFormatComponent component = components[i];
            switch (component) {
                case POSITION_XYZWF:
                    if (weightBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because weightBits is already set to " + weightBits + ".");
                    weightBits = 0;
                    break;
                case POSITION_XYZF:
                    // Valid, do nothing.
                    break;
                case WEIGHT1F:
                    if (weightBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because weightBits is already set to " + weightBits + ".");
                    weightBits = 1;
                    break;
                case WEIGHT2F:
                    if (weightBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because weightBits is already set to " + weightBits + ".");
                    weightBits = 2;
                    break;
                case WEIGHT3F:
                    if (weightBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because weightBits is already set to " + weightBits + ".");
                    weightBits = 3;
                    break;
                case WEIGHT4F:
                    if (weightBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because weightBits is already set to " + weightBits + ".");
                    weightBits = 4;
                    break;
                case NORMAL_XYZF:
                    fvf |= FVF_FLAG_PC_NORMALS;
                    break;
                case PSIZE:
                    fvf |= FVF_FLAG_PC_PSIZE;
                    break;
                case DIFFUSE_RGBAI:
                    fvf |= FVF_FLAG_PC_DIFFUSE_RGBAI;
                    break;
                case SPECULAR_RGBAI:
                    fvf |= FVF_FLAG_PC_SPECULAR_RGBAI;
                    break;
                case TEX1F:
                    if (textureBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because textureBits is already set to " + textureBits + ".");

                    textureBits = 1;
                    break;
                case TEX2F:
                    if (textureBits != -1)
                        throw new RuntimeException("Cannot process " + component + ", because textureBits is already set to " + textureBits + ".");

                    textureBits = 2;
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported kcVertexFormatComponent: " + component);
            }
        }

        fvf |= ((weightBits + 2) << FVF_MASK_WEIGHTS_START) & FVF_MASK_WEIGHTS; // If the weightBits is -1, it's still valid, and just means there are no weights.
        if (textureBits != -1)
            fvf |= (textureBits << FVF_MASK_TEXTURE_START) & FVF_MASK_TEXTURE;

        return fvf;
    }
}
