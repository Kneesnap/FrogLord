package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptValidationData;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * The scripting system has no support for conditional statements.
 * For instance, you can't say "If player has item, do X"
 * This is how they got around this.
 * The script action 'SendPlayerHasItem {itemType}' will fire functions with this cause.
 * This can specify whether it should run if the item is found or if it is not found.
 * This way, you can for example, handle planting a seed but only if they have a seed to plant.
 * Or alternatively, show a message if a player is trying to open a door they need a key for.
 * Created by Kneesnap on 8/17/2023.
 */
@Getter
public class kcScriptCauseWhenItem extends kcScriptCause {
    private boolean playerShouldHaveItem;

    public kcScriptCauseWhenItem(kcScript script) {
        super(script, kcScriptCauseType.WHEN_ITEM, 1, 1);
    }

    @Override
    public void load(int primaryValue, List<Integer> extraValues) {
        this.playerShouldHaveItem = !readBoolean(primaryValue, "playerShouldHaveItem in kcScriptCauseWhenItem");

        int unusedValueZero = extraValues.get(0);
        if (unusedValueZero != 0)
            throw new RuntimeException("The unused value in kcScriptCauseWhenItem is expected to be zero, but actually was " + unusedValueZero + ".");
    }

    @Override
    public void save(List<Integer> output) {
        writeBoolean(output, !this.playerShouldHaveItem);
        output.add(0);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.playerShouldHaveItem = arguments.useNext().getAsBoolean();
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsBoolean(this.playerShouldHaveItem);
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (this.playerShouldHaveItem ? Constants.BIT_FLAG_24 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((kcScriptCauseWhenItem) obj).isPlayerShouldHaveItem() == this.playerShouldHaveItem;
    }

    @Override
    public void printAdvancedWarnings(kcScriptValidationData data) {
        super.printAdvancedWarnings(data);
        if (!data.anyActionsMatch(kcActionID.WITH_ITEM))
            printWarning(data.getLogger(), data.getEntityName() + " never executes " + kcActionID.WITH_ITEM.getFrogLordName() + ".");

        // Because an entity terminating is still capable of calling SendNumber, and triggering its own script listeners, even if kcScriptCause.isEntityTerminated is true,
        // it is likely that OnReceivePlayerHasItem works as well.
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When 'SendPlayerHasItem' checks the player's inventory for an item and they ");
        builder.append(this.playerShouldHaveItem ? "have" : "do not have");
        builder.append(" it");
    }
}