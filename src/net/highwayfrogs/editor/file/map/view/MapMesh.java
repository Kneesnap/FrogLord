package net.highwayfrogs.editor.file.map.view;

import javafx.scene.shape.VertexFormat;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.standard.SVector;

import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds Map mesh information.
 * Created by Kneesnap on 11/25/2018.
 */
@Getter
public class MapMesh extends FrogMesh {
    private MAPFile map;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor ANIMATION_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor INVISIBLE_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);
    public static final CursorVertexColor GRID_COLOR = new CursorVertexColor(Color.BLUE, Color.BLACK);

    public MapMesh(MAPFile file, TextureMap texMap) {
        super(texMap, VertexFormat.POINT_TEXCOORD);
        this.map = file;
        updateData();
    }

    @Override
    public void onUpdatePolygonData() {
        AtomicInteger texId = new AtomicInteger();
        getMap().forEachPrimitive(prim -> {
            if (prim instanceof MAPPolygon)
                addPolygon(prim, texId);
        });
    }

    @Override
    public List<SVector> getVertices() {
        return getMap().getVertexes();
    }
}
