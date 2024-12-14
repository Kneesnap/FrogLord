package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.generic.InventoryItem;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcRotationAxis;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionAISetGoal.kcAISystemGoalType;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionActivateSpecial.kcSpecialActivationMask;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionNumber.NumberOperation;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionPlaySound;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionSetAnimation;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffectCamera.kcCameraPivotParam;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.util.Arrays;

/**
 * Represents 4 bytes with ambiguous interpretation. Used in Frogger TGQ.
 * Created by Kneesnap on 1/4/2021.
 */
@Getter
public class kcParam {
    private byte[] bytes;

    public kcParam() {
        this(new byte[4]);
    }

    public kcParam(byte[] value) {
        Utils.verify(value != null && value.length == 4, "Invalid input array! (" + (value != null ? value.length : "null") + ")");
        this.bytes = value;
    }

    public kcParam(int number) {
        this();
        setValue(number);
    }

    /**
     * Gets the value as an integer.
     */
    public int getAsInteger() {
        return DataUtils.readIntFromBytes(this.bytes, 0);
    }

    /**
     * Gets the value as a float.
     */
    public float getAsFloat() {
        return DataUtils.readFloatFromBytes(this.bytes);
    }

    /**
     * Gets the value as a boolean.
     */
    public boolean getAsBoolean() {
        return getAsInteger() != 0;
    }

    /**
     * Gets the value as an inverted boolean.
     */
    public boolean getAsInvertedBoolean() {
        return getAsInteger() == 0;
    }

    /**
     * Gets this param as an enum value.
     * @param values the enum values to get the enum from
     * @return enumValue or null
     */
    @SuppressWarnings("unused")
    public <E extends Enum<E>> E getEnum(E[] values) {
        int value = getAsInteger();
        return value >= 0 && values.length > value ? values[value] : null;
    }

    /**
     * Get this kcParam as a sound path
     * @param instance the instance to resolve sound path from
     * @return soundPath
     */
    public String getAsSoundPath(GreatQuestInstance instance) {
        if (instance != null) {
            return instance.getFullSoundPath(getAsInteger() & ~kcActionPlaySound.BITMASK_STOP_SOUND);
        } else {
            return String.valueOf(getAsInteger());
        }
    }

    /**
     * Gets this kcParam as an angle in degrees.
     */
    public float getAsAngleInDegrees() {
        return (float) Math.toDegrees(getAsFloat());
    }

    /**
     * Sets the integer value.
     */
    public void setValue(kcParam otherParam) {
        if (otherParam == null)
            throw new NullPointerException("otherParam");

        this.bytes = Arrays.copyOf(otherParam.bytes, otherParam.bytes.length);
    }

    /**
     * Sets the integer value.
     */
    public void setValue(int value) {
        this.bytes = DataUtils.toByteArray(value);
    }

    /**
     * Sets the float value.
     */
    public void setValue(float value) {
        this.bytes = DataUtils.writeFloatToBytes(value);
    }

    /**
     * Sets the boolean value.
     */
    public void setValue(boolean value) {
        setValue(value ? 1 : 0);
    }

    /**
     * Sets the enum value.
     */
    public <TEnum extends Enum<TEnum>> void setValue(TEnum value) {
        if (value == null)
            throw new NullPointerException("value");

        setValue(value.ordinal());
    }

    /**
     * Write the parameter as a display string.
     * @param builder   The builder to write to.
     * @param paramType The parameter type.
     */
    public void toString(kcActionExecutor executor, StringBuilder builder, kcParamType paramType, kcScriptDisplaySettings settings) {
        StringNode tempNode = new StringNode();
        toConfigNode(executor, settings, tempNode, paramType);
        builder.append(tempNode.getAsStringLiteral());
    }

