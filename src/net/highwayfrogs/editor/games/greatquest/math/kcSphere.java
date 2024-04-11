package net.highwayfrogs.editor.games.greatquest.math;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.IInfoWriter;
import net.highwayfrogs.editor.games.greatquest.IInfoWriter.IMultiLineInfoWriter;

/**
 * Represents the 'kcSphere' struct and 'kcCSphere' class.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcSphere extends GameObject implements IInfoWriter, IMultiLineInfoWriter {
    private final kcVector3 position = new kcVector3();
    private float radius;

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
    public void writeInfo(StringBuilder builder) {
        builder.append("Position: ");
        this.position.writeInfo(builder);
        builder.append(", Radius: ").append(this.radius);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Position: ");
        this.position.writeInfo(builder);
        builder.append(Constants.NEWLINE);

        builder.append(padding).append("Radius: ").append(this.radius).append(Constants.NEWLINE);
    }
}