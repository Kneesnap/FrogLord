package net.highwayfrogs.editor.file.map.view;

import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.file.standard.SVector;

import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds Map mesh information.
 * Created by Kneesnap on 11/25/2018.
 */
@Getter
public class MapMesh extends FrogMesh<MAPPolygon> {
    private MAPFile map;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.ORANGE, Color.BLACK);
    public static final CursorVertexColor ANIMATION_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor INVISIBLE_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);
    public static final CursorVertexColor GRID_COLOR = new CursorVertexColor(Color.BLUE, Color.BLACK);
    public static final CursorVertexColor REMOVE_FACE_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor GENERAL_SELECTION = new CursorVertexColor(Color.CYAN, Color.BLACK);

    public MapMesh(MAPFile file, TextureMap texMap) {
        super(texMap, VertexFormat.POINT_TEXCOORD);
        this.map = file;
        updateData();
    }

    @Override
    public void onUpdatePolygonData() {
        AtomicInteger texId = new AtomicInteger();
        for (MAPPolygon poly : getMap().getAllPolygons())
            addPolygon(poly, texId);

        // Apply shading to textured polygons. This is done separately from adding the polygons, because for some reason it garbles the textures if we don't separate it. (I think it's expected that polygons are added before anything else.)
        for (MAPPolygon poly : getMap().getAllPolygons())
            poly.onMeshSetup(this);
    }

    /**
     * Render over an existing polygon.
     * @param targetPoly The polygon to render over.
     * @param color      The color to render.
     */
    public void renderOverPolygon(MAPPolygon targetPoly, CursorVertexColor color) {
        renderOverPolygon(targetPoly, color.getTreeNode(getTextureMap()));
    }

    /**
     * Render over an existing polygon.
     * @param targetPoly The polygon to render over.
     * @param node       The node to render.
     */
    public void renderOverPolygon(MAPPolygon targetPoly, TextureTreeNode node) {
        int increment = getVertexFormat().getVertexIndexSize();
        boolean isQuad = (targetPoly.getVerticeCount() == MAPPolygon.QUAD_SIZE);

        int face = getPolyFaceMap().get(targetPoly) * getFaceElementSize();
        int v1 = getFaces().get(face);
        int v2 = getFaces().get(face + increment);
        int v3 = getFaces().get(face + (2 * increment));

        if (isQuad) {
            int v4 = getFaces().get(face + (3 * increment));
            int v5 = getFaces().get(face + (4 * increment));
            int v6 = getFaces().get(face + (5 * increment));
            addRectangle(node, v1, v2, v3, v4, v5, v6);
        } else {
            addTriangle(node, v1, v2, v3);
        }
    }

    @Override
    public List<SVector> getVertices() {
        return getMap().getVertexes();
    }
}
