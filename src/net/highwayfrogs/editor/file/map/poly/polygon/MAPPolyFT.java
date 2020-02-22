package net.highwayfrogs.editor.file.map.poly.polygon;

import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.VertexColor;
import net.highwayfrogs.editor.file.standard.psx.PSXColorVector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * Created by Kneesnap on 2/21/2020.
 */
public class MAPPolyFT extends MAPPolyTexture implements VertexColor, ColoredPoly {
    public MAPPolyFT(MAPPolygonType type, int verticeCount) {
        super(type, verticeCount, 1);
    }

    @Override
    public PSXColorVector[] getColors() {
        return getVectors();
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics) {
        graphics.setColor(getColors()[0].toColor(MAPFile.VERTEX_SHADING_APPROXIMATION_ALPHA));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    @Override
    public void onDrawMap(MapMesh mesh) {
        mesh.renderOverPolygon(this, getTreeNode(mesh.getTextureMap()));
    }

    @Override
    public BigInteger makeColorIdentifier() {
        int[] colors = new int[getColors().length];
        for (int i = 0; i < getColors().length; i++)
            colors[i] = getColors()[i].toRGB();
        return makeColorIdentifier("", colors);
    }
}
