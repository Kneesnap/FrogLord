package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash.kcHashedResource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcIGenericResourceData;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.function.Function;

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
        GreatQuestUtils.resolveResourceHash(kcCResourceModel.class, parentFile, this, this.vtxModelRef, vtxResourceHash, true);
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, parentFile, this, this.cruiseParticleEffectRef, cruiseParticleEffectHash, true);
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, parentFile, this, this.hitParticleEffectRef, hitParticleEffectHash, true);
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
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);

        writeAssetInfo(builder, padding, "Model", this.vtxModelRef, kcCResourceModel::getFullPath);
        writeAssetInfo(builder, padding, "Cruise Particle Effect", this.cruiseParticleEffectRef, kcCResource::getName);
        writeAssetInfo(builder, padding, "Hit Particle Effect", this.hitParticleEffectRef, kcCResource::getName);

        builder.append(" - Projectile Life Time: ").append(this.projectileLifeTime).append(Constants.NEWLINE);
        if (this.speed != DEFAULT_SPEED && this.speed != 0)
            builder.append(" - Speed: ").append(this.speed).append(Constants.NEWLINE);
    }

    private <TResource extends kcCResource> void writeAssetInfo(StringBuilder builder, String padding, String prefix, GreatQuestHash<? extends TResource> hash, Function<TResource, String> getter) {
        builder.append(padding).append(prefix).append(": ");

        TResource resource = hash != null ? hash.getResource() : null;
        int resourceHash = hash != null ? hash.getHashNumber() : 0;
        if (resource != null) {
            builder.append(getter.apply(resource));
        } else if (resourceHash != 0 && resourceHash != -1) {
            builder.append(NumberUtils.to0PrefixedHexString(resourceHash));
        } else {
            builder.append("None");
        }

        builder.append(Constants.NEWLINE);
    }

    @Override
    public kcCResourceGenericType getResourceType() {
        return kcCResourceGenericType.LAUNCHER_DESCRIPTION;
    }

    @Override
    public void fromConfig(Config input) {
        super.fromConfig(input);
        this.resolve(input.getOptionalKeyValueNode(CONFIG_KEY_MODEL), kcCResourceModel.class, this.vtxModelRef);
        this.resolve(input.getOptionalKeyValueNode(CONFIG_KEY_CRUISE_PARTICLE_EFFECT), kcCResourceGeneric.class, this.cruiseParticleEffectRef);
        this.resolve(input.getOptionalKeyValueNode(CONFIG_KEY_HIT_PARTICLE_EFFECT), kcCResourceGeneric.class, this.hitParticleEffectRef);
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

    /**
     * Resolves a resource from a config node.
     * @param node the node to resolve the resource from
     * @param resourceClass the type of resource to resolve
     * @param hashObj the hash object to apply the result to
     * @param <TResource> the type of resource to resolve
     */
    private <TResource extends kcHashedResource> void resolve(ConfigValueNode node, Class<TResource> resourceClass, GreatQuestHash<TResource> hashObj) {
        int nodeHash = GreatQuestUtils.getAsHash(node, hashObj.isNullZero() ? 0 : -1, hashObj);
        GreatQuestUtils.resolveResourceHash(resourceClass, getParentFile(), getResource(), hashObj, nodeHash, true);
    }
}