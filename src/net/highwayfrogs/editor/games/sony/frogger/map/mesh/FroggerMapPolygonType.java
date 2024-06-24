package net.highwayfrogs.editor.games.sony.frogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry of various Frogger map polygon types.
 * Created by Kneesnap on 5/26/2024.
 */
@Getter
public enum FroggerMapPolygonType {
    F3(PSXPolygonType.POLY_F3, 12, 12), // Flat Shaded Triangle
    F4(PSXPolygonType.POLY_F4, 12, 12), // Flat Shaded Quad
    FT3(PSXPolygonType.POLY_FT3, 28, 24), // Flat Shaded Textured Triangle
    FT4(PSXPolygonType.POLY_FT4, 28, 24), // Flat Shaded Textured Quad
    G3(PSXPolygonType.POLY_G3, 20, 20), // Gouraud Shaded Triangle
    G4(PSXPolygonType.POLY_G4, 24, 24), // Gouraud Shaded Quad
    GT3(PSXPolygonType.POLY_GT3, 36, 32), // Gouraud Shaded Textured Triangle
    GT4(PSXPolygonType.POLY_GT4, 40, 36), // Gouraud Shaded Textured Quad
    G2(null, 12, -1); // Gouraud Shaded Line

    private final PSXPolygonType internalType;
    private final int sizeInBytes;
    private final int earlyFormatSizeInBytes;
    private static final Map<PSXPolygonType, FroggerMapPolygonType> BY_INTERNAL_TYPE = new HashMap<>();

    FroggerMapPolygonType(PSXPolygonType polygonType, int sizeInBytes, int earlyFormatSizeInBytes) {
        this.internalType = polygonType;
        this.sizeInBytes = sizeInBytes;
        this.earlyFormatSizeInBytes = earlyFormatSizeInBytes;
    }

    /**
     * Gets whether this polygon type uses gouraud shading or not.
     */
    public boolean isGouraud() {
        return (this == G2) || this.internalType.isGouraud();
    }

    /**
     * Gets whether this polygon type uses a texture or not.
     */
    public boolean isTextured() {
        return (this != G2) && this.internalType.isTextured();
    }

    /**
     * Get the number of vertices available to this type.
     */
    public int getVertexCount() {
        return (this == G2) ? 2 : this.internalType.getVerticeCount();
    }

    /**
     * Get the number of colors available to this type.
     */
    public int getColorCount() {
        return (this == G2) ? 2 : this.internalType.getColorCount();
    }

    /**
     * Returns true iff this polygon type is a quad.
     */
    public boolean isQuad() {
        return (this != G2) && this.internalType.isQuad();
    }

    /**
     * Gets the frogger map polygon type by the internal polygon type.
     * @param polygonType The internal polygon type to lookup.
     */
    public static FroggerMapPolygonType getByInternalType(PSXPolygonType polygonType) {
        return BY_INTERNAL_TYPE.get(polygonType);
    }

    static {
        for (FroggerMapPolygonType type : values())
            BY_INTERNAL_TYPE.put(type.getInternalType(), type);
    }
}