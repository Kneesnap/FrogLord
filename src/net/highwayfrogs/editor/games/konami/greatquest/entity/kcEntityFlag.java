package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Contains documentation of the various runtime flags "this->mFlags" held by kcCEntity and its extenders.
 * This contains more guesses than usual for Great Quest, and likely contains errors.
 * It turns out that most of these flags are not used in the game files, as the instanced flap mappings are used instead, and converted with kcCEntity::MapInstanceFlags
 * Created by Kneesnap on 11/7/2024.
 */
@Getter
@AllArgsConstructor
public enum kcEntityFlag {
    // kcCEntity Default Flags: 0x40000000
    // kcCEntity3D Default 3D Flags: 0xC
    // kcCActorBase Default Flags: 0x242000
    // There are different flag values

    // 0x1E8 flag offset -> flag value: 1.
    // This flag is forcibly removed in kcCEmitter::Spawn, presumably to make it only spawn once.

    // 0x208 (actor base) -> flag value: 1
    // kcCActorBase::Update verifies this be 1 on an entity in order to resolve collision.

    // If this is not set, it will not render the shadow in sRenderGrouped
    // kcCEntity::OnActivate sets this to match the activation state. Eg: If activated, this flag is present.
    // While inactive, the entity is not displayed, and is presumably not ticked either.
    // It appears that sRenderGrouped() is responsible for enabling entities once they get inside kcCOctTreeSceneMgr::msTouchRangeSquared.
    ACTIVE(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_0), // 0x1

    // Set by kcCEntity::OnTerminate, which will run kcCGameSystem::DestroyInstance, which sets up a callback. (Seems random but OK)
    // Checked by: kcCEntity3D::BuildLcsToWcs, GoingToWaypoint, kcCActorBase::OnActivate
    // Frogger has this flag removed by CFrogCtl::Execute() whenever kcCDialog::mbActive is false. (He stops looking at people when dialog ends.)
    // If the kcCProxyCapsule is marked as facing the nearest entity in kcCActorBase::TerrainTrack, the collision will be resolved then the entity will be placed at the position of the proxy.
    // Strangely, it seems that kcCProxyCapsule::ResolveCollisionIteration shows that if this flag is unset, collision is impossible to occur. I don't think I have that right.
    FACE_TARGET_ENTITY(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_1), // 0x2

    // This flag is enabled by default for kcCEntity3D.
    // If this is set, it will not sort in sRenderGrouped.
    // Setting or clearing this flag will run OnActivate(true/false).
    // kcCEntity::MimicActivateUsingHideFlag
    // I think the entities are still getting ticked when hidden.
    // Seems to automatically be enabled in CCharacter::Update if a kcCSkeleton is queryable on death. (Yep. The entity gets hidden when it shrinks & dies.)
    HIDE(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_2), // 0x4 - Prevents rendering & collision.

    // If this is set, it will not render the shadow in sRenderGrouped
    HIDE_SHADOW(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_2), // 0x4 - Prevents rendering & collision.

    // kcCActorBase::Animate -> Seems to change the position (caused by an animation) change based on the angle of the terrain surface.
    // kcCActorBase::UpdateMotion() -> Controls something for the 0x20 / 0x40 flags. Something about snapping to the ground?
    // kcCActorBase::Update() -> If this flag is set, TerrainTrack will occur.
    ENABLE_TERRAIN_TRACK(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_2), // 0x4 - ?

    // Running kcCActorBase::EvaluateTransform will wipe this bit, but running SetPosition or SetRotation will re-apply it.
    // Applied by kcCEntity3D::Reset, kcCEntity3D has this set by default
    // Though... kcCActor::OnDamage() also sets this when the entity dies or takes damage.
    // Seems weird in CCharacter::OnPickup and kcCEntity3D::Entity3DUpdate
    UNKNOWN(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_3), // 0x8

    // This must be true for kcCEntity::Update to run the "Execute" function.
    // This is likely some kind of activation state, but it's currently unclear what controls it.
    // This bit is automatically set to false if BIT 31 ENTITY is unset in kcCActorBase::Update.
    // If bit 31 ENTITY is set, it will be true if the finite state machine property on the entity is not null.
    // CCharacter::Init will also set this for PLAYER characters.
    HAS_ACTIVE_FSM(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_4), // 0x10

    // Tracks if the entity has a target entity set.
    // Assignments: kcCEntity::ResetInt, kcCEntity3D::OnSetTarget, kcCEntity::OnCommand() for id 9.
    HAS_TARGET_ENTITY(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_5), // 0x20

