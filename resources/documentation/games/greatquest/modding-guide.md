# Frogger: The Great Quest Modding Guide
This guide will go over the basics of creating mods for Frogger: The Great Quest.  
Please ensure read the [setup guide](./modding-setup-guide.md) before following this guide.
If you are just trying to install mods, navigate [here](installing-mods.md) instead.  
**NOTE:** This guide is specific to Frogger: The Great Quest, and the information here may not be valid for other games.

## 1) An introduction to the proper way of making mods.
As mentioned in the [setup guide](./modding-setup-guide.md), each mod gets its own folder.
Create a folder with the desired mod name at `<root folder>\<name of mod>`.
This folder is known as the "mod folder" (not to be confused with the root folder established in the setup guide.)  

**Instructions:**  
There are two required files to create in the mod folder: `script.ndl` and `main.cfg`.

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
#icon=image.bmp # (Optional) This is the path to a square image icon representing the mod. This may be displayed in multiple places, so high resolutions are preferred.
Text written here or on subsequent lines is treated as a description of the mod.
```

**script.ndl**  
Create a file called `script.ndl`, and leave it empty for now.
This is a [Noodle](scripting.md) script which will instruct FrogLord on which files to import/apply changes from.  
This will be the most complex part of making mods, so it is strongly recommended to refer to other mods such as [this one](https://github.com/Kneesnap/frogger-tgq-project-puck/blob/main/script.ndl), or asking for help in the Highway Frogs discord server.  
These scripts can be run in FrogLord with `Edit > Run Noodle Script`.  

To avoid the complexity of Noodle, ignore this file for now, as in theory it's not necessary until the mod is ready to share.  

**Everything Else:**  
Other files/folders may be included in the mod folder in any organization, although it is recommended to follow the following directory structure.

```
Folders:
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

Files/Folders inside of each of the previous folders:
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

## 4) Making an example mod.
The biggest key to modding Frogger: The Great Quest is the .gqs file, which can be thought of as a "Great Quest Data Set".
It contains a list of changes to apply to a chunked file (such as a level).  

As an example, save the following as a file named `ModdingExample.gqs`:  
```PowerShell
[DeleteResources]
FrogmotherInst001
```

After saving the .gqs file, using FrogLord, select `01.dat` (Rolling Rapids Creek), and right-click the resource chunk named `scriptdata`.  
Then, select "Import GQS Script Group" and give FrogLord the `ModdingExample.gqs` file.  
That's it! The mod can now be saved with `File > Save` or `Ctrl + S`, and tested by running `PC\GreatQuest.exe`.  
Ensure that the mod worked by loading Rolling Rapids Creek, and seeing that Fairy Frogmother is missing from the tutorial.  

To make further changes to the GQS file, read [the documentation](./modding-gqs-file.md).  

## 5) Next Steps
Now that you've created a modified version of the game, you're ready to take that mod in whatever direction you like!  
From here, read through the [game file guide](./modding-game-files.md) for detailed information on modding the different kinds of files available in the game.  

If you run into any issues or need help, we're always available in the [Highway Frogs discord server](https://discord.gg/XZH9Wa5rMV).

## 6) Sharing Mods
When it becomes time to share the mod, the following steps are performed.  
Do not share `data.bin` file directly, this would be copyright infrigement.
Instead, compress the mod folder into a `.zip` file, before renaming it to `.mod` instead.  
Make sure the `.zip` file has the mod files in the root directory and not in a sub-folder. Afterward, rename the `.zip` to be a `.mod` file instead.  
Ensure installation of the `.mod` file succeeds using FrogLord, and then you're able to share the mod wherever! (It is recommended to share it with [Highway Frogs](https://highwayfrogs.net/) of course!)  

## A note for developers (Optional)
Frogger: The Great Quest's PS2 builds contain exhaustive debug symbols (DWARFv1) for everything except the skeletal animation library.  
This means we are often using the original data structure names, and very often mention original function names for reference.
If you'd like to get Ghidra setup to use these symbols and poke around in the original game, reach out in the discord server.
I'd like to document how to setup Ghidra at some point, but it's low priority.