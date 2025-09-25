package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.generic.InventoryItem;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Implements the kcAction 'WITH_ITEM', which we've decided to call 'SendPlayerHasItem'.
 * Created by Kneesnap on 8/18/2025.
 */
public class kcActionSendPlayerHasItem extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.INVENTORY_ITEM, "item");
    @NonNull private InventoryItem item = InventoryItem.NONE;

    public kcActionSendPlayerHasItem(kcActionExecutor executor) {
        super(executor, kcActionID.WITH_ITEM);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.item = reader.next().getEnum(InventoryItem.values());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.item);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.item = arguments.useNext().getAsEnumOrError(InventoryItem.class);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.item);
    }
    @Override
    public void printAdvancedWarnings(kcScriptValidationData data) {
        super.printAdvancedWarnings(data);
        if (!data.anyCausesMatch(kcScriptCauseType.WHEN_ITEM, null))
            printWarning(data.getLogger(), data.getEntityName() + " has no cause listening for " + kcScriptCauseType.WHEN_ITEM.getDisplayName() + ".");
    }
}
