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
For example, `SetSequence "NrmIdle01" --AsEntity "FrogInst001"` will run `SetSequence` as the player character, instead of the script owner.

## Getting started by creating/modifying scripts
The easiest way to get started is by looking at examples. To export scripts from the original game, select a level in FrogLord, and find the chunk named `scriptdata`.
By right-clicking it and selecting "Export Scripts" you'll be able to specify a folder to save the scripts to.
Make sure to use a different folder for each level you export scripts for to avoid confusion and overriding files with the same name.
While these files have a `.gqs` extension, they are just text files, and can be opened in any text editor.
For Notepad++, it is recommended to set the language of these files to PowerShell, as its syntax highlighting works well.

Even though it would be possible to make changes to these scripts and then re-import them by right-clicking `scriptdata` and selecting "Import Scripts", this is not recommended.  
Since [mods must follow this guide](modding-guide.md), the recommended method for modifying scripts is by creating **GQS Script Groups**.  

### GQS Script Groups
GQS Script Group files are designed to organize GQS scripts.  
They reduce the number of files necessary when making a script by allowing separate resources such as entities, dialog text, script functions, and others to be included in a single file.

```PowerShell
# The following section contains the text/string resources which can be used as dialog text.
# The quotes are optional, but help make syntax highlighters display this file more cleanly.
[Dialog]
FFM_DIALOG_001="This is an example, which could be displayed in-game as dialog." # Shown in-game with 'ShowDialog "FFM_DIALOG_001"'.

# The following section will contain entities to add (or replace).
[Entities]

# The config section named 'FrogmotherInst001' has two "layers" of [square brackets].
# The number of layers matters, as it indicates what config section the new section belongs to.
# In this case, since there are two layer, it will attach to the last section with one layer, so [Entities].
[[FrogmotherInst001]]
# ... The data which goes here is exactly the same as a regular entity definition.
# ... For the purpose of the example it has been omitted.

# Note how there's only one layer of square braces here.
# This is because we do not want this data to be linked to [Entities], thus being treated as an entity.
[EntityDefinitions]
# Entity definition data can also be placed here, similarly to how it works for entities, with a new section for each definition.

[Scripts]
# This section contains scripts.
# The scripts are added to existing entity scripts, meaning if an entity can have parts of its script spread across different GQS Script groups.

[[FrogmotherInst001]] # The name of the entity to add script functions to.
[[[Function]]] # Define a new function for Fairy FrogMotherInst001. (Note the 3 square brackets).
cause=OnPlayer INTERACT # When the player interacts with Fairy Frogmother.
ShowDialog "FFM_DIALOG_001" # Shows the dialog defined earlier to the player.
```

#### How to use GQS Script Groups in Noodle?
TODO: Include some information on how to manage scripts with Noodle.

## Design Quirks
There is no concept of an if statement, so controlling execution flow can be a bit annoying.
Instead, any code which should only run conditionally should be split into multiple script functions, and features such as entity variables can be used to control which script function(s) are run.

### Entity Variables
Each entity has 8 variables available (accessible via IDs 0 through 7).
These variables can be set to any whole number.
They can be used as a way to control which code gets run, as the `SendNumber` command can send an entity variable.  
For example:

