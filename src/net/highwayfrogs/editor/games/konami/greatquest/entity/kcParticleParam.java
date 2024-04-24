package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.map.kcColor4;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;

/**
 * Represents the 'kcParticleParam' struct.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcParticleParam extends GameObject implements IMultiLineInfoWriter {
    private int burstMode;
    private float emitAngle;
    private float emitAngleVariance;
    private int partPerSecond;
    private float speed;
    private float speedVariance;
    private float lifeTime;
    private float lifeVariance;
    private float sizeBegin;
    private float sizeEnd;
    private float sizeVariance;
    private final kcColor4 colorBegin = new kcColor4();
    private final kcColor4 colorEnd = new kcColor4();
    private final kcColor4 colorVariance = new kcColor4();
    private final kcVector4 gravityBegin = new kcVector4();
    private final kcVector4 gravityEnd = new kcVector4();
    private float gravityVariance;
    private float lineLeft;
    private float lineRight;
    private float orientation;
    private static final int PADDING_VALUES = 5;

    @Override
    public void load(DataReader reader) {
        this.burstMode = reader.readInt();
        this.emitAngle = reader.readFloat();
        this.emitAngleVariance = reader.readFloat();
        this.partPerSecond = reader.readInt();
        this.speed = reader.readFloat();
        this.speedVariance = reader.readFloat();
        this.lifeTime = reader.readFloat();
        this.lifeVariance = reader.readFloat();
        this.sizeBegin = reader.readFloat();
        this.sizeEnd = reader.readFloat();
        this.sizeVariance = reader.readFloat();
        this.colorBegin.load(reader);
        this.colorEnd.load(reader);
        this.colorVariance.load(reader);
        this.gravityBegin.load(reader);
        this.gravityEnd.load(reader);
        this.gravityVariance = reader.readFloat();
        this.lineLeft = reader.readFloat();
        this.lineRight = reader.readFloat();
        this.orientation = reader.readFloat();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.burstMode);
        writer.writeFloat(this.emitAngle);
        writer.writeFloat(this.emitAngleVariance);
        writer.writeInt(this.partPerSecond);
        writer.writeFloat(this.speed);
        writer.writeFloat(this.speedVariance);
        writer.writeFloat(this.lifeTime);
        writer.writeFloat(this.lifeVariance);
        writer.writeFloat(this.sizeBegin);
        writer.writeFloat(this.sizeEnd);
        writer.writeFloat(this.sizeVariance);
        this.colorBegin.save(writer);
        this.colorEnd.save(writer);
        this.colorVariance.save(writer);
        this.gravityBegin.save(writer);
        this.gravityEnd.save(writer);
        writer.writeFloat(this.gravityVariance);
        writer.writeFloat(this.lineLeft);
        writer.writeFloat(this.lineRight);
        writer.writeFloat(this.orientation);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Burst Mode: ").append(this.burstMode).append(Constants.NEWLINE);
        builder.append(padding).append("Emit Angle: ").append(this.emitAngle).append(Constants.NEWLINE);
        builder.append(padding).append("Emit Angle Variance: ").append(this.emitAngleVariance).append(Constants.NEWLINE);
        builder.append(padding).append("Part Per Second: ").append(this.partPerSecond).append(Constants.NEWLINE);
        builder.append(padding).append("Speed: ").append(this.speed).append(Constants.NEWLINE);
        builder.append(padding).append("Speed Variance: ").append(this.speedVariance).append(Constants.NEWLINE);
        builder.append(padding).append("Life Time: ").append(this.lifeTime).append(Constants.NEWLINE);
        builder.append(padding).append("Life Variance: ").append(this.lifeVariance).append(Constants.NEWLINE);
        this.colorBegin.writePrefixedInfoLine(builder, "Begin Color", padding);
        this.colorEnd.writePrefixedInfoLine(builder, "End Color", padding);
        this.colorVariance.writePrefixedInfoLine(builder, "Color Variance", padding);
        this.gravityBegin.writePrefixedInfoLine(builder, "Begin Gravity", padding);
        this.gravityEnd.writePrefixedInfoLine(builder, "End Gravity", padding);
        builder.append(padding).append("Gravity Variance: ").append(this.gravityVariance).append(Constants.NEWLINE);
        builder.append(padding).append("Line Left: ").append(this.lineLeft).append(Constants.NEWLINE);
        builder.append(padding).append("Line Right: ").append(this.lineRight).append(Constants.NEWLINE);
        builder.append(padding).append("Orientation: ").append(this.orientation).append(Constants.NEWLINE);
    }
}