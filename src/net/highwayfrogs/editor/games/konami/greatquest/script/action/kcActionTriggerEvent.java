package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'TRIGGER_EVENT' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionTriggerEvent extends kcActionTemplate {
    private final GreatQuestHash<?> eventRef = new GreatQuestHash<>();

    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH, "eventHash");

    public kcActionTriggerEvent(kcActionExecutor executor) {
        super(executor, kcActionID.TRIGGER_EVENT);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        int eventHash = reader.next().getAsInteger();
        String eventName = GreatQuestUtils.getEventName(eventHash);
        if (eventName != null) {
            this.eventRef.setHash(eventName);
        } else {
            this.eventRef.setHash(eventHash);
        }
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.eventRef.getHashNumber());
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        String eventName = arguments.useNext().getAsString(); // We can't resolve the sequence by the hash of the string normally since these seem to use randomized hash values.
        if (NumberUtils.isHexInteger(eventName)) {
            this.eventRef.setHash(NumberUtils.parseHexInteger(eventName));
        } else {
            this.eventRef.setHash(eventName);
        }
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.eventRef.applyGqsString(arguments.createNext(), settings);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        if (GreatQuestUtils.getEventName(this.eventRef.getHashNumber()) == null)
            printWarning(logger, "the event " + this.eventRef.getDisplayString(false) + " is not supported by the game. (Has it has been spelled correctly?)");
    }
}