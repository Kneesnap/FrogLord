package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.generic.InventoryItem;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Set whether the player has an item.
 * Created by Kneesnap on 11/5/2024.
 */
@Getter
public class kcActionSetPlayerHasItem extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.INVERTED_BOOLEAN, "shouldGiveItem", kcParamType.INVENTORY_ITEM, "item");
    private boolean shouldGiveItem;
    @NonNull private InventoryItem item = InventoryItem.NONE;

    public kcActionSetPlayerHasItem(kcActionExecutor executor) {
        super(executor, kcActionID.GIVE_TAKE_ITEM);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.shouldGiveItem = reader.next().getAsInvertedBoolean();
        this.item = reader.next().getEnum(InventoryItem.values());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(!this.shouldGiveItem);
        writer.write(this.item);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.item = arguments.useNext().getAsEnumOrError(InventoryItem.class);
        this.shouldGiveItem = arguments.useNext().getAsBoolean();
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.item);
        arguments.createNext().setAsBoolean(this.shouldGiveItem);
    }
}
