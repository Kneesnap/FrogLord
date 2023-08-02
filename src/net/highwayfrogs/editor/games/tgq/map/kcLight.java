package net.highwayfrogs.editor.games.tgq.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.math.kcVector4;

/**
 * Represents the '_kcLight' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
public class kcLight extends GameObject {
    private kcLightType lightType;
    private kcColor4 diffuseColor;
    private kcColor4 ambientColor;
    private kcColor4 specularColor;
    private kcVector4 position;
    private kcVector4 direction;
    private float range;
    private float falloff;
    private float atten0;
    private float atten1;
    private float atten2;
    private float theta;
    private float phi;

    @Override
    public void load(DataReader reader) {
        this.lightType = kcLightType.readLightType(reader);

        if (this.diffuseColor == null)
            this.diffuseColor = new kcColor4();
        this.diffuseColor.load(reader);

        if (this.ambientColor == null)
            this.ambientColor = new kcColor4();
        this.ambientColor.load(reader);

        if (this.specularColor == null)
            this.specularColor = new kcColor4();
        this.specularColor.load(reader);

        if (this.position == null)
            this.position = new kcVector4();
        this.position.load(reader);

        if (this.direction == null)
            this.direction = new kcVector4();
        this.direction.load(reader);

        this.range = reader.readFloat();
        this.falloff = reader.readFloat();
        this.atten0 = reader.readFloat();
        this.atten1 = reader.readFloat();
        this.atten2 = reader.readFloat();
        this.theta = reader.readFloat();
        this.phi = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        kcLightType.writeLightType(writer, this.lightType);
        this.diffuseColor.save(writer);
        this.ambientColor.save(writer);
        this.specularColor.save(writer);
        this.position.save(writer);
        this.direction.save(writer);
        writer.writeFloat(this.range);
        writer.writeFloat(this.falloff);
        writer.writeFloat(this.atten0);
        writer.writeFloat(this.atten1);
        writer.writeFloat(this.atten2);
        writer.writeFloat(this.theta);
        writer.writeFloat(this.phi);
    }

    /**
     * Writes information about this environment.
     * @param builder The builder to write the information to.
     * @param padding The padding to apply to new lines.
     */
    public void writeInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Light Type: ").append(this.lightType).append(Constants.NEWLINE);
        if (this.diffuseColor != null) {
            builder.append(padding).append("Diffuse Color: ");
            this.diffuseColor.writeInfo(builder);
            builder.append(Constants.NEWLINE);
        }

        if (this.ambientColor != null) {
            builder.append(padding).append("Ambient Color: ");
            this.ambientColor.writeInfo(builder);
            builder.append(Constants.NEWLINE);
        }

        if (this.specularColor != null) {
            builder.append(padding).append("Specular Color: ");
            this.specularColor.writeInfo(builder);
            builder.append(Constants.NEWLINE);
        }

        if (this.position != null) {
            builder.append(padding).append("Position: ");
            this.position.writeInfo(builder);
            builder.append(Constants.NEWLINE);
        }

        if (this.direction != null) {
            builder.append(padding).append("Direction: ");
            this.direction.writeInfo(builder);
            builder.append(Constants.NEWLINE);
        }

        builder.append(padding).append("Range: ").append(this.range).append(Constants.NEWLINE);
        builder.append(padding).append("Falloff: ").append(this.falloff).append(Constants.NEWLINE);
        builder.append(padding).append("Atten0: ").append(this.atten0).append(Constants.NEWLINE);
        builder.append(padding).append("Atten1: ").append(this.atten1).append(Constants.NEWLINE);
        builder.append(padding).append("Atten2: ").append(this.atten2).append(Constants.NEWLINE);
        builder.append(padding).append("Theta: ").append(this.theta).append(Constants.NEWLINE);
        builder.append(padding).append("Phi: ").append(this.phi).append(Constants.NEWLINE);
        builder.append(padding).append("Range: ").append(this.range).append(Constants.NEWLINE);
    }
}
