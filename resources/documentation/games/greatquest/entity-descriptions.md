# Entity Descriptions (Frogger: The Great Quest)
When creating entities in Frogger: The Great Quest, one of the most important things to consider is the entity description.  
Each entity description is like a template, containing basic information about a particular entity that can be reused across many entity instances.  
For example, there are nearly a dozen (entity) instances of Fairy Frogmother in Rolling Rapids Creek, and it would be wasteful to copy-paste all of her information across every single instance.  
However, if one of those Fairy Frogmother entity instances required different data, a completely new entity description would be required.  

# The Basics
As described [here](./modding-gqs-file.md#entitydescriptions), entity descriptions can be created in a `.gqs` file under the `[EntityDescriptions]` section.  

**Entity Types:**  
The first thing to choose when choosing an entity is what kind of entities they are.  
Entities are inheritance-based (Object-Oriented Programming), so each type of entity description inherits all properties from its parent type.  
**Inheritance:**  
```
Entity
 ->ParticleEmitter
 ->Waypoint
 ->ActorBase
  ->Prop
  ->Actor
   ->Character
  ->Item
   ->Coin
   ->Gem
   ->Honeypot
   ->MagicStone
   ->ObjKey
   ->UniqueItem
```
See below for more information on individual entity types.  
Upon selecting an entity type, the remaining description data can be filled out.  

**Properties:**  
```PowerShell
[[{entity name}Desc]] # This will be the name of the entity description. Each entity description may have its own naming convention to follow.
# Controls what kind of entity description the following data will be.
# Available Types: CHARACTER, PROP, UNIQUE_ITEM, PARTICLE_EMITTER, WAYPOINT, ACTOR, ACTOR_BASE
#  COIN, GEM, HONEY_POT, ITEM, MAGIC_STONE, OBJ_KEY, UNIQUE_ITEM
type=...

# When creating a new entity instance with this template in the 3D map viewer, the entity flags specified here will be applied to the entity.
# This rarely will matter when creating entities with GQS, but this data is present in the game files/would be used by the developer's original editor.
# These should be formatted as a comma-separated list, without the "--" prefix.
# A full list of flags is available here:
# https://github.com/Kneesnap/FrogLord/blob/master/resources/documentation/games/greatquest/scripting.md#setflags-both
defaultFlags=...

# Each entity has a sphere surrounding it, centered at the entity's world position.
# This sphere is used mainly for collision, so the sphere should always contain all parts of the collision proxy.
# If the sphere is too small, the player might get stuck when touching the entity collision.
# NOTE: Waypoint descriptions may use a bounding box instead, leaving this data unused.
boundingSpherePos=0, 0, 0
boundingSphereRadius=1
```

# Particle Emitter
Contains information about particle-emitting entities.
```PowerShell
[[{entity name}}]]
<All properties inherited from base entity.>

# Blending is how the particle texture will combine with the pixels displayed behind it.
# The source blend and destination blend settings correspond to situations which have not been reverse engineered yet.
# Available Blend modes: ZERO, ONE, SRC_COLOR, INV_SRC_COLOR, SRC_ALPHA, INV_SRC_ALPHA, DEST_ALPHA, INV_DEST_ALPHA,
#  DEST_COLOR, INV_DEST_COLOR, SRC_ALPHA_SAT, BOTH_SRC_ALPHA, BOTH_INV_SRC_ALPHA
srcBlend=...
dstBlend=...

# The name of the texture ref to use.
texture=...

# Valid values are -1, or between (0, 60).
# Seems to be how long the particle emitter will stay alive for, in seconds.
lifeTime=30.0

TODO: A full kcParticleParam needs to be described here too.
```

# Waypoint
Waypoint entities are invisible, and are primarily used in scripts for purposes ranging from being pathfinding targets to checking when an entity enters/exits a certain part of the world.  
They can have scripts like any other entity, and can even be used to cull out hidden parts of the world.  

```PowerShell
[[{entity name}}WayptDesc]]
<All properties inherited from base entity.>

# Specifies what kind of waypoint this is.
# BOUNDING_SPHERE: A waypoint whose area is defined by the entity bounding sphere.
# BOUNDING_BOX: A waypoint whose area is defined by a bounding box.
# APPLY_WATER_CURRENT: A waypoint whose area is defined by the entity bounding sphere, AND pushes the player in the direction which the UVs are scrolling if the player is swimming and inside the waypoint area.
waypointType=<BOUNDING_SPHERE|BOUNDING_BOX|APPLY_WATER_CURRENT>

# Names of actual entity instances. (Optional)
# When an entity pathfinds to this entity, their pathfind target will update to the nextWaypoint.
# It seems like for entity pathing purposes there's also a prevWaypoint entity, but this seems to be unused.
# Note that these do not actually need to be waypoints, the next target will be set to any entity provided regardless of if it is a waypoint.
prevWaypoint=...
nextWaypoint=...

# If the type is BOUNDING_BOX, this represents the dimensions of the bounding box, in the form "X length, Y length, Z length".
boundingBoxDimensions=1.0, 2.0, 3.0

# The strength is how far the water pushes the player along the current. (IF TYPE IS APPLY_WATER_CURRENT. Otherwise, this is optional.)
strength=0.0
```

# Actor Base
Actor bases represent most entities, but 
In the vanilla game, there are no entities which are directly base actor, only inherited types such as `Prop` and `Character`.  

**Properties:**
```PowerShell
[[{entity name}]]
<All properties inherited from base entity.>

# The name of the model description (3D model ref) to use with this entity.
modelDesc=...

# The name of the collision proxy description (3D model ref) to use with this entity.
proxyDesc=...

# The name of the animation skeleton file to use with this entity.
skeleton=...

# The number of animation channels to use with this entity. (Almost always two)
channelCount=2

# The name of the animation set (list of animations) available to use with the model ref.
animationSet=...

# The name of the table containing action sequence names, to allow resolving action sequences from by their name.
actionSequenceTable=...
```

## Prop
Props are inanimate objects such as chairs, tables, decoration, etc.  
They can still have scripts and animations, but are incapable of AI.  
There are no properties in Prop other than the ones inherited from `Base Actor`.  

## Actor
Actors are a step-up from actor-base, being able to take damage/have health.  
In the vanilla game, there are no entities which are directly base actor, only inherited types such as `Character`.  

**Properties:**  
```PowerShell
<All properties inherited from base actor.>

# The health configuration for the entity. (Optional)
# These values are arbitrary, but 100 is the standard amount in the base game.
maxHealth=100
startHealth=100

# A comma-separated list of damage flags which the entity is immune to/will not take damage from. (Optional)
# Please refer to the following link for a list of damage flags, but do not include the '--' prefix:
#  https://github.com/Kneesnap/FrogLord/blob/master/resources/documentation/games/greatquest/scripting.md#takedamage-script-only
immuneMask=

# How long (in milliseconds) the entity is invincible after taking damage. (Optional)
# Note that this might actually be ignored by the game.
invincibleDurationLimitMs=2000
```

### Character
Characters are capable of using the AI/pathfinding system.  
Pretty much all living/breathing entities are characters.  

**Properties:**
```PowerShell
<All properties inherited from actor.>

characterType=<PLAYER|STATIC|WALKER|FLYER|SWIMMER> # Different kinds of characters are available, with different AI settings.

# Required:

# A comma-separated list of damage flags which the entity is immune to/will not take damage from.
# Please refer to the following link for a list of damage flags, but do not include the '--' prefix:
#  https://github.com/Kneesnap/FrogLord/blob/master/resources/documentation/games/greatquest/scripting.md#takedamage-script-only
weaponMask= 
aggressionTimer=0 # This appears to be a counter. 255 means ALWAYS aggressive (recommended), anything else will assign a timer to this value when damage occurs, then after the timer reaches 0, the entity will no longer be aggressive.
aiMeleeDamage=10 # How much damage to deal in-case of melee attack.
attackGoalPercent=100 # The likelihood of an entity attacking its target (while AI is active).

# Sometimes Required:
attackStrength=10 # This represents the health amount to heal for edible bug entities. Yes, the name is 'attackStrength' that in the actual game.
flyOrSwimSpeed=10 # When the entity is either a swimmer or a flyer, this controls how fast they move.
avoidWater=false # If true, AND THE CHARACTER TYPE IS A FLYER, the entity will be teleported out from under the water back above the surface.

# Optional
homePos=0.0, 0.0, 0.0 # Represents the local offset of the collision proxy (CCharacter::Init)
visionRange=30.0 # Determines the visual range where the character can see other entities within.
visionFov=1.2217305 # The field of view which the entity can see.
hearRange=30.0 # How far away the entity can detect entities within.
meleeRange=1.0 # How far away the entity is from the player to enter their attack sequence.
missileRange=1.0 # How close an entity must be to be targetted an entity with a ranged attack.
monsterGroup=0 # Usually zero. When this value is not zero, and the character is attacked, I believe all other characters of the same group will act as if they were also attacked.
fleePercent=0 # The chance of an entity to flee during combat.
tauntPercent=0 # How likely the entity is to enter their taunt sequence.
wanderGoalPercent=0 # How likely the entity is to start wandering aimlessly.
preferRanged=false # Used in cases where an entity can do both melee and ranged damage, such as the Crossbow Goblin or the Magical General. I'm not sure if this works.
meleeAttackSpeed=10 # The entity's melee attack speed.
rangedAttackSpeed=10 # The entity's ranged attack speed.
preferRun=false # If true, the entity will prefer to use it's "Agg" suffixed animations instead of their regular walk animations. (Agg is short for aggressive)

# There are other unused parameters which may be shown sometimes, but have been omitted from this list because they should not be used.
```

## Item
Items are entities which can be picked up when the player collides with the entity.  
Their collision proxy is hardcoded, meaning the actor base's collision proxy is discarded/ignored.  
In the vanilla game, there are no entities which are directly item, only inherited types such as `COIN`, `GEM`, etc.  
Items automatically spin in-place while the player is nearby.  

There are no properties in Item other than the ones inherited from `Base Actor`.
Theoretically, any item can be handled (not just the ones listed below) via [scripting](scripting.md).  

### Unique Item
Represents an item only used in one level/situation.  

**Properties:**
```PowerShell
[[{entity name}}]] # No suffix is included.
<Inherits all properties from Item>

# Available Types:
# Items which store a quantity:
# BONE, SEED,
# Items which are tracked just as "player has" or "player does not have":
# CLOVER, FAKE_CLOVER, ENGINE_FUEL, TEMPLE_STATUE,
#  SQUARE_ARTIFACT, CIRCLE_ARTIFACT, TRIANGLE_ARTIFACT, BONE_CRUNCHER_STATUE,
#  CROWN, RUBY_SHARD, RUBY_SPHERE, RUBY_TEARDROP
itemType=...
```

### Coin
Represents a collectible coin for the player to pickup.

**Properties:**
```PowerShell
[[{entity name}}]] # No suffix is included.
<Inherits all properties from Item>

# The type of coin to add to the player's inventory when the player picks up the item.
coinType=<COPPER|SILVER|GOLD>
```

### Gem
Represents a collectible gem for the player to pickup.

**Properties:**
```PowerShell
[[{entity name}}]] # No suffix is included.
<Inherits all properties from Item>

# The type of gem to add to the player's inventory when the player picks up the item.
# Unused (broken) option: QUARTZ
gemType=<AMETHYST|RUBY|DIAMOND|SAPPHIRE>
```

### Honeypot
This is the honey pot item seen within Bog Town.  
There are no properties in `Honeypot` other than the ones inherited from `Item`.  

### Magic Stone
Represents one of the various magic stones.
**Properties:**
```PowerShell
[[{entity name}}]] # No suffix is included.
<Inherits all properties from Item>

# The type of stone to add to the player's inventory when picked up.
# Unused (broken) options include: LIGHT, SLEEP, LIGHTNING, WIND, VORTEX
stoneType=<FIRE|ICE|SPEED|SHRINK>
```

### Obj Key
Represents one of the various keys.
**Properties:**
```PowerShell
[[{entity name}}]] # No suffix is included.
<Inherits all properties from Item>

# The type of key to add to the player's inventory when picked up.
keyType=<DOOR|CHEST|SLICK_WILLY|CLOVER_GATE|FAIRY_TOWN_A|FAIRY_TOWN_B|FAIRY_TOWN_C|TREE_OF_KNOWLEDGE|ENGINE_ROOM>
```