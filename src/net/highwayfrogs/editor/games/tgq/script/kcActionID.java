package net.highwayfrogs.editor.games.tgq.script;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registration of different actions((byte) 0x00), as defined in "_kcActionID".
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
@AllArgsConstructor
public enum kcActionID {
    NONE((byte) 0x00), // kcCEntity::OnCommand
    STOP((byte) 0x01), // // kcCActorBase::ProcessAction
    ACTIVATE((byte) 0x02, kcParamType.BOOLEAN, "activate"), // kcCEntity::OnCommand
    ENABLE((byte) 0x03), // TODO
    TERMINATE((byte) 0x04), // kcCActorBase::ProcessAction, kcCEntity::OnCommand
    INITFLAGS((byte) 0x05, kcParamType.INT, "flagId"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand kcCEntity::OnCommand, kcCActor::OnCommand
    SETFLAGS((byte) 0x06, kcParamType.INT, "flagId"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCEntity::OnCommand, kcCActor::OnCommand
    CLEARFLAGS((byte) 0x07, kcParamType.INT, "flagId"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCEntity::OnCommand, kcCActor::OnCommand
    SETSTATE((byte) 0x08), // kcCActorBaseMsg::OnCommand TODO: Hmm, this feels like it should have arguments.
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
    WAIT_SEQUENCE((byte) 0x1C), // TODO
    LOOP((byte) 0x1D, kcParamType.INT, "loopCount"), // kcCActorBase::ProcessAction
    IMPULSE((byte) 0x1E, kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z"), // kcCActorBase::ProcessAction, kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    DAMAGE((byte) 0x1F, kcParamType.UNSIGNED_INT, "weaponMask", kcParamType.INT, "attackStrength"), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    SEEK((byte) 0x20), // TODO
    PERSUE((byte) 0x21), // TODO
    OFFSET_PERSUE((byte) 0x22), // TODO
    ARRIVAL((byte) 0x23), // TODO
    FLEE((byte) 0x24), // TODO
    EVADE((byte) 0x25), // TODO
    WANDER((byte) 0x26), // TODO
    CONTAINMENT((byte) 0x27), // TODO
    FLOCK((byte) 0x28), // TODO
    ENEMY_REACT((byte) 0x29), // TODO
    OBSTACLE_AVOID((byte) 0x2A), // TODO
    WALL_FOLLOW((byte) 0x2B), // TODO
    PATH_FOLLOW((byte) 0x2C), // TODO
    FLOWFIELD_FOLLOW((byte) 0x2D), // TODO
    LEADER_FOLLOW((byte) 0x2E), // TODO
    PROMPT((byte) 0x2F, kcParamType.HASH, "hRes"), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand TODO: Unconfirmed.
    DIALOG((byte) 0x30, kcParamType.HASH, "hRes"), // kcCActorBaseMsg::OnCommand, kcCActor::OnCommand
    COMPLETE((byte) 0x31), // TODO
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
    ATTACH_SENSOR((byte) 0x41), // CCharacter::OnCommand TODO
    DETACH_SENSOR((byte) 0x42, kcParamType.INT, "mustBe3", kcParamType.HASH, "hOwner"), // CCharacter::OnCommand
    ATTACH((byte) 0x43), // CCharacter::OnCommand TODO
    DETACH((byte) 0x44, kcParamType.INT, "mustBe3", kcParamType.HASH, "hOwner"), // CCharacter::OnCommand
    ACTIVATE_SPECIAL((byte) 0x45, kcParamType.BOOLEAN, "activate", kcParamType.ANY, "value_has_different_purpose_for_different_entities"); // kcCEntity::OnCommand

    private final byte opcode;
    private final kcParamType[] parameterTypes;
    private final String[] parameterNames;
    private final int parameterCount;

    kcActionID(byte opcode) {
        this.opcode = opcode;
        this.parameterTypes = new kcParamType[0];
        this.parameterNames = new String[0];
        this.parameterCount = 0;
    }

    kcActionID(byte opcode, kcParamType firstArgType, String firstArgName) {
        this.opcode = opcode;
        this.parameterTypes = new kcParamType[]{firstArgType};
        this.parameterNames = new String[]{firstArgName};
        this.parameterCount = 1;
    }

    kcActionID(byte opcode, kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName) {
        this.opcode = opcode;
        this.parameterTypes = new kcParamType[]{firstArgType, secondArgType};
        this.parameterNames = new String[]{firstArgName, secondArgName};
        this.parameterCount = 2;
    }

    kcActionID(byte opcode, kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName, kcParamType thirdArgType, String thirdArgName) {
        this.opcode = opcode;
        this.parameterTypes = new kcParamType[]{firstArgType, secondArgType, thirdArgType};
        this.parameterNames = new String[]{firstArgName, secondArgName, thirdArgName};
        this.parameterCount = 3;
    }

    kcActionID(byte opcode, kcParamType firstArgType, String firstArgName, kcParamType secondArgType, String secondArgName, kcParamType thirdArgType, String thirdArgName, kcParamType fourthArgType, String fourthArgName) {
        this.opcode = opcode;
        this.parameterTypes = new kcParamType[]{firstArgType, secondArgType, thirdArgType, fourthArgType};
        this.parameterNames = new String[]{firstArgName, secondArgName, thirdArgName, fourthArgName};
        this.parameterCount = 4;
    }
}
