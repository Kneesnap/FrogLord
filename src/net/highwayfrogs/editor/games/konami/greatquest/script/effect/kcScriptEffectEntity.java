package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.*;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents an entity script effect.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcScriptEffectEntity extends kcScriptEffectAction {
    private kcAction action;

    public kcScriptEffectEntity(kcScriptFunction parentFunction, int effectID) {
        this(parentFunction, kcEntityEffect.getTypeOrError(effectID));
    }

    public kcScriptEffectEntity(kcScriptFunction parentFunction, @NonNull kcEntityEffect effect) {
        super(parentFunction, kcScriptEffectType.ENTITY, effect.ordinal());
    }

    @Override
    public kcAction getAction() {
        if (this.action != null)
            return this.action;

        switch (getEntityEffect()) {
            case ACTIVATE_ENTITY:
                return this.action = new kcActionActivate(this, true);
            case DEACTIVATE_ENTITY:
                return this.action = new kcActionActivate(this, false);
            case ENABLE_ENTITY_UPDATES:
                return this.action = new kcActionEnableUpdate(this, true);
            case DISABLE_ENTITY_UPDATES:
                return this.action = new kcActionEnableUpdate(this, false);
            case ACTIVATE_SPECIAL:
                return this.action = new kcActionActivateSpecial(this);
            default:
                throw new RuntimeException("Unsupported Entity effect " + getEntityEffect());
        }
    }

    @Override
    public String getEffectCommandName() {
        return getEntityEffect().getFrogLordDisplayName();
    }

    @Override
    public void load(kcParamReader reader) {
        if (getAction().getActionID() == kcActionID.ACTIVATE_SPECIAL)
            getAction().load(reader);
    }

    @Override
    public void save(kcParamWriter writer) {
        if (getAction().getActionID() == kcActionID.ACTIVATE_SPECIAL)
            getAction().save(writer);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        if (getAction().getActionID() == kcActionID.ACTIVATE_SPECIAL)
            getAction().load(arguments);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        if (getAction().getActionID() == kcActionID.ACTIVATE_SPECIAL)
            getAction().save(arguments, settings);
    }

    /**
     * Gets the entity effect represented by this object.
     */
    public kcEntityEffect getEntityEffect() {
        return kcEntityEffect.getType(getEffectID(), false);
    }

    @Getter
    @AllArgsConstructor
    public enum kcEntityEffect {
        ACTIVATE_ENTITY("Entity.Activate"),
        DEACTIVATE_ENTITY("Entity.Deactivate"),
        ENABLE_ENTITY_UPDATES("Entity.EnableUpdates"),
        DISABLE_ENTITY_UPDATES("Entity.DisableUpdates"),
        ACTIVATE_SPECIAL("Entity.ActivateSpecial");

        private final String frogLordDisplayName;

        /**
         * Gets the kcEntityEffect corresponding to the provided value.
         * Throws an exception if the value does not correspond to an effect.
         * @param value The value to lookup.
         * @return entityEffect
         */
        public static kcEntityEffect getTypeOrError(int value) {
            return getType(value, false);
        }

        /**
         * Gets the kcEntityEffect corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return entityEffect
         */
        public static kcEntityEffect getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the kcEntityEffect from value " + value + ".");
            }

            return values()[value];
        }

        /**
         * Gets the entity effect by its command name.
         * @param commandName The name of the command.
         * @return entityEffect or null
         */
        public static kcEntityEffect getType(String commandName) {
            for (int i = 0; i < values().length; i++) {
                kcEntityEffect effect = values()[i];
                if (effect.getFrogLordDisplayName() != null && effect.getFrogLordDisplayName().equals(commandName))
                    return effect;
            }

            return null;
        }
    }
}