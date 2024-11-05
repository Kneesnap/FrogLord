# Great Quest Scripting (GQS)
Frogger: The Great Quest has most of its "story"/per-level occurrences managed entirely through a scripting system.
This system gives tons of flexibility to create mods due to the immense amount of 
This scripting system is NOT the same thing as Noodle, which is the FrogLord scripting system.
Instead, GQS/kcScript is a term for the scripting system found in the original Frogger: The Great Quest.
The official name for this system is not known, so we use the term "QGS" to refer to FrogLord's support for this system, and kcScript as a placeholder name for the original system.

## The Basics
Each entity in a level can have a script, for example Frogger, coins, etc.
Each script is broken up into any number of functions, which contain the actual script behavior.
A function has two parts, the trigger (cause) and the effects.
An example of a trigger would be `OnLevel BEGIN`, which indicates that the function should run when the level begins.
On the other hand, effects are the commands which can impact the game, such as by playing a sound, making entities invisible, etc.  
The following example would play a sound effect when the level loads.

**Example:**  
```  
[Function]
trigger=OnLevel BEGIN
PlaySound "sfx/level_start_sfx"
# More script effects could be written here if desired.
```  

Because scripts belong to individual entities, they are considered to execute/run "as" that entity.
For example, if you use the `SetSequence` effect to cause an entity to animate, by default it will apply the animation to the entity which the script is written for.
In order to specify you'd like to run it on a different entity, supply an extra flag `--AsEntity <String: entityName>`.
For example, `SetSequence "NrmIdle01" --RunAsEntity "FrogInst001"` will run `SetSequence` as the player character, instead of the script entity.

## Getting started by creating/modifying scripts
The easiest way to get started is by looking at examples. To export scripts from the original game, select a level in FrogLord, and find the chunk named `scriptdata`.
By right-clicking it and selecting "Export Scripts" you'll be able to specify a folder to save the scripts to.
Make sure to use a different folder for each level you export scripts for to avoid confusion and overriding files with the same name.
While these files have a `.gqs` extension, they are just text files, and can be opened in any text editor.
For Notepad++, a [user-defined language file](./GreatQuestScript.xml) has been included to allow for colored syntax-highlighting of these files.

TODO: Actually include the Notepad++ file, and maybe link to instructions on how to install it?

When it's time to import your modified script back into the game, you can right-click `scriptdata` again, but this time select "Import Scripts" instead.  
This method of importing scripts should only be used for development/testing purposes, since [mods must follow this guide instead](modding-guide.md).  
TODO: Include some information on how to manage scripts with Noodle.

## Design Quirks
There is no concept of an if statement, or other convenient ways of controlling 
TODO: !

### Action Sequences
Action Sequences are a special kind of script specific to animations.
Some scripting features may only be available in action sequences, others only in normal entity scripts.
Some are available in both. When such a restriction is relevant, it will be mentioned.

## Available Triggers
Triggers ONLY exist for entity scripts, not for action sequences.
The following documents all triggers found within the game, and how they work.
TODO: Document triggers.

## Available Effects
The following documentation explains all script effects/actions found within the game, and how they work.
Some of these may only work in scripts, others only in action sequences.

### DoNothing (Unsupported)
**Summary:** Does nothing, likely a test command.  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `Do not use.`  

### EndScript (Unsupported)
**Summary:** Stops running an action sequence. Completely optional/unnecessary.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `Do not use.`  

### SetActive (Script Only)
**Summary:** Set whether the entity is active/visible/has collision.  
**Original Implementations:** `kcCEntity::OnCommand -> kcCEntity::ActivateAndUnhide`  
**Usage:** `SetActive <bool markAsActive>`  

### SetEnable (Unsupported)
**Summary:** Does nothing.  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `Do not use.`  

### TerminateEntity (Both)
**Summary:** Remove the entity from existence.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity::OnCommand -> kcCEntity::OnTerminate`  
**Usage:** `TerminateEntity`  

### InitFlags (Both)
**Summary:** Resets instance entity flags, and then sets the flags provided.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
**Usage:** `InitFlags [Optional Entity Flags]`  

TODO: Document the entity flags somewhere.

### SetFlags (Both)
**Summary:** Applies the provided entity flags to the entity.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
**Usage:** `SetFlags [Optional Entity Flags]`  

### ClearFlags (Both)
**Summary:** Removes the provided entity flags from the entity.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
**Usage:** `ClearFlags [Optional Entity Flags]`  

### SetState (Unsupported)
**Summary:** Unimplemented, does nothing.  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `Do not use.`  

### SetTarget (Script Only)
**Summary:** Change the current entities target entity.  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `SetTarget <kcCResourceEntityInst targetEntity>`  
A target entity is used for AI-related operations.  
For example, most enemies have `"FrogInst001"` as their target, so they attack Frogger.  
Or another example could be Fairy Frogmother, always faces Frogger because her target is `"FrogInst001"`.  

### SetAnimationSpeed (Both)
**Summary:** Sets the animation speed for the entity. (Default: 1.0?)  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetAnimationSpeed <float speed>`  

