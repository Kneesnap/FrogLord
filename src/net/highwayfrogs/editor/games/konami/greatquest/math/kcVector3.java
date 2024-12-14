package net.highwayfrogs.editor.games.konami.greatquest.math;

import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.system.math.Vector3f;

/**
 * Represents the 'kcVector3' struct as defined in kcMath3D.h.
 * Created by Kneesnap on 8/21/2023.
 */
public class kcVector3 extends Vector3f implements IInfoWriter {
    public kcVector3() {
        super();
    }

    public kcVector3(float x, float y, float z) {
        super(x, y, z);
    }

    public kcVector3(Vector3f vec) {
        super(vec);
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append(getX())
                .append(", ")
                .append(getY())
                .append(", ")
                .append(getZ());
    }
}