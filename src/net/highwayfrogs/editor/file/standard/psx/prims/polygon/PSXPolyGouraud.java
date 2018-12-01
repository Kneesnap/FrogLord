package net.highwayfrogs.editor.file.standard.psx.prims.polygon;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.standard.psx.prims.VertexColor;
import net.highwayfrogs.editor.file.vlo.TextureMap;
import net.highwayfrogs.editor.file.vlo.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Represents polygons with gouraud shading.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
public class PSXPolyGouraud extends PSXPolygon implements VertexColor {
    private PSXColorVector[] colors;
    @Setter private transient TextureEntry textureEntry;

    public PSXPolyGouraud(int verticeCount) {
        super(verticeCount);
        this.colors = new PSXColorVector[verticeCount];
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Read colors
        for (int i = 0; i < this.colors.length; i++) {
            PSXColorVector vector = new PSXColorVector();
            vector.load(reader);
            this.colors[i] = vector;
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (PSXColorVector vector : colors)
            vector.save(writer);
    }

    @Override
    public TextureEntry getEntry(TextureMap map) {
        return textureEntry;
    }

    @Override //TODO: Shading.
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        int i = 0;
        for (int x = 0; x < image.getWidth(); x += (image.getWidth() / 2)) {
            for (int y = 0; y < image.getHeight(); y += (image.getHeight() / 2)) {
                if (i >= colors.length)
                    continue;

                graphics.setColor(colors[i++].toColor());
                graphics.fillRect(x, y, image.getWidth() / 2, image.getHeight() / 2);
            }
        }
    }
}
