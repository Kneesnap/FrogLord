package net.highwayfrogs.editor.file.map.group;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXGPUPrimitive;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXPrimitiveType;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the "MAP_GROUP" struct.
 * TODO: Are all Polygons owned by a map group? All polygons only have one map group, right? If the answer to both of these is true, MAP_GROUP should have a list of polygons, not MAPFile.
 * Created by Kneesnap on 8/29/2018.
 */
public class MAPGroup extends GameObject {
    private Map<PSXPrimitiveType, Short> polygonCountMap = new HashMap<>();
    private Map<PSXPrimitiveType, Integer> polygonPointerMap = new HashMap<>();
    private int entityRootPointer; // Points to the linked list of entities which project over this map group. TODO.

    private transient MAPFile parent;
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
            polygonCountMap.put(type, reader.readUnsignedByteAsShort());

        reader.readBytes(3);
        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            polygonPointerMap.put(type, reader.readInt());

        reader.readInt(5 * Constants.POINTER_SIZE); // 5 run-time pointers.
        this.entityRootPointer = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            writer.writeUnsignedByte(polygonCountMap.get(type));

        writer.writeNull(3);
        this.pointerLocation = writer.getIndex();
        writer.writeNull(Constants.POINTER_SIZE); // Save this pointer later, after polygons are saved.
        writer.writeNull(5 * Constants.POINTER_SIZE);
        writer.writeInt(this.entityRootPointer = 0); //TODO: Automatically calculate this later.
    }

    /**
     * Write polygon pointers. Must be done after polygons are saved.
     */
    public void writePolygonPointers(DataWriter writer) {
        Utils.verify(this.pointerLocation > 0 && parent.getSavePolygonPointerMap().size() > 0, "Cannot save polygon pointers before polygons are written.");
        writer.jumpTemp(this.pointerLocation);

        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES) {
            int pointer = polygonPointerMap.get(type);
            if (pointer > 0 && polygonCountMap.get(type) > 0) {
                PSXGPUPrimitive poly = parent.getLoadPointerPolygonMap().get(pointer);
                Utils.verify(poly != null, "Failed to update group polygon address for %d (%s).", pointer, Integer.toHexString(pointer));

                Integer newPointer = parent.getSavePolygonPointerMap().get(poly);
                Utils.verify(newPointer != null, "A starting MAP_GROUP polygon was not written. [%d]", pointer);
                pointer = newPointer;
            }

            writer.writeInt(pointer);
        }

        this.pointerLocation = 0;
        writer.jumpReturn();
    }
}