```PowerShell
[Function]
cause=OnReceiveNumber EQUALS 1
ShowDialog "DialogOne"

[Function]
cause=OnReceiveNumber EQUALS 2
ShowDialog "DialogTwo"

[Function]
cause=... # This function's cause doesn't matter for the purpose of this example.
# This is a very basic example, but in the following function we can choose which of the two above functions we run by choosing either:
#SetVariable 0 1 # This would set Variable #0 to equal 1
#SetVariable 0 2 # This would set Variable #0 to equal 2

# This will send a number cause with the number that is currently in Variable #0.
# By having two separate functions which listen for different numbers, we can control which function runs based on the variable.
# This can be used to recreate the conceptual behavior of "If X, do Y, otherwise, do Z.".
SendNumber ENTITY_VARIABLE 0 
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
**Ghidra Reference (For Coders):** `EvLevelBegin, EvLevelEnd`  
**Usage:** `OnLevel <BEGIN|END>`  

### OnPlayer
**Summary:** Executes when the player has an interaction with the script owner.  
**Supported Entity Types:** Base Actors  
**Usage:** `OnPlayer <INTERACT|BUMPS|ATTACK|PICKUP_ITEM>`  
**Actions:**  
```properties
INTERACT # The player interacts with the script owner. script owner must be at least a kcCActorBase. Code: CFrogCtl::OnBeginAction, CFrogCtl::CheckForHealthBug
BUMPS # The player collides/bumps with the script owner. script owner must be a CProp or a CCharacter. Code: CProp::TriggerHitCallback, CCharacter::BumpCallback
ATTACK # The player attacks the script owner. script owner must be kcCActorBase. Code: CFrogCtl::Spit, CFrogCtl::OnBeginMissile, CFrogCtl::OnBeginMagicStone, and, CFrogCtl::OnBeginMelee
PICKUP_ITEM # The player picks up the script owner as an item. script owner must be CItemDesc. Code: CCharacter::PickupCallback
```

### OnActor
**Summary:** Executes when one of the following actions happens to the script owner.  
**Supported Entity Types:** Base Actors  
**Usage:** `OnActor <BUMPS|TAKE_DAMAGE|DEATH>`  
**Actions:**  
```properties
BUMPS # Another actor bumps into the script owner. script owner must be a CProp or a CCharacter. Code: CProp::TriggerHitCallback, CCharacter::BumpCallback
TAKE_DAMAGE # The script owner takes damage. Code: script owner must be at least a kcCActor. kcCActor::OnDamage
DEATH # The script owner dies. Code: script owner must be at least a kcCActor. kcCActor::OnDamage
```

### OnDamage
**Summary:** Executes when the script owner takes a certain type of damage.  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCActorBase::OnDamage`  
**Usage:** `OnDamage <damageType>`  
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
# There are a ton more possible damage types than the ones listed above, but most are unused.
# Thus, they are free to be used for whatever kind of damage the mod-creator likes.
# To specify one of the unused damage types, use the following:
UNNAMED_TYPE_XX # Where XX is a number between 0 and 31 and is not one of the numbers listed above. For example: UNNAMED_TYPE_16
```

### OnAlarm
**Summary:** Executes when the specified alarm (timer) expires.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::AlarmCallback`  
**Supported Entity Types:** Base Actors  
**Usage:** `OnAlarm FINISHED <alarmId>`  
Any whole number between 0 and 31 can be used as an alarm ID.  
To use `OnAlarm`, the alarm must first be activated using `SetAlarm`.
Note that other options such as `IN_PROGRESS` may work in-place of `FINISHED`, but are not currently used/understood.  

### OnPrompt (Unsupported)
**Summary:** Does not work correctly.  
**Ghidra Reference (For Coders):** `kcCActorBase::OnCommand[PROMPT]`  
**Usage:** `OnPrompt <promptName>`

### OnEventTrigger (Unsupported)
**Summary:** Supposed to execute when the specified event triggers, but it actually fires when ANY event triggers.  
**Ghidra Reference (For Coders):** `kcCEventMgr::Trigger`  
**Usage:** `OnEventTrigger <eventName>`  

### OnDialog
**Summary:** Executes when the given dialog text begins or is advanced.  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `EvDialogBegin, EvDialogAdvance`  
**Usage:** `OnDialog <BEGIN|ADVANCE> <dialogStrName>`  
This event is not global, so it will only work if the entity defining this script cause is also the entity to show the dialog.  
The `dialogStrName` should be the name of the text resource containing the dialog text.  
See `ShowDialog` for more details.  

