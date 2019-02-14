package net.highwayfrogs.editor.system;

import net.highwayfrogs.editor.file.standard.psx.ByteUV;

/**
 * An interface for textured polygons.
 * Created by Kneesnap on 2/13/2019.
 */
public interface TexturedPoly {

    public ByteUV[] getUvs();

    default void performSwap() {

    }
}
