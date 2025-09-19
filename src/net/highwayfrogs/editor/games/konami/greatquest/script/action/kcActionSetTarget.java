package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the kcAction for setting an actor's target.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionSetTarget extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH_NULL_IS_ZERO, "targetEntity");
    private final GreatQuestHash<kcCResourceEntityInst> newEntityTargetRef = new GreatQuestHash<>();

    public kcActionSetTarget(kcActionExecutor executor) {
        super(executor, kcActionID.SET_TARGET);
        this.newEntityTargetRef.setNullRepresentedAsZero();
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        setNewEntityTargetHash(reader.next().getAsInteger());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.newEntityTargetRef.getHashNumber());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        resolveResource(arguments.useNext(), kcCResourceEntityInst.class, this.newEntityTargetRef);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.newEntityTargetRef.applyGqsString(arguments.createNext(), settings);
    }

    /**
     * Resolves a hash for the target used as an argument in this action.
     * @param newTargetEntityHash the hash to resolve
     */
    public void setNewEntityTargetHash(int newTargetEntityHash) {
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceEntityInst.class, getChunkedFile(), this, this.newEntityTargetRef, newTargetEntityHash, false);
    }
}