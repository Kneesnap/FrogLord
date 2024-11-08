# Great Quest Scripting (GQS)
Frogger: The Great Quest has most of its "story"/per-level occurrences managed entirely through a scripting system.
This system gives tons of flexibility to create mods since this lets us change almost all level behavior.  
This scripting system is NOT the same thing as Noodle, which is the FrogLord scripting system.
Instead, GQS/kcScript is a term we made for the scripting system found in the original Frogger: The Great Quest, as the official/original name is not known.

## The Basics
Each entity in a level can have a script, for example Frogger, coins, etc.
Each script is broken up into any number of functions, which contain the actual script behavior.
A function has two parts, the cause (conditions for running/trigger) and the effects.
An example of a script cause would be `OnLevel BEGIN`, which indicates that the function should run when the level begins.
On the other hand, effects are the commands which can impact the game, such as by playing a sound, making entities invisible, etc.  
The following example would play a sound effect when the level loads.

**Example:**  
```PowerShell
[Function]
cause=OnLevel BEGIN
PlaySound "sfx/level_start_sfx"
# More script effects could be written here if desired.
```  

Because scripts belong to individual entities, they are considered to execute/run "as" that entity.
For example, if you use the `SetSequence` effect to cause an entity to animate, by default it will apply the animation to the entity which the script is written for.
In order to specify you'd like to run it on a different entity, supply an extra flag `--AsEntity <String: entityName>`.
For example, `SetSequence "NrmIdle01" --AsEntity "FrogInst001"` will run `SetSequence` as the player character, instead of the script entity.

## Getting started by creating/modifying scripts
The easiest way to get started is by looking at examples. To export scripts from the original game, select a level in FrogLord, and find the chunk named `scriptdata`.
By right-clicking it and selecting "Export Scripts" you'll be able to specify a folder to save the scripts to.
Make sure to use a different folder for each level you export scripts for to avoid confusion and overriding files with the same name.
While these files have a `.gqs` extension, they are just text files, and can be opened in any text editor.
For Notepad++, it is recommended to set the language of these files to PowerShell, as its syntax highlighting works well.

When it's time to import your modified script back into the game, you can right-click `scriptdata` again, but this time select "Import Scripts" instead.  
This method of importing scripts should only be used for development/testing purposes, since [mods must follow this guide instead](modding-guide.md).  
TODO: Include some information on how to manage scripts with Noodle.

## Design Quirks
There is no concept of an if statement, so controlling execution flow can be a bit annoying.
Instead, any code which should only run conditionally should be split into multiple script functions, and features such as entity variables can be used to control which script function(s) are run.

### Entity Variables
Each entity has 8 variables available (accessible via IDs 0 through 7).
These variables can be set to any whole number.
They can be used as a way to control which code gets run, as the `BroadcastNumberCause` can broadcast an entity variable.  
For example:

```PowerShell
[Function]
cause=OnNumber EQUALS 1
ShowDialog "DialogOne"

[Function]
cause=OnNumber EQUALS 2
ShowDialog "DialogTwo"

[Function]
cause=... # This function's cause doesn't matter for the purpose of this example.
# This is a very basic example, but in the following function we can choose which of the two above functions we run by choosing either:
#SetVariable 0 1 # This would set Variable #0 to equal 1
#SetVariable 0 2 # This would set Variable #0 to equal 2

# This will broadcast a number cause with the number that is currently in Variable #0.
# By having two separate functions which listen for different numbers, we can control which function runs based on the variable.
# This can be used to recreate the conceptual behavior of "If X, do Y, otherwise, do Z.".
BroadcastNumberCause ENTITY_VARIABLE 0 
```

### Action Sequences
Action Sequences are a special kind of script specific to animations.
Some scripting features may only be available in action sequences, others only in normal entity scripts.
Some are available in both. When such a restriction is relevant, it will be mentioned.

## Available Causes
Causes ONLY exist for entity scripts, not for action sequences.
The following documents all causes found within the game, and how they work.  

### OnLevel
**Summary:** Executes when the level starts & ends.  
**Supported Entity Types:** All  
**Calling Functions:** `EvLevelBegin, EvLevelEnd`  
**Usage:** `OnLevel <BEGIN|END>`  