### SetPositionOnAxis (Script Only)
**Summary:** Sets the entities positional coordinate on the given axis.  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `SetPositionOnAxis <Axis axis> <float coordinate>`  

### SetPosition (Script Only)
**Summary:** Sets the entity position.  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `SetPosition <float x> <float y> <float z>`  

### AddPositionOnAxis (Script Only)
**Summary:** Offsets the enttiy position by the given value on the specified axis.  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `AddPositionOnAxis <Axis axis> <float displacement>`  

### AddPosition (Script Only)
**Summary:** Adds values to the entities current position.  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `AddPosition <float x> <float y> <float z>`  

### SetRotationOnAxis (Script Only)
**Summary:** Sets a rotation value on the specified axis.  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `SetRotationOnAxis <Axis axis> <float angleInDegrees>`  

### SetRotation (Script Only)
**Summary:** Sets the entity rotation.  
**Original Implementations:** `kcCEntity3D::OnCommand`  
**Usage:** `SetRotation <float xAngleInDegrees> <float yAngleInDegrees> <float zAngleInDegrees>`  

### AddRotationOnAxis (Both)
**Summary:** Adds a rotation value on the specified axis.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `AddRotationOnAxis <Axis axis> <float angleInDegrees>`  

### AddRotation (Both)
**Summary:** Adds rotation values to the existing rotation.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `AddRotation <float xAngleInDegrees> <float yAngleInDegrees> <float zAngleInDegrees>`  

### RotateRight (Both)
**Summary:** Rotates the entity to look right.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `RotateRight <float angleInDegrees>`  

### RotateLeft (Both)
**Summary:** Rotates the entity to look left.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `RotateLeft <float angleInDegrees>`  

### MakeTargetLookAtMe (Both)
**Summary:** Make the execution entity's target look towards the execution entity.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity3D::OnCommand`  
**Usage:** `MakeTargetLookAtMe`  

### SetAnimation (Both)
**Summary:** Changes the animation currently performed.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetAnimation <kcCResourceTrack animationFileName> [--Repeat] [--FirstAnimationInSequence] [--StartTime <float startTimeInSeconds>]`  

### SetSequence (Script Only)
**Summary:** Sets the active action sequence for the entity.  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetSequence <kcCActionSequence sequenceName> [--IgnoreIfAlreadyActive] [--OpenBoneChannel]`  
```
--IgnoreIfAlreadyActive - If this is set, and the sequence we'd like to apply is already the current sequence, then this flag says we should not restart the sequence.
Mrs. Boxy in Mushroom Valley really should have been configured to use this to prevent restarting her animation.

--OpenBoneChannel -> Not sure what this does yet, but it has something to do with animation.
TODO: It probably controls whether blending between the previous animation occurs or not.
```

### Wait (Action Sequence Only)
**Summary:** Wait a given amount of time before continuing the action sequence.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `Wait <float timeInSeconds>`  

### WaitForAxisRotation (Action Sequence Only)
**Summary:** Waits for an axis rotation to complete before continuing the action sequence.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `WaitForAxisRotation <Axis axis>`  

### WaitForFullRotation (Action Sequence Only)
**Summary:** Waits for all rotations to complete before continuing the action sequence.  
**Original Implementations:** `kcCActorBase::ProcessAction`  
**Usage:** `WaitForFullRotation`  

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
**Original Implementations:** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `ApplyMotionImpulse <float x> <float y> <float z>`  

### Prompt (Unsupported)
**Summary:** This was never fully supported by the game, but it looks like it was supposed to allow the player to make choices within dialog text-boxes.  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `Prompt <Hash promptResourceName>`  

### ShowDialog (Script Only)
**Summary:** Creates a dialog box with text for the player.  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `ShowDialog <String dialogResourceName>`  
The string here is the name of a generic resource string.
In other words, the actual text to display is kept in another file, and this command needs to be given the name of the file.

