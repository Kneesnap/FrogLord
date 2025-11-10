package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents a RenderWare vector. Implemented from src/plcore/batypes.h
 * Created by Kneesnap on 8/26/2024.
 */
@Getter @Setter
public class RwV2d extends RwStruct {
    private float x;
    private float y;

    public static final int SIZE_IN_BYTES = 2 * Constants.FLOAT_SIZE;

    public RwV2d(GameInstance instance) {
        super(instance, RwStructType.VECTOR2);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.x = reader.readFloat();
        this.y = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
    }

    @Override
    public String toString() {
        return "RwV2d{x=" + this.x + ",y=" + this.y + "}";
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("X", this.x);
        propertyList.add("Y", this.y);
    }
}