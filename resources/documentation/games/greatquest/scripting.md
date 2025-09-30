# kcScript (Great Quest Scripting)
Frogger: The Great Quest has most of its "story"/per-level occurrences managed entirely through a scripting system.
This system gives tons of flexibility to create mods since this lets us change almost all level behavior.  
This scripting system is NOT the same thing as Noodle, which is the FrogLord scripting system.
Instead, `kcScript` is a term we made for the scripting system found in the original Frogger: The Great Quest, as the official/original name is not known.

## The Basics
Each entity in a level can have a script, for example Frogger, coins, etc.
These scripts are broken up into any number of functions, which contain a cause (what triggers it), and the effects to apply.  
For example, `OnLevel BEGIN` is a cause which indicates the function should be run when the level begins.
When the function runs, its effects will impact the game such as playing a sound, making entities invisible, etc.  
The following example would play a sound effect when the level loads.

**Example:**  
```powershell
[Function] # When writing scripts, make sure the correct number of square brackets are used. (For more information, refer to the config file documentation and GQS documentation.)
cause=OnLevel BEGIN
PlaySound "sfx/level_start_sfx"
# More script effects could be written here if desired.
```  

Since scripts belong to individual entities, they execute *as* that entity.
For example, if you use the `SetSequence` effect to cause an entity to animate, by default it will apply the animation to the entity which the script is written for.
In order to specify you'd like to run it on a different entity, add `--AsEntity <String: entityName>`.
For example, `SetSequence "NrmIdle01" --AsEntity "FrogInst001"` will run `SetSequence` as the player character, instead of the script owner.
Up until this point, the term "script owner" has meant the entity which the script runs as/was written for.  
But since it's possible to use `--AsEntity` to run effects as other entities, the "script owner" will be treated as the `--AsEntity` target if `--AsEntity` is used.  

## Getting started by creating/modifying scripts
The easiest way to get started is by looking at examples.

To export scripts from the original game:
1) Select a level in FrogLord, and find the chunk named `scriptdata`.  
2) Right-click `scriptdata` and select "Export Scripts".
3) Use a different folder for each level when exporting to avoid overwriting files.

To import new/modified scripts back into FrogLord, the scripts must be written in [GQS Files](./modding-gqs-file.md).  

### GQS Example
```PowerShell
# The following section contains the text/string resources which can be used as dialog text.
# The quotes are optional, but help make syntax highlighters display this file more cleanly.
# Information about GQS files in general are available in the GQS file documentation.
# This example demonstrates how different parts of GQS files can be necessary for scripts to work.
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
# The scripts are added to existing entity scripts, meaning if an entity can have parts of its script spread across different GQS files.

[[FrogmotherInst001]] # The name of the entity to add script functions to.
[[[Function]]] # Define a new function for Fairy FrogMotherInst001. (Note the 3 square brackets).
cause=OnPlayer INTERACT # When the player interacts with Fairy Frogmother.
ShowDialog "FFM_DIALOG_001" # Shows the dialog defined earlier to the player.
```

> [!NOTE]
> When writing `.gqs` files, sections are grouped using square brackets.  
> The number of brackets corresponds to how deeply nested the data is, similar to folders within folders.  
> For further information on writing configuration files see [the documentation](../../froglord/config-files.md).

> [!TIP]
> Create multiple `.gqs` files instead of a single large `.gqs` file for better organization.

## Design Quirks
Unlike every other scripting system/programming language, there is no such thing as an "if statement".

The following example is very easy in most scripting languages, but it is more complex in The Great Quest.
```python
def on_player_interact():
  if not playerHasTalkedToFairyFrogMother:
     start_tutorial() # Only happens if the player hasn't talked to FFM yet.
```

In The Great Quest, it would look like:
```PowerShell
[Entities]
[[FrogmotherInst001]] # Creates a new entity called FrogmotherInst001.
# ... Frogmother's entity data goes here like where she is in the world, what 3D model to use for her, etc.
# It has been skipped because it is not relevant to this example.

[[[Script]]]

# This function will be called when the player interacts with Fairy Frogmother.
[[[[Function]]]]
cause=OnPlayer INTERACT
# The following effect will make FFM send the number currently in entity variable slot zero to herself.
# Therefore, if the number in slot 0 is zero, then FFM will 
SendNumber ENTITY_VARIABLE 0

# This function will run ONLY when FFM receives the number zero.
[[[[Function]]]]
cause=OnReceiveNumber EQUAL_TO 0
SetVariable 0 1 # By setting the value in slot 0 to one, we prevent FFM from talking the next time the player interacts with her.
ShowDialog "FFM 0 prompt 1" # Shows a dialog box containing "Fairy Frog Mother: Hello, Frogger!"
```

The code up there will allow the player to interact with Fairy Frogmother (FFM), and she'll say "Hello, Frogger!".  
But if the player tries to interact with her again, she will completely ignore the player.  
This was possible because of how the example used "entity variables".  

### Entity Variables
Each entity has 8 variable slots available, and are zero-indexed. In other words, the first slot is called 'slot 0', the second slot is called 'slot 1', up until reaching 'slot 7'.
Any whole number can be put in each slot, despite there being only 8 slots per entity.

**What is this used for?**  
If you want to have different behavior from the same cause, use `SendNumber ENTITY_VARIABLE #`.  
That `#` character should actually be a number, which refers to a variable slot. Whatever number is found within that slot will be sent.  
Then, the entity which receives the number (the entity who sent it) checks its functions.  
If it finds a cause `cause=OnReceiveNumber EQUAL_TO <the number sent>`, that function will run.  
This allows 


