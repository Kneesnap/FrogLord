package net.highwayfrogs.editor.file.map.poly.polygon;

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
public class MAPPolyGouraud extends MAPPolygon implements VertexColor {
    private PSXColorVector[] colors;
    @Setter private transient TextureEntry textureEntry;

    private static final int FULL_SIZE = MAPFile.VERTEX_COLOR_IMAGE_SIZE;
    private static final int SMALL_SIZE = FULL_SIZE / 2;
    private static final int TRIANGLE_SIZE = 3;

    private static final int[][] POSITION = {
            {0, 0},
            {0, 1},
            {1, 0},
            {1, 1}
    };

    //0-2
    //| |
    //1-3
    private static final int[][][] TRIANGLE_POSITON = {
            {{0, FULL_SIZE, 0}, {FULL_SIZE, 0, 0}},
            {{0, FULL_SIZE, 0}, {0, FULL_SIZE, FULL_SIZE}},
            {{0, FULL_SIZE, FULL_SIZE}, {0, FULL_SIZE, 0}},
            {{0, FULL_SIZE, FULL_SIZE}, {FULL_SIZE, 0, FULL_SIZE}}
    };

    public MAPPolyGouraud(MAPPolygonType type, int verticeCount) {
        super(type, verticeCount);
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

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        for (int i = 0; i < colors.length; i++) {
            graphics.setColor(colors[i].toColor());
            int[] pos = POSITION[i];
            graphics.fillRect(pos[0] * SMALL_SIZE, pos[1] * SMALL_SIZE, SMALL_SIZE, SMALL_SIZE);
        }

        // Apply Vertex Shading.
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5F));
        for (int i = 0; i < colors.length; i++) {
            graphics.setPaint(colors[i].toColor());

            int[][] points = TRIANGLE_POSITON[i];
            graphics.fillPolygon(points[0], points[1], TRIANGLE_SIZE);
        }
    }
}
