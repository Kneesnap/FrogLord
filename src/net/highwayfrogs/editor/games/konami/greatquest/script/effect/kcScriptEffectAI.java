package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionAISetGoal;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;

/**
 * Implements AI script effects.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcScriptEffectAI extends kcScriptEffectAction {
    private kcAction action;

    public kcScriptEffectAI(GreatQuestInstance gameInstance, int effectID) {
        super(gameInstance, kcScriptEffectType.AI, effectID);
    }

    @Override
    public void load(kcParamReader reader) {
        if (getEffectID() != 0)
            throw new RuntimeException("Unknown AI effect ID " + getEffectID());

        this.action = new kcActionAISetGoal();
        this.action.load(reader);
    }

    @Override
    public void save(kcParamWriter writer) {
        this.action.save(writer);
    }
}