**Alternative explanation.**  
Variables are extremely powerful when used with the `SendNumber` command.  
Think of `SendNumber` like a postal service, but a crappy one which only delivers a piece of paper containing a single number written on it.  
Each entity can use the `SendNumber` postal service to send one number to themselves or to other entities.  
Then, the entity who receives the number from the postal service will execute its functions caused by `OnReceiveNumber`, if the number they got from the postal service matches the cause.  

For example:
```PowerShell
[Function]
cause=OnReceiveNumber EQUAL_TO 1
ShowDialog "DialogOne"

[Function]
cause=OnReceiveNumber EQUAL_TO 2
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
<!---**Ghidra Reference (Ignore):** `EvLevelBegin, EvLevelEnd`-->
**Usage:** `OnLevel <BEGIN|END>`  

### OnPlayer
**Summary:** Executes when the player has an interaction with the script owner.  
**Supported Entity Types:** Base Actors  
**Usage:** `OnPlayer <INTERACT|BUMPS|TARGET_FOR_ATTACK|PICKUP_ITEM>`  
**Actions:**  
```properties
INTERACT # The player interacts with the script owner. script owner must be at least a kcCActorBase. Code: CFrogCtl::OnBeginAction, CFrogCtl::CheckForHealthBug
BUMPS # The player collides/bumps with the script owner. script owner must be a CProp or a CCharacter. Code: CProp::TriggerHitCallback, CCharacter::BumpCallback
TARGET_FOR_ATTACK # The player targets the script owner for an attack. script owner must be kcCActorBase. Code: CFrogCtl::Spit, CFrogCtl::OnBeginMissile, CFrogCtl::OnBeginMagicStone, and, CFrogCtl::OnBeginMelee
PICKUP_ITEM # The player picks up the script owner as an item. script owner must be CItemDesc. Code: CCharacter::PickupCallback
```

### OnActor
**Summary:** Executes when one of the following actions happens to the script owner.  
**Supported Entity Types:** Base Actors  
**Usage:** `OnActor <BUMPS|HEAL|DEATH>`  
**Actions:**  
```properties
BUMPS # Another actor bumps into the script owner. script owner must be a CProp or a CCharacter. Code: CProp::TriggerHitCallback, CCharacter::BumpCallback
HEAL # The script owner heals. Code: script owner must be at least a kcCActor. kcCActor::OnDamage
DEATH # The script owner dies. Code: script owner must be at least a kcCActor. kcCActor::OnDamage
```

### OnDamage
**Summary:** Executes when the script owner takes a certain type of damage.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::OnDamage`-->
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
<!---**Ghidra Reference (Ignore):** `kcCEntity::AlarmCallback`-->
**Supported Entity Types:** Base Actors  
**Usage:** `OnAlarm <REPEAT|FINISHED> <alarmId>`  
Any whole number between 0 and 31 can be used as an alarm ID.  
To use `OnAlarm`, the alarm must first be activated using `SetAlarm`.  
If `REPEAT` is specified, the cause will run each time the alarm repeats. (Only occurs for `SetAlarm --Repeat`)  
If `FINISHED` is specified, the cause will only run once, when the alarm finishes (which is after all repeats).

### OnPrompt (Unsupported)
**Summary:** Does not work correctly.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::OnCommand[PROMPT]`-->
**Usage:** `OnPrompt <promptName>`

### OnEventTrigger (Unsupported)
**Summary:** Supposed to execute when the specified event triggers, but it actually fires when ANY event triggers.  
<!---**Ghidra Reference (Ignore):** `kcCEventMgr::Trigger`-->
**Usage:** `OnEventTrigger <eventName>`  

### OnDialog
**Summary:** Executes when the given dialog text begins or is advanced.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `EvDialogBegin, EvDialogAdvance`-->
**Usage:** `OnDialog <BEGIN|ADVANCE> <dialogStrName>`  
This event is not global, so it will only work if the entity defining this script cause is also the entity to show the dialog.  
The `dialogStrName` should be the name of the text resource containing the dialog text.  
See `ShowDialog` for more details.  

### OnReceiveNumber
**Summary:** Executes when a number is received matching the given criteria.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnNumber`-->
**Usage:** `OnReceiveNumber <operation> <number>`  
Using the postal system analogy described earlier, `OnReceiveNumber` is for an entity waiting for a number to be sent from the postal system with `SendNumber`.  
Whenever a number is received from the postal system, the entity will check if the number it got matches the criteria it's waiting for.  
In most cases, the criteria/operation is if the number is `EQUAL_TO` the number listened for.  
For example, `OnReceiveNumber EQUAL_TO 12` will make the function run every time the number 12 is received from the postal system, but NOT any other number.  
Only whole numbers (integers) are supported by this cause.  

**Valid Operations:**  
```properties
EQUAL_TO # The received number is equal to <number>. (This is almost always what you'll want to use.)
NOT_EQUAL_TO # The received number is not equal to <number>
LESS_THAN # Received number < <number>
GREATER_THAN # Received number > <number>
LESS_THAN_OR_EQUAL_TO # Received number <= <number>
GREATER_THAN_OR_EQUAL_TO # Received number >= <number>
```

### OnReceivePlayerHasItem
**Summary:** Executes when the script owner receives the value from `SendPlayerHasItem`.  
**Supported Entity Types:** Props & Characters  
<!---**Ghidra Reference (Ignore):** `CCharacter::OnWithItem, CProp::OnWithItem`-->
**Usage:** `OnReceivePlayerHasItem <true|false>`  

When `SendPlayerHasItem <inventoryItem>` is used:  
 - If the player did have the item, then functions with the cause `OnReceivePlayerHasItem true` will execute.
 - Otherwise, any function with the cause `OnReceivePlayerHasItem false` will execute.

