package net.highwayfrogs.editor.games.tgq.map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the '_kcPerspective' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class kcPerspective extends GameObject {
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
     * Writes information about this perspective.
     * @param builder The builder to write the information to.
     * @param padding The padding to apply to new lines.
     */
    public void writeInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("FOV Vert: ").append(this.fovVert).append(Constants.NEWLINE);
        builder.append(padding).append("Aspect: ").append(this.aspect).append(Constants.NEWLINE);
        builder.append(padding).append("zNear: ").append(this.zNear).append(Constants.NEWLINE);
        builder.append(padding).append("zFar: ").append(this.zFar).append(Constants.NEWLINE);
    }
}
