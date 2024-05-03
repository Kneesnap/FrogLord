package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcBlend;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;
import net.highwayfrogs.editor.games.konami.greatquest.toc.GreatQuestChunkTextureReference;

/**
 * Represents the 'kcParticleEmitterParam' struct.
 * Loaded from kcParticleEmitterParam::Init.
 * TODO: Inst flags are copied in kcCParticleEmitter::SetParticleDefaults. I'm not sure if this matters yet.
 * Created by Kneesnap on 8/22/2023.
 */
@Setter
public class kcParticleEmitterParam extends kcEntity3DDesc {
    @Getter private kcBlend srcBlend = kcBlend.ONE; // ZERO actually means ONE.
    @Getter private kcBlend dstBlend = kcBlend.ONE; // ZERO actually means ONE.
    private int textureReferenceHash = -1;
    private int selfHash = -1; // TODO: In the future, we'll get rid of this maybe. We should figure out hash tracking.
    @Getter private final kcParticleParam particleParam = new kcParticleParam();
    @Getter private float lifeTimeEmitter = -1F; // Valid Values: [-1, 0) union (0, 60) (Seen in kcCParticleEmitter::SetParticleDefaults) If the value is not in the specified range, the kcParticleParam value will be used instead.
    private int maxParticle = DEFAULT_MAX_PARTICLE;
    private static final int PADDING_VALUES = 6;

    private static final int DEFAULT_MAX_PARTICLE = 250;

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
        reader.skipPointer(); // Texture pointer (zero)
        this.textureReferenceHash = reader.readInt();
        this.selfHash = reader.readInt();
        this.particleParam.load(reader);
        this.lifeTimeEmitter = reader.readFloat();
        this.maxParticle = reader.readInt();
        GreatQuestUtils.skipPaddingRequireEmptyOrByte(reader, PADDING_VALUES * Constants.INTEGER_SIZE, GreatQuestInstance.PADDING_BYTE_DEFAULT);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.srcBlend.getValue());
        writer.writeInt(this.dstBlend.getValue());
        writer.writeInt(0); // Texture pointer (zero)
        writer.writeInt(this.textureReferenceHash);
        writer.writeInt(this.selfHash);
        this.particleParam.save(writer);
        writer.writeFloat(this.lifeTimeEmitter);
        writer.writeInt(this.maxParticle);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE); // CFrogCtl::__ct includes the constructor, which shows that padding is initialized to zero. The old CC padding values are from an old version.
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Src Blend: ").append(this.srcBlend).append(Constants.NEWLINE);
        builder.append(padding).append("Dest Blend: ").append(this.dstBlend).append(Constants.NEWLINE);
        writeAssetLine(builder, padding, "Texture", this.textureReferenceHash);
        writeAssetLine(builder, padding, "Self Hash", this.selfHash);
        this.particleParam.writePrefixedMultiLineInfo(builder, "Particle Params", padding);
        builder.append(padding).append("Emitter Life Time (Garbage?): ").append(this.lifeTimeEmitter).append(Constants.NEWLINE);
        builder.append(padding).append("Max Particle: ").append(getMaxParticle()).append(Constants.NEWLINE);
    }

    /**
     * Gets the texture reference used by this emitter.
     */
    public GreatQuestChunkTextureReference getTextureReference() {
        return getParentFile() != null ? getParentFile().getResourceByHash(this.textureReferenceHash) : null;
    }

    /**
     * Gets the max particle value.
     */
    public int getMaxParticle() {
        // TODO: If flames 'FireBallParticleParam' and it's the default padding, use 100. (CFrogCtl::SetParticleParams)
        return this.maxParticle == 0 || this.maxParticle == GreatQuestInstance.PADDING_DEFAULT_INT ? 250 : this.maxParticle;
    }
}