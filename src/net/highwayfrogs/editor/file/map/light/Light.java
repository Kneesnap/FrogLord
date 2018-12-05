package net.highwayfrogs.editor.file.map.light;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Holds lighting data, or the "LIGHT" struct in mapdisp.H
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
public class Light extends GameObject {
    private short type;
    private short priority;
    private int parentId;
    private short apiType;
    private int color; // BbGgRr
    private SVector position;
    private SVector direction;
    private int attribute0;
    private int attribute1;

    public static final int TYPE_DUMMY = 0;
    public static final int TYPE_STATIC = 1;
    public static final int TYPE_ENTITY = 2;

    public static final int API_TYPE_AMBIENT = 1;
    public static final int API_TYPE_PARALLEL = 2;
    public static final int API_TYPE_POINT = 4;

    @Override
    public void load(DataReader reader) {
        this.type = reader.readUnsignedByteAsShort();
        this.priority = reader.readUnsignedByteAsShort();
        this.parentId = reader.readUnsignedShortAsInt();
        this.apiType = reader.readUnsignedByteAsShort();
        reader.readBytes(3); // Padding
        this.color = reader.readInt();
        this.position = SVector.readWithPadding(reader);
        this.direction = SVector.readWithPadding(reader);
        this.attribute0 = reader.readUnsignedShortAsInt();
        this.attribute1 = reader.readUnsignedShortAsInt();
        reader.readInt(); // Frame pointer. Only used at run-time.
        reader.readInt(); // Object pointer. Only used at run-time.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte(this.type);
        writer.writeUnsignedByte(this.priority);
        writer.writeUnsignedShort(this.parentId);
        writer.writeUnsignedByte(this.apiType);
        writer.writeNull(3);
        writer.writeInt(this.color);
        this.position.saveWithPadding(writer);
        this.direction.saveWithPadding(writer);
        writer.writeUnsignedShort(this.attribute0);
        writer.writeUnsignedShort(this.attribute1);
        writer.writeInt(0);
        writer.writeInt(0);
    }
}
