package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkTextureReference;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcBlend;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents the 'kcParticleEmitterParam' struct.
 * Loaded from kcParticleEmitterParam::Init.
 * NOTE: This cannot be used in kcCResourceEntityInst, see kcCResourceEntityInst::Prepare() to see that it will not work.
 * Created by Kneesnap on 8/22/2023.
 */
@Setter
public class kcParticleEmitterParam extends kcEntity3DDesc {
    @Getter private kcBlend srcBlend = kcBlend.ONE; // ZERO actually means ONE.
    @Getter private kcBlend dstBlend = kcBlend.ONE; // ZERO actually means ONE.
    @Getter private final GreatQuestHash<GreatQuestChunkTextureReference> textureRef; // Resolved by kcCParticleEmitter::Init. CFrogCtl::__ct() shows how parent hash can be set to -1. However, some hashes already exist which are not -1.
    @Getter private final GreatQuestHash<kcCResourceGeneric> parentHash; // This is -1 sometimes. The conditions for it being -1 seem unclear, but it looks like potentially it's older data OR data shared between levels though it's hard to say for sure. CFrogCtl::__ct() shows how parent hash can be set to -1. However, some hashes already exist which are not -1.
    @Getter private final kcParticleParam particleParam = new kcParticleParam();
    @Getter private float lifeTimeEmitter = -1F; // Valid Values: [-1, 0) union (0, 60) (Seen in kcCParticleEmitter::SetParticleDefaults) If the value is not in the specified range, the kcParticleParam value will be used instead.
    private int maxParticle = DEFAULT_MAX_PARTICLE;
    private static final int PADDING_VALUES = 6;

    private static final int DEFAULT_FIREBALL_PARTICLE = 100;
    private static final int DEFAULT_MAX_PARTICLE = 250;
    public static final String NAME_SUFFIX = "ParticleParam";

    public kcParticleEmitterParam(@NonNull kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.PARTICLE_EMITTER);
        this.textureRef = new GreatQuestHash<>();
        this.parentHash = new GreatQuestHash<>(resource);
        GreatQuestUtils.applySelfNameSuffixAndToFutureNameChanges(resource, NAME_SUFFIX);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.srcBlend = kcBlend.getMode(reader.readInt(), false);
        this.dstBlend = kcBlend.getMode(reader.readInt(), false);
        reader.skipPointer(); // Texture pointer (zero)
        int textureReferenceHash = reader.readInt();
        int hThis = reader.readInt();
        this.particleParam.load(reader);
        this.lifeTimeEmitter = reader.readFloat();
        this.maxParticle = reader.readInt();
        GreatQuestUtils.skipPaddingRequireEmptyOrByte(reader, PADDING_VALUES * Constants.INTEGER_SIZE, GreatQuestInstance.PADDING_BYTE_DEFAULT);

        // Resolve hashes.
        GreatQuestUtils.resolveLevelResourceHash(GreatQuestChunkTextureReference.class, this, this.textureRef, textureReferenceHash, true);
        if (hThis != this.parentHash.getHashNumber() && hThis != -1)
            throw new RuntimeException("The kcParticleEmitterParam reported the parent chunk as " + NumberUtils.to0PrefixedHexString(hThis) + ", but it was expected to be " + this.parentHash.getHashNumberAsString() + ".");
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.srcBlend.getValue());
        writer.writeInt(this.dstBlend.getValue());
        writer.writeInt(0); // Texture pointer (zero)
        writer.writeInt(this.textureRef.getHashNumber());
        writer.writeInt(this.parentHash.getHashNumber());
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
        writeAssetLine(builder, padding, "Texture", this.textureRef);
        writeAssetLine(builder, padding, "Self Hash", this.parentHash);
        this.particleParam.writePrefixedMultiLineInfo(builder, "Particle Params", padding);
        builder.append(padding).append("Emitter Life Time (Garbage?): ").append(this.lifeTimeEmitter).append(Constants.NEWLINE);
        builder.append(padding).append("Max Particle: ").append(getMaxParticle()).append(Constants.NEWLINE);
    }

    private static final String CONFIG_KEY_SOURCE_BLEND = "srcBlend";
    private static final String CONFIG_KEY_DESTINATION_BLEND = "dstBlend";
    private static final String CONFIG_KEY_TEXTURE = "texture";
    private static final String CONFIG_KEY_LIFETIME_EMITTER = "lifeTimeEmitter";

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);
        this.srcBlend = input.getOrDefaultKeyValueNode(CONFIG_KEY_SOURCE_BLEND).getAsEnum(kcBlend.ONE);
        this.dstBlend = input.getOrDefaultKeyValueNode(CONFIG_KEY_DESTINATION_BLEND).getAsEnum(kcBlend.ONE);
        this.lifeTimeEmitter = input.getOrDefaultKeyValueNode(CONFIG_KEY_LIFETIME_EMITTER).getAsFloat(-1);
        this.resolveResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_TEXTURE), GreatQuestChunkTextureReference.class, this.textureRef);
        if (this.lifeTimeEmitter < -1 || this.lifeTimeEmitter >= 60) // Allow zero as the indicator to use from the kcParticleParam struct instead.
            throw new RuntimeException("The lifeTimeEmitter value (" + this.lifeTimeEmitter + ") was not in the expected range!");

        this.particleParam.fromConfig(logger, input);
    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SOURCE_BLEND).setAsEnum(this.srcBlend);
        output.getOrCreateKeyValueNode(CONFIG_KEY_DESTINATION_BLEND).setAsEnum(this.dstBlend);
        output.getOrCreateKeyValueNode(CONFIG_KEY_TEXTURE).setAsString(this.textureRef.getAsGqsString(settings));
        output.getOrCreateKeyValueNode(CONFIG_KEY_LIFETIME_EMITTER).setAsFloat(this.lifeTimeEmitter);
        this.particleParam.toConfig(output);
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.PARTICLE_EMITTER_PARAM;
    }

    /**
     * Gets the max particle value.
     */
    public int getMaxParticle() {
        if (this.maxParticle != 0 && this.maxParticle != GreatQuestInstance.PADDING_DEFAULT_INT) // CFrogCtl::SetParticleParams
            return this.maxParticle;

        if (getResource() != null && getResource().doesNameMatch("FireBall")) { // CFrogCtl::SetParticleParams
            return DEFAULT_FIREBALL_PARTICLE;
        } else {
            return DEFAULT_MAX_PARTICLE;
        }
    }
}