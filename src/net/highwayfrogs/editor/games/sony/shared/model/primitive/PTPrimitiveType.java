package net.highwayfrogs.editor.games.sony.shared.model.primitive;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.SCGameInstance;

/**
 * A registry of different primitive types.
 * Created by Kneesnap on 5/17/2024.
 */
@Getter
@AllArgsConstructor
public enum PTPrimitiveType {
    CONTROL(null, 8, 0, false, 0, 0, 0),
    F3(PSXPolygonType.POLY_F3, 12, 3, false, 1, 1, 0),
    F4(PSXPolygonType.POLY_F4, 12, 4, false, 1, 1, 0),
    FT3(PSXPolygonType.POLY_FT3, 24, 3, true, 1, 1, 0),
    FT4(PSXPolygonType.POLY_FT4, 24, 4, true, 1, 1, 0),
    G3(PSXPolygonType.POLY_F3, 16, 3, false, 1, 3, 0), // This is only gouraud because of the vertex lighting applied during the game. For FrogLord's purposes, we treat it as flat since our lighting isn't vertex-color based.
    G4(PSXPolygonType.POLY_F4, 16, 4, false, 1, 4, 0), // This is only gouraud because of the vertex lighting applied during the game. For FrogLord's purposes, we treat it as flat since our lighting isn't vertex-color based.
    GT3(PSXPolygonType.POLY_FT3, 28, 3, true, 1, 3, 0), // This is only gouraud because of the vertex lighting applied during the game. For FrogLord's purposes, we treat it as flat since our lighting isn't vertex-color based.
    GT4(PSXPolygonType.POLY_FT4, 32, 4, true, 1, 4, 0), // This is only gouraud because of the vertex lighting applied during the game. For FrogLord's purposes, we treat it as flat since our lighting isn't vertex-color based.
    C3(PSXPolygonType.POLY_G3, 24, 3, false, 3, 3, 0), // This is the real G3 polygon type.
    C4(PSXPolygonType.POLY_G4, 28, 4, false, 4, 4, 0), // This is the real G4 polygon type.
    CT3(PSXPolygonType.POLY_GT3, 36, 3, true, 3, 3, 0), // This is the real GT3 polygon type.
    CT4(PSXPolygonType.POLY_GT4, 44, 4, true, 4, 4, 0); // This is the real GT4 polygon type.
    // Seemingly unfinished.
    //E3(null, 20, 3, false, 1, 3, 3),
    //E4(null, 24, 4, false, 1, 4, 4),
    //R3(null, 32, 3, true, 1, 3, 3),
    //R4(null, 40, 4, true, 1, 4, 4);

    private final PSXPolygonType underlyingType;
    private final int sizeInBytes;
    private final int vertexCount;
    private final boolean textured;
    private final int colorCount;
    private final int normalCount;
    private final int environmentNormalCount;

    /**
     * Get the number of texture uvs.
     */
    public int getTextureUvCount() {
        return this.textured ? this.vertexCount : 0;
    }

    /**
     * Creates a new primitive instance.
     * @param instance The game instance to create the primitive instance for.
     * @return newPrimitiveInstance
     */
    public IPTPrimitive newPrimitive(SCGameInstance instance) {
        if (this == CONTROL) {
            return new PTPrimitiveControl(instance);
        } else {
            return new PTPolygon(instance, this);
        }
    }

    /**
     * Get the most appropriate PTPrimitiveType from the PSXPolygonType.
     * @param polygonType the polygon to get a primitive type from.
     * @return primitiveType
     */
    public static PTPrimitiveType getPrimitiveType(PSXPolygonType polygonType) {
        switch (polygonType) {
            case POLY_F3:
                return F3;
            case POLY_F4:
                return F4;
            case POLY_FT3:
                return FT3;
            case POLY_FT4:
                return FT4;
            case POLY_G3:
                return C3;
            case POLY_G4:
                return C4;
            case POLY_GT3:
                return CT3;
            case POLY_GT4:
                return CT4;
            default:
                throw new RuntimeException("Unsupported PSXPolygonType: " + polygonType);
        }
    }
}