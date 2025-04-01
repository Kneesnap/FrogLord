package net.highwayfrogs.editor.file.map.poly.polygon;

import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShadingMode;
import net.highwayfrogs.editor.utils.ColorUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * Created by Kneesnap on 2/21/2020.
 */
public class MAPPolyFT extends MAPPolyTexture {
    public MAPPolyFT(MAPPolygonType type, int verticeCount) {
        super(type, verticeCount, 1);
    }

    @Override
    public BufferedImage makeTexture(TextureMap map) {
        if (map.getMode() == ShadingMode.NO_SHADING) {
            return getGameImage(map).toBufferedImage(map.getDisplaySettings());
        } else if (isOverlay(map)) {
            return makeShadeImage(map, false);
        } else {
            return MAPPolyTexture.makeFlatShadedTexture(getGameImage(map).toBufferedImage(map.getDisplaySettings()), ColorUtils.fromRGB(getColors()[0].toRGB()), false);
        }
    }

    @Override
    public BigInteger makeIdentifier(TextureMap map) {
        if (map.getMode() == ShadingMode.NO_SHADING) {
            return makeIdentifier(0x7E8BA5E, getTextureId());
        } else {
            return makeIdentifier(0xF1A77E8, getTextureId(), getColors()[0].toRGB());
        }
    }

    @Override
    public BufferedImage makeShadeImage(TextureMap map, int width, int height, boolean useRaw) {
        BufferedImage shadeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = shadeImage.createGraphics();

        graphics.setColor(useRaw ? getColors()[0].toShadeColor() : ColorUtils.toAWTColor(MAPPolyGT.loadColor(getColors()[0])));
        graphics.fillRect(0, 0, shadeImage.getWidth(), shadeImage.getHeight());

        graphics.dispose();
        return shadeImage;
    }
}