    // Entities will be registered by kcCEntity3D::Reset() if they have this flag set, and I believe this means they can interact with waypoints.
    ALLOW_WAYPOINT_INTERACTION(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_5), // 0x20

    // Also set to true in kcCActorBase::OnImpulse
    // This is assigned by kcCActorBase::UpdateMotion when there is no target, and you're .03 distance units away from the ground, along with the next flag.
    // 0x20

    // Assigned to the spawned entity by kcCEmitter::Spawn
    // This is assigned by kcCActorBase::UpdateMotion when there is no target, and you're .03 distance units away from the ground. (Falling)
    FALLING(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_6), // 0x40

    // Unknown.
    // sWaypointDeactivateEntities appears to be a callback for an OctTree search.
    // It appears to accept entity references, and it requires this flag to NOT be set on the target entity to deactivate it.
    // It also skips waypoint entities.
    // In kcCActorBase::Update, having this bit NOT set with there being at least 60 frames since the last "touch" (sRenderGrouped "touches" entities which are nearby), it will auto-disable updates.
    // Set in CCharacter::OnCommand for ATTACH_SENSOR/ATTACH.
    // kcCEntity::MapInstanceFlags() will remove this flag when performing INITFLAGS.
    FORCE_STAY_ACTIVE_WHEN_FAR_AWAY(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_7), // 0x80 Applied by kcCEntity::MapInstanceFlags()

    // It is set when an entity is falling in kcCActorBase::UpdateMotion and is < 1.5 distance units away from the ground.
    // This flag appears to be cleared when an entity lands on the ground. (<= 0.03 distance units away from the ground)
    // Some other flag with 0x80 for kcCActorBase goes here.

    // This is assigned by kcCActorBase::UpdateMotion when there is no target, and you're more than .03 distance units away from the ground.
    // This flag becomes unset when a movement update occurs with bit 0x400 set and the position change is exclusively vertical.
    // Seems to be set when the entity is not falling (on ground).
    SNAPPED_TO_GROUND(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_8), // 0x100

    // This flag is tested by kcCActorBase::ProcessAction, in the ROTATE_TARGET action, as well as kcCActorBase::SeekRotationToTarget.
    // It also appears in kcCActorBase::UpdateMotion where it will change how motion behaves.
    // CFrogCtl::OnBeginDamage() is what makes it appear this is swimming. CCharacter::Update sets this flag based on if a water surface is found.
    IS_CURRENTLY_SWIMMING(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_9), // 0x200

    // This is checked by kcCActorBase::UpdateMotion
    // Appears to calculate velocity using delta-time and changes between position when unset.
    UNKNOWN_10(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_10), // 0x400

    // kcCActorBase::MapInstanceFlags() -> This seems to occur at the start of Rolling Rapids Creek.
    // CCharacter::Update() will not run AiSystem_Process() if this is set, among other things it skips.
    // Disabling the AI does not disable facing the player, if that flag is set.
    DISABLE_AI(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_11), // 0x800

    // This is checked by kcCActorBase::UpdateMotion, and if it is not set, the function will exit and motion updates will not occur.
    // This is checked by kcCActorBase::OnImpulse, and if it's missing, the impulse will not occur.
    // CCharacter::ResetInt is what assigns this to ALL characters.
    ENABLE_MOTION(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_12), // 0x1000

    // CProp::OnDamage() -> This flag must be set in order to take damage.
    // If this flag is NOT set, and flag 31 (is player) IS set, CCharacter::Update() will abort/skip. However, the super call to kcActor::Update will still run.
    NOT_A_STATIC_MODEL(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_13), // 0x2000

    // This flag as used in kcCActorBase::TerrainTrack appears to prevent snapping the entity to the surface in some situations.
    // kcCOctTreeSceneMgr::RenderProjectedTextureShadow -> This is skipped if this flag is set. (Shadows not rendered)
    // CFrogCtl::CanJump() will skip some the surface normal check if this bit is set. (Making jumping much better!?)
    MORE_LENIENT_JUMPING_CHECKS(kcEntityFlagType.ENTITY3D, Constants.BIT_FLAG_14), // 0x4000

    // Appears to skip some calculations for animations in kcCActorBase::Animate if this bit is set.
    // Seems to be set in CFrogCtl::Execute() -> If snapping to ground is set AND there is no target.
    // I think this will prevent animations from changing the root position of the entity.
    PREVENT_ANIMATIONS_FROM_CHANGING_POSITION(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_15), // 0x8000

