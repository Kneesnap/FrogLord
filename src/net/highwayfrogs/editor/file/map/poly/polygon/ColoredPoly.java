package net.highwayfrogs.editor.file.map.poly.polygon;

import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;

import java.util.Objects;

/**
 * Created by Kneesnap on 2/21/2020.
 */
public interface ColoredPoly {

    public PSXColorVector[] getColors();

    public default void onDrawMap(MapMesh mesh) {

    }

    public default int makeHashCode() {
        PSXColorVector[] colors = getColors();

        if (colors.length == 1) {
            return colors[0].toRGB();
        } else {
            return ((colors[0].getRed() & 0xFF) << 24)
                    | ((colors[1].getGreen() & 0xFF) << 16)
                    | ((colors[2].getBlue() & 0xFF) << 8)
                    | ((colors.length > 3 ? colors[3].getRed() : colors[0].getBlue()) & 0xFF);
        }
    }

    public default boolean testEquals(Object other) {
        if (!(other instanceof ColoredPoly))
            return false;

        ColoredPoly otherPoly = (ColoredPoly) other;
        PSXColorVector[] colors = otherPoly.getColors();
        if (colors.length != getColors().length)
            return false;

        for (int i = 0; i < colors.length; i++)
            if (!Objects.equals(colors[i], getColors()[i]))
                return false;

        return true;
    }
}
