package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInheritanceGroup;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A registration of different actions((byte) 0x00), as defined in "_kcActionID".
 * Created by Kneesnap on 6/26/2023.
 */
@AllArgsConstructor
public enum kcActionID {
    NONE((byte) 0x00, "DoNothing", kcEntityInheritanceGroup.ENTITY, false, kcActionEmptyTemplate::new), // kcCEntity::OnCommand
    STOP((byte) 0x01, "EndScript", null, true, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction
    ACTIVATE((byte) 0x02, "SetActive", kcEntityInheritanceGroup.ENTITY, false, kcActionActivate::new), // kcCEntity::OnCommand
    ENABLE((byte) 0x03, "SetEnable_UNIMPLEMENTED", kcEntityInheritanceGroup.ENTITY, false), // I was unable to actually find where this is implemented, though kcCEntity::OnCommand does check that it's NOT this action.
    TERMINATE((byte) 0x04, "TerminateEntity", kcEntityInheritanceGroup.ENTITY, true, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction, kcCEntity::OnCommand
    INIT_FLAGS((byte) 0x05, "InitFlags", kcEntityInheritanceGroup.ENTITY, true, kcActionFlag::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand kcCEntity::OnCommand
    SET_FLAGS((byte) 0x06, "SetFlags", kcEntityInheritanceGroup.ENTITY, true, kcActionFlag::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand
    CLEAR_FLAGS((byte) 0x07, "ClearFlags", kcEntityInheritanceGroup.ENTITY, true, kcActionFlag::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand
    SET_STATE((byte) 0x08, "SetState_DoNothing", kcEntityInheritanceGroup.ACTOR_BASE, false), // kcCActorBase::OnCommand/kcCActor::OnCommand NOTE: This is unused, and while the name suggests it should have parameters, none appear implemented.
    SET_TARGET((byte) 0x09, "SetTarget", kcEntityInheritanceGroup.ENTITY, false, kcActionSetTarget::new), // kcCEntity::OnCommand
    SET_SPEED((byte) 0x0A, "SetAnimationSpeed", kcEntityInheritanceGroup.ACTOR_BASE, true, kcActionSetSpeed::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand
    SET_POSITION((byte) 0x0B, "SetAxisPosition", kcEntityInheritanceGroup.ENTITY3D, false, kcActionSetPosition::new), // kcCEntity3D::OnCommand
    SET_POSITION_XYZ((byte) 0x0C, "SetPosition", kcEntityInheritanceGroup.ENTITY3D, false, kcActionSetPositionXyz::new), // kcCEntity3D::OnCommand
    ADD_POSITION((byte) 0x0D, "AddToAxisPosition", kcEntityInheritanceGroup.ENTITY3D, false, kcActionLazyTemplate.ADD_POSITION_ARGUMENTS), // kcCEntity3D::OnCommand
    ADD_POSITION_XYZ((byte) 0x0E, "AddPosition", kcEntityInheritanceGroup.ENTITY3D, false, kcActionLazyTemplate.ADD_POSITION_XYZ_ARGUMENTS), // kcCEntity3D::OnCommand
    SET_ROTATION((byte) 0x0F, "SetAxisRotation", kcEntityInheritanceGroup.ENTITY3D, false, kcActionLazyTemplate.SET_ROTATION_ARGUMENTS), // kcCEntity3D::OnCommand
    SET_ROTATION_XYZ((byte) 0x10, "SetRotation", kcEntityInheritanceGroup.ENTITY3D, false, kcActionLazyTemplate.SET_ROTATION_XYZ_ARGUMENTS), // kcCEntity3D::OnCommand
    ADD_ROTATION((byte) 0x11, "AddToAxisRotation", kcEntityInheritanceGroup.ENTITY3D, true, kcActionLazyTemplate.ADD_ROTATION_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ADD_ROTATION_XYZ((byte) 0x12, "AddRotation", kcEntityInheritanceGroup.ENTITY3D, true, kcActionLazyTemplate.ADD_ROTATION_XYZ_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_RIGHT((byte) 0x13, "RotateRight", kcEntityInheritanceGroup.ENTITY3D, true, kcActionLazyTemplate.ROTATE_RIGHT_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_LEFT((byte) 0x14, "RotateLeft", kcEntityInheritanceGroup.ENTITY3D, true, kcActionLazyTemplate.ROTATE_LEFT_ARGUMENTS), // kcCActorBase::ProcessAction, kcCEntity3D::OnCommand
    ROTATE_TARGET((byte) 0x15, "LookAtTargetEntity", kcEntityInheritanceGroup.ENTITY3D, true, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity3D::OnCommand
    SET_ANIMATION((byte) 0x16, "SetAnimation", kcEntityInheritanceGroup.ACTOR_BASE, true, kcActionSetAnimation::new), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand
    SET_SEQUENCE((byte) 0x17, "SetSequence", kcEntityInheritanceGroup.ACTOR_BASE, false, kcActionSetSequence::new), // kcCActorBase::OnCommand/kcCActor::OnCommand
    WAIT((byte) 0x18, "Wait", null, true, kcActionLazyTemplate.WAIT_ARGUMENTS), // kcCActorBase::ProcessAction, Seems unused.
    WAIT_ROTATE((byte) 0x19, "WaitForAxisRotation", null, true, kcActionLazyTemplate.WAIT_ROTATE_ARGUMENTS), // kcCActorBase::ProcessAction, Seems unused.
    WAIT_ROTATE_XYZ((byte) 0x1A, "WaitForFullRotation", null, true, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction, Seems unused.
    WAIT_ANIMATION((byte) 0x1B, "WaitForAnimation", null, true, kcActionEmptyTemplate::new), // kcCActorBase::ProcessAction
    LOOP((byte) 0x1D, "Loop", null, true, kcActionLazyTemplate.LOOP_ARGUMENTS), // kcCActorBase::ProcessAction. Marks the sequence to be restarted a given number of times. This will not cause it to restart during subsequent executions.
    IMPULSE((byte) 0x1E, "ApplyImpulse", kcEntityInheritanceGroup.ACTOR_BASE, true, kcActionLazyTemplate.IMPULSE_ARGUMENTS), // kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand
    DAMAGE((byte) 0x1F, "Damage_REMAPPED", kcEntityInheritanceGroup.ACTOR_BASE, false, kcActionGiveDamage::new), // kcCActorBase::OnCommand/kcCActor::OnCommand. This isn't called directly, but GIVE_DAMAGE is automatically remapped to this command.
    PROMPT((byte) 0x2F, "Prompt", kcEntityInheritanceGroup.ACTOR_BASE, false, kcActionLazyTemplate.PROMPT_ARGUMENTS), // kcCActorBase::OnCommand/kcCActor::OnCommand NOTE: This seems unused, and we don't know for certain the argument is labelled correctly. It is implemented though. This feature can be useful to reduce code duplication however, so it makes sense to use it.
    DIALOG((byte) 0x30, "ShowDialog", kcEntityInheritanceGroup.ACTOR_BASE, false, kcActionDialog::new), // kcCActorBase::OnCommand/kcCActor::OnCommand
    SET_ALARM((byte) 0x32, "SetAlarm", kcEntityInheritanceGroup.ENTITY, true, kcActionSetAlarm::new), // kcCActorBase::ProcessAction, kcCEntity::OnCommand
    TRIGGER_EVENT((byte) 0x33, "TriggerEvent", kcEntityInheritanceGroup.ENTITY, true, kcActionTriggerEvent::new), // kcCActorBase::ProcessAction, kcCEntity::OnCommand
    PLAY_SFX((byte) 0x34, "PlaySound", kcEntityInheritanceGroup.ENTITY, false, kcActionPlaySound::new), // kcCEntity::OnCommand, kcCEntity3D::OnCommand (If kcCEntity3D, then it will be positional audio at the position of the entity, otherwise it will be played directly.)
    VARIABLE_SET((byte) 0x35, "SetVariable", kcEntityInheritanceGroup.ENTITY, false, kcActionLazyTemplate.VARIABLE_SET_ARGUMENTS), // kcCEntity::OnCommand
    VARIABLE_ADD((byte) 0x36, "AddToVariable", kcEntityInheritanceGroup.ENTITY, false, kcActionLazyTemplate.VARIABLE_ADD_ARGUMENTS), // kcCEntity::OnCommand
    NUMBER((byte) 0x37, "SendNumber", kcEntityInheritanceGroup.ENTITY, false, kcActionNumber::new), // kcCEntity::OnCommand
    PARTICLE((byte) 0x38, "SpawnParticleEffect", kcEntityInheritanceGroup.ACTOR_BASE, false, kcActionSpawnParticleEffect::new), // kcCActorBase::OnCommand/kcCActor::OnCommand
    KILL_PARTICLE((byte) 0x39, "KillParticleEffect", kcEntityInheritanceGroup.ACTOR_BASE, false, kcActionEmptyTemplate::new), // kcCActorBase::OnCommand/kcCActor::OnCommand Kills particle effect(s) spawned by the current entity. (The entity which calls SpawnParticleEffect is the entity to kill it too.)
    LAUNCHER((byte) 0x3A, "Launcher_UNUSED", kcEntityInheritanceGroup.CHARACTER, false, kcActionEmptyTemplate::new), // CCharacter::OnCommand, This opens a dialog message indicating that the Launcher command has been removed.
    WITH_ITEM((byte) 0x3B, "SendPlayerHasItem", kcEntityInheritanceGroup.PROP_OR_CHARACTER, false, kcActionLazyTemplate.WITH_ITEM_ARGUMENTS), // CCharacter::OnCommand, CProp::OnCommand
    GIVE_TAKE_ITEM((byte) 0x3C, "SetPlayerHasItem", kcEntityInheritanceGroup.PROP_OR_CHARACTER, false, kcActionSetPlayerHasItem::new), // CCharacter::OnCommand, CProp::OnCommand
    GIVE_DAMAGE((byte) 0x3D, "TakeDamage", kcEntityInheritanceGroup.ACTOR_BASE, false, kcActionGiveDamage::new), // kcCScriptMgr::FireActorEffect converts this to the 'DAMAGE' command at 0x1F and flips the arguments.
    SAVEPOINT((byte) 0x3E, "SetSavePoint", kcEntityInheritanceGroup.CHARACTER, false, kcActionLazyTemplate.SAVEPOINT_ARGUMENTS), // CCharacter::OnCommand
    ENABLE_UPDATE((byte) 0x3F, "SetUpdatesEnabled", kcEntityInheritanceGroup.ENTITY, false, kcActionEnableUpdate::new), // kcCEntity::OnCommand
    AI_SETGOAL((byte) 0x40, "SetAIGoal", kcEntityInheritanceGroup.CHARACTER, false, kcActionAISetGoal::new), // CCharacter::OnCommand
    ATTACH_SENSOR((byte) 0x41, "AttachSensor", kcEntityInheritanceGroup.CHARACTER, false, kcActionAttachSensor::new), // CCharacter::OnCommand
    DETACH_SENSOR((byte) 0x42, "DetachSensor", kcEntityInheritanceGroup.CHARACTER, false, kcActionDetachSensor::new), // CCharacter::OnCommand
    ATTACH((byte) 0x43, "Attach", kcEntityInheritanceGroup.CHARACTER, false, kcActionAttachSensor::new), // CCharacter::OnCommand
    DETACH((byte) 0x44, "Detach", kcEntityInheritanceGroup.CHARACTER, false, kcActionDetachSensor::new), // CCharacter::OnCommand
    ACTIVATE_SPECIAL((byte) 0x45, "SetWorldActive", kcEntityInheritanceGroup.ENTITY, false, kcActionActivateSpecial::new); // kcCEntity::OnCommand

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
    @Getter private final String frogLordName; // This is the displayName used for the FrogLord syntax.
    @Getter private final kcEntityInheritanceGroup actionTargetType;
    @Getter private final boolean enableForActionSequences;
    private final BiFunction<kcActionExecutor, kcActionID, kcAction> paramMaker;
    private final Function<kcActionExecutor, kcAction> maker;

    private static final Map<String, kcActionID> IDS_BY_COMMAND_NAME = new HashMap<>();

    kcActionID(byte opcode, String frogLordName, kcEntityInheritanceGroup actionTargetType, boolean enableForActionSequences) {
        this.opcode = opcode;
        this.frogLordName = frogLordName;
        this.actionTargetType = actionTargetType;
        this.enableForActionSequences = enableForActionSequences;
        this.paramMaker = null;
        this.maker = null;
    }

    kcActionID(byte opcode, String frogLordName, kcEntityInheritanceGroup actionTargetType, boolean enableForActionSequences, BiFunction<kcActionExecutor, kcActionID, kcAction> maker) {
        this.opcode = opcode;
        this.frogLordName = frogLordName;
        this.actionTargetType = actionTargetType;
        this.enableForActionSequences = enableForActionSequences;
        this.paramMaker = maker;
        this.maker = null;
    }

    kcActionID(byte opcode, String frogLordName, kcEntityInheritanceGroup actionTargetType, boolean enableForActionSequences, Function<kcActionExecutor, kcAction> maker) {
        this.opcode = opcode;
        this.frogLordName = frogLordName;
        this.actionTargetType = actionTargetType;
        this.enableForActionSequences = enableForActionSequences;
        this.paramMaker = null;
        this.maker = maker;
    }

    kcActionID(byte opcode, String frogLordName, kcEntityInheritanceGroup actionTargetType, boolean enableForActionSequences, kcArgument[] arguments) {
        this(opcode, frogLordName, actionTargetType, enableForActionSequences, (executor, actionID) -> new kcActionLazyTemplate(executor, actionID, arguments));
    }

    /**
     * Creates a new kcAction instance for the type.
     */
    public kcAction newInstance(kcActionExecutor executor) {
        if (this.paramMaker != null) {
            return this.paramMaker.apply(executor, this);
        } else if (this.maker != null) {
            return this.maker.apply(executor);
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

    /**
     * Gets an action by its FrogLord-syntax command name.
     * @param commandName the name of the command to lookup
     * @return actionId, or null.
     */
    public static kcActionID getActionByCommandName(String commandName) {
        if (commandName == null)
            throw new NullPointerException("commandName");

        return IDS_BY_COMMAND_NAME.get(commandName);
    }

    static {
        // Ensure entries are sorted by opcode, so we can verify binary search will work.
        for (int i = 1; i < values().length; i++)
            if ((values()[i - 1].getOpcode() & 0xFF) >= (values()[i].getOpcode() & 0xFF))
                throw new RuntimeException("kcActionID is not sorted by opcode, " + values()[i].name() + " is out of order!");

        for (int i = 0; i < values().length; i++) {
            kcActionID actionID = values()[i];
            if (actionID.getFrogLordName() == null)
                continue;

            kcActionID oldID = IDS_BY_COMMAND_NAME.put(actionID.getFrogLordName(), actionID);
            if (oldID != null)
                throw new RuntimeException("Both kcActionID." + oldID.name() + " and kcActionID." + actionID.name() + " share the command name '" + actionID.getFrogLordName() + "'.");
        }
    }
}