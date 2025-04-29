package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcIGenericResourceData;
import net.highwayfrogs.editor.utils.NumberUtils;

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
    @Setter private int projectileLifeTime;
    @Setter private float speed;
    private static final int PADDING_VALUES = 7;

    private static final String NAME_SUFFIX = "LauncherDesc";

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
}