    /**
     * Write the parameter to a config value node.
     * @param executor the action executor
     * @param paramType The parameter type to write the value as.
     */
    public String getInvalidValueWarning(kcActionExecutor executor, kcParamType paramType) {
        int intValue = getAsInteger();
        float floatValue = getAsFloat();
        GreatQuestInstance instance = executor != null ? executor.getGameInstance() : null;

        switch (paramType) {
            case BOOLEAN:
            case INVERTED_BOOLEAN:
                if (intValue != 0 && intValue != 1)
                    return intValue + " cannot be represented as a boolean.";
                break;
            case FLOAT:
            case ANGLE:
            case TIMESTAMP_TICK:
                if (!Float.isFinite(floatValue))
                    return floatValue + " cannot be represented as a floating point value.";
                break;
            case AXIS:
                return getEnumWarning(kcRotationAxis.values());
            case INVENTORY_ITEM:
                return getEnumWarning(InventoryItem.values());
            case AI_GOAL_TYPE:
                return getEnumWarning(kcAISystemGoalType.values());
            case NUMBER_OPERATION:
                return getEnumWarning(NumberOperation.values());
            case CAMERA_PIVOT_PARAM:
                return getEnumWarning(kcCameraPivotParam.values());
            case ATTACH_ID:
                return getEnumWarning(kcAttachID.values());
            case SPECIAL_ACTIVATION_BIT_MASK:
                return getEnumWarning(kcSpecialActivationMask.values());
            case SOUND:
                if (intValue < 0 || instance == null || intValue >= instance.getNextFreeSoundId() || !instance.hasFullSoundPathFor(intValue))
                    return intValue + " does not appear to correspond to any named sound effects.";
                break;
            case MILLISECONDS:
                if (intValue < 0)
                    return intValue + " would indicate a time in the past. (Timestamps must be positive)";
                break;
            case ANIMATION_TICK:
                if (intValue != kcActionSetAnimation.DEFAULT_START_TIME && !Float.isFinite(floatValue))
                    return floatValue + " cannot be represented as an animation tick value.";
                break;
            case ALARM_ID:
                if (intValue < 0 || intValue >= Constants.BITS_PER_INTEGER)
                    return intValue + " is not a valid alarm ID.";
                break;
            case VARIABLE_ID:
                int variableId = getAsInteger();
                if (variableId < 0 || variableId >= 8)
                    return variableId + " is not a valid variable ID.";

                break;
            case BONE_TAG:
                kcActorBaseDesc actorDesc = executor != null ? executor.getExecutingActorBaseDescription() : null;
                kcCResourceSkeleton skeleton = actorDesc != null ? actorDesc.getSkeleton() : null;
                if (skeleton != null) {
                    kcNode bone = skeleton.getNodeByTag(intValue);
                    if (bone == null)
                        return intValue + " is not a valid bone tag for the skeleton '" + skeleton.getName() + "'.";
                } else if (intValue != 0) {
                    return intValue + " is not a valid bone tag when the entity (" + (actorDesc != null ? actorDesc.getResource().getName() : "null") + ") has no skeleton!";
                }

                break;
        }

        // No warning.
        return null;
    }

