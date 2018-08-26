package net.highwayfrogs.editor.file.standard.psx.prims.line;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXPrimitiveType;

import java.util.function.Supplier;

/**
 * A registry of all PSX line types.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@AllArgsConstructor
public enum PSXLineType implements PSXPrimitiveType {
    G2(PSXLineG2::new, 12);

    private Supplier<PSXLine> maker;
    private final int byteLength;
}
