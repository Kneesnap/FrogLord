package net.highwayfrogs.editor.games.sony.shared.mof2.mesh;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;

/**
 * An ordered registry of the different MOF "primitive" types.
 * We call them the polygon types instead of primitive types because the non-polygon (line) primitives are exceedingly rare, and after seeing more than a hundred builds of Millennium games, the line primitives have never been seen. TODO: Perhaps include a warning if we do see them.
 * TODO: Why is there only one color regardless of gouraud/not???
 *   -> Is gouraud shading's only purpose for lighting?
 *   -> I bet it only exists for lighting purposes. Was it possible to choose this on a per-face basis or just a per-model basis? Hmm. I think perhaps I should make some documentation?
 * Created by Kneesnap on 2/19/2025.
 */
@Getter
@RequiredArgsConstructor
public enum MRMofPolygonType {
    F3(PSXPolygonType.POLY_F3, 3, 0, 1, false), // Flat Shaded Triangle
    F4(PSXPolygonType.POLY_F4, 4, 0, 1, false), // Flat Shaded Quad
    FT3(PSXPolygonType.POLY_FT3, 3, 0, 1, true), // Flat Shaded Textured Triangle
    FT4(PSXPolygonType.POLY_FT4, 4, 0, 1, true), // Flat Shaded Textured Quad
    G3(PSXPolygonType.POLY_F3, 3, 0, 3, false), // Gouraud Shaded Triangle
    G4(PSXPolygonType.POLY_F4, 4, 0, 4, false), // Gouraud Shaded Quad
    GT3(PSXPolygonType.POLY_FT3, 3, 0, 3, true), // Gouraud Shaded Textured Triangle
    GT4(PSXPolygonType.POLY_FT4, 4, 0, 4, true), // Gouraud Shaded Textured Quad
    E3(PSXPolygonType.POLY_F3, 3, 3, 1, false), // The MR API supports this, albeit I don't believe this was ever used outside of prototypes.
    E4(PSXPolygonType.POLY_F4, 4, 4, 1, false), // The MR API supports this, albeit I don't believe this was ever used outside of prototypes.
    LF2(null, 2, 0, 0, false), // The MR API supports this, albeit I don't believe this was ever used outside of prototypes.
    LF3(PSXPolygonType.POLY_F3, 3, 0, 0, false), // The MR API supports this, albeit I don't believe this was ever used outside of prototypes.
    HLF3(PSXPolygonType.POLY_F3, 3, 0, 0, false), // The MR API supports this, albeit I don't believe this was ever used outside of prototypes.
    HLF4(PSXPolygonType.POLY_F4, 4, 0, 0, false), // The MR API supports this, albeit I don't believe this was ever used outside of prototypes.
    GE3(PSXPolygonType.POLY_F3, 3, 3, 3, false), // The MR API supports this, albeit I don't believe this was ever used outside of prototypes.
    GE4(PSXPolygonType.POLY_F4, 4, 4, 4, false); // The MR API supports this, albeit I don't believe this was ever used outside of prototypes.

    private final PSXPolygonType internalType; // Used when displaying by FrogLord.
    private final int vertexCount;
    private final int environmentNormalCount;
    private final int normalCount;
    private final boolean textured;
}