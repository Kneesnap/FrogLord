package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcBlend;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'kcParticleEmitterParam' struct.
 * Loaded from kcParticleEmitterParam::Init.
 * TODO: Really look this one over.
 * TODO: Inst flags are copied in kcCParticleEmitter::SetParticleDefaults
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
@Setter
public class kcParticleEmitterParam extends kcEntity3DDesc {
    private kcBlend srcBlend = kcBlend.ZERO; // ZERO actually means ONE.
    private kcBlend dstBlend = kcBlend.ZERO; // ZERO actually means ONE.
    private int textureHash; // TODO: Getter?
    private int descHash = -1; // TODO: What is this?
    private final kcParticleParam particleParam = new kcParticleParam();
    private float lifeTimeEmitter; // These may be garbage / unused.
    private int maxParticle;
    private static final int PADDING_VALUES = 6;

    public kcParticleEmitterParam(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public int getTargetClassID() {
        return kcClassID.PARTICLE_EMITTER.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.srcBlend = kcBlend.getMode(reader.readInt(), false);
        this.dstBlend = kcBlend.getMode(reader.readInt(), false);
        reader.skipInt(); // Texture pointer (zero)
        this.textureHash = reader.readInt();
        this.descHash = reader.readInt();
        this.particleParam.load(reader);
        this.lifeTimeEmitter = reader.readFloat();
        this.maxParticle = reader.readInt();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.srcBlend.getValue());
        writer.writeInt(this.dstBlend.getValue());
        writer.writeInt(0); // Texture pointer (zero)
        writer.writeInt(this.textureHash);
        writer.writeInt(this.descHash);
        this.particleParam.save(writer);
        writer.writeFloat(this.lifeTimeEmitter);
        writer.writeInt(this.maxParticle);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Src Blend: ").append(this.srcBlend).append(Constants.NEWLINE);
        builder.append(padding).append("Dest Blend: ").append(this.dstBlend).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Texture", this.textureHash);
        writeAssetLine(builder, padding, "Description", this.descHash);
        this.particleParam.writePrefixedMultiLineInfo(builder, "Particle Params", padding);
        builder.append(padding).append("Emitter Life Time (Garbage?): ").append(this.lifeTimeEmitter).append(Constants.NEWLINE);
        builder.append(padding).append("Max Particle: ").append(getMaxParticle()).append(Constants.NEWLINE);
    }


    public int getMaxParticle() {
        return this.maxParticle == 0 || this.maxParticle == -0x33333334 ? 250 : this.maxParticle;
    }
}