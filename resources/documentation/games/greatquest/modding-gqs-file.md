# Great Quest Chunk Data Set (.GQS Files)
Chunked files in Frogger: The Great Quest contain all the various data chunks loaded for a scene at any given time (such as level or menu screen).  
GQS files are a special FrogLord format which detail the changes applied to a chunked file in order to install the mod.

## Before Starting
In order to understand how `.gqs` configuration files work, make sure to read about [FrogLord configuration files](../../froglord/config-files.md).  

### Hashes
When making GQS files, we try to use text-based names whenever possible.  
But they're actually a lie. The game internally tracks resources and data by IDs, called hashes.  
These hashes look like this: `0x1234ABCD`, and on their own look like gibberish.  
Hashes should only be used when there is no corresponding text available (or in the case of Action Sequences).  
FrogLord shows the hash values for each file and chunk resource when selected.  

## Getting Started
The recommended editor to use for `.gqs` files is [Notepad++](https://notepad-plus-plus.org/downloads/), but other text editors will work, even Visual Studio Code.  
While using Notepad++, it is recommended to set the syntax highlighting to either `Shell` or `PowerShell` with the `Language > ...` menu, to color the scripts.

## Applying GQS Files Manually
In FrogLord:
1. Locate the level you want to modify.
2. Right-click the level's `scriptdata` chunk.
3. Choose "Import GQS Script Group" and select the desired `.qgs` file.
4. Check for warnings shown by FrogLord.
FrogLord will import the GQS file. If FrogLord reports any warnings, make sure to read them, as they often indicate real issues.  
The original game can often trip these warning messages, so if a warning is from the original game, it may be okay to ignore.  

## How to use GQS Script Groups in Noodle?
TODO: Include some information on how to manage scripts with Noodle later.

## GQS Sections
The following sections document each of the available GQS config sections which are available, how they work, and how to configure them.  

### Quick Sections Reference
- `[Models]`             – Adds 3D models and descriptions to chunked file
- `[SoundEffects]`       – Adds references to streamed sound effects in `.SCK`
- `[CopyResources]`      – Copies resources from one `.dat` to another
- `[DeleteResources]`    – Deletes chunks from current level
- `[Animations]`         – Adds animations to Animation Set
- `[Sequences]`          – Creates/replaces Action Sequences
- `[Dialog]`             – Creates/replaces dialog strings
- `[Collision]`          – Creates/updates collision proxies
- `[EntityDescriptions]` – Creates/updates entity templates (Actor, Prop, Item, etc.)
- `[Entities]`           – Creates/updates Entity Instances
- `[Scripts]`            – Adds scripts to existing entities

### [Models]
Adds all included 3D model files as `Model References` to the chunked file.
If `--CreateModelDesc <modelDescName>` is included (which in most circumstances it should be), a corresponding `Model Description` will also be created.  

Example:  
```PowerShell
[Models]
\GameSource\Level00Global\Characters\C036\C036.VTX --CreateModelDesc "GeneralModelDesc" # The Magical General
\GameSource\Level00Global\Characters\C058\C058.vtx --CreateModelDesc "Princess JoyModelDesc" # Princess Joy
\GameSource\Level18JoyTowers\Props\MomRing\MOM_RING.VTX --CreateModelDesc "MomRingModelDesc" # MomRing
\GameSource\Level00Global\Characters\C054\C054.VTX --CreateModelDesc "HollyModelDesc" # Holly
\GameSource\Level00Global\Characters\C062\C062.VTX --CreateModelDesc "PhroiModelDesc" # Phroi
\GameSource\Level17JoyCastle\Level\17DOME.VTX # Sky Box (No need for --CreateModelDesc because Sky boxes aren't entities)
```

### [SoundEffects]
Makes streamed sound effects (the ones found in the global .SCK file) accessible to the level the .gqs file is applied to.  
In more technical terms, it creates a new `Entry` (as opposed to a new `Wave`) in the .SBR file for the active level.  
Currently this does not import any new sound effects, only creates sound effects for sounds already in the `.SCK` file.  

Example:
```PowerShell
[SoundEffects]
characters/MagicalGeneral/hurt0
characters/Phroi/lvl04_sick_response
characters/Phroi/lvl08_dialog_07
characters/Phroi/lvl10_dialog_04
```

### [CopyResources]
Copies resource chunks from one chunked file to the chunked file which the .gqs file is applied to.  

Example:
```PowerShell
# The following resources are copied from Fairy Town Spring to the current level.
[CopyResources]
[[\GameData\Level06FairyTownSpring\Level\06.dat]]
C054.bhe
C054-1AnimSet
Holly{seqs}
C054NrmIdle01.bae
0xBA0EB676 # Holly[NrmIdle01] Idle pose <--- This line contains an example of a raw hash.
C054FlyIdle01.bae
0xA4B8D667 # Holly[FlyIdle01] Flying idle pose <--- This line contains an example of a raw hash.
C054NrmReac01.bae
0xC20C9145 # Holly[NrmReac01] Flinch and cover face <--- This line contains an example of a raw hash.
```

### [DeleteResources]
Deletes resource chunks from the chunked file which the .gqs file is applied to.  

Example:
```PowerShell
# Any resource chunk in the target chunk file could be deleted here, not just entity instances as shown here.
[DeleteResources]
CrateInst005 # Unnecessary; replaced by CrateInst002
CrateInst006 # Unnecessary; replaced by CrateInst003
CrateInst007 # Unnecessary; replaced by CrateInst004
FrogmotherInst002 # Unnecessary; utilizing FrogmotherInst001
FrogmotherInst003 # Unnecessary; utilizing FrogmotherInst001
FrogmotherInst004 # Unnecessary; utilizing FrogmotherInst001
```

### [Animations]
Add animation files to the given `Animation Set`.

Example:
```PowerShell
[Animations]
[[C002-1AnimSet]] # The animation set to add animations to.
C002FlyDodg01.bae
```

### [Sequences]
Creates/replaces Action Sequences.
See the [scripting documentation](./scripting.md) for in-depth information on writing scripts.  

Example:
```PowerShell
[Sequences]
[[Frog]] # The Action Sequence name prefix.
[[[WaveAtGeneral]]] # The name of the action sequence. (FrogLord will create one named 'Frog[WaveAtGeneral]' when combining the name prefix)
hash=0x91A48D4F # This value is random. It uniquely identifies this sequence, so the same number should not be used more than once. Put down a new hash number for every unique action sequence.
# Beyond this line is the action sequence script. See the scripting documentation for more information.
SetAnimation "C001NRMCONV10.BAE" 0.1 --FirstInSequence
WaitForAnimation
SetAnimation "C001NRMCONV10.BAE" 0.1
WaitForAnimation
SetAnimation "C001NrmIdle01.bae" 0.2 --Repeat
```

### [Dialog]
Add/replace `String Resources` (dialog strings) so they can be used in scripts.

Example:
```PowerShell
[Dialog]
WEDDING_BUMBLY_001="Bumbly Dumbly: Go to wedding! :]"
WEDDING_HOLLY_001="Princess Holly: Frogger, over here!"
WEDDING_HOLLY_002="Princess Holly: Please help my sister, I left as soon as I could, but I'm too late."
WEDDING_HOLLY_003="Princess Holly: I find myself unable to do anything without alerting Phroi, who followed me from Fairy Town..."
WEDDING_HOLLY_004="Princess Holly: Since I now know his true identity, I have no doubt he would try to stop me, but perhaps he wouldn't do the same to you."
WEDDING_FROGGER_001="Frogger: Gotcha."
WEDDING_HOLLY_005="Princess Holly: Can you find out what he's planning?"
WEDDING_FROGGER_002="Frogger: Well, ok, I'll try!"
```

### [Collision]
Allows creating/updating `Collision Proxy Descriptions`.

Example:
```PowerShell
[Collision]
[[HollyProxyDesc]]
type=CAPSULE
reaction=SLIDE
collisionGroups=NonHostileEntities
collideWith=TriangleMeshes, Player, NonHostileEntities, HostileEntities, Terrain
radius=0.35
height=0.7
offset=0.2
```

### [EntityDescriptions]
This section can be used to create/update any kind of entity description (Actor descriptions, Prop descriptions, Item descriptions, etc.)  
Check out the [game file documentation](./modding-game-files.md) for details on each available description type.  

Example:
```PowerShell
[EntityDescriptions]
[[Wedding WaypointWayptDesc]]
type=WAYPOINT
flags=HideShadow
boundingSpherePos=0.0, 0.0, 0.0
boundingSphereRadius=1.0
waypointType=BOUNDING_BOX
prevWaypoint=0xFFFFFFFF
nextWaypoint=0xFFFFFFFF
boundingBoxDimensions=10.0, 13.0, 14.0
```

### [Entities]
This section is used to create & update `Entity Instances`.  
Similarly to the `[Scripts]` section, [scripts](./scripting.md) can be added to entities here.  
Unlike `[Scripts]`, any included scripts will remove/replace ALL existing scripts.  

Example:
```PowerShell
[Entities]
[[Wedding Waypoint]] # Waypoint for starting the wedding ceremony
description=Wedding WaypointWayptDesc # The name of the description (template) describing what kind of entity this is.
priority=1 # Controls the priority in which actions from different entities are handled. 1 is fine in most cases.
targetEntity=FrogInst001 # The entity to target. If unsure, use FrogInst001.
flags=HideShadow # For a full list of flags, refer to the GQS scripting documentation.
billboardAxis=Y
position=-1.5, 6.5, -59.5
rotation=0.0, 0.0, 0.0
scale=1.0, 1.0, 1.0

[[[Script]]] # An optional tag to add scripts.
[[[[Function]]]]
cause=OnWaypoint ENTITY_ENTERS "FrogInst001"
SendNumber ENTITY_VARIABLE 0 # Only handle the first time that the Frog enters the waypoint.

[[[[Function]]]] # Handle wedding waypoint trigger.
cause=OnReceiveNumber EQUAL_TO 0
SetVariable 0 1 # Prevent this from getting called again.

SetSequence "NrmIdle04" --IgnoreIfAlreadyActive --AsEntity "HollyInst001"
SetAlarm 1 1.0

[[[[Function]]]]
cause=OnAlarm FINISHED 1
ShowDialog "WEDDING_HOLLY_001" --AsEntity "HollyInst001" # Princess Holly: Frogger, over here!
```

### [Scripts]
Allows defining script functions for entities which are not necessarily created in the same .gqs file.  
Will be added to the pre-existing entity scripts instead of replacing them.  
For detailed information on writing scripts, check out the [scripting documentation](./scripting.md).  

Example:
```PowerShell
[Scripts]
[[FrogInst001]] # The script will be added to the 'FrogInst001' entity.
[[[Function]]] # The script only has one function.
cause=OnReceiveNumber EQUAL_TO 1
SetTarget "FrogInst001"
SetSavePoint 100 -1.5 1.5 -15
DeactivateCamera 0.0
SetAlarm 0 1.0
```