### OnReceiveNumber
**Summary:** Executes when a number is received matching the given criteria.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnNumber`  
**Usage:** `OnReceiveNumber <operation> <number>`  
This will only execute when a number matching the specified criteria is sent to the script owner using the `SendNumber` effect.  
The most common operation will be `EQUAL_TO`, which will allow specifying behavior upon receiving a specific number.  
Only whole numbers (integers) are supported by this cause.  

**Valid Operations:**  
```properties
EQUAL_TO # The received number is equal to <number>
NOT_EQUAL_TO # The received number is not equal to <number>
LESS_THAN # Received number < <number>
GREATER_THAN # Received number > <number>
LESS_THAN_OR_EQUAL # Received number <= <number>
GREATER_THAN_OR_EQUAL # Received number >= <number>
```

### OnReceiveWhetherPlayerHasItem
**Summary:** When the script owner receives whether the player has an item (from `SendWhetherPlayerHasItem`).  
**Supported Entity Types:** Props & Characters  
**Ghidra Reference (For Coders):** `CCharacter::OnWithItem, CProp::OnWithItem`  
**Usage:** `OnReceiveWhetherPlayerHasItem <true|false>`  
When the `SendWhetherPlayerHasItem` effect is used, the `OnReceiveWhetherPlayerHasItem` cause will run, based on whether the player had the item or not.  
If the player did have the item, then the cause `OnReceiveWhetherPlayerHasItem true` will execute, otherwise `OnReceiveIfPlayerHasItem false` will execute.  
Note that there is no way to make `OnReceiveWhetherPlayerHasItem` restrict which items it will activate for.  
Thus it would be necessary to use multiple entities (each testing for one item) in order to test for multiple items.  

### OnEntity
**Summary:** Executes when the script owner interacts with a waypoint.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCEntity3D::Notify, sSendWaypointStatus`  
**Usage:**  
```php
OnEntity ENTERS_WAYPOINT_AREA <waypointEntityName> # Executes when the script owner enters the area of the specified waypoint.
OnEntity LEAVES_WAYPOINT_AREA <waypointEntityName> # Executes when the script owner leaves the area of the specified waypoint.

# The following are usable ONLY when the target entity is a Waypoint.
OnEntity ENTERS_TARGET_WAYPOINT_AREA # Executes when the script owner enters its target waypoint's area.
OnEntity LEAVES_TARGET_WAYPOINT_AREA # Executes when the script owner leaves its target waypoint's area.
```

### OnWaypoint
**Summary:** Executes when an entity enters/leaves the waypoint script owner's area.  
**Supported Entity Types:** Waypoints  
**Ghidra Reference (For Coders):** `sSendWaypointStatus`  
**Usage:** `OnWaypoint <ENTITY_ENTERS|ENTITY_LEAVES> <entityName>`

## Available Effects
The following documentation explains all script effects/actions found within the game, and how they work.
Some of these may only work in scripts, others only in action sequences.

### DoNothing (Unsupported)
**Summary:** Does nothing, likely a test command.  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### EndScript (Unsupported)
**Summary:** Stops running an action sequence. Completely optional/unnecessary.  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### SetActive (Script Only)
**Summary:** Set whether the entity is active. (An inactive entity will be invisible and lack collision)  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand -> kcCEntity::ActivateAndUnhide`  
**Usage:** `SetActive <true|false>`  
**Aliases:**  
```properties
Entity.Activate # Alias for 'SetActive true'
Entity.Deactivate # Alias for 'SetActive false'
```
Waypoints are capable of activating invisible parts of the map when this is used. (See `SetWorldActive` for more info.)  

### SetEnable (Unsupported)
**Summary:** Does nothing.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### TerminateEntity (Both)
**Summary:** Remove the entity from existence.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCEntity::OnCommand -> kcCEntity::OnTerminate`  
**Usage:** `TerminateEntity`  

