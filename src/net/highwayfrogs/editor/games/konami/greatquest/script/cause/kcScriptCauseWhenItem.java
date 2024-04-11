package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * The scripting system has no support for conditional statements.
 * For instance, you can't say "If player has item, do X"
 * This is how they got around this.
 * The script action 'WITHITEM {itemType}' will fire functions with this cause.
 * This can specify whether it should run if the item is found or if it is not found.
 * This way, you can for example, handle planting a seed but only if they have a seed to plant.
 * Or alternatively, show a message if a player is trying to open a door they need a key for.
 * Created by Kneesnap on 8/17/2023.
 */
public class kcScriptCauseWhenItem extends kcScriptCause {
    private boolean playerShouldHaveItem;

    public kcScriptCauseWhenItem() {
        super(kcScriptCauseType.WHEN_ITEM, 1);
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
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When the 'WHEN_ITEM' action is run and the player ");
        builder.append(this.playerShouldHaveItem ? "has" : "does not have");
        builder.append(" the specified item.");
    }
}