    // kcCEntity::OnActivate shows this flag is set TRUE when deactivated, and FALSE when activated, or rather that it's enabling / disabling collision.
    // kcCScriptMgr::OnActivate() also does this as it is a stub to kcCEntity::OnActivate.
    // This flag must NOT be set for sRenderGrouped to queue the entity shadow.
    UNKNOWN_FLAG_16(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_16), // 0x10000

    // kcCEntity::OnHide will set both this flag and the HIDE flag to either true or false together, so these are likely related.
    // If this is set, it will not render the shadow in sRenderGrouped
    // Setting this flag causes kcCActorBase::Render to call RenderDebug() for the entity.
    // Setting this flag appears to cause CProp::Render() to not occur, unless there's no attached matrix. (There's never an attached matrix unless it's a prop connected to a parent model.)
    // If this is set true, the entity collision will be active. Reference: kcCActorBase::ProcessAction or kcCActorBase::Reset
    // Barrels remove this flag when they get destroyed.
    // Confirmed via in-game testing.
    COLLISION_ENABLED(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_16), // 0x10000

    // This is checked by kcCActorBase::UpdateMotion.
    // It will force the update PROCESS_REGISTERED flag for the motion (flag 30/40000000) to be the inverse of this state. So if this flag is set, that other flag will become unset.
    // I suspect this means snapping to the ground disables motion-based updates.
    // Appears to cause velocity / motion to calculate in the direction the entity is facing, at the entity's speed.
    // Having this bit set in kcCActorBase::TerrainTrack appears to constantly move the actor half the distance between them and the terrain surface.
    ENABLE_PHYSICS(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_17), // 0x20000

    // CFrogCtl::AutoTarget - The CCharacter the MAGICSTONE_ICE was thrown at must have this flag (OR Bit Flag 21) to take the ice damage.
    // kcCActorBase::OnCommand - If the target entity has this flag, the DAMAGE command will work.
    // This appears correct, stuff like barrels will remove this flag when they are destroyed, so they can't be hit anymore.
    // Additionally, the crates that can't be attacked until a certain point in the tutorial have this flag set when it's time.
    DAMAGE_TAKER(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_18), // 0x40000
    UNUSED_19(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_19), // 0x80000 Appears to be unused, nowhere in the binary seems to reference such a flag.
    UNUSED_20(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_20), // 0x100000 Appears to be unused, nowhere in the binary seems to reference such a flag.

    // CFrogCtl::AutoTarget - The CCharacter the MAGICSTONE_ICE was thrown at must have this flag (OR Bit Flag 18)  to take the ice damage.
    // Also in AutoTarget, it appears to set the position of the unused question mark at the position of the entity targetted (for an attack?). Presumably this would have shown up if you interacted with people.
    // CFrogCtl::OnBeginAction requires that the target entity have this flag in order for interaction / dialog to be possible
    // Looking at scripts this looks correct, stuff like barrels will remove this flag once they have been attacked.
    INTERACT_ENABLED(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_21), // 0x200000

    UNUSED_29(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_29), // 0x20000000 Never appears to be checked

    // Settable from the set flag command. (kcCEntity::MapInstanceFlags)
    // Confirmed by testing in-game. It seems to be intended for non-player entities though, since the player can only jump forward in this state.
    CANNOT_DIE(kcEntityFlagType.ACTOR_BASE, Constants.BIT_FLAG_29), // 0x20000000 Seems to prevent entity death in CCharacter::Update.

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

    // Whenever kcCEntity3D::Reset() is called, if this has flag 0x8, this flag will be set, then Update() will be called, then the flag is removed?.
    // If this bit is set, kcCEntity::Update and kcCActorBase::UpdateMotion will skip their logic.
    // [SET - CCharacter doesn't take damage when IMMORTAL FROGGER IS SET] CCharacter::OnDamage will be skipped if this flag is set AND "IMMORTAL FROGGER" is disabled in the debug menu (MenuData.value == 0).
    // [Item pickup login will only run if set.] - CCharacter::OnPickup will ONLY pickup the item if this flag is set on the entity picking up the item.
    // [Upon death, the entity will only be removed from the scene if this is not set] DieCallback
    // [Respawn - Call the entity reset function if this flag is set and the state signal is 0x100000 (respawn?)] CCharacter::Update()
    // [If this flag is set and this is a static model, abort the update] CCharacter::Update()
    // [Skip AI Processing if this flag is set] CCharacter::Update()
    // [If not set, on death, spawn hit puff particles. If it IS set, spawn frog die particles.] CCharacter::Update()
    // [If look at entity flag is set, and EITHER this flag is NOT set, or DISABLE_AI is set, then look at the target while idling.] CCharacter::Update()
    // Tested at kcCActorBase::UpdateMotion, where if this is set, a certain calculation is skipped.
    // Tested at CCharacter::GoingToWaypoint & kcCActorBase::Update for terrain.
    // If this is not set for kcCActorBase::Render() (& 0x10000 is set), the entity will be skipped.
    // CCharacter::Update() will not run AiSystem_Process() if this is set.
    // Plays the respawn SFX in CCharacter::ResetInt
    // I set this flag on Frogger and all it did was cause Frogger to wander aimlessly. When he died, he dropped a coin, and didn't respawn.

