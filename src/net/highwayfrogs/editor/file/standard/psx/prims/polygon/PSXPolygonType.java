package net.highwayfrogs.editor.file.standard.psx.prims.polygon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXPrimitiveType;

import java.util.function.Supplier;

/**
 * A registry of PSX polygon types.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@AllArgsConstructor
public enum PSXPolygonType implements PSXPrimitiveType {
    F3(PSXPolyF3::new, 12), // "Flat shaded" triangle.
    F4(PSXPolyF4::new, 12), // "Flat shaded" rectangle.
    FT3(PSXPolyFT3::new, 28), // Flat textured triangle.
    FT4(PSXPolyFT4::new, 28), // Flat textured rectangle.
    G3(PSXPolyG3::new, 20), // "Gouraud shaded" triangle.
    G4(PSXPolyG4::new, 24), // "Gouraud shaded" rectangle.
    GT3(PSXPolyGT3::new, 36), // "Gouraud shaded" + TEXTURED triangle.
    GT4(PSXPolyGT4::new, 40); // "Gouraud shaded" + TEXTURED rectangle.

    private final Supplier<PSXPolygon> maker;
    private final int byteLength;
}
