package net.highwayfrogs.editor.file.map.poly.line;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;

/**
 * Represents a PSX line.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class MAPLine extends MAPPrimitive {
    public MAPLine(MAPPrimitiveType type, int verticeCount) {
        super(type, verticeCount);
    }
}
