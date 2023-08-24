package net.highwayfrogs.editor.games.tgq.script.effect;

import lombok.Getter;
import net.highwayfrogs.editor.games.tgq.script.action.kcAction;
import net.highwayfrogs.editor.games.tgq.script.action.kcActionID;
import net.highwayfrogs.editor.games.tgq.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.tgq.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.tgq.script.kcScriptEffectType;

/**
 * Implements actor script effects.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcScriptEffectActor extends kcScriptEffectAction {
    private kcAction action;

    public kcScriptEffectActor(int effectID) {
        super(kcScriptEffectType.ACTOR, effectID);
    }

    @Override
    public void load(kcParamReader reader) {
        kcActionID actionID = kcActionID.getActionByOpcode(getEffectID());
        this.action = actionID.newInstance();
        this.action.load(reader);
    }

    @Override
    public void save(kcParamWriter writer) {
        this.action.save(writer);
    }
}

