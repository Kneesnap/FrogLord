package net.highwayfrogs.editor.file.map.poly.polygon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;

import java.util.function.Supplier;

/**
 * A registry of PSX polygon types.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@AllArgsConstructor
public enum MAPPolygonType implements MAPPrimitiveType {
    F3(MAPPolyF3::new, 12), // "Flat shaded" triangle.
    F4(MAPPolyF4::new, 12), // "Flat shaded" rectangle.
    FT3(MAPPolyFT3::new, 28), // Flat textured triangle.
    FT4(MAPPolyFT4::new, 28), // Flat textured rectangle.
    G3(MAPPolyG3::new, 20), // "Gouraud shaded" triangle.
    G4(MAPPolyG4::new, 24), // "Gouraud shaded" rectangle.
    GT3(MAPPolyGT3::new, 36), // "Gouraud shaded" + TEXTURED triangle.
    GT4(MAPPolyGT4::new, 40); // "Gouraud shaded" + TEXTURED rectangle.

    private final Supplier<MAPPolygon> maker;
    private final int byteLength;
}