    // This flag is set for many entities when the level loads via kcCEntity3D::Reset(), and to Frogger/the camera when he dies.
    RESET_ENTITY(kcEntityFlagType.ENTITY, Constants.BIT_FLAG_31); // 0x80000000

    private final kcEntityFlagType flagType;
    private final int bitFlagMask;

    @Getter
    public enum kcEntityFlagType {
        ENTITY(kcEntityInheritanceGroup.ENTITY, kcEntityFlag.HIDE, kcEntityFlag.FORCE_STAY_ACTIVE_WHEN_FAR_AWAY), // Struct offset: 0x54
        ENTITY3D(kcEntityInheritanceGroup.ENTITY3D, kcEntityFlag.FACE_TARGET_ENTITY, kcEntityFlag.HIDE, kcEntityFlag.HAS_TARGET_ENTITY), // Struct offset: 0x118
        ACTOR_BASE(kcEntityInheritanceGroup.ACTOR_BASE, kcEntityFlag.ACTIVE, kcEntityFlag.HIDE, kcEntityFlag.DISABLE_AI,
                kcEntityFlag.COLLISION_ENABLED, kcEntityFlag.ENABLE_PHYSICS, kcEntityFlag.DAMAGE_TAKER,
                kcEntityFlag.UNUSED_19, kcEntityFlag.UNUSED_20, kcEntityFlag.INTERACT_ENABLED); // Struct offset 0x208

        private final kcEntityInheritanceGroup inheritanceGroup;
        private final kcEntityFlag[] initClearFlags;

        kcEntityFlagType(kcEntityInheritanceGroup inheritanceGroup, kcEntityFlag... initClearFlags) {
            this.inheritanceGroup = inheritanceGroup;
            this.initClearFlags = initClearFlags;
        }
    }

    /**
     * Represents all available instance flags.
     * There are three different instanced flag values in-game, so the same target bit flag may be used multiple times!
     */
    @Getter
    @AllArgsConstructor
    public enum kcEntityInstanceFlag {
        // ENTITY:
        HIDE(kcEntityFlagType.ENTITY, "Hide", Constants.BIT_FLAG_0, kcEntityFlag.HIDE), // 0x1 -> 0x4 <-> There are multiple targeting kcEntityFlag.HIDE because the INIT_FLAGS action will reset this flag at each inheritance level, so each flag is here to allow keeping such a thing.
        UNUSED_01(kcEntityFlagType.ENTITY, "UnusedBitFlag01", Constants.BIT_FLAG_1, kcEntityFlag.UNUSED_29), // 0x2 -> 0x20000000 Appears unused by scripts. Makes sense since only actors can die.
        FORCE_STAY_ACTIVE(kcEntityFlagType.ENTITY, "ForceStayActive", Constants.BIT_FLAG_14, kcEntityFlag.FORCE_STAY_ACTIVE_WHEN_FAR_AWAY), // 0x4000 -> 0x80 Unused in scripts, but some entities have it set, such as an invisible FFM in the tutorial.

        // ENTITY3D:
        FACE_TARGET_ENTITY(kcEntityFlagType.ENTITY3D, "FaceTargetEntity", Constants.BIT_FLAG_2, kcEntityFlag.FACE_TARGET_ENTITY), // 0x4 -> 0x2
        HIDE_SHADOW(kcEntityFlagType.ENTITY3D, "HideShadow", Constants.BIT_FLAG_3, kcEntityFlag.HIDE_SHADOW), // 0x8 -> 0x4 <-> There are multiple targeting kcEntityFlag.HIDE because the INIT_FLAGS action will reset this flag at each inheritance level, so each flag is here to allow keeping such a thing.
        ALLOW_WAYPOINT_INTERACTION(kcEntityFlagType.ENTITY3D, "AllowWaypointInteraction", Constants.BIT_FLAG_5, kcEntityFlag.ALLOW_WAYPOINT_INTERACTION), // 0x20 -> 0x20

