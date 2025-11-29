package net.highwayfrogs.editor.games.konami.greatquest.math;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListDataEntry;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.math.Vector4f;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'kcSphere' struct and 'kcCSphere' class.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcSphere implements IBinarySerializable {
    private final kcVector3 position;
    private float radius;

    public kcSphere() {
        this(0, 0, 0, 1F);
    }

    public kcSphere(float x, float y, float z, float radius) {
        this.position = new kcVector3(x, y, z);
        this.radius = radius;
    }

    @Override
    public void load(DataReader reader) {
        this.position.load(reader);
        this.radius = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        this.position.save(writer);
        writer.writeFloat(this.radius);
    }

    @Override
    public String toString() {
        return "kcSphere{pos=" + this.position.getX() + "," + this.position.getY() + "," + this.position.getZ() + ",radius=" + this.radius + "}";
    }

    /**
     * Adds this sphere to the property list
     * @param propertyList the property list to add to
     * @param name the name to give this sphere
     * @return propertyListEntry
     */
    public PropertyListDataEntry<kcSphere> addToPropertyList(PropertyListNode propertyList, String name) {
        return propertyList.add(name, this)
                .setDataToStringConverter(sphere -> sphere.position.toParseableString() + ", " + sphere.radius)
                .setDataFromStringConverter(newText -> {
                    Vector4f newVector = new Vector4f();
                    newVector.parse(newText);
                    kcSphere sphere = new kcSphere();
                    sphere.getPosition().setXYZ(newVector.getXYZ(null));
                    sphere.setRadius(newVector.getW());
                    return sphere;
                })
                .setDataHandler((entry, newSphere) -> {
                    this.position.setXYZ(newSphere.getPosition());
                    this.radius = newSphere.getRadius();
                    //noinspection unchecked
                    ((PropertyListDataEntry<kcSphere>) entry.getEntry()).setDataObject(this);
                });
    }

    /**
     * Creates a clone of this sphere.
     */
    public kcSphere clone() {
        return new kcSphere(this.position.getX(), this.position.getY(), this.position.getZ(), this.radius);
    }
}