package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
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
        setParticleHash(getLogger(), reader.next().getAsInteger());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.particleRef.getHashNumber());
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        // This might be kcCResourceGenericType.PARTICLE_EMITTER_PARAM instead.
        resolveResource(logger, arguments.useNext(), kcCResourceGenericType.EMITTER_DESCRIPTION, this.particleRef);
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.particleRef.applyGqsString(arguments.createNext(), settings);
    }

    /**
     * Resolves the hash of a particle definition.
     * @param newParticleHash the hash of the new dialog.
     */
    public void setParticleHash(ILogger logger, int newParticleHash) {
        // This might be kcCResourceGenericType.PARTICLE_EMITTER_PARAM instead.
        GreatQuestUtils.resolveLevelResourceHash(logger, kcCResourceGenericType.EMITTER_DESCRIPTION, getChunkedFile(), this, this.particleRef, newParticleHash, true);
    }
}
