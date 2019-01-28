package net.highwayfrogs.editor.file.map.poly.line;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;

import java.util.function.Supplier;

/**
 * A registry of all PSX line types.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@AllArgsConstructor
public enum MAPLineType implements MAPPrimitiveType {
    G2(MAPLineG2::new, 12);

    private Supplier<MAPLine> maker;
    private final int byteLength;
}
