package net.highwayfrogs.editor.file.map.poly.polygon;

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
public class MAPPolyFT extends MAPPolyTexture implements VertexColor, ColoredPoly {
    public MAPPolyFT(MAPPolygonType type, int verticeCount) {
        super(type, verticeCount, 1);
    }

    @Override
    public PSXColorVector[] getColors() {
        return getVectors();
    }

    @Override
    public void makeTexture(BufferedImage image, Graphics2D graphics, boolean useRaw) {
        graphics.setColor(Utils.toAWTColor(useRaw ? Utils.fromRGB(getColors()[0].toRGB()) : MAPPolyGT.loadColor(getColors()[0])));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    @Override
    public void onDrawMap(MapMesh mesh) {
        mesh.renderOverPolygon(this, getTreeNode(mesh.getTextureMap()));
    }

    @Override
    public BigInteger makeColorIdentifier() {
        return makeColorIdentifier("", getColors()[0].toRGB());
    }
}