    /**
     * Reads the parameter value from a config value node.
     * @param instance the game instance which the script belongs to
     * @param node The node to load the value from.
     * @param paramType The parameter type to resolve the value as.
     */
    public void fromConfigNode(kcActionExecutor executor, GreatQuestInstance instance, StringNode node, kcParamType paramType) {
        switch (paramType) {
            case HEX_INTEGER:
            case HASH:
            case HASH_NULL_IS_ZERO:
                if (!node.isNull() && NumberUtils.isHexInteger(node.getAsString())) {
                    setValue(NumberUtils.parseHexInteger(node.getAsString()));
                } else if (paramType == kcParamType.HASH_NULL_IS_ZERO) {
                    setValue(node.isNull() ? 0 : GreatQuestUtils.hash(node.getAsString()));
                } else {
                    setValue(node.isNull() ? -1 : GreatQuestUtils.hash(node.getAsString()));
                }

                break;
            case UNSIGNED_INT:
                setValue(node.getAsUnsignedInteger());
                break;
            case INT:
            case ALARM_ID:
            case VARIABLE_ID:
                setValue(node.getAsInteger());
                break;
            case BOOLEAN:
                setValue(node.getAsBoolean());
                break;
            case INVERTED_BOOLEAN:
                setValue(!node.getAsBoolean());
                break;
            case FLOAT:
                setValue(node.getAsFloat());
                break;
            case AXIS:
                setEnumFromNode(kcRotationAxis.class, node);
                break;
            case INVENTORY_ITEM:
                setEnumFromNode(InventoryItem.class, node);
                break;
            case AI_GOAL_TYPE:
                setEnumFromNode(kcAISystemGoalType.class, node);
                break;
            case NUMBER_OPERATION:
                setEnumFromNode(NumberOperation.class, node);
                break;
            case CAMERA_PIVOT_PARAM:
                setEnumFromNode(kcCameraPivotParam.class, node);
                break;
            case ATTACH_ID:
                setEnumFromNode(kcAttachID.class, node);
                break;
            case SPECIAL_ACTIVATION_BIT_MASK:
                setEnumFromNode(kcSpecialActivationMask.class, node);
                break;
            case SOUND:
                int sfxId = instance.getSfxIdFromFullSoundPath(node.getAsString());
                setValue(sfxId);
                break;
            case MILLISECONDS:
                setValue(Math.round(node.getAsFloat() * 1000));
                break;
            case ANIMATION_TICK:
                if (Math.abs(node.getAsFloat()) <= .00001) {
                    setValue(kcActionSetAnimation.DEFAULT_START_TIME);
                } else {
                    setValue(Math.round(node.getAsFloat() * GreatQuestModelMesh.TICKS_PER_SECOND));
                }
                break;
            case TIMESTAMP_TICK:
                setValue(Math.round(node.getAsFloat() * GreatQuestModelMesh.TICKS_PER_SECOND));
                break;
            case ANGLE:
                setValue((float) Math.toRadians(node.getAsFloat()));
                break;
            case BONE_TAG:
                kcActorBaseDesc actorDesc = executor != null ? executor.getExecutingActorBaseDescription() : null;
                kcCResourceSkeleton skeleton = actorDesc != null ? actorDesc.getSkeleton() : null;
                if (node.isNull()) {
                    setValue(0); // No bone!
                } else if (skeleton != null) {
                    String boneName = node.getAsString();
                    kcNode bone = skeleton.getNodeByName(boneName);
                    if (bone == null && NumberUtils.isInteger(boneName))
                        bone = skeleton.getNodeByTag(Integer.parseInt(boneName));

                    if (bone != null) {
                        setValue(bone.getTag());
                    } else if (NumberUtils.isHexInteger(boneName)) {
                        setValue(node.getAsInteger());
                    } else {
                        int newHash = GreatQuestUtils.hash(boneName);
                        skeleton.getLogger().warning("Could not resolve a bone named '" + boneName + "', storing its hash so it can still be identified as " + NumberUtils.to0PrefixedHexString(newHash) + ".");
                        setValue(newHash);
                    }
                } else {
                    setValue(node.getAsInteger());
                }
                break;
            default:
                throw new RuntimeException("Could not convert kcParamType " + paramType + " toString for value " + DataUtils.toByteString(this.bytes) + ".");
        }
    }

