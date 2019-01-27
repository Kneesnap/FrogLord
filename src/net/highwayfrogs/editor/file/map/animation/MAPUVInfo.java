package net.highwayfrogs.editor.file.map.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.editor.MAPController;

/**
 * Represents the MAP_UV_INFO struct.
 * Created by Kneesnap on 8/28/2018.
 */
@Getter
public class MAPUVInfo extends GameObject {
    private MAPPolygon polygon;
    private transient MAPFile map;

    private static final int UV_COUNT = 4;
    private static final int TOTAL_UV_BLOCK_SIZE = UV_COUNT * ByteUV.BYTE_SIZE;
    public static final int BYTE_SIZE = Constants.POINTER_SIZE + TOTAL_UV_BLOCK_SIZE;

    public MAPUVInfo(MAPFile map) {
        this.map = map;
    }

    public MAPUVInfo(MAPFile map, MAPPolygon selected) {
        this(map);
        this.polygon = selected;
    }

    @Override
    public void load(DataReader reader) {
        int polyPointer = reader.readInt();

        PSXGPUPrimitive foundPrim = getMap().getLoadPointerPolygonMap().get(polyPointer);
        Utils.verify(foundPrim != null, "No polygon was loaded from %s.", Integer.toHexString(polyPointer));
        Utils.verify(foundPrim instanceof MAPPolygon, "Found prim was not a MAPPolygon.");

        this.polygon = (MAPPolygon) foundPrim;
        reader.readBytes(TOTAL_UV_BLOCK_SIZE); // There are a bunch of uvs, but they're run-time only.
    }

    @Override
    public void save(DataWriter writer) {
        Utils.verify(getMap().getSavePolygonPointerMap().containsKey(getPolygon()), "The relevant polygon was not saved!");
        writer.writeInt(getMap().getSavePolygonPointerMap().get(getPolygon()));
        writer.writeNull(TOTAL_UV_BLOCK_SIZE); // Run-time UV space.
    }

    /**
     * Draw over the existing polygon.
     */
    public void writeOver(MAPController controller, CursorVertexColor color) {
        controller.renderOverPolygon(getPolygon(), color);
    }
}
