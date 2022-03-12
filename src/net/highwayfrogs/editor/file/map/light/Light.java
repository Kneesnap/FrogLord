package net.highwayfrogs.editor.file.map.light;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.LightManager;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Holds lighting data, or the "LIGHT" struct in mapdisp.H
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
public class Light extends GameObject {
    private short priority; // Always 131 for PARALLEL, 4 or 128 = UNKNOWN (4 is seen in SWP2.MAP), 130 = POINT, 255 = AMBIENT
    private int parentId;
    private APILightType apiType = APILightType.AMBIENT;
    private int color; // BbGgRr
    private SVector position = new SVector(); // This is probably the place in the world the light is placed in the editor.
    private SVector direction = new SVector(); // When this is AMBIENT, I think this is arbitrary. When this is parallel, it seems to be a 12bit normalized direction vector. When this is point it is unused.
    private int attribute0; // "falloff if point, umbra angle if spot."
    private int attribute1;

    public Light() {
    }

    public Light(APILightType apiLightType) {
        this.apiType = apiLightType;
    }

    @Override
    public void load(DataReader reader) {
        short lightType = reader.readUnsignedByteAsShort();
        if (lightType != 1)
            throw new RuntimeException("Light type was not STATIC, instead it was " + lightType + ".");

        this.priority = reader.readUnsignedByteAsShort();
        this.parentId = reader.readUnsignedShortAsInt();
        this.apiType = APILightType.getType(reader.readUnsignedByteAsShort());
        reader.skipBytes(3); // Padding (Always zero)
        this.color = reader.readInt();
        this.position = SVector.readWithPadding(reader);
        this.direction = SVector.readWithPadding(reader);
        this.attribute0 = reader.readUnsignedShortAsInt();
        this.attribute1 = reader.readUnsignedShortAsInt();
        reader.skipPointer(); // Frame pointer. Only has a value at run-time.
        reader.skipPointer(); // Object pointer. Only has a value at run-time.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) 1); // Light type. Always STATIC.
        writer.writeUnsignedByte(this.priority);
        writer.writeUnsignedShort(this.parentId);
        writer.writeUnsignedByte((short) this.apiType.getFlag());
        writer.writeNull(3);
        writer.writeInt(this.color);
        this.position.saveWithPadding(writer);
        this.direction.saveWithPadding(writer);
        writer.writeUnsignedShort(this.attribute0);
        writer.writeUnsignedShort(this.attribute1);
        writer.writeNullPointer();
        writer.writeNullPointer();
    }

    /**
     * Makes a lighting editor.
     * @param editor Lighting editor.
     */
    public void makeEditor(GUIEditorGrid editor, LightManager lightManager) {
        // Don't need to edit the lightType, as static is the only one that does anything.
        int rgbColor = Utils.toRGB(Utils.fromBGR(getColor()));
        editor.addColorPicker("Color:", 25, rgbColor, newColor -> {
            setColor(Utils.toBGR(Utils.fromRGB(newColor)));
            lightManager.updateEntityLighting();
        });

        editor.addShortField("Priority:", this.priority, newPriority -> this.priority = newPriority, newPriority -> newPriority >= 0 && newPriority <= 0xFF);

        editor.addIntegerField("Parent ID:", this.parentId, newId -> this.parentId = newId, newId -> newId >= 0 && newId <= 0xFFFF);

        editor.addFloatVector("Position", this.position,
                lightManager::updateEntityLighting, lightManager.getController());

        editor.addFloatVector("Direction", this.direction,
                lightManager::updateEntityLighting, lightManager.getController(), 12);

        editor.addIntegerField("Attribute 0:", this.attribute0, newValue -> this.attribute0 = newValue, newValue -> newValue >= 0 && newValue <= 0xFFFF);
        editor.addIntegerField("Attribute 1:", this.attribute1, newValue -> this.attribute1 = newValue, newValue -> newValue >= 0 && newValue <= 0xFFFF);

    }
}