### InitFlags (Both)
**Summary:** Resets instance entity flags, and then sets the flags provided.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
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
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
**Usage:** `SetFlags [Entity Flags]`  
See `InitFlags` above for a list of flags.  

### ClearFlags (Both)
**Summary:** Removes the provided entity flags from the entity.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`  
**Usage:** `ClearFlags [Entity Flags]`  
See `InitFlags` above for a list of flags.  

### SetState (Unsupported)
**Summary:** Unimplemented, does nothing.  
**Ghidra Reference (For Coders):** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### SetTarget (Script Only)
**Summary:** Change the current entities target entity.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand`  
**Usage:** `SetTarget <kcCResourceEntityInst targetEntity>`  
A target entity is used for AI-related operations.  
For example, most enemies have `"FrogInst001"` as their target, so they attack Frogger.  
Others, such as Fairy Frogmother face the player by setting their target as `FrogInst001`.  

### SetAnimationSpeed (Both)
**Summary:** Sets the animation speed for the entity. (Default: 1.0?)  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetAnimationSpeed <float speed>`  
Not used in the vanilla game.

### SetPositionOnAxis (Script Only)
**Summary:** Sets the entities positional coordinate on the given axis.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCEntity3D::OnCommand`  
**Usage:** `SetPositionOnAxis <X|Y|Z> <coordinate>`  
**Example:** `SetPositionOnAxis X 22.5`  
Not used in the vanilla game.

### SetPosition (Script Only)
**Summary:** Sets the entity position.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCEntity3D::OnCommand`  
**Usage:** `SetPosition <x> <y> <z>`  

### AddPositionOnAxis (Script Only)
**Summary:** Offsets the enttiy position by the given value on the specified axis.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCEntity3D::OnCommand`  
**Usage:** `AddPositionOnAxis <X|Y|Z> <amount>`  
Not used in the vanilla game.

### AddPosition (Script Only)
**Summary:** Adds values to the entities current position.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCEntity3D::OnCommand`  
**Usage:** `AddPosition <x> <y> <z>`  

### SetRotationOnAxis (Script Only)
**Summary:** Sets a rotation value on the specified axis.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCEntity3D::OnCommand`  
**Usage:** `SetRotationOnAxis <X|Y|Z> <angleInDegrees>`  
Not used in the vanilla game.

### SetRotation (Script Only)
**Summary:** Sets the entity rotation.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCEntity3D::OnCommand`  
**Usage:** `SetRotation <xAngleInDegrees> <yAngleInDegrees> <zAngleInDegrees>`  
Not used in the vanilla game.

### AddRotationOnAxis (Both)
**Summary:** Adds a rotation value on the specified axis.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `AddRotationOnAxis <X|Y|Z> <angleInDegrees>`  
Not used in the vanilla game.

### AddRotation (Both)
**Summary:** Adds rotation values to the existing rotation.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `AddRotation <xAngleInDegrees> <yAngleInDegrees> <zAngleInDegrees>`  
Not used in the vanilla game.

### RotateRight (Both)
**Summary:** Rotates the entity to look right.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `RotateRight <angleInDegrees>`  

### RotateLeft (Both)
**Summary:** Rotates the entity to look left.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`  
**Usage:** `RotateLeft <angleInDegrees>`  

### LookAtTargetEntity (Both)
**Summary:** Make the execution entity face its target entity.  
**Supported Entity Types:** All 3D Entities  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity3D::OnCommand`  
**Usage:** `LookAtTargetEntity`  
Not used in the vanilla game.

### SetAnimation (Both)
**Summary:** Changes the animation currently performed.  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetAnimation <animationFileName> [--Repeat] [--FirstAnimationInSequence] [--StartTime <startTimeInSeconds>]`  
While this effect appears to work outside an action sequence, the game scripts always use `SetSequence` instead of directly calling `SetAnimation`.  

