package net.highwayfrogs.editor.file.map.view;

import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.ColoredPoly;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Kneesnap on 2/24/2020.
 */
public class MapOverlayMesh extends MapMesh {
    private AtomicInteger texCount = new AtomicInteger(0);

    public MapOverlayMesh(MAPFile file, TextureMap texMap) {
        super(file, texMap);
    }

    @Override
    public void onUpdatePolygonData() {
        texCount = new AtomicInteger(0);

        // Apply shading to textured polygons. This is done separately from adding the polygons, because for some reason it garbles the textures if we don't separate it. (I think it's expected that polygons are added before anything else.)
        for (MAPPolygon poly : getMap().getAllPolygons())
            if (poly instanceof ColoredPoly) // Textured shaded poly.
                ((ColoredPoly) poly).onDrawMap(this);
    }

    @Override
    public void renderOverPolygon(MAPPolygon targetPoly, TextureTreeNode node) {
        if (node != null) {
            int[] verts = targetPoly.getVertices();
            if (targetPoly.isQuadFace()) {
                addRectangle(node, verts[0], verts[3], verts[1], verts[1], verts[3], verts[2]);
            } else {
                addTriangle(node, verts[2], verts[1], verts[0]);
            }
        } else {
            addPolygon(targetPoly, this.texCount);
        }
    }
}
