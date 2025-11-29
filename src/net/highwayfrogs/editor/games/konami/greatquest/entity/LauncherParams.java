package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcIGenericResourceData;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents the 'LauncherParams' struct.
 * Loaded by CLauncher::Init
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
public class LauncherParams extends kcProjectileParams implements kcIGenericResourceData {
    private final kcCResourceGeneric resource;
    private final GreatQuestHash<kcCResourceModel> vtxModelRef; // Resolved in CLauncher::Init()
    private final GreatQuestHash<kcCResourceGeneric> cruiseParticleEffectRef; // Resolved in CLauncher::Init()
    private final GreatQuestHash<kcCResourceGeneric> hitParticleEffectRef; // Resolved in CLauncher::Init()
    @Setter private int projectileLifeTime; // How long the projectile lasts, in milliseconds.
    @Setter private float speed = DEFAULT_SPEED; // How fast the projectile will move.
    private static final int PADDING_VALUES = 7;

    private static final String NAME_SUFFIX = "LauncherDesc";
    private static final float DEFAULT_SPEED = 10F;

    public LauncherParams(@NonNull kcCResourceGeneric resource) {
        super(resource.getGameInstance());
        this.resource = resource;
        this.vtxModelRef = new GreatQuestHash<>();
        this.cruiseParticleEffectRef = new GreatQuestHash<>();
        this.hitParticleEffectRef = new GreatQuestHash<>();
        GreatQuestUtils.applySelfNameSuffixAndToFutureNameChanges(resource, NAME_SUFFIX);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int vtxResourceHash = reader.readInt();
        int cruiseParticleEffectHash = reader.readInt();
        int hitParticleEffectHash = reader.readInt();
        this.projectileLifeTime = reader.readInt();
        this.speed = reader.readFloat();
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);

        GreatQuestChunkedFile parentFile = this.resource.getParentFile();
        GreatQuestUtils.resolveLevelResourceHash(getLogger(), kcCResourceModel.class, parentFile, this, this.vtxModelRef, vtxResourceHash, true);
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceGenericType.PARTICLE_EMITTER_PARAM, parentFile, this, this.cruiseParticleEffectRef, cruiseParticleEffectHash, true);
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceGenericType.PARTICLE_EMITTER_PARAM, parentFile, this, this.hitParticleEffectRef, hitParticleEffectHash, true);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.vtxModelRef.getHashNumber());
        writer.writeInt(this.cruiseParticleEffectRef.getHashNumber());
        writer.writeInt(this.hitParticleEffectRef.getHashNumber());
        writer.writeInt(this.projectileLifeTime);
        writer.writeFloat(this.speed);
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        this.vtxModelRef.addToPropertyList(propertyList, "Model", getParentFile(), kcCResourceModel.class);
        this.cruiseParticleEffectRef.addToPropertyList(propertyList, "Cruise Particle Effect", getParentFile(), kcCResourceGenericType.PARTICLE_EMITTER_PARAM);
        this.hitParticleEffectRef.addToPropertyList(propertyList, "Hit Particle Effect", getParentFile(), kcCResourceGenericType.PARTICLE_EMITTER_PARAM);
        propertyList.addInteger("Projectile Life Time", this.projectileLifeTime, newValue -> this.projectileLifeTime = newValue);
        propertyList.addFloat("Speed", this.speed, newValue -> this.speed = newValue);
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.LAUNCHER_DESCRIPTION;
    }

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);
        GreatQuestUtils.resolveLevelResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_MODEL), kcCResourceModel.class, getParentFile(), getResource(), this.vtxModelRef, true);
        GreatQuestUtils.resolveLevelResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_CRUISE_PARTICLE_EFFECT), kcCResourceGenericType.PARTICLE_EMITTER_PARAM, getParentFile(), getResource(), this.cruiseParticleEffectRef, true);
        GreatQuestUtils.resolveLevelResource(logger, input.getOptionalKeyValueNode(CONFIG_KEY_HIT_PARTICLE_EFFECT), kcCResourceGenericType.PARTICLE_EMITTER_PARAM, getParentFile(), getResource(), this.hitParticleEffectRef, true);
        this.projectileLifeTime = Math.round(input.getKeyValueNodeOrError(CONFIG_KEY_PROJECTILE_LIFE_TIME).getAsFloat() * 1000F);
        this.speed = input.getOrDefaultKeyValueNode(CONFIG_KEY_SPEED).getAsFloat(DEFAULT_SPEED);
    }

    private static final String CONFIG_KEY_MODEL = "projectileModel";
    private static final String CONFIG_KEY_CRUISE_PARTICLE_EFFECT = "cruiseParticleEffect";
    private static final String CONFIG_KEY_HIT_PARTICLE_EFFECT = "hitParticleEffect";
    private static final String CONFIG_KEY_PROJECTILE_LIFE_TIME = "projectileLifeTime";
    private static final String CONFIG_KEY_SPEED = "projectileSpeed";

    @Override
    public void toConfig(Config output) {
        super.toConfig(output);
        kcScriptDisplaySettings settings = getParentFile().createScriptDisplaySettings();
        if (this.vtxModelRef.getResource() != null || this.vtxModelRef.getOriginalString() != null)
            output.getOrCreateKeyValueNode(CONFIG_KEY_MODEL).setAsString(this.vtxModelRef.getAsGqsString(settings));
        if (this.cruiseParticleEffectRef.getResource() != null || this.cruiseParticleEffectRef.getOriginalString() != null)
            output.getOrCreateKeyValueNode(CONFIG_KEY_CRUISE_PARTICLE_EFFECT).setAsString(this.cruiseParticleEffectRef.getAsGqsString(settings));
        if (this.hitParticleEffectRef.getResource() != null || this.hitParticleEffectRef.getOriginalString() != null)
            output.getOrCreateKeyValueNode(CONFIG_KEY_HIT_PARTICLE_EFFECT).setAsString(this.hitParticleEffectRef.getAsGqsString(settings));
        output.getOrCreateKeyValueNode(CONFIG_KEY_PROJECTILE_LIFE_TIME).setAsFloat(this.projectileLifeTime  / 1000F);
        output.getOrCreateKeyValueNode(CONFIG_KEY_SPEED).setAsFloat(this.speed);
    }
}