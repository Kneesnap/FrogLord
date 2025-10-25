package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

/**
 * Represents a camera script effect.
 * Implementation of kcCScriptMgr::FireCameraEffect
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcScriptEffectCamera extends kcScriptEffect {
    private final kcCameraEffect cameraEffect;
    private kcParam[] arguments;
    private final GreatQuestHash<kcCResourceEntityInst> entityParamRef; // Confirmed that -1 is null by kcCCameraStack::OnSetTarget.

    private static final int TARGET_ENTITY_REF_INDEX = 0;

    public kcScriptEffectCamera(kcScriptFunction parentFunction, int effectID) {
        this(parentFunction, kcCameraEffect.getType(effectID, false));
    }

    public kcScriptEffectCamera(kcScriptFunction parentFunction, kcCameraEffect effect) {
        super(parentFunction, kcScriptEffectType.CAMERA);
        if (effect == null)
            throw new NullPointerException("effect");

        this.cameraEffect = effect;
        this.entityParamRef = effect.hasTargetEntity() ? new GreatQuestHash<>() : null;
    }

    @Override
    public int getEffectID() {
        return this.cameraEffect.ordinal();
    }

    @Override
    public String getEffectCommandName() {
        return this.cameraEffect.getFrogLordDisplayName();
    }

    @Override
    public void load(kcParamReader reader) {
        this.arguments = reader.getArguments();
        reader.setCurrentIndex(this.arguments.length);
        resolveEntityFromArguments(getLogger());
    }

    @Override
    public void save(kcParamWriter writer) {
        if (this.entityParamRef != null) // Ensure we save the target entity hash.
            this.arguments[TARGET_ENTITY_REF_INDEX].setValue(this.entityParamRef.getHashNumber());

        for (int i = 0; i < this.arguments.length; i++)
            writer.write(this.arguments[i]);
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments, int lineNumber, String fileName) {
        this.arguments = new kcParam[this.cameraEffect.getArguments().length];
        for (int i = 0; i < this.arguments.length; i++) {
            StringNode node = arguments.useNext();
            kcArgument argumentTemplate = this.cameraEffect.getArguments()[i];

            kcParam newArgument = new kcParam();
            this.arguments[i] = newArgument;
            newArgument.fromConfigNode(logger, this, getGameInstance(), node, argumentTemplate.getType());
        }

        resolveEntityFromArguments(logger);
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        for (int i = 0; i < Math.min(this.arguments.length, this.cameraEffect.getArguments().length); i++) {
            StringNode node = arguments.createNext();
            kcArgument argumentTemplate = this.cameraEffect.getArguments()[i];

            if (this.entityParamRef != null && i == TARGET_ENTITY_REF_INDEX) {
                this.entityParamRef.applyGqsString(node, settings); // Confirmed that -1 is null by kcCCameraStack::OnSetTarget.
            } else {
                this.arguments[i].toConfigNode(this, settings, node, argumentTemplate.getType());
            }
        }
    }

    private void resolveEntityFromArguments(ILogger logger) {
        if (this.entityParamRef == null)
            return;

        int entityHash = this.arguments[0].getAsInteger();
        GreatQuestUtils.resolveLevelResourceHash(logger, kcCResourceEntityInst.class, getChunkedFile(), this, this.entityParamRef, entityHash, false);
    }

    @Override
    public boolean isActionApplicableToTarget() {
        return true; // I believe this will work even if the target entity is not found.
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        super.toString(builder, settings);
        kcAction.writeAction(this, builder, this.cameraEffect.name(), this.cameraEffect.getArguments(), this.arguments, settings);
    }

    @Getter
    @AllArgsConstructor
    public enum kcCameraEffect {
        ACTIVATE_CAMERA("ActivateCamera", kcArgument.make(kcParamType.TIMESTAMP_TICK, "transitionTicks")),
        DEACTIVATE_CAMERA("DeactivateCamera", kcArgument.make(kcParamType.TIMESTAMP_TICK, "transitionTicks")),
        SET_CAMERA_TARGET("SetCameraTarget", kcArgument.make(kcParamType.HASH, "targetEntity")),
        SET_CAMERA_PIVOT("SetCameraPivot", kcArgument.make(kcParamType.HASH, "targetEntity")), // kcCCameraStack::OnSetPivot will only do something if the classID of the entity is 0x2CB3B5FF (kcCCameraPivot)
        SET_CAMERA_PARAM("SetCameraParam", kcArgument.make(kcParamType.CAMERA_PIVOT_PARAM, "param", kcParamType.FLOAT, "value"));

        private final String frogLordDisplayName;
        private final kcArgument[] arguments;

        /**
         * Returns true iff there is a target entity parameter for this effect.
         */
        public boolean hasTargetEntity() {
            return this == SET_CAMERA_TARGET || this == SET_CAMERA_PIVOT;
        }

        /**
         * Gets the kcCameraEffect corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return cameraEffect
         */
        public static kcCameraEffect getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the kcCameraEffect from value " + value + ".");
            }

            return values()[value];
        }

        /**
         * Gets the camera effect by its command name.
         * @param commandName The name of the command.
         * @return cameraEffect or null
         */
        public static kcCameraEffect getType(String commandName) {
            for (int i = 0; i < values().length; i++) {
                kcCameraEffect effect = values()[i];
                if (effect.getFrogLordDisplayName() != null && effect.getFrogLordDisplayName().equals(commandName))
                    return effect;
            }

            return null;
        }
    }

    @Getter
    @AllArgsConstructor
    public enum kcCameraPivotParam {
        PIVOT_DISTANCE,
        TARGET_OFFSET_X,
        TARGET_OFFSET_Y,
        TARGET_OFFSET_Z,
        PIVOT_OFFSET_X,
        PIVOT_OFFSET_Y,
        PIVOT_OFFSET_Z,
        TRANSITION_DURATION,
        CAMERA_BASE_FLAGS;

        /**
         * Gets the kcCameraPivotParam corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return cameraPivotParam
         */
        public static kcCameraPivotParam getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the kcCameraPivotParam from value " + value + ".");
            }

            return values()[value];
        }
    }
}