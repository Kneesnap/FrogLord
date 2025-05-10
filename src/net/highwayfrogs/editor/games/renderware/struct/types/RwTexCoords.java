package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Represents a texture coordinate pair, as defined in batypes.h
 * Created by Kneesnap on 8/17/2024.
 */
@Getter
public class RwTexCoords extends RwStruct {
    private float u;
    private float v;

    public RwTexCoords(GameInstance instance) {
        super(instance, RwStructType.TEXCOORDS);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.u = reader.readFloat();
        this.v = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer, int version) {
        writer.writeFloat(this.u);
        writer.writeFloat(this.v);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("U", this.u);
        propertyList.add("V", this.v);
        return propertyList;
    }

    @Override
    public String toString() {
        return "RwTextureCoords{u=" + this.u + ",v=" + this.v + "}";
    }
}