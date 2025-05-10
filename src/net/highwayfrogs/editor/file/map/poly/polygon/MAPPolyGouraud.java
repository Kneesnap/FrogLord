package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXTextureShader;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.awt.image.BufferedImage;

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
    public FroggerMapPolygon convertToNewFormat(FroggerMapFile mapFile) {
        FroggerMapPolygon polygon = super.convertToNewFormat(mapFile);
        if (polygon.getColors().length != this.colors.length)
            throw new RuntimeException("Invalid number of colors! Expected " + this.colors.length + ", got " + polygon.getColors().length + "! (" + getType() + "/" + polygon.getPolygonType() + ")");

        for (int i = 0; i < this.colors.length; i++)
            polygon.getColors()[i] = this.colors[i].toCVector(null);
        return polygon;
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
        return PSXTextureShader.makeGouraudShadedImage(width, height, new CVector[] {
                CVector.makeColorFromRGB(ColorUtils.toRGB(c0)),
                CVector.makeColorFromRGB(ColorUtils.toRGB(c1)),
                CVector.makeColorFromRGB(ColorUtils.toRGB(c2)),
                CVector.makeColorFromRGB(ColorUtils.toRGB(c3))
        });
    }
}