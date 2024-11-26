package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.CPropDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.CharacterParams;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a cause of an actor action.
 * Created by Kneesnap on 8/19/2023.
 */
@Getter
public class kcScriptCauseActor extends kcScriptCause {
    private kcScriptCauseEntityAction action;

    public kcScriptCauseActor(kcScript script) {
        super(script, kcScriptCauseType.ACTOR, 0, 1);
    }

    @Override
    public void load(int primaryValue, List<Integer> extraValues) {
        this.action = kcScriptCauseEntityAction.getAction(primaryValue, false);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.action.getValue());
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.action = arguments.useNext().getAsEnumOrError(kcScriptCauseEntityAction.class);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.action);
    }

    @Override
    public void printWarnings(Logger logger) {
        // super.printWarnings(logger); // Don't call this, since we'll be testing the entity description here too.
        if (!this.action.isImplementedForActor())
            printWarning(logger, "uses action " + this.action + ", which is not supported by the Actor cause type.");
        this.action.getEntityGroup().logEntityTypeWarnings(logger, this, this.action.name());
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((kcScriptCauseActor) obj).getAction() == this.action;
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        kcCResourceEntityInst scriptEntity = getScriptEntity();
        String actorDescription = this.action.getActorDescription();

        // Replace actor description.
        if (this.action == kcScriptCauseEntityAction.BUMPS) {
            kcEntityInst entityInst = scriptEntity != null ? scriptEntity.getInstance() : null;
            kcEntity3DDesc entityDesc = entityInst != null ? entityInst.getDescription() : null;
            if (entityDesc instanceof CPropDesc) { // Listening CProp expects CCharacter.
                actorDescription = actorDescription.replace("another entity/player", "a CCharacter");
            } else if (entityDesc instanceof CharacterParams) { // Listening CCharacter expects kcCActorBase.
                actorDescription = actorDescription.replace("another entity/player", "another kcCActorBase");
            }
        } else {
            actorDescription = actorDescription.replace("another entity/player", "another actor (kcCActorBase)");
        }

        // Replace script entity with its name.
        if (scriptEntity != null && scriptEntity.getName() != null)
            actorDescription = actorDescription.replace("the script entity", scriptEntity.getName());

        builder.append(actorDescription);
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ this.action.ordinal();
    }
}