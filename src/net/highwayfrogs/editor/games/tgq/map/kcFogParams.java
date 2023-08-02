package net.highwayfrogs.editor.games.tgq.map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.TGQUtils;

/**
 * Represents the '_kcFogParams' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class kcFogParams extends GameObject {
    private kcFogMode mode;
    private kcColor3 color;
    private float start;
    private float end;
    private float density;
    private boolean rangeBased;

    @Override
    public void load(DataReader reader) {
        if (this.color == null)
            this.color = new kcColor3();

        this.mode = kcFogMode.readFogMode(reader);
        this.color.load(reader);
        this.start = reader.readFloat();
        this.end = reader.readFloat();
        this.density = reader.readFloat();
        this.rangeBased = TGQUtils.readTGQBoolean(reader);
    }

    @Override
    public void save(DataWriter writer) {
        kcFogMode.writeFogMode(writer, this.mode);
        this.color.save(writer);
        writer.writeFloat(this.start);
        writer.writeFloat(this.end);
        writer.writeFloat(this.density);
        TGQUtils.writeTGQBoolean(writer, this.rangeBased);
    }

    /**
     * Writes information about this object.
     * @param builder The builder to write the information to.
     * @param padding The padding to apply to new lines.
     */
    public void writeInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Fog Mode: ").append(this.mode).append(Constants.NEWLINE);
        if (this.color != null) {
            builder.append(padding).append("Color: ");
            this.color.writeInfo(builder);
            builder.append(Constants.NEWLINE);
        }

        builder.append(padding).append("Start: ").append(this.start).append(Constants.NEWLINE);
        builder.append(padding).append("End: ").append(this.end).append(Constants.NEWLINE);
        builder.append(padding).append("Density: ").append(this.density).append(Constants.NEWLINE);
        builder.append(padding).append("Range Based: ").append(this.rangeBased).append(Constants.NEWLINE);
    }
}
