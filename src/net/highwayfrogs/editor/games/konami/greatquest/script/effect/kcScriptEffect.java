package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcInterimScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;

/**
 * Represents a script effect.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
public abstract class kcScriptEffect extends GameObject<GreatQuestInstance> {
    private final kcScriptEffectType effectType;
    @Setter private int targetEntityHash;

    public kcScriptEffect(GreatQuestInstance instance, kcScriptEffectType effectType) {
        super(instance);
        this.effectType = effectType;
    }

    /**
     * Gets the effect ID that identifies the behavior which runs with this effect.
     */
    public abstract int getEffectID();

    /**
     * Writes kcParam values to the output list.
     * @param reader The source of kcParam values.
     */
    public abstract void load(kcParamReader reader);

    /**
     * Writes kcParam values to the output list.
     * @param writer The destination to write kcParam values to.
     */
    public abstract void save(kcParamWriter writer);

    /**
     * Converts this effect to an interim script effect.
     */
    public kcInterimScriptEffect toInterimScriptEffect() {
        kcInterimScriptEffect scriptEffect = new kcInterimScriptEffect(getGameInstance());
        scriptEffect.load(this);
        return scriptEffect;
    }

    /**
     * Writes the script effect to a string builder.
     * @param builder  The builder to write the script to.
     * @param settings The settings for displaying the output.
     */
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("[Target: ");
        builder.append(kcScriptDisplaySettings.getHashDisplay(settings, this.targetEntityHash, true));
        builder.append("] ");
    }
}