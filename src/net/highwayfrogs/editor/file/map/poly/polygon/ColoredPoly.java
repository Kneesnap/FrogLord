package net.highwayfrogs.editor.file.map.poly.polygon;

import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;

/**
 * Created by Kneesnap on 2/21/2020.
 */
public interface ColoredPoly {

    public PSXColorVector[] getColors();

    public default void onDrawMap(MapMesh mesh) {

    }
}
