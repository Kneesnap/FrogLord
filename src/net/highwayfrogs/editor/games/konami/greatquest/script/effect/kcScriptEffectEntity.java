package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import net.highwayfrogs.editor.games.konami.greatquest.script.action.*;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;

/**
 * Represents an entity script effect.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcScriptEffectEntity extends kcScriptEffectAction {
    private kcAction action;

    public kcScriptEffectEntity(kcScriptFunction parentFunction, int effectID) {
        super(parentFunction, kcScriptEffectType.ENTITY, effectID);
    }

    @Override
    public kcAction getAction() {
        if (this.action != null)
            return this.action;

        switch (getEffectID()) {
            case 0:
                kcActionActivate activate = new kcActionActivate(getChunkedFile());
                activate.setNewState(true);
                return activate;
            case 1:
                return new kcActionActivate(getChunkedFile());
            case 2:
                kcActionEnableUpdate enableUpdate = new kcActionEnableUpdate(getChunkedFile());
                enableUpdate.setNewState(true);
                return enableUpdate;
            case 3:
                return new kcActionEnableUpdate(getChunkedFile());
            case 4:
                return new kcActionActivateSpecial(getChunkedFile());
            default:
                throw new RuntimeException("Unknown Entity effect ID " + getEffectID());
        }
    }

    @Override
    public void load(kcParamReader reader) {
        this.action = getAction();
        if (this.action.getActionID() == kcActionID.ACTIVATE_SPECIAL)
            this.action.load(reader);
    }

    @Override
    public void save(kcParamWriter writer) {
        if (this.action.getActionID() == kcActionID.ACTIVATE_SPECIAL)
            this.action.save(writer);
    }
}