Note that there is no way to make `OnReceivePlayerHasItem` restrict which items it will listen for.  
The `SendPlayerHasItem <inventoryItem>` effect is what actually checks for the item, and it only shares whether the player had the item (true/false).  
Thus in order to test for multiple items it would be necessary to use multiple entities, each testing for a different item and having its own `OnReceivePlayerHasItem` listener.  

### OnEntity
**Summary:** Executes when the script owner interacts with a waypoint.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCEntity3D::Notify, sSendWaypointStatus`-->
**Usage:**  
```php
OnEntity ENTERS_WAYPOINT_AREA <waypointEntityName> # Executes when the script owner enters the area of the specified waypoint.
OnEntity LEAVES_WAYPOINT_AREA <waypointEntityName> # Executes when the script owner leaves the area of the specified waypoint.

# The following is used when the script owner is pathfinding towards a Waypoint.
# Or more accurately, when the script owner's target entity is a waypoint.
OnEntity ENTERS_TARGET_WAYPOINT_AREA # Executes when the script owner enters the area of its target waypoint.
OnEntity LEAVES_TARGET_WAYPOINT_AREA # Executes when the script owner leaves the area of its target waypoint.
```

> ![WARNING]  
> If the entity (player or non-player) dies, they will NOT trigger this cause.  

### OnWaypoint
**Summary:** Executes when an entity enters/leaves the script owner (who must be a waypoint)'s area.  
**Supported Entity Types:** Waypoints  
<!---**Ghidra Reference (Ignore):** `sSendWaypointStatus`-->
**Usage:** `OnWaypoint <ENTITY_ENTERS|ENTITY_LEAVES> <entityName>`

> ![WARNING]  
> If the entity (player or non-player) dies, they will NOT trigger this cause.  

## Available Effects
The following documentation explains all script effects/actions found within the game, and how they work.
Some of these may only work in scripts, others only in action sequences.

### DoNothing (Unsupported)
**Summary:** Does nothing, likely a test command.  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand`-->
**Usage:** `Do not use.`  
Not used in the vanilla game.

### EndScript (Unsupported)
**Summary:** Stops running an action sequence. Completely optional/unnecessary.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction`-->
**Usage:** `Do not use.`  
Not used in the vanilla game.

### SetActive (Script Only)
**Summary:** Set whether the script owner is active. (An inactive entity will be invisible and lack collision)  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand -> kcCEntity::ActivateAndUnhide`-->
**Usage:** `Entity.Activate` and `Entity.Deactivate`  
**Alias:** `SetActive <true|false>` (DOES NOT WORK)  
Waypoints are capable of activating invisible parts of the map when this is used. 

### SetEnable (Unsupported)
**Summary:** Does nothing.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand`-->
**Usage:** `Do not use.`  
Not used in the vanilla game.

### TerminateEntity (Both Action Sequences & Scripts)
**Summary:** Remove the script owner from the level.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCEntity::OnCommand -> kcCEntity::OnTerminate`-->
**Usage:** `TerminateEntity`

### SetFlags (Both)
**Summary:** Applies the provided entity flags to the script owner.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`-->
**Usage:** `SetFlags [--Entity Flags...]`  
**Example:** `SetFlags --DisableAI --HideShadow`  

```properties
# Entity Flags:
--Hide # Marks the entity to not be drawn. When set, it will also activate/deactivate the entity, along with collision. However, these can then be undone afterward.
--UnusedBitFlag01 # Couldn't find any behavior tied to this flag, and it is never used by the game.
--ForceStayActive # The entity should stay active even when the player is outside the normal range.

# Entity 3D Flags:
--FaceTargetEntity # Causes the entity to constantly rotate to face its target entity.
--HideShadow # Hides the entity shadow. The shadow will also be hidden if --Hide is set.
--AllowWaypointInteraction # Marks an entity as needing to be registered to trigger waypoint events. This will only work if Reset() is called first, so it's best to keep this on the entity instance, and not via scripts.

# Actor Base Flags:
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

### ClearFlags (Both)
**Summary:** Removes the provided entity flags from the script owner.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`-->
**Usage:** `ClearFlags [--Entity Flags...]`  
**Example:** `ClearFlags --DisableAI --HideShadow`
See `SetFlags` above for a list of flags.  

### InitFlags (Both)
**Summary:** Clears instance entity flags, then applies the provided entity instance flags.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity::OnCommand`-->
**Usage:** `InitFlags [--Entity Flags...]`  
**Example:** `InitFlags --DisableAI --HideShadow`
See `SetFlags` above for a list of flags.  
This can be thought of as a combination of the previous two effects: `ClearFlags` and `SetFlags`.  
Firstly, `InitFlags` will clear (almost) all entity instance flags. Unlike `ClearFlags` these are hardcoded and not the flags provided to `InitFlags` by the user.  
Then, then it will apply the specified flags as if it were `SetFlags`.  

### SetState (Unsupported)
**Summary:** Unimplemented, does nothing.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `Do not use.`  
Not used in the vanilla game.

### SetTarget (Script Only)
**Summary:** Change the script owner's target entity.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand`-->
**Usage:** `SetTarget <targetEntityName>`  
A target entity is used for AI-related operations.  
For example, most enemies have `"FrogInst001"` as their target, so they attack Frogger.  
Others, such as Fairy Frogmother face the player by setting their target as `FrogInst001`.

> [!IMPORTANT]
> If the `--DisableAI` flag is set, the entity will not pathfind UNLESS the target is a waypoint.

> [!CAUTION]
> If the entity has the `--FaceTargetEntity` flag set, it may not be able to move vertically (up/down).

### SetAnimationSpeed (Both)
**Summary:** Sets the animation speed for the script owner.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `SetAnimationSpeed <speed>`  
Speed is a decimal number, likely multiplicative, so 1.0 would be 1x speed, 2.5 is 2.5x speed, etc.  
Not used in the vanilla game.

