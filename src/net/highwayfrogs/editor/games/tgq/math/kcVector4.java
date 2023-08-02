package net.highwayfrogs.editor.games.tgq.math;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the 'kcVector4' struct as defined in kcMath3D.h.
 * Created by Kneesnap on 7/12/2023.
 */
@Getter
@Setter
public class kcVector4 extends GameObject {
    private float x;
    private float y;
    private float z;
    private float w;

    @Override
    public void load(DataReader reader) {
        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();
        this.w = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);
        writer.writeFloat(this.w);
    }

    /**
     * Writes vector information to the builder.
     * @param builder The builder to write information to.
     */
    public void writeInfo(StringBuilder builder) {
        builder.append(this.x)
                .append(", ")
                .append(this.y)
                .append(", ")
                .append(this.z)
                .append(", ")
                .append(this.w);
    }
}
