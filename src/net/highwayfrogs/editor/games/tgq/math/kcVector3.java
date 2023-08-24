package net.highwayfrogs.editor.games.tgq.math;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.IInfoWriter;

/**
 * Represents the 'kcVector3' struct as defined in kcMath3D.h.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class kcVector3 extends GameObject implements IInfoWriter {
    private float x;
    private float y;
    private float z;

    @Override
    public void load(DataReader reader) {
        this.x = reader.readFloat();
        this.y = reader.readFloat();
        this.z = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeFloat(this.x);
        writer.writeFloat(this.y);
        writer.writeFloat(this.z);
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
                .append(this.z);
    }
}
