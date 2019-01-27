package net.highwayfrogs.editor.file.map.poly;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;

/**
 * Represents a MAP primitive.
 * Created by Kneesnap on 1/26/2019.
 */
@Getter
public abstract class MAPPrimitive extends PSXGPUPrimitive {
    private MAPPrimitiveType type;

    public MAPPrimitive(MAPPrimitiveType type) {
        this.type = type;
    }
}
