package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShadingMode;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.utils.ColorUtils;

import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * Represents gouraud textured polys.
 * Created by Kneesnap on 2/21/2020.
 */
public class MAPPolyGT extends MAPPolyTexture {
    public MAPPolyGT(MAPPolygonType type, int verticeCount, int colorCount) {
        super(type, verticeCount, colorCount);
    }

    @Override
    public BufferedImage makeTexture(TextureMap map) {
        if (map.getMode() == ShadingMode.NO_SHADING) {
            GameImage image = getGameImage(map);
            return image != null ? image.toBufferedImage(map.getDisplaySettings()) : null;
        } else if (isOverlay(map)) {
            return makeShadeImage(map, false);
        } else {
            return makeShadedTexture(map, getGameImage(map).toBufferedImage(map.getDisplaySettings()));
        }
    }

    @Override
    public BigInteger makeIdentifier(TextureMap map) {
        if (map.getMode() == ShadingMode.NO_SHADING) {
            return makeIdentifier(0x7E8BA5E, getTextureId());
        } else {
            int[] colors = new int[getColors().length + 2];
            colors[0] = 0xF0A54ADE;
            colors[1] = getTextureId();
            for (int i = 0; i < getColors().length; i++)
                colors[i + 2] = getColors()[i].toRGB();
            return makeIdentifier(colors);
        }
    }

    @Override
    public BufferedImage makeShadeImage(TextureMap map, int width, int height, boolean useRaw) {
        final Color c0 = useRaw ? ColorUtils.fromRGB(getColors()[0].toShadeRGB()) : loadColor(getColors()[0]);
        final Color c1 = useRaw ? ColorUtils.fromRGB(getColors()[1].toShadeRGB()) : loadColor(getColors()[1]);
        final Color c2 = useRaw ? ColorUtils.fromRGB(getColors()[2].toShadeRGB()) : loadColor(getColors()[2]);
        final Color c3 = (getColors().length > 3 ? (useRaw ? ColorUtils.fromRGB(getColors()[3].toShadeRGB()) : loadColor(getColors()[3])) : Color.TRANSPARENT);

        if (isOverlay(map))
            return MAPPolyGouraud.makeGouraudImage(width, height, c0, c1, c2, c3); //c0 c1 c2 c3 -> works nicely for overlay mode. Doesn't work as well outside overlay mode.

        //TODO: Apply UVs / proper direction. [Overlay surprisingly gets this right, maybe we should take a page out of that.]
        if (isQuadFace()) {
            return MAPPolyGouraud.makeGouraudImage(width, height, c0, c2, c1, c3);
        } else {
            return MAPPolyGouraud.makeGouraudImage(width, height, c0, c1, c2, c3);
        }
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