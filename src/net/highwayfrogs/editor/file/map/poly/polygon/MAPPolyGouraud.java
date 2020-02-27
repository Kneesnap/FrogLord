package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
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
public class MAPPolyGouraud extends MAPPolygon {
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
    public BufferedImage makeTexture(TextureMap map) {
        final Color c0 = Utils.fromRGB(this.colors[0].toRGB());
        final Color c1 = Utils.fromRGB(this.colors[1].toRGB());
        final Color c2 = Utils.fromRGB(this.colors[2].toRGB());
        final Color c3 = (this.colors.length > 3 ? Utils.fromRGB(this.colors[3].toRGB()) : c2);
        return makeGouraudImage(MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, c0, c1, c2, c3);
    }

    @Override
    public boolean isOverlay(TextureMap map) {
        return true;
    }

    @Override
    public BigInteger makeIdentifier(TextureMap map) {
        int[] colors = new int[getColors().length + 1];
        colors[0] = 0xF0ADFACE;
        for (int i = 0; i < getColors().length; i++)
            colors[i] = getColors()[i].toRGB();
        return makeIdentifier(colors);
    }

    @Override
    public GameImage getGameImage(TextureMap map) {
        return null;
    }

    /**
     * Creates a new gouraud shaded texture.
     * @param width  The width of the texture.
     * @param height The height of the texture.
     * @param c0     Top left color.
     * @param c1     Bottom left color.
     * @param c2     Top right color.
     * @param c3     Bottom right color.
     * @return gouraudImage
     */
    public static BufferedImage makeGouraudImage(int width, int height, Color c0, Color c1, Color c2, Color c3) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.createGraphics();

        float tx, ty;
        for (int x = 0; x < image.getWidth(); x++) {
            tx = (image.getWidth() == 1) ? .5F : (float) x / (float) (image.getWidth() - 1);
            for (int y = 0; y < image.getHeight(); y++) {
                ty = (image.getHeight() == 1) ? .5F : (float) y / (float) (image.getHeight() - 1);
                graphics.setColor(Utils.toAWTColor(Utils.calculateBilinearInterpolatedColour(c0, c2, c1, c3, tx, ty)));
                graphics.fillRect(x, y, 1, 1);
            }
        }

        graphics.dispose();
        return image;
    }
}
