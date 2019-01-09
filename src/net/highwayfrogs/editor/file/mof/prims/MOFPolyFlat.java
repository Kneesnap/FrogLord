package net.highwayfrogs.editor.file.mof.prims;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.map.view.VertexColor;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Represents a flat MOF polygon.
 * Created by Kneesnap on 1/1/2019.
 */
@Getter
public class MOFPolyFlat extends MOFPolygon implements VertexColor {
    @Setter private transient TextureEntry textureEntry;

    public MOFPolyFlat(MOFPrimType type, int verticeCount) {
        super(type, verticeCount, 1, 0);
    }

    @Override
    public TextureEntry getEntry(TextureMap map) {
        return getTextureEntry();
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        graphics.setColor(getColor().toColor());
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }
}