### SetSequence (Script Only)
**Summary:** Sets the active action sequence for the entity.  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `SetSequence <actionSequenceName> [--IgnoreIfAlreadyActive] [--OpenBoneChannel]`  
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
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction`  
**Usage:** `Wait <timeInSeconds>`  
Not used in the vanilla game.

### WaitForAxisRotation (Action Sequence Only)
**Summary:** Waits for an axis rotation to complete before continuing the action sequence.  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction`  
**Usage:** `WaitForAxisRotation <X|Y|Z>`  
Not used in the vanilla game.

### WaitForFullRotation (Action Sequence Only)
**Summary:** Waits for all rotations to complete before continuing the action sequence.  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction`  
**Usage:** `WaitForFullRotation`  
Not used in the vanilla game.

### WaitForAnimation (Action Sequence Only)
**Summary:** Waits for the active animation to complete before continuing the action sequence.  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction`  
**Usage:** `WaitForAnimation`  

### Loop (Action Sequence Only)
**Summary:** The action sequence will restart the number of times specified.  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction`  
**Usage:** `Loop <numberOfTimesToLoop>`  

### ApplyMotionImpulse (Both)
**Summary:** Applies a motion "impulse" to the entity.  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `ApplyMotionImpulse <x> <y> <z>`
This will only work if the `--EnablePhysics` flag is applied to the entity.  

### Prompt (Unsupported)
**Summary:** This was never fully supported by the game, but it looks like it was supposed to allow the player to make choices within dialog text-boxes.  
**Ghidra Reference (For Coders):** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `Prompt <promptName>`  
Not used in the vanilla game.

### ShowDialog (Script Only)
**Summary:** Creates a dialog box with text for the player.  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `ShowDialog <dialogResourceName>`  
IMPORTANT! This command is more complicated than it would initially seem.  
It would seem intuitive to use it like `ShowDialog "Bruiser: You got my honey yet?"`.  
However, this will not work. In-game this would show a dialog box with the text "not found".  
This is because `ShowDialog` is expecting the name of a text resource containing the dialog text, and not the dialog text itself.  
So, `ShowDialog "DIALOG_004"` would work if there is a text resource named `DIALOG_004` in the level.  
This allowed the original team to translate the game into multiple languages without having to copy the scripts for every single language.  
Instructions for adding text/string resources are in the `GQS Script Group` documentation near the start of this file.  

### SetAlarm (Both)
**Summary:** Sets an alarm to ring (Script Cause: `OnAlarm`) after a delay.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCEntity::OnCommand`  
**Usage:** `SetAlarm <alarmId> <durationInSeconds> [--Repeat <numberOfTimesToRepeat>]`  
Any number between 0 and 31 is a valid alarm ID.  
The duration of the alarm can be a decimal number.  
The timer will start counting down from the number of seconds given.
Once the timer reaches 0, it will send `OnAlarm` with the alarm ID provided.  
The main purpose of this feature is to run script effects after a delay.  

