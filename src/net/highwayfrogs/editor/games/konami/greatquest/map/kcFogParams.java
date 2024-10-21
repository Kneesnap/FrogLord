package net.highwayfrogs.editor.games.konami.greatquest.map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.IBinarySerializable;

/**
 * Represents the '_kcFogParams' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class kcFogParams implements IMultiLineInfoWriter, IBinarySerializable {
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

    /**
     * Writes information about this object.
     * @param builder The builder to write the information to.
     * @param padding The padding to apply to new lines.
     */
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Fog Mode: ").append(this.mode).append(Constants.NEWLINE);
        this.color.writePrefixedInfoLine(builder, "Color", padding);

        builder.append(padding).append("Start: ").append(this.start).append(Constants.NEWLINE);
        builder.append(padding).append("End: ").append(this.end).append(Constants.NEWLINE);
        builder.append(padding).append("Density: ").append(this.density).append(Constants.NEWLINE);
        builder.append(padding).append("Range Based: ").append(this.rangeBased).append(Constants.NEWLINE);
    }
}