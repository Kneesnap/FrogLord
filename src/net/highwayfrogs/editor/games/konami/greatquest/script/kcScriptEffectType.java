package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.*;

import java.util.function.Function;

/**
 * Represents different instructions.
 * Created by Kneesnap on 8/1/2023.
 */
@Getter
@AllArgsConstructor
public enum kcScriptEffectType {
    ACTOR(Constants.BIT_FLAG_5, kcScriptEffectActor::new), // 32|0x20
    CAMERA(Constants.BIT_FLAG_16, kcScriptEffectCamera::new), // 65536|0x10000
    ENTITY(Constants.BIT_FLAG_17, kcScriptEffectEntity::new), // 131072|0x20000
    AI(Constants.BIT_FLAG_18, kcScriptEffectAI::new); // 262144|0x40000

    private final int value;
    private final Function<Integer, kcScriptEffect> effectMaker;

    /**
     * Creates a new instance of a kcScriptEffect representing this type.
     */
    public kcScriptEffect newInstance(int effectID) {
        if (this.effectMaker == null)
            throw new RuntimeException("Cannot create new kcScriptEffect for " + name() + ".");

        return this.effectMaker.apply(effectID);
    }

    /**
     * Gets the kcScriptEffectType corresponding to the provided value.
     * @param value     The value to lookup.
     * @param allowNull If null is allowed.
     * @return effectType
     */
    public static kcScriptEffectType getEffectType(int value, boolean allowNull) {
        for (int i = 0; i < values().length; i++) {
            kcScriptEffectType type = values()[i];
            if (type.value == value)
                return type;
        }

        if (!allowNull)
            throw new RuntimeException("Couldn't determine instruction type from value " + value + ".");
        return null;
    }
}