### TriggerEvent (Both)
**Summary:** Triggers a named event.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCActorBase::ProcessAction, kcCEntity::OnCommand`  
**Usage:** `TriggerEvent <String eventName>`  

**Valid Events:**  
```properties
"LevelLoadComplete" # The game will load the sky box, water, setup lighting, and setup environment render states from kcEnvironment. Usually called by ExecuteLoad() completing.
"LevelBegin" # Sets up default data like coin pickup particles, the AI system, adds the system entities. Called when the level start FMV ends.
"LevelCompleted" # Destroys all active cameras, and sets a flag for completing the level. Triggered by in-game scripts.
"LevelEnd" # Stops all sound effects and sends the OnLevel script cause for completing the level. -> Not sure what triggers this event.
"LevelUnload" # Cleanup/remove water & sky dome, stop all sounds, hide the HID, unload main menu/interface resources. Called by exiting the pause menu requesting to quit the game (PauseEndCase) or the level stops. (PlayEndCase).
"BeginScreenFade" # Causes the screen to fade to black. Called by a lot of things.
"EndScreenFade" # Unfades/unhides the contents of the screen. Called by a lot of things.
"StartMovie" # Seems to setup FMV/movie playback. Called by PlayMovieUpdate
"CutMovie" # Stops movie playback. Called when the player skips an FMV or it completes. (MovieDoneOrRequestAdvance)
"MovieContinueGame", # Seems to setup the game to continue playback. Registered in PlayMovieUpdate(). I don't think this is ever called.
"LockPlayerControl" # Disables controller/keyboard input from influencing the player character. Exclusively called from scripts. NOTE: This will be automatically be enabled when a dialog box opens, and disabled when closed.
"UnlockPlayerControl" # Re-enables controller/keyboard input for the player character. Exclusively called from scripts.
"DialogBegin" # Displays the dialog text box, and sends the script cause `OnDialog BEGIN`. Called by the handler for the script command 'ShowDialog' (kcCActorBase::OnCommand).
"DialogAdvance" # Hides the dialog text box, and sends the script cause `OnDialog ADVANCE`. Called by the dialog update logic (kcCDialog::Update).
"DialogEnd" # Never called, but if it were it would hide the dialog text box and sends the script cause `OnDialog END`. Re-enables player input.
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
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand, kcCEntity3D::OnCommand`  
**Usage:** `PlaySound <soundFilePath> [--StopSound]`  
A sound file path can be obtained by right-clicking a sound in the FrogLord sound list, and clicking "Copy file path".

### SetVariable (Script Only)
**Summary:** Sets an entity variable by its ID.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand`  
**Usage:** `SetVariable <variableId> <value>`  
Stores the value into the variable ID/slot given.  
Valid variable IDs are between 0 and 7.  
The provided value must be a whole number.

### AddToVariable (Script Only)
**Summary:** Adds to an existing entity variable by its ID.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand`  
**Usage:** `AddToVariable <variableId> <value>`  
Adds the value into the variable ID/slot.  
Valid variable IDs are between 0 and 7.  
The provided value must be a whole number.  

### SendNumber (Script Only)
**Summary:** Sends a number, which will cause the `OnReceiveNumber` script cause.
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand`  
**Usage:** `SendNumber <LITERAL_NUMBER|ENTITY_VARIABLE|RANDOM> <number>`  
```properties
LITERAL_NUMBER # The number sent will be the number provided as an argument.
ENTITY_VARIABLE # The number sent will be the value of the entity variable at the provided ID.
RANDOM # The number sent will be a random number between 0 and the number provided.
```

If the `--AsEntity` flag is included, the number will be sent to the `--AsEntity` target instead of the script owner.  
When sending an `ENTITY_VARIABLE` the number sent will be the value of the variable obtained from the script owner, instead of from the `--AsEntity` target.  

