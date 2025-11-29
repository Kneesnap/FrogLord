package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

/**
 * Represents the 'Loop' command.
 * Created by Kneesnap on 10/31/2025.
 */
public class kcActionLoop extends kcActionTemplate {
    private int loopCount;

    private static final int MAX_LOOP_COUNT = Integer.MAX_VALUE;
    private static final String MAX_ARGUMENT = "MAX";
    private static final kcArgument[] LOOP_ARGUMENTS = kcArgument.make(kcParamType.INT, "loopCount");

    public kcActionLoop(kcActionExecutor executor) {
        super(executor, kcActionID.LOOP);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return LOOP_ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.loopCount = reader.next().getAsInteger();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.loopCount);
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        StringNode node = arguments.useNext();
        if (MAX_ARGUMENT.equalsIgnoreCase(node.getAsString())) {
            this.loopCount = MAX_LOOP_COUNT;
        } else {
            this.loopCount = node.getAsInteger();
        }
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        if (MAX_LOOP_COUNT == this.loopCount) {
            arguments.createNext().setAsString(MAX_ARGUMENT, false);
        } else {
            arguments.createNext().setAsInteger(this.loopCount);
        }
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        if (this.loopCount <= 0)
            printWarning(logger, "the number of times to loop was less than or equal to zero!");
    }
}
