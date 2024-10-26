package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.map.kcColor4;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector4;

/**
 * Represents the 'kcParticleParam' struct.
 * Default values obtained from 'kcParticleParam::__ct'.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcParticleParam implements IMultiLineInfoWriter, IBinarySerializable {
    private int burstMode = 1;
    private float emitAngle = .55F;
    private float emitAngleVariance = 0F;
    private int partPerSecond = 940;
    private float speed = 24.25F;
    private float speedVariance = 0F;
    private float lifeTime = 1.65F;
    private float lifeVariance = 2.1F;
    private float sizeBegin = 1.2F;
    private float sizeEnd = 1.4F;
    private float sizeVariance = .04F;
    private final kcColor4 colorBegin = new kcColor4(.7F, .94F, .94F, .97F);
    private final kcColor4 colorEnd = new kcColor4(.21F, .04F, .67F, .98F);
    private final kcColor4 colorVariance = new kcColor4(.01F, .01F, .01F, .01F);
    private final kcVector4 gravityBegin = new kcVector4(0F, .28F, 0F, 0F);
    private final kcVector4 gravityEnd = new kcVector4(0F, -0.2F, 0F, 0F);
    private float gravityVariance = 0F;
    private float lineLeft = 0F; // Range: [0, 20] (Seen in kcCParticleEmitter::SetParticleDefaults)
    private float lineRight = 0F; // Range: [0, 20] (Seen in kcCParticleEmitter::SetParticleDefaults)
    private float orientation = 0F;
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
        GreatQuestUtils.skipPaddingRequireEmptyOrByte(reader, PADDING_VALUES * Constants.INTEGER_SIZE, GreatQuestInstance.PADDING_BYTE_DEFAULT);
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
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE); // It seems older versions used 0xCC padding bytes. However, the retail versions use null bytes. Old files still contain the old padding
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