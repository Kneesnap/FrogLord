package net.highwayfrogs.editor.games.konami.greatquest.map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the '_kcPerspective' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class kcPerspective implements IMultiLineInfoWriter, IBinarySerializable, IPropertyListCreator {
    private float fovVert;
    private float aspect;
    private float zNear;
    private float zFar;

    @Override
    public void load(DataReader reader) {
        this.fovVert = reader.readFloat();
        this.aspect = reader.readFloat();
        this.zNear = reader.readFloat();
        this.zFar = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeFloat(this.fovVert);
        writer.writeFloat(this.aspect);
        writer.writeFloat(this.zNear);
        writer.writeFloat(this.zFar);
    }

    /**
     * Creates the editor for the data here.
     * @param editorGrid the editor to setup
     */
    public void setupEditor(GUIEditorGrid editorGrid) {
        // I think we should add a note saying these settings are not reflected in the editor.
        // But that if you want to see them, you can play around in the camera settings view.
        editorGrid.addDoubleField("FOV (Degrees)", Math.toDegrees(this.fovVert), newValue -> this.fovVert = (float) Math.toRadians(newValue), null);
        editorGrid.addFloatField("Aspect Ratio", this.aspect, newValue -> this.aspect = newValue, null);
        editorGrid.addFloatField("zNear", this.zNear, newValue -> this.zNear = newValue, null);
        editorGrid.addFloatField("zFar", this.zFar, newValue -> this.zFar = newValue, null);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("FOV (Degrees): ").append(Math.toDegrees(this.fovVert)).append(Constants.NEWLINE);
        builder.append(padding).append("Aspect Ratio: ").append(this.aspect).append(Constants.NEWLINE);
        builder.append(padding).append("zNear: ").append(this.zNear).append(Constants.NEWLINE);
        builder.append(padding).append("zFar: ").append(this.zFar).append(Constants.NEWLINE);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.add("FOV (Degrees)", Math.toDegrees(this.fovVert));
        propertyList.add("Aspect Ratio", this.aspect);
        propertyList.add("Near Clip", this.zNear);
        propertyList.add("Far Clip", this.zFar);
    }
}