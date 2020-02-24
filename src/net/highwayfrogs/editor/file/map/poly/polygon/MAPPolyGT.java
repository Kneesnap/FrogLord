package net.highwayfrogs.editor.file.map.poly.polygon;

import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * Created by Kneesnap on 2/21/2020.
 */
public class MAPPolyGT extends MAPPolyTexture implements VertexColor, ColoredPoly {
    public MAPPolyGT(MAPPolygonType type, int verticeCount, int colorCount) {
        super(type, verticeCount, colorCount);
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        final javafx.scene.paint.Color c0 = Utils.fromRGB(getVectors()[0].toRGB());
        final javafx.scene.paint.Color c1 = Utils.fromRGB(getVectors()[1].toRGB());
        final javafx.scene.paint.Color c2 = Utils.fromRGB(getVectors()[2].toRGB());
        final javafx.scene.paint.Color c3 = (getVectors().length > 3 ? Utils.fromRGB(getVectors()[3].toRGB()) : c2);

        float tx, ty;
        for (int x = 0; x < image.getWidth(); x++) { //TODO: Doesn't give a great preview. Bad color approximation, washes color out in many cases.
            tx = (image.getWidth() == 1) ? .5F : (float) x / (float) (image.getWidth() - 1);
            for (int y = 0; y < image.getHeight(); y++) {
                ty = (image.getHeight() == 1) ? .5F : (float) y / (float) (image.getHeight() - 1);
                graphics.setColor(Utils.toAWTColor(Utils.calculateBilinearInterpolatedColour(c0, c2, c1, c3, tx, ty)));
                graphics.fillRect(x, y, x + 1, y + 1);
            }
        }

        // Fixes the alpha, makes sure the texture is evenly distributed.
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                image.setRGB(x, y, (image.getRGB(x, y) & 0xFFFFFF) | (MAPFile.VERTEX_SHADING_APPROXIMATION_ALPHA << 24));
    }

    @Override
    public void onDrawMap(MapMesh mesh) {
        mesh.renderOverPolygon(this, getTreeNode(mesh.getTextureMap()));
    }

    @Override
    public PSXColorVector[] getColors() {
        return getVectors();
    }

    @Override
    public BigInteger makeColorIdentifier() {
        int[] colors = new int[getColors().length];
        for (int i = 0; i < getColors().length; i++)
            colors[i] = getColors()[i].toRGB();
        return makeColorIdentifier("", colors);
    }
}
