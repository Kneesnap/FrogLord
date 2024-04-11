package net.highwayfrogs.editor.games.greatquest.script.effect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.greatquest.script.*;

/**
 * Represents a camera script effect.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcScriptEffectCamera extends kcScriptEffect {
    private final kcCameraEffect cameraEffect;
    private kcParam[] arguments;

    public kcScriptEffectCamera(int effectID) {
        super(kcScriptEffectType.CAMERA);
        this.cameraEffect = kcCameraEffect.getType(effectID, false);
    }

    @Override
    public int getEffectID() {
        return this.cameraEffect.ordinal();
    }

    @Override
    public void load(kcParamReader reader) {
        this.arguments = reader.getArguments();
        reader.setCurrentIndex(this.arguments.length);
    }

    @Override
    public void save(kcParamWriter writer) {
        for (int i = 0; i < this.arguments.length; i++)
            writer.write(this.arguments[i]);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        super.toString(builder, settings);
        kcAction.writeAction(builder, this.cameraEffect.name(), this.cameraEffect.getArguments(), this.arguments, settings);
    }

    @Getter
    @AllArgsConstructor
    public enum kcCameraEffect {
        ACTIVATE_CAMERA(kcArgument.make(kcParamType.INT, "transitionTicks")),
        DEACTIVATE_CAMERA(kcArgument.make(kcParamType.INT, "transitionTicks")),
        SET_CAMERA_TARGET(kcArgument.make(kcParamType.HASH, "targetEntity")),
        SET_CAMERA_PIVOT(kcArgument.make(kcParamType.HASH, "targetEntity")),
        SET_CAMERA_PARAM(kcArgument.make(kcParamType.CAMERA_PIVOT_PARAM, "param", kcParamType.FLOAT, "value"));

        private final kcArgument[] arguments;

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