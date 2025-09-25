package net.highwayfrogs.editor.games.sony.medievil;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.GamePlatform;
import net.highwayfrogs.editor.games.generic.GameUtils;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilScripter.BitFlagEnum;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilScripter.VelocityMode;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.reader.FileSource;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Kneesnap on 9/3/2025.
 */
public class MediEvilIntermissionDecompiler {

    public static void main(String[] args) throws Throwable {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter the path to the executable file/overlay: ");
        File file = new File(scanner.nextLine());
        if (!file.isFile() || !file.exists()) {
            System.out.println("Executable file not found.");
            return;
        }

        System.out.print("What is the address of the intermission nodes (In the file): ");
        String addressStr = scanner.nextLine();
        if (!NumberUtils.isHexInteger(addressStr)) {
            System.out.print("Invalid hex integer '" + addressStr + "'.");
            return;
        }

        int address = Integer.parseInt(addressStr.substring(2), 16);
        DataReader reader = new DataReader(new FileSource(file));
        reader.setIndex(address);
        System.out.println();
        System.out.println("MR_LONG gal##?????Nodes[] =");
        System.out.println("{");

        for (IntermissionNode block : readScript(reader))
            System.out.println("\t" + block);

        System.out.println("\tDEF_ITERMINATE");
        System.out.println("};");
        System.out.println();
    }

    public static List<IntermissionNode> readScript(DataReader reader) {
        List<IntermissionNode> nodes = new ArrayList<>();

        IntermissionNode newNode;
        while ((newNode = readNode(reader)) != null)
            nodes.add(newNode);

        return nodes;
    }

    private static IntermissionNode readNode(DataReader reader) {
        int startIndex = reader.getIndex();

        int nodeTypeId = reader.readInt();
        IntermissionNodeType nodeType = IntermissionNodeType.getNodeTypeById(nodeTypeId);
        if (nodeType == IntermissionNodeType.NULL) {
            reader.setIndex(startIndex);
            return null; // This is the terminator signal.
        }

        if (nodeType == null)
            throw new RuntimeException("Unknown nodeTypeId ID: " + nodeTypeId + ".");

        IntermissionNode newNode;
        if (nodeType == IntermissionNodeType.TIMER) {
            reader.setIndex(reader.getIndex() + Constants.INTEGER_SIZE + (2 * Constants.SHORT_SIZE));
            int timeIntermissionId = reader.readInt();
            IntermissionNodeTimeType timeType = IntermissionNodeTimeType.values()[timeIntermissionId];
            newNode = timeType.createNode();
        } else {
            newNode = nodeType.createNode();
        }

        reader.setIndex(startIndex);
        if (newNode == null)
            return null;

        // Reset reader.
        newNode.load(reader);
        return newNode;
    }

    private static void writeFixedVector(StringBuilder builder, short x, short y, short z) {
        builder.append(x).append(", ")
                .append(y).append(", ")
                .append(z);
    }

    public static <E extends Enum<E>> String getEnumName(E[] enumValues, int ordinal) {
        return ordinal >= 0 && ordinal < enumValues.length ? enumValues[ordinal].name() : "/*[TODO: BAD ENUM]*/ " + NumberUtils.toHexString(ordinal);
    }

    public static String getBitFlagsString(int bitValue, BitFlagEnum[] values) {
        StringBuilder builder = new StringBuilder();
        appendBitFlagsString(builder, bitValue, values);
        return builder.toString();
    }

    public static void appendBitFlagsString(StringBuilder builder, int bitValue, BitFlagEnum[] values) {
        int numberAdded = 0;

        for (int i = values.length - 1; i >= 0; i--) {
            BitFlagEnum enumValue = values[i];
            if (((long) bitValue & enumValue.getBitMask()) == enumValue.getBitMask()) {
                bitValue = (int) ((long) bitValue ^ enumValue.getBitMask());
                if (numberAdded++ > 0)
                    builder.append(" | ");

                builder.append(enumValue.name());
            }
        }

        if (bitValue != 0) {
            if (numberAdded++ > 0)
                builder.append(" | ");

            builder.append("0x").append(Integer.toHexString(bitValue).toUpperCase());
        }

        if (numberAdded == 0)
            builder.append("NULL");
    }

    public enum IntermissionBitFlag implements BitFlagEnum { // TODO: TOSS?
        IGLOB_NO_ENT_FREEZE_F,                // If currently active entities should NOT be frozen.
        IGLOB_CREATE_FAKE_CAM_TARGET_F,        // Set if dummy entity should be created for use as a camera target.
        IGLOB_CAM_START_AT_CURRENT_POS_F,    // Set if camera should start intermission at current position (use with CREATE_FAKE_CAM_TARGET).
        IGLOB_CAM_LOOK_AT_REL_MAT_F,        // Set if camera should start looking at the intermission's relative matrix.
        IGLOB_PROTECT_DAN_F,                // Prevents Dan from being targeted or damaged during the intermission
        IGLOB_PLAYER_CONTROL_OVERRIDE_F,    // Prevent player from moving Dan.  Overrides the camera plugin setting.
        IGLOB_NO_XA_MUSIC_F,                // Set if intermission should stop/restart XA music.

        // Dynamic...

        IGLOB_USE_FAKE_CAM_TARGET_F,        // Set if fake entity should be currently acting as the camera target.
        IGLOB_PROGRESS_SUSPENDED_F,            // Set when intermission update is suspended by TOGGLE_SUSPEND node.
        IGLOB_WAITING_FOR_USER_VAR_F,        // Set when all intermission processing (aside from call to user update function) is waiting for user var to equal a given value.
    }

    @Getter
    public abstract static class IntermissionNode implements IBinarySerializable {
        @NonNull private final IntermissionNodeType type;
        private int flags;
        private short timeCode;

        public IntermissionNode(IntermissionNodeType type) {
            this.type = type;
        }

        @Override
        public final void load(DataReader reader) {
            int startIndex = reader.getIndex();

            int typeId = reader.readInt();
            if (typeId != this.type.ordinal())
                throw new RuntimeException("The node type ID was mismatched! (Expected: " + this.type.ordinal() + ", Got: " + typeId + ")");

            this.flags = reader.readInt();
            if (this.flags != 0) // This should probably be a warning, not an exception.
                throw new RuntimeException("The node flags were not zero! (Got: " + this.flags + ")");

            this.timeCode = reader.readShort();
            int expectedSize = reader.readShort();

            // Load additional data.
            loadExtraData(reader);

            // Validate correct number of bytes were read.
            int bytesRead = reader.getIndex() - startIndex;
            if (bytesRead != expectedSize)
                throw new RuntimeException("The number of bytes read for " + getClass().getSimpleName() + "/" + this + " was " + bytesRead + ", when it was expected to be " + expectedSize + ".");
        }

        /**
         * Loads extra data specific to this node.
         * @param reader the reader to load the data from
         */
        protected abstract void loadExtraData(DataReader reader);

        @Override
        public final void save(DataWriter writer) {
            int startIndex = writer.getIndex();
            writer.writeInt(this.type.ordinal());
            writer.writeInt(this.flags);
            writer.writeShort(this.timeCode);
            int sizeIndex = writer.getIndex();
            writer.writeUnsignedShort(0);

            // Save data.
            saveExtraData(writer);

            // Save data size.
            int fullDataSize = writer.getIndex() - startIndex;
            writer.jumpTemp(sizeIndex);
            writer.writeUnsignedShort(fullDataSize);
            writer.jumpReturn();
        }

