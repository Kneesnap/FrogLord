package net.highwayfrogs.editor.file.map.group;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.prims.PSXPrimitiveType;
import net.highwayfrogs.editor.file.writer.DataWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the "MAP_GROUP" struct.
 * Created by Kneesnap on 8/29/2018.
 */
public class MAPGroup extends GameObject {
    private Map<PSXPrimitiveType, Short> polygonCountMap = new HashMap<>();
    private Map<PSXPrimitiveType, Integer> polygonPointerMap = new HashMap<>();
    private int entityRootPointer; // Points to the linked list of entities which project over this map group. TODO.

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
            polygonPointerMap.put(type, reader.readInt()); // TODO

        reader.readInt(5 * Constants.INTEGER_SIZE);
        this.entityRootPointer = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            writer.writeUnsignedByte(polygonCountMap.get(type));

        writer.writeNull(3);
        for (PSXPrimitiveType type : MAPFile.PRIMITIVE_TYPES)
            writer.writeInt(polygonPointerMap.get(type));

        writer.writeNull(5 * Constants.INTEGER_SIZE);
        writer.writeInt(this.entityRootPointer);
    }
}
