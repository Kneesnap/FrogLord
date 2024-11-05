package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents kcActions which run commands on flags.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionFlag extends kcAction {
    private int flagMask;
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HEX_INTEGER, "flagMask");

    public kcActionFlag(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        return 0; // All of them are flags.
    }

    @Override
    public void load(kcParamReader reader) {
        this.flagMask = reader.next().getAsInteger();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.flagMask);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.flagMask = kcEntityInstanceFlag.getValueFromArguments(arguments);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        kcEntityInstanceFlag.addFlags(this.flagMask, arguments);
    }

    @Getter
    public enum kcEntityFlagType {
        ENTITY(kcEntityFlag.HIDE, kcEntityFlag.DISABLE_IF_PLAYER_NOT_NEARBY),
        ENTITY3D(kcEntityFlag.DISABLED, kcEntityFlag.HIDE, kcEntityFlag.HAS_TARGET),
        ACTOR_BASE(kcEntityFlag.ACTIVE, kcEntityFlag.HIDE, kcEntityFlag.DISABLE_MOVEMENT, kcEntityFlag.COLLISION_ENABLED,
                kcEntityFlag.UNKNOWN_17, kcEntityFlag.DAMAGE_TAKER,
                kcEntityFlag.UNKNOWN_19, kcEntityFlag.UNKNOWN_20, kcEntityFlag.INTERACT_ENABLED); // This is wrong.

        private final kcEntityFlag[] initClearFlags;

        kcEntityFlagType(kcEntityFlag... initClearFlags) {
            this.initClearFlags = initClearFlags;
        }
    }

    @Getter // TODO: Probably move this to the flag description place.
    @AllArgsConstructor
    public enum kcEntityFlag { // TODO: We can try to do this in-game I guess.
        // kcCEntity Default Flags: 0x40000000
        // kcCEntity3D Default 3D Flags: 0xC
        // kcCActorBase Default Flags: 0x242000

        // This flag is forcibly removed in kcCEmitter::Spawn, presumably to make it only spawn once.
        // If this is not set, it will not render the shadow in sRenderGrouped
        // kcCEntity::OnActivate sets this to match the activation state. Eg: If activated, this flag is present.
        // kcCActorBase::Update verifies this be 1 on an entity in order to resolve collision.
        ACTIVE(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_0), // 0x1

        // Set by kcCEntity::OnTerminate, which will run kcCGameSystem::DestroyInstance, which sets up a callback.
        // When the callback is run, it will unregister the entity process from kcCProcMgr, reset Flag 30, then run the destructor.
        // This indicates the flag must be checked to test against removing the entity from the global list first.
        // Seems to indicate if the entity has been terminated.
        // Checked by: kcCEntity3D::BuildLcsToWcs
        // If the kcCProxyCapsule is marked as terminated in kcCActorBase::TerrainTrack, the collision will be resolved then the entity will be placed at the position of the proxy.
        // Strangely, it seems that kcCProxyCapsule::ResolveCollisionIteration shows that if this flag is unset, collision is impossible to occur.
        // kcCActorBase::OnActivate provides better insight. This is some kind of activation flag.
        // Frogger has this flag removed by CFrogCtl:":Execute() whenever kcCDialog::mbActive is false.
        // So perhaps this is a little unusual.
        // This is also applied to things during Bog Town. I think this turns off the AI of the Bees, who would otherwise attack Frogger. Or FFM who would otherwise fly off?
        DISABLED(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_1), // 0x2

        // If this is set, it will not render the shadow in sRenderGrouped
        // Setting or clearing this flag will run OnActivate(true/false).
        // kcCEntity::MimicActivateUsingHideFlag
        // kcCEntity::MapInstanceFlags() will remove this flag when performing INITFLAGS.
        HIDE(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_2), // 0x4 - Prevents rendering & collision. TODO: The scripts seem to set this bit when they want to show the entity, not set when they want to hide it. I'd like to investigate this further and see if this might be a visible flag, not a hidden flag.

        // This flag is enabled by default for kcCEntity3D. TODO: Really??
        // This flag must be set for the shadow to render in sRenderGrouped. TODO: Really???
        // In kcCActorBase::UpdateMotion, having this bit set changes other flag behavior. (I think this snaps to terrain? Not sure)
        // In kcCActorBase::Update, only if this bit is set does TerrainTrack run.
        // Does something I currently don't understand in kcCActorBase::Animate when set.
        // Seems to automatically be enabled in CCharacter::Update if a kcCSkeleton is queryable.
        ENABLE_TERRAIN_TRACK(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_2), // 0x4

        // Running kcCActorBase::EvaluateTransform will wipe this bit, but running SetPosition or SetRotation will re-apply it.
        // Applied by kcCEntity3D::Reset, kcCEntity3D has this set by default
        // Though... kcCActor::OnDamage() also sets this when the entity dies or takes damage. TODO: Hmmm.
        HAS_POSITION(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_3), // 0x8

        // This must be true for kcCEntity::Update to run the "Execute" function.
        // This is likely some kind of activation state, but it's currently unclear what controls it.
        // This bit is automatically set to false if BIT 31 ENTITY is unset in kcCActorBase::Update.
        // If bit 31 ENTITY is set, it will be true if the finite state machine property on the entity is not null.
        // CCharacter::Init will also set this for PLAYER characters.
        UNKNOWN_4(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_4), // 0x10 TODO: FINISH / better name.

        // Tracks if the entity has a target entity set.
        // Assignments: kcCEntity::ResetInt, kcCEntity3D::OnSetTarget, kcCEntity::OnCommand() for id 9.
        HAS_TARGET(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_5), // 0x20

        // Tracks if the entity should be registered as a waypoint.
        // This is assigned by kcCActorBase::UpdateMotion when there is no target, and you're .03 distance units away from the ground.
        // Tests: kcCEntity3D::Reset
        // Seems to be re-used later for something velocity related.
        // If this is TRUE, SeekRotationToTarget, this will change how it looks at another entity.
        // Also set to true in kcCActorBase::OnImpulse
        REGISTER_AS_WAYPOINT(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_5), // 0x20

        // Assigned to the spawned entity by kcCEmitter::Spawn
        SPAWNED_BY_EMITTER(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_6), // 0x40

        // This is assigned by kcCActorBase::UpdateMotion when there is no target, and you're .03 distance units away from the ground. (Falling)
        FALLING_FAR_FROM_GROUND(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_6), // 0x40

        // Unknown.
        // sWaypointDeactivateEntities appears to be a callback for an OctTree search.
        // It appears to accept entity references, and it requires this flag to NOT be set on the target entity to deactivate it.
        // It also skips waypoint entities.
        // In kcCActorBase::Update, having this bit NOT set with there being at least 60 frames since the last "touch", it will auto-disable updates.
        // Occurs in CCharacter::OnCommand for ATTACH_SENSOR/ATTACH, not sure why.
        // kcCEntity::MapInstanceFlags() will remove this flag when performing INITFLAGS.
        DISABLE_IF_PLAYER_NOT_NEARBY(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_7), // 0x80 Applied by kcCEntity::MapInstanceFlags()

        // This flag appears to be cleared when an entity lands on the ground. (<= 0.03 distance units away from the ground)
        // It is set when an entity is falling in kcCActorBase::UpdateMotion and is < 1.5 distance units away from the ground.
        ALMOST_HIT_GROUND(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_7), // 0x80 TODO: APPLIED BY ???.

        // This is assigned by kcCActorBase::UpdateMotion when there is no target, and you're more than .03 distance units away from the ground.
        // This flag becomes unset when a movement update occurs with bit 0x400 set and the position change is exclusively vertical.
        // Seems to be set when the entity is not falling (on ground).
        UNKNOWN_8(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_8), // 0x100 TODO: APPLIED BY ???.

        // This flag is tested by kcCActorBase::ProcessAction, in the ROTATE_TARGET action, as well as kcCActorBase::SeekRotationToTarget.
        // It appears to change how rotation occurs so that it will always behave as if the entity was a waypoint. TODO: What actually does this change?
        // It also appears in kcCActorBase::UpdateMotion where it will force motion to update as if there was a waypoint.
        // It's also available at the end and has to do with falling / landing. This probably is some kind of motion setting.
        UNKNOWN_9(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_9), // 0x200 TODO: APPLIED BY ???.

        // This is checked by kcCActorBase::UpdateMotion
        // Appears to calculate velocity using delta-time and changes between position when unset.
        UNKNOWN_10(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_10), // 0x400 TODO: APPLIED BY ???.
        // kcCActorBase::MapInstanceFlags() -> This seems to occur at the start of Rolling Rapids Creek.
        // Further looking at CFrogCtl::Execute() seems to corroborate this, as only when TODO: !
        // CCharacter::Update() will not run AiSystem_Process() if this is set.
        DISABLE_MOVEMENT(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_11), // 0x800 TODO: !

        // This is checked by kcCActorBase::UpdateMotion, and if it is not set, the function will exit and motion updates will not occur.
        // This is checked by kcCActorBase::OnImpulse, and if it's missing, the impulse will not occur.
        UNKNOWN_12(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_12), // 0x1000 TODO: APPLIED BY ???.
        // TODO: CProp::OnDamage() -> This flag must be set in order to take damage.
        // TODO: If this flag is NOT set, and flag 31 IS set, CCharacter::Update() will abort/skip. However, the super call to kcActor::Update will still run.
        UNKNOWN_13(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_13), // 0x2000 TODO: APPLIED BY ???.

        // This flag as used in kcCActorBase::TerrainTrack appears to prevent snapping the entity to the surface in some situations.
        // kcCOctTreeSceneMgr::RenderProjectedTextureShadow -> This is skipped if this flag is set. (Shadows not rendered)
        //
        UNKNOWN_14(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_14), // 0x4000 TODO: APPLIED BY ???.

        // Appears to disable animation in kcCActorBase::Animate if this bit is set.
        // Seems to be set in CFrogCtl::Execute()
        DISABLE_ANIMATIONS(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_15), // 0x8000 TODO: APPLIED BY ???.

        // kcCEntity::OnHide will set both this flag and the HIDE flag to either true or false together, so these are likely related.
        // If this is set, it will not render the shadow in sRenderGrouped
        // Setting this flag causes kcCActorBase::Render to call RenderDebug() for the entity.
        // Setting this flag appears to cause CProp::Render() to not occur, unless there's no attached matrix.
        // kcCScriptMgr::OnActivate() keeps this flag & ACTIVE inverted.
        // kcCEntity::OnActivate shows this flag is set TRUE when deactivated, and FALSE when activated, or rather that it's enabling / disabling collision.
        // If this is set true, the entity collision will be active. Reference: kcCActorBase::ProcessAction or kcCActorBase::Reset
        // I believe this is correct since barrels remove this flag when they get destroyed.
        COLLISION_ENABLED(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_16), // 0x10000 TODO: I'm not convinced this is entirely correct, since this flag exists for non kcCEntity3Ds.

        // This is checked by kcCActorBase::UpdateMotion.
        // It will force the update PROCESS_REGISTERED flag (flag 30/40000000) to be the inverse of this state. So if this flag is set, that other flag will become unset.
        // Appears to cause velocity / motion to calculate in the direction the entity is facing, at the entity's speed.
        // Having this bit set in kcCActorBase::TerrainTrack appears to constantly move Frogger half the distance between him and the terrain surface.
        UNKNOWN_17(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_17), // 0x20000 TODO: APPLIED BY ???.

        // CFrogCtl::AutoTarget - The CCharacter the MAGICSTONE_ICE was thrown at must have this flag (OR Bit Flag 21) to take the ice damage.
        // kcCActorBase::OnCommand - If the target entity has this flag, the DAMAGE command will work. Otherwise it won't.
        // This appears correct, stuff like barrels will remove this flag when they are destroyed, so they can't be hit anymore.
        // Additionally, the crates that can't be attacked until a certain point in the tutorial have this flag set when it's time.
        DAMAGE_TAKER(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_18), // 0x40000 TODO: APPLIED BY ???.
        UNKNOWN_19(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_19), // 0x80000 TODO: APPLIED BY ???.
        UNKNOWN_20(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_20), // 0x100000 TODO: APPLIED BY ???.

        // CFrogCtl::AutoTarget - The CCharacter the MAGICSTONE_ICE was thrown at must have this flag (OR Bit Flag 18)  to take the ice damage.
        // Also in AutoTarget, it appears to set the position of the unused question mark at the position of the entity targetted for an attack. TODO: This correct?
        // CFrogCtl::OnBeginAction requires that the target entity have this flag in order for interaction / dialog to be possible
        // Looking at scripts this looks correct, stuff like barrels will remove this flag once they have been attacked.
        INTERACT_ENABLED(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_21), // 0x200000 TODO: APPLIED BY ???.

        // Settable from the set flag command. (kcCEntity::MapInstanceFlags)
        UNKNOWN_29(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_29), // 0x20000000 TODO: APPLIED BY ???.
        UNKNOWN_29_DUPLICATE(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_29), // 0x20000000 TODO: APPLIED BY ???.

        // kcCGameSystem::Remove Removes this flag as it the entity from the kcCProcMgr, but remove the entity from the global entity list.
        // Inversely, kcCGameSystem::Insert sets this flag to true as it adds the entity to kcCProcMgr. This also has no bearing on the global entity list.
        // kcCProcMgr is responsible for firing callbacks setup in the ::Submit method. These are hooks, and are the things that will trigger stuff like "DialogUpdate", etc.
        // This is largely used for system entities, like the HudMgr entity, dialog entity, but also things like updating the entity 3D positions.
        // kcCGameSystem::CreateInstance will automatically register the entity if "bInsert" is true. It will also ALWAYS add the entity to the global entity list.
        // Likewise, kcCGameSystem::DestroyInstance will call EntityDestroy() which can delete this flag.
        // kcCResourceEntityInst::Prepare creates the instance and inserts it into the game system ONLY if QueryInterface "kcCParticleEmitter" returns nothing.
        // The ENABLE_UPDATE script command eventually reaches kcCEntity::OnEnableUpdate, which appears to also control this flag, but only pause / resume the process instead of fully removing/re-adding
        // kcCScriptMgr::OnActive() -> Shows this flag is added when entity activation occurs, and removed when deactivation occurs.
        // kcCEntity::OnEnableUpdate() sets/removes this flag too, so this flag most likely indicates whether its process is registered/whether the
        PROCESS_REGISTERED(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_30), // 0x40000000 This flag represents whether the entity is considered to be active (updates are enabled).

        // If this bit is set, kcCEntity::Update and kcCActorBase::UpdateMotion will skip their logic.
        // This is likely used for position resets.
        UNKNOWN_31(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_31), // 0x80000000 TODO: APPLIED BY ???.

        // Tested at kcCActorBase::UpdateMotion, where if this is set, a certain calculation is skipped.
        // Tested at CCharacter::GoingToWaypoint & kcCActorBase::Update for terrain.
        // If this is not set for kcCActorBase::Render() (& 0x10000 is set), the entity will be skipped.
        // CCharacter::Update() will not run AiSystem_Process() if this is set.
        // TODO: This actually narrows down the flags which could indicate far away...!
        UNKNOWN_ACTOR_31(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_31); // 0x80000000 TODO: APPLIED BY ???.

        // TODO: kcCEntity::Update runs ::Execute if FSM != NULL AND FLAG 0x10 AND !!! NOT FLAG 0x80000000

        // ActorBase:
        // 0x20000 - Always snap to terrain? (TerrainTrack)
        // TODO: TerrainTrack also has 0x100 and 0x60 and 0x40 and 0x4000

        // If 0x40000 is not set, it won't take damage from scripts.

        private final kcEntityFlagType flagType;
        private final int bitFlagMask;
    }

    @Getter
    @AllArgsConstructor
    public enum kcEntityInstanceFlag {
        // ENTITY:
        HIDE(kcEntityFlagType.ENTITY, "HiddenEntity", Constants.BIT_FLAG_0, kcEntityFlag.HIDE), // 0x1 -> 0x4 <-> There are multiple targeting kcEntityFlag.HIDE because the INIT_FLAGS action will reset this flag at each inheritance level, so each flag is here to allow keeping such a thing.
        UNKNOWN_ENTITY_1(kcEntityFlagType.ENTITY, "UnknownBitFlag1", Constants.BIT_FLAG_1, kcEntityFlag.UNKNOWN_29), // 0x2 -> 0x20000000
        DISABLE_IF_PLAYER_NOT_NEARBY(kcEntityFlagType.ENTITY, "DisableIfPlayerNotNearby", Constants.BIT_FLAG_14, kcEntityFlag.DISABLE_IF_PLAYER_NOT_NEARBY), // 0x4000 -> 0x80

        // ENTITY3D:
        UNKNOWN_ENTITY3D_2(kcEntityFlagType.ENTITY3D, "UnknownBitFlag2", Constants.BIT_FLAG_2, kcEntityFlag.DISABLED), // 0x4 -> 0x2
        HIDE_ENTITY3D(kcEntityFlagType.ENTITY3D, "HiddenEntity3D", Constants.BIT_FLAG_3, kcEntityFlag.HIDE), // 0x8 -> 0x4 <-> There are multiple targeting kcEntityFlag.HIDE because the INIT_FLAGS action will reset this flag at each inheritance level, so each flag is here to allow keeping such a thing.
        UNKNOWN_ENTITY3D_5(kcEntityFlagType.ENTITY3D, "UnknownBitFlag5", Constants.BIT_FLAG_5, kcEntityFlag.HAS_TARGET), // 0x20 -> 0x20

        // ACTOR_BASE:
        ACTIVE(kcEntityFlagType.ACTOR_BASE, "Active", Constants.BIT_FLAG_4, kcEntityFlag.ACTIVE), // 0x10 -> 0x1
        HIDE_ACTOR_BASE(kcEntityFlagType.ACTOR_BASE, "HiddenActor", Constants.BIT_FLAG_6, kcEntityFlag.HIDE), // 0x40 -> 0x4 <-> There are multiple targeting kcEntityFlag.HIDE because the INIT_FLAGS action will reset this flag at each inheritance level, so each flag is here to allow keeping such a thing.
        DISABLE_MOVEMENT(kcEntityFlagType.ACTOR_BASE, "DisableMovement", Constants.BIT_FLAG_7, kcEntityFlag.DISABLE_MOVEMENT), // 0x80 -> 0x800
        COLLISION_ENABLED(kcEntityFlagType.ACTOR_BASE, "EnableCollision", Constants.BIT_FLAG_8, kcEntityFlag.COLLISION_ENABLED), // 0x100 -> 0x10000
        UNKNOWN_ACTOR_9(kcEntityFlagType.ACTOR_BASE, "UnknownBitFlag9", Constants.BIT_FLAG_9, kcEntityFlag.UNKNOWN_17), // 0x200 -> 0x20000
        DAMAGE_TAKER(kcEntityFlagType.ACTOR_BASE, "CanTakeDamage", Constants.BIT_FLAG_10, kcEntityFlag.DAMAGE_TAKER), // 0x400 -> 0x40000
        UNKNOWN_ACTOR_11(kcEntityFlagType.ACTOR_BASE, "UnknownBitFlag11", Constants.BIT_FLAG_11, kcEntityFlag.UNKNOWN_19), // 0x800 -> 0x80000
        UNKNOWN_ACTOR_12(kcEntityFlagType.ACTOR_BASE, "UnknownBitFlag12", Constants.BIT_FLAG_12, kcEntityFlag.UNKNOWN_20), // 0x1000 -> 0x100000
        INTERACT_ENABLED(kcEntityFlagType.ACTOR_BASE, "PlayerCanInteract", Constants.BIT_FLAG_13, kcEntityFlag.INTERACT_ENABLED), // 0x2000 -> 0x200000
        UNKNOWN_ACTOR_16(kcEntityFlagType.ACTOR_BASE, "UnknownBitFlag16", Constants.BIT_FLAG_16, kcEntityFlag.UNKNOWN_ACTOR_31), // 0x10000 -> 0x80000000
        PROCESS_REGISTERED(kcEntityFlagType.ACTOR_BASE, "RegisteredProcess", Constants.BIT_FLAG_17, kcEntityFlag.PROCESS_REGISTERED), // 0x20000 -> 0x40000000
        UNKNOWN_ACTOR_18(kcEntityFlagType.ACTOR_BASE, "UnknownBitFlag18", Constants.BIT_FLAG_18, kcEntityFlag.UNKNOWN_29_DUPLICATE); // 0x40000 -> 0x20000000

        private final kcEntityFlagType flagType; // The class which the instanced mapping occurs.
        private final String displayName;
        private final int instanceBitFlagMask;
        private final kcEntityFlag entityFlag;

        /**
         * Add flags to the arguments for the corresponding damage flags.
         * @param value The value to determine which flags to apply from
         * @param arguments The arguments to add the flags to
         */
        public static void addFlags(int value, OptionalArguments arguments) {
            // Write flags.
            for (int i = 0; i < values().length; i++) {
                kcEntityInstanceFlag flag = values()[i];
                if ((value & flag.getInstanceBitFlagMask()) == flag.getInstanceBitFlagMask())
                    arguments.getOrCreate(flag.getDisplayName());
            }
        }

        /**
         * Consume optional flag arguments to build a value containing the same flags as specified by the arguments.
         * @param arguments The arguments to create the value from.
         * @return flagArguments
         */
        public static int getValueFromArguments(OptionalArguments arguments) {
            int value = 0;
            for (int i = 0; i < values().length; i++) {
                kcEntityInstanceFlag flag = values()[i];
                if (arguments.useFlag(flag.getDisplayName()))
                    value |= flag.getInstanceBitFlagMask();
            }

            return value;
        }
    }
}