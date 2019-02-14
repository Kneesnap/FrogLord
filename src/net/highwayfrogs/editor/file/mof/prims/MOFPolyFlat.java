package net.highwayfrogs.editor.file.mof.prims;

/**
 * Represents a flat MOF polygon.
 * Created by Kneesnap on 1/1/2019.
 */
public class MOFPolyFlat extends MOFColorPolygon {
    public MOFPolyFlat(MOFPrimType type, int verticeCount) {
        super(type, verticeCount, 1, 0);
    }
}
