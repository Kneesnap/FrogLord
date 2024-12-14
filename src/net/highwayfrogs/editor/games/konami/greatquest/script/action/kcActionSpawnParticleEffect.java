package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Shows dialog to the player when run.
 * Created by Kneesnap on 10/31/2024.
 */
@Getter
public class kcActionSpawnParticleEffect extends kcAction {
    private final GreatQuestHash<kcCResourceGeneric> particleRef = new GreatQuestHash<>();

    public static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH, "particle");

    public kcActionSpawnParticleEffect(kcActionExecutor executor) {
        super(executor, kcActionID.PARTICLE);
        this.particleRef.setNullRepresentedAsZero();
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        setParticleHash(reader.next().getAsInteger());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.particleRef.getHashNumber());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        setParticleHash(GreatQuestUtils.getAsHash(arguments.useNext(), 0, this.particleRef));
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.particleRef.applyGqsString(arguments.createNext(), settings);
    }

    /**
     * Resolves the hash of a particle definition.
     * @param newParticleHash the hash of the new dialog.
     */
    public void setParticleHash(int newParticleHash) {
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, getChunkedFile(), this, this.particleRef, newParticleHash, true);
    }
}
