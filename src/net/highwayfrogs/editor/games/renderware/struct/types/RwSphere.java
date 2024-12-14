package net.highwayfrogs.editor.games.renderware.struct.types;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.struct.RwStruct;
import net.highwayfrogs.editor.games.renderware.struct.RwStructType;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;

/**
 * Represents the RwSphere struct as defined in batypes.h
 * Created by Kneesnap on 8/25/2024.
 */
@Getter
public class RwSphere extends RwStruct {
    private final RwV3d center;
    @Setter private float radius = 1F;

    public RwSphere(GameInstance instance) {
        super(instance, RwStructType.SPHERE);
        this.center = new RwV3d(instance);
    }

    @Override
    public void load(DataReader reader, int version, int byteLength) {
        this.center.load(reader, version, byteLength);
        this.radius = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer, int version) {
        this.center.save(writer, version);
        writer.writeFloat(this.radius);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Center", this.center);
        propertyList.add("Radius", this.radius);
        return propertyList;
    }

    @Override
    public String toString() {
        return "RwSphere{center=" + this.center + ",radius=" + this.radius + "}";
    }
}