package net.highwayfrogs.editor.games.konami.greatquest.math;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.utils.IBinarySerializable;

/**
 * Represents the 'kcSphere' struct and 'kcCSphere' class.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcSphere implements IInfoWriter, IMultiLineInfoWriter, IBinarySerializable {
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