package net.highwayfrogs.editor.file.mof.prims;

import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.file.mof.MOFPart;

import java.util.function.Function;

/**
 * A registry of MOF Prim types.
 * Created by Kneesnap on 1/1/2019.
 */
@AllArgsConstructor
public enum MOFPrimType {
    F3(MOFPolyF3::new),
    F4(MOFPolyF4::new),
    FT3(MOFPolyFT3::new),
    FT4(MOFPolyFT4::new),
    G3(MOFPolyG3::new),
    G4(MOFPolyG4::new),
    GT3(MOFPolyGT3::new),
    GT4(MOFPolyGT4::new),
    E3(MOFPolyE3::new), // Unused by Frogger, though it exists in the PC rendering dlls.
    E4(MOFPolyE4::new), // Unused by Frogger, though it exists in the PC rendering dlls.
    LF2(MOFPolyLF2::new), // Unused by Frogger, though it exists in the PC rendering dlls.
    LF3(MOFPolyLF3::new), // Unused by Frogger, though it exists in the PC rendering dlls.
    HLF3(MOFPolyHLF3::new), // Unused by Frogger, though it exists in the PC rendering dlls.
    HLF4(MOFPolyHLF4::new), // Unused by Frogger, though it exists in the PC rendering dlls.
    GE3(MOFPolyGE3::new), // Unused by Frogger, though it exists in the PC rendering dlls.
    GE4(MOFPolyGE4::new); // Unused by Frogger, though it exists in the PC rendering dlls.

    private final Function<MOFPart, MOFPolygon> maker;

    /**
     * Create a new MOFPrimitive.
     * @param part The part which will own this primitive.
     * @return mofPrimitive
     */
    public MOFPolygon makeNew(MOFPart part) {
        return maker.apply(part);
    }
}