### SetAxisPosition (Script Only)
**Summary:** Sets the script owner's positional coordinate on the given axis.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCEntity3D::OnCommand`-->
**Usage:** `SetAxisPosition <X|Y|Z> <coordinate>`  
**Example:** `SetAxisPosition X 22.5`  
Not used in the vanilla game.  
See `SetPosition` for any limitations.  

### SetPosition (Script Only)
**Summary:** Sets the script owner's position.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCEntity3D::OnCommand`-->
**Usage:** `SetPosition <x> <y> <z>`  
**Warning:**  
```
This command does not work properly when used on the player if collision sits between the player and the new position.
If collision is disabled for the player, the teleport will work up until the moment collision is re-enabled, when the player will be snapped back.

It is recommended to fade the screen out, and perform a series of teleports (With at least one frame of delay between each) to get the player to the destination without going through any collision.
The other alternative is to set the player's respawn point and kill the player which is faster for testing, but not great for player experience.

Further research/debugging is necessary to determine what causes this issue.
```

### AddToAxisPosition (Script Only)
**Summary:** Offsets the script owner's current position by the given value on the specified axis.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCEntity3D::OnCommand`-->
**Usage:** `AddToAxisPosition <X|Y|Z> <amount>`  
Not used in the vanilla game.  
See `SetPosition` for restrictions.

### AddPosition (Script Only)
**Summary:** Adds an offset to the script owner's current position.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCEntity3D::OnCommand`-->
**Usage:** `AddPosition <x> <y> <z>`  
See `SetPosition` for restrictions.

### SetAxisRotation (Script Only)
**Summary:** Sets a rotation value on the specified axis.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCEntity3D::OnCommand`-->
**Usage:** `SetAxisRotation <X|Y|Z> <angleInDegrees>`  
Not used in the vanilla game.

### SetRotation (Script Only)
**Summary:** Sets the script owner's rotation.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCEntity3D::OnCommand`-->
**Usage:** `SetRotation <xAngleInDegrees> <yAngleInDegrees> <zAngleInDegrees>`  
Not used in the vanilla game.

### AddToAxisRotation (Both)
**Summary:** Adds a rotation value on the specified axis.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`-->
**Usage:** `AddToAxisRotation <X|Y|Z> <angleInDegrees>`  
Not used in the vanilla game.

### AddRotation (Both)
**Summary:** Adds rotation values to the existing rotation.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`-->
**Usage:** `AddRotation <xAngleInDegrees> <yAngleInDegrees> <zAngleInDegrees>`  
Not used in the vanilla game.

### RotateRight (Both)
**Summary:** Rotates the script owner to their right.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`-->
**Usage:** `RotateRight <angleInDegrees>`  

### RotateLeft (Both)
**Summary:** Rotates the script owner to their left.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCEntity3D::OnCommand`-->
**Usage:** `RotateLeft <angleInDegrees>`  

### LookAtTargetEntity (Both)
**Summary:** Make the script owner face its target entity.  
**Supported Entity Types:** All 3D Entities  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand, kcCEntity3D::OnCommand`-->
**Usage:** `LookAtTargetEntity`  
Not used in the vanilla game.

### SetAnimation (Both)
**Summary:** Changes the animation currently performed.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `SetAnimation <animationFileName> <transitionTime> [--Repeat] [--FirstAnimationInSequence] [--StartTime <startTimeInSeconds>]`  
The `transitionTime` argument is how long it takes to switch (blend?) from the current animation to the new one. 0 would be instant.  
While this effect appears to work outside an action sequence, the game scripts always use `SetSequence` instead of directly calling `SetAnimation`.  
In other words, this command should mainly be called from action sequences, and not scripts.  
Scripts should instead use `SetSequence` to apply the sequence which then in-turn calls `SetAnimation`.  
It has been done this way so that the AI system can also activate sequences without causing major visual issues depending on what the script is doing.  

### SetSequence (Script Only)
**Summary:** Sets the script owner's active action sequence.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `SetSequence <actionSequenceName> [--IgnoreIfAlreadyActive] [--OpenBoneChannel]`  
```properties
--IgnoreIfAlreadyActive
# If this is set, and the sequence we'd like to apply is already the current sequence,
# then this flag indicates that we'd like to not reset the sequence, and let the existing playback complete.
# Mrs. Boxy in Mushroom Valley really should have been configured to use this, since you can spam interact her and she'll continually restart her animation.

--OpenBoneChannel
# Appears unused in the vanilla game.
# This seems to combine the current animation with any new ones
# Eg: Only the bones which aren't animated on the existing sequence will be animated from the new one.
```

### Wait (Action Sequence Only)
**Summary:** Wait a given amount of time before continuing the action sequence.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction`-->
**Usage:** `Wait <timeInSeconds>`  
Not used in the vanilla game.

### WaitForAxisRotation (Action Sequence Only)
**Summary:** Waits for an axis rotation to complete before continuing the action sequence.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction`-->
**Usage:** `WaitForAxisRotation <X|Y|Z>`  
Not used in the vanilla game.

### WaitForFullRotation (Action Sequence Only)
**Summary:** Waits for all rotations to complete before continuing the action sequence.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction`-->
**Usage:** `WaitForFullRotation`  
Not used in the vanilla game.

### WaitForAnimation (Action Sequence Only)
**Summary:** Waits for the active animation to complete before continuing the action sequence.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction`-->
**Usage:** `WaitForAnimation`  

### Loop (Action Sequence Only)
**Summary:** The action sequence will restart the number of times specified.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction`-->
**Usage:** `Loop <numberOfTimesToLoop>`  

