package net.highwayfrogs.editor.file.map.animation;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the MAP_ANIM struct.
 * Created by Kneesnap on 8/27/2018.
 */
public class MAPAnimation extends GameObject {
    private short uChange; // Delta U (Each frame)
    private short vChange;
    private short duration; // Frames before resetting.

    private short celCount;

    private short flags;
    private short polygonCount;


    @Override
    public void load(DataReader reader) {
        this.uChange = reader.readUnsignedByteAsShort();
        this.vChange = reader.readUnsignedByteAsShort();
        this.duration = reader.readShort();
        reader.readBytes(4); // Four run-time bytes.

        // Texture information.
        this.celCount = reader.readShort();
        reader.readShort(); // Run-time short.
        int celListPointer = reader.readInt(); //TODO: Cel Animation :/
        short celPeriod = reader.readShort(); // Frames before resetting.
        reader.readShort(); // Run-time variable.

        this.flags = reader.readShort(); //TODO: Port flags.
        this.polygonCount = reader.readShort();
        reader.readInt(); // Texture pointer. Generated at run-time.

        int mapUvInfoPointer = reader.readInt(); //TODO. (Array Size = polygonCount)
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.uChange);
        writer.writeUnsignedByte(this.vChange);
        writer.writeShort(this.duration);
        writer.writeNull(4); // Run-time.
        writer.writeShort(this.celCount);
        writer.writeNull(Constants.SHORT_SIZE); // Run-time.
        //TODO: Cel-List pointer.
        //TODO: celPeriod
        writer.writeShort((short) 0); // Runtime.
        writer.writeShort(this.flags);
        writer.writeShort(this.polygonCount);
        writer.writeInt(0); // Run-time.
        //TODO: MAPUV Pointer.
    }
}
