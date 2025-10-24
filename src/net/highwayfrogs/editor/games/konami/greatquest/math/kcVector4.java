package net.highwayfrogs.editor.games.konami.greatquest.math;

import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.system.math.Vector4f;

/**
 * Represents the 'kcVector4' struct as defined in kcMath3D.h.
 * Created by Kneesnap on 7/12/2023.
 */
public class kcVector4 extends Vector4f implements IInfoWriter {
    public kcVector4() {
        super();
    }

    public kcVector4(float x, float y, float z, float w) {
        super(x, y, z, w);
    }

    public kcVector4(Vector4f vec) {
        super(vec);
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append(this.x)
                .append(", ")
                .append(this.y)
                .append(", ")
                .append(this.z)
                .append(", ")
                .append(this.w);
    }

    @Override
    public kcVector4 clone() {
        return new kcVector4(this.x, this.y, this.z, this.w);
    }
}