# Frogger: The Great Quest Modding Guide
This guide will go over the basics of creating mods for Frogger: The Great Quest.
If you are just trying to install mods, navigate [here](installing-mods.md) instead.  
**NOTE:** This guide is specific to Frogger: The Great Quest, and the information here may not be valid for other games.  

## A note for developers (Optional)
Frogger: The Great Quest's PS2 builds contain exhaustive debug symbols (DWARFv1) for everything except the skeletal animation library.  
This means we are often using the original data structure names, and very often mention original function names for reference.
If you'd like to get Ghidra setup to use these symbols and poke around in the original game, reach out in the discord server.
I'd like to document how to setup Ghidra at some point, but it's low priority.

## 1) Download FrogLord
Instructions: [here](../../download-froglord.md)
TODO: CREATE!

## 2) Getting the right version(s) of the game.
It may be tempting to jump straight into FrogLord and start making changes, but I urge reading this guide first.
Before getting started, you'll need a legally obtained copy of Frogger: The Great Quest.
For modding purposes, you'll want both the PC version and (optionally) the PS2 NTSC version.

**PS2 NTSC (Recommended):**  
The PS2 NTSC version is the recommended version to make mods for, since it is stable, has debug symbols, and is playable on PC via emulation.  

**PC Version (Discouraged, but good for testing):**  
The PC version has issues that make it not desirable for modding such as:
 - Requiring a frame-limiter to function. (Asks more from players)
 - Frequent crashes even with a frame-limiter.
 - No Vertex Coloring
 - Entities visibly T-Posing.
 - Entities deactivating at a much shorter range than the PS2 version.

However, this version is very good for quickly testing changes since it does not need to build a PS2 CD image for every build.

**PS2 PAL (Highly Discouraged):**  
This version is discouraged for modding due to its multi-language capabilities, which are a pain to deal with.
Additionally, it is capped to 50 FPS.

## 3) Creating your first mod
It may appear that FrogLord is the place to start making mods, and when you're done you can just hit save.
Doing that is completely fine for development & testing purposes, but sharing your data.bin file (even if it is modified) is copyright infringement.
Instead, we create .zip files (Renamed to .mod) which contain the files necessary to transform an original copy of the game into your modded version.
This .zip/.mod file (or the unpacked folder it is created from) is usually referred to as just "the mod" or "a mod".

Organizing mods like this comes with other benefits too:
- Enabling mods to support multiple versions/platforms.
- Prevents accidentally corrupting your entire mod due to bugs/human error.
- Allows collaboration with multiple people by using version control software such as Git.
- In certain cases, multiple mods may be possible to apply together.

**Instructions:**  
Create a folder somewhere on your computer, this folder will be the root folder of the mod.
There are two required files, `script.ndl` and `main.cfg`.

**main.cfg:**  
Create a file called `main.cfg`, and copy the following into it. Then, fill it out.  
```batch
name=My Cool Mod # The name of the mod.
game=greatquest # Which game is this mod for?
id=00000000-0000-0000-0000-000000000000 # Replace this with a randomly generated UUIDv4, such as from: https://www.uuidgenerator.net/ This will uniquely identify your mod, allowing other mods to require your mod to be installed.
version=1.0.0 # The mod version. (Should be increased with every release)
author=Kneesnap # Who made the mod?
minFrogLordVersion=1.0.0 # The earliest version of FrogLord which can apply the mod.
#likelyCompatibleWithOtherMods=false # (Optional, default = false), this indicates if this mod is likely to be compatible with other mods.
#icon=image.bmp # (Optional) This is the path to a square image icon representing the mod.
Text written here or on subsequent lines is treated as a description of the mod.
```

**script.ndl**  
Create a file called `script.ndl`. This is a [Noodle](scripting.md) script, which will instruct FrogLord how to modify the game.  
Noodle is easily the most complicated part of making mods, which is why it is strongly recommended to refer to other mods such as [this one](https://github.com/Kneesnap/frogger-tgq-project-puck/blob/main/script.ndl).  

TODO: Noodle link needs documentation.

**Everything Else:**  
Other files/folders may be included however you like, although it is recommended to follow the following directory structure as it will simplify your work in Noodle quite a bit.

```
level01/ Level Data for "Rolling Rapids Creek". (Follows the level folder structure seen below)
level02/ Level Data for "Bog Town". (Follows the level folder structure seen below)
level03/ Level Data for "Slick Willy's River Boat". (Follows the level folder structure seen below)
level04/ Level Data for "River Town". (Follows the level folder structure seen below)
level05/ Level Data for "Mushroom Valley". (Follows the level folder structure seen below)
level06/ Level Data for "Fairy Town Spring". (Follows the level folder structure seen below)
level07/ Level Data for "The Tree of Knowledge". (Follows the level folder structure seen below)
level08/ Level Data for "Fairy Town Summer". (Follows the level folder structure seen below)
level09/ Level Data for "The Cat Dragon's Lair". (Follows the level folder structure seen below)
level10/ Level Data for "Fairy Town Fall". (Follows the level folder structure seen below)
level11/ Level Data for "The Dark Trail". (PC Only) (Follows the level folder structure seen below)
level11A/ Level Data for "The Dark Trail Ruins". (Follows the level folder structure seen below)
level12/ Level Data for "Dr. Starkenstein's Castle". (Follows the level folder structure seen below)
level13/ Level Data for "The Catacombs". (Follows the level folder structure seen below)
level14/ Level Data for "The Goblin Trail". (Follows the level folder structure seen below)
level15/ Level Data for "The Goblin Fort". (Follows the level folder structure seen below)
level16/ Level Data for "The Ruins of Joy Town". (PC Only) (Follows the level folder structure seen below)
level17/ Level Data for "Joy Castle". (Follows the level folder structure seen below)
level18/ Level Data for "The Towers of Joy Castle". (Follows the level folder structure seen below)

Level Folder Structure:
 sfx/ Contains the sound effects (as .wav with the )
 entity-descriptions/ Contains descriptions of entities
 entities/ Contains entity definitions, and scripts which are isolated relatively to individual entities.
 scripts/ Contains files that define scripts which may target multiple entities, but overall are related. (This is just an organization tactic.)
 script.ndl The script to apply the changes for this particular level.
 
TODO: 3D Models, 3D Model Animations, 3D Model Skeletons, 3D Model Collision
TODO: Map Terrain, Map Terrain Collision, etc
TODO: Textures
TODO: More probably.
```

## 4) Testing your first mod.
For the purposes of testing your first mod, we'll keep it simple. `script.ndl` should have this pasted into it:
```js
TODO: ADD!!
```

Once that's done, it's time to test your mod.

TODO: Tell the user how to setup for testing.


## 5) Expanding your mod
Now that you've made your first mod, you're ready to take this in whatever direction you like!
This section is here to teach about how Frogger: The Great Quest works, and how to modify.
This section is here to link resources and provide information on things you should know about while modding.
If you run into any issues or need help, we're always available in the Highway Frogs discord server. TODO: Link

TODO: Guide to hashes and how the game loads files.

TODO: Asset Information [Maps, 3D Models, SFX Info, Textures]