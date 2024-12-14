package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * Represents a cause of a player action.
 * Created by Kneesnap on 8/16/2023.
 */
@Getter
public class kcScriptCausePlayer extends kcScriptCause {
    private kcScriptCauseEntityAction action;

    public kcScriptCausePlayer(kcScript script) {
        super(script, kcScriptCauseType.PLAYER, 0, 1);
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
    public void printWarnings(ILogger logger) {
        //super.printWarnings(logger); // Don't call, so we can check the type here!
        kcCResourceEntityInst entity = getScriptEntity();
        if (entity != null && entity.getHash() == kcEntityInst.PLAYER_ENTITY_HASH)
            printWarning(logger, "will never occur because the script entity is the player entity.");

        if (!this.action.isImplementedForPlayer())
            printWarning(logger, "uses action " + this.action + ", which is not supported by the Player cause type.");
        this.action.getEntityGroup().logEntityTypeWarnings(logger, this, this.action.name());
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ this.action.ordinal();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((kcScriptCausePlayer) obj).getAction() == this.action;
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        kcCResourceEntityInst scriptEntity = getScriptEntity();
        String actorDescription = this.action.getPlayerDescription()
                .replace("another entity/player", "the player");

        // Replace script entity name.
        if (scriptEntity != null && scriptEntity.getName() != null)
            actorDescription = actorDescription.replace("the script entity", scriptEntity.getName());

        builder.append(actorDescription);
    }
}