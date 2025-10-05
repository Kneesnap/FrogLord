package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Implements actor script effects.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
public class kcScriptEffectActor extends kcScriptEffectAction {
    private final kcAction action;

    public kcScriptEffectActor(kcScriptFunction parentFunction, int effectID) {
        super(parentFunction, kcScriptEffectType.ACTOR, effectID);
        kcActionID actionID = kcActionID.getActionByOpcode(effectID);
        this.action = actionID.newInstance(this);
    }

    public kcScriptEffectActor(kcScriptFunction parentFunction, @NonNull kcActionID actionId) {
        this(parentFunction, actionId.getOpcode());
    }

    @Override
    public String getEffectCommandName() {
        return this.action.getActionID().getFrogLordName();
    }

    @Override
    public void load(kcParamReader reader) {
        this.action.load(reader);
    }

    @Override
    public void save(kcParamWriter writer) {
        this.action.save(writer);
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments, int lineNumber, String fileName) {
        this.action.load(logger, arguments, lineNumber, fileName);
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.action.save(logger, arguments, settings);
    }
}