### OnPlayer
**Summary:** Executes when the player has an interaction with the script entity.  
**Supported Entity Types:** kcCActorBase+  
**Usage:** `OnPlayer <INTERACT|BUMPS|ATTACK|PICKUP_ITEM>`  
**Actions:**  
```properties
INTERACT # The player interacts with the script entity. Script entity must be at least a kcCActorBase. Code: CFrogCtl::OnBeginAction, CFrogCtl::CheckForHealthBug
BUMPS # The player collides/bumps with the script entity. Script entity must be a CProp or a CCharacter. Code: CProp::TriggerHitCallback, CCharacter::BumpCallback
ATTACK # The player attacks the script entity. Script entity must be kcCActorBase. Code: CFrogCtl::Spit, CFrogCtl::OnBeginMissile, CFrogCtl::OnBeginMagicStone, and, CFrogCtl::OnBeginMelee
PICKUP_ITEM # The player picks up the script entity as an item. Script entity must be CItemDesc. Code: CCharacter::PickupCallback
```

### OnActor
**Summary:** Executes when one of the following actions happens to the script entity.  
**Supported Entity Types:** kcCActorBase+  
**Usage:** `OnActor <BUMPS|TAKE_DAMAGE|DEATH>`  
**Actions:**  
```properties
BUMPS # Another actor bumps into the script entity. Script entity must be a CProp or a CCharacter. Code: CProp::TriggerHitCallback, CCharacter::BumpCallback
TAKE_DAMAGE # The script entity takes damage. Code: Script entity must be at least a kcCActor. kcCActor::OnDamage
DEATH # The script entity dies. Code: Script entity must be at least a kcCActor. kcCActor::OnDamage
```

### OnDamage
**Summary:** Executes when the script entity takes a certain type of damage.  
**Supported Entity Types:** kcCActorBase+  
**Calling Function(s):** `kcCActorBase::OnDamage`  
**Usage:** `OnDamage <DamageType damageType>`  
```properties
# Damage dealt can have any number of damage types.
# These damage types are directly compared against the entity's configured "immune mask".
# If the entity is immune to the given damage type, this will not run, nor will the damage be applied.
# However, if the entity is a kcCActorBase and NOT a kcCActor, there is no immunity, nor health. An entity can get damaged despite not having the concept of health.
# If an entity is not immune to the damage, and at least one of the damage types is the one an OnDamage cause is looking for, then the OnDamage caused function will execute.
# Damage Types:
FIRE # 0
ICE # 1
MELEE # 3
RANGED # 4
FALL # 12
UNNAMED_TYPE_XX # Where XX is a number between 0 and 31 and is not one of the numbers listed above.
```

### OnAlarm
**Summary:** Executes when the specified alarm expires.  
**Supported Entity Types:** All  
**Calling Function(s):** `kcCEntity::AlarmCallback`  
**Supported Entity Types:** kcCActorBase+  
**Usage:** `OnAlarm <IN_PROGRESS|FINISHED> <int alarmId>`  

### OnPrompt (Unsupported)
**Summary:** Does not work correctly.  
**Calling Function(s):** `kcCActorBase::OnCommand[PROMPT]`  
**Usage:** `OnPrompt <unused>`

### OnEventTrigger (Unsupported)
**Summary:** Supposed to execute when the specified event triggers, but it actually fires when ANY event triggers.  
**Calling Function(s):** `kcCEventMgr::Trigger`  
**Usage:** `OnEventTrigger <String eventName>`  

### OnDialog
**Summary:** Executes when the given dialog text begins or is advanced.  
**Supported Entity Types:** kcCActorBase+  
**Calling Function(s):** `EvDialogBegin, EvDialogAdvance`  
**Usage:** `OnDialog <BEGIN|ADVANCE> <String dialogStrName>`  
This event is not global, so it will only work if the entity defining this script cause is also the entity to show the dialog.  

### OnNumber
**Summary:** Executes when a number is received equal to the specified number.  
**Supported Entity Types:** All  
**Calling Function(s):** `kcCEntity::OnNumber`  
**Usage:** `OnNumber <int number>`  
This will only execute when the number is broadcasted by the script entity.
However, when `--AsEntity` is used, it will allow the calling script's entity to provide its own variable.

### OnPlayerHasItem
**Summary:** When it is broadcast whether the player has an item (via `BroadcastIfPlayerHasItem`), and whether they have the item matches the expected value.  
**Supported Entity Types:** CProp or CCharacter  
**Calling Function(s):** `CCharacter::OnWithItem, CProp::OnWithItem`  
**Usage:** `OnPlayerHasItem <bool shouldHaveItem>`  
It is not possible to specify which item to listen for, this will execute regardless of which item was given to `BroadcastIfPlayerHasItem`.  
This will only work when there is a `BroadcastIfPlayerHasItem` effect executed by the script entity.

