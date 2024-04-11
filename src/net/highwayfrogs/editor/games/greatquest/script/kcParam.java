package net.highwayfrogs.editor.games.greatquest.script;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.script.action.kcActionAISetGoal.kcGoalType;
import net.highwayfrogs.editor.games.greatquest.script.action.kcActionNumber.NumberOperation;
import net.highwayfrogs.editor.games.greatquest.script.effect.kcScriptEffectCamera.kcCameraPivotParam;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents 4 bytes with ambiguous interpretation. Used in Frogger TGQ.
 * Created by Kneesnap on 1/4/2021.
 */
@Getter
public class kcParam {
    private byte[] bytes;

    private static final String[] AXIS_ENUM_VALUES = {"X_AXIS", "Y_AXIS", "Z_AXIS"};
    private static final String[] INVENTORY_ITEM_ENUM_VALUES = {
            "ITEM_NONE", "ITEM_STONE_FIRE", "ITEM_STONE_ICE", "ITEM_STONE_SPEED", "ITEM_STONE_SHRINK", "ITEM_STONE_LIGHTNING",
            "ITEM_COIN_G", "ITEM_COIN_S", "ITEM_COIN_B", "ITEM_GEM_D", "ITEM_GEM_S", "ITEM_GEM_R", "ITEM_GEM_A",
            "ITEM_DOOR_KEY", "ITEM_CHEST_KEY", "ITEM_BONE", "ITEM_CHECKPOINT", "ITEM_SEED", "ITEM_EXTRA_BIN_19", "ITEM_EXTRA_BIN_20",
            "ITEM_MAP_00", "ITEM_MAP_01", "ITEM_MAP_02", "ITEM_MAP_03", "ITEM_MAP_04", "ITEM_MAP_05", "ITEM_MAP_06",
            "ITEM_MAP_07", "ITEM_MAP_08", "ITEM_MAP_09", "ITEM_MAP_10", "ITEM_MAP_11", "ITEM_MAP_12", "ITEM_MAP_13",
            "ITEM_MAP_14", "ITEM_MAP_15", "ITEM_MAP_16", "ITEM_MAP_17", "ITEM_MAP_18", "ITEM_MAP_19", "ITEM_MAP_20",
            "ITEM_MAP_21", "ITEM_MAP_22", "ITEM_MAP_23",
            "ITEM_HONEY_POT", "ITEM_ENGINE_ROOM_KEY", "ITEM_MAYOR_HOUSE_KEY",
            "ITEM_CLOVER_GATE_KEY", "ITEM_CLOVER", "ITEM_FAKE_CLOVER",
            "ITEM_FAIRY_TOWN_KEY_1", "ITEM_FAIRY_TOWN_KEY_2", "ITEM_FAIRY_TOWN_KEY_3",
            "ITEM_TREE_KEY", "ITEM_ENGINE_FUEL", "ITEM_SHRUNK_BONE_CRUSHER", "ITEM_ENGINE_KEY",
            "ITEM_STATUE", "ITEM_SQUARE_ARTIFACT", "ITEM_CIRCLE_ARTIFACT", "ITEM_TRIANGLE_ARTIFACT",
            "ITEM_CROWN", "ITEM_GRIM_BITE",
            "ITEM_RUBY_SHARD", "ITEM_RUBY_SPHERE", "ITEM_RUBY_TEARDROP"
    };

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
        return Utils.readIntFromBytes(this.bytes, 0);
    }

    /**
     * Gets the value as a float.
     */
    public float getAsFloat() {
        return Utils.readFloatFromBytes(this.bytes);
    }

    /**
     * Gets the value as a boolean.
     */
    public boolean getAsBoolean() {
        return getAsInteger() != 0;
    }

    /**
     * Sets the integer value.
     */
    public void setValue(int value) {
        this.bytes = Utils.toByteArray(value);
    }

    /**
     * Sets the float value.
     */
    public void setValue(float value) {
        this.bytes = Utils.writeFloatToBytes(value);
    }

    /**
     * Sets the boolean value.
     */
    public void setValue(boolean value) {
        setValue(value ? 1 : 0);
    }

    /**
     * Write the parameter as a display string.
     * @param builder   The builder to write to.
     * @param paramType The parameter type.
     */
    public void toString(StringBuilder builder, kcParamType paramType, kcScriptDisplaySettings settings) {
        switch (paramType) {
            case ANY:
                builder.append(kcScriptDisplaySettings.getHashDisplay(settings, getAsInteger(), false));
                break;
            case UNSIGNED_INT:
                builder.append(getAsInteger() & 0xFFFFFFFFL);
                break;
            case INT:
                builder.append(getAsInteger());
                break;
            case BOOLEAN:
                builder.append(getAsBoolean());
                break;
            case FLOAT:
                builder.append(getAsFloat());
                break;
            case AXIS:
                builder.append(getEnum(AXIS_ENUM_VALUES, "Axis"));
                break;
            case HASH:
                builder.append(kcScriptDisplaySettings.getHashDisplay(settings, getAsInteger(), true));
                break;
            case INVENTORY_ITEM:
                builder.append(getEnum(INVENTORY_ITEM_ENUM_VALUES, "InventoryItem"));
                break;
            case GOAL_TYPE:
                builder.append(getEnum(kcGoalType.values()));
                break;
            case NUMBER_OPERATION:
                builder.append(getEnum(NumberOperation.values()));
                break;
            case CAMERA_PIVOT_PARAM:
                builder.append(getEnum(kcCameraPivotParam.values()));
                break;
            case ATTACH_ID:
                builder.append(getEnum(kcAttachID.values()));
                break;
            default:
                throw new RuntimeException("Could not convert kcParamType " + paramType + " toString for value " + Utils.toByteString(this.bytes) + ".");
        }
    }

    private String getEnum(String[] values, String description) {
        int value = getAsInteger();
        return values == null || value < 0 || value >= values.length ? "<Invalid " + description + ": " + value + ">" : values[value];
    }

    private <E extends Enum<E>> String getEnum(E[] values) {
        int value = getAsInteger();

        if (values == null || value < 0 || value >= values.length) {
            String enumTypeName = (values != null && values.length > 0 && values[0] != null)
                    ? values[0].getClass().getSimpleName() : "Enum";

            return "<Invalid " + enumTypeName + ": " + value + ">";
        }

        return values[value].name();
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