        // ACTOR_BASE:
        ACTIVE(kcEntityFlagType.ACTOR_BASE, "Active", Constants.BIT_FLAG_4, kcEntityFlag.ACTIVE), // 0x10 -> 0x1
        ENABLE_TERRAIN_TRACKING(kcEntityFlagType.ACTOR_BASE, "EnableTerrainTracking", Constants.BIT_FLAG_6, kcEntityFlag.ENABLE_TERRAIN_TRACK), // 0x40 -> 0x4 <-> There are multiple targeting kcEntityFlag.HIDE because the INIT_FLAGS action will reset this flag at each inheritance level, so each flag is here to allow keeping such a thing.
        DISABLE_AI(kcEntityFlagType.ACTOR_BASE, "DisableAI", Constants.BIT_FLAG_7, kcEntityFlag.DISABLE_AI), // 0x80 -> 0x800
        COLLISION_ENABLED(kcEntityFlagType.ACTOR_BASE, "EnableCollision", Constants.BIT_FLAG_8, kcEntityFlag.COLLISION_ENABLED), // 0x100 -> 0x10000
        ENABLE_PHYSICS(kcEntityFlagType.ACTOR_BASE, "EnablePhysics", Constants.BIT_FLAG_9, kcEntityFlag.ENABLE_PHYSICS), // 0x200 -> 0x20000
        DAMAGE_TAKER(kcEntityFlagType.ACTOR_BASE, "CanTakeDamage", Constants.BIT_FLAG_10, kcEntityFlag.DAMAGE_TAKER), // 0x400 -> 0x40000
        UNUSED_ACTOR_11(kcEntityFlagType.ACTOR_BASE, "UnusedBitFlag11", Constants.BIT_FLAG_11, kcEntityFlag.UNUSED_19), // 0x800 -> 0x80000 Appears unused (in scripts). I couldn't find this flag checked in the code.
        UNUSED_ACTOR_12(kcEntityFlagType.ACTOR_BASE, "UnusedBitFlag12", Constants.BIT_FLAG_12, kcEntityFlag.UNUSED_20), // 0x1000 -> 0x100000 It doesn't appear the code does anything with this flag, even if it is cleared off FFM during the tutorial.
        INTERACT_ENABLED(kcEntityFlagType.ACTOR_BASE, "PlayerCanInteract", Constants.BIT_FLAG_13, kcEntityFlag.INTERACT_ENABLED), // 0x2000 -> 0x200000
        RESET_ENTITY(kcEntityFlagType.ACTOR_BASE, "ResetEntity", Constants.BIT_FLAG_16, kcEntityFlag.RESET_ENTITY), // 0x10000 -> 0x80000000 Appears unused (in scripts)
        UPDATES_ENABLED(kcEntityFlagType.ACTOR_BASE, "UpdatesEnabled", Constants.BIT_FLAG_17, kcEntityFlag.PROCESS_REGISTERED), // 0x20000 -> 0x40000000
        PREVENT_DEATH(kcEntityFlagType.ACTOR_BASE, "PreventDeath", Constants.BIT_FLAG_18, kcEntityFlag.CANNOT_DIE); // 0x40000 -> 0x20000000

        private final kcEntityFlagType flagType; // The class which the instanced mapping occurs.
        private final String displayName;
        private final int instanceBitFlagMask;
        private final kcEntityFlag entityFlag;

        /**
         * Add flags to an optional arguments object.
         * @param value The value to determine which flags to apply from
         */
        public static OptionalArguments getAsOptionalArguments(int value) {
            OptionalArguments arguments = new OptionalArguments();
            addFlags(value, arguments);
            return arguments;
        }

        /**
         * Add flags to the arguments for the corresponding entity flags.
         * @param value The value to determine which flags to apply from
         * @param arguments The arguments to add the flags to
         */
        public static void addFlags(int value, OptionalArguments arguments) {
            // Write flags.
            for (int i = 0; i < values().length; i++) {
                kcEntityInstanceFlag flag = values()[i];
                if ((value & flag.getInstanceBitFlagMask()) == flag.getInstanceBitFlagMask()) {
                    arguments.getOrCreate(flag.getDisplayName());
                    value &= ~flag.getInstanceBitFlagMask();
                }
            }

            if (value != 0)
                Utils.getLogger().warning("kcEntityFlags.addFlags() skipped some bits! " + NumberUtils.toHexString(value));
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