### OnEntity
**Summary:** Executes when the script entity interacts with a waypoint.  
**Supported Entity Types:** kcCEntity3D+  
**Calling Function(s):** `kcCEntity3D::Notify, sSendWaypointStatus`  
**Usage:**  
```php
OnEntity ENTERS_WAYPOINT_AREA <kcCWaypoint waypointEntityName> # Executes when the script entity enters the area of the waypoint entity specified as an argument.
OnEntity LEAVES_WAYPOINT_AREA <kcCWaypoint waypointEntityName> # Executes when the script entity leaves the area of the waypoint entity specified as an argument.
OnEntity ENTERS_TARGET_WAYPOINT_AREA # Executes when the script entity enters a waypoint's area which appears to be the entity's target entity.
OnEntity LEAVES_TARGET_WAYPOINT_AREA # Executes when the script entity leaves a waypoint's area which appears to be the entity's target entity.
```

### OnWaypoint
**Summary:** Executes when an entity enters/leaves the waypoint script entity's area.  
**Supported Entity Types:** kcCWaypoint  
**Calling Function(s):** `sSendWaypointStatus`  
**Usage:** `OnWaypoint <ENTITY_ENTERS|ENTITY_LEAVES> <kcCEntity entityName>`  


## Available Effects
The following documentation explains all script effects/actions found within the game, and how they work.
Some of these may only work in scripts, others only in action sequences.

### DoNothing (Unsupported)
**Summary:** Does nothing, likely a test command.  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### EndScript (Unsupported)
**Summary:** Stops running an action sequence. Completely optional/unnecessary.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### SetActive (Script Only)
**Summary:** Set whether the entity is active/visible/has collision.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand -> kcCEntity::ActivateAndUnhide`  
**Usage:** `SetActive <bool markAsActive>`  
**Aliases:** `Entity.Activate, Entity.Deactivate`  

### SetEnable (Unsupported)
**Summary:** Does nothing.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### TerminateEntity (Both)
**Summary:** Remove the entity from existence.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity::OnCommand -> kcCEntity::OnTerminate`  
**Usage:** `TerminateEntity`  

### InitFlags (Both)
**Summary:** Resets instance entity flags, and then sets the flags provided.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
**Usage:** `InitFlags [Entity Flags]`  
```properties
# There are multiple targeting kcEntityFlag.HIDE because the INIT_FLAGS action will reset this flag at each inheritance level, so each flag is here to allow keeping such a thing.
# kcCEntity Flags:
--Hide # Marks the entity to not be drawn. When set, it will also activate/deactivate the entity, along with collision. However, these can then be undone afterward.
--UnusedBitFlag01 # Couldn't find any behavior tied to this flag, and it is never used by the game.
--ForceStayActive # The entity should stay active even when the player is outside the normal range.

# kcCEntity3D Flags:
--FaceTargetEntity # Causes the entity to constantly rotate to face its target entity.
--HideShadow # Hides the entity shadow. The shadow will also be hidden if --Hide is set.
--AllowWaypointInteraction # Marks an entity as needing to be registered to trigger waypoint events. This will only work if Reset() is called first, so it's best to keep this on the entity instance, and not via scripts.

# kcCActorBase Flags:
--Active # Whether the entity is active.
--EnableTerrainTracking # Makes the entity try to rotate to appear standing on the terrain (I think).
--DisableAI # Disables AI updates, so attempting to attack the player, wandering, will not occur while this flag is set.
--EnableCollision # Enables entity collision.
--EnablePhysics # Controls if physics (gravity, impulses, etc) are enabled for this entity.
--CanTakeDamage # Controls if the actor is capable of accepting damage right now. Actors without health can take damage for the purposes of handling it in a script.
--UnusedBitFlag11 # Unused, it appears this does not do anything.
--UnusedBitFlag12 # Unused, it appears this does not do anything.
--PlayerCanInteract # Allows the player to interact with the entity.
--ResetEntity # Reset the entity when it next updates. Unused.
--UpdatesEnabled # Marks an entity as needing updates, even if it may be hidden.
--PreventDeath # Makes it impossible for an entity to die, regardless of if it is capable of taking damage.
```

### SetFlags (Both)
**Summary:** Applies the provided entity flags to the entity.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
**Usage:** `SetFlags [Entity Flags]`  
See `InitFlags` above for a list of flags.  

