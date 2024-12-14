package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionAISetGoal;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Implements AI script effects.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcScriptEffectAI extends kcScriptEffectAction {
    private final kcActionAISetGoal action = new kcActionAISetGoal(this);;

    public static final String EFFECT_COMMAND = kcActionID.AI_SETGOAL.getFrogLordName();

    public kcScriptEffectAI(kcScriptFunction parentFunction) {
        this(parentFunction, 0); // An effect ID of 0 has been seen in
    }

    public kcScriptEffectAI(kcScriptFunction parentFunction, int effectID) {
        super(parentFunction, kcScriptEffectType.AI, effectID);
        if (effectID != 0) // Zero is the only valid effect ID, as seen in kcCScriptMgr::FireEvent()
            throw new RuntimeException("Unsupported effect ID: " + effectID + " (Expected 0)");
    }

    @Override
    public String getEffectCommandName() {
        return EFFECT_COMMAND;
    }

    @Override
    public void load(kcParamReader reader) {
        if (getEffectID() != 0)
            throw new RuntimeException("Unknown AI effect ID " + getEffectID());

        this.action.load(reader);
    }

    @Override
    public void save(kcParamWriter writer) {
        this.action.save(writer);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments, int lineNumber, String fileName) {
        this.action.load(arguments, lineNumber, fileName);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.action.save(arguments, settings);
    }
}