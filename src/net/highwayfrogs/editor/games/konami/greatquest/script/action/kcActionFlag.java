package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityFlag.kcEntityInstanceFlag;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.logging.Logger;

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
    public void printWarnings(Logger logger) {
        super.printWarnings(logger);

        // Warn about incompatible flags.
        for (kcEntityInstanceFlag flag : kcEntityInstanceFlag.values())
            if ((this.flagMask & flag.getInstanceBitFlagMask()) == flag.getInstanceBitFlagMask())
                flag.getFlagType().getInheritanceGroup().logEntityTypeWarnings(logger, this, flag.getDisplayName());
    }
}