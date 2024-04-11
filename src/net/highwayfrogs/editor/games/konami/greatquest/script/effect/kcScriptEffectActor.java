package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;

/**
 * Implements actor script effects.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
public class kcScriptEffectActor extends kcScriptEffectAction {
    private kcAction action;

    public kcScriptEffectActor(int effectID) {
        super(kcScriptEffectType.ACTOR, effectID);
    }

    public kcScriptEffectActor(kcAction action, int targetEntity) {
        super(kcScriptEffectType.ACTOR, action.getActionID().getOpcode());
        this.action = action;
        setTargetEntityHash(targetEntity);
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