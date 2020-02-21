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
    private APILightType apiType = APILightType.AMBIENT;
    private int color; // BbGgRr
    private SVector direction = new SVector();

    public Light() {}

    public Light(APILightType apiLightType) {
        this.apiType = apiLightType;
    }

    @Override
    public void load(DataReader reader) {
        short lightType = reader.readUnsignedByteAsShort();
        if (lightType != 1)
            throw new RuntimeException("Light type was not STATIC, instead it was " + lightType + ".");

        reader.skipByte(); // Unused 'priority'.
        reader.skipShort(); // Unused 'parentId'.
        this.apiType = APILightType.getType(reader.readUnsignedByteAsShort());
        reader.skipBytes(3); // Padding
        this.color = reader.readInt();
        SVector.readWithPadding(reader); // Unused position.
        this.direction = SVector.readWithPadding(reader);
        reader.skipShort(); // Unused 'attribute0'.
        reader.skipShort(); // Unused 'attribute1'.
        reader.skipPointer(); // Frame pointer. Only used at run-time.
        reader.skipPointer(); // Object pointer. Only used at run-time.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedByte((short) 1); // Light type. Always STATIC.
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

        if (getApiType() == APILightType.POINT || getApiType() == APILightType.PARALLEL)
            editor.addFloatVector(getApiType() == APILightType.POINT ? "Position:" : "Direction:", this.direction,
                    lightManager::updateEntityLighting, lightManager.getController());
    }

    /**
     * It appears UNKNOWN API light-types are pointless.
     * @return isWorthKeeping
     */
    public boolean isWorthKeeping() {
        return getApiType() != APILightType.UNKNOWN;
    }
}