### SpawnParticleEffect (Script Only)
**Summary:** Sets up a particle emitter for the entity.  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCActorBase::OnCommand/kcCActor::OnCommand -> kcCParticleMgr::SpawnEffect`  
**Usage:** `SpawnParticleEffect <particleEmitterDataName>`  
Not used in the vanilla game.

### KillParticleEffect (Script Only)
**Summary:** Disables particle effect(s) spawned by the current entity.  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `KillParticleEffect`  
Not used in the vanilla game.

### Launcher (Unsupported)
**Summary:** Opens up a dialog box saying it is no longer supported.  
**Ghidra Reference (For Coders):** `CCharacter::OnCommand`  
**Usage:** `Do not use.`  
Not used in the vanilla game.

### SendWhetherPlayerHasItem (Script Only)
**Summary:** Send whether the player has the given item, thus causing `OnReceiveWhetherPlayerHasItem`.  
**Supported Entity Types:** Character or Prop  
**Ghidra Reference (For Coders):** `CCharacter::OnCommand, CProp::OnCommand`  
**Usage:** `SendWhetherPlayerHasItem <inventoryItem>`  
Click [here](../../../../src/net/highwayfrogs/editor/games/konami/greatquest/generic/InventoryItem.java) to see a list of InventoryItem values.  
When the `--AsEntity` flag is included, the number will be sent to the `--AsEntity` target instead of the script owner.  

### SetPlayerHasItem (Script Only)
**Summary:** Add or remove an inventory item in the player's inventory.  
**Supported Entity Types:** Character or Prop  
**Ghidra Reference (For Coders):** `CCharacter::OnCommand, CProp::OnCommand`  
**Usage:** `SetPlayerHasItem <inventoryItem> <true|false>`  
Click [here](../../../../src/net/highwayfrogs/editor/games/konami/greatquest/generic/InventoryItem.java) to see a list of InventoryItem values.

### TakeDamage (Script Only)
**Summary:** The script owner takes damage (loses health).  
**Supported Entity Types:** Base Actors  
**Ghidra Reference (For Coders):** `kcCScriptMgr::FireActorEffect[Remap] -> kcCActorBase::OnCommand/kcCActor::OnCommand`  
**Usage:** `TakeDamage <attackStrength> [Damage Flags]`  
The attack strength is a whole number indicating how much damage to take.  
A negative number will heal the entity.  

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
**Ghidra Reference (For Coders):** `CCharacter::OnCommand`  
**Usage:** `SetSavePoint <savePointId> <x> <y> <z>`  
The player's respawn position will be set to the new coordinates.  
Also, the game will find an entity named `"Save pointInstXXX"` where `XXX` is the `savePointId`.  
If such an entity is found, particles will be played at the position of that entity.  

### SetUpdatesEnabled (Script Only)
**Summary:** Sets whether updates are enabled for the entity.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand`  
**Usage:** `SetUpdatesEnabled <true|false>`  
**Aliases:**
```properties
Entity.EnableUpdates # Equivalent to 'SetUpdatesEnabled true'
Entity.DisableUpdates # Equivalent to 'SetUpdatesEnabled false'
```

It would be unused in the vanilla game if not for one Ruby found in Joy Towers.  
This command is almost the same as `SetActive`, except it doesn't touch collision/activation state.

### SetAIGoal (Script Only)
**Summary:** Sets the current entity AI goal.  
**Supported Entity Types:** CCharacter  
**Ghidra Reference (For Coders):** `CCharacter::OnCommand`  
**Usage:** `SetAIGoal <goalType>`  
**Aliases:**
```
AI.SetGoal <FIND|FLEE|WANDER|GUARD|DEAD|SLEEP>
```

```properties
# Notable Goal Types:
FIND # Attempts to run towards the target, and attack if possible. Will attempt to wander sometimes if it gets stuck.
FLEE # Starts fleeing its current position. This is what happens when enemies run away from the player.
WANDER # Picks a random "wander point" up to 15x15 units away in a square, and "Walk00" -> Walks (sometimes aggressively) towards it?
GUARD # Seems to try to walk along a waypoint path back and forth until it has an actor target, where it will attempt to chase them. So, if we set the target as a waypoint, it will just keep walking across the path. -> How is this different from when we set waypoint goals?
DEAD # Applies the entity death animation.
SLEEP # Applies the entity sleep animation.
```