### ClearFlags (Both)
**Summary:** Removes the provided entity flags from the entity.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
**Usage:** `ClearFlags [Entity Flags]`  
See `InitFlags` above for a list of flags.  

### SetState (Unsupported)
**Summary:** Unimplemented, does nothing.  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### SetTarget (Script Only)
**Summary:** Change the current entities target entity.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `SetTarget <kcCResourceEntityInst targetEntity>`  
A target entity is used for AI-related operations.  
For example, most enemies have `"FrogInst001"` as their target, so they attack Frogger.  
Or another example could be Fairy Frogmother, always faces Frogger because her target is `"FrogInst001"`.  

### SetAnimationSpeed (Both)
**Summary:** Sets the animation speed for the entity. (Default: 1.0?)  
**Supported Entity Types:** kcCActorBase+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetAnimationSpeed <float speed>`  
Not used in the vanilla game.

### SetPositionOnAxis (Script Only)
**Summary:** Sets the entities positional coordinate on the given axis.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `SetPositionOnAxis <Axis axis> <float coordinate>`  
Not used in the vanilla game.

### SetPosition (Script Only)
**Summary:** Sets the entity position.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `SetPosition <float x> <float y> <float z>`  

### AddPositionOnAxis (Script Only)
**Summary:** Offsets the enttiy position by the given value on the specified axis.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `AddPositionOnAxis <Axis axis> <float displacement>`  
Not used in the vanilla game.

### AddPosition (Script Only)
**Summary:** Adds values to the entities current position.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `AddPosition <float x> <float y> <float z>`  

### SetRotationOnAxis (Script Only)
**Summary:** Sets a rotation value on the specified axis.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `SetRotationOnAxis <Axis axis> <float angleInDegrees>`  
Not used in the vanilla game.

### SetRotation (Script Only)
**Summary:** Sets the entity rotation.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `SetRotation <float xAngleInDegrees> <float yAngleInDegrees> <float zAngleInDegrees>`  
Not used in the vanilla game.

### AddRotationOnAxis (Both)
**Summary:** Adds a rotation value on the specified axis.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `AddRotationOnAxis <Axis axis> <float angleInDegrees>`  
Not used in the vanilla game.

### AddRotation (Both)
**Summary:** Adds rotation values to the existing rotation.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `AddRotation <float xAngleInDegrees> <float yAngleInDegrees> <float zAngleInDegrees>`  
Not used in the vanilla game.

### RotateRight (Both)
**Summary:** Rotates the entity to look right.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `RotateRight <float angleInDegrees>`  

### RotateLeft (Both)
**Summary:** Rotates the entity to look left.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `RotateLeft <float angleInDegrees>`  

### LookAtTargetEntity (Both)
**Summary:** Make the execution entity face its target entity.  
**Supported Entity Types:** kcCEntity3D+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity3D::OnCommand`  
**Usage:** `LookAtTargetEntity`  
Not used in the vanilla game.

### SetAnimation (Both)
**Summary:** Changes the animation currently performed.  
**Supported Entity Types:** kcCActorBase+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetAnimation <kcCResourceTrack animationFileName> [--Repeat] [--FirstAnimationInSequence] [--StartTime <float startTimeInSeconds>]`  

### SetSequence (Script Only)
**Summary:** Sets the active action sequence for the entity.  
**Supported Entity Types:** kcCActorBase+  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetSequence <kcCActionSequence sequenceName> [--IgnoreIfAlreadyActive] [--OpenBoneChannel]`  
```properties
--IgnoreIfAlreadyActive
# If this is set, and the sequence we'd like to apply is already the current sequence,
# then this flag indicates that we'd like to not reset the sequence, and let the existing playback complete.
# Mrs. Boxy in Mushroom Valley really should have been configured to use this, since you can spam interact her and she'll continually restart her animation.

--OpenBoneChannel
# Appears unused in the vanilla game.
# This seems to combine the new sequence with the existing one (I think?)
# Eg: Only the bones which aren't animated on the existing sequence will be animated from the new one.
```

### Wait (Action Sequence Only)
**Summary:** Wait a given amount of time before continuing the action sequence.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `Wait <float timeInSeconds>`  
Not used in the vanilla game.

### WaitForAxisRotation (Action Sequence Only)
**Summary:** Waits for an axis rotation to complete before continuing the action sequence.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `WaitForAxisRotation <Axis axis>`  
Not used in the vanilla game.

