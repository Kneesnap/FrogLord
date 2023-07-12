package net.highwayfrogs.editor.games.tgq.script;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registration of different actions((byte) 0x00), as defined in "_kcActionID".
 * Created by Kneesnap on 6/26/2023.
 */
@AllArgsConstructor
public enum kcActionID {
    NONE((byte) 0x00), // kcCEntity::OnCommand NOTE: Seems to have unused parameters sometimes.
    STOP((byte) 0x01), // // kcCActorBase::ProcessAction. NOTE: Seems to have unused parameters sometimes.
    ACTIVATE((byte) 0x02, kcParamType.BOOLEAN, "activate"), // kcCEntity::OnCommand
    ENABLE((byte) 0x03, kcParamType.HASH, "hTarget"), // NOTE: I was unable to actually find where this is implemented, it's possible this is unimplemented.
    TERMINATE((byte) 0x04), // kcCActorBase::ProcessAction, kcCEntity::OnCommand  NOTE: Seems to have unused parameters sometimes.
    INITFLAGS((byte) 0x05, kcParamType.INT, "flagId"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand kcCEntity::OnCommand, kcCActor::OnCommand
    SETFLAGS((byte) 0x06, kcParamType.INT, "flagId"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCEntity::OnCommand, kcCActor::OnCommand
    CLEARFLAGS((byte) 0x07, kcParamType.INT, "flagId"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCEntity::OnCommand, kcCActor::OnCommand
    SETSTATE((byte) 0x08), // kcCActorBaseMsg::OnCommand NOTE: This is unused, and while the name suggests it should have parameters, none appear implemented.
    SETTARGET((byte) 0x09, kcParamType.INT, "targetId"), // kcCEntity::OnCommand
    SETSPEED((byte) 0x0A, kcParamType.FLOAT, "speed"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    SETPOSITION((byte) 0x0B, kcParamType.AXIS, "axis", kcParamType.FLOAT, "pos"), // kcCEntity3D::OnCommand
    SETPOSITION_XYZ((byte) 0x0C, kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z"), // kcCEntity3D::OnCommand
    ADDPOSITION((byte) 0x0D, kcParamType.AXIS, "axis", kcParamType.FLOAT, "displacement"), // kcCEntity3D::OnCommand
    ADDPOSITION_XYZ((byte) 0x0E, kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z"), // kcCEntity3D::OnCommand
    SETROTATION((byte) 0x0F, kcParamType.AXIS, "axis", kcParamType.FLOAT, "angleInRadians"), // kcCEntity3D::OnCommand
    SETROTATION_XYZ((byte) 0x10, kcParamType.FLOAT, "xAngleInRadians", kcParamType.FLOAT, "yAngleInRadians", kcParamType.FLOAT, "zAngleInRadians"), // kcCEntity3D::OnCommand
    ADDROTATION((byte) 0x11, kcParamType.AXIS, "axis", kcParamType.FLOAT, "angleInRadians"), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ADDROTATION_XYZ((byte) 0x12, kcParamType.FLOAT, "xAngleInRadians", kcParamType.FLOAT, "yAngleInRadians", kcParamType.FLOAT, "zAngleInRadians"), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_RIGHT((byte) 0x13, kcParamType.FLOAT, "angleInRadians"), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_LEFT((byte) 0x14, kcParamType.FLOAT, "angleInRadians"), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_TARGET((byte) 0x15), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCEntity3D::OnCommand, kcCActor::OnCommand
    SETANIMATION((byte) 0x16, kcParamType.HASH, "hTrack", kcParamType.INT, "startTick", kcParamType.INT, "transTime", kcParamType.UNSIGNED_INT, "mode"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCActor::OnCommand TODO: kcCActorBaseMsg::OnCommand & kcCActor::OnCommand seem to swap the order of the values so 'mode' is wrong.
    SETSEQUENCE((byte) 0x17, kcParamType.HASH, "hSequence", kcParamType.BOOLEAN, "interrupt", kcParamType.BOOLEAN, "openBoneChannel"), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    WAIT((byte) 0x18, kcParamType.INT, "time"), // kcCActorBase::ProcessAction
    WAIT_ROTATE((byte) 0x19, kcParamType.AXIS, "axis"), // kcCActorBase::ProcessAction
    WAIT_ROTATE_XYZ((byte) 0x1A), // kcCActorBase::ProcessAction
    WAIT_ANIMATION((byte) 0x1B), // kcCActorBase::ProcessAction
    LOOP((byte) 0x1D, kcParamType.INT, "loopCount"), // kcCActorBase::ProcessAction
    IMPULSE((byte) 0x1E, kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    DAMAGE((byte) 0x1F, kcParamType.UNSIGNED_INT, "weaponMask", kcParamType.INT, "attackStrength"), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    PROMPT((byte) 0x2F, kcParamType.HASH, "hRes"), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand NOTE: This seems unused, and we don't know for certain the argument is labelled correctly. It is implemented though.
    DIALOG((byte) 0x30, kcParamType.HASH, "hRes"), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    SETALARM((byte) 0x32, kcParamType.INT, "alarmId", kcParamType.UNSIGNED_INT, "duration", kcParamType.INT, "repeatCount"), // kcCActorBase::ProcessAction, kcCEntity::OnCommand
    TRIGGER_EVENT((byte) 0x33, kcParamType.HASH, "eventHash", kcParamType.UNSIGNED_INT, "priority"), // kcCActorBase::ProcessAction, kcCEntity::OnCommand TODO: kcCEntity::OnCommand doesn't include "priority", instead always sending zero.
    PLAYSFX((byte) 0x34, kcParamType.INT, "sound"), // kcCEntity::OnCommand, kcCEntity3D::OnCommand
    VARIABLESET((byte) 0x35, kcParamType.INT, "variableId", kcParamType.INT, "value"), // kcCEntity::OnCommand
    VARIABLEADD((byte) 0x36, kcParamType.INT, "variableId", kcParamType.INT, "value"), // kcCEntity::OnCommand
    NUMBER((byte) 0x37, kcParamType.INT, "number", kcParamType.INT, "operation", kcParamType.HASH, "hFromEntity"), // kcCEntity::OnCommand
    PARTICLE((byte) 0x38, kcParamType.PARTICLE, "particleType"), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    KILLPARTICLE((byte) 0x39), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    LAUNCHER((byte) 0x3A), // CCharacter::OnCommand
    WITHITEM((byte) 0x3B, kcParamType.INVENTORY_ITEM, "item"), // CCharacter::OnCommand, CProp::OnCommand
    GIVETAKEITEM((byte) 0x3C, kcParamType.BOOLEAN, "takeItem", kcParamType.INVENTORY_ITEM, "item"), // CCharacter::OnCommand, CProp::OnCommand
    GIVEDAMAGE((byte) 0x3D, kcParamType.UNSIGNED_INT, "weaponMask", kcParamType.INT, "attackStrength"), // kcCScriptMgr::FireActorEffect converts this to the 'DAMAGE' command at 0x1F.
    SAVEPOINT((byte) 0x3E, kcParamType.UNSIGNED_INT, "savePointNumber", kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z"), // CCharacter::OnCommand
    ENABLE_UPDATE((byte) 0x3F, kcParamType.BOOLEAN, "update"), // kcCEntity::OnCommand
    AI_SETGOAL((byte) 0x40, kcParamType.INT, "goal"), // CCharacter::OnCommand
    ATTACH_SENSOR((byte) 0x41, kcParamType.ATTACH_ID, "type"), // CCharacter::OnCommand
    DETACH_SENSOR((byte) 0x42, kcParamType.ATTACH_ID, "type"), // CCharacter::OnCommand
    ATTACH((byte) 0x43, kcParamType.ATTACH_ID, "type"), // CCharacter::OnCommand
    DETACH((byte) 0x44, kcParamType.ATTACH_ID, "type"), // CCharacter::OnCommand
    ACTIVATE_SPECIAL((byte) 0x45, kcParamType.BOOLEAN, "activate", kcParamType.ANY, "value_has_different_purpose_for_different_entities"); // kcCEntity::OnCommand

    /*
    The following have enum entries in the game's 'kcActionID', but are not actually implemented to do anything in the game code.
    They are not used in any known version of the game.
    As such, they have been commented out here.
    WAIT_SEQUENCE((byte) 0x1C),
    SEEK((byte) 0x20),
    PERSUE((byte) 0x21),
    OFFSET_PERSUE((byte) 0x22),
    ARRIVAL((byte) 0x23),
    FLEE((byte) 0x24),
    EVADE((byte) 0x25),
    WANDER((byte) 0x26),
    CONTAINMENT((byte) 0x27),
    FLOCK((byte) 0x28),
    ENEMY_REACT((byte) 0x29),
    OBSTACLE_AVOID((byte) 0x2A),
    WALL_FOLLOW((byte) 0x2B),
    PATH_FOLLOW((byte) 0x2C),
    FLOWFIELD_FOLLOW((byte) 0x2D),
    LEADER_FOLLOW((byte) 0x2E),
    COMPLETE((byte) 0x31),*/

    @Getter private final byte opcode;
    private final NamedArgument[] parameters;

    private static final NamedArgument[] ATTACH_PARTICLE = makeArray(kcParamType.ATTACH_ID, "type", kcParamType.HASH, "hEffect", kcParamType.UNSIGNED_INT, "tag");
    private static final NamedArgument[] ATTACH_LAUNCHER = makeArray(kcParamType.ATTACH_ID, "type", kcParamType.UNSIGNED_INT, "tag", kcParamType.HASH, "hLaunchData");
    private static final NamedArgument[] ATTACH_ATTACK_OR_BUMP = makeArray(kcParamType.ATTACH_ID, "type", kcParamType.UNSIGNED_INT, "boneID", kcParamType.FLOAT, "radius", kcParamType.UNSIGNED_INT, "focus");
    private static final NamedArgument[] DETACH_PARTICLE = makeArray(kcParamType.ATTACH_ID, "type", kcParamType.HASH, "hOwner");

    kcActionID(byte opcode) {
        this.opcode = opcode;
        this.parameters = new NamedArgument[0];
    }

    kcActionID(byte opcode, kcParamType firstArgType, String firstArgName) {
        this.opcode = opcode;
        this.parameters = makeArray(firstArgType, firstArgName);
    }

    kcActionID(byte opcode, kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName) {
        this.opcode = opcode;
        this.parameters = makeArray(firstArgType, firstArgName, secondArgType, secondArgName);
    }

    kcActionID(byte opcode, kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName, kcParamType thirdArgType, String thirdArgName) {
        this.opcode = opcode;
        this.parameters = makeArray(firstArgType, firstArgName, secondArgType, secondArgName, thirdArgType, thirdArgName);
    }

    kcActionID(byte opcode, kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName, kcParamType thirdArgType, String thirdArgName, kcParamType fourthArgType, String fourthArgName) {
        this.opcode = opcode;
        this.parameters = makeArray(firstArgType, firstArgName, secondArgType, secondArgName, thirdArgType, thirdArgName, fourthArgType, fourthArgName);
    }

    /**
     * Get the parameter definitions for the action, assuming the given arguments are given.
     * @param arguments The arguments to the action. If null is supplied, the default parameters are returned.
     * @return parameters
     */
    public NamedArgument[] getParameters(kcParam[] arguments) {
        if (arguments == null || arguments.length == 0)
            return this.parameters;

        int firstArg = arguments[0].getAsInteger();

        if (this == ATTACH || this == ATTACH_SENSOR) {
            // CCharacter::OnCommand
            if (firstArg == kcAttachID.PARTICLE_EMITTER.ordinal()) {
                return ATTACH_PARTICLE;
            } else if (firstArg == kcAttachID.LAUNCHER.ordinal()) {
                return ATTACH_LAUNCHER;
            } else if (firstArg == kcAttachID.ATTACK_SENSOR.ordinal() || firstArg == kcAttachID.BUMP_SENSOR.ordinal()) {
                return ATTACH_ATTACK_OR_BUMP;
            }
        } else if (this == DETACH || this == DETACH_SENSOR) {
            // CCharacter::OnCommand
            if (firstArg == kcAttachID.PARTICLE_EMITTER.ordinal())
                return DETACH_PARTICLE;
        }

        return this.parameters;
    }

    private static NamedArgument[] makeArray(kcParamType firstArgType, String firstArgName) {
        return new NamedArgument[]{new NamedArgument(firstArgType, firstArgName)};
    }

    private static NamedArgument[] makeArray(kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName) {
        return new NamedArgument[]{
                new NamedArgument(firstArgType, firstArgName),
                new NamedArgument(secondArgType, secondArgName)
        };
    }

    private static NamedArgument[] makeArray(kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName, kcParamType thirdArgType, String thirdArgName) {
        return new NamedArgument[]{
                new NamedArgument(firstArgType, firstArgName),
                new NamedArgument(secondArgType, secondArgName),
                new NamedArgument(thirdArgType, thirdArgName)
        };
    }

    private static NamedArgument[] makeArray(kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName, kcParamType thirdArgType, String thirdArgName, kcParamType fourthArgType, String fourthArgName) {
        return new NamedArgument[]{
                new NamedArgument(firstArgType, firstArgName),
                new NamedArgument(secondArgType, secondArgName),
                new NamedArgument(thirdArgType, thirdArgName),
                new NamedArgument(fourthArgType, fourthArgName)
        };
    }

    @Getter
    @AllArgsConstructor
    public static class NamedArgument {
        private final kcParamType type;
        private final String name;
    }

    /**
     * Gets an action by its opcode.
     * @param opcode The opcode to search for.
     * @return action, or null.
     */
    public static kcActionID getActionByOpcode(int opcode) {
        int left = 0, right = values().length - 1;
        while (left <= right) {
            int mid = (left + right) / 2;
            int midOpcode = values()[mid].getOpcode() & 0xFF;

            if (opcode == midOpcode) {
                return values()[mid];
            } else if (opcode > midOpcode) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        System.out.println("There was no kcActionID found for the opcode " + opcode + ".");
        return null;
    }

    static {
        // Ensure entries are sorted by opcode so we can verify binary search will work.
        for (int i = 1; i < values().length; i++)
            if ((values()[i - 1].getOpcode() & 0xFF) >= (values()[i].getOpcode() & 0xFF))
                throw new RuntimeException("kcActionID is not sorted by opcode, " + values()[i].name() + " is out of order!");
    }
}
