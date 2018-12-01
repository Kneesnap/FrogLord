package net.highwayfrogs.editor.file.standard.psx.prims.polygon;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
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

    private static final double SIZE_MULT = .5D;
    private static final int SIZE = MAPFile.VERTEX_COLOR_IMAGE_SIZE / 2;
    private static final int[][] POSITION = {
            {0, 0},
            {0, 1},
            {1, 0},
            {1, 1}
    };

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
        for (int i = 0; i < colors.length; i++) {
            graphics.setColor(colors[i].toColor());
            int[] pos = POSITION[i];
            graphics.fillRect(pos[0] * SIZE, pos[1] * SIZE, SIZE, SIZE);
        }
    }
}
