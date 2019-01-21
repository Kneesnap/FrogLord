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
    private LightType type;
    private APILightType apiType;
    private int color; // BbGgRr
    private SVector direction;

    @Override
    public void load(DataReader reader) {
        this.type = LightType.values()[reader.readUnsignedByteAsShort()];
        reader.readUnsignedByteAsShort(); // Unused 'priority'.
        reader.readUnsignedShortAsInt(); // Unused 'parentId'.
        this.apiType = APILightType.getType(reader.readUnsignedByteAsShort());
        reader.readBytes(3); // Padding
        this.color = reader.readInt();
        SVector.readWithPadding(reader); // Unused position.
        this.direction = SVector.readWithPadding(reader);
        reader.readUnsignedShortAsInt(); // Unused 'attribute0'.
        reader.readUnsignedShortAsInt(); // Unused 'attribute1'.
        reader.readInt(); // Frame pointer. Only used at run-time.
        reader.readInt(); // Object pointer. Only used at run-time.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) this.type.ordinal());
        writer.writeUnsignedByte((short) 0); // Unused 'priority'.
        writer.writeUnsignedShort(0); // Unused 'parentId'.
        writer.writeUnsignedByte((short) this.apiType.getFlag());
        writer.writeNull(3);
        writer.writeInt(this.color);
        SVector.EMPTY.saveWithPadding(writer); // Unused 'position'.
        this.direction.saveWithPadding(writer);
        writer.writeUnsignedShort((short) 0); // Unused 'attribute0'.
        writer.writeUnsignedShort((short) 0); // Unused 'attribute1'.
        writer.writeNullPointer();
        writer.writeNullPointer();
    }
}