    /**
     * Write the parameter to a config value node.
     * @param settings the script display settings
     * @param node The node to save this value to.
     * @param paramType The parameter type to write the value as.
     */
    public void toConfigNode(kcActionExecutor executor, kcScriptDisplaySettings settings, StringNode node, kcParamType paramType) {
        switch (paramType) {
            case HEX_INTEGER:
                node.setAsString(NumberUtils.to0PrefixedHexString(getAsInteger()), false);
                break;
            case HASH:
            case HASH_NULL_IS_ZERO:
                kcScriptDisplaySettings.applyGqsSyntaxHashDisplay(node, settings, getAsInteger());
                break;
            case UNSIGNED_INT:
                node.setAsUnsignedInteger(getAsInteger());
                break;
            case INT:
            case ALARM_ID:
            case VARIABLE_ID:
                node.setAsInteger(getAsInteger());
                break;
            case BOOLEAN:
                node.setAsBoolean(getAsBoolean());
                break;
            case INVERTED_BOOLEAN:
                node.setAsBoolean(getAsInvertedBoolean());
                break;
            case FLOAT:
                node.setAsFloat(getAsFloat());
                break;
            case AXIS:
                setNodeToEnumName(node, kcRotationAxis.values());
                break;
            case INVENTORY_ITEM:
                setNodeToEnumName(node, InventoryItem.values());
                break;
            case AI_GOAL_TYPE:
                setNodeToEnumName(node, kcAISystemGoalType.values());
                break;
            case NUMBER_OPERATION:
                setNodeToEnumName(node, NumberOperation.values());
                break;
            case CAMERA_PIVOT_PARAM:
                setNodeToEnumName(node, kcCameraPivotParam.values());
                break;
            case ATTACH_ID:
                setNodeToEnumName(node, kcAttachID.values());
                break;
            case SPECIAL_ACTIVATION_BIT_MASK:
                setNodeToEnumName(node, kcSpecialActivationMask.values());
                break;
            case SOUND:
                String soundPath = getAsSoundPath(settings != null ? settings.getGameInstance() : null);
                node.setAsString(soundPath, !NumberUtils.isInteger(soundPath));
                break;
            case MILLISECONDS:
                node.setAsFloat(getAsInteger() / 1000F);
                break;
            case ANIMATION_TICK:
                if (getAsInteger() == kcActionSetAnimation.DEFAULT_START_TIME) {
                    node.setAsFloat(0);
                } else {
                    node.setAsFloat((float) getAsInteger() / GreatQuestModelMesh.TICKS_PER_SECOND);
                }
                break;
            case TIMESTAMP_TICK:
                node.setAsFloat((float) getAsInteger() / GreatQuestModelMesh.TICKS_PER_SECOND);
                break;
            case ANGLE:
                node.setAsFloat(getAsAngleInDegrees());
                break;
            case BONE_TAG:
                kcActorBaseDesc actorDesc = executor != null ? executor.getExecutingActorBaseDescription() : null;
                kcCResourceSkeleton skeleton = actorDesc != null ? actorDesc.getSkeleton() : null;
                if (skeleton != null) {
                    int boneTag = getAsInteger();
                    kcNode bone = skeleton.getNodeByTag(boneTag);

                    if (bone != null) {
                        node.setAsString(bone.getName(), true);
                    } else if (boneTag <= kcCResourceSkeleton.MAXIMUM_BONE_COUNT) {
                        node.setAsInteger(boneTag);
                    } else {
                        node.setAsString(NumberUtils.to0PrefixedHexString(boneTag), false);
                    }
                } else {
                    node.setAsInteger(getAsInteger());
                }
                break;
            default:
                throw new RuntimeException("Could not convert kcParamType " + paramType + " toString for value " + DataUtils.toByteString(this.bytes) + ".");
        }
    }

    private <E extends Enum<E>> String getEnumWarning(E[] values) {
        int value = getAsInteger();

        if (values == null || value < 0 || value >= values.length) {
            String enumTypeName = (values != null && values.length > 0 && values[0] != null)
                    ? values[0].getClass().getSimpleName() : "Enum";

            return value + " is not representable as a(n) " + enumTypeName + ".";
        }

        return null;
    }

    private <E extends Enum<E>> void setNodeToEnumName(StringNode node, E[] values) {
        int value = getAsInteger();

        if (values == null || value < 0 || value >= values.length) {
            String enumTypeName = (values != null && values.length > 0 && values[0] != null)
                    ? values[0].getClass().getSimpleName() : "Enum";

            node.setAsString("<Invalid " + enumTypeName + ": " + value + ">", true);
            return;
        }

        node.setAsEnum(values[value]);
    }

    private <E extends Enum<E>> void setEnumFromNode(Class<E> enumClass, StringNode node) {
        String input = node.getAsString();

        if (input.startsWith("<Invalid")) {
            String strippedInput = input.split(": ")[1].split(">")[0];
            if (!NumberUtils.isInteger(strippedInput))
                throw new RuntimeException("Could not interpret '" + input + "' as an enum value.");

            setValue(Integer.parseInt(strippedInput));
            return;
        }

        E enumValue = node.getAsEnum(enumClass);
        if (enumValue == null)
            throw new RuntimeException("Could not interpret '" + input + "' as a(n) " + enumClass.getSimpleName() + " value.");

        setValue(enumValue);
    }

    /**
     * Reads a kcParam from a DataReader.
     * @param reader The reader to read from.
     * @return loadedParam
     */
    public static kcParam readParam(DataReader reader) {
        return new kcParam(reader.readBytes(4));
    }

    /**
     * Writes a kcParam to the data writer.
     * @param writer the writer to write to.
     * @param param  The parameter to write.
     */
    public static void writeParam(DataWriter writer, kcParam param) {
        if (param != null && param.getBytes() != null && param.getBytes().length == 4) {
            writer.writeBytes(param.getBytes());
        } else { // Write empty.
            writer.writeNull(4);
        }
    }
}