### ApplyImpulse (Both)
**Summary:** Applies a physics-based motion "impulse" (instantaneous force) to the script owner.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `ApplyImpulse <x> <y> <z>`  
This will only work if the `--EnablePhysics` flag is applied to the script owner.  
The physics system in the game is not currently reverse engineered, but this is most likely for impulse-based dynamics (physics simulation).  
That would mean that "impulse" means "the change in momentum of an object".  
So in other words, `ApplyImpulse` changes the momentum of the entity.  
For example, `ApplyImpulse 0 100 0` would launch the entity flying into the air by around the size of Frogger's 3D model.

> [!IMPORTANT]  
> Make sure the `--EnablePhysics` flag is set, and the `--EnableTerrainTracking` flag is **NOT** set.

> [!IMPORTANT]  
> Only `CCharacter` entities can use this command. Other entities such as `CProp` entities will need to be converted into a `CCharacter` to work properly.
<!---
It's CCharacter::ResetInt which applies a special flag (0x1000) that enables motion at all.
So, even though this action looks like it's supposed to work for most entity types, in practice it only works on CCharacter entities.
-->

> [!CAUTION]  
> If the entity is stuck inside of terrain or another entity, physics such as gravity, impulses, etc. may not occur.  

### Prompt (Unsupported)
**Summary:** This was never fully supported by the game, but it looks like it was supposed to allow the player to make choices within dialog text-boxes.  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `Prompt <promptName>`  
Not used in the vanilla game.

### ShowDialog (Script Only)
**Summary:** Creates a dialog box with text for the player.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `ShowDialog <dialogResourceName>`  

