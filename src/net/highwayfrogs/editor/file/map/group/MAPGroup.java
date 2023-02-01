package net.highwayfrogs.editor.file.map.group;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitive;
import net.highwayfrogs.editor.file.map.poly.MAPPrimitiveType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the "MAP_GROUP" struct.
 * This determines what polygons render.
 * Created by Kneesnap on 8/29/2018.
 */
@Getter
public class MAPGroup extends GameObject {
    private final FroggerMapConfig mapConfig;
    private final Map<MAPPrimitiveType, List<MAPPrimitive>> polygonMap = new HashMap<>();

    private final transient Map<MAPPrimitiveType, Short> loadPolygonCountMap = new HashMap<>();
    private final transient Map<MAPPrimitiveType, Integer> loadPolygonPointerMap = new HashMap<>();
    private transient int savePointerLocation;

    public MAPGroup(FroggerMapConfig mapConfig) {
        this.mapConfig = mapConfig;
        for (MAPPrimitiveType type : MAPFile.getTypes(mapConfig))
            polygonMap.put(type, new ArrayList<>());
    }

    @Override
    public void load(DataReader reader) {
        List<MAPPrimitiveType> types = MAPFile.getTypes(this.mapConfig);
        for (MAPPrimitiveType type : types)
            loadPolygonCountMap.put(type, reader.readUnsignedByteAsShort());

        int offsetAmount = (reader.getIndex() % Constants.INTEGER_SIZE);
        if (offsetAmount != 0)
            reader.skipBytes(Constants.INTEGER_SIZE - offsetAmount);

        for (MAPPrimitiveType type : types)
            loadPolygonPointerMap.put(type, reader.readInt());

        reader.skipBytes(this.mapConfig.getGroupPaddingAmount() * Constants.POINTER_SIZE); // There's actually some data here, but it's not used by the game.
    }

    @Override
    public void save(DataWriter writer) {
        List<MAPPrimitiveType> types = MAPFile.getTypes(this.mapConfig);
        for (MAPPrimitiveType type : types)
            writer.writeUnsignedByte((short) this.polygonMap.get(type).size());

        int offsetAmount = (writer.getIndex() % Constants.INTEGER_SIZE);
        if (offsetAmount != 0)
            writer.writeNull(Constants.INTEGER_SIZE - offsetAmount);

        this.savePointerLocation = writer.getIndex();
        writer.writeNull(types.size() * Constants.POINTER_SIZE); // Save this pointer later, after polygons are saved.
        writer.writeNull(this.mapConfig.getGroupPaddingAmount() * Constants.POINTER_SIZE);
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
        if (map.getMapConfig().isOldFormFormat())
            return; // The groups data looks correct, but it doesn't seem to align with the expected data format.

        for (MAPPrimitiveType type : group.keySet()) {
            List<MAPPrimitive> from = group.get(type);
            if (from == null || from.isEmpty())
                continue;

            int count = loadPolygonCountMap.get(type);
            if (count > 0) {
                int polyPtr = loadPolygonPointerMap.get(type);
                MAPPrimitive mapPrimitive = map.getLoadPointerPolygonMap().get(polyPtr);
                int primIndex = from.indexOf(mapPrimitive);

                if (primIndex == -1) {
                    System.out.println("Failed to setup MAP_GROUP in " + MWDFile.CURRENT_FILE_NAME + " for polygon: " + type + ", " + count + ", " + polyPtr + ", " + mapPrimitive);
                    continue;
                }

                for (int i = 0; i < count; i++)
                    from.get(primIndex + i).setAllowDisplay(true);
            }
        }

        this.loadPolygonCountMap.clear();
        this.loadPolygonPointerMap.clear();
    }
}
