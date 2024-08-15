package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Represents a RenderWare vector. Implemented from src/plcore/batypes.h
 * Created by Kneesnap on 8/12/2024.
 */
@Getter
public class RwV3d extends RwStruct {
    private float x;
    private float y;
    private float z;

    public RwV3d(GameInstance instance) {
        super(instance, RwStructType.VECTOR3);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);
    }

    @Override
    public String toString() {
        return "RwV3d{x=" + this.x + ",y=" + this.y + ",z=" + this.z + "}";
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("X", this.x);
        propertyList.add("Y", this.y);
        propertyList.add("Z", this.z);
        return propertyList;
    }
}