> [!IMPORTANT]
> This command is more complicated than it would initially seem.
> It would seem intuitive to use it like `ShowDialog "Bruiser: You got my honey yet?"`.  
> However, this will not work. In-game this would show a dialog box with the text "not found".  
> This is because `ShowDialog` is expecting the name of a text resource containing the dialog text, and not the dialog text itself.  
> So, `ShowDialog "DIALOG_004"` would work if there is a text resource named `DIALOG_004` in the level.  
> This allowed the original team to translate the game into multiple languages without having to copy the scripts for every single language.  
> Instructions for adding text/string resources are in the documentation near the start of this file, but can also be found [here](./modding-gqs-file.md#dialog)  

### SetAlarm (Both)
**Summary:** Sets an alarm to ring (Script Cause: `OnAlarm`) after a delay.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCEntity::OnCommand`-->
**Usage:** `SetAlarm <alarmId> <durationInSeconds> [--Repeat <numberOfTimesToRepeat>]`  
Any number between 0 and 31 is a valid alarm ID.  
The duration of the alarm can be a decimal number.  
The timer will start counting down from the number of seconds given.  
Once the timer reaches 0, it will send `OnAlarm` with the alarm ID provided.  
The main purpose of this feature is to run script effects after a delay.  

### TriggerEvent (Both)
**Summary:** Triggers a named event.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::ProcessAction, kcCEntity::OnCommand`-->
**Usage:** `TriggerEvent <eventName>`  

**Valid Events:**  
```properties
"LevelCompleted" # Destroys all active cameras, and sets a flag for completing the level. Triggered by in-game scripts.
"BeginScreenFade" # Causes the screen to fade to black. Called by a lot of things.
"EndScreenFade" # Unfades/unhides the contents of the screen. Called by a lot of things.
"LockPlayerControl" # Disables controller/keyboard input from influencing the player character. NOTE: This will be automatically be enabled when a dialog box opens, and disabled when closed, so using dialog will unlock player control. Exclusively called from scripts. 
"UnlockPlayerControl" # Re-enables controller/keyboard input for the player character. Exclusively called from scripts.
"ShakeCameraRand" # Shakes the camera randomly. Exclusively used by scripts. NOTE: If this does nothing, ActivateCamera/DeactivateCamera should be used immediately before/after this event is triggered.
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
```

**Events which could be used in very rare situations:**  
```properties
"LevelLoadComplete" # The game will load the sky box, water, setup lighting, and setup environment render states from kcEnvironment. Usually called by ExecuteLoad() completing.
"LevelBegin" # Sets up default data like coin pickup particles, the AI system, adds the system entities. Called when the level start FMV ends.
"LevelEnd" # Stops all sound effects and sends the OnLevel script cause for completing the level. -> Not sure what triggers this event.
"LevelUnload" # Cleanup/remove water & sky dome, stop all sounds, hide the HID, unload main menu/interface resources. Called by exiting the pause menu requesting to quit the game (PauseEndCase) or the level stops. (PlayEndCase).
"DialogBegin" # Displays the dialog text box, and sends the script cause `OnDialog BEGIN`. Called by the handler for the script command 'ShowDialog' (kcCActorBase::OnCommand).
"DialogAdvance" # Hides the dialog text box, and sends the script cause `OnDialog ADVANCE`. Called by the dialog update logic (kcCDialog::Update).
"DialogEnd" # Never called, but if it were it would hide the dialog text box and sends the script cause `OnDialog END`. Re-enables player input.
```

**Events that exist but probably shouldn't ever be used:**  
```properties
"StartMovie" # Seems to setup FMV/movie playback. Called by PlayMovieUpdate
"CutMovie" # Stops movie playback. Called when the player skips an FMV or it completes. (MovieDoneOrRequestAdvance)
"MovieContinueGame", # Seems to setup the game to continue playback. Registered in PlayMovieUpdate(). I don't think this is ever called.

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
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand, kcCEntity3D::OnCommand`-->
**Usage:** `PlaySound <soundFilePath>`  
A sound file path can be obtained by right-clicking a sound in the FrogLord sound list, and clicking "Copy file path".  
To stop all sounds, run `TriggerEvent "LevelEnd"`.  
This is safe to do even when the level isn't over, but it will still trigger the cause `OnLevel END` after stopping sounds.  

### SetVariable (Script Only)
**Summary:** Sets one of the script owner's entity variables by its ID.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand`-->
**Usage:** `SetVariable <variableId> <value>`  
Stores the value into the variable ID/slot given.  
Valid variable IDs are between 0 and 7.  
The provided value must be a whole number.  
The only way to use a variable is with the `SendNumber` effect.  

> ![NOTE]
> Variables can only represent whole numbers, between `-32768` and `32767`.  

### AddToVariable (Script Only)
**Summary:** Adds a value to one of the script owner's entity variables by its ID.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand`-->
**Usage:** `AddToVariable <variableId> <value>`  
Adds the value into the variable ID/slot.  
Valid variable IDs are between 0 and 7.  
The provided value must be a whole number.  
The only way to use a variable is with the `SendNumber` effect.

> ![NOTE]
> Variables can only represent whole numbers, between `-32768` and `32767`.

### SendNumber (Script Only)
**Summary:** Sends a number, which will cause the `OnReceiveNumber` script cause.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand`-->
**Usage:** `SendNumber <LITERAL_NUMBER|ENTITY_VARIABLE|RANDOM> <number>`  
Think of `SendNumber` like a postal service, but a crappy one which only delivers a piece of paper containing a single number written on it.  
Each entity can use the `SendNumber` postal service to send one number to themselves or to other entities.  
Then, the entity who receives the number from the postal service will execute its functions caused by `OnReceiveNumber`, if the number they got from the postal service matches the cause.

```properties
LITERAL_NUMBER # The number sent with the postal service is the argument named <number> in the above example.
ENTITY_VARIABLE # The number sent with the postal service is the value in the provided entity variable slot.
RANDOM # The number sent with the postal service is a random number between 0 and provided number.
# When using RANDOM, the number provided is exclusive, so for 'SendNumber RANDOM 5' the random numbers generated are between 0 and 4.
```

If the `--AsEntity` flag is included, the number will be sent to the `--AsEntity` target instead of the script owner.  
When sending an `ENTITY_VARIABLE` the number sent will be the value of the variable obtained from the script owner, instead of from the `--AsEntity` target.  

### SpawnParticleEffect (Script Only)
**Summary:** Sets up a particle emitter for the script owner.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::OnCommand/kcCActor::OnCommand -> kcCParticleMgr::SpawnEffect`-->
**Usage:** `SpawnParticleEffect <particleEmitterDataName>`  
Not used in the vanilla game.

### KillParticleEffect (Script Only)
**Summary:** Disables particle effect(s) spawned by the script owner.  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `KillParticleEffect`  
Not used in the vanilla game.

### Launcher (Unsupported)
**Summary:** Opens up a dialog box saying it is no longer supported.  
<!---**Ghidra Reference (Ignore):** `CCharacter::OnCommand`-->
**Usage:** `Do not use.`  
Not used in the vanilla game.

### SendPlayerHasItem (Script Only)
**Summary:** Send whether the player has the given item, thus causing `OnReceivePlayerHasItem`.  
**Supported Entity Types:** Character or Prop  
<!---**Ghidra Reference (Ignore):** `CCharacter::OnCommand, CProp::OnCommand`-->
**Usage:** `SendPlayerHasItem <inventoryItem>`  
Click [here](../../../../src/net/highwayfrogs/editor/games/konami/greatquest/generic/InventoryItem.java) to see a list of InventoryItem values.  
Using the `--AsEntity` flag will change both the sender and the receiver, unlike `SendNumber` which would change only the entity receiving the number, not the sender.  

### SetPlayerHasItem (Script Only)
**Summary:** Add or remove an inventory item in the player's inventory.  
**Supported Entity Types:** Character or Prop  
<!---**Ghidra Reference (Ignore):** `CCharacter::OnCommand, CProp::OnCommand`-->
**Usage:** `SetPlayerHasItem <inventoryItem> <true|false>`  
Click [here](../../../../src/net/highwayfrogs/editor/games/konami/greatquest/generic/InventoryItem.java) to see a list of InventoryItem values.

> ![NOTE]  
> `STONE_FIRE`, `STONE_ICE`, `STONE_SPEED`, `STATUE`, and `CROWN` are tracked between levels/in the save file.  
> This can be expanded by changing the hardcoded list of inventory items named `CarryOverItems` in the executable, or by patching `CInventory::Reset` to not reset these items.  

### TakeDamage (Script Only)
**Summary:** The script owner takes damage (loses health).  
**Supported Entity Types:** Base Actors  
<!---**Ghidra Reference (Ignore):** `kcCScriptMgr::FireActorEffect[Remap] -> kcCActorBase::OnCommand/kcCActor::OnCommand`-->
**Usage:** `TakeDamage <attackStrength> <Damage Flags...>`  
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
<!---**Ghidra Reference (Ignore):** `CCharacter::OnCommand`-->
**Usage:** `SetSavePoint <savePointId> <x> <y> <z>`  
The player's respawn position will be set to the new coordinates.  
Also, the game will find an entity named `"Save pointInstXXX"` where `XXX` is the `savePointId`.  
If such an entity is found, particles will be played at the position of that entity.  

### SetUpdatesEnabled (Script Only)
**Summary:** Sets whether updates are enabled for the script owner.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand`-->
**Usage:** `Entity.EnableUpdates` and `Entity.DisableUpdates`  
**Alias:** `SetUpdatesEnabled <true|false>` (DOES NOT WORK)  
See the documentation for "entity updates" below for an explanation.  
Note that this will not prevent the game from activating/deactivating entity updates based on if it is on-screen or not.  

### SetAIGoal (Script Only)
**Summary:** Sets the script owner's AI goal.  
**Supported Entity Types:** CCharacter  
<!---**Ghidra Reference (Ignore):** `CCharacter::OnCommand`-->
**Usage:** `SetAIGoal <FIND|FLEE|WANDER|GUARD|DEAD|SLEEP>`  
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
**Summary:** Attaches a sensor to the script owner.  
**Supported Entity Types:** CCharacter  
<!---**Ghidra Reference (Ignore):** `CCharacter::OnCommand`-->
**Usage:**
```ruby
# Makes the given 3D model bone deal damage when a bounding sphere surrounding the bone intersects with another entity.
# In other words, it makes a part of the script owner's 3D model deal damage to any entity it touches.
# The bone will usually be an arm, but some entities will use bones such as part of a sword.
# This makes combat appear more fluid, as the player will only react to getting hit the moment they are hit.
# The 'radius' value is a decimal number representing the collision/bounding sphere's radius.
# Any collision group can be used instead of just --Player, if you'd like to have monsters able to damage each other.
# See the collision documentation below for a list of valid groups.
Attach ATTACK_SENSOR <boneNameOrId> <radius> <--Player or other collision group>
AttachSensor <boneNameOrId> <radius> <--Player or other collision group>

# Enables a listener to allow collision script events to fire
# Without doing this, I don't believe entities will fire collision events.
# The 'radius' value is a decimal number representing the collision/bounding sphere's radius.
# Any collision group can be used instead of just --Player, if you'd like to have monsters able to bump each other.
# See the collision documentation below for a list of valid groups.
Attach BUMP_SENSOR <boneNameOrId> <radius> <--Player or other collision group>

# Enables a projectile launcher.
# I'm not sure yet if this means to launch a projectile or just to enable it.
Attach LAUNCHER <boneNameOrId> <launcherParamName>

# Creates a particle emitter, attached to a bone in the script owner.
Attach PARTICLE_EMITTER <boneNameOrId> <particleEmitterParamName>
```

### Detach (Script Only)
**Summary:** Detaches a previously attached PARTICLE_EMITTER from a bone on the script owner.  
**Supported Entity Types:** CCharacter  
<!---**Ghidra Reference (Ignore):** `CCharacter::OnCommand`-->
**Usage:** `Detach PARTICLE_EMITTER <boneNameOrId>`

### SetWorldActive
**Summary:** Set whether entities/terrain are enabled in the world area covered by the waypoint.  
**Supported Entity Types:** Waypoint  
<!---**Ghidra Reference (Ignore):** `kcCEntity::OnCommand`-->
**Usage:** `Entity.ActivateSpecial <ENTITIES|TERRAIN|BOTH> <true|false>`  
**Alias:**  `SetWorldActive <ENTITIES|TERRAIN|BOTH> <true|false>` (DOES NOT WORK)  
Not used in the vanilla game.  
This command will control if the world segments (OctTree nodes) the waypoint resides within are active.  
In large levels such as The Goblin Trail or Joy Castle, if the player uses glitches to skip ahead, parts of the map are invisible.
When the player reaches certain points in the map, the vanilla game will make those areas visible by running `Entity.Activate` on waypoints covering the invisible area.  
Unlike `SetActive`, `Entity.ActivateSpecial` is able to specify entities separately from terrain.  

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
<!---**Ghidra Reference (Ignore):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnActivatePivotCamera`-->
**Usage:** `ActivateCamera <transitionInSeconds>`  
`transitionInSeconds` is a decimal number indicating how long it will take (in seconds) to switch to the new camera.

### DeactivateCamera (Script Only)
**Summary:** Deactivates the current camera, reverting to the previous camera.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnDeactivatePivotCamera`-->
**Usage:** `DeactivateCamera <transitionInSeconds>`  
`transitionInSeconds` is a decimal number indicating how long it will take (in seconds) to switch to the previous camera.

### SetCameraTarget (Script Only)
**Summary:** Sets the entity which the current camera focuses on.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetTarget`-->
**Usage:** `SetCameraTarget <entityName>`

### SetCameraPivot (Script Only)
**Summary:** Set the rotational pivot entity for the current camera.
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetPivot`-->
**Usage:** `SetCameraPivot <entityName>`  
The pivot entity is an entity which the camera will use to calculate where to be in the world by finding a position that puts the pivot entity between the camera and the camera's target entity.  
In other words, the position/rotation of the camera is calculated by facing the target entity in a manner that also makes the camera directly face the pivot entity.

### SetCameraParam (Script Only)
**Summary:** Change the current camera's settings.  
**Supported Entity Types:** All  
<!---**Ghidra Reference (Ignore):** `kcCScriptMgr::FireCameraEffect -> kcCCameraStack::OnSetParam`-->
**Usage:** `SetCameraParam <cameraParam> <value>`
The value is a decimal number.

```properties
# kcCameraPivotParam Values:
PIVOT_DISTANCE # How much distance to put between the camera and the pivot entity.
TARGET_OFFSET_X # An offset to the position the camera looks at (the target entity).
TARGET_OFFSET_Y # An offset to the position the camera looks at (the target entity).
TARGET_OFFSET_Z # An offset to the position the camera looks at (the target entity).
PIVOT_OFFSET_X # An offset to the pivot position (the pivot entity).
PIVOT_OFFSET_Y # An offset to the pivot position (the pivot entity).
PIVOT_OFFSET_Z # An offset to the pivot position (the pivot entity).
TRANSITION_DURATION # How long the camera transition should take.
CAMERA_BASE_FLAGS # The flags to apply to the camera entity. (Currently undocumented/unknown)
```

## Extra Documentation
This section contains extra documentation for how certain parts of the game work.

### Entity Activation
What does it mean for an entity to be active/inactive, such as when set with `SetActive`?

**While the entity is active:**
- Its shadow is capable of rendering. (Ghidra: `sRenderGrouped`)
- Entity updates are most likely enabled. (See below)
- Entity will most likely be visible. (Script effects which set the active flag inadvertently set the hidden flag.)

**While an entity is not active:**
- It becomes active if the entity is not hidden, on-screen, and less than 15 world units away from the camera. (Ghidra: `sRenderGrouped`)
- Entity updates are most likely disabled. (See below)
- Entity will most likely be hidden. (Script effects which set the active flag inadvertently set the hidden flag.)

**Features independent of the entity being active:**
- Entity Scripts

**Ways to activate:**
```properties
# Script Effect
# This will directly set if the entity is active or not.
SetActive

# Script Effects
# These will set the activation state based on the state of the 'Hidden' flag.
# Ie: If hidden flag is set, deactivate, otherwise activate.
# The update occurs regardless of if the hidden flag changed.
SetFlags
InitFlags
ClearFlags

# Entities which are on-screen (in the view frustrum), and less than 15 (sqrt kcCOctTreeSceneMgr::msTouchRangeSquared)
# world units away from the camera will be set active/inactive based on if they are not hidden every frame.
sRenderGrouped # Ghidra Reference

# If an entity resets, the activation state will update.
kcCEntity::Reset # Ghidra Reference.
```

#### Entity Updates
When an entity is activated/deactivated, entity updates will be enabled/disabled based on the activation state.  
However, it is possible to modify independently of the activation state with the `SetUpdatesEnabled` script effect.  

When the level starts (after data is applied, but before the level begin script cause triggers), all entities (except the player) have their updates disabled.  
Any entities set active/inactive by scripts will have their updates set.

Every frame, all entities who have updates enabled call their update method.
- Tick Animations (`kcCActorBase`)
- Update Animation Sequence (`kcCActorBase`)
- Entity Collision (`kcCActorBase`)
- Terrain Tracking & Physics (`kcCActorBase`)
- Update Entity AI (`CCharacter`)
- Rotate Item (`CItem`)
- Update Health (`kcCActor`)

**Afterward, if it has been at least 60 frames since either the entity was in the view frustum OR the player was not within the touch distance of 15 world units, updates are disabled.**  
This check can be bypassed with the `--ForceStayActive` flag.

Then, when entities are rendered, if the entity is within the view frustum (scheduled for rendering) and is within the touch range of 15 world units, they will be forced inactive if hidden, and forced active if visible.  
Thus, the entity updates flag is only possible to control when the entity is far away from the player.  
This functionality is not impacted by the `--ForceStayActive` flag.  

### Waypoints (World Visibility)
Waypoint entities have special behavior applied to them.  
When a waypoint entity is activated/deactivated, all entities & terrain buffers which had collision intersecting the waypoint will be activated/deactivated as well.
Waypoints do not appear to automatically activate/deactivate based on player position like the rest, presumably because they are not rendered.  
NOTE: Bounding boxes must ALSO have bounding spheres set which cover the same area too, otherwise the game will skip certain parts of the world. (`sTestWaypointIntersection`)

## Entity Descriptions
TODO: Document Entity descriptions.

## Collision
Collision happens through what are called "collision proxies", which are stand-ins for objects (terrain, entities, etc.)
These proxies are 3D shapes which aren't shown in-game, but can be previewed using FrogLord.  
There are two kinds of proxies, "capsules" and "triangle meshes".

**Capsules (kcCProxyCapsule):**  
A "proxy capsule" is a pill-shaped area (a cylinder with a round top/bottom). These are very fast but not very flexible.

TODO: Creating through configurations.
TODO: Example image?

**Triangle Meshes (kcCProxyTriMesh):**  
A "triangle mesh" is a just a normal 3D model, but without any textures.
These are more flexible than capsules but do not perform very well.  
Note that these cannot have animations like the models which get displayed in-game can.  

TODO: Creating through configurations.
TODO: Example image?

### There's more to collision though!
Before the proxy will be tested, there is a sphere on every actor description.  
This sphere is a very fast collision test which gets checked BEFORE the proxies are tested.  
This is because spheres are extremely fast/quick/easy to check, so the sphere can eliminate some of the more heavy collision checks.  


### Waypoint Collision
Waypoints are special, as they do not have collision proxy data.  
They can either have a `BOUNDING_BOX` or a `SPHERE` shape.
If a sphere is chosen, the entity sphere described before is used.  
If a bounding box is selected, the sphere appears to be completely ignored, and the box dimensions are found in the waypoint description data instead of a collision proxy.  

### Items
Items are also special, because their collision proxy data is hardcoded. (Applied by `CItem::Init`.)  
Even though it may appear possible to assign custom data to them, it will be overriden on load.  

### Collision Groups
The game groups together certain similar entities so that collision which should be skipped can be skipped quickly.  
For example, enemies don't need to collide with coins/gems/etc, but they do need to collide with the player.  
So each entity description has a "collisionGroup" field to indicate what group(s) it is part of, as well as a "collideWith" field to indicate which collision groups the entity should collide with.  
The following are the collision groups which can be used:
```properties
TriangleMeshes # 00
Player # 01
NonHostileEntities # 02
HostileEntities # 03
PlayerKicks # 04 (The player's feet while they are kicking)
PlayerPunches # 05 (The player's hands while they are punching)
Flyers # 11
Swimmers # 12
Sensors # 14 (Attack / Bump Sensors)
Items # 15
Terrain # 16
Climbable # 31, Seems to be set on climbable models such as ladders and vines.
# There are a ton more possible collision groups than the ones listed above, but most are unused.
# Thus, they are free to be used for whatever kind of purpose the mod-creator likes.
# To specify one of the unused collision groups, use the following:
UnnamedGroupXX # Where XX is a number between 0 and 31 and is not one of the numbers listed above. For example: UnnamedGroup17
```