### AttachSensor / Attach (Script Only)
**Summary:** Attaches a sensor to the entity.  
**Supported Entity Types:** CCharacter  
**Ghidra Reference (For Coders):** `CCharacter::OnCommand`
**Usage:**
```ruby
# Makes the given 3D model bone deal damage when a bounding sphere surrounding the bone intersects with another entity.
# In other words, it makes a part of the entity's 3D model deal damage to any entity it touches.
# The bone will usually be an arm, but some entities will use bones such as part of a sword.
# This makes combat appear more fluid, as the player will only react to getting hit the moment they are hit.
# The 'radius' value is a decimal number representing the collision/bounding sphere's radius.
# The 'focus' value is a whole number with a currently unknown purpose.
Attach ATTACK_SENSOR <boneNameOrId> <radius> <focus>
AttachSensor <boneNameOrId> <radius> <focus>

# Enables a listener to allow collision script events to fire
# Without doing this, I don't believe entities will fire collision events.
# The 'radius' value is a decimal number representing the collision/bounding sphere's radius.
# The 'focus' value is a whole number with a currently unknown purpose.
Attach BUMP_SENSOR <boneNameOrId> <radius> <focus>

# Enables a projectile launcher.
# I'm not sure yet if this means to launch a projectile or just to enable it.
Attach LAUNCHER <boneNameOrId> <launcherParamName>

# Creates a particle emitter, attached to a bone in the script owner.
Attach PARTICLE_EMITTER <boneNameOrId> <particleEmitterParamName>
```

### Detach (Script Only)
**Summary:** Detaches a previously attached PARTICLE_EMITTER.  
**Supported Entity Types:** CCharacter  
**Ghidra Reference (For Coders):** `CCharacter::OnCommand`  
**Usage:** `Detach PARTICLE_EMITTER <boneNameOrId>`

### SetWorldActive
**Summary:** Set whether entities/terrain are enabled in the world area covered by the waypoint.  
**Supported Entity Types:** Waypoint  
**Ghidra Reference (For Coders):** `kcCEntity::OnCommand`  
**Usage:** `SetWorldActive <ENTITIES|TERRAIN|BOTH> <true|false>`  
**Alias:**  `Entity.ActivateSpecial <ENTITIES|TERRAIN|BOTH> <true|false>`  
Not used in the vanilla game.  
This command will control if the world segments (OctTree nodes) the waypoint resides within are active.  
In large levels such as The Goblin Trail or Joy Castle, if the player uses glitches to skip ahead, parts of the map are invisible.
When the player reaches certain points in the map, the vanilla game will make those areas visible by running `SetActive` on waypoints covering the invisible area.  
Unlike `SetActive`, `SetWorldActive` is able to disable parts of the map as well, and can be used more than once.  

```properties
# kcSpecialActivationMode Options:
NONE # Does nothing.
ENTITIES # Controls whether the entities are active.
TERRAIN # Controls whether the map terrain is visible.
BOTH # Controls both entity visibility and terrain visibility.
```

### ActivateCamera (Script Only)
**Summary:** Activates a new camera, causing the game to switch to it.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnActivatePivotCamera`  
**Usage:** `ActivateCamera <transitionInSeconds>`
`transitionInSeconds` is a decimal number indicating how long it will take (in seconds) to switch to the new camera.

### DeactivateCamera (Script Only)
**Summary:** Deactivates the current camera, reverting to the previous camera.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnDeactivatePivotCamera`  
**Usage:** `DeactivateCamera <float transitionInSeconds>`  
`transitionInSeconds` is a decimal number indicating how long it will take (in seconds) to switch to the previous camera.

### SetCameraTarget (Script Only)
**Summary:** Sets the entity which the current camera focuses on.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetTarget`  
**Usage:** `SetCameraTarget <entityName>`

### SetCameraPivot (Script Only)
**Summary:** Set the rotational pivot entity for the current camera.
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetPivot`  
**Usage:** `SetCameraPivot <entityName>`  
The pivot entity is an entity which the camera will calculate where to be in the world by finding a position that puts the pivot entity between the camera and the camera's target entity.  
In other words, the position/rotation of the camera is calculated by facing the target entity in a manner that also makes the camera directly face the pivot entity.

### SetCameraParam (Script Only)
**Summary:** Change the current camera's settings.  
**Supported Entity Types:** All  
**Ghidra Reference (For Coders):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetParam`  
**Usage:** `SetCameraParam <cameraParam> <value>`
The value is a decimal number.

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