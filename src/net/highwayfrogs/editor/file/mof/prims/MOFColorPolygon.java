package net.highwayfrogs.editor.file.mof.prims;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.vlo.GameImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * Represents a vertex color mof polygon.
 * Created by Kneesnap on 2/13/2019.
 */
@Getter
public class MOFColorPolygon extends MOFPolygon {
    public MOFColorPolygon(MOFPart parent, MOFPrimType type, int verticeCount, int normalCount, int enCount) {
        super(parent, type, verticeCount, normalCount, enCount);
    }

    @Override
    public BufferedImage makeTexture(TextureMap map) {
        BufferedImage shadeImage = new BufferedImage(CursorVertexColor.VERTEX_COLOR_IMAGE_SIZE, CursorVertexColor.VERTEX_COLOR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = shadeImage.createGraphics();

        graphics.setColor(getColor().toColor());
        graphics.fillRect(0, 0, shadeImage.getWidth(), shadeImage.getHeight());

        graphics.dispose();
        return shadeImage;
    }

    @Override
    public boolean isOverlay(TextureMap map) {
        return true;
    }

    @Override
    public BigInteger makeIdentifier(TextureMap map) {
        return makeIdentifier(0xF1A7C010, getColor().toRGB());
    }

    @Override
    public GameImage getGameImage(TextureMap map) {
        return null;
    }
}