### SetAlarm (Both)
**Summary:** Sets an alarm to ring (Script Cause: `OnAlarm`) after a delay.
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity::OnCommand`  
**Usage:** `SetAlarm <AlarmId alarmId> <float durationInSeconds> <int repeatCount>`  
Any number between 0 and 31 is a valid alarm ID.
A repeat count of zero will cause the alarm to ring once.

### TriggerEvent (Both)
**Summary:** Triggers a named event.  
**Original Implementations:** `kcCActorBase::ProcessAction, kcCEntity::OnCommand`  
**Usage:** `TriggerEvent <String eventName>`  

TODO: Include a list of all valid event names.

### PlaySound (Script Only)
**Summary:** Plays (or stops) a sound effect.  
**Original Implementations:** `kcCEntity::OnCommand, kcCEntity3D::OnCommand`  
**Usage:** `PlaySound <String soundFilePath> [--StopSound]`  

### SetVariable (Script Only)
**Summary:** Sets an entity variable by its ID.  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `SetVariable <VariableId variableId> <int value>`  
Valid variable IDs are between 0 and 7.

### AddToVariable (Script Only)
**Summary:** Adds to an existing entity variable by its ID.  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `AddToVariable <VariableId variableId> <int value>`  
Valid variable IDs are between 0 and 7.

### TriggerByNumber (Script Only)
**Summary:** Broadcasts a script cause (`OnNumber`) using a number.
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `TriggerByNumber <NumberOperation operation> <int number>`  

```
NumberOperation:
 - LITERAL_NUMBER # The number broadcasted will be the number provided as an argument.
 - ENTITY_VARIABLE # The number broadcasted will be the value of the entity variable at the provided ID.
 - RANDOM # The number broadcasted will be a random number between 0 and the number provided.
```

### SpawnParticleEffect (Script Only)
**Summary:** TODO  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SpawnParticleEffect <Hash particle>`  

### KillParticleEffect (Script Only)
**Summary:** TODO  
**Original Implementations:** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `KillParticleEffect`  

### Launcher (Unsupported)
**Summary:** Opens up a dialog box saying it is no longer supported.  
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:** `Do not use.`  

### TriggerIfPlayerHasItem (Script Only)
**Summary:** Test if the player has the given item, then broadcast a script cause (`OnPlayerHasItem`) with the result.  
**Original Implementations:** `CCharacter::OnCommand, CProp::OnCommand`  
**Usage:** `TriggerIfPlayerHasItem <InventoryItem item>`  
See below for a list of valid InventoryItem values.

### SetPlayerHasItem (Script Only)
**Summary:** Give or take an inventory item to/from the player.  
**Original Implementations:** `CCharacter::OnCommand, CProp::OnCommand`  
**Usage:** `SetPlayerHasItem <bool shouldGiveItem> <InventoryItem item>`  
TODO: Include a list of valid InventoryItem values.

### TakeDamage (Script Only)
**Summary:** Causes the entity to take damage.  
**Original Implementations:** `kcCScriptMgr::FireActorEffect -> `  
**Usage:** `TakeDamage <Int attackStrength> <HexInteger weaponMask>`  

### SetSavePoint (Script Only)
**Summary:** Sets the player's respawn point.  
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:** `SetSavePoint <int savePointNumber> <float x> <float y> <float z>`  
`savePointNumber` will be used to find the entity named `"Save pointInstXXX"` where `XXX` is the `savePointNumber`.  
Particles will be played at the position of that save point, if found.  

### SetUpdatesEnabled (Script Only)
**Summary:** Sets whether updates are enabled for the entity.  
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `SetUpdatesEnabled <bool shouldEnable>`  

### SetAIGoal (Script Only)
**Summary:** Sets the current entity AI goal.  
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:** `SetAIGoal <AiGoalType goal>`  

```PowerShell
AiGoalType Options:
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
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:**
```PowerShell
Attach ATTACK_SENSOR <String boneNameOrId> <float radius> <int focus> # Enables the entity to take damage, and allows scripts to listen for attacks. Not sure yet what focus is.
Attach BUMP_SENSOR <String boneNameOrId> <float radius> <int focus> # Enables a listener to allow collision script events to fire. Not sure yet what focus is.
Attach LAUNCHER <String boneNameOrId> <LauncherParams launcherData> # Enables a projectile launcher.
Attach PARTICLE_EMITTER <String boneNameOrId> <kcParticleEmitterParam emitterData> # Creates a particle emitter.
Attach LIGHT <String boneNameOrId> # UNUSED
```

### DetachSensor / Detach (Script Only)
**Summary:** Detaches a previously attached PARTICLE_EMITTER.  
**Original Implementations:** `CCharacter::OnCommand`  
**Usage:** `Detach PARTICLE_EMITTER <String boneNameOrId>`

### SetWorldActive
**Summary:** Allows changing whether entities/terrain in certain parts of the world is enabled.
**Original Implementations:** `kcCEntity::OnCommand`  
**Usage:** `SetWorldActive <bool shouldActivate> <SpecialActivationMode activationMode>`  
Can only run as a Waypoint entity.  
The world is broken up into segments (for those who know what this is-- an OctTree).  
This command will control if the segments a waypoint resides within are active or not.  
For example, on The Goblin Trail, if we get to the end of the map via glitches, it is invisible.  
The one problem with this theory is that this action doesn't ever appear to be used.  
I suspect that same function is called by the game code, instead of by a script instead, so the functionality is used even if it's not by the scripting system.  

```
SpecialActivationMode Options:
 NONE - Does nothing.
 ENTITIES - Controls whether the entities are active.
 TERRAIN - Controls whether the map terrain is visible.
 BOTH - Controls both entity visibility and terrain visibility.
```