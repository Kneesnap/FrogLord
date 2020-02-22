package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * Represents polygons with gouraud shading.
 * Created by Kneesnap on 8/25/2018.
 */
@Getter
@Setter
public class MAPPolyGouraud extends MAPPolygon implements VertexColor, ColoredPoly {
    private PSXColorVector[] colors;

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
        return getTreeNode(map);
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        final Color c0 = Utils.fromRGB(this.colors[0].toRGB());
        final Color c1 = Utils.fromRGB(this.colors[1].toRGB());
        final Color c2 = Utils.fromRGB(this.colors[2].toRGB());
        final Color c3 = (this.colors.length > 3 ? Utils.fromRGB(this.colors[3].toRGB()) : c2);

        float tx, ty;

        for (int x = 0; x < image.getWidth(); x++) {
            tx = (x == image.getWidth() - 1) ? 1F : (float) x / (float) image.getWidth();
            for (int y = 0; y < image.getHeight(); y++) {
                ty = (y == image.getHeight() - 1) ? 1F : (float) y / (float) image.getHeight();
                final Color newColor = Utils.calculateBilinearInterpolatedColour(c0, c2, c1, c3, tx, ty);
                graphics.setColor(Utils.toAWTColor(newColor));
                graphics.fillRect(x, y, x + 1, y + 1);
            }
        }
    }

    @Override
    public BigInteger makeColorIdentifier() {
        int[] colors = new int[getColors().length];
        for (int i = 0; i < getColors().length; i++)
            colors[i] = getColors()[i].toRGB();
        return makeColorIdentifier("", colors);
    }
}
