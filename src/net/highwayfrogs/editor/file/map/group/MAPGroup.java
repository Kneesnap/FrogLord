package net.highwayfrogs.editor.file.map.group;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents the "MAP_GROUP" struct.
 * This determines what polygons render.
 * Created by Kneesnap on 8/29/2018.
 */
@Getter
public class MAPGroup extends GameObject {
    private Map<MAPPrimitiveType, List<MAPPrimitive>> polygonMap = new HashMap<>();

    private transient Map<MAPPrimitiveType, Short> loadPolygonCountMap = new HashMap<>();
    private transient Map<MAPPrimitiveType, Integer> loadPolygonPointerMap = new HashMap<>();
    private transient int savePointerLocation;

    private static final int NULL_POINTERS = 6; // 1 unused. 5 runtime pointers.

    public MAPGroup() {
        for (MAPPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            polygonMap.put(type, new LinkedList<>());
    }

    @Override
    public void load(DataReader reader) {
        for (MAPPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            loadPolygonCountMap.put(type, reader.readUnsignedByteAsShort());

        reader.readBytes(3);
        for (MAPPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            loadPolygonPointerMap.put(type, reader.readInt());

        reader.readInt(NULL_POINTERS * Constants.POINTER_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        for (MAPPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            writer.writeUnsignedByte((short) polygonMap.get(type).size());

        writer.writeNull(3);
        this.savePointerLocation = writer.getIndex();
        writer.writeNull(MAPFile.PRIMITIVE_TYPES.size() * Constants.POINTER_SIZE); // Save this pointer later, after polygons are saved.
        writer.writeNull(NULL_POINTERS * Constants.POINTER_SIZE);
    }

    /**
     * Write polygon pointers. Must be done after polygons are saved.
     */
    public void writePolygonPointers(MAPFile map, DataWriter writer) {
        Utils.verify(this.savePointerLocation > 0, "Cannot save polygon pointers before polygons are written.");
        writer.jumpTemp(this.savePointerLocation);

        for (MAPPrimitiveType type : MAPFile.PRIMITIVE_TYPES) {
            List<MAPPrimitive> polyList = polygonMap.get(type);

            int pointer = 0;
            if (!polyList.isEmpty()) {
                Integer newPointer = map.getSavePolygonPointerMap().get(polyList.get(0));
                Utils.verify(newPointer != null, "A MAP_GROUP polygon was not written.");
                pointer = newPointer;
            }

            writer.writeInt(pointer);
        }

        this.savePointerLocation = 0;
        writer.jumpReturn();
    }

    /**
     * Called after polygons are loaded. Sets up polygon data.
     */
    public void setupPolygonData(MAPFile map, Map<MAPPrimitiveType, List<MAPPrimitive>> group) {
        Utils.verify(this.loadPolygonCountMap.size() > 0, "Cannot setup polygon data twice.");

        for (MAPPrimitiveType type : MAPFile.PRIMITIVE_TYPES) {
            List<MAPPrimitive> from = group.get(type);
            int count = loadPolygonCountMap.get(type);

            if (count > 0 && from != null) {
                int index = from.indexOf(map.getLoadPointerPolygonMap().get(loadPolygonPointerMap.get(type)));
                for (int i = 0; i < count; i++)
                    from.get(index + i).setAllowDisplay(true);
            }
        }

        this.loadPolygonCountMap.clear();
        this.loadPolygonPointerMap.clear();
    }
}
