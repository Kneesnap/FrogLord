package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Represents kcActions which run commands on flags.
 * TODO: Label each of the flag bits, so we don't have magic numbers.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionFlag extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.ANY, "flagId");

    public kcActionFlag(GreatQuestChunkedFile chunkedFile, kcActionID action) {
        super(chunkedFile, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    public enum kcEntityFlagType {
        ENTITY,
        ENTITY3D
    }

    /*@Getter
    @AllArgsConstructor
    public enum kcEntityFlag { // TODO: We can try to do this in-game I guess.
        // kcCEntity Default Flags: 0x40000000
        // kcCEntity3D Default 3D Flags: 0xC
        // kcCActorBase Default Flags: 0x242000

        // This flag is forcibly removed in kcCEmitter::Spawn, presumably to make it only spawn once.
        // If this is not set, it will not render the shadow in sRenderGrouped
        // kcCEntity::OnActivate sets this to match the activation state. Eg: If activated, this flag is present.
        ACTIVE(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_0), // 0x1

        // kcCActorBase::Update verifies this be 1 on an entity in order to resolve collision.
        UNKNOWN_ENTITY_0(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_0), // 0x1

        // Set by kcCEntity::OnTerminate, which will run kcCGameSystem::DestroyInstance, which sets up a callback.
        // When the callback is run, it will unregister the entity process from kcCProcMgr, reset Flag 30, then run the destructor.
        // This indicates the flag must be checked to test against removing the entity from the global list first.
        // Seems to indicate if the entity has been terminated.
        UNKNOWN_1(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_1), // 0x2 TODO: NAME

        // Checked by: kcCEntity3D::BuildLcsToWcs
        // If the kcCProxyCapsule is marked as terminated in kcCActorBase::TerrainTrack, the collision will be resolved then the entity will be placed at the position of the proxy.
        // Strangely, it seems that kcCProxyCapsule::ResolveCollisionIteration shows that if this flag is unset, collision is impossible to occur.
        // kcCActorBase::OnActivate provides better insight. This is some kind of activation flag.
        UNKNOWN_ENTITY_1(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_1), // 0x2

        // If this is set, it will not render the shadow in sRenderGrouped
        // Setting or clearing this flag will run OnActivate(true/false).
        // kcCEntity::MimicActivateUsingHideFlag
        HIDE(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_2), // 0x4 - Prevents rendering & collision.

        // This flag is enabled by default for kcCEntity3D.
        // This flag must be set for the shadow to render in sRenderGrouped.
        // In kcCActorBase::UpdateMotion, having this bit set changes other flag behavior. (I think this snaps to terrain? Not sure)
        // In kcCActorBase::Update, only if this bit is set does TerrainTrack run.
        // Does something I currently don't understand in kcCActorBase::Animate when set.
        // Seems to automatically be enabled in CCharacter::Update if a kcCSkeleton is queryable.
        ENABLE_TERRAIN_TRACK(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_2), // 0x4

        // Running kcCActorBase::EvaluateTransform will wipe this bit, but running SetPosition or SetRotation will re-apply it.
        // 0x8 - default
        // None

        // Applied by kcCEntity3D::Reset, kcCEntity3D has this set by default
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
        DISABLE_IF_PLAYER_NOT_NEARBY(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_7), // 0x80 TODO: APPLIED BY ???.

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
        UNKNOWN_11(Constants.BIT_FLAG_11), // 0x800 TODO: APPLIED BY ???.

        // This is checked by kcCActorBase::UpdateMotion, and if it is not set, the function will exit and motion updates will not occur.
        // This is checked by kcCActorBase::OnImpulse, and if it's missing, the impulse will not occur.
        UNKNOWN_12(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_12), // 0x1000 TODO: APPLIED BY ???.

        // This flag as used in kcCActorBase::TerrainTrack appears to prevent snapping the entity to the surface in some situations.
        // kcCOctTreeSceneMgr::RenderProjectedTextureShadow -> This is skipped if this flag is set. (Shadows not rendered)
        //
        UNKNOWN_14(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_14), // 0x4000 TODO: APPLIED BY ???.

        // Appears to disable animation in kcCActorBase::Animate if this bit is set.
        DISABLE_ANIMATIONS(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_15), // 0x8000 TODO: APPLIED BY ???.

        // kcCEntity::OnHide will set both this flag and the HIDE flag to either true or false together, so these are likely related.
        // If this is set, it will not render the shadow in sRenderGrouped
        // Setting this flag causes kcCActorBase::Render to call RenderDebug() for the entity.
        // Setting this flag appears to cause CProp::Render() to not occur, unless there's no attached matrix.
        RENDER_DEBUG(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_16), // 0x10000

        // kcCEntity::OnActivate shows this flag is set TRUE when deactivated, and FALSE when activated, or rather that it's enabling / disabling collision.
        // If this is set true, the entity collision will be active. Reference: kcCActorBase::ProcessAction or kcCActorBase::Reset
        COLLIDEABLE(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_16), // 0x10000 TODO: FIX NAME

        // This is checked by kcCActorBase::UpdateMotion.
        // It will force the update PROCESS_REGISTERED flag (flag 30/40000000) to be the inverse of this state. So if this flag is set, that other flag will become unset.
        // Appears to cause velocity / motion to calculate in the direction the entity is facing, at the entity's speed.
        // Having this bit set in kcCActorBase::TerrainTrack appears to constantly move Frogger half the distance between him and the terrain surface.
        UNKNOWN_17(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_17), // 0x20000 TODO: APPLIED BY ???.

        // CFrogCtl::AutoTarget - The CCharacter the MAGICSTONE_ICE was thrown at must have this flag (OR Bit Flag 21) to take the ice damage.
        // kcCActorBase::OnCommand - If the target entity has this flag, the DAMAGE command will work. Otherwise it won't.
        //
        DAMAGE_TAKER(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_18), // 0x40000 TODO: APPLIED BY ???.
        UNKNOWN_19(Constants.BIT_FLAG_19), // 0x80000 TODO: APPLIED BY ???.
        UNKNOWN_20(Constants.BIT_FLAG_20), // 0x100000 TODO: APPLIED BY ???.

        // CFrogCtl::AutoTarget - The CCharacter the MAGICSTONE_ICE was thrown at must have this flag (OR Bit Flag 18)  to take the ice damage.
        // Also in AutoTarget, it appears to set the position of the unused question mark at the position of the entity targetted for an attack. TODO: This correct?
        // CFrogCtl::OnBeginAction requires that the target entity have this flag in order for interaction / dialog to be possible
        INTERACTABLE(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_21), // 0x200000 TODO: APPLIED BY ???.
        UNKNOWN_22(Constants.BIT_FLAG_22), // 0x400000 TODO: APPLIED BY ???.
        UNKNOWN_23(Constants.BIT_FLAG_23), // 0x800000 TODO: APPLIED BY ???.
        UNKNOWN_24(Constants.BIT_FLAG_24), // 0x1000000 TODO: APPLIED BY ???.
        UNKNOWN_25(Constants.BIT_FLAG_25), // 0x2000000 TODO: APPLIED BY ???.
        UNKNOWN_26(Constants.BIT_FLAG_26), // 0x4000000 TODO: APPLIED BY ???. UpdateMotion?
        UNKNOWN_27(Constants.BIT_FLAG_27), // 0x8000000 TODO: APPLIED BY ???.
        UNKNOWN_28(Constants.BIT_FLAG_28), // 0x10000000 TODO: APPLIED BY ???.

        // Settable from the set flag command.
        UNKNOWN_29(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_29), // 0x20000000 TODO: APPLIED BY ???.

        // kcCGameSystem::Remove Removes this flag as it the entity from the kcCProcMgr, but remove the entity from the global entity list.
        // Inversely, kcCGameSystem::Insert sets this flag to true as it adds the entity to kcCProcMgr. This also has no bearing on the global entity list.
        // kcCProcMgr is responsible for firing callbacks setup in the ::Submit method. These are hooks, and are the things that will trigger stuff like "DialogUpdate", etc.
        // This is largely used for system entities, like the HudMgr entity, dialog entity, but also things like updating the entity 3D positions.
        // kcCGameSystem::CreateInstance will automatically register the entity if "bInsert" is true. It will also ALWAYS add the entity to the global entity list.
        // kcCResourceEntityInst::Prepare creates the instance and inserts it into the game system ONLY if QueryInterface "kcCParticleEmitter" returns nothing.
        // The ENABLE_UPDATE script command eventually reaches kcCEntity::OnEnableUpdate, which appears to also control this flag, but only pause / resume the process instead of fully removing/re-adding
        PROCESS_REGISTERED(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_30), // 0x40000000 This flag represents whether the entity is considered to be active (updates are enabled).

        // If this bit is set, kcCEntity::Update and kcCActorBase::UpdateMotion will skip their logic.
        // This is likely used for position resets.
        UNKNOWN_31(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_31), // 0x80000000 TODO: APPLIED BY ???.

        // Tested at kcCActorBase::UpdateMotion, where if this is set, a certain calculation is skipped.
        // Tested at CCharacter::GoingToWaypoint
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
        // BIT_FLAG_1 (2) has something to do with activation.

        // ENTITY:
        HIDE(Constants.BIT_FLAG_0, Constants.BIT_FLAG_2), // 0x1 -> 0x4
        UNKNOWN_ENTITY_1(Constants.BIT_FLAG_1, Constants.BIT_FLAG_29), // 0x2 -> 0x20000000
        UNKNOWN_ENTITY_14(Constants.BIT_FLAG_14, Constants.BIT_FLAG_7), // 0x4000 -> 0x80

        // ENTITY3D:
        UNKNOWN_ENTITY3D_2(Constants.BIT_FLAG_2, Constants.BIT_FLAG_1), // 0x4 -> 0x2
        UNKNOWN_ENTITY3D_3(Constants.BIT_FLAG_3, Constants.BIT_FLAG_2), // 0x8 -> 0x4
        UNKNOWN_ACTOR_4(Constants.BIT_FLAG_4, Constants.BIT_FLAG_0), // 0x10 -> 0x1
        UNKNOWN_ENTITY3D_5(Constants.BIT_FLAG_5, Constants.BIT_FLAG_5), // 0x20 -> 0x20
        UNKNOWN_ACTOR_6(Constants.BIT_FLAG_6, Constants.BIT_FLAG_2), // 0x40 -> 0x4
        UNKNOWN_ACTOR_7(Constants.BIT_FLAG_7, Constants.BIT_FLAG_15), // 0x80 -> 0x800 TODO: This is applied to Frogger, but we dunno what it is. This may disable movement control, as it's removed when the camera is done. We could test by removing this flag.
        COLLISION_ENABLED(Constants.BIT_FLAG_8, Constants.BIT_FLAG_16), // 0x100 -> 0x10000
        UNKNOWN_ACTOR_9(Constants.BIT_FLAG_9, Constants.BIT_FLAG_17), // 0x200 -> 0x20000
        DAMAGE_TAKER(Constants.BIT_FLAG_10, Constants.BIT_FLAG_18), // 0x400 -> 0x40000
        UNKNOWN_ACTOR_11(Constants.BIT_FLAG_11, Constants.BIT_FLAG_19), // 0x800 -> 0x80000
        UNKNOWN_ACTOR_12(Constants.BIT_FLAG_12, Constants.BIT_FLAG_20), // 0x1000 -> 0x100000
        INTERACTABLE(Constants.BIT_FLAG_13, Constants.BIT_FLAG_21), // 0x2000 -> 0x200000
        UNKNOWN_ACTOR_16(Constants.BIT_FLAG_16, Constants.BIT_FLAG_31), // 0x10000 -> 0x80000000
        UNKNOWN_ACTOR_17(Constants.BIT_FLAG_17, Constants.BIT_FLAG_30), // 0x20000 -> 0x40000000
        UNKNOWN_ACTOR_18(Constants.BIT_FLAG_18, Constants.BIT_FLAG_29); // 0x40000 -> 0x20000000

        private final int instanceBitFlagMask;
        private final int entityBitFlagMask;
    }*/
}