### WaitForFullRotation (Action Sequence Only)
**Summary:** Waits for all rotations to complete before continuing the action sequence.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `WaitForFullRotation`  
Not used in the vanilla game.

### WaitForAnimation (Action Sequence Only)
**Summary:** Waits for the active animation to complete before continuing the action sequence.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `WaitForAnimation`  

### Loop (Action Sequence Only)
**Summary:** The action sequence will restart the number of times specified.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `Loop <int loopCount>`  

### ApplyMotionImpulse (Both)
**Summary:** Applies a motion "impulse" to the entity.  
**Supported Entity Types:** kcCActorBase+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `ApplyMotionImpulse <float x> <float y> <float z>`  

### Prompt (Unsupported)
**Summary:** This was never fully supported by the game, but it looks like it was supposed to allow the player to make choices within dialog text-boxes.  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `Prompt <Hash promptResourceName>`  
Not used in the vanilla game.

### ShowDialog (Script Only)
**Summary:** Creates a dialog box with text for the player.  
**Supported Entity Types:** kcCActorBase+  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `ShowDialog <String dialogResourceName>`  
The string here is the name of a generic resource string.
In other words, the actual text to display is kept in another file, and this command needs to be given the name of the file.

### SetAlarm (Both)
**Summary:** Sets an alarm to ring (Script Cause: `OnAlarm`) after a delay.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity::OnCommand`  
**Usage:** `SetAlarm <AlarmId alarmId> <float durationInSeconds> [--Repeat <int repeatCount>]`  
Any number between 0 and 31 is a valid alarm ID.
A repeat count of zero will cause the alarm to ring once.

### TriggerEvent (Both)
**Summary:** Triggers a named event.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity::OnCommand`  
**Usage:** `TriggerEvent <String eventName>`  

**Valid Events:**  
```properties
"LevelLoadComplete" # The game will load the sky box, water, setup lighting, and setup environment render states from kcEnvironment. Usually called by ExecuteLoad() completing.
"LevelBegin" # Sets up default data like coin pickup particles, the AI system, adds the system entities. Called when the level start FMV ends.
"LevelCompleted" # Destroys all active cameras, and sets a flag for completing the level. Triggered by in-game scripts.
"LevelEnd" # Stops all sound effects and broadcasts the script cause (OnLevel) for completing the level. -> Not sure what triggers this event.
"LevelUnload" # Cleanup/remove water & sky dome, stop all sounds, hide the HID, unload main menu/interface resources. Called by exiting the pause menu requesting to quit the game (PauseEndCase) or the level stops. (PlayEndCase).
"BeginScreenFade" # Causes the screen to fade to black. Called by a lot of things.
"EndScreenFade" # Unfades/unhides the contents of the screen. Called by a lot of things.
"StartMovie" # Seems to setup FMV/movie playback. Called by PlayMovieUpdate
"CutMovie" # Stops movie playback. Called when the player skips an FMV or it completes. (MovieDoneOrRequestAdvance)
"MovieContinueGame", # Seems to setup the game to continue playback. Registered in PlayMovieUpdate(). I don't think this is ever called.
"LockPlayerControl" # Disables controller/keyboard input from influencing the player character. Exclusively called from scripts. NOTE: This will be automatically be enabled when a dialog box opens, and disabled when closed.
"UnlockPlayerControl" # Re-enables controller/keyboard input for the player character. Exclusively called from scripts.
"DialogBegin" # Displays the dialog text box, and broadcasts the script cause `OnDialog BEGIN`. Called by the handler for the script command 'ShowDialog' (kcCActorBase::OnCommand).
"DialogAdvance" # Hides the dialog text box, and broadcasts the script cause `OnDialog ADVANCE`. Called by the dialog update logic (kcCDialog::Update).
"DialogEnd" # Never called, but if it were it would hide the dialog text box and broadcast the script cause `OnDialog END`. Re-enables player input.
"ShakeCameraRand" # Shakes the camera randomly. Exclusively used by scripts.
"PlayMidMovie01" # Plays the FMV "OMOVIES/MDRAGONF.PSS"/"mid_catdragon_fire.fpc" (This file does not exist in the vanilla game.) Description: "Play Dragon Fire Movie"
"PlayMidMovie02" # Plays the FMV "OMOVIES/MWITCH.PSS"/"mid_catdragon_fire.fpc" (Introduction of Big Bertha.), Description: "Play Witch Movie"
"PlayMidMovie03" # Plays the FMV "OMOVIES/MSTARK.PSS"/"mid_starkenstein.fpc" (Introduction of the Metal Chicken Ray.), Description: "Play Ckicken Emerge Movie"
"PlayMidMovie04" # Plays the FMV "OMOVIES/MCHICK.PSS"/"mid_chicken.fpc" (This file does not exist in the vanilla game.)
"PlayMidMovie05" # Plays the FMV "OMOVIES/MBATTLE.PSS"/"mid_goblinfort.fpc" (The file doesn't exist but presumably this shows the long war FMV where the cow is thrown out of a catapult.), Description: "Play Battle Movie"
"PlayMidMovie06" # Plays the FMV "OMOVIES/MTOWER.PSS"/"mid_joytower.fpc" (Shows the FMV before the fight with the general.), Description: "Play Crusher Enlarge Movie"
"PlayMidMovie07" # Plays the FMV "OMOVIES/MDEAD.PSS"/"mid_frogger_dead.fpc" (This file does not exist in the vanilla game.)
"PlayMidMovie08" # Plays the FMV "OMOVIES/PREVIEW.PSS"/"preview_frogger2.fpc" (Shows the preview of the cancelled sequel.)
"PlayMidMovie09" # Plays the FMV "OMOVIES/MDRAGONS.PSS"/"mid_catdragon_smoke.fpc" (This file does not exist in the vanilla game.), Description: "Play Dragon Smoke Movie"
"PlayMidMovie10" # Plays the FMV "OMOVIES/MCASTLE.PSS"/"mid_joycastle.fpc" (Unused video showing the general entering Joy Towers.), Description: "Play General Entering Movie"

# The following exist, but appear to do nothing:
"LevelLoad" # Does nothing, never used by the game.
"LevelUnloadComplete" # Seems unused/does nothing.
"PlayerAttack" # Never called / does nothing.
"PlayerDamage" # Never called / does nothing.
"PlayerDead" # Never called / does nothing.
"MovieCutFog", # Called by PlayMovieUpdate
"ModeAfterMovie", # Called when a movie completes. Doesn't appear to do anything.
"TracePublicEvent" # Does nothing. Seems like it was probably used for some kind of debugging purposes.
"EvStartDemoBGM" # This is registered as something that probably showed up in a UI, but nothing listens for it.
```