        /**
         * Saves extra data specific to this node.
         * @param writer the writer to save the data to
         */
        protected abstract void saveExtraData(DataWriter writer);

        /**
         * Get a string containing a textual form of the bit flags.
         */
        protected String getFlagsString() { // TODO: TOSS?
            return getBitFlagsString(this.flags, IntermissionBitFlag.values());
        }

        /**
         * Write this event block as a valid string to the builder.
         * @param builder the builder to write the event block to
         */
        public abstract void writeAsText(StringBuilder builder);

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            this.writeAsText(builder);
            return builder.toString();
        }
    }

    @Getter
    public enum IntermissionNodeType {
        CAMERA_POSITION,
        CAMERA_FOCUS,
        VIEW_CONSTANTS,
        ENT0,
        ENT1,
        ENT2,
        ENT3,
        TIMER,
        NULL;

        /**
         * Creates an intermission node represented by this type.
         */
        public IntermissionNode createNode() {
            switch (this) {
                case CAMERA_POSITION:
                    return new IntermissionNodeCameraPosition();
                case CAMERA_FOCUS:
                    return new IntermissionNodeCameraFocus();
                case VIEW_CONSTANTS:
                    return new IntermissionNodeViewConstants();
                case ENT0:
                    return new IntermissionNodeEntityPosition(0);
                case ENT1:
                    return new IntermissionNodeEntityPosition(1);
                case ENT2:
                    return new IntermissionNodeEntityPosition(2);
                case ENT3:
                    return new IntermissionNodeEntityPosition(3);
                case TIMER:
                    throw new RuntimeException("TIMER IntermissionNodeTypes can only be created via their own custom type system.");
                case NULL:
                    return null;
                default:
                    throw new RuntimeException("IntermissionNode type: " + this + " not supported.");
            }
        }

        /**
         * Gets the node type by its ID, if the provided ID is valid
         * @param id the id of the node type
         * @return nodeType or null
         */
        public static IntermissionNodeType getNodeTypeById(int id) {
            return id >= 0 && id < values().length ? values()[id] : null;
        }
    }

    @Getter
    public static class IntermissionNodeCameraPosition extends IntermissionNode {
        private int velocityMode;
        private short x;
        private short y;
        private short z;
        private short zRot;

        public IntermissionNodeCameraPosition() {
            super(IntermissionNodeType.CAMERA_POSITION);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("DEF_IMOTION_POS(")
                    .append(this.getTimeCode()).append(", ");
            writeFixedVector(builder, this.x, this.y, this.z);
            builder.append(", ").append(this.zRot).append(", ")
                    .append(getEnumName(VelocityMode.values(), this.velocityMode)).append(")");
        }

        @Override
        protected void loadExtraData(DataReader reader) {
            this.velocityMode = reader.readInt();
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            this.zRot = reader.readShort();
        }

        @Override
        protected void saveExtraData(DataWriter writer) {
            writer.writeInt(this.velocityMode);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort(this.zRot);
        }
    }

    @Getter
    public static class IntermissionNodeCameraFocus extends IntermissionNode {
        private int velocityMode;
        private short x;
        private short y;
        private short z;

        public IntermissionNodeCameraFocus() {
            super(IntermissionNodeType.CAMERA_FOCUS);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("DEF_IMOTION_FOCUS(")
                    .append(this.getTimeCode()).append(", ");
            writeFixedVector(builder, this.x, this.y, this.z);
            builder.append(", ")
                    .append(getEnumName(VelocityMode.values(), this.velocityMode)).append(")");
        }

        @Override
        protected void loadExtraData(DataReader reader) {
            this.velocityMode = reader.readInt();
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE);
        }

        @Override
        protected void saveExtraData(DataWriter writer) {
            writer.writeInt(this.velocityMode);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort((short) 0);
        }
    }

    @Getter
    public static class IntermissionNodeViewConstants extends IntermissionNode {
        private int velocityMode;
        private short perspective;
        private short viewDistance;
        private short fogRatio;

        public IntermissionNodeViewConstants() {
            super(IntermissionNodeType.VIEW_CONSTANTS);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("DEF_IMOTION_VIEW_CONST(")
                    .append(this.getTimeCode()).append(", ")
                    .append(this.perspective).append(", ")
                    .append(this.viewDistance).append(", ")
                    .append(this.fogRatio).append(", ")
                    .append(getEnumName(VelocityMode.values(), this.velocityMode)).append(")");
        }

        @Override
        protected void loadExtraData(DataReader reader) {
            this.velocityMode = reader.readInt();
            this.perspective = reader.readShort();
            this.viewDistance = reader.readShort();
            this.fogRatio = reader.readShort();
            reader.skipBytesRequireEmpty(Constants.SHORT_SIZE);
        }

        @Override
        protected void saveExtraData(DataWriter writer) {
            writer.writeInt(this.velocityMode);
            writer.writeShort(this.perspective);
            writer.writeShort(this.viewDistance);
            writer.writeShort(this.fogRatio);
            writer.writeShort((short) 0);
        }
    }

    @Getter
    public static class IntermissionNodeEntityPosition extends IntermissionNode {
        private final int entitySlotId;
        private int velocityMode;
        private short x;
        private short y;
        private short z;
        private short yRot;

        private static final IntermissionNodeType[] NODE_TYPES = {
                IntermissionNodeType.ENT0,
                IntermissionNodeType.ENT1,
                IntermissionNodeType.ENT2,
                IntermissionNodeType.ENT3
        };

        public IntermissionNodeEntityPosition(int slotId) {
            super(NODE_TYPES[slotId]);
            this.entitySlotId = slotId;
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            builder.append("DEF_IMOTION_ENT_POS(")
                    .append(this.getTimeCode()).append(", ")
                    .append(this.entitySlotId).append(", ");
            writeFixedVector(builder, this.x, this.y, this.z);
            builder.append(", ").append(this.yRot)
                    .append(getEnumName(VelocityMode.values(), this.velocityMode)).append(")");
        }

        @Override
        protected void loadExtraData(DataReader reader) {
            this.velocityMode = reader.readInt();
            this.x = reader.readShort();
            this.y = reader.readShort();
            this.z = reader.readShort();
            this.yRot = reader.readShort();
        }

        @Override
        protected void saveExtraData(DataWriter writer) {
            writer.writeInt(this.velocityMode);
            writer.writeShort(this.x);
            writer.writeShort(this.y);
            writer.writeShort(this.z);
            writer.writeShort(this.yRot);
        }
    }

    @Getter
    public enum IntermissionNodeTimeType {
        ITIME_SUSPEND_TOGGLE(),
        ITIME_FAKE_CAM_TARGET_TOGGLE(),
        ITIME_PLAYER_CONTROL_TOGGLE(),
        ITIME_CALL_FUNC("function", IntermissionValueType.HEX_INTEGER, "Function address.",
                "data", IntermissionValueType.AMBIGUOUS, "LONG data param for function call."),
        ITIME_FORCE_CAM_POS("commandType", IntermissionValueType.SKIPPED, "Skipped.",
                "position", IntermissionValueType.POSITION, "x, y, z.",
                "zRot", IntermissionValueType.SHIFTED_SHORT, "zrot"),
        ITIME_FORCE_VALUES("perspective", IntermissionValueType.SHIFTED_SHORT, "Perspective.",
                "viewDistance", IntermissionValueType.SHIFTED_SHORT, "View distance.",
                "fogRatio", IntermissionValueType.SHIFTED_SHORT, "Fog ratio."), // Unused.

        ITNODE_CREATE_ENT("slotId", IntermissionValueType.INTEGER, "Slot Id.",
                "species", IntermissionValueType.SPECIES, "Species.",
                "subSpecies", IntermissionValueType.SUBSPECIES, "Subspecies.",
                "position", IntermissionValueType.POSITION, "x, y, z.",
                "yRot", IntermissionValueType.INTEGER, "y Rot."),
        ITIME_GET_ENT_PLAYER("slotId", IntermissionValueType.INTEGER, "Entity slot ID."),
        ITIME_GET_ENT_FROM_CAM_TARGET("slotId", IntermissionValueType.INTEGER, "Entity slot ID."),
        ITIME_GET_ENT_FROM_GID("slotId", IntermissionValueType.INTEGER, "Entity slot ID.",
                "gid", IntermissionValueType.INTEGER, "Global ID of entity."),
        ITIME_UNLINK_ENT("slotId", IntermissionValueType.INTEGER, "Entity slot ID."),
        ITIME_SET_ENT_AS_CAM_TARGET("slotId", IntermissionValueType.INTEGER, "Entity slot ID."),
        ITIME_SET_ENT_FOR_MOVEMENT_CONTROL("slotId", IntermissionValueType.INTEGER, "Entity slot ID."),

        ITIME_SET_ENT_MOVE("slotId", IntermissionValueType.INTEGER, "Entity slot ID",
                "moveReq", IntermissionValueType.ENTITY_MOVE_FLAGS, "Move request type."),
        ITIME_SET_ENT_ANIM("slotId", IntermissionValueType.INTEGER, "Entity slot ID",
                "animAction", IntermissionValueType.ANIM_ACTION, "Anim action."),
        ITIME_SET_ENT_MOVE_AND_ANIM("slotId", IntermissionValueType.INTEGER, "Entity slot ID",
                "moveReq", IntermissionValueType.ENTITY_MOVE_FLAGS, "Move request type.",
                "animAction", IntermissionValueType.ANIM_ACTION, "Anim action."),
        ITIME_SET_ENT_MOVE_TARGET("slotId", IntermissionValueType.INTEGER, "Entity slot ID",
                "position", IntermissionValueType.POSITION, "x, y, z."),
        ITIME_SET_ENT_MOVE_DYNAMIC_TARGET("slotId", IntermissionValueType.INTEGER, "Entity slot ID",
                "targetSlotId", IntermissionValueType.INTEGER, "Other entity slot ID."), // Doesn't seem to be used.
        ITIME_SET_ENT_BEHAVIOUR("slotId", IntermissionValueType.INTEGER, "Entity slot ID",
                "behavior", IntermissionValueType.ENTITY_BEHAVIOR, "Behaviour mode."),

        ITIME_SET_EVENT("eventId", IntermissionValueType.EVENT_ID, "Event number.",
                "state", IntermissionValueType.INTEGER, "State."),
        ITIME_TRIGGER_HELP("helpId", IntermissionValueType.HELP, null),
        ITIME_PLAY_SOUND("sound", IntermissionValueType.SOUND_NAME, "Sound effect."),
        ITIME_PLAY_SOUND_3D("sound", IntermissionValueType.SOUND_NAME, "Sound effect.",
                "position", IntermissionValueType.POSITION, "x, y, z"), // Unused in 0.31, the values aren't included.
        ITIME_WAIT_FOR_USER_VAR("value", IntermissionValueType.INTEGER, "Value to wait for"), // Seems to indeed wait for an integer.
        ITIME_SET_REL_MAT("matrix", IntermissionValueType.INTEGER, "Slot ID (-1 = world)"); // Only used in zl.c in 0.31. Seems to be a slot ID.

        private final IntermissionNodeArgument[] lazyArguments;

        IntermissionNodeTimeType() {
            this.lazyArguments = new IntermissionNodeArgument[0];
        }

        IntermissionNodeTimeType(String arg1Name, IntermissionValueType arg1Type, String arg1Comment) {
            this.lazyArguments = new IntermissionNodeArgument[] {
                    new IntermissionNodeArgument(arg1Name, arg1Type, arg1Comment),
            };
        }

        IntermissionNodeTimeType(String arg1Name, IntermissionValueType arg1Type, String arg1Comment,
                                 String arg2Name, IntermissionValueType arg2Type, String arg2Comment) {
            this.lazyArguments = new IntermissionNodeArgument[] {
                    new IntermissionNodeArgument(arg1Name, arg1Type, arg1Comment),
                    new IntermissionNodeArgument(arg2Name, arg2Type, arg2Comment),
            };
        }

        IntermissionNodeTimeType(String arg1Name, IntermissionValueType arg1Type, String arg1Comment,
                                 String arg2Name, IntermissionValueType arg2Type, String arg2Comment,
                                 String arg3Name, IntermissionValueType arg3Type, String arg3Comment) {
            this.lazyArguments = new IntermissionNodeArgument[] {
                    new IntermissionNodeArgument(arg1Name, arg1Type, arg1Comment),
                    new IntermissionNodeArgument(arg2Name, arg2Type, arg2Comment),
                    new IntermissionNodeArgument(arg3Name, arg3Type, arg3Comment),
            };
        }

        IntermissionNodeTimeType(String arg1Name, IntermissionValueType arg1Type, String arg1Comment,
                                 String arg2Name, IntermissionValueType arg2Type, String arg2Comment,
                                 String arg3Name, IntermissionValueType arg3Type, String arg3Comment,
                                 String arg4Name, IntermissionValueType arg4Type, String arg4Comment,
                                 String arg5Name, IntermissionValueType arg5Type, String arg5Comment) {
            this.lazyArguments = new IntermissionNodeArgument[] {
                    new IntermissionNodeArgument(arg1Name, arg1Type, arg1Comment),
                    new IntermissionNodeArgument(arg2Name, arg2Type, arg2Comment),
                    new IntermissionNodeArgument(arg3Name, arg3Type, arg3Comment),
                    new IntermissionNodeArgument(arg4Name, arg4Type, arg4Comment),
                    new IntermissionNodeArgument(arg5Name, arg5Type, arg5Comment),
            };
        }

        /**
         * Creates a new node represented by the given type.
         */
        public IntermissionTimeNode createNode() {
            return new IntermissionTimeBasicNode(this);
        }
    }


    @Getter
    public abstract static class IntermissionTimeNode extends IntermissionNode {
        private final IntermissionNodeTimeType timeNodeType;

        public IntermissionTimeNode(IntermissionNodeTimeType timeNodeType) {
            super(IntermissionNodeType.TIMER);
            this.timeNodeType = timeNodeType;
        }

        @Override
        protected void loadExtraData(DataReader reader) {
            int timeNodeTypeId = reader.readInt();
            if (timeNodeTypeId != this.timeNodeType.ordinal())
                throw new RuntimeException("Expected timeNodeType: " + this.timeNodeType + "/" + this.timeNodeType.ordinal() + ", but got " + timeNodeTypeId + " instead.");
        }

        @Override
        protected void saveExtraData(DataWriter writer) {
            writer.writeInt(this.timeNodeType.ordinal());
        }
    }

    @Getter
    public static class IntermissionTimeBasicNode extends IntermissionTimeNode {
        private final List<IntermissionNodeValue> values = new ArrayList<>();

        public IntermissionTimeBasicNode(IntermissionNodeTimeType timeNodeType) {
            super(timeNodeType);
        }

        @Override
        protected void loadExtraData(DataReader reader) {
            super.loadExtraData(reader);

            this.values.clear();
            IntermissionNodeArgument[] arguments = getTimeNodeType().getLazyArguments();
            for (int i = 0; i < arguments.length; i++) {
                IntermissionNodeValue newValue = new IntermissionNodeValue(arguments[i]);
                newValue.load(reader);
                this.values.add(newValue);
            }
        }

        @Override
        protected void saveExtraData(DataWriter writer) {
            super.saveExtraData(writer);
            for (int i = 0; i < this.values.size(); i++)
                this.values.get(i).save(writer);
        }

        @Override
        public void writeAsText(StringBuilder builder) {
            int targetLength = 1; // TODO: !

            String baseName = getTimeNodeType().name();

            // There's a special case for this and this alone.
            if (getTimeNodeType() == IntermissionNodeTimeType.ITIME_FORCE_CAM_POS && this.values.size() > 0 && this.values.get(0).values[0] == 1)
                baseName = "ITIME_FORCE_CAM_FOCUS_POS";

            builder.append("DEF_").append(baseName).append("(");

            builder.append(getTimeCode());


            // TODO: FORMAT PROPERLY
            for (int i = 0; i < this.values.size(); i++) {
                IntermissionNodeValue value = this.values.get(i);
                if (value.getArgumentDefinition().getValueType() == IntermissionValueType.SKIPPED)
                    continue;

                builder.append(", ");
                value.writeString(builder);
                // TODO: Each has a separate line, and a comment.
            }

            builder.append(")");
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum IntermissionValueType {
        INTEGER(1),
        SHIFTED_SHORT(1),
        SOUND_NAME(1),
        EVENT_ID(1),
        HEX_INTEGER(1),
        AMBIGUOUS(1),
        SPECIES(1),
        SUBSPECIES(1),
        POSITION(3),
        ENTITY_MOVE_FLAGS(1),
        ANIM_ACTION(1),
        ENTITY_BEHAVIOR(1),
        HELP(1),
        SKIPPED(1);

        private final int valuesHandled;
    }

    @RequiredArgsConstructor
    public static class IntermissionNodeValue implements IBinarySerializable {
        @Getter @NonNull private final IntermissionNodeArgument argumentDefinition;
        private int[] values;

        @Override
        public void load(DataReader reader) {
            this.values = new int[this.argumentDefinition.getValueType().getValuesHandled()];
            for (int i = 0; i < this.values.length; i++)
                this.values[i] = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            for (int i = 0; i < this.values.length; i++)
                writer.writeInt(this.values[i]);
        }

        /**
         * Writes the value as a string to the builder
         * @param builder the builder to write the string to
         */
        public void writeString(StringBuilder builder) {
            switch (this.argumentDefinition.getValueType()) {
                case SKIPPED:
                    return;
                case INTEGER:
                case EVENT_ID:
                case SOUND_NAME:
                case POSITION:
                case SUBSPECIES:
                    for (int i = 0; i < this.values.length; i++) {
                        if (i > 0)
                            builder.append(", ");
                        builder.append(this.values[i]);
                    }
                    break;
                case SHIFTED_SHORT:
                    builder.append((short) (this.values[0] >>> 16));
                    break;
                case HEX_INTEGER:
                    builder.append("0x").append(NumberUtils.to0PrefixedHexString(this.values[0]));
                    break;
                case AMBIGUOUS:
                    if (GameUtils.isValidLookingPointer(GamePlatform.PLAYSTATION, this.values[0])) {
                        builder.append("0x").append(NumberUtils.to0PrefixedHexString(this.values[0]));
                    } else {
                        builder.append(this.values[0]);
                    }
                    break;
                case SPECIES:
                    builder.append(getEnumName(EntitySpecies.values(), this.values[0]));
                    break;
                case ENTITY_MOVE_FLAGS:
                    builder.append(getBitFlagsString(this.values[0], EntityMovementType.values()));
                    break;
                case ANIM_ACTION:
                    builder.append(getEnumName(EntityAnimActionMode.values(), this.values[0]));
                    break;
                case HELP:
                    builder.append(getEnumName(HelpIDs.values(), this.values[0]));
                    break;
                default:
                    throw new RuntimeException("Unsupported argument type: " + this.argumentDefinition.getValueType());
            }
        }

        /**
         * Gets the comment marking the user needs to fix something.
         * TODO: USE THIS.
         */
        public String getTodoComment() {
            switch (this.argumentDefinition.getValueType()) {
                case SKIPPED:
                case INTEGER:
                case SHIFTED_SHORT:
                case POSITION:
                case SPECIES:
                case ENTITY_MOVE_FLAGS:
                case ANIM_ACTION:
                case HELP:
                    return null;
                case ENTITY_BEHAVIOR:
                case SOUND_NAME:
                case EVENT_ID:
                case SUBSPECIES:
                    return "Resolve the ID to a name/label.";
                case AMBIGUOUS:
                    if (!GameUtils.isValidLookingPointer(GamePlatform.PLAYSTATION, this.values[0]))
                        return null;
                case HEX_INTEGER:
                    return "Resolve memory reference to name.";
                default:
                    throw new RuntimeException("Unsupported argument type: " + this.argumentDefinition.getValueType());
            }
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class IntermissionNodeArgument {
        @NonNull private final String name;
        @NonNull private final IntermissionValueType valueType;
        private final String todoComment;
    }

    private enum HelpIDs {
        HELP_RES_ALL_BUTTONS,							// 0
        HELP_RES_JUST_EXIT,								// 1
        HELP_RESERVED3,									// 2
        HELP_RESERVED4,									// 3

        HELP_CONTROL_REFERENCE,		// 4
        HELP_TEST,										// 5
        HELP_NULL1,										// 6 - AVAILABLE!

        HELP_NULL2,										// 7 - AVAILABLE!
        HELP_NULL3,										// 8 - AVAILABLE!
        HELP_NULL4,										// 9 - AVAILABLE!
        HELP_NULL5,										// 10 - AVAILABLE!
        HELP_NULL6,										// 11 - AVAILABLE!

        HELP_NULL7,										// 12 - AVAILABLE!
        HELP_NULL8,										// 13 - AVAILABLE!
        HELP_NULL9,										// 14 - AVAILABLE!
        HELP_NULL10,									// 15 - AVAILABLE!
        HELP_NULL11,									// 16 - AVAILABLE!

        HELP_GY1_SHOP_ADVICE,							// 17

        HELP_GS_CAPTAINSTART_ADVICE,					// 18
        HELP_GS_CAPTAINEND_ADVICE,						// 19

        HELP_TD_START_NAR,							// 20

        HELP_GG_SERPENTADVICE,							// 21
        HELP_GG_SELLGARGADVICE,							// 22

        HELP_GY1_HEALTH_ADVICE,							// 23
        HELP_GY1_INVENTORY_ADVICE,						// 24
        HELP_GY1_KEY_ADVICE2,							// 25
        HELP_GY1_ANGEL_HINT,							// 26
        HELP_GY1_SKULL_DOOR_ADVICE,						// 27

        HELP_TD_TRAIN_ADVICE,							// 28

        HELP_GY1_AMBUSH_ADVICE,							// 29
        HELP_GY1_EXIT_NAR,								// 30

        HELP_CH_CLUB_ADVICE,							// 31
        HELP_CH_CLUB2_ADVICE,							// 32

        HELP_DC_FLOORWARN,								// 33
        HELP_DC_IMP_ADVICE,								// 34
        HELP_DC_MR_ORGAN_ADVICE,						// 35
        HELP_DC_SMASHCOFFIN_ADVICE,						// 36
        HELP_DC_DEMON_ADVICE,							// 37

        HELP_GY2_SKULL_GATE_ADVICE,						// 38

        HELP_EH_START_NAR,								// 39
        HELP_EH_IMPSADVICE,								// 40

        HELP_GY2_TREASURE_ADVICE,						// 41
        HELP_GY2_WORLDMAP_NAR,							// 42

        HELP_SF_SCARECROW_ADVICE,						// 43
        HELP_SF_WICKERMEN_ADVICE,						// 44
        HELP_SF_VILLAGE_ADVICE,							// 45
        HELP_SF_INVENTIONS_ADVICE,						// 46
        HELP_SF_CORN_CUTTING_ADVICE,					// 47
        HELP_SF_PUMPKINGORGE_NAR,						// 48

        HELP_LA_BOATMAN_ARRIVAL_SPEAK,					// 49

        HELP_EE_TALISMAN_ADVICE,						// 50

        HELP_PG_EXIT_NAR,								// 51

        HELP_CH_ZAROK_SPEAK,							// 52
        HELP_CH_GARGOYLE_SPEAK,							// 53

        HELP_LA_BARNACLE_ADVICE,						// 54

        HELP_LA_HUB_PIECE_INFO,							// 55

        HELP_AG_RIDDLE_INTRO,							// 56
        HELP_AG_RIDDLE_STAR,							// 57
        HELP_AG_RIDDLE_CLOWN,							// 58
        HELP_AG_RIDDLE_ELEPHANT,						// 59
        HELP_AG_RIDDLE_SHADOW,							// 60
        HELP_AG_RIDDLE_STARCOMPLETE,					// 61
        HELP_AG_RIDDLE_CLOWNCOMPLETE,					// 62
        HELP_AG_RIDDLE_ELEPHANTCOMPLETE,				// 63
        HELP_AG_RIDDLE_SHADOWCOMPLETE,					// 64
        HELP_AG_RIDDLE_ALLSOLVED,						// 65

        HELP_GY2_STONEWOLVES_WARNING,					// 66

        HELP_AC_FAIRY_FAIRIESTRAPPED,					// 67
        HELP_AC_FAIRY_FORCEFIELDANDFIREFLIES,			// 68
        HELP_AC_WITCH_SUCCESS,							// 69
        HELP_AC_WITCH_FAILURE,							// 70
        HELP_AC_WITCH_FAIRY_SUCCESS,					// 71
        HELP_AC_WITCH_RESTORE_DAN,						// 72

        HELP_PD_BOATMAN_SCENE1,							// 73
        HELP_PD_BOATMAN_SCENE2,							// 74
        HELP_PD_BOATMAN_SCENE3,							// 75

        HELP_EE_SELLGARGOYLERELEASED,					// 76

        HELP_AC_MOCKING_NAR,	 						// 77
        HELP_AC_FORCEFIELDADVICE,						// 78
        HELP_AC_QUEENADVICE,							// 79

        HELP_PD_START_NAR,								// 80
        HELP_PD_JUMPADVICE,								// 81

        HELP_AG_STARTADVICE,							// 82
        HELP_AG_CHESSBOARDADVICE,						// 83

        HELP_EE_STARTADVICE,							// 84
        HELP_EE_SHADOWGATEADVICE,						// 85
        HELP_EE_WITCHADVICE,							// 86
        HELP_EE_SHADOWTOMB_NAR,							// 87
        HELP_EE_TOMBEXIT_NAR,							// 88

        HELP_EE_WITCH_ASK_DAN,							// 89
        HELP_EE_WITCH_DAN_REFUSES,						// 90
        HELP_EE_WITCH_DAN_AGREES,						// 91

        HELP_SV_START_NAR,								// 92
        HELP_SV_CRUCIFIXADVICE,							// 93
        HELP_SV_FOUNTAINADVICE,							// 94
        HELP_SV_MAYORCAPTUREDADVICE,					// 95
        HELP_SV_CRUCIFIXCAST_NOTE,						// 96
        HELP_SV_METALBUST_PLAQUE,						// 97
        HELP_SV_HISTORY1_BOOK,							// 98
        HELP_SV_HISTORY2_BOOK,							// 99
        HELP_SV_HISTORY3_BOOK,							// 100
        HELP_SV_HISTORY4_BOOK,							// 101

        HELP_HR_FARMADVICE,								// 102
        HELP_HR_BRIDGEADVICE,							// 103 UNUSED
        HELP_HR_FRONTPARAPETADVICE,						// 104
        HELP_HR_SHADOWGATE_NAR,							// 105
        HELP_HR_THRONEROOMADVICE,						// 106
        HELP_HR_LAVAFLOODGATEADVICE,					// 107
        HELP_HR_CASTLEEXITADVICE,						// 108

        HELP_HR_KING_SPEAK,								// 109
        HELP_HR_FLOOD_GATE_ACTIVATED,					// 110

        HELP_HRCROWNADVICE,								// 111

        HELP_HR_FARMER_THANKS,							// 112

        HELP_LA_CRYSTALENTRANCE_NAR,				  	// 113

        HELP_PS_PODADVICE,								// 114

        HELP_SV_CHURCH_BOOK,							// 115
        HELP_SV_TOURISTGUIDE1_BOOK,						// 116
        HELP_SV_TOURISTGUIDE2_BOOK,						// 117
        HELP_SV_HEROES1_BOOK,							// 118

        HELP_PS_WITCH_BEFORE_PS_DEAD,					// 119
        HELP_PS_WITCH_BEFORE_PS_DEAD2,					// 120
        HELP_PS_WITCH_AFTER_PS_DEAD,					// 121

        HELP_CC_DRAGON_GIVE_ARMOUR,						// 122

        HELP_CC_START_NAR,								// 123
        HELP_CC_DRAGON_DISTURBED,						// 124
        HELP_CC_EXITADVICE,								// 125 UNUSED
        HELP_CC_DRAGONADVICE,							// 126

        HELP_GS_THINPIRATE_SPEAK,						// 127
        HELP_GS_PIRATECAPTAIN_SPEAK,					// 128

        HELP_CR_START1_NAR,								// 129
        HELP_CR_ADVICE1,								// 130
        HELP_CR_INVERT_BOOK,							// 131
        HELP_CR_POWERATTACK_BOOK,						// 132
        HELP_CR_ADVICE4,								// 133
        HELP_CR_ADVICE5,								// 134
        HELP_CR_ADVICE6,								// 135

        HELP_SV_BOILGUARD_SPEAK1,						// 136
        HELP_SV_BOILGUARD_SPEAK2,						// 137

        HELP_CR_TREASURE_BOOK,							// 138
        HELP_CR_SHOULDERCHARGE_BOOK,					// 139
        HELP_CR_ADVICE11,								// 140

        HELP_CR_BOOK1,									// 141

        HELP_ZL_ENTRANCE_NAR,							// 142
        HELP_ZL_ZAROK_SPEAK1,							// 143
        HELP_ZL_ZAROK_SPEAK2,							// 144
        HELP_ZL_GOOD_SKELS,								// 145
        HELP_ZL_ZAROK_SPEAK3,							// 146
        HELP_ZL_ZAROK_SPEAK4,							// 147

        HELP_IA_MAYOR_SPEAK1,							// 148

        HELP_CH_BOOK1,									// 149

        HELP_EH_MAGICBOOK1,								// 150
        HELP_EH_MAGICBOOK2,								// 151

        HELP_CH_WITCH_NAR,								// 152

        HELP_ZL_ZAROKHEAD_SPEAK1,						// 153
        HELP_ZL_ZAROKHEAD_SPEAK2,						// 154
        HELP_ZL_ZAROKHEAD_SPEAK3,						// 155
        HELP_ZL_ZAROKHEAD_SPEAK4,						// 156
        HELP_ZL_ZAROKHEAD_SPEAK5,						// 157
        HELP_ZL_ZAROKHEAD_SPEAK6,						// 158
        HELP_ZL_GOOD_SKELS_WIN,							// 159
        HELP_ZL_BAD_SKELS_WIN,							// 160
        HELP_ZL_ZAROK_DEFEATED_SPEAK,					// 161
        HELP_ZL_ZAROK_ARENAFALL_SPEAK,					// 162
        HELP_ZL_SKEL_BATTLE_OVER,						// 163

        HELP_HR_FARMER_SPEAK1,							// 164
        HELP_HR_FARMER_SPEAK2,							// 165
        HELP_HR_FARMER_SPEAK3,							// 166

        HELP_ZL_GOOD_SKELS_HELP,						// 167

        HELP_NAR_INTRO,									// 168
        HELP_NAR_GAMEOVER1,								// 169
        HELP_NAR_GAMEOVER2,								// 170

        HELP_CR_START2_NAR,								// 171

        HELP_PD_FATKNIGHTADVICE,						// 172

        HELP_HR_CASTLE_NAR,								// 173

        HELP_EE_WITCH_SPEAK2,							// 174

        HELP_HR_KING_SPEAK2,							// 175
        HELP_HR_KING_SPEAK3,							// 176
        HELP_HR_KING_SPEAK4,							// 177

        HELP_GS_THINPIRATE_SPEAK2,						// 178
        HELP_GS_PIRATECAPTAIN_SPEAK2,					// 179

        HELP_IA_MAYOR_SPEAK2,							// 180

        HELP_MAX_ID,
        HELP_NULL_ID
    }

    private enum EntitySpecies
    {
        ENT_SPECIES_DAN,
        ENT_SPECIES_GY1ZOMBIE1,
        ENT_SPECIES_GY1ZOMBIE2,
        ENT_SPECIES_ZLSKELETON,
        ENT_SPECIES_CHEST,
        ENT_SPECIES_WEAPON,
        ENT_SPECIES_SHIELD,
        ENT_SPECIES_POTION,
        ENT_SPECIES_GENERATOR,
        ENT_SPECIES_DOOR1,
        ENT_SPECIES_SWORD,
        ENT_SPECIES_THUMPER,
        ENT_SPECIES_LIGHTNING,
        ENT_SPECIES_PROJECTILE,
        ENT_SPECIES_PSPUMPKIN,
        ENT_SPECIES_SMARTBOMB,
        ENT_SPECIES_DRAGONDAN,
        ENT_SPECIES_RUNEKEY,
        ENT_SPECIES_GY2WOLF1,
        ENT_SPECIES_GY2HEADLESS1,
        ENT_SPECIES_TUMBLING1,
        ENT_SPECIES_SEVHAND1,					// Free!
        ENT_SPECIES_SFCROW,
        ENT_SPECIES_ZAROKTRAIL,
        ENT_SPECIES_SFSCREW,
        ENT_SPECIES_PARTICLEENT,
        ENT_SPECIES_STATICENT,
        ENT_SPECIES_SFWICKERMANHEAD,
        ENT_SPECIES_SFDISCOFDEATH,
        ENT_SPECIES_SFWINDMILL,
        ENT_SPECIES_FIREBALL,
        ENT_SPECIES_SFINOUTCTRL,
        ENT_SPECIES_SFINOUTPOLE,
        ENT_SPECIES_SFSPINNER,
        ENT_SPECIES_SFSPINNYBOWL,
        ENT_SPECIES_SFTOPGRINDER,
        ENT_SPECIES_AGCHESSPIECE,
        ENT_SPECIES_DCIMP1,
        ENT_SPECIES_DC_COFFINLID,
        ENT_SPECIES_TORCH,
        ENT_SPECIES_AGHEAD,
        ENT_SPECIES_GY1COFFIN1,
        ENT_SPECIES_PDCHARIOT,
        ENT_SPECIES_MADMONK,
        ENT_SPECIES_IAPATIENT,
        ENT_SPECIES_STAINEDGLASSDEMON,
        ENT_SPECIES_MUDKNIGHT,
        ENT_SPECIES_NMEPROJ,
        ENT_SPECIES_FATKNIGHT,
        ENT_SPECIES_PICKUP,
        ENT_SPECIES_MRORGAN,
        ENT_SPECIES_GHOUL,
        ENT_SPECIES_SVBELLOWS,
        ENT_SPECIES_VINE,
        ENT_SPECIES_MRSMAD,
        ENT_SPECIES_PGMAGGOT,					// Free!
        ENT_SPECIES_PGPPLANT,
        ENT_SPECIES_PGPUMPKIN,
        ENT_SPECIES_MRMAD,
        ENT_SPECIES_HRCRAWLDEMON1,
        ENT_SPECIES_NELLIEMAD,
        ENT_SPECIES_ANT,
        ENT_SPECIES_SVSTOREPAD,
        ENT_SPECIES_QUEENANT,
        ENT_SPECIES_HRCYCLOPS,
        ENT_SPECIES_HRCANNON,
        ENT_SPECIES_CHICKEN,
        ENT_SPECIES_ANTLARVAE,
        ENT_SPECIES_HRCORNPILE,
        ENT_SPECIES_SVCRUCIFIXHOLDER,
        ENT_SPECIES_PGMONSTER,
        ENT_SPECIES_PGPODBALL,
        ENT_SPECIES_HRCHAIN,
        ENT_SPECIES_EETOMBLID,
        ENT_SPECIES_HRCATAPULT,
        ENT_SPECIES_HRBOULDER,
        ENT_SPECIES_HRBLOCK,
        ENT_SPECIES_SVFOUNTAIN,
        ENT_SPECIES_RHINOTAUR,
        ENT_SPECIES_SERPENT,
        ENT_SPECIES_PGBARNSCREW,
        ENT_SPECIES_SVDUCKY,						// Free!
        ENT_SPECIES_SVDUCKSHOOT,					// Free!
        ENT_SPECIES_IAHEADLESS1,
        ENT_SPECIES_CCLARGELIFT,
        ENT_SPECIES_EHIMP1,
        ENT_SPECIES_CCCRYSTAL,
        ENT_SPECIES_ACFIREFLY,
        ENT_SPECIES_GSPIRATECAPTAIN,
        ENT_SPECIES_GSTHINPIRATE,
        ENT_SPECIES_SFSCARECROW,
        ENT_SPECIES_GSBARREL,
        ENT_SPECIES_GSSPINDLE,
        ENT_SPECIES_GSCANNON,
        ENT_SPECIES_CCSCOUTDEMON1,
        ENT_SPECIES_GSCANNONDOORCONT,
        ENT_SPECIES_GSNETDAEMON,
        ENT_SPECIES_PGMUSHYTOP,
        ENT_SPECIES_SFCORNKILLER,
        ENT_SPECIES_GSCAPSTAN,
        ENT_SPECIES_GSCRANE,
        ENT_SPECIES_GSLIFT,
        ENT_SPECIES_CCDRAGON,
        ENT_SPECIES_SFFARMER,
        ENT_SPECIES_TDBOILERGUARD,
        ENT_SPECIES_GSPIRATEFRED,
        ENT_SPECIES_ZARHEAD,		//				I'm free!
        ENT_SPECIES_SFCROWTREE,
        ENT_SPECIES_GSCAMDAEMON,
        ENT_SPECIES_SKEWEREDMAN,
        ENT_SPECIES_ACBOULDER,
        ENT_SPECIES_TDCOG,
        ENT_SPECIES_TDFINGER,
        ENT_SPECIES_TDEYE,
        ENT_SPECIES_TDGATE,
        ENT_SPECIES_TDEGGTIMER,
        ENT_SPECIES_KEYHOLE,
        ENT_SPECIES_SVWOLF1,
        ENT_SPECIES_MARKER,
        ENT_SPECIES_SFBAT1,
        ENT_SPECIES_HRLAVATAP,
        ENT_SPECIES_SFSAWBLADE,
        ENT_SPECIES_TDTURNTABLE,
        ENT_SPECIES_TDCIRCUIT,
        ENT_SPECIES_SFCHAFFBLOWER,
        ENT_SPECIES_TDGLOBE,
        ENT_SPECIES_DCDEMONHEART,
        ENT_SPECIES_CCBAT1,
        ENT_SPECIES_HRMACEKNIGHT1,
        ENT_SPECIES_TDCRYSTAL,
        ENT_SPECIES_PSFLAPPYFISH,
        ENT_SPECIES_LATOADDEMON,
        ENT_SPECIES_LAELEDRAGON,
        ENT_SPECIES_PGTUNNELWALL,
        ENT_SPECIES_TDSWITCH1,
        ENT_SPECIES_TDSIMPLELIFT,
        ENT_SPECIES_SWJABBER,
        ENT_SPECIES_SWSPIDER,
        ENT_SPECIES_BIGPLANET,
        ENT_SPECIES_HANGEDMAN,
        ENT_SPECIES_LAFISH,
        ENT_SPECIES_LABARNACLE,
        ENT_SPECIES_LACRYSTAL,
        ENT_SPECIES_LAJUNCTION,
        ENT_SPECIES_EECABLECAR,
        ENT_SPECIES_IAZOMBIE3,
        ENT_SPECIES_GGFALLINGSLAB,
        ENT_SPECIES_CCDEATHRAY,
        ENT_SPECIES_CCBLOCK,
        ENT_SPECIES_CCIMP1,
        ENT_SPECIES_PGSPIRALHILLGEN,
        ENT_SPECIES_LASCOUTDEMON1,
        ENT_SPECIES_GMTRAIN,
        ENT_SPECIES_DCSPIRITS,
        ENT_SPECIES_GMSWITCH1,
        ENT_SPECIES_EEGOBBA,
        ENT_SPECIES_EEPODSPITTA,
        ENT_SPECIES_GMCONDUIT,
        ENT_SPECIES_GMLIFT,
        ENT_SPECIES_EEPODBOMB,
        ENT_SPECIES_GMHUBCUPBOARD,
        ENT_SPECIES_CHBOULDER,
        ENT_SPECIES_GMFLAMER,
        ENT_SPECIES_GMSTEAMER,
        ENT_SPECIES_GMEYE1,
        ENT_SPECIES_TDEYE1,
        ENT_SPECIES_TDWATCH,
        ENT_SPECIES_TDCLOCK,
        ENT_SPECIES_GMCOG,
        ENT_SPECIES_GMHAMMER,
        ENT_SPECIES_GMHANDWHEEL,
        ENT_SPECIES_GMHANDSTER,
        ENT_SPECIES_EECRAWLDEMON1,
        ENT_SPECIES_EESCOUTDEMON1,
        ENT_SPECIES_EEGODDESSES,
        ENT_SPECIES_TDCLOCKSWITCH,
        ENT_SPECIES_ZLKNIGHT,
        ENT_SPECIES_TDELECTRICGLOBE,
        ENT_SPECIES_EEPLATFORM,
        ENT_SPECIES_TDCLOCKTELLER,
        ENT_SPECIES_EEBADSPIDER,
        ENT_SPECIES_EEROCKBLOCK,
        ENT_SPECIES_EEBIRDLEGS,
        ENT_SPECIES_IABOMB,
        ENT_SPECIES_TDKEYGLASS,
        ENT_SPECIES_EEEGG,
        ENT_SPECIES_IACANNON,
        ENT_SPECIES_TDNETDAEMON,
        ENT_SPECIES_TDTRAIN,
        ENT_SPECIES_FREESLOT,			// Free slot
        ENT_SPECIES_FREESLOT2,			// Free slot
        ENT_SPECIES_IABAT1,
        ENT_SPECIES_GMBOILER,
        ENT_SPECIES_GMCHIMNEY1,
        ENT_SPECIES_GMCHIMNEY2,
        ENT_SPECIES_CHHEADLESS1,
        ENT_SPECIES_GY1ANGEL1,
        ENT_SPECIES_GY2SHIFTYBLOCK,
        ENT_SPECIES_CHZOMBIE1,
        ENT_SPECIES_GY2ZOMBIE2,
        ENT_SPECIES_CHGARGOYLE,
        ENT_SPECIES_GY2STONEWOLF,
        ENT_SPECIES_SMASHROCK,
        ENT_SPECIES_GY2COFFIN1,
        ENT_SPECIES_PGBOULDER,
        ENT_SPECIES_DCSMASHGLASS,
        ENT_SPECIES_DCFIREBLOCK,
        ENT_SPECIES_DCSPIRITHEART,
        ENT_SPECIES_HELP_GARGOYLE,
        ENT_SPECIES_GY2ANGEL1,
        ENT_SPECIES_CHZAROK,
        ENT_SPECIES_GY2WARPTWINKLE,
        ENT_SPECIES_SFWICKERMAN,
        ENT_SPECIES_SFHAYCART,
        ENT_SPECIES_LAEYE,
        ENT_SPECIES_LASEAWEED,
        ENT_SPECIES_LALEVER,
        ENT_SPECIES_SFPUSHCRATE,
        ENT_SPECIES_SFHAYSTACK,
        ENT_SPECIES_SFLEVER,
        ENT_SPECIES_SFHARVESTER,
        ENT_SPECIES_SFBARNWALL,
        ENT_SPECIES_ACROCKNROLL,
        ENT_SPECIES_AGSTAR,
        ENT_SPECIES_AGBELL,
        ENT_SPECIES_AGFACE,
        ENT_SPECIES_AGGRILL,
        ENT_SPECIES_ACSHATTERWALL,
        ENT_SPECIES_AGCLOWN,
        ENT_SPECIES_AGFIREBLOCK,
        ENT_SPECIES_ACFAIRY,
        ENT_SPECIES_AGHEDGE,
        ENT_SPECIES_ACFORCEFIELD,
        ENT_SPECIES_AGRODENT,
        ENT_SPECIES_AGHEDGECAT,
        ENT_SPECIES_AGELEPHANTDAEMON,
        ENT_SPECIES_ACWITCH,
        ENT_SPECIES_AGELEPHANTWALL,
        ENT_SPECIES_PDBOAT,
        ENT_SPECIES_EEDRAGONTOAD,
        ENT_SPECIES_PDBOATMAN,
        ENT_SPECIES_EEWINGEDDEMON,
        ENT_SPECIES_EEFLAME,
        ENT_SPECIES_EEMOLTENROCK,
        ENT_SPECIES_HHSTATUE,
        ENT_SPECIES_EEWITCH,
        ENT_SPECIES_HRGOLEM,
        ENT_SPECIES_HRFLYDEMON,
        ENT_SPECIES_HRFARMER,
        ENT_SPECIES_HRFLAME,
        ENT_SPECIES_SVLEVER,
        ENT_SPECIES_HRLEVER,
        ENT_SPECIES_SVPUSHABLE,
        ENT_SPECIES_SVBOILERGUARD,
        ENT_SPECIES_HRTHEKING,
        ENT_SPECIES_PSPPLANT,						// 255 - last entity that may be generated!
        ENT_SPECIES_PSWITCH,
        ENT_SPECIES_SVSAFE,
        ENT_SPECIES_CCROCK,
        ENT_SPECIES_GSLAMPWRAPPER,
        ENT_SPECIES_TDTRAINPLATFORM,
        ENT_SPECIES_GSEXPLODEBARREL,
        ENT_SPECIES_GSCANNONBALL,
        ENT_SPECIES_TDFLAME,
        ENT_SPECIES_TDSTEAM,
        ENT_SPECIES_TDLEVER,
        ENT_SPECIES_GSCLOUDDAEMON,
        ENT_SPECIES_TDROLLCLOCK,
        ENT_SPECIES_TDGLOBECONDUIT,
        ENT_SPECIES_IAMAYOR,
        ENT_SPECIES_IAOBJECTDROPPER,
        ENT_SPECIES_ZLZAROK,
        ENT_SPECIES_ZLCHALICE,
        ENT_SPECIES_EHSPHERE,
        ENT_SPECIES_CHBOOKCASE,
        ENT_SPECIES_CHMAGICFIRE,
        ENT_SPECIES_PSRAT1,
        ENT_SPECIES_SVRAT1,
        ENT_SPECIES_GY1RAT1,
        ENT_SPECIES_GY2RAT1,
        ENT_SPECIES_GGZOMBIE3,
        ENT_SPECIES_CRSMASHWALL,
        ENT_SPECIES_ZLZARHEAD,
        ENT_SPECIES_LABOAT,
        ENT_SPECIES_LABOATMAN,
        ENT_SPECIES_ZLTRAIN,
        ENT_SPECIES_GY2FLAME,
        ENT_SPECIES_ZLOBJECTDROPPER,
        ENT_SPECIES_ZLDROPBLOCK,
        ENT_SPECIES_EEFALLAWAY,
        ENT_SPECIES_DCSMASHSPIKES,

        ENT_SPECIES_MAX
    }

    private enum EntityAnimActionMode
    {
        ENT_MODE_STANDING, 		// Basic wait
        ENT_MODE_WAITING1,		// Special waits...
        ENT_MODE_WAITING2,
        ENT_MODE_TIPTOE,
        ENT_MODE_WALKING,
        ENT_MODE_TURN_LEFT,
        ENT_MODE_TURN_RIGHT,
        ENT_MODE_BACKSTEP,
        ENT_MODE_WALK_LEFT,
        ENT_MODE_WALK_RIGHT,
        ENT_MODE_RUNNING1,	 	// Runs...
        ENT_MODE_RUNNING2,
        ENT_MODE_SKIDDING,
        ENT_MODE_ACTION1,
        ENT_MODE_ACTION2,
        ENT_MODE_ACTION3,
        ENT_MODE_ACTION4,
        ENT_MODE_ACTION5,
        ENT_MODE_ACTION6,
        ENT_MODE_ACTION7,		// (defend by convention)
        ENT_MODE_ACTION8,		// (duck by convention)
        ENT_MODE_ACTION9,		// (moving jump by convention)
        ENT_MODE_ACTION10,		// (static jump by convention)
        ENT_MODE_ATTACK1,	 	// Type specific attacks...
        ENT_MODE_ATTACK2,
        ENT_MODE_ATTACK3,
        ENT_MODE_ATTACK4,
        ENT_MODE_ATTACK5,
        ENT_MODE_ATTACK6,
        ENT_MODE_ATTACK7,
        ENT_MODE_ATTACK8,
        ENT_MODE_RECOIL1,
        ENT_MODE_RECOIL2,
        ENT_MODE_DEATH1,	 	// Death anims...
        ENT_MODE_DEATH2,
        ENT_MODE_DEATH3,
        ENT_MODE_DEATH4,

        ENT_ACTIONS_MAX
    };

    private enum EntityMovementType implements BitFlagEnum {
        // Low level movement request types...

        ENT_MOVE_FORWARD_F,		 	// Move forwards - ground-based or aerial.
        ENT_MOVE_BACKWARD_F,	 	// Move backwards - ground-based or aerial.
        ENT_MOVE_LEFT_F,		 	// Move left/side-step.
        ENT_MOVE_RIGHT_F,		 	// Move right/side-step.
        ENT_MOVE_UP_F,			 	// Move up - aerial only.
        ENT_MOVE_DOWN_F,		 	// Move down - aerial only.

        ENT_MOVE_TURN_LEFT_F,	 	// Turn by species TurnSpeed.
        ENT_MOVE_TURN_RIGHT_F,	 	// Turn by species TurnSpeed.

        // Higher level movement requests...

        ENT_MOVE_TARGET_POINT_F, 	// Workout, target and turn to map point (y rotate only).
        ENT_MOVE_TARGET_ANGLE_F, 	// Turn to angle (y rotate only).

        // Action related requests...

        ENT_MOVE_RUN_F,			 	// Perform defined action at 'run' speed.
        ENT_MOVE_TIPTOE_F,			// Perform defined action at 'tiptoe' speed.

        ENT_MOVE_JUMP_F,		 	// Trigger entity jump (if physics allows).
        ENT_MOVE_DUCK_F,		 	// Trigger entity duck.

        // Anim system integration...

        ENT_MOVE_SET_ANIM_F,	 	// Move also requires animation resolution and change.
        ENT_MOVE_REQ_FROM_ANIM_F,	// Move bits should be used for anim res but NOT movement.

        // Miscellanous movement system related...

        ENT_MOVE_INVOLUNTARY_F,		// Flag that involuntary move vector should be applied.
        ENT_MOVE_REBUILD_MATRIX_F,	// Flag matrix rebuild necessary (for hand-rolled rotations, etc).

        ENT_MOVE_FORCE_YROT_F,		// Used to force entity's y rotation during movement.
        ENT_MOVE_LIMIT_F
    }
}