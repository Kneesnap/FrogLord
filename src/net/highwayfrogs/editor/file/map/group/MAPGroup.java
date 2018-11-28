package net.highwayfrogs.editor.file.map.group;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXPrimitiveType;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the "MAP_GROUP" struct.
 * Created by Kneesnap on 8/29/2018.
 */
@Getter
public class MAPGroup extends GameObject {
    private Map<PSXPrimitiveType, List<PSXGPUPrimitive>> polygonMap = new HashMap<>();
    private List<Short> entityList = new ArrayList<>(); // Points to the array of entity ids which project over this map group.

    private transient MAPFile parent;
    private transient Map<PSXPrimitiveType, Short> loadPolygonCountMap = new HashMap<>();
    private transient Map<PSXPrimitiveType, Integer> loadPolygonPointerMap = new HashMap<>();
    private transient int savePointerLocation;
    private transient int pointerLocation;

    public MAPGroup(MAPFile parent) {
        this.parent = parent;
    }

    public static final int BYTE_SIZE = (Constants.BYTE_SIZE * MAPFile.PRIMITIVE_TYPES.size())
            + (3 * Constants.BYTE_SIZE)
            + (Constants.INTEGER_SIZE * MAPFile.PRIMITIVE_TYPES.size())
            + (6 * Constants.INTEGER_SIZE);

    @Override
    public void load(DataReader reader) {
        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            loadPolygonCountMap.put(type, reader.readUnsignedByteAsShort());

        reader.readBytes(3);
        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            loadPolygonPointerMap.put(type, reader.readInt());

        reader.readInt(5 * Constants.POINTER_SIZE); // 5 run-time pointers.

        int entityRootPointer = reader.readInt();
        if (entityRootPointer > 0) {
            reader.jumpTemp(entityRootPointer);
            short temp;
            while ((temp = reader.readShort()) != MAPFile.MAP_ANIMATION_TEXTURE_LIST_TERMINATOR)
                getEntityList().add(temp);
            reader.jumpReturn();
        }
    }

    @Override
    public void save(DataWriter writer) {
        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            writer.writeUnsignedByte((short) polygonMap.get(type).size());

        writer.writeNull(3);
        this.pointerLocation = writer.getIndex();
        writer.writeNull(MAPFile.PRIMITIVE_TYPES.size() * Constants.POINTER_SIZE); // Save this pointer later, after polygons are saved.
        writer.writeNull(5 * Constants.POINTER_SIZE);
        this.savePointerLocation = writer.getIndex();
        writer.writeInt(0); // Indice pointer, saved later.
    }

    /**
     * Write polygon pointers. Must be done after polygons are saved.
     */
    public void writePolygonPointers(DataWriter writer) {
        Utils.verify(this.pointerLocation > 0, "Cannot save polygon pointers before polygons are written.");
        writer.jumpTemp(this.pointerLocation);

        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES) {
            List<PSXGPUPrimitive> polyList = polygonMap.get(type);

            int pointer = 0;
            if (!polyList.isEmpty()) {
                Integer newPointer = parent.getSavePolygonPointerMap().get(polyList.get(0));
                Utils.verify(newPointer != null, "A MAP_GROUP polygon was not written.");
                pointer = newPointer;
            }

            writer.writeInt(pointer);
        }

        this.pointerLocation = 0;
        writer.jumpReturn();
    }

    /**
     * Called after polygons are loaded. Sets up polygon data.
     */
    public void setupPolygonData(Map<PSXPrimitiveType, List<PSXGPUPrimitive>> group) {
        Utils.verify(this.loadPolygonCountMap.size() > 0, "Cannot setup polygon data twice.");

        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES) {
            List<PSXGPUPrimitive> from = group.get(type);
            int count = loadPolygonCountMap.get(type);
            List<PSXGPUPrimitive> loadedPolys = new ArrayList<>();

            if (count > 0 && from != null) {
                int index = from.indexOf(getParent().getLoadPointerPolygonMap().get(loadPolygonPointerMap.get(type)));
                for (int i = 0; i < count; i++)
                    loadedPolys.add(from.remove(index));
            }
            polygonMap.put(type, loadedPolys);
        }

        this.loadPolygonCountMap.clear();
        this.loadPolygonPointerMap.clear();
    }

    /**
     * Write the entity list.
     * @param writer The writer to write the data to.
     */
    public void writeEntityList(DataWriter writer) {
        parent.writeEntityList(writer, getEntityList(), this.savePointerLocation);
        this.savePointerLocation = 0;
    }
}
