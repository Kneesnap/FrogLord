# Setup Guide
The following contains guides for setting up a workspace for modding Frogger: The Great Quest.

## 1) Getting Started
It is strongly recommended to create mods using the PC version of the game before porting them to PS2 because the PC version enables much faster testing.

First, create a folder somewhere memorable. This folder will be the root of ALL of your Great Quest data.  
Keeping an organized workspace is strongly recommended due to the nature of the game.  
This folder will be referred to as the "root folder" or the "mod folder" throughout the guide.

### Getting the right version(s) of the game.
It may be tempting to jump straight into FrogLord and start making changes, but but reading this guide first can save you a lot of time and frustration.  

Before getting started, you'll need a legally obtained copy of Frogger: The Great Quest.
> [!NOTE]
> For modding purposes, you'll want both the **PC version** and (optionally) the **PS2 NTSC version**.

**PC Version (Recommended for testing, Discouraged for sharing mods):**  
The PC version has issues that make it undesirable for modding such as:
- Requiring a frame-limiter to function. (Asks more from players)
- Frequent crashes even with a frame-limiter
- No Vertex Coloring
- Entities visibly T-Posing
- Entities deactivating at a much shorter range than the PS2 version

However, this version is very good for quickly testing changes since it does not need to build a PS2 CD image for every build.

**PS2 NTSC (Recommended for sharing mods, discouraged for testing):**  
The PS2 NTSC version is the recommended version to make mods for, since it is stable, has debug symbols, and is playable on PC via emulation.

**PS2 PAL (Highly Discouraged):**  
This version is discouraged for modding due to its multi-language capabilities, which are a pain to deal with.
Additionally, it is capped to 50 FPS.

### Setting up the game.
Unless otherwise specified, the guide assumes the use of the PC version.  
In the root folder, start by creating a new folder called "PC".  
Next, we'll install the game.

**If Frogger: The Great Quest is already installed:**
1) Copy all game files (Usually found at `C:\Program Files (x86)\Konami\Frogger\Frogger - The Great Quest`) into the previously created `PC` folder.
2) Next, open the Windows registry editor (`regedit.exe`). The game keeps track of where it was originally installed, and will not use files from the mod folder unless the registry is updated.
3) Copy-paste `Computer\HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\{745B416E-1796-43E6-9497-BAF7AFE11821}` into the text field at the top of the application.
4) Change `InstallLocation` to be the full file path to the previously created `PC` folder. **IMPORTANT:** Make sure the file path ends with a backslash (`\`) — this is required to run the game.
5) The game should now be possible to run from the `PC` folder.

**If Frogger: The Great Quest is not currently installed:**  
Install your legally obtained copy of the game, and make sure to select the previously created "PC" folder as the installation directory.  
If an incorrect installation directory is chosen, follow the steps for an already installed version of the game to fix it.

## 2) Fixing the PC version.
The PC version of Frogger: The Great Quest has game-breaking issues which must be fixed in order to make mods effectively.  
Follow [the PC port guide](./pc-version-fixes.md) in order to properly set up the PC version.

## 3) Getting started with FrogLord
In the `<root folder>\PC` folder, make a copy of the `data.bin` file, and name it `data-original.bin`.  
`data.bin` contains all game data inside of it, and is what FrogLord is able to modify.  
Also, create a copy of the `SOUND` folder, and name it `SOUND (Original)`.

Next, open FrogLord and select the version of the game (in this case `Windows (Retail)`) to open.  
Then, give FrogLord the path to `data-original.bin`, **but NOT** `data.bin`.

Congrats! Any changes you were to make right now could be saved to `data.bin` using `File > Save` or `Ctrl + S`.  
**But wait!** When making mods it may be tempting to save changes to `data.bin`, then directly load changes from `data.bin`, and that's probably okay while getting familiar with FrogLord, but once it's time to make something more serious or intended for sharing, there will be problems!

> [!CAUTION]
> **Even though FrogLord CAN open modified data.bin files, this is STRONGLY discouraged. Any mods created this way will have important downsides:**
- No way of undoing changes once they are done, meaning accidentally deleting data or breaking the game means the mod is gone forever.
- The mod will only be compatible with the version of the game you modded. (Eg: No ability to make a mod support both PC + PS2 simultaneously.)
- The mod will be very difficult to work on with multiple people at the same time.
- It would be impossible for multiple mods to be installed together.
- Massive file-sizes when shared (even via delta-patching).

## 4) Making mods the "proper" way
As mentioned before, while it may be tempting to jump in and start making changes directly in FrogLord and just keeping a single modified `data.bin`, this creates problems.

Instead, the solution is to **ALWAYS** make FrogLord open a fresh unmodified version of the game (`data-original.bin`).  
FrogLord can then be used to load/apply all the mod changes from a mod project folder, solving the previously mentioned issues.

To learn how to create mods the proper way, continue by reading [this guide](./modding-guide.md).

## FAQ
### Q: What are the steps for modding the PS2 version?
The process is virtually identical to the one explained above, except you will need a method of extracting + building PS2 CD images.  
Such a process is not currently discussed by the guide, but will be added at a later date.
