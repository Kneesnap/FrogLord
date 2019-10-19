package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Represents polygons with gouraud shading.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class MAPPolyGouraud extends MAPPolygon implements VertexColor {
    private PSXColorVector[] colors;
    private transient TextureTreeNode textureNode;

    // NOTE: Changing MAPFile.VERTEX_COLOR_IMAGE_SIZE to a higher value will improve the shading quality, but may
    //       also break texture related stuff elsewhere (I haven't done much in the way of testing).
    //       Try changing the default value from 8 to 32 for example.
    //       I don't like the fact we are relying on texture generation and resolution to shade the polygons. We should
    //       really just be setting vertex color values and letting the hardware do the shading work. This just seems
    //       very, very wrong - but I don't know how we can get around it right now due to the crappy limitations of
    //       JavaFx. It's a problem for sure.
    private static final int FULL_SIZE = MAPFile.VERTEX_COLOR_IMAGE_SIZE;

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
    public TextureTreeNode getNode(TextureMap map) {
        return textureNode;
    }

    private Color getFromColor(PSXColorVector psxColVec) {
        float r = psxColVec.getRed() / 255.0f;
        float g = psxColVec.getGreen() / 255.0f;
        float b = psxColVec.getBlue() / 255.0f;
        float a = psxColVec.getCd() / 255.0f;

        // Not really sure how out-of-range color values should be handled... hence grabbing absolute values for now.
        if (r < 0.0f) { r = Math.abs(r); }
        if (g < 0.0f) { g = Math.abs(g); }
        if (b < 0.0f) { b = Math.abs(b); }
        if (a < 0.0f) { a = Math.abs(a); }

        return new Color(r, g, b, a);
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        final Color c0 = this.getFromColor(colors[0]);
        final Color c1 = this.getFromColor(colors[1]);
        final Color c2 = this.getFromColor(colors[2]);
        final Color c3 = (colors.length > 3) ? (this.getFromColor(colors[3])) : (c2);

        float tx, ty;

        for (int x = 0; x < FULL_SIZE; x++) {
            tx = (x == FULL_SIZE - 1) ? (1.0f) : (float)x / (float)FULL_SIZE;
            for (int y = 0; y < FULL_SIZE; y++) {
                ty = (y == FULL_SIZE - 1) ? (1.0f) : (float)y / (float)FULL_SIZE;
                final Color newColor = Utils.calculateBilinearInterpolatedColour(c0, c2, c1, c3, tx, ty);
                graphics.setColor(Utils.toAWTColor(newColor));
                graphics.fillRect(x, y, x + 1, y + 1);
            }
        }
    }
}
