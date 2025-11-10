package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the RpVertexNormal struct in basector.h
 * Created by Kneesnap on 8/17/2024.
 */
@Setter
@Getter
public class RpVertexNormal extends RwStruct {
    private byte x;
    private byte y;
    private byte z;
    private byte padding;

    public static final int SIZE_IN_BYTES = 4;

    public RpVertexNormal(GameInstance instance) {
        super(instance, RwStructType.VERTEX_NORMAL);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.x = reader.readByte();
        this.y = reader.readByte();
        this.z = reader.readByte();
        this.padding = reader.readByte();
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeByte(this.x);
        writer.writeByte(this.y);
        writer.writeByte(this.z);
        writer.writeByte(this.padding); // Padding.
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("X", this.x);
        propertyList.add("Y", this.y);
        propertyList.add("Z", this.z);
    }

    @Override
    public String toString() {
        return "RpVertexNormal{x=" + this.x + ",y=" + this.y + ",z=" + this.z + "}";
    }
}