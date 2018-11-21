package net.highwayfrogs.editor.file.map.animation;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.ByteUV;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the MAP_UV_INFO struct.
 * Created by Kneesnap on 8/28/2018.
 */
@Getter
public class MAPUVInfo extends GameObject {
    private ByteUV[] uvs = new ByteUV[4]; // The UVs to apply to the poly address.
    private PSXGPUPrimitive polygon;
    private MAPFile map;

    private static final int UV_COUNT = 4;
    private static final int TOTAL_UV_BLOCK_SIZE = UV_COUNT * ByteUV.BYTE_SIZE;
    public static final int BYTE_SIZE = Constants.POINTER_SIZE + TOTAL_UV_BLOCK_SIZE;


    public MAPUVInfo(MAPFile map) {
        this.map = map;
    }

    @Override
    public void load(DataReader reader) {
        int polyPointer = reader.readInt();
        this.polygon = getMap().getLoadPointerPolygonMap().get(polyPointer);
        Utils.verify(this.polygon != null, "No polygon was loaded from %s.", Integer.toHexString(polyPointer));
        reader.readBytes(TOTAL_UV_BLOCK_SIZE); // There are a bunch of uvs, but they're run-time only. TODO: Confirm run-time only.
    }

    @Override
    public void save(DataWriter writer) {
        Utils.verify(getMap().getSavePolygonPointerMap().containsKey(getPolygon()), "The relevant polygon was not saved!");
        writer.writeInt(getMap().getSavePolygonPointerMap().get(getPolygon()));
        writer.writeNull(TOTAL_UV_BLOCK_SIZE); // Run-time UV space.
    }
}
