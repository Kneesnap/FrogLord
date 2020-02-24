package net.highwayfrogs.editor.file.map.poly.polygon;

import javafx.scene.paint.Color;
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
    public void makeTexture(BufferedImage image, Graphics2D graphics, boolean raw) {
        final Color c0 = loadColor(getVectors()[0]);
        final Color c1 = loadColor(getVectors()[1]);
        final Color c2 = loadColor(getVectors()[2]);
        final Color c3 = (getVectors().length > 3 ? loadColor(getVectors()[3]) : c2);

        float tx, ty;
        for (int x = 0; x < image.getWidth(); x++) {
            tx = (image.getWidth() == 1) ? .5F : (float) x / (float) (image.getWidth() - 1);
            for (int y = 0; y < image.getHeight(); y++) {
                ty = (image.getHeight() == 1) ? .5F : (float) y / (float) (image.getHeight() - 1);
                final Color fxColor = Utils.calculateBilinearInterpolatedColour(c0, c2, c1, c3, tx, ty);
                graphics.setColor(Utils.toAWTColor(fxColor));
                graphics.fillRect(x, y, 1, 1);
            }
        }
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

    public static Color loadColor(PSXColorVector color) {
        //TODO: 1. Can we make overlays more accurate?
        //TODO: 2. Is there some other non-overlay system which we could use to do this.
        //TODO: 3 How realistic is it to create a texture for each shading. Attempt to use a mix of things, where all sprites above a certain area get shaded by the shitty shader, but everything else gets HQ shading.
        return Utils.fromRGB(color.toRGB(), (1D - ((color.getRed() + color.getGreen() + color.getBlue()) / 127D / 3D)));
    }
}
