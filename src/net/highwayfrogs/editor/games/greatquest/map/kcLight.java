package net.highwayfrogs.editor.games.greatquest.map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.greatquest.math.kcVector4;

/**
 * Represents the '_kcLight' struct.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@Setter
@NoArgsConstructor
public class kcLight extends GameObject implements IMultiLineInfoWriter {
    private kcLightType lightType;
    private final kcColor4 diffuseColor = new kcColor4();
    private final kcColor4 ambientColor = new kcColor4();
    private final kcColor4 specularColor = new kcColor4();
    private final kcVector4 position = new kcVector4();
    private final kcVector4 direction = new kcVector4();
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
        this.diffuseColor.load(reader);
        this.ambientColor.load(reader);
        this.specularColor.load(reader);
        this.position.load(reader);
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

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Light Type: ").append(this.lightType).append(Constants.NEWLINE);
        this.diffuseColor.writePrefixedInfoLine(builder, "Diffuse Color", padding);
        this.ambientColor.writePrefixedInfoLine(builder, "Ambient Color", padding);
        this.specularColor.writePrefixedInfoLine(builder, "Specular Color", padding);
        this.position.writePrefixedInfoLine(builder, "Position", padding);
        this.direction.writePrefixedInfoLine(builder, "Direction", padding);

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