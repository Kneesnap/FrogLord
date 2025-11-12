package net.highwayfrogs.editor.games.konami.greatquest.map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.components.propertylist.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the '_kcFogParams' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class kcFogParams implements IPropertyListCreator, IBinarySerializable {
    private kcFogMode mode;
    private final kcColor3 color = new kcColor3();
    private float start;
    private float end;
    private float density;
    private boolean rangeBased;

    @Override
    public void load(DataReader reader) {
        this.mode = kcFogMode.readFogMode(reader);
        this.color.load(reader);
        this.start = reader.readFloat();
        this.end = reader.readFloat();
        this.density = reader.readFloat();
        this.rangeBased = GreatQuestUtils.readTGQBoolean(reader);
    }

    @Override
    public void save(DataWriter writer) {
        kcFogMode.writeFogMode(writer, this.mode);
        this.color.save(writer);
        writer.writeFloat(this.start);
        writer.writeFloat(this.end);
        writer.writeFloat(this.density);
        GreatQuestUtils.writeTGQBoolean(writer, this.rangeBased);
    }

    /**
     * Creates the editor for the data here.
     * @param editorGrid the editor to setup
     */
    public void setupEditor(GUIEditorGrid editorGrid, kcEnvironment environment) {
        editorGrid.addEnumSelector("Fog Mode", this.mode, kcFogMode.values(), false, newValue -> this.mode = newValue);
        editorGrid.addColorPicker("Color", this.color.toColor().getRGB(), this.color::fromRGB);

        editorGrid.addFloatField("Start", this.start, newValue -> this.start = newValue, newFogStart -> newFogStart >= 0 && newFogStart <= this.end);
        editorGrid.addFloatField("End", this.end, newValue -> this.end = newValue, newFogEnd -> newFogEnd >= this.start && (environment == null || newFogEnd <= environment.getPerspective().getZFar()));
        editorGrid.addFloatField("Density", this.density, newValue -> this.density = newValue, null);
        editorGrid.addCheckBox("Range Based", this.rangeBased, newValue -> this.rangeBased = newValue);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        propertyList.addEnum("Fog Mode", this.mode, kcFogMode.class, newFogMode -> this.mode = newFogMode, false);
        this.color.addToPropertyList(propertyList, "Color");
        propertyList.addFloat("Start", this.start, newValue -> this.start = newValue);
        propertyList.addFloat("End", this.end, newValue -> this.end = newValue);
        propertyList.addFloat("Density", this.density, newValue -> this.density = newValue);
        propertyList.addBoolean("Range Based", this.rangeBased, newValue -> this.rangeBased = newValue);
    }
}