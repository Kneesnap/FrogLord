package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInheritanceGroup;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents kcActions which run commands on flags.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionFlag extends kcAction {
    private int flagMask;
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HEX_INTEGER, "flagMask");

    public kcActionFlag(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        return 0; // All of them are flags.
    }

    @Override
    public void load(kcParamReader reader) {
        this.flagMask = reader.next().getAsInteger();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.flagMask);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.flagMask = kcEntityInstanceFlag.getValueFromArguments(arguments);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        kcEntityInstanceFlag.addFlags(this.flagMask, arguments);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);

        // Warn about incompatible flags.
        kcEntity3DDesc entityDesc = getExecutor() != null ? getExecutor().getExecutingEntityDescription() : null;
        if (entityDesc != null) {
            for (kcEntityInstanceFlag flag : kcEntityInstanceFlag.values()) {
                kcEntityInheritanceGroup group = flag.getFlagType().getInheritanceGroup();
                if ((this.flagMask & flag.getInstanceBitFlagMask()) == flag.getInstanceBitFlagMask() && !group.isApplicable(entityDesc))
                    printWarning(logger, "the entity flag '" + flag.getDisplayName() + "' requires the entity description '" + entityDesc.getResource().getName() + "' to extend " + group.getDisplayName() + ", but the entity was actually a " + Utils.getSimpleName(entityDesc) + ".");
            }
        }

        // INIT_FLAGS shouldn't warn if no flags are specified since it will still do something (reset flags).
        // But the others will do nothing since there are no flags given.
        if (this.flagMask == 0 && getActionID() != kcActionID.INIT_FLAGS)
            printWarning(logger, "it does not specify any entity flags.");
    }
}