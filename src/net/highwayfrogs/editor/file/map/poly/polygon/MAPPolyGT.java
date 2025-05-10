package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.utils.ColorUtils;

/**
 * Represents gouraud textured polys.
 * Created by Kneesnap on 2/21/2020.
 */
public class MAPPolyGT extends MAPPolyTexture {
    public MAPPolyGT(MAPPolygonType type, int verticeCount, int colorCount) {
        super(type, verticeCount, colorCount);
    }

    /**
     * Turn color data into a color which can be used to create images.
     * @param color The color to convert.
     * @return loadedColor
     */
    public static Color loadColor(PSXColorVector color) { // Color Application works with approximately this formula: (texBlue - (alphaAsPercentage * (texBlue - shadeBlue)))
        return ColorUtils.fromRGB(color.toShadeRGB(), (1D - Math.max(0D, Math.min(1D, ((color.getShadingRed() + color.getShadingGreen() + color.getShadingBlue()) / 127D / 3D)))));
    }
}