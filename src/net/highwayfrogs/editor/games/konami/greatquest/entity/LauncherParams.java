package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceModel;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Function;

/**
 * Represents the 'LauncherParams' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
public class LauncherParams extends kcProjectileParams {
    private int vtxResourceHash;
    private int cruiseParticleEffectHash;
    private int hitParticleEffectHash;
    private int projectileLifeTime;
    private float speed;
    private final int[] padLauncherParams = new int[7];
    private final transient GreatQuestChunkedFile holder;

    public LauncherParams(GreatQuestChunkedFile holder) {
        this.holder = holder;
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.vtxResourceHash = reader.readInt();
        this.cruiseParticleEffectHash = reader.readInt();
        this.hitParticleEffectHash = reader.readInt();
        this.projectileLifeTime = reader.readInt();
        this.speed = reader.readFloat();
        for (int i = 0; i < this.padLauncherParams.length; i++)
            this.padLauncherParams[i] = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.vtxResourceHash);
        writer.writeInt(this.cruiseParticleEffectHash);
        writer.writeInt(this.hitParticleEffectHash);
        writer.writeInt(this.projectileLifeTime);
        writer.writeFloat(this.speed);
        for (int i = 0; i < this.padLauncherParams.length; i++)
            writer.writeInt(this.padLauncherParams[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);

        writeAssetInfo(builder, padding, "Model", this.vtxResourceHash, kcCResourceModel::getFullPath);
        writeAssetInfo(builder, padding, "Cruise Particle Effect", this.cruiseParticleEffectHash, kcCResource::getName);
        writeAssetInfo(builder, padding, "Hit Particle Effect", this.hitParticleEffectHash, kcCResource::getName);

        builder.append(" - Projectile Life Time: ").append(this.projectileLifeTime).append(Constants.NEWLINE);
        builder.append(" - Speed: ").append(this.speed).append(Constants.NEWLINE);
    }

    private <TResource extends kcCResource> void writeAssetInfo(StringBuilder builder, String padding, String prefix, int resourceHash, Function<TResource, String> getter) {
        builder.append(padding).append(prefix).append(": ");

        TResource resource = this.holder != null ? this.holder.getResourceByHash(resourceHash) : null;
        if (resource != null) {
            builder.append(getter.apply(resource));
        } else if (resourceHash != 0 && resourceHash != -1) {
            builder.append(Utils.to0PrefixedHexString(resourceHash));
        } else {
            builder.append("None");
        }

        builder.append(Constants.NEWLINE);
    }
}