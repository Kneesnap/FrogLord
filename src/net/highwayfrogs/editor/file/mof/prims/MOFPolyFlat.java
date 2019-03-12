package net.highwayfrogs.editor.file.mof.prims;

import net.highwayfrogs.editor.file.mof.MOFPart;

/**
 * Represents a flat MOF polygon.
 * Created by Kneesnap on 1/1/2019.
 */
public class MOFPolyFlat extends MOFColorPolygon {
    public MOFPolyFlat(MOFPart parent, MOFPrimType type, int verticeCount) {
        super(parent, type, verticeCount, 1, 0);
    }
}
