package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A registration of different actions((byte) 0x00), as defined in "_kcActionID".
 * Created by Kneesnap on 6/26/2023.
 */
@AllArgsConstructor
public enum kcActionID {
    NONE((byte) 0x00, kcActionEmptyTemplate::new), // kcCEntity::OnCommand
    STOP((byte) 0x01, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction
    ACTIVATE((byte) 0x02, kcActionActivate::new), // kcCEntity::OnCommand
    ENABLE((byte) 0x03), // I was unable to actually find where this is implemented.
    TERMINATE((byte) 0x04, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction, kcCEntity::OnCommand
    INIT_FLAGS((byte) 0x05, kcActionFlag::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand kcCEntity::OnCommand, kcCActor::OnCommand
    SET_FLAGS((byte) 0x06, kcActionFlag::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand, kcCEntity::OnCommand, kcCActor::OnCommand
    CLEAR_FLAGS((byte) 0x07, kcActionFlag::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand, kcCEntity::OnCommand, kcCActor::OnCommand
    SET_STATE((byte) 0x08), // kcCActorBase::OnCommand NOTE: This is unused, and while the name suggests it should have parameters, none appear implemented.
    SET_TARGET((byte) 0x09, kcActionSetTarget::new), // kcCEntity::OnCommand
    SET_SPEED((byte) 0x0A, kcActionSetSpeed::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand, kcCActor::OnCommand
    SET_POSITION((byte) 0x0B, kcActionSetPosition::new), // kcCEntity3D::OnCommand
    SET_POSITION_XYZ((byte) 0x0C, kcActionSetPositionXYZ::new), // kcCEntity3D::OnCommand
    ADD_POSITION((byte) 0x0D, kcActionLazyTemplate.ADD_POSITION_ARGUMENTS), // kcCEntity3D::OnCommand
    ADD_POSITION_XYZ((byte) 0x0E, kcActionLazyTemplate.ADD_POSITION_XYZ_ARGUMENTS), // kcCEntity3D::OnCommand
    SET_ROTATION((byte) 0x0F, kcActionLazyTemplate.SET_ROTATION_ARGUMENTS), // kcCEntity3D::OnCommand
    SET_ROTATION_XYZ((byte) 0x10, kcActionLazyTemplate.SET_ROTATION_XYZ_ARGUMENTS), // kcCEntity3D::OnCommand
    ADD_ROTATION((byte) 0x11, kcActionLazyTemplate.ADD_ROTATION_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ADD_ROTATION_XYZ((byte) 0x12, kcActionLazyTemplate.ADD_ROTATION_XYZ_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_RIGHT((byte) 0x13, kcActionLazyTemplate.ROTATE_RIGHT_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_LEFT((byte) 0x14, kcActionLazyTemplate.ROTATE_LEFT_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_TARGET((byte) 0x15, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand, kcCEntity3D::OnCommand, kcCActor::OnCommand
    SET_ANIMATION((byte) 0x16, kcActionSetAnimation::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand, kcCActor::OnCommand
    SET_SEQUENCE((byte) 0x17, kcActionLazyTemplate.SET_SEQUENCE_ARGUMENTS), // kcCActorBase::OnCommand, kcCActor::OnCommand
    WAIT((byte) 0x18, kcActionLazyTemplate.WAIT_ARGUMENTS), // kcCActorBase::ProcessAction
    WAIT_ROTATE((byte) 0x19, kcActionLazyTemplate.WAIT_ROTATE_ARGUMENTS), // kcCActorBase::ProcessAction
    WAIT_ROTATE_XYZ((byte) 0x1A, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction
    WAIT_ANIMATION((byte) 0x1B, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction
    LOOP((byte) 0x1D, kcActionLazyTemplate.LOOP_ARGUMENTS), // kcCActorBase::ProcessAction
    IMPULSE((byte) 0x1E, kcActionLazyTemplate.IMPULSE_ARGUMENTS), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand, kcCActor::OnCommand
    DAMAGE((byte) 0x1F, kcActionLazyTemplate.DAMAGE_ARGUMENTS), // kcCActorBase::OnCommand, kcCActor::OnCommand
    PROMPT((byte) 0x2F, kcActionLazyTemplate.PROMPT_ARGUMENTS), // kcCActorBase::OnCommand, kcCActor::OnCommand NOTE: This seems unused, and we don't know for certain the argument is labelled correctly. It is implemented though.
    DIALOG((byte) 0x30, kcActionLazyTemplate.DIALOG_ARGUMENTS), // kcCActorBase::OnCommand, kcCActor::OnCommand
    SET_ALARM((byte) 0x32, kcActionLazyTemplate.SET_ALARM_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity::OnCommand
    TRIGGER_EVENT((byte) 0x33, kcActionTriggerEvent::new), // kcCActorBase::ProcessAction, kcCEntity::OnCommand
    PLAY_SFX((byte) 0x34, kcActionLazyTemplate.PLAY_SFX_ARGUMENTS), // kcCEntity::OnCommand, kcCEntity3D::OnCommand
    VARIABLE_SET((byte) 0x35, kcActionLazyTemplate.VARIABLE_SET_ARGUMENTS), // kcCEntity::OnCommand
    VARIABLE_ADD((byte) 0x36, kcActionLazyTemplate.VARIABLE_ADD_ARGUMENTS), // kcCEntity::OnCommand
    NUMBER((byte) 0x37, kcActionNumber::new), // kcCEntity::OnCommand
    PARTICLE((byte) 0x38, kcActionLazyTemplate.PARTICLE_ARGUMENTS), // kcCActorBase::OnCommand, kcCActor::OnCommand
    KILL_PARTICLE((byte) 0x39, kcActionEmptyTemplate::new), // kcCActorBase::OnCommand, kcCActor::OnCommand
    LAUNCHER((byte) 0x3A, kcActionEmptyTemplate::new), // CCharacter::OnCommand
    WITH_ITEM((byte) 0x3B, kcActionLazyTemplate.WITH_ITEM_ARGUMENTS), // CCharacter::OnCommand, CProp::OnCommand
    GIVE_TAKE_ITEM((byte) 0x3C, kcActionLazyTemplate.GIVE_TAKE_ITEM_ARGUMENTS), // CCharacter::OnCommand, CProp::OnCommand
    GIVE_DAMAGE((byte) 0x3D, kcActionLazyTemplate.GIVE_DAMAGE_ARGUMENTS), // kcCScriptMgr::FireActorEffect converts this to the 'DAMAGE' command at 0x1F and flips the arguments.
    SAVEPOINT((byte) 0x3E, kcActionLazyTemplate.SAVEPOINT_ARGUMENTS), // CCharacter::OnCommand
    ENABLE_UPDATE((byte) 0x3F, kcActionEnableUpdate::new), // kcCEntity::OnCommand
    AI_SETGOAL((byte) 0x40, kcActionAISetGoal::new), // CCharacter::OnCommand
    ATTACH_SENSOR((byte) 0x41, kcActionAttachSensor::new), // CCharacter::OnCommand
    DETACH_SENSOR((byte) 0x42, kcActionDetachSensor::new), // CCharacter::OnCommand
    ATTACH((byte) 0x43, kcActionAttachSensor::new), // CCharacter::OnCommand
    DETACH((byte) 0x44, kcActionDetachSensor::new), // CCharacter::OnCommand
    ACTIVATE_SPECIAL((byte) 0x45, kcActionActivateSpecial::new); // kcCEntity::OnCommand

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
    private final Function<kcActionID, kcAction> paramMaker;
    private final Supplier<kcAction> maker;

    kcActionID(byte opcode) {
        this.opcode = opcode;
        this.paramMaker = null;
        this.maker = null;
    }

    kcActionID(byte opcode, Function<kcActionID, kcAction> maker) {
        this.opcode = opcode;
        this.paramMaker = maker;
        this.maker = null;
    }

    kcActionID(byte opcode, Supplier<kcAction> maker) {
        this.opcode = opcode;
        this.paramMaker = null;
        this.maker = maker;
    }

    kcActionID(byte opcode, kcArgument[] arguments) {
        this(opcode, actionID -> new kcActionLazyTemplate(actionID, arguments));
    }

    /**
     * Creates a new kcAction instance for the type.
     */
    public kcAction newInstance() {
        if (this.paramMaker != null) {
            return this.paramMaker.apply(this);
        } else if (this.maker != null) {
            return this.maker.get();
        } else {
            throw new RuntimeException("Failed to create new kcAction instance for kcActionID " + name() + ". (Not supported?)");
        }
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

        throw new RuntimeException("There was no kcActionID found for the opcode " + opcode + ".");
    }

    static {
        // Ensure entries are sorted by opcode so we can verify binary search will work.
        for (int i = 1; i < values().length; i++)
            if ((values()[i - 1].getOpcode() & 0xFF) >= (values()[i].getOpcode() & 0xFF))
                throw new RuntimeException("kcActionID is not sorted by opcode, " + values()[i].name() + " is out of order!");
    }
}