### PlaySound (Script Only)
**Summary:** Plays (or stops) a sound effect.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand, kcCEntity3D::OnCommand`  
**Usage:** `PlaySound <String soundFilePath> [--StopSound]`  

### SetVariable (Script Only)
**Summary:** Sets an entity variable by its ID.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `SetVariable <VariableId variableId> <int value>`  
Valid variable IDs are between 0 and 7.

### AddToVariable (Script Only)
**Summary:** Adds to an existing entity variable by its ID.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `AddToVariable <VariableId variableId> <int value>`  
Valid variable IDs are between 0 and 7.

### BroadcastNumberCause (Script Only)
**Summary:** Broadcasts a script cause (`OnNumber`) using a number.
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `BroadcastNumberCause <LITERAL_NUMBER|ENTITY_VARIABLE|RANDOM> <int number>`  
This action has special behavior (applied in `kcCScriptMgr::FireActorEvent`) when the `--AsEntity` setting is used with `ENTITY_VARIABLE`.
Unlike most cases where the effect is run entirely as the override entity, the variable value will always be obtained from the script entity.
This effectively allows for calling functions in an entity based on the variables of another entity.

```properties
# NumberOperation Values:
LITERAL_NUMBER # The number broadcasted will be the number provided as an argument.
ENTITY_VARIABLE # The number broadcasted will be the value of the entity variable at the provided ID.
RANDOM # The number broadcasted will be a random number between 0 and the number provided.
```

### SpawnParticleEffect (Script Only)
**Summary:** Sets up a particle emitter for the entity.  
**Supported Entity Types:** kcCActorBase+  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand -> kcCParticleMgr::SpawnEffect`  
**Usage:** `SpawnParticleEffect <kcParticleEmitterParam particleEmitterDataName>`  
Not used in the vanilla game.

### KillParticleEffect (Script Only)
**Summary:** Disables particle effect(s) spawned by the current entity.  
**Supported Entity Types:** kcCActorBase+  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `KillParticleEffect`  
Not used in the vanilla game.

### Launcher (Unsupported)
**Summary:** Opens up a dialog box saying it is no longer supported.  
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### BroadcastIfPlayerHasItem (Script Only)
**Summary:** Test if the player has the given item, then broadcast a script cause (`OnPlayerHasItem`) with the result.  
**Supported Entity Types:** CCharacter or CProp  
**Original Implementations:** `CCharacter::OnCommand, CProp::OnCommand`  
**Usage:** `BroadcastIfPlayerHasItem <InventoryItem item>`  
Click [here](../../../../src/net/highwayfrogs/editor/games/konami/greatquest/generic/InventoryItem.java) to see a list of InventoryItem values.

### SetPlayerHasItem (Script Only)
**Summary:** Give or take an inventory item to/from the player.  
**Supported Entity Types:** CCharacter or CProp  
**Original Implementations:** `CCharacter::OnCommand, CProp::OnCommand`  
**Usage:** `SetPlayerHasItem <InventoryItem item> <bool shouldGiveItem>`  
Click [here](../../../../src/net/highwayfrogs/editor/games/konami/greatquest/generic/InventoryItem.java) to see a list of InventoryItem values.

### TakeDamage (Script Only)
**Summary:** Causes the entity to take damage.  
**Supported Entity Types:** kcCActorBase+  
**Original Implementations:** `kcCScriptMgr::FireActorEffect[Remap] -> kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `TakeDamage <int attackStrength> [Damage Flags]`
```properties
# Damage dealt can have any number of damage types.
# These damage types are directly compared against the entity's configured "immune mask".
# If the entity is seen as immune to even just a single one of the damage types, the damage will be ignored.
# However, if the entity is a kcCActorBase and NOT a kcCActor, there is no immunity, nor health. An entity can get damaged despite not having the concept of health.
# Damage Types:
--Fire # 0
--Ice # 1
--Melee # 3
--Ranged # 4
--Fall # 12
--UnnamedDamageTypeXX # Where XX is a number between 0 and 31 and is not one of the numbers listed above.
```

### SetSavePoint (Script Only)
**Summary:** Sets the player's respawn point.  
**Supported Entity Types:** CCharacter  
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:** `SetSavePoint <int savePointNumber> <float x> <float y> <float z>`  
`savePointNumber` will be used to find the entity named `"Save pointInstXXX"` where `XXX` is the `savePointNumber`.  
Particles will be played at the position of that save point, if found.  

### SetUpdatesEnabled (Script Only)
**Summary:** Sets whether updates are enabled for the entity.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `SetUpdatesEnabled <bool shouldEnable>`  
**Aliases:**  `Entity.EnableUpdates, Entity.DisableUpdates`  
Used once in the vanilla game for a Ruby in Joy Towers. So it's basically unused.  
This command is almost the same as `SetActive`, except it doesn't touch collision/activation state.

### SetAIGoal (Script Only)
**Summary:** Sets the current entity AI goal.  
**Supported Entity Types:** CCharacter  
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:** `SetAIGoal <AiGoalType goal>`  
**Alias:** `AI.SetGoal`

```properties
# AiGoalType Values:
SCRIPT # I don't think this is implemented/does anything. But at some point this should be tested.
STATIC # I don't think this is implemented/does anything. But at some point this should be tested.
FIND # Implemented as MonsterClass::Do_Find(), Attempts to run towards the target, and attack if possible. Will attempt to wander sometimes if it gets stuck.
FLEE # Implemented as MonsterClass::TransitionTo() "Run00" -> Starts running. -> MonsterClass:Anim_Checks will be skipped when this is set.
WANDER # Implemented as MonsterClass::Do_Wander(). Picks a random "wander point" up to 15x15 units away in a square, and "Walk00" -> Walks (sometimes aggressively) towards it?
GUARD # Implemented as MonsterClass::Do_Guard(). Seems to try to walk to walk along a waypoint path back and forth until it has an actor target, where it will attempt to chase them. So, if we set the target as a waypoint, it will just keep walking across the path. -> How is this different from when we set waypoint goals?
DEAD # I don't think this is implemented/does anything. But at some point this should be tested.
SLEEP # I don't think this is implemented/does anything. But at some point this should be tested.
UNKNOWN # I don't think this is implemented/does anything. This name was not in GoalNames[] either.
```

### AttachSensor / Attach (Script Only)
**Summary:** Attaches a sensor to the entity.  
**Supported Entity Types:** CCharacter  
**Original Implementations:** `CCharacter::OnCommand`  
**Aliases:** `AttachSensor`  
**Usage:**
```ruby
# Enables the entity to deal damage with the bone selected.
# For most entities this is an arm, but for others this will be something like a sword.
# The purpose is to make it so the damage is dealt at the exact moment dangerous stuff touches.
Attach ATTACK_SENSOR <BoneTag boneNameOrId> <float radius> <int focus>

# Enables a listener to allow collision script events to fire
# Without doing this, I don't believe entities will fire collision events.
# Not sure yet what focus is yet.
Attach BUMP_SENSOR <BoneTag boneNameOrId> <float radius> <int focus>

# Enables a projectile launcher.
# I'm not sure yet if this means to launch a projectile or just to enable it.
Attach LAUNCHER <BoneTag boneNameOrId> <LauncherParams launcherData>

# Creates a particle emitter, attached to a bone in the script entity.
Attach PARTICLE_EMITTER <BoneTag boneNameOrId> <kcParticleEmitterParam emitterData>
```

### DetachSensor / Detach (Script Only)
**Summary:** Detaches a previously attached PARTICLE_EMITTER.  
**Supported Entity Types:** CCharacter  
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:** `Detach PARTICLE_EMITTER <BoneTag boneNameOrId>`  
**Aliases:** `DetachSensor`  

### SetWorldActive
**Summary:** Allows changing whether entities/terrain in certain parts of the world is enabled.  
**Supported Entity Types:** kcCEntity+  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `SetWorldActive <kcSpecialActivationMode activationMode> <bool shouldActivate>`  
**Aliases:**  `Entity.ActivateSpecial`  
Not used in the vanilla game, at least not directly. Can only run as a Waypoint entity.  
The world is broken up into segments (for those who know what this is-- an OctTree).  
This command will control if the segments a waypoint resides within are active or not.  
For example, on The Goblin Trail, if we get to the end of the map via glitches, it is invisible.  
The one problem with this theory is that this action doesn't ever appear to be used.  
I suspect that same function is called by the game code, instead of by a script instead, so the functionality is used even if it's not by the scripting system.  

```properties
# kcSpecialActivationMode Options:
NONE # Does nothing.
ENTITIES # Controls whether the entities are active.
TERRAIN # Controls whether the map terrain is visible.
BOTH # Controls both entity visibility and terrain visibility.
```

### ActivateCamera (Script Only)
**Summary:** Activates a new camera and switches to it.  
**Supported Entity Types:** All  
**Original Implementations:** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnActivatePivotCamera`  
**Usage:** `ActivateCamera <float transitionInSeconds>`

### DeactivateCamera (Script Only)
**Summary:** Deactivates the current camera, reverting to the previous camera.  
**Supported Entity Types:** All  
**Original Implementations:** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnDeactivatePivotCamera`  
**Usage:** `DeactivateCamera <float transitionInSeconds>`

### SetCameraTarget (Script Only)
**Summary:** Sets the entity which active camera focuses on.  
**Supported Entity Types:** All  
**Original Implementations:** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetTarget`  
**Usage:** `SetCameraTarget <kcCResourceEntityInst entityName>`

### SetCameraPivot (Script Only)
**Summary:** Set the rotational pivot entity for the active camera.
**Supported Entity Types:** All  
**Original Implementations:** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetPivot`  
**Usage:** `SetCameraPivot <kcCResourceEntityInst entityName>`  
The pivot entity is an entity who sits between the camera and its target.
In other words, the position/rotation of the camera is calculated by facing the target entity in a manner that also makes the camera directly face the pivot entity.

### SetCameraParam (Script Only)
**Summary:** Set camera settings for the active camera.  
**Supported Entity Types:** All  
**Original Implementations:** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetParam`  
**Usage:** `SetCameraParam <kcCameraPivotParam cameraParam> <float value>`

```properties
# kcCameraPivotParam Values:
PIVOT_DISTANCE # How much distance to put between the camera and the pivot entity.
TARGET_OFFSET_X # An offset to the position of the target entity.
TARGET_OFFSET_Y # An offset to the position of the target entity.
TARGET_OFFSET_Z # An offset to the position of the target entity.
PIVOT_OFFSET_X # An offset to the position of the pivot entity.
PIVOT_OFFSET_Y # An offset to the position of the pivot entity.
PIVOT_OFFSET_Z # An offset to the position of the pivot entity.
TRANSITION_DURATION # How long the camera transition should take.
CAMERA_BASE_FLAGS # The